package com.psd.dto;

import com.psd.entity.Project;
import com.psd.entity.Section;
import com.psd.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SectionStatusDto {
    private Section section;
    private boolean generated;
    private String statusText;
    private String badgeClass;
    private String buttonText;
    private String buttonIcon;
    private Long resultChapterId;
    private List<User> assignedUsers = new ArrayList<>();

    // Конструкторы, геттеры и сеттеры

    public static SectionStatusDto from(Section section, Project project) {
        SectionStatusDto dto = new SectionStatusDto();
        dto.setSection(section);

        boolean isGenerated = project.getGeneratedSections().contains(section);
        dto.setGenerated(isGenerated);

        if (isGenerated) {
            dto.setStatusText("Сформировано");
            dto.setBadgeClass("status-generated");
            dto.setButtonText("Сформировать повторно");
            dto.setButtonIcon("🔄");
        } else {
            dto.setStatusText("Не сформировано");
            dto.setBadgeClass("status-not-generated");
            dto.setButtonText("Сформировать");
            dto.setButtonIcon("⚡");
        }

        return dto;
    }
}