package com.elotech.taskmanager.task.dto;

import com.elotech.taskmanager.task.domain.TaskPriority;

public record TaskCountByPriority(TaskPriority priority, Long total) {
}
