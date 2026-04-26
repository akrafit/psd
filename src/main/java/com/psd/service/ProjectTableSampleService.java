package com.psd.service;

import com.psd.entity.Chapter;
import com.psd.entity.Project;
import com.psd.entity.ProjectTablesSample;
import com.psd.entity.Section;
import com.psd.repo.ProjectTableRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ProjectTableSampleService {
    private final ProjectTableRepository projectTableRepository;
    private final ChapterService chapterService;

    public ProjectTableSampleService(ProjectTableRepository projectTableRepository, ChapterService chapterService) {
        this.projectTableRepository = projectTableRepository;
        this.chapterService = chapterService;
    }

    public ProjectTablesSample getProjectTableSample(Project project, Section section) {
        return projectTableRepository.findProjectTableByProjectAndSection(project, section);
    }

    public ProjectTablesSample save(ProjectTablesSample projectTablesSample) {
        return projectTableRepository.save(projectTablesSample);
    }

    @Transactional
    public void delete(ProjectTablesSample pts) {

        Chapter table = pts.getTableChapter();
        Chapter stamp = pts.getStamp();
        Chapter sample = pts.getSample();

        // 🔴 КЛЮЧЕВАЯ СТРОКА
        if (pts.getProject() != null) {
            pts.getProject().getProjectTablesSamples().remove(pts);
        }

        projectTableRepository.delete(pts);
        projectTableRepository.flush();

        if (table != null) chapterService.deleteChapter(table);
        if (stamp != null) chapterService.deleteChapter(stamp);
        if (sample != null) chapterService.deleteChapter(sample);
    }
}
