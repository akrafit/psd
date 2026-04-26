package com.psd.service;

import com.psd.entity.User;
import com.psd.enums.UserRole;
import com.psd.repo.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long countUser() {
        return this.userRepository.count();
    }

    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        String email;

        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (principal instanceof String str) {
            email = str;
        } else {
            throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
        }

        return this.userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
    }

    public User findById(Long userId) {
        return this.userRepository.findUserById(userId);
    }

    public List<User> getUsersByRole(UserRole contractor) {
        return this.userRepository.findUserByRole(contractor);
    }

    public List<User> getAllUsers() {
        return this.userRepository.findAll();
    }
}