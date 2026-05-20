package com.project.Blog_Management_System.Logging;

import com.project.Blog_Management_System.Entities.UserEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(2)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER  = "X-Request-ID";

    private static final String MDC_REQUEST_ID   = "requestId";
    private static final String MDC_USER_ID      = "userId";
    private static final String MDC_USER_EMAIL   = "userEmail";
    private static final String MDC_HTTP_METHOD  = "httpMethod";
    private static final String MDC_REQUEST_URI  = "requestUri";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }

            MDC.put(MDC_REQUEST_ID,  requestId);
            MDC.put(MDC_HTTP_METHOD, request.getMethod());
            MDC.put(MDC_REQUEST_URI, request.getRequestURI());
            populateUserContext();

            response.setHeader(REQUEST_ID_HEADER, requestId);

            long startTime = System.currentTimeMillis();
            log.info("Incoming request from IP={}", resolveClientIp(request));

            filterChain.doFilter(request, response);

            log.info("Completed request status={} duration={}ms",
                    response.getStatus(),
                    System.currentTimeMillis() - startTime);

        } finally {
            MDC.clear();
        }
    }

    private void populateUserContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserEntity user) {
            MDC.put(MDC_USER_ID,    user.getId().toString());
            MDC.put(MDC_USER_EMAIL, user.getEmail());
        } else {
            MDC.put(MDC_USER_ID,    "anonymous");
            MDC.put(MDC_USER_EMAIL, "anonymous");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator/health") || uri.endsWith(".ico");
    }
}