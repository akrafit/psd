package com.psd.service.onlyoffice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DocumentEditorSessionService {

    private static final Duration TTL = Duration.ofMinutes(2);

    // 1 пользователь = 1 активная сессия
    private final ConcurrentHashMap<Long, DocumentEditorSession> sessionsByUserId = new ConcurrentHashMap<>();

    public record RegisterResult(boolean success, String message, DocumentEditorSession session) {}

    public RegisterResult registerSession(Long userId,
                                          String username,
                                          Long projectId,
                                          Long sectionId,
                                          Long chapterId,
                                          String documentKey) {

        Instant now = Instant.now();
        final RegisterResult[] resultHolder = new RegisterResult[1];

        sessionsByUserId.compute(userId, (key, existing) -> {

            // если старая сессия протухла — считаем, что её нет
            if (existing != null && isExpired(existing, now)) {
                existing = null;
            }

            // у пользователя уже есть активная сессия
            if (existing != null) {
                boolean sameDocument =
                        Objects.equals(existing.getProjectId(), projectId)
                                && Objects.equals(existing.getSectionId(), sectionId)
                                && Objects.equals(existing.getChapterId(), chapterId);

                if (!sameDocument) {
                    resultHolder[0] = new RegisterResult(
                            false,
                            "У вас уже открыт другой документ в OnlyOffice",
                            existing
                    );
                    return existing;
                }

                // тот же документ -> просто обновляем существующую сессию
                existing.setUsername(username);
                existing.setDocumentKey(documentKey);
                existing.setLastHeartbeatAt(now);

                resultHolder[0] = new RegisterResult(
                        true,
                        null,
                        existing
                );
                return existing;
            }

            // новой активной сессии ещё нет -> создаём
            String sessionId = UUID.randomUUID().toString();

            DocumentEditorSession newSession = DocumentEditorSession.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .username(username)
                    .projectId(projectId)
                    .sectionId(sectionId)
                    .chapterId(chapterId)
                    .documentKey(documentKey)
                    .openedAt(now)
                    .lastHeartbeatAt(now)
                    .build();

            resultHolder[0] = new RegisterResult(true, null, newSession);
            return newSession;
        });

        return resultHolder[0];
    }

    public void heartbeat(Long userId) {
        sessionsByUserId.computeIfPresent(userId, (key, existing) -> {
            existing.setLastHeartbeatAt(Instant.now());
            System.out.println("сердце " + userId);
            return existing;
        });
    }

    public void closeByUserId(Long userId) {
        DocumentEditorSession removed = sessionsByUserId.remove(userId);
        if (removed != null) {
            log.info("OnlyOffice session closed for user {}", userId);
        }
    }

    public void closeByDocumentKey(String documentKey) {
        if (documentKey == null || documentKey.isBlank()) return;

        Iterator<Map.Entry<Long, DocumentEditorSession>> it = sessionsByUserId.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, DocumentEditorSession> entry = it.next();
            DocumentEditorSession session = entry.getValue();
            if (documentKey.equals(session.getDocumentKey())) {
                it.remove();
            }
        }
    }

    public boolean hasActiveSession(Long userId) {
        DocumentEditorSession session = sessionsByUserId.get(userId);
        return session != null && !isExpired(session);
    }

    public boolean isChapterOpen(Long chapterId) {
        Instant now = Instant.now();
        return sessionsByUserId.values().stream()
                .filter(session -> !isExpired(session, now))
                .anyMatch(session -> Objects.equals(session.getChapterId(), chapterId));
    }

    public List<DocumentEditorSession> getEditorsByChapter(Long chapterId) {
        Instant now = Instant.now();
        return sessionsByUserId.values().stream()
                .filter(session -> !isExpired(session, now))
                .filter(session -> Objects.equals(session.getChapterId(), chapterId))
                .sorted(Comparator.comparing(DocumentEditorSession::getOpenedAt))
                .toList();
    }

    public List<DocumentEditorSession> getAllActiveSessions() {
        Instant now = Instant.now();
        return sessionsByUserId.values().stream()
                .filter(session -> !isExpired(session, now))
                .sorted(Comparator.comparing(DocumentEditorSession::getOpenedAt))
                .toList();
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanupExpiredSessions() {
        Instant now = Instant.now();
        Iterator<Map.Entry<Long, DocumentEditorSession>> it = sessionsByUserId.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, DocumentEditorSession> entry = it.next();
            if (isExpired(entry.getValue(), now)) {
                it.remove();
            }
        }
    }

    private boolean isExpired(DocumentEditorSession session) {
        return isExpired(session, Instant.now());
    }

    private boolean isExpired(DocumentEditorSession session, Instant now) {
        return session.getLastHeartbeatAt() == null
                || session.getLastHeartbeatAt().plus(TTL).isBefore(now);
    }

    public record SessionInfo(
            Long userId,
            String userEmail,
            Long projectId,
            Long sectionId,
            Long chapterId,
            String documentKey
    ) {}

    public Optional<SessionInfo> findByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        DocumentEditorSession session = sessionsByUserId.get(userId);
        if (session == null) {
            return Optional.empty();
        }

        if (isExpired(session)) {
            sessionsByUserId.remove(userId, session);
            return Optional.empty();
        }

        return Optional.of(new SessionInfo(
                session.getUserId(),
                session.getUsername(),
                session.getProjectId(),
                session.getSectionId(),
                session.getChapterId(),
                session.getDocumentKey()
        ));
    }
}