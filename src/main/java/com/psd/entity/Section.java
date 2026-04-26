package com.psd.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "section")
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(nullable = false)
    private String name;

    @Column(name = "visible", nullable = false)
    private Boolean visible = true;

    public Section() {}

    public Section(String name) {
        this.name = name;
    }

}