package com.magnab.employeelifecycle.service;

import com.magnab.employeelifecycle.entity.AuditEvent;
import com.magnab.employeelifecycle.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Async
    @Transactional
    public void logAuthenticationAttempt(String username, boolean success, String ipAddress, UUID userId) {
        try {
            AuditEvent event = new AuditEvent();
            event.setUserId(userId);
            event.setActionType(success ? "USER_LOGIN" : "USER_LOGIN_FAILED");
            event.setDescription(success ? "User logged in successfully" : "Failed login attempt");
            event.setMetadata(buildJsonMetadata(username, ipAddress));
            event.setTimestamp(LocalDateTime.now());

            auditEventRepository.save(event);

            if (!success) {
                log.warn("Failed login attempt for username: {}", username);
            } else {
                log.info("User {} logged in successfully", username);
            }
        } catch (Exception e) {
            log.error("Failed to log authentication attempt", e);
        }
    }

    @Async
    @Transactional
    public void logLogout(String username, UUID userId) {
        try {
            AuditEvent event = new AuditEvent();
            event.setUserId(userId);
            event.setActionType("USER_LOGOUT");
            event.setDescription("User logged out");
            event.setMetadata(buildJsonMetadata(username, null));
            event.setTimestamp(LocalDateTime.now());

            auditEventRepository.save(event);
            log.info("User {} logged out", username);
        } catch (Exception e) {
            log.error("Failed to log logout event", e);
        }
    }

    private String buildJsonMetadata(String username, String ipAddress) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"username\":\"").append(escapeJson(username)).append("\"");
        if (ipAddress != null) {
            json.append(",\"ip\":\"").append(escapeJson(ipAddress)).append("\"");
        }
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
