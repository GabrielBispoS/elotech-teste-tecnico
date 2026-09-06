package com.elotech.taskmanager.task.dto;

import com.elotech.taskmanager.common.validation.DeadlineWindow;
import com.elotech.taskmanager.task.domain.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 4000) String description,
        @NotNull TaskPriority priority,
        @NotNull(message = "o prazo e obrigatorio") @DeadlineWindow LocalDate deadline,
        Long assigneeId) {
}
