package com.psd.service.zip;//package com.portal.controller;
//
//import com.portal.dto.YandexDiskItem;
//import com.portal.entity.Chapter;
//import com.portal.entity.Section;
//import com.portal.service.ChapterService;
//import com.portal.service.SectionService;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//
//@Controller
//@RequestMapping("/section-template")
//public class SectionTemplateController {
//
//    private final SectionService sectionService;
//    private final ChapterService chapterService;
//
//    public SectionTemplateController(SectionService sectionService,
//                                     ChapterService chapterService) {
//        this.sectionService = sectionService;
//        this.chapterService = chapterService;
//    }
//
//    /**
//     * Загрузка или замена шаблона для раздела
//     */
//    @PostMapping("/upload/{sectionId}")
//    public String uploadSectionTemplate(
//            @PathVariable Long sectionId,
//            @RequestParam("file") MultipartFile file) {
//
//        try {
//            if (file.isEmpty()) {
//                return "redirect:/admin/generals/sections?message=" + encodeMessage("Файл пустой") + "&type=error";
//            }
//
//            Section section = sectionService.getSectionById(sectionId);
//            if (section == null) {
//                return "redirect:/admin/generals/sections?message=" + encodeMessage("Раздел не найден") + "&type=error";
//            }
//
//            // Путь для сохранения на Яндекс.Диске
//            String folderPath = "portal/sections";
//
//            // Загружаем файл используя существующий метод
//            YandexDiskItem uploadedFile = yandexDiskService.uploadFileToFolder(file, folderPath);
//
//            if (uploadedFile == null) {
//                return "redirect:/admin/generals/sections?message=" + encodeMessage("Ошибка загрузки файла на Яндекс.Диск") + "&type=error";
//            }
//
//            // Если уже есть шаблон - удаляем старый файл и обновляем Chapter
//            if (section.getTemplateChapter() != null) {
//                Chapter existingChapter = section.getTemplateChapter();
//
//                // Удаляем старый файл с Яндекс.Диска
//                yandexDiskService.deleteFile(existingChapter.getPath());
//
//                // Обновляем существующий Chapter с новыми данными
//                updateExistingChapter(existingChapter, uploadedFile);
//
//                // Сохраняем обновленный Chapter
//                chapterService.updateChapter(existingChapter);
//
//                return "redirect:/admin/generals/sections?message=" + encodeMessage("Шаблон успешно заменен") + "&type=success";
//
//            } else {
//                // Создаем новый Chapter как шаблон
//                //Chapter templateChapter = createTemplateChapter(uploadedFile);
//                Chapter templateChapter = new Chapter(uploadedFile,null);
//                templateChapter.setTemplate(true);
//                Chapter savedChapter = chapterService.createChapterForSectionTemplate(templateChapter);
//
//                // Связываем раздел с шаблонным Chapter
//                section.setTemplateChapter(savedChapter);
//                sectionService.updateSection(section);
//
//                return "redirect:/admin/generals/sections?message=" + encodeMessage("Шаблон успешно загружен") + "&type=success";
//            }
//
//        } catch (Exception e) {
//            return "redirect:/admin/generals/sections?message=" + encodeMessage("Ошибка загрузки шаблона") + "&type=error";
//        }
//    }
//
//    /**
//     * Удаление шаблона раздела
//     */
//    @PostMapping("/delete/{sectionId}")
//    public String deleteSectionTemplate(@PathVariable Long sectionId) {
//        try {
//            Section section = sectionService.getSectionById(sectionId);
//            if (section == null || section.getTemplateChapter() == null) {
//                return "redirect:/admin/generals/sections?message=" + encodeMessage("Шаблон не найден") + "&type=error";
//            }
//
//            Chapter templateChapter = section.getTemplateChapter();
//            // Удаляем файл с Яндекс.Диска
//            yandexDiskService.deleteFile(templateChapter.getPath());
//            // Убираем связь
//            section.setTemplateChapter(null);
//            sectionService.updateSection(section);
//            // Удаляем Chapter из базы
//            chapterService.deleteChapter(templateChapter.getId());
//            return "redirect:/admin/generals/sections?message=" + encodeMessage("Шаблон успешно удален") + "&type=success";
//
//        } catch (Exception e) {
//            return "redirect:/admin/generals/sections?message=" + encodeMessage("Ошибка удаления шаблона") + "&type=error";
//        }
//    }
//
//
//
//    private void updateExistingChapter(Chapter existingChapter, YandexDiskItem newFile) {
//        existingChapter.setName(newFile.getName());
//        existingChapter.setPath(newFile.getPath());
//        existingChapter.setResourceId(newFile.getResourceId());
//        existingChapter.setModified(newFile.getModified());
//        existingChapter.setPublicUrl(newFile.getPublicUrl());
//        existingChapter.setSrc(newFile.getFile() != null ? newFile.getFile() : newFile.getPreview());
//        existingChapter.setSize(newFile.getSize());
//    }
//
//    // Метод для кодирования русских символов в URL
//    private String encodeMessage(String message) {
//        return URLEncoder.encode(message, StandardCharsets.UTF_8);
//    }
//}