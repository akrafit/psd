package com.psd.repo;

import com.psd.entity.Project;
import com.psd.entity.ProjectTablesSample;
import com.psd.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectTableRepository extends JpaRepository<ProjectTablesSample,Long> {
    ProjectTablesSample findProjectTableByProjectAndSection(Project project, Section section);
    @Modifying
    @Query("""
        delete from ProjectTablesSample p
        where p.tableChapter.id = :chapterId
           or p.stamp.id = :chapterId
           or p.sample.id = :chapterId
        """)
    void deleteByAnyChapterId(@Param("chapterId") Long chapterId);
}
