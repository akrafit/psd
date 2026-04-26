package com.psd.controller.onlyOffice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psd.entity.Chapter;
import com.psd.repo.ChapterRepository;
import com.psd.service.*;

import com.psd.service.onlyoffice.DocumentEditorSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/onlyoffice/callback")
public class OnlyOfficeCallbackController {
    private final ChapterRepository chapterRepository;
    private final ChapterService chapterService;

    String europeanDatePattern = "dd.MM.yyyy HH:mm:ss";
    DateTimeFormatter europeanDateFormatter = DateTimeFormatter.ofPattern(europeanDatePattern);

    public OnlyOfficeCallbackController(ChapterRepository chapterRepository,
                                        ChapterService chapterService) {
        this.chapterRepository = chapterRepository;
        this.chapterService = chapterService;
    }

    @PostMapping("/{id}")
    public ResponseEntity<Map<String, Object>> handleCallback(@PathVariable Long id,
                                                              @RequestBody Map<String, Object> body) {
        System.out.println("Callback для документа " + id + ": " + body);

        Map<String, Object> response = new HashMap<>();

        Integer status = extractStatus(body);

        if (status == null) {
            response.put("error", 0);
            return ResponseEntity.ok(response);
        }

        switch (status) {
            case 2:
                System.out.println("=== Документ " + id + " сохранен пользователем === status 2");

                boolean saved = handleDocumentSave(id, body);
                if (saved) {
                    chapterService.bumpVersion(id);

//                    try {
//                        Chapter chapter = chapterService.findById(id);
//                        if (isDocx(chapter) && !chapter.isTemplate()) {
//                            SyncRun runReindex = syncRunService.start(
//                                    chapter.getProject().getId(), null, id, SyncRun.RunType.REINDEX_FRAGMENTS
//                            );
//                            try {
//                                ReindexFragmentsResponse resp = documentProcessorClient.reindexFragments(id);
//                                fragmentDefService.upsertFromReindexResponse(id, resp);
//
//                                Map<String, Object> stats = new HashMap<>();
//                                stats.put("runId", resp.getRunId());
//                                if (resp.getCounts() != null) stats.put("counts", resp.getCounts());
//                                if (resp.getBroken() != null) stats.put("broken", resp.getBroken());
//
//                                syncRunService.finishOk(runReindex.getId(), objectMapper.writeValueAsString(stats));
//                            } catch (Exception e) {
//                                syncRunService.finishError(runReindex.getId(), "reindexFragments error: " + e.getMessage());
//                                log.warn("reindexFragments failed after save for chapter {}: {}", id, e.getMessage());
//                            }
//
//                            SyncRun runScan = syncRunService.start(
//                                    chapter.getProject().getId(), null, id, SyncRun.RunType.SCAN_INCLUDES
//                            );
//                            try {
//                                ScanIncludesResponse scan = documentProcessorClient.scanIncludes(id);
//                                includeRefService.upsertFromScan(id, scan);
//
//                                Map<String, Object> stats2 = new HashMap<>();
//                                stats2.put("runId", scan.getRunId());
//                                if (scan.getCounts() != null) stats2.put("counts", scan.getCounts());
//                                if (scan.getBroken() != null) stats2.put("broken", scan.getBroken());
//                                stats2.put("invalidMarkersCount",
//                                        scan.getInvalidMarkers() == null ? 0 : scan.getInvalidMarkers().size());
//
//                                syncRunService.finishOk(runScan.getId(), objectMapper.writeValueAsString(stats2));
//                            } catch (Exception e) {
//                                syncRunService.finishError(runScan.getId(), "scanIncludes error: " + e.getMessage());
//                                log.warn("scanIncludes failed after save for chapter {}: {}", id, e.getMessage());
//                            }
//                        }
//                    } catch (Exception e) {
//                        log.warn("Ошибка пост-обработки документа {}: {}", id, e.getMessage(), e);
//                    }
                }

                response.put("error", 0);
                break;

            case 6:
                System.out.println("=== Force save документа " + id + " === status 6");
                handleDocumentSave(id, body);
                response.put("error", 0);
                break;

            case 7: // Версия файла изменена
                System.out.println("=== OnlyOffice обнаружил изменение версии документа " + id + " ===");
                // Разрешаем перезагрузку документа
                response.put("error", 0);
                break;

            case 8: // Принудительное сохранение
                System.out.println("Стастус 8 +++++++++++++++ Пользователь вышел из документа " + id);
                response.put("error", 0);
                break;

            default:
                response.put("error", 0);
                break;
        }

        return ResponseEntity.ok(response);
    }

    private boolean isDocx(Chapter chapter) {
        String name = chapter.getName();
        if (name == null) return false;
        return name.toLowerCase().endsWith(".docx");
    }

    private boolean handleDocumentSave(Long id, Map<String, Object> body) {
        try {
            String downloadUri = getDownloadUri(body);
            if (downloadUri == null) {
                System.out.println("ОШИБКА: Не найден URL для скачивания файла");
                return false;
            }

            // Заменяем localhost на контейнер onlyoffice
            downloadUri = downloadUri
                    .replace("https://ink-aae:8071/", "http://onlyoffice/")
                    .replace("http://ink-aae:8071/", "http://onlyoffice/")
                    .replace("https://localhost:8071/", "http://onlyoffice/")
                    .replace("http://localhost:8071/", "http://onlyoffice/");
            System.out.println("Скачивание файла из: " + downloadUri);

            Chapter chapter = chapterRepository.findById(id).orElseThrow();
            Resource resource = new UrlResource(downloadUri);

            Path chapterPath = Paths.get(chapter.getPath());
            Path targetPath = chapterPath.isAbsolute()
                    ? chapterPath
                    : Paths.get("/app/file-storage").resolve(chapterPath).normalize();

            Files.createDirectories(targetPath.getParent());
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                chapter.setModified(europeanDateFormatter.format(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))));
                chapterRepository.save(chapter);
                System.out.println("=== ФАЙЛ УСПЕШНО СОХРАНЕН: " + targetPath + " ===");
                return true;
            }
        } catch (Exception e) {
            System.err.println("ОШИБКА сохранения документа " + id + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    private Integer extractStatus(Map<String, Object> body) {
        try {
            Object statusObj = body.get("status");
            if (statusObj instanceof Integer) {
                return (Integer) statusObj;
            } else if (statusObj instanceof String) {
                return Integer.parseInt((String) statusObj);
            }
        } catch (Exception e) {
            System.err.println("Ошибка извлечения статуса: " + e.getMessage());
        }
        return null;
    }

    private String getDownloadUri(Map<String, Object> body) {
        // Проверяем все возможные поля
        if (body.get("url") != null) {
            return body.get("url").toString();
        } else if (body.get("downloadUri") != null) {
            return body.get("downloadUri").toString();
        } else if (body.get("fileUrl") != null) {
            return body.get("fileUrl").toString();
        }
        System.out.println("Предупреждение: URL не найден в теле запроса");
        return null;
    }
    private String extractDocumentKey(Map<String, Object> body) {
        Object key = body.get("key");
        return key != null ? key.toString() : null;
    }
}
