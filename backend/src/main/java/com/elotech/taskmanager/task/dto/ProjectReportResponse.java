package com.elotech.taskmanager.task.dto;

import com.elotech.taskmanager.task.domain.TaskPriority;
import com.elotech.taskmanager.task.domain.TaskStatus;

import java.util.Map;

public record ProjectReportResponse(Map<TaskStatus, Long> byStatus, Map<TaskPriority, Long> byPriority) {
}
