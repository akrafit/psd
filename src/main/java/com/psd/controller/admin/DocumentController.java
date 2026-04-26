package com.psd.controller.admin;

import com.psd.entity.Chapter;
import com.psd.service.ChapterService;
import com.psd.service.GeneralService;
import com.psd.service.SectionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/template/{generalId}")
public class DocumentController {
    private final GeneralService generalService;
    private final SectionService sectionService;
    private final ChapterService chapterService;


    @Value("${onlyoffice.document-server.api-url}")
    private String apiUrl;

    public DocumentController(GeneralService generalService, SectionService sectionService, ChapterService chapterService) {
        this.generalService = generalService;
        this.sectionService = sectionService;
        this.chapterService = chapterService;
    }

    @GetMapping("/document/{chapterId}")
    public String openDocument(@PathVariable Long generalId,
                               @PathVariable Long chapterId,
                               Model model) {
        Chapter chapter = chapterService.findById(chapterId);
        model.addAttribute("chapter", chapter);
        model.addAttribute("api", apiUrl);
        model.addAttribute("generalId", generalId);
        return "documentGeneral"; // Thymeleaf-шаблон document.html
    }
}