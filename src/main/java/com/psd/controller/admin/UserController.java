package com.psd.controller.admin;

import com.psd.entity.User;
import com.psd.enums.UserRole;
import com.psd.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping({"/admin/users"})
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String getAllUsers(Model model, Authentication authentication) {
        List<User> users = this.userService.getAllUsers();
        model.addAttribute("user", this.userService.getCurrentUser(authentication));
        model.addAttribute("users", users);
        model.addAttribute("userRoles", UserRole.values());
        model.addAttribute("adminCount", this.userService.getUsersByRole(UserRole.ADMIN).size());
        model.addAttribute("employeeCount", this.userService.getUsersByRole(UserRole.EMPLOYEE).size());
        model.addAttribute("contractorCount", this.userService.getUsersByRole(UserRole.CONTRACTOR).size());
        model.addAttribute("userService", this.userService);
        return "admin/users-management";
    }
}