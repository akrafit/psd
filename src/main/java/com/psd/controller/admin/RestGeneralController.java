package com.psd.controller.admin;

import com.psd.dto.ChapterForm;
import com.psd.dto.FileItem;
import com.psd.entity.Chapter;
import com.psd.entity.General;
import com.psd.entity.Section;
import com.psd.service.ChapterService;
import com.psd.service.GeneralService;
import com.psd.service.LocalFileService;
import com.psd.service.SectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/admin/generals")
public class RestGeneralController {

    private final GeneralService generalService;
    private final ChapterService chapterService;
    private final SectionService sectionService;
    private final LocalFileService localFileService;

    public RestGeneralController(GeneralService generalService,
                                 ChapterService chapterService,
                                 SectionService sectionService,
                                 LocalFileService localFileService) {
        this.generalService = generalService;
        this.chapterService = chapterService;
        this.sectionService = sectionService;
        this.localFileService = localFileService;
    }

    @PostMapping
    public String createGeneral(@ModelAttribute General general) {
        try {
            general.setSrc(general.getName());
            General serviceGeneral = generalService.createGeneral(general);
            if (serviceGeneral == null){
                return "redirect:/admin/generals?error=ошибка_создания";
            }
            return "redirect:/admin/generals";
        } catch (Exception e) {
            return "redirect:/admin/generals?error=" + e.getMessage();
        }
    }

    @PostMapping("/{generalId}/chapters")
    public String createChapter(@PathVariable Long generalId,
                                @ModelAttribute ChapterForm chapterForm) {
        try {
            Chapter chapter = new Chapter();
            chapter.setName(chapterForm.getName());
            chapter.setSrc(chapterForm.getSrc());
            chapterService.createChapter(chapter, generalId);
            return "redirect:/admin/generals/" + generalId;
        } catch (Exception e) {
            return "redirect:/admin/generals/" + generalId + "?error=" + e.getMessage();
        }
    }

    @PostMapping("/{generalId}/chapters/{chapterId}/sections")
    public String updateChapterSections(@PathVariable Long generalId,
                                        @PathVariable Long chapterId,
                                        @RequestParam(required = false) List<Long> sectionIds) {
        try {
            chapterService.updateChapterSections(chapterId, sectionIds != null ? sectionIds : List.of());
            return "redirect:/admin/generals/" + generalId;
        } catch (Exception e) {
            return "redirect:/admin/generals/" + generalId + "?error=" + e.getMessage();
        }
    }


    @PostMapping("/sections")
    public String createSection(@ModelAttribute Section section) {
        try {
            sectionService.createSection(section);
            return "redirect:/admin/generals/sections";
        } catch (Exception e) {
            return "redirect:/admin/generals/sections?error=" + e.getMessage();
        }
    }

    @PostMapping("/{generalId}/save-sections")
    public String saveAllChapterSections(@PathVariable Long generalId,
                                         @RequestParam(value = "chapterSections", required = false) List<String> chapterSections) {
        try {
            // 1) Собираем выбранные секции по главам
            Map<Long, List<Long>> chapterSectionsMap = new HashMap<>();

            if (chapterSections != null) {
                for (String chapterSection : chapterSections) {
                    String[] parts = chapterSection.split("_");
                    if (parts.length == 2) {
                        Long chapterId = Long.parseLong(parts[0]);
                        Long sectionId = Long.parseLong(parts[1]);
                        chapterSectionsMap
                                .computeIfAbsent(chapterId, k -> new ArrayList<>())
                                .add(sectionId);
                    }
                }
            }

            // 2) ВАЖНО: обновляем ВСЕ главы данного general
            List<Chapter> chapters = chapterService.getChaptersByGeneral(generalId);

            for (Chapter chapter : chapters) {
                List<Long> selected = chapterSectionsMap.getOrDefault(chapter.getId(), Collections.emptyList());
                chapterService.updateChapterSections(chapter.getId(), selected);
            }

            return "redirect:/admin/generals/" + generalId + "?success=Sections+updated";
        } catch (Exception e) {
            return "redirect:/admin/generals/" + generalId + "?error=Error+saving+sections";
        }
    }


    /**
     * Новый метод для загрузки файла в шаблон
     */
    @PostMapping("/{generalId}/upload-template")
    public String uploadTemplateFile(@PathVariable Long generalId,
                                     @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return "redirect:/admin/generals/" + generalId + "?error=Файл+не+выбран";
            }

            General general = generalService.getGeneralById(generalId);
            if (general == null) {
                return "redirect:/admin/generals/" + generalId + "?error=Шаблон+не+найден";
            }

            // Сохраняем файл локально
            String filePath = localFileService.saveTemplateFile(file, generalId);

            // Создаем запись в БД
            FileItem item = new FileItem();
            item.setName(file.getOriginalFilename());
            item.setPath(filePath);
            item.setType("file");

            chapterService.createChapterForTemplate(item, generalId);

            return "redirect:/admin/generals/" + generalId + "?success=Файл+успешно+загружен";

        } catch (Exception e) {
            log.error("Ошибка загрузки файла в шаблон: {}", e.getMessage());
            return "redirect:/admin/generals/" + generalId + "?error=" +
                    URLEncoder.encode("Ошибка загрузки: " + e.getMessage(), StandardCharsets.UTF_8);
        }
    }


    /**
     * Вспомогательный метод для извлечения имени файла из пути
     */
    private String extractFileNameFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "file_" + System.currentTimeMillis();
        }
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < path.length() - 1) {
            return path.substring(lastSlashIndex + 1);
        }
        return path;
    }
    @PostMapping("/sections/{sectionId}/visibility")
    @ResponseBody
    public Map<String, Object> updateSectionVisibility(@PathVariable Long sectionId,
                                                       @RequestBody Map<String, Object> payload) {
        boolean visible = Boolean.parseBoolean(String.valueOf(payload.get("visible")));
        Section updated = sectionService.updateVisibility(sectionId, visible);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("sectionId", updated.getId());
        result.put("visible", updated.getVisible());
        return result;
    }
}