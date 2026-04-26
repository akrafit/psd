package com.psd.controller;

import com.psd.entity.Chapter;
import com.psd.repo.ChapterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/internal/chapters")
@RequiredArgsConstructor
@Slf4j
public class InternalChapterController {

    private final ChapterRepository chapterRepository;

    // В application.yml: app.internal-token: ${APP_INTERNAL_TOKEN:supersecret-internal}
    @Value("${app.internal-token}")
    private String internalToken;

    // Как у тебя в callback: если путь относительный — он лежит в /app/file-storage
    private static final Path STORAGE_ROOT = Paths.get("/app/file-storage");

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> download(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Token", required = false) String token
    ) {
        checkToken(token);

        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found: " + id));

        Path filePath = resolveChapterPath(chapter);

        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + filePath);
        }

        Resource resource = new FileSystemResource(filePath);

        String original = filePath.getFileName().toString();
        String asciiFallback = original.replaceAll("[^A-Za-z0-9._-]", "_");

// RFC 5987
        String encoded = java.net.URLEncoder.encode(original, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");

        String contentDisposition =
                "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(resource);
    }
    @GetMapping("/{id}/meta")
    public Map<String, Object> meta(@PathVariable Long id,
                                    @RequestHeader(value="X-Internal-Token", required=false) String token) {
        checkToken(token);
        Chapter ch = chapterRepository.findById(id).orElseThrow();
        return Map.of(
                "chapterId", ch.getId(),
                "projectId", ch.getProject().getId(),
                "name", ch.getName()
        );
    }

    @PutMapping(value = "/{id}/content",
            consumes = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    public ResponseEntity<Void> upload(
            @PathVariable Long id,
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody byte[] data
    ) throws IOException {
        checkToken(token);

        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found: " + id));

        Path filePath = resolveChapterPath(chapter);

        Files.createDirectories(filePath.getParent());
        Files.write(filePath, data);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/resolve")
    public Map<String, Long> resolve(
            @RequestBody ResolveChapterRequest req,
            @RequestHeader("X-Internal-Token") String token
    ) {
        checkToken(token);

        String raw = req.filename() == null ? "" : req.filename().trim();
        if (raw.isBlank()) return Map.of();

        log.info("RESOLVE projectId={} filename='{}'", req.projectId(), req.filename());

        String withExt = raw.toLowerCase().endsWith(".docx") ? raw : raw + ".docx";
        String withoutExt = raw.replaceAll("(?i)\\.docx$", "");

        // ещё полезно: схлопнуть множественные пробелы (частая причина)
        raw = raw.replace('\u00A0', ' ').replaceAll("\\s+", " ");
        withExt = withExt.replace('\u00A0', ' ').replaceAll("\\s+", " ");
        withoutExt = withoutExt.replace('\u00A0', ' ').replaceAll("\\s+", " ");

        String finalWithExt = withExt;
        String finalWithoutExt = withoutExt;
        log.warn("RESOLVE NOT FOUND normalized raw='{}' withExt='{}' withoutExt='{}'", raw, withExt, withoutExt);
        return chapterRepository.findByProjectIdAndNameIgnoreCase(req.projectId(), raw)
                .or(() -> chapterRepository.findByProjectIdAndNameIgnoreCase(req.projectId(), finalWithExt))
                .or(() -> chapterRepository.findByProjectIdAndNameIgnoreCase(req.projectId(), finalWithoutExt))
                .map(ch -> Map.of("chapterId", ch.getId()))
                .orElseGet(Map::of);
    }

    public record ResolveChapterRequest(Long projectId, String filename) {}


    // Если когда-то потребуется загрузка без строгого Content-Type:
    // @PutMapping("/{id}/content") ... consumes можно убрать

    private Path resolveChapterPath(Chapter chapter) {
        Path p = Paths.get(chapter.getPath());

        // Если абсолютный путь — используем как есть
        if (p.isAbsolute()) {
            return p.normalize();
        }

        // Если относительный — считаем от /app/file-storage
        return STORAGE_ROOT.resolve(p).normalize();
    }

    private void checkToken(String token) {
        if (token == null || token.isBlank() || !token.equals(internalToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }
    }
    private String normalizeChapterName(String filename) {
        if (filename == null) return null;
        String s = filename.trim();
        // убрать .docx (без учета регистра)
        s = s.replaceAll("(?i)\\.docx$", "");
        return s;
    }
}
