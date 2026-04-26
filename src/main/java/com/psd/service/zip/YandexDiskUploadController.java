package com.psd.service.zip;//package com.portal.controller;
//
//import com.portal.dto.YandexDiskItem;
//import com.portal.entity.General;
//import com.portal.service.ChapterService;
//import com.portal.service.GeneralService;
//import com.portal.service.YandexDiskService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//@RestController
//@RequestMapping("/api/yandex-disk")
//public class YandexDiskUploadController {
//
//    private final YandexDiskService yandexDiskService;
//    private final ChapterService chapterService;
//    private final GeneralService generalService;
//
//    public YandexDiskUploadController(YandexDiskService yandexDiskService, ChapterService chapterService, GeneralService generalService) {
//        this.yandexDiskService = yandexDiskService;
//        this.chapterService = chapterService;
//        this.generalService = generalService;
//    }
//
//    /**
//     * Загружает файл в папку portal/template
//     */
//    @PostMapping("/upload/template")
//    public ResponseEntity<YandexDiskItem> uploadToTemplate(@RequestParam("file") MultipartFile file, @RequestParam("generalId") Long generalId) {
//        try {
//
//            if (file.isEmpty()) {
//                return ResponseEntity.badRequest()
//                        .body(new YandexDiskItem("Отсутствует файл"));
//            }
//
//            if (generalId == null) {
//                return ResponseEntity.badRequest()
//                        .body(new YandexDiskItem("Проблема с сайтом"));
//            }
//            // Создаем папку если не существует
//            General general = generalService.getGeneralById(generalId);
//            if (general == null) {
//                return ResponseEntity.badRequest()
//                        .body(new YandexDiskItem("Не существующий главный шаблон"));
//            }
//            yandexDiskService.createTemplateFolderIfNotExists();
//            yandexDiskService.createFolderForGeneral(general);
//            // Загружаем файл
//            YandexDiskItem uploadedFile = yandexDiskService.uploadFileToGeneralFolder(file, generalId);
//            if (uploadedFile.getError() != null){
//                return ResponseEntity.badRequest().body(uploadedFile);
//            }
//            chapterService.createChapterForTemplate(uploadedFile, generalId);
//            return ResponseEntity.ok(uploadedFile);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    /**
//     * Загружает файл в папку проекта
//     */
//    @PostMapping("/upload/project/{projectId}")
//    public ResponseEntity<YandexDiskItem> uploadToProject(
//            @RequestParam("file") MultipartFile file,
//            @PathVariable Long projectId) {
//        try {
//            YandexDiskItem uploadedFile = yandexDiskService.uploadFileToProjectFolder(file, projectId);
//            return ResponseEntity.ok(uploadedFile);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    /**
//     * Загружает файл в папку шаблона (General)
//     */
//    @PostMapping("/upload/general/{generalId}")
//    public ResponseEntity<YandexDiskItem> uploadToGeneral(
//            @RequestParam("file") MultipartFile file,
//            @PathVariable Long generalId) {
//        try {
//            YandexDiskItem uploadedFile = yandexDiskService.uploadFileToGeneralFolder(file, generalId);
//            return ResponseEntity.ok(uploadedFile);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    /**
//     * Универсальный метод загрузки в указанную папку
//     */
//    @PostMapping("/upload")
//    public ResponseEntity<YandexDiskItem> uploadToFolder(
//            @RequestParam("file") MultipartFile file,
//            @RequestParam("folderPath") String folderPath) {
//        try {
//            YandexDiskItem uploadedFile = yandexDiskService.uploadFileToFolder(file, folderPath);
//            return ResponseEntity.ok(uploadedFile);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }
//}