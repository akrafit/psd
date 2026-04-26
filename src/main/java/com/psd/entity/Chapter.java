package com.psd.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "chapter")
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String path;

    @Column(columnDefinition = "TEXT")
    private String created;
    @Column(columnDefinition = "TEXT")
    private String modified;
    private Long size;

    @Column(columnDefinition = "TEXT")
    private String preview;

    @Column(columnDefinition = "TEXT")
    private String resourceId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String src;

    @Column(name = "url", columnDefinition = "TEXT")
    private String publicUrl;

    @Column(name = "template")
    private boolean template;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "general_id")
    private General general;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "version")
    private Integer version = 0;

    private String type;

    @Column(name = "status")
    private Integer status = 0;


    @ManyToMany
    @JoinTable(
            name = "chapter_section",
            joinColumns = @JoinColumn(name = "chapter_id"),
            inverseJoinColumns = @JoinColumn(name = "section_id")
    )
    private List<Section> sections = new ArrayList<>();

    @PreRemove
    private void preRemove() {
        sections.clear();
    }

    public Chapter() {}

    public String getPublicUrl() {
        return publicUrl;
    }

}