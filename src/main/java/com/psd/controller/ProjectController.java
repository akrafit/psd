package com.psd.controller;

import com.psd.dto.ProjectForm;
import com.psd.entity.Project;
import com.psd.entity.User;
import com.psd.enums.UserRole;
import com.psd.service.ChapterService;
import com.psd.service.GeneralService;
import com.psd.service.ProjectService;
import com.psd.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final GeneralService generalService;
    private final UserService userService;
    private final ChapterService chapterService;

    public ProjectController(ProjectService projectService, GeneralService generalService, UserService userService, ChapterService chapterService) {
        this.projectService = projectService;
        this.generalService = generalService;
        this.userService = userService;
        this.chapterService = chapterService;
    }

    @GetMapping
    public String projects(Model model, Authentication authentication) {
        User user = userService.getCurrentUser(authentication);

        List<Project> projectList;

        if (user.hasRole(UserRole.ADMIN) || user.hasRole(UserRole.GIP)) {
            projectList = projectService.findAll();
        } else if (user.hasRole(UserRole.CONTRACTOR)) {
            projectList = projectService.findWhereUserContractor(user);
        } else if (user.hasRole(UserRole.EMPLOYEE)) {
            projectList = projectService.findWhereUserEmployee(user);
        } else {
            projectList = java.util.Collections.emptyList();
        }

        model.addAttribute("projects", projectList);
        model.addAttribute("projectForm", new ProjectForm());
        model.addAttribute("user", user);
        model.addAttribute("generals", generalService.getAllGenerals());
        return "projects";
    }

    @PostMapping
    public String createProject(@ModelAttribute ProjectForm projectForm,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            Project project = new Project();
            project.setName(projectForm.getName());
            project.setDescription(projectForm.getDescription());

            // Используем новый метод без YandexResponse
            projectService.createProjectLocal(project, authentication, projectForm.getGeneralId());

            redirectAttributes.addFlashAttribute("successMessage", "Проект успешно создан!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при создании проекта: " + e.getMessage());
        }
        return "redirect:/projects";
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteProject(Authentication authentication,
                                           @PathVariable Long id){
        User user = userService.getCurrentUser(authentication);
        Project project = projectService.getProjectById(id);
        System.out.println("11");
        if (user.hasRole(UserRole.ADMIN) && project != null) {
            projectService.deleteProject(project.getId());
            return ResponseEntity.ok("Ок");
        }
        System.out.println("22");
        return ResponseEntity.badRequest().body("Ошибка");

    }
}