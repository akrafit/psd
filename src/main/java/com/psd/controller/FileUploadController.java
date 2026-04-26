package com.psd.controller;

import com.psd.dto.FileItem;
import com.psd.entity.*;
import com.psd.enums.Type;
import com.psd.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final LocalFileService localFileService;
    private final ChapterService chapterService;
    private final GeneralService generalService;
    private final SectionService sectionService;
    private final GeneralSectionService generalSectionService;
    private final ProjectService projectService;

    public FileUploadController(LocalFileService localFileService,
                                ChapterService chapterService,
                                GeneralService generalService,
                                SectionService sectionService,
                                GeneralSectionService generalSectionService, ProjectService projectService) {
        this.localFileService = localFileService;
        this.chapterService = chapterService;
        this.generalService = generalService;
        this.sectionService = sectionService;
        this.generalSectionService = generalSectionService;
        this.projectService = projectService;
    }

    /**
     * Загружает шаблон для конкретного раздела и создает запись в GeneralSection
     * Добавлен функционал для загрузки общей таблицы, через Type
     * Путь: psd/section/{generalId}/section_{sectionId}_имя_файла
     */
    @PostMapping("/upload/section-template/{generalId}/{sectionId}")
    public ResponseEntity<FileItem> uploadSectionTemplate(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long generalId,
            @PathVariable Long sectionId,
            @RequestParam("type") Type type) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new FileItem("Отсутствует файл"));
            }

            General general = generalService.getGeneralById(generalId);
            if (general == null) {
                return ResponseEntity.badRequest().body(new FileItem("General не найден"));
            }

            Section section = sectionService.getSectionById(sectionId);
            if (section == null) {
                return ResponseEntity.badRequest().body(new FileItem("Раздел не найден"));
            }


            // Сохраняем файл локально
            String filePath = localFileService.saveTemplateSectionFile(file, generalId, sectionId);


            Chapter chapter = new Chapter();
            chapter.setName(file.getOriginalFilename());
            chapter.setPath(filePath);
            chapter.setSize(file.getSize());
            chapter.setTemplate(true);
            chapter.setGeneral(general);
            chapter.setSrc(filePath);
            chapter.setType(String.valueOf(type));
            chapter.setCreated(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))));

            // Сохраняем Chapter для шаблона раздела
            chapterService.saveChapter(chapter);

            // Создаем или обновляем запись в GeneralSection
            generalSectionService.createOrUpdateGeneralSection(general, section, chapter, type);

            return ResponseEntity.ok(new FileItem());
        } catch (Exception e) {
            log.error("Ошибка загрузки шаблона для раздела: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new FileItem("Ошибка загрузки шаблона раздела: " + e.getMessage()));
        }
    }

    /**
     * Удаляет привязку шаблона к разделу
     */
    @DeleteMapping("/remove/section-template/{generalId}/{sectionId}/{type}")
    public ResponseEntity<?> removeSectionTemplate(
            @PathVariable Long generalId,
            @PathVariable Long sectionId,
            @PathVariable Type type) {
        try {
            // Находим и удаляем запись GeneralSection
            General general = generalService.getGeneralById(generalId);
            Section section = sectionService.getSectionById(sectionId);
            GeneralSection generalSection = generalSectionService.findByGeneralIdAndSectionId(general,section,String.valueOf(type));

            if (generalSection != null) {
                // Удаляем запись GeneralSection
                generalSectionService.delete(generalSection);
                // Удаляем связанную главу
                chapterService.deleteChapter(generalSection.getChapter());
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Ошибка удаления шаблона раздела: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Ошибка удаления шаблона раздела: " + e.getMessage());
        }
    }

    /**
     * Удаляет главу
     */
    @DeleteMapping("/remove/chapter-template/{generalId}/{chapterId}")
    public ResponseEntity<?> removeChapterTemplate(
            @PathVariable Long generalId,
            @PathVariable Long chapterId) {
        try {
            // Находим и удаляем запись GeneralSection
            General general = generalService.getGeneralById(generalId);
            Chapter chapter = chapterService.getChapterById(chapterId);

            if (chapter != null && chapter.getGeneral().getId().equals(general.getId())) {
                chapterService.deleteChapter(chapter);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Ошибка удаления главы: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Ошибка удаления главы: " + e.getMessage());
        }
    }

    /**
     * Загружает файл в папку psd/templates/{generalId}
     */
    @PostMapping("/upload/template")
    public ResponseEntity<FileItem> uploadToTemplate(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("generalId") Long generalId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new FileItem("Отсутствует файл"));
            }

            General general = generalService.getGeneralById(generalId);
            if (general == null) {
                return ResponseEntity.badRequest().body(new FileItem("Шаблон не найден"));
            }

            // Сохраняем файл локально
            String filePath = localFileService.saveTemplateFile(file, generalId);

            Chapter chapter = new Chapter();
            chapter.setName(file.getOriginalFilename());
            chapter.setPath(filePath);
            chapter.setSize(file.getSize());
            chapter.setTemplate(true);
            chapter.setGeneral(general);
            chapter.setSrc(filePath);
            chapter.setType(String.valueOf(Type.GENERAL));
            chapter.setCreated(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))));

            // Сохраняем Chapter для шаблона раздела
            chapterService.saveChapter(chapter);

            return ResponseEntity.ok(new FileItem());
        } catch (Exception e) {
            log.error("Ошибка загрузки файла в шаблон: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new FileItem("Ошибка загрузки: " + e.getMessage()));
        }
    }
    @PostMapping("/upload/templates/batch")
    public ResponseEntity<?> uploadBatch(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("generalId") Long generalId) {

        if (files.length > 30) {
            return ResponseEntity.badRequest()
                    .body("Максимум 10 файлов за раз");
        }

        General general = generalService.getGeneralById(generalId);

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            // Сохраняем файл локально
            String filePath = localFileService.saveTemplateFile(file, generalId);

            Chapter chapter = new Chapter();
            chapter.setName(file.getOriginalFilename());
            chapter.setPath(filePath);
            chapter.setSize(file.getSize());
            chapter.setTemplate(true);
            chapter.setGeneral(general);
            chapter.setSrc(filePath);
            chapter.setType(String.valueOf(Type.GENERAL));
            chapter.setCreated(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))));

            // Сохраняем Chapter для шаблона раздела
            chapterService.saveChapter(chapter);

        }

        return ResponseEntity.ok(Map.of(
                "uploaded", files.length
        ));
    }

    /**
     * Загружает файл в папку psd/projects/{projectId}
     */
    @PostMapping("/upload/project/{projectId}")
    public ResponseEntity<FileItem> uploadToProject(@RequestParam("file") MultipartFile file,
                                                    @PathVariable Long projectId) {
        Optional<Project> project = projectService.findById(projectId);
        if(project.isEmpty()){
            return ResponseEntity.badRequest().body(new FileItem("Ошибка загрузки"));
        }
        try {
            String filePath = localFileService.saveProjectFile(file, projectId);

            Chapter chapter = new Chapter();
            chapter.setName(file.getOriginalFilename());
            chapter.setPath(filePath);
            chapter.setSize(file.getSize());
            chapter.setTemplate(true);
            chapter.setProject(project.get());
            chapter.setSrc(filePath);
            chapter.setType(String.valueOf(Type.CHAPTER));
            chapter.setCreated(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))));
            // Сохраняем Chapter
            chapterService.saveChapter(chapter);

            return ResponseEntity.ok(new FileItem());
        } catch (Exception e) {
            log.error("Ошибка загрузки файла в проект: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new FileItem("Ошибка загрузки: " + e.getMessage()));
        }
    }

    /**
     * Удаляет файл
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestParam String filePath) {
        try {
            localFileService.deleteFile(filePath);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Ошибка удаления файла: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Ошибка удаления файла: " + e.getMessage());
        }
    }
}