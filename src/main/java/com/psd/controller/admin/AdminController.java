package com.psd.controller.admin;

import com.psd.service.ChapterService;
import com.psd.service.ProjectService;
import com.psd.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserService userService;
    private final ChapterService chapterService;
    private final ProjectService projectService;

    public AdminController(UserService userService, ChapterService chapterService, ProjectService projectService) {
        this.userService = userService;
        this.chapterService = chapterService;
        this.projectService = projectService;
    }

    @GetMapping
    public String adminPanel(Model model, Authentication authentication) {
        model.addAttribute("user", userService.getCurrentUser(authentication));
        model.addAttribute("userCount", userService.countUser());
        model.addAttribute("chapterCount", chapterService.countChapter());
        model.addAttribute("projectCount", projectService.countProject());
        return "admin";
    }
}
