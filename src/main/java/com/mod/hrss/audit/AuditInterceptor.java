package com.mod.hrss.audit;

import com.mod.hrss.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String method = request.getMethod();
        
        // Log state-changing operations only (POST, PUT, PATCH, DELETE)
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) 
                || "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            
            String endpoint = request.getRequestURI();
            String ip = getClientIP(request);
            String username = "ANONYMOUS";
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() 
                    && !authentication.getPrincipal().equals("anonymousUser")) {
                username = authentication.getName();
            }
            
            try {
                auditLogService.log(username, endpoint, method, ip);
                log.info("Audit Logged: User={} Method={} Endpoint={} IP={}", username, method, endpoint, ip);
            } catch (Exception e) {
                log.warn("Failed to write audit log to database: {}", e.getMessage());
            }
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
