package com.elotech.taskmanager.task.dto;

import com.elotech.taskmanager.task.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequest(@NotNull TaskStatus status) {
}
