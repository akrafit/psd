package com.psd.dto;

import lombok.Data;

@Data
public class FileItem {
    private String name;
    private String path;
    private String type;
    private String publicUrl; // можно оставить для совместимости
    private String error;
    private Long size;
    private Boolean exists;

    public FileItem() {}

    public FileItem(String error) {
        this.error = error;
    }

    public FileItem(String name, String path, String type) {
        this.name = name;
        this.path = path;
        this.type = type;
    }
}