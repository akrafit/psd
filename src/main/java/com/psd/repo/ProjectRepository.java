package com.psd.repo;

import com.psd.entity.Project;
import com.psd.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByCreatedBy(User user);
    List<Project> findAllByOrderByCreatedAtDesc();
}
