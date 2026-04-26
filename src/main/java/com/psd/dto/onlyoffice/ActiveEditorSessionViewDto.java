package com.psd.dto.onlyoffice;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ActiveEditorSessionViewDto {
    private String sessionId;

    private Long userId;
    private String username;

    private Long projectId;
    private String projectName;

    private Long sectionId;
    private String sectionName;

    private Long chapterId;
    private String chapterName;

    private String documentKey;

    private Instant openedAt;
    private Instant lastHeartbeatAt;
}