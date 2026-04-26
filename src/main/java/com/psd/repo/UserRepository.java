package com.psd.repo;

import com.psd.entity.User;
import com.psd.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findUserByRole(UserRole role);

    User findUserById(Long id);
}