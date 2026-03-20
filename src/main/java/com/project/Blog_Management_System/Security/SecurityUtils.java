package com.project.Blog_Management_System.Security;

import jakarta.servlet.http.Cookie;

public class SecurityUtils {

    public static Cookie clearAuthCookie() {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        return cookie;
    }

    public static Cookie getAuthCookie(String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60 * 60 * 24 * 30 * 6); // 6 months
        return cookie;
    }
}
