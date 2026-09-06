package com.elotech.taskmanager.task.dto;

import com.elotech.taskmanager.task.domain.TaskStatus;

public record TaskCountByStatus(TaskStatus status, Long total) {
}
