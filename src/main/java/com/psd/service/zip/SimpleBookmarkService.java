package com.psd.service.zip;//package com.portal.service;
//
//import com.portal.entity.Chapter;
//import com.portal.entity.ChapterBookmark;
//import com.portal.repo.ChapterBookmarkRepository;
//import org.apache.poi.xwpf.usermodel.XWPFDocument;
//import org.apache.poi.xwpf.usermodel.XWPFParagraph;
//import org.apache.poi.xwpf.usermodel.XWPFRun;
//import org.springframework.stereotype.Service;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.InputStream;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class SimpleBookmarkService {
//    private final ChapterBookmarkRepository bookmarkRepository;
//
//    public SimpleBookmarkService(ChapterBookmarkRepository bookmarkRepository) {
//        this.bookmarkRepository = bookmarkRepository;
//    }
//
//    /**
//     * Добавляет в конец файла названия глав из списка с закладками
//     */
//    public List<ChapterBookmark> generateBookMark(Chapter chapter, List<Chapter> chapterList) {
//        List<ChapterBookmark> createdBookmarks = new ArrayList<>();
//
//        try {
//            // Проверяем существование файла
//            if (!yandexDiskService.isFolderExists(chapter.getPath())) {
//                throw new RuntimeException("Файл не найден: " + chapter.getPath());
//            }
//
//            // 1. Скачиваем файл
//            byte[] fileContent = yandexDiskService.downloadFile(chapter.getPath());
//
//            // 2. Открываем документ
//            XWPFDocument document;
//            try (InputStream inputStream = new ByteArrayInputStream(fileContent)) {
//                document = new XWPFDocument(inputStream);
//            }
//
//            // 3. Добавляем разделитель и заголовок
//            addSeparator(document);
//            addSectionTitle(document, "Содержание глав:");
//
//            // 4. Добавляем названия глав с закладками
//            for (int i = 0; i < chapterList.size(); i++) {
//                Chapter currentChapter = chapterList.get(i);
//                String bookmarkName = "chapter_" + (i + 1);
//
//                addChapterWithBookmark(document, currentChapter, bookmarkName);
//
//                ChapterBookmark bookmark = saveBookmark(bookmarkName, currentChapter, chapter);
//                createdBookmarks.add(bookmark);
//            }
//
//            // 5. Сохраняем и загружаем обратно
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            document.write(outputStream);
//            document.close();
//
//            yandexDiskService.uploadFile(chapter.getPath(), outputStream.toByteArray());
//
//            System.out.println("✅ Успешно добавлено " + createdBookmarks.size() + " закладок в файл: " + chapter.getPath());
//
//        } catch (Exception e) {
//            throw new RuntimeException("Ошибка при создании закладок: " + e.getMessage(), e);
//        }
//
//        return createdBookmarks;
//    }
//
//    private void addSeparator(XWPFDocument document) {
//        XWPFParagraph separator = document.createParagraph();
//        XWPFRun run = separator.createRun();
//        run.setText("---");
//        run.setColor("CCCCCC");
//        run.addBreak();
//    }
//
//    private void addSectionTitle(XWPFDocument document, String title) {
//        XWPFParagraph titleParagraph = document.createParagraph();
//        XWPFRun titleRun = titleParagraph.createRun();
//        titleRun.setText(title);
//        titleRun.setBold(true);
//        titleRun.setFontSize(14);
//        titleRun.addBreak();
//    }
//
//    private void addChapterWithBookmark(XWPFDocument document, Chapter chapter, String bookmarkName) {
//        XWPFParagraph paragraph = document.createParagraph();
//
//        // Простая закладка (без сложного CTBookmark)
//        XWPFRun run = paragraph.createRun();
//        run.setText(extractFileName(chapter.getPath()));
//        run.setFontSize(12);
//
//        // Можно добавить более сложные закладки при необходимости
//        addSimpleBookmark(paragraph, bookmarkName);
//    }
//
//    private void addSimpleBookmark(XWPFParagraph paragraph, String bookmarkName) {
//        // Упрощенная реализация закладки
//        // Для полной реализации нужно использовать CTBookmark
//        System.out.println("Добавлена закладка: " + bookmarkName);
//    }
//
//    private String extractFileName(String filePath) {
//        if (filePath == null || filePath.isEmpty()) return "Неизвестный файл";
//        String[] parts = filePath.split("/");
//        return parts[parts.length - 1].replace(".docx", "");
//    }
//
//    private ChapterBookmark saveBookmark(String bookmarkName, Chapter sourceChapter, Chapter targetChapter) {
//        ChapterBookmark bookmark = new ChapterBookmark();
//        bookmark.setBookmarkName(bookmarkName);
//        bookmark.setChapterTitle(extractFileName(sourceChapter.getPath()));
//        bookmark.setPath(targetChapter.getPath());
//        bookmark.setCreatedDate(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
//
//        return bookmarkRepository.save(bookmark);
//    }
//}