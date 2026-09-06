package com.elotech.taskmanager.task.dto;

import com.elotech.taskmanager.task.domain.Task;
import com.elotech.taskmanager.task.domain.TaskPriority;
import com.elotech.taskmanager.task.domain.TaskStatus;
import com.elotech.taskmanager.user.dto.UserSummary;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(Long id, Long projectId, String title, String description,
                           TaskStatus status, TaskPriority priority, LocalDate deadline,
                           UserSummary assignee, Instant createdAt, Instant updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getProject().getId(), task.getTitle(), task.getDescription(),
                task.getStatus(), task.getPriority(), task.getDeadline(), UserSummary.from(task.getAssignee()),
                task.getCreatedAt(), task.getUpdatedAt());
    }
}
