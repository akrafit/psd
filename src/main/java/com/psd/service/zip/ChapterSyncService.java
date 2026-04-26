package com.psd.service.zip;//package com.portal.service;
//
//import com.portal.dto.YandexDiskItem;
//import com.portal.entity.Chapter;
//import com.portal.repo.ChapterRepository;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//public class ChapterSyncService {
//
//    private final ChapterRepository chapterRepository;
//
//    public ChapterSyncService(ChapterRepository chapterRepository) {
//        this.chapterRepository = chapterRepository;
//    }
//
//    @Transactional
//    public void syncChapterFromYandexDiskItem(YandexDiskItem diskItem, Chapter existingChapter) {
//        // Обновляем основные поля
//        existingChapter.setName(diskItem.getName());
//        existingChapter.setPath(diskItem.getPath());
//        existingChapter.setResourceId(diskItem.getResourceId());
//        existingChapter.setSrc(diskItem.getFile() != null ? diskItem.getFile() : diskItem.getPreview());
//        existingChapter.setPublicUrl(diskItem.getPublicUrl());
//
//        // Сохраняем обновлённую сущность
//        chapterRepository.save(existingChapter);
//    }
//
//    // Метод для поиска Chapter по resourceId (если нужно обновить существующий)
//    public Chapter findChapterByResourceId(String resourceId) {
//        return chapterRepository.findByResourceId(resourceId).orElse(null);
//    }
//}