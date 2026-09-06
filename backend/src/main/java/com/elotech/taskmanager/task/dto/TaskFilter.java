package com.elotech.taskmanager.task.dto;

import com.elotech.taskmanager.task.domain.TaskPriority;
import com.elotech.taskmanager.task.domain.TaskStatus;

import java.time.LocalDate;

/** Filtros opcionais da listagem; {@code from}/{@code to} delimitam o prazo (deadline). */
public record TaskFilter(TaskStatus status, TaskPriority priority, Long assigneeId,
                         LocalDate from, LocalDate to) {
}
