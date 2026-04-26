package com.psd.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Chapter> chapters = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SectionAssignment> sectionAssignments = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProjectTablesSample> projectTablesSamples = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "general_id")
    private General general; // Шаблон для проекта

    @ManyToMany
    @JoinTable(
            name = "project_generated_sections",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "section_id")
    )
    private Set<Section> generatedSections = new HashSet<>();
    // геттеры и сеттеры
    public Set<Section> getGeneratedSections() { return generatedSections; }
    public void setGeneratedSections(Set<Section> generatedSections) { this.generatedSections = generatedSections; }

    // вспомогательные методы
    public void addGeneratedSection(Section section) {
        this.generatedSections.add(section);
    }

    public void removeGeneratedSection(Section section) {
        this.generatedSections.remove(section);
    }

    public boolean isSectionGenerated(Section section) {
        return this.generatedSections.contains(section);
    }

    // Конструкторы
    public Project() {
    }

    public Project(String name, String description) {
        this.name = name;
        this.description = description;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
    }
}
