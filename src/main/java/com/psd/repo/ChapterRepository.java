package com.psd.repo;

import com.psd.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByGeneralId(Long generalId);
    List<Chapter> findByGeneralAndTemplateTrueAndType(General general, String type);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Chapter c JOIN c.sections s WHERE c.id = :chapterId AND s.id = :sectionId")
    boolean existsChapterSectionRelation(@Param("chapterId") Long chapterId,
                                         @Param("sectionId") Long sectionId);

    List<Chapter> findChaptersByProject(Project project);

    @Query("SELECT c FROM Chapter c JOIN c.sections s WHERE c.project = :project AND s.id = :sectionId AND c.type = 'CHAPTER'")
    List<Chapter> findChaptersByProjectAndSection(@Param("project") Project project,
                                                  @Param("sectionId") Long sectionId);
    Optional<Chapter> findByResourceId(String resourceId);

    @Query("SELECT DISTINCT c FROM Chapter c " +
            "JOIN c.sections s " +
            "WHERE c.general = :general AND c.template = true AND s = :section")
    List<Chapter> findByGeneralAndTemplateTrueAndContainingSection(@Param("general") General general,
                                                                   @Param("section") Section section);

    Long countChapterByProject(Project project);

    Optional<Chapter> findByProjectAndName(Project project, String name);

    Chapter findChapterById(Long id);

    Chapter findTopByProjectAndSections_IdAndTypeOrderByIdDesc(Project project, Long sectionId, String type);
    Optional<Chapter> findByProjectIdAndName(Long projectId, String filename);
    Optional<Chapter> findByProjectIdAndNameIgnoreCase(Long projectId, String name);

    @Query("""
    select c
    from Chapter c
    join c.sections s
    where c.project.id = :projectId
      and s.id = :sectionId
      and c.type = 'RESULT'
    order by c.id desc
""")
    List<Chapter> findAllResultChapters(@Param("projectId") Long projectId,
                                        @Param("sectionId") Long sectionId);

    Optional<Chapter> findByGeneralAndTemplateTrueAndNameAndType(General general, String name, String type);



}