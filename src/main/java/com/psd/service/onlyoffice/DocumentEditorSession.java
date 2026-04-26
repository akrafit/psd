package com.psd.service.onlyoffice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEditorSession {
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