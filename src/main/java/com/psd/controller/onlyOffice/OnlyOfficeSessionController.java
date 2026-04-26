package com.psd.controller.onlyOffice;

import com.psd.dto.onlyoffice.ActiveEditorSessionDto;
import com.psd.dto.onlyoffice.ActiveEditorSessionViewDto;
import com.psd.dto.onlyoffice.EditorHeartbeatRequest;
import com.psd.entity.Chapter;
import com.psd.entity.Section;
import com.psd.entity.User;
import com.psd.service.ChapterService;
import com.psd.service.SectionService;
import com.psd.service.UserService;
import com.psd.service.onlyoffice.DocumentEditorSession;
import com.psd.service.onlyoffice.DocumentEditorSessionService;
import com.psd.service.onlyoffice.OnlyOfficeSessionViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/onlyoffice/sessions")
public class OnlyOfficeSessionController {

    private final DocumentEditorSessionService documentEditorSessionService;
    private final OnlyOfficeSessionViewService onlyOfficeSessionViewService;
    private final UserService userService;
    private final SectionService sectionService;
    private final ChapterService chapterService;

    @PostMapping("/heartbeat")
    public void heartbeat(@RequestBody(required = false) EditorHeartbeatRequest request,
                          Authentication authentication) {
        Long userId = userService.getCurrentUser(authentication).getId();
        documentEditorSessionService.heartbeat(userId);
    }

    @GetMapping("/active")
    public List<ActiveEditorSessionDto> getAllActiveSessions() {
        return documentEditorSessionService.getAllActiveSessions().stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/active-view")
    public List<ActiveEditorSessionViewDto> getAllActiveSessionsView() {
        return onlyOfficeSessionViewService.getAllActiveSessionsView();
    }

    @GetMapping("/chapter/{chapterId}")
    public List<ActiveEditorSessionDto> getChapterSessions(@PathVariable Long chapterId) {
        return documentEditorSessionService.getEditorsByChapter(chapterId).stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/chapter/{chapterId}/view")
    public List<ActiveEditorSessionViewDto> getChapterSessionsView(@PathVariable Long chapterId) {
        return onlyOfficeSessionViewService.getChapterSessionsView(chapterId);
    }

    private ActiveEditorSessionDto toDto(DocumentEditorSession s) {
        return ActiveEditorSessionDto.builder()
                .sessionId(s.getSessionId())
                .userId(s.getUserId())
                .username(s.getUsername())
                .projectId(s.getProjectId())
                .sectionId(s.getSectionId())
                .chapterId(s.getChapterId())
                .documentKey(s.getDocumentKey())
                .openedAt(s.getOpenedAt())
                .lastHeartbeatAt(s.getLastHeartbeatAt())
                .build();
    }
    @PostMapping("/close")
    public void closeSession(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        if (user == null) {
            return;
        }
        Long userId = user.getId();

        try {
            var sessionOpt = documentEditorSessionService.findByUserId(userId);
            if (sessionOpt.isPresent()) {
                var session = sessionOpt.get();
                Chapter chapter = chapterService.findById(session.chapterId());
                Section contextSection = sectionService.getSectionById(session.sectionId());
            }
        } catch (Exception e) {
            log.warn("Не удалось записать лог CLOSE_CHAPTER для userId={}: {}", userId, e.getMessage(), e);
        } finally {
            documentEditorSessionService.closeByUserId(userId);
        }
    }
}