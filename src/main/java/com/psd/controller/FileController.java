package com.psd.controller;

import com.psd.dto.FileItem;
import com.psd.entity.Chapter;
import com.psd.service.ChapterService;
import com.psd.service.LocalFileService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final LocalFileService localFileService;
    private final ChapterService chapterService;

    public FileController(LocalFileService localFileService, ChapterService chapterService) {
        this.localFileService = localFileService;
        this.chapterService = chapterService;
    }

    /**
     * Проверяет существование файла
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> fileExists(@RequestParam String filePath) {
        try {
            boolean exists = localFileService.fileExists(filePath);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
    }

    /**
     * Получает информацию о файле
     */
    @GetMapping("/info")
    public ResponseEntity<FileItem> getFileInfo(@RequestParam String filePath) {
        try {
            // Здесь можно добавить логику для получения дополнительной информации о файле
            FileItem item = new FileItem();
            item.setPath(filePath);
            item.setExists(localFileService.fileExists(filePath));
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getFile(@PathVariable Long id) {
        Chapter chapter = chapterService.findById(id);

        Path filePath = Paths.get("/app/file-storage").resolve(chapter.getPath());
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            throw new RuntimeException("File not found: " + filePath);
        }

        String fileName = filePath.getFileName().toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + UriUtils.encode(fileName, StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}