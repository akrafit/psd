package com.psd.service;

import com.psd.entity.Project;
import com.psd.entity.Section;
import com.psd.entity.SectionAssignment;
import com.psd.entity.User;
import com.psd.enums.UserRole;
import com.psd.repo.SectionAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SectionAssignmentService {

    private final SectionAssignmentRepository sectionAssignmentRepository;
    private final UserService userService;

    public SectionAssignmentService(SectionAssignmentRepository sectionAssignmentRepository, UserService userService) {
        this.sectionAssignmentRepository = sectionAssignmentRepository;
        this.userService = userService;
    }

    public SectionAssignment assignUserToSection(Project project, Section section, User user) {
        // Удаляем существующие назначения для этого раздела в проекте
        //sectionAssignmentRepository.deleteByProjectAndSection(project, section);
        Optional<SectionAssignment> lastSectionAssigment = sectionAssignmentRepository.findByProjectAndSectionAndAssignedUser(project,section,user);
        if(lastSectionAssigment.isPresent()){
            return lastSectionAssigment.get();
        }

        // Создаем новое назначение
        SectionAssignment assignment = new SectionAssignment(project, section, user);
        return sectionAssignmentRepository.save(assignment);
    }

    public List<User> getAssignedUsersForSection(Project project, Section section) {
        return sectionAssignmentRepository.findByProjectAndSection(project, section)
                .stream()
                .map(SectionAssignment::getAssignedUser)
                .toList();
    }

    public void removeAssignment(Project project, Section section, User user) {
        sectionAssignmentRepository.findByProjectAndSectionAndAssignedUser(project, section, user)
                .ifPresent(sectionAssignmentRepository::delete);
    }


    public List<User> getAvailableContractors(UserRole role) {
        return userService.getUsersByRole(role);
    }

    public List<SectionAssignment> getAssignmentsForUser(User user) {
        return sectionAssignmentRepository.findSectionAssignmentByAssignedUser(user);
    }
    public List<SectionAssignment> getAssignmentsByProjectAndUser(Project project, User user) {
        return sectionAssignmentRepository.findSectionAssignmentByAssignedUserAndAndProject(user, project);
    }

}