package com.psd.dto.onlyoffice;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ActiveEditorSessionDto {
    private String sessionId;
    private Long userId;
    private String username;

    private Long projectId;
    private Long sectionId;
    private Long chapterId;

    private String documentKey;

    private Instant openedAt;
    private Instant lastHeartbeatAt;
}