package com.psd.service.onlyoffice;

import com.psd.dto.onlyoffice.ActiveEditorSessionViewDto;
import com.psd.entity.Chapter;
import com.psd.entity.Project;
import com.psd.entity.Section;
import com.psd.service.ChapterService;
import com.psd.service.ProjectService;
import com.psd.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OnlyOfficeSessionViewService {

    private final DocumentEditorSessionService documentEditorSessionService;
    private final ProjectService projectService;
    private final SectionService sectionService;
    private final ChapterService chapterService;

    public List<ActiveEditorSessionViewDto> getAllActiveSessionsView() {
        return documentEditorSessionService.getAllActiveSessions().stream()
                .map(this::toViewDto)
                .toList();
    }

    public List<ActiveEditorSessionViewDto> getChapterSessionsView(Long chapterId) {
        return documentEditorSessionService.getEditorsByChapter(chapterId).stream()
                .map(this::toViewDto)
                .toList();
    }

    private ActiveEditorSessionViewDto toViewDto(DocumentEditorSession s) {
        String projectName = null;
        String sectionName = null;
        String chapterName = null;

        if (s.getProjectId() != null) {
            Project project = projectService.findById(s.getProjectId()).orElse(null);
            if (project != null) {
                projectName = project.getName();
            }
        }

        if (s.getSectionId() != null) {
            Section section = sectionService.getSectionById(s.getSectionId());
            if (section != null) {
                sectionName = section.getName();
            }
        }

        if (s.getChapterId() != null) {
            Chapter chapter = chapterService.findById(s.getChapterId());
            if (chapter != null) {
                chapterName = chapter.getName();
            }
        }

        return ActiveEditorSessionViewDto.builder()
                .sessionId(s.getSessionId())
                .userId(s.getUserId())
                .username(s.getUsername())
                .projectId(s.getProjectId())
                .projectName(projectName)
                .sectionId(s.getSectionId())
                .sectionName(sectionName)
                .chapterId(s.getChapterId())
                .chapterName(chapterName)
                .documentKey(s.getDocumentKey())
                .openedAt(s.getOpenedAt())
                .lastHeartbeatAt(s.getLastHeartbeatAt())
                .build();
    }
}