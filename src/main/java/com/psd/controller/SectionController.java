package com.psd.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psd.dto.ChapterWorkStatsDto;
import com.psd.dto.DocumentDto;

import com.psd.entity.*;
import com.psd.enums.UserRole;
import com.psd.service.*;
import com.psd.service.onlyoffice.DocumentEditorSession;
import com.psd.service.onlyoffice.DocumentEditorSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@Slf4j
@RequestMapping("/project/{projectId}/{sectionId}")
public class SectionController {
    private final ProjectService projectService;
    private final SectionService sectionService;
    private final ChapterService chapterService;
    private final UserService userService;

    private final ProjectTableSampleService projectTableSampleService;


    private final LocalFileService localFileService;
    private final ObjectMapper objectMapper;
    private final DocumentEditorSessionService documentEditorSessionService;

    @Value("${onlyoffice.document-server.api-url}")
    private String apiUrl;

    public SectionController(ProjectService projectService,
                             SectionService sectionService,
                             ChapterService chapterService,
                             UserService userService,

                             ProjectTableSampleService projectTableSampleService,

                             LocalFileService localFileService,

                             ObjectMapper objectMapper,
                             DocumentEditorSessionService documentEditorSessionService) { // Добавляем в конструктор
        this.projectService = projectService;
        this.sectionService = sectionService;
        this.chapterService = chapterService;
        this.userService = userService;
        this.projectTableSampleService = projectTableSampleService;
        this.localFileService = localFileService;

        this.objectMapper = objectMapper;
        this.documentEditorSessionService = documentEditorSessionService;

    }

    @GetMapping
    public String getProjectSectionDocuments(@PathVariable Long projectId,
                                             @PathVariable Long sectionId,
                                             Model model,
                                             Authentication authentication) {
        Project project = projectService.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Section section = sectionService.getSectionById(sectionId);

        List<Chapter> chapterList = chapterService.getChaptersByProject(project, section);
        chapterList.sort(Comparator.comparing(
                Chapter::getName,
                Comparator.nullsLast(String::compareToIgnoreCase)
        ));

        Chapter resultChapter = chapterService.getResultChapterId(section, project);

        ProjectTablesSample projectTablesSample = projectTableSampleService.getProjectTableSample(project, section);

        Chapter tableChapter = null;
        Chapter stampChapter = null;
        Chapter sampleChapter = null;

        if (projectTablesSample != null) {
            tableChapter = projectTablesSample.getTableChapter();
            stampChapter = projectTablesSample.getStamp();
            sampleChapter = projectTablesSample.getSample();
        } else {
            log.warn("ProjectTablesSample not found for projectId={}, sectionId={}", projectId, sectionId);
        }

        List<DocumentDto> documentDtoList = new ArrayList<>();
        for (Chapter chapter : chapterList) {
            documentDtoList.add(new DocumentDto(chapter));
        }

        log.info("Section page: projectId={}, sectionId={}, chaptersCount={}, hasPts={}",
                projectId, sectionId, chapterList.size(), projectTablesSample != null);

        List<Long> chapterIds = new ArrayList<>(chapterList.stream()
                .map(Chapter::getId)
                .toList());
        assert projectTablesSample != null;
        chapterIds.add(projectTablesSample.getStamp().getId());
        chapterIds.add(projectTablesSample.getTableChapter().getId());
//        Map<Long, ChapterWorkStatsDto> chapterWorkStats = chapterWorkStatsService
//                .calculateForChapters(projectId, chapterIds);

//        model.addAttribute("chapterWorkStats", chapterWorkStats);

        model.addAttribute("user", userService.getCurrentUser(authentication));
        model.addAttribute("project", project);
        model.addAttribute("resultChapter", resultChapter);
        model.addAttribute("documentDtoList", documentDtoList);
        model.addAttribute("section", section);
        model.addAttribute("tableChapter", tableChapter);
        model.addAttribute("stampChapter", stampChapter);
        model.addAttribute("sampleChapter", sampleChapter);
//        model.addAttribute("attachments", sectionAttachmentService.list(projectId, sectionId));
//        model.addAttribute("sectionLogs", sectionLogService.getLastLogsForSection(projectId, chapterIds));

        return "section";
    }





    private boolean isDocx(Chapter chapter) {
        // лучше по name (у тебя в БД name с .docx)
        String name = chapter.getName();
        if (name == null) return false;
        return name.toLowerCase().endsWith(".docx");
    }

    @GetMapping("/document/{chapterId}")
    public String openDocument(@PathVariable Long projectId,
                               @PathVariable Long sectionId,
                               @PathVariable Long chapterId,
                               Model model,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        Project project = projectService.findById(projectId).orElseThrow();
        Section section = sectionService.getSectionById(sectionId);
        Chapter chapter = chapterService.findById(chapterId);

        User currentUser = userService.getCurrentUser(authentication);

        // 1) Проверяем, что уже открыто у текущего пользователя
        var currentUserSessionOpt = documentEditorSessionService.findByUserId(currentUser.getId());
        if (currentUserSessionOpt.isPresent()) {
            var currentUserSession = currentUserSessionOpt.get();

            boolean sameDocument =
                    Objects.equals(currentUserSession.projectId(), projectId)
                            && Objects.equals(currentUserSession.sectionId(), sectionId)
                            && Objects.equals(currentUserSession.chapterId(), chapterId);

            if (!sameDocument) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "У вас уже открыт другой документ в OnlyOffice. Закройте его перед открытием нового."
                );
                return "redirect:/project/" + projectId + "/" + sectionId;
            }
        }

        // 2) Смотрим, есть ли уже активные редакторы у главы
        List<com.psd.service.onlyoffice.DocumentEditorSession> activeChapterSessions =
                documentEditorSessionService.getEditorsByChapter(chapterId);

        boolean chapterAlreadyOpened = activeChapterSessions != null && !activeChapterSessions.isEmpty();

        // 3) Preprocess только если это DOCX и глава ещё никем не открыта
//        if (isDocx(chapter) && !chapterAlreadyOpened) {
//            ScanIncludesResponse scan = null;
//
//            try {
//                SyncRun run = syncRunService.start(projectId, sectionId, chapterId, SyncRun.RunType.SCAN_INCLUDES);
//
//                scan = documentProcessorClient.scanIncludes(chapterId);
//                includeRefService.upsertFromScan(chapterId, scan);
//
//                Map<String, Object> stats = new HashMap<>();
//                stats.put("runId", scan.getRunId());
//                if (scan.getCounts() != null) stats.put("counts", scan.getCounts());
//                if (scan.getBroken() != null) stats.put("broken", scan.getBroken());
//                if (scan.getInvalidMarkers() != null) stats.put("invalidMarkersCount", scan.getInvalidMarkers().size());
//
//                syncRunService.finishOk(run.getId(), objectMapper.writeValueAsString(stats));
//
//            } catch (Exception e) {
//                log.warn("scanIncludes failed for chapter {}: {}", chapterId, e.getMessage(), e);
//            }
//
//            boolean hasIncludes = scan != null
//                    && scan.getIncludes() != null
//                    && !scan.getIncludes().isEmpty();
//
//            if (hasIncludes) {
//                try {
//                    SyncRun run = syncRunService.start(projectId, sectionId, chapterId, SyncRun.RunType.MATERIALIZE_INCLUDES);
//
//                    MaterializeIncludesResponse mat = documentProcessorClient.materializeIncludes(chapterId);
//
//                    int updated = (mat != null && mat.getUpdatedBlocks() != null)
//                            ? mat.getUpdatedBlocks()
//                            : 0;
//
//                    if (updated > 0) {
//                        chapterService.bumpVersion(chapterId);
//                        chapter = chapterService.findById(chapterId);
//                    }
//
//                    includeApplyService.markApplied(chapterId);
//
//                    Map<String, Object> stats = new HashMap<>();
//                    if (mat != null) {
//                        stats.put("runId", mat.getRunId());
//                        if (mat.getCounts() != null) stats.put("counts", mat.getCounts());
//                        stats.put("updatedBlocks", updated);
//                        stats.put("skippedCount", mat.getSkipped() == null ? 0 : mat.getSkipped().size());
//                    }
//
//                    syncRunService.finishOk(run.getId(), objectMapper.writeValueAsString(stats));
//
//                } catch (Exception e) {
//                    log.warn("materializeIncludes failed for chapter {}: {}", chapterId, e.getMessage(), e);
//                }
//            } else {
//                log.info("Skip materializeIncludes for chapter {} (no includes found)", chapterId);
//            }
//
//        } else if (isDocx(chapter)) {
//            log.info("Skip preprocessing for chapter {} because it already has active editors", chapterId);
//        } else {
//            log.info("Skip python processing for chapter {} (not DOCX)", chapterId);
//        }

        // 4) Регистрируем сессию ПОСЛЕ возможного bumpVersion
        String documentKey = chapterId + "_v" + chapter.getVersion();

        DocumentEditorSessionService.RegisterResult registerResult =
                documentEditorSessionService.registerSession(
                        currentUser.getId(),
                        currentUser.getEmail(),
                        projectId,
                        sectionId,
                        chapterId,
                        documentKey
                );

        if (!registerResult.success()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "У вас уже открыт другой документ в OnlyOffice. Закройте его перед открытием нового."
            );
            return "redirect:/project/" + projectId + "/" + sectionId;
        }

        model.addAttribute("project", project);
        model.addAttribute("section", section);
        model.addAttribute("chapter", chapter);
        model.addAttribute("api", apiUrl);

        return "document";
    }


    @PostMapping("/status/{chapterId}")
    public ResponseEntity<Response> changeStatus(@PathVariable Long projectId,
                               @PathVariable Long sectionId,
                               @PathVariable Long chapterId,
                               @RequestParam("status") String status,
                                                 Authentication authentication) {
        Chapter chapter = chapterService.getChapterById(chapterId);
        int statusId = Integer.parseInt(status);
        if(chapter != null && statusId < 4){
            Section section = sectionService.getSectionById(sectionId);
            User currentUser = userService.getCurrentUser(authentication);

            Integer oldStatus = chapter.getStatus();
            chapter.setStatus(statusId);
            chapterService.saveChapter(chapter);
            return ResponseEntity.ok(new Response("success"));
        }else{
            return ResponseEntity.status(400).body(new Response("error"));
        }
    }
    public record Response (String massage) {}


}