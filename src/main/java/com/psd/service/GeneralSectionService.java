package com.psd.service;

import com.psd.entity.Chapter;
import com.psd.entity.General;
import com.psd.entity.GeneralSection;
import com.psd.entity.Section;
import com.psd.enums.Type;
import com.psd.repo.GeneralSectionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class GeneralSectionService {
    private final GeneralSectionRepository generalSectionRepository;

    public GeneralSectionService(GeneralSectionRepository generalSectionRepository) {
        this.generalSectionRepository = generalSectionRepository;
    }

    public void createOrUpdateGeneralSection(General general, Section section, Chapter chapter, Type type) {
        GeneralSection generalSection = new GeneralSection();
        generalSection.setGeneral(general);
        generalSection.setSection(section);
        generalSection.setChapter(chapter);
        generalSection.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        generalSection.setType(String.valueOf(type));
        generalSectionRepository.save(generalSection);
    }

    public GeneralSection findByGeneralIdAndSectionId(General general, Section section, String type) {
        return generalSectionRepository.findGeneralSectionByGeneralAndSectionAndType(general, section, type);
    }

    public void delete(GeneralSection generalSection) {
        generalSectionRepository.delete(generalSection);
    }

    public List<GeneralSection> findByGeneral(General general, String type) {
        return generalSectionRepository.findGeneralSectionByGeneralAndType(general, type);
    }

}
