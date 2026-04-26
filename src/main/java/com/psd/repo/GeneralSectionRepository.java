package com.psd.repo;

import com.psd.entity.General;
import com.psd.entity.GeneralSection;
import com.psd.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneralSectionRepository extends JpaRepository<GeneralSection, Long> {

    GeneralSection findGeneralSectionByGeneralAndSectionAndType(General general, Section section, String type);

    List<GeneralSection> findGeneralSectionByGeneralAndType(General general, String type);

}
