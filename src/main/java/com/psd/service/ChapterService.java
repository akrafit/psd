package com.psd.service;

import com.psd.dto.FileItem;
import com.psd.entity.*;
import com.psd.enums.Type;
import com.psd.repo.ChapterRepository;
import com.psd.repo.GeneralRepository;
import com.psd.repo.ProjectTableRepository;
import com.psd.repo.SectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.psd.enums.Type.SAMPLE;

@Slf4j
@Service
public class ChapterService {
    private final ChapterRepository chapterRepository;
    private final GeneralRepository generalRepository;
    private final SectionRepository sectionRepository;
    private final LocalFileService localFileService;
    private final GeneralSectionService generalSectionService;
    private final ProjectTableRepository projectTableRepository;


    public ChapterService(ChapterRepository chapterRepository, GeneralRepository generalRepository, SectionRepository sectionRepository, LocalFileService localFileService, GeneralSectionService generalSectionService, ProjectTableRepository projectTableRepository) {
        this.chapterRepository = chapterRepository;
        this.generalRepository = generalRepository;
        this.sectionRepository = sectionRepository;
        this.localFileService = localFileService;
        this.generalSectionService = generalSectionService;
        this.projectTableRepository = projectTableRepository;
    }

    public Chapter createChapter(Chapter chapter, Long generalId) {
        General general = generalRepository.findById(generalId)
                .orElseThrow(() -> new RuntimeException("General not found with id: " + generalId));
        chapter.setGeneral(general);
        chapter.setSrc(general.getSrc() + "/" + chapter.getName());
        return chapterRepository.save(chapter);
    }


    public void updateChapterSections(Long chapterId, List<Long> sectionIds) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found with id: " + chapterId));

        List<Section> sections;
        if (sectionIds != null && !sectionIds.isEmpty()) {
            sections = sectionRepository.findAllById(sectionIds);
        } else {
            sections = new ArrayList<>();
        }

        chapter.setSections(sections);
        chapterRepository.save(chapter);
    }


    public List<Chapter> getChaptersByGeneral(Long generalId) {
        return chapterRepository.findByGeneralId(generalId);
    }
    public List<Chapter> getChaptersByGeneralTemplate(General general) {
        // УБИРАЕМ вызов makeChaptersPublicUrl - больше не нужно открывать доступ через Яндекс.Диск
        return chapterRepository.findByGeneralAndTemplateTrueAndType(general, String.valueOf(Type.GENERAL));
    }

    public List<Chapter> getChaptersByGeneralTemplate(General general, Section section) {
        return chapterRepository.findByGeneralAndTemplateTrueAndContainingSection(general, section);
    }

    public List<Chapter> getChaptersByProject(Project project, Section section) {
        return chapterRepository.findChaptersByProjectAndSection(project, section.getId());
        //return chapterRepository.findChaptersByProjectAndSectionsIs(project,section);
    }

    public Long countChaptersToProject(Project project) {
        return chapterRepository.countChapterByProject(project);
    }

    public Long countChapter() {
        return chapterRepository.count();
    }

    public Chapter saveChapter(Chapter newChapter) {
        return chapterRepository.save(newChapter);
    }

    public void createChapterForTemplate(FileItem uploadedFile, Long generalId) {
        General general = generalRepository.findById(generalId)
                .orElseThrow(() -> new RuntimeException("General not found with id: " + generalId));

        Chapter chapter = new Chapter();
        chapter.setTemplate(true);
        chapter.setGeneral(general);
        chapter.setName(uploadedFile.getName());
        chapter.setPath(uploadedFile.getPath());
        chapter.setSrc(general.getSrc() + "/" + uploadedFile.getName());

        chapterRepository.save(chapter);
        log.info("Создана глава шаблона: {}", uploadedFile.getPath());
    }

    /**
     * Копирует главы из шаблона в проект (заменяет старый метод из YandexDiskService)
     */
    public Boolean copyFromTemplateToProject(List<Chapter> chapterList, Project project, Section section) {
        try {
            // 1. Находим главы, которые еще не связаны с этой секцией
            List<Chapter> chaptersToLink = new ArrayList<>();
            List<Chapter> chaptersToCreate = new ArrayList<>();

            for (Chapter templateChapter : chapterList) {
                // Ищем главу с таким же именем в проекте
                Optional<Chapter> existingChapter = chapterRepository
                        .findByProjectAndName(project, templateChapter.getName());

                if (existingChapter.isPresent()) {
                    Chapter chapter = existingChapter.get();
                    // Проверяем, не связана ли уже глава с этой секцией
                    if (!chapter.getSections().contains(section)) {
                        chaptersToLink.add(chapter);
                    }
                    // Если уже связана - ничего не делаем
                } else {
                    // Главы нет в проекте - создаем новую
                    chaptersToCreate.add(templateChapter);
                }
            }

            // 2. Связываем существующие главы с новой секцией
            if (!chaptersToLink.isEmpty()) {
                chaptersToLink.forEach(chapter -> chapter.getSections().add(section));
                chapterRepository.saveAll(chaptersToLink);
            }

            // 3. Создаем и копируем только совершенно новые главы
            if (!chaptersToCreate.isEmpty()) {
                return copyChaptersToProject(chaptersToCreate, project, section);
            }

            return true;

        } catch (Exception e) {
            log.error("Ошибка копирования глав в проект: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Копирует главы в проект (внутренний метод)
     */
    private Boolean copyChaptersToProject(List<Chapter> chapterList, Project project, Section section) {
        try {
            List<Chapter> newChapters = new ArrayList<>();

            for (Chapter templateChapter : chapterList) {
                // Копируем файл в локальном хранилище
                String newFilePath = localFileService.copyTemplateToProject(templateChapter.getPath(), project.getId());

                // Создаем новую сущность Chapter для проекта
                Chapter newChapter = new Chapter();
                newChapter.setName(templateChapter.getName());
                newChapter.setPath(newFilePath);
                newChapter.setCreated(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))));
                newChapter.setProject(project);
                newChapter.setTemplate(false);
                newChapter.getSections().add(section);
                newChapter.setGeneral(project.getGeneral());
                newChapter.setType(Type.CHAPTER.toString());
                newChapter.setSrc(project.getGeneral().getSrc() + "/" + templateChapter.getName());
                newChapters.add(newChapter);
            }

            // Сохраняем все новые Chapter в базу данных
            if (!newChapters.isEmpty()) {
                chapterRepository.saveAll(newChapters);
                // Обновляем связь проекта с главами
                project.getChapters().addAll(newChapters);
            }

            return true;

        } catch (Exception e) {
            log.error("Ошибка создания новых глав в проекте: {}", e.getMessage());
            return false;
        }
    }

    public Chapter getChapterById(Long chapterId) {
        return chapterRepository.findChapterById(chapterId);
    }

    public Chapter findById(Long id) {
        return chapterRepository.findChapterById(id);
    }

    public Chapter getResultChapterId(Section section, Project project) {
        return chapterRepository
                .findTopByProjectAndSections_IdAndTypeOrderByIdDesc(project, section.getId(), "RESULT");
    }
    @Transactional
    public void deleteChapter(Chapter chapter) {
        if (chapter == null) {
            return;
        }

        Long chapterId = chapter.getId();

        // 🔴 СНАЧАЛА убрать ссылки project_table
        projectTableRepository.deleteByAnyChapterId(chapterId);


        // 2. удалить физический файл
        try {
            localFileService.deleteFile(chapter.getPath());
        } catch (Exception e) {
            log.info("Не удалось удалить файл {}: {}", chapter.getPath(), e.getMessage());
        }

        // 3. очистка кэша фрагментов

        // 4. разорвать связи many-to-many
        chapter.getSections().clear();

        // 5. разорвать связи many-to-one
        chapter.setGeneral(null);
        chapter.setProject(null);

        // 6. обязательно зафиксировать разрыв связей до delete
        chapterRepository.flush();

        // 7. удалить саму главу
        chapterRepository.delete(chapter);
        chapterRepository.flush();
    }

    @Transactional
    public Chapter bumpVersion(Long chapterId) {
        Chapter ch = chapterRepository.findById(chapterId).orElseThrow();
        int v = ch.getVersion() == null ? 1 : ch.getVersion();
        ch.setVersion(v + 1);
        return chapterRepository.save(ch);
    }

    public Chapter getTableChapter(Project project, Section section, Type type) {
        GeneralSection generalSection = generalSectionService.findByGeneralIdAndSectionId(project.getGeneral(), section, String.valueOf(type));
        if(generalSection != null){
            return generalSection.getChapter();
        }
        return null;
    }

    public Chapter getSampleChapter(Project project, Section section) {
        GeneralSection generalSection = generalSectionService.findByGeneralIdAndSectionId(project.getGeneral(), section, String.valueOf(SAMPLE));
        if(generalSection != null){
            return generalSection.getChapter();
        }
        return null;
    }
    @Transactional(readOnly = true)
    public List<Chapter> findResultChapters(Long projectId, Long sectionId) {
        return chapterRepository.findAllResultChapters(projectId, sectionId);
    }

    @Transactional
    public void cleanupChapterBeforeDelete(Chapter chapter) {
        if (chapter == null) {
            return;
        }

        Long chapterId = chapter.getId();

        try {
            localFileService.deleteFile(chapter.getPath());
        } catch (Exception e) {
            log.info("Не удалось удалить файл {}: {}", chapter.getPath(), e.getMessage());
        }

    }

    @Transactional
    public void restoreChapterFromGeneral(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Глава не найдена"));
        if (chapter.isTemplate()) {
            throw new RuntimeException("Нельзя восстанавливать шаблонную главу");
        }
        if (chapter.getProject() == null) {
            throw new RuntimeException("Глава не привязана к проекту");
        }
        if (chapter.getProject().getGeneral() == null) {
            throw new RuntimeException("У проекта не указан генеральный проект");
        }
        if (chapter.getName() == null || chapter.getName().isBlank()) {
            throw new RuntimeException("У главы отсутствует имя");
        }
        if (chapter.getPath() == null || chapter.getPath().isBlank()) {
            throw new RuntimeException("У восстанавливаемой главы отсутствует путь к файлу");
        }
        Optional<Chapter> sourceOpt = chapterRepository.findByGeneralAndTemplateTrueAndNameAndType(
                chapter.getProject().getGeneral(),
                chapter.getName(),
                Type.GENERAL.toString()
        );
        if (sourceOpt.isEmpty()) {
            throw new RuntimeException("Источник в генеральном проекте не найден");
        }
        Chapter source = sourceOpt.get();
        if (source.getPath() == null || source.getPath().isBlank()) {
            throw new RuntimeException("У исходной главы отсутствует путь к файлу");
        }
        localFileService.restoreChapterFileFromGeneral(source.getPath(), chapter.getPath());

        bumpVersion(chapterId);
        log.info("Глава {} восстановлена из генерального проекта: {}", chapterId, source.getPath());
    }

    public String lastUpdateOnProject(Project project) {
        DateTimeFormatter parseFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        try {
            return chapterRepository.findChaptersByProject(project).stream()
                    .map(Chapter::getModified)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        try {
                            return LocalDateTime.parse(s, parseFormatter);
                        } catch (Exception e) {
                            System.out.println("Не удалось распарсить modified: " + s);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .map(dt -> dt.format(outputFormatter))
                    .orElse(null);
        } catch (Exception e) {
            System.out.println("Ошибка в lastUpdateOnProject: " + e.getMessage());
            return null;
        }
    }
}
