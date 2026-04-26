package com.psd.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Data
@Getter
@Setter
public class Resource {
    private String name;
    private String path;
    private String type;
    private String file;
    private Long size;
    private Date created;
    private Date modified;
    private String mime_type;
    private String preview;
}
