package com.project.Blog_Management_System.Utils;

import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.Role;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public class AppUtils {

    public static UserEntity getCurrentUser() {
        return (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static boolean hasRole(Role role) {
        return Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getAuthorities().stream().anyMatch(
                authority -> Objects.equals(authority.getAuthority(), "ROLE_" + role.name())
        );
    }

}
