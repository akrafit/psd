package com.psd.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChapterWorkStatsDto {
    private Long chapterId;
    private long totalSeconds;
    private int openCount;
    private boolean openedNow;
    private String humanTotal;
}