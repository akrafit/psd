package com.psd.repo;

import com.psd.entity.Project;
import com.psd.entity.Section;
import com.psd.entity.SectionAssignment;
import com.psd.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionAssignmentRepository extends JpaRepository<SectionAssignment, Long> {

    List<SectionAssignment> findByProjectAndSection(Project project, Section section);

    Optional<SectionAssignment> findByProjectAndSectionAndAssignedUser(Project project, Section section, User user);

    List<SectionAssignment> findByProject(Project project);

    List<SectionAssignment> findBySection(Section section);

    @Query("SELECT sa FROM SectionAssignment sa WHERE sa.project = :project AND sa.section.id = :sectionId")
    List<SectionAssignment> findByProjectAndSectionId(@Param("project") Project project, @Param("sectionId") Long sectionId);

    void deleteByProjectAndSection(Project project, Section section);

    boolean existsByProjectAndSection(Project project, Section section);
    List<SectionAssignment> findSectionAssignmentByAssignedUser(User user);

    List<SectionAssignment> findSectionAssignmentByAssignedUserAndAndProject(User user, Project project);
}