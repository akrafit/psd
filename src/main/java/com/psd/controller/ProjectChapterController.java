package com.psd.controller;

import com.psd.dto.SectionStatusDto;
import com.psd.entity.*;
import com.psd.enums.Type;
import com.psd.enums.UserRole;
import com.psd.repo.SectionRepository;
import com.psd.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/projects/{projectId}/documents")
public class ProjectChapterController {

    private final ProjectService projectService;
    private final LocalFileService localFileService;
    private final SectionRepository sectionRepository;
    private final ChapterService chapterService;
    private final UserService userService;
    private final SectionAssignmentService sectionAssignmentService;
    private final ProjectTableSampleService projectTableSampleService;

    public ProjectChapterController(ProjectService projectService,
                                    LocalFileService localFileService, SectionRepository sectionRepository,
                                    ChapterService chapterService,
                                    UserService userService,
                                    SectionAssignmentService sectionAssignmentService, ProjectTableSampleService projectTableSampleService) {
        this.projectService = projectService;
        this.localFileService = localFileService;
        this.sectionRepository = sectionRepository;
        this.chapterService = chapterService;
        this.userService = userService;
        this.sectionAssignmentService = sectionAssignmentService;
        this.projectTableSampleService = projectTableSampleService;
    }

    @GetMapping
    public String getProjectSections(@PathVariable Long projectId, Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.getCurrentUser(authentication);
            model.addAttribute("user", user);
            model.addAttribute("isProjectCreator", isProjectCreator(projectId, user));
            Project project = projectService.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            //Если подрядчик то только его разделы
            if (!user.hasRole(UserRole.CONTRACTOR)) {
                List<Section> sections = projectService.getAllSections(project);
                List<SectionStatusDto> sectionStatuses = sections.stream()
                        .map(section -> {
                            SectionStatusDto dto = SectionStatusDto.from(section, project);
                            List<User> assignedUsers = sectionAssignmentService.getAssignedUsersForSection(project, section);
                            Chapter resultChapter = chapterService.getResultChapterId(section, project);
                            dto.setResultChapterId(resultChapter !=  null ? resultChapter.getId() : null);
                            dto.setAssignedUsers(assignedUsers);
                            return dto;
                        })
                        .sorted(Comparator.comparing(sectionStatusDto -> sectionStatusDto.getSection().getName()))
                        .collect(Collectors.toList());
                model.addAttribute("sectionStatuses", sectionStatuses);
            }else{
                List<SectionAssignment> sectionAssignmentList = sectionAssignmentService.getAssignmentsByProjectAndUser(project, user);
                List<SectionStatusDto> sectionStatuses = sectionAssignmentList.stream()
                        .map(sectionAssignment -> {
                            SectionStatusDto dto = SectionStatusDto.from(sectionAssignment.getSection(),project);
                            List<User> assignedUsers = sectionAssignmentService.getAssignedUsersForSection(project, sectionAssignment.getSection());
                            dto.setAssignedUsers(assignedUsers);
                            return dto;
                        })
                        .sorted(Comparator.comparing(sectionStatusDto -> sectionStatusDto.getSection().getName()))
                        .collect(Collectors.toList());
                model.addAttribute("sectionStatuses", sectionStatuses);
            }
            Long size = chapterService.countChaptersToProject(project);
            String lastUpdate = chapterService.lastUpdateOnProject(project);
            List<User> availableContractors = sectionAssignmentService.getAvailableContractors(UserRole.CONTRACTOR);
            availableContractors.addAll(sectionAssignmentService.getAvailableContractors(UserRole.EMPLOYEE));
            model.addAttribute("project", project);
            model.addAttribute("size", size);
            model.addAttribute("availableContractors", availableContractors);
            model.addAttribute("lastUpdate", lastUpdate);

            return "project";
        }else{
            return "404";
        }
    }
    @PostMapping("/sections/{sectionId}/generate")
    public String generateSection(@PathVariable Long projectId,
                                  @PathVariable Long sectionId,
                                  RedirectAttributes redirectAttributes) {
        Project project = projectService.findById(projectId).orElseThrow();
        Section section = sectionRepository.findById(sectionId).orElseThrow();
        ProjectTablesSample projectTablesSample = projectTableSampleService.getProjectTableSample(project, section);
        if (projectTablesSample == null){
            projectTablesSample = new ProjectTablesSample();
        }
        try {
            Chapter tableGeneral = chapterService.getTableChapter(project, section, Type.TABLE);
            if (tableGeneral == null) {
                redirectAttributes.addFlashAttribute("error", "Отсутствует таблица в шаблонах генерального проекта");
                return "redirect:/projects/" + projectId + "/documents";
            }
            Chapter sampleGeneral = chapterService.getSampleChapter(project, section);
            if (sampleGeneral == null) {
                redirectAttributes.addFlashAttribute("error", "Отсутствует шаблон раздела");
                return "redirect:/projects/" + projectId + "/documents";
            }
            // Файлы главый
            Boolean result = projectService.loadChaptersFromGeneral(project, section);
            if(!result){
                return "redirect:/projects/" + projectId + "/documents?error=Generation+failed";
            }
            //перенос таблицы
            Chapter projectTable = projectService.loadTableChapter(project,section,tableGeneral);
            projectTablesSample.setTableChapter(projectTable);

            //перенос шаблона
            Chapter projectSample = projectService.loadSampleChapter(project,section,sampleGeneral);
            projectTablesSample.setSample(projectSample);
            //перенос штампа
            Chapter projectStamp = projectService.copyStampFile(project, section);
            projectTablesSample.setStamp(projectStamp);
            projectTablesSample.setProject(project);
            projectTablesSample.setSection(section);
            projectTablesSample.setAssignedAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
            System.out.println(projectTableSampleService.save(projectTablesSample).getId());

            return "redirect:/projects/" + projectId + "/documents?success=Section+generated";
        } catch (Exception e) {
            return "redirect:/projects/" + projectId + "/documents?error=Generation+failed";
        }
    }

    @PostMapping("/sections/{sectionId}/assign")
    public String assignUserToSection(@PathVariable Long projectId,
                                      @PathVariable Long sectionId,
                                      @RequestParam Long userId,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser(authentication);
            Project project = projectService.findById(projectId).orElseThrow();

            // Любой, кто является создателем ИЛИ EMPLOYEE ИЛИ ADMIN
            boolean hasAccess = isProjectCreator(projectId, currentUser)
                    || currentUser.getRole().equals(UserRole.EMPLOYEE)
                    || currentUser.getRole().equals(UserRole.ADMIN);

            if (!hasAccess) {
                redirectAttributes.addFlashAttribute("error", "Недостаточно прав для назначения ответственного");
                return "redirect:/projects/" + projectId + "/documents";
            }

            Section section = sectionRepository.findById(sectionId).orElseThrow();


            // Проверяем, что пользователь действительно CONTRACTOR
//            if (!contractor.getRole().equals(UserRole.CONTRACTOR)) {
//                redirectAttributes.addFlashAttribute("error", "Можно назначать только пользователей с ролью CONTRACTOR");
//                return "redirect:/projects/" + projectId + "/documents";
//            }
            //Если пользователь есть удалаяем, если нет добавляем
            User contractor = userService.findById(userId);
            List<User> userList = sectionAssignmentService.getAssignedUsersForSection(project,section);
            boolean exists = userList.stream()
                    .anyMatch(u -> u.getId().equals(contractor.getId()));
            if(exists){
                sectionAssignmentService.removeAssignment(project,section,contractor);
                redirectAttributes.addFlashAttribute("success", "Пользователь успешно удален из раздела");
            }else{
                sectionAssignmentService.assignUserToSection(project, section, contractor);
                redirectAttributes.addFlashAttribute("success", "Пользователь успешно назначен на раздел");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при назначении пользователя: " + e.getMessage());
        }

        return "redirect:/projects/" + projectId + "/documents";
    }

    private boolean isProjectCreator(Long projectId, User user) {
        Project project = projectService.findById(projectId).orElseThrow();
        if(project.getCreatedBy().getId().equals(user.getId())){
            return true;
        }else if (user.hasRole(UserRole.ADMIN)){
            return user.hasRole(UserRole.ADMIN);
        }else{
            return user.hasRole(UserRole.GIP);
        }
    }
}

