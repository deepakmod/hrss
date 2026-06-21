package com.mod.hrss.service.impl;

import com.mod.hrss.entity.AuditLog;
import com.mod.hrss.repository.AuditLogRepository;
import com.mod.hrss.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String username, String endpoint, String requestMethod, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(username);
        auditLog.setEndpoint(endpoint);
        auditLog.setRequestMethod(requestMethod);
        auditLog.setIpAddress(ipAddress);
        auditLogRepository.save(auditLog);
    }
}
