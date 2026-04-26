package com.psd.controller.admin;

import com.psd.entity.*;
import com.psd.enums.Type;
import com.psd.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/admin/generals")
public class GeneralController {

    private final GeneralService generalService;
    private final ChapterService chapterService;
    private final SectionService sectionService;
    private final GeneralSectionService generalSectionService;
    private final UserService userService;

    public GeneralController(GeneralService generalService,
                             ChapterService chapterService,
                             SectionService sectionService,
                             GeneralSectionService generalSectionService, UserService userService) {
        this.generalService = generalService;
        this.chapterService = chapterService;
        this.sectionService = sectionService;
        this.generalSectionService = generalSectionService;
        this.userService = userService;
    }

    @GetMapping
    public String getAllGenerals(Model model, Authentication authentication) {
        try {
            model.addAttribute("user", userService.getCurrentUser(authentication));
            List<General> generals = generalService.getAllGenerals();
            model.addAttribute("generals", generals);
            if(!generals.isEmpty()){
                model.addAttribute("generals", generals);
            }else{
                model.addAttribute("generals",  null);
            }
            return "admin/generals-list";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при загрузке списка: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/{id}")
    public String getGeneralDetail(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            General general = generalService.getGeneralById(id);
            if (general == null){
                return "error";
            }

            // Получаем все главы (обычные)
            List<Chapter> chapters = chapterService.getChaptersByGeneralTemplate(general);
            chapters.sort(Comparator.comparing(Chapter::getName));

            // Создаем Map для быстрой проверки связей (для всех глав)
            Map<Long, Set<Long>> chapterSectionMap = new HashMap<>();

            // Добавляем связи для обычных глав
            for (Chapter chapter : chapters) {
                Set<Long> sectionIds = chapter.getSections().stream()
                        .map(Section::getId)
                        .collect(Collectors.toSet());
                chapterSectionMap.put(chapter.getId(), sectionIds);
            }

            // Собираем sectionId, у которых есть хотя бы одна глава с чекбоксом
            Set<Long> sectionsWithChapters = chapterSectionMap.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());

                        // Показываем раздел если:
                        // 1) visible = true
                        // ИЛИ
                        // 2) у раздела есть хотя бы одна глава
            List<Section> sections = sectionService.getAllSections().stream()
                    .filter(section -> Boolean.TRUE.equals(section.getVisible())
                            || sectionsWithChapters.contains(section.getId()))
                    .sorted(Comparator.comparing(Section::getName))
                    .toList();
            // Создаем Map для связи разделов с шаблонными главами через GeneralSection
            Map<Long, Chapter> generalSectionMap = generate(general, String.valueOf(Type.SAMPLE));
            // Создаем Map для связи разделов с шаблонными таблицами через GeneralSection
            Map<Long, Chapter> generalSectionTableMap = generate(general, String.valueOf(Type.TABLE));

            model.addAttribute("user", userService.getCurrentUser(authentication));
            model.addAttribute("generalSectionMap", generalSectionMap);
            model.addAttribute("generalSectionTableMap", generalSectionTableMap);
            model.addAttribute("general", general);
            model.addAttribute("chapters", chapters);
            model.addAttribute("sections", sections);
            model.addAttribute("chapterSectionMap", chapterSectionMap);

            return "admin/general-detail";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при загрузке деталей генерального: " + e.getMessage());
            return "error";
        }
    }

    private Map<Long, Chapter> generate(General general, String valueOf) {
        Map<Long, Chapter> generalSectionMap = new HashMap<>();
        List<GeneralSection> generalSections = generalSectionService.findByGeneral(general, valueOf);
        for (GeneralSection gs : generalSections) {
            if (gs.getChapter() != null) {
                generalSectionMap.put(gs.getSection().getId(), gs.getChapter());
            }
        }
        return generalSectionMap;
    }

    @GetMapping("/sections")
    public String getSectionsManagement(Model model, Authentication authentication) {
        try {
            List<Section> sections = sectionService.getAllSections();
            sections.sort(Comparator.comparing(Section::getName));
            model.addAttribute("user", userService.getCurrentUser(authentication));
            model.addAttribute("sections", sections);
            model.addAttribute("newSection", new Section());
            return "admin/sections-management";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при загрузке разделов: " + e.getMessage());
            return "error";
        }
    }
}