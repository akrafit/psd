package com.psd.dto;

import lombok.Data;

import java.util.List;

@Data
public class FileListResponse {
    private List<Resource> items;
    private Long limit;
    private Long offset;
}
