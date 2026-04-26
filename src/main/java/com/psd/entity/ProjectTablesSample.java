package com.psd.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Getter
@Setter
@Table(name = "project_table")
public class ProjectTablesSample {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "table_id")
    private Chapter tableChapter;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "stamp_id")
    private Chapter stamp;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "sample_id")
    private Chapter sample;

    private LocalDateTime assignedAt;

    // Конструкторы
    public ProjectTablesSample() {}

    public ProjectTablesSample(Project project, Section section) {
        this.project = project;
        this.section = section;
        this.assignedAt = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
    }
}