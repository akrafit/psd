package com.psd.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Getter
@Setter
@Table(name = "section_assignments")
public class SectionAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    private LocalDateTime assignedAt;

    // Конструкторы
    public SectionAssignment() {}

    public SectionAssignment(Project project, Section section, User assignedUser) {
        this.project = project;
        this.section = section;
        this.assignedUser = assignedUser;
        this.assignedAt = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
    }
}