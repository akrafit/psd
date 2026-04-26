package com.psd.service;

import com.psd.entity.Section;
import com.psd.repo.SectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;

    public SectionService(SectionRepository sectionRepository) {
        this.sectionRepository = sectionRepository;
    }

    public List<Section> getAllSections() {
        return sectionRepository.findAll();
    }

    public void createSection(Section section) {
        section.setVisible(true);
        sectionRepository.save(section);
    }
    public Section getSectionById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Section not found: " + id));
    }
    public Section updateVisibility(Long sectionId, boolean visible) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Раздел не найден"));
        section.setVisible(visible);
        return sectionRepository.save(section);
    }
    public List<Section> getVisibleSections() {
        return sectionRepository.findByVisibleTrueOrderByNameAsc();
    }
    // Добавляем метод для обновления раздела

}
