package com.psd.service;

import com.psd.entity.*;
import com.psd.enums.Type;
import com.psd.repo.GeneralRepository;
import com.psd.repo.ProjectRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final LocalFileService localFileService;
    private final GeneralRepository generalRepository;
    private final ChapterService chapterService;
    private final SectionAssignmentService sectionAssignmentService;

    public ProjectService(ProjectRepository projectRepository,
                          UserService userService,
                          LocalFileService localFileService,
                          GeneralRepository generalRepository,
                          ChapterService chapterService,
                          SectionAssignmentService sectionAssignmentService) {
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.localFileService = localFileService;
        this.generalRepository = generalRepository;
        this.chapterService = chapterService;
        this.sectionAssignmentService = sectionAssignmentService;

    }
    public Project getProjectById(Long Id){
        return projectRepository.getReferenceById(Id);
    }

    public Optional<Project> findById(Long id) {
        return projectRepository.findById(id);
    }

    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    public List<Project> findByUser(User user) {
        return projectRepository.findByCreatedBy(user);
    }

    public List<Section> getAllSections(Project project) {
        if (project == null || project.getGeneral() == null) {
            return new ArrayList<>();
        }
        Set<Section> sections = new HashSet<>();
        List<Chapter> chapters = chapterService.getChaptersByGeneral(project.getGeneral().getId());

        for (Chapter chapter : chapters) {
            sections.addAll(chapter.getSections());
        }

        return new ArrayList<>(sections);
    }

    public Boolean loadChaptersFromGeneral(Project project, Section section) {
        project.addGeneratedSection(section);
        List<Chapter> chapterList = chapterService.getChaptersByGeneralTemplate(project.getGeneral(), section);
        Boolean result = chapterService.copyFromTemplateToProject(chapterList, project,section);
        if (result){
            projectRepository.save(project);
            return true;
        }
        return false;
    }

    public List<Project> findWhereUserContractor(User user) {
        return sectionAssignmentService.getAssignmentsForUser(user).stream()
                .map(SectionAssignment::getProject)
                .distinct()
                .toList();
    }
    public List<Project> findWhereUserEmployee(User user) {
        var own = findByUser(user); // твой текущий метод (свои проекты)

        var assigned = sectionAssignmentService.getAssignmentsForUser(user).stream()
                .map(SectionAssignment::getProject)
                .toList();

        return java.util.stream.Stream.concat(own.stream(), assigned.stream())
                .distinct()
                .toList();
    }

    public Long countProject() {
        return projectRepository.count();
    }
    /**
     * Новый метод для создания проекта с локальным файловым хранилищем
     */
    public void createProjectLocal(Project project, Authentication authentication, Long generalId) {
        if (generalId != null) {
            General general = generalRepository.findById(generalId)
                    .orElseThrow(() -> new RuntimeException("General not found with id: " + generalId));
            project.setGeneral(general);
        }
        User currentUser = userService.getCurrentUser(authentication);
        project.setCreatedBy(currentUser);
        project.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));

        // Сначала сохраняем проект чтобы получить ID
        projectRepository.save(project);

        try {
            // Создаем папку проекта в локальном хранилище
            localFileService.createProjectFolder(project.getId());
        } catch (Exception e) {
            // Если не удалось создать папку, удаляем проект и пробрасываем исключение
            projectRepository.delete(project);
            throw new RuntimeException("Не удалось создать файловую структуру проекта: " + e.getMessage());
        }
    }

    public Chapter loadTableChapter(Project project, Section section, Chapter tableGeneral) {
        //переносим файл таблицы из главного шаблона
        String newPath = localFileService.copyTemplateTableToProject(tableGeneral.getPath(),project.getId(),section.getId());
        Chapter tableChapter = new Chapter();
        tableChapter.setName(tableGeneral.getName());
        tableChapter.setPath(newPath);
        tableChapter.setTemplate(false);
        tableChapter.setGeneral(project.getGeneral());
        tableChapter.setSrc(newPath);
        tableChapter.setType(String.valueOf(Type.TABLE));
        tableChapter.setCreated(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))));
        tableChapter.setProject(project);
        return chapterService.saveChapter(tableChapter);
    }
    @Transactional
    public void deleteProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // 1. cleanup для обычных глав проекта
        for (Chapter chapter : new ArrayList<>(project.getChapters())) {
            chapterService.cleanupChapterBeforeDelete(chapter);
        }

        // 2. cleanup для служебных глав из project_table
        for (ProjectTablesSample pts : new ArrayList<>(project.getProjectTablesSamples())) {
            if (pts.getTableChapter() != null) {
                chapterService.cleanupChapterBeforeDelete(pts.getTableChapter());
            }
            if (pts.getStamp() != null) {
                chapterService.cleanupChapterBeforeDelete(pts.getStamp());
            }
            if (pts.getSample() != null) {
                chapterService.cleanupChapterBeforeDelete(pts.getSample());
            }
        }


        // 3. само удаление сущностей делает Hibernate каскадом
        projectRepository.delete(project);
        projectRepository.flush();
    }
    public Chapter copyStampFile(Project project, Section section) {

        String tablePath = localFileService.copyStamp(project, section);
        Chapter stamp = new Chapter();
        stamp.setName("stamp.xlsx");
        stamp.setPath(tablePath);
        stamp.setTemplate(false);
        stamp.setGeneral(project.getGeneral());
        stamp.setSrc(tablePath);
        stamp.setType(String.valueOf(Type.STAMP));
        stamp.setCreated(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))));
        stamp.setProject(project);
        // Сохраняем Chapter для шаблона штампа
        return chapterService.saveChapter(stamp);
    }

    public Chapter loadSampleChapter(Project project, Section section, Chapter sampleGeneral) {
        //переносим файл главы
        String newPath = localFileService.copyTemplateSampleToProject(sampleGeneral,project.getId(),section.getId());
        Chapter tableChapter = new Chapter();
        tableChapter.setName(sampleGeneral.getName());
        tableChapter.setPath(newPath);
        tableChapter.setTemplate(false);
        tableChapter.setGeneral(project.getGeneral());
        tableChapter.setSrc(newPath);
        tableChapter.setType(String.valueOf(Type.SAMPLE));
        tableChapter.setCreated(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))));
        tableChapter.setProject(project);
        return chapterService.saveChapter(tableChapter);
    }

}