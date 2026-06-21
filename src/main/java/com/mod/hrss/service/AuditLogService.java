package com.mod.hrss.service;

public interface AuditLogService {
    void log(String username, String endpoint, String requestMethod, String ipAddress);
}
