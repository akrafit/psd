package com.psd.controller;

import com.psd.entity.Chapter;
import com.psd.service.ChapterService;
import com.psd.service.LocalFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileDownloadController {

    private final LocalFileService localFileService;
    private final ChapterService chapterService;

    public FileDownloadController(LocalFileService localFileService, ChapterService chapterService) {
        this.localFileService = localFileService;
        this.chapterService = chapterService;
    }

    /**
     * Скачивание файла главы
     */
    @GetMapping("/download/{chapterId}")
    public ResponseEntity<Resource> downloadChapter(@PathVariable Long chapterId) {
        try {
            Chapter chapter = chapterService.getChapterById(chapterId); // Нужно добавить этот метод в ChapterService

            Resource resource = localFileService.loadFile(chapter.getPath());

            // Определяем Content-Type
            String contentType = determineContentType(chapter.getPath());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + chapter.getName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Ошибка скачивания файла: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Просмотр файла в браузере
     */
    @GetMapping("/view/{chapterId}")
    public ResponseEntity<Resource> viewChapter(@PathVariable Long chapterId) {
        try {
            Chapter chapter = chapterService.getChapterById(chapterId);
            Resource resource = localFileService.loadFile(chapter.getPath());

            String contentType = determineContentType(chapter.getPath());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String determineContentType(String filePath) {
        try {
            String fileName = filePath.toLowerCase();
            if (fileName.endsWith(".pdf")) return "application/pdf";
            if (fileName.endsWith(".doc")) return "application/msword";
            if (fileName.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            if (fileName.endsWith(".xls")) return "application/vnd.ms-excel";
            if (fileName.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            return "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
}