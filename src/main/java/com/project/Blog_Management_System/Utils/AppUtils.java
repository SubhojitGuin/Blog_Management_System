package com.project.Blog_Management_System.Utils;

import com.project.Blog_Management_System.Entities.UserEntity;
import com.project.Blog_Management_System.Enums.Role;
import jakarta.servlet.http.Cookie;
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

    public static String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");
    }

    public static Cookie clearAuthCookie() {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        return cookie;
    }

}
