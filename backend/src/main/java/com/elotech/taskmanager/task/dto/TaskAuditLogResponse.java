package com.elotech.taskmanager.task.dto;

import com.elotech.taskmanager.task.domain.TaskAuditLog;
import com.elotech.taskmanager.user.dto.UserSummary;

import java.time.Instant;

public record TaskAuditLogResponse(Long id, String field, String oldValue, String newValue,
                                   UserSummary changedBy, Instant changedAt) {

    public static TaskAuditLogResponse from(TaskAuditLog log) {
        return new TaskAuditLogResponse(log.getId(), log.getField(), log.getOldValue(), log.getNewValue(),
                UserSummary.from(log.getChangedBy()), log.getChangedAt());
    }
}
