package com.example.demo.Service;

import com.example.demo.Models.AuditLog;
import com.example.demo.Repositories.AuditLogRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String action, String details) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setDetails(details);
        entry.setUsername(currentUsername());
        auditLogRepository.save(entry);
    }

    public java.util.List<AuditLog> recent() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
