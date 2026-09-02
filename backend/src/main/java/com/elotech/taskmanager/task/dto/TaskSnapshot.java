package com.elotech.taskmanager.task.dto;

import com.elotech.taskmanager.task.domain.Task;
import com.elotech.taskmanager.task.domain.TaskPriority;
import com.elotech.taskmanager.task.domain.TaskStatus;

import java.time.LocalDate;

/** Estado da tarefa antes da alteracao, usado para calcular o diff do audit log. */
public record TaskSnapshot(String title, String description, TaskStatus status, TaskPriority priority,
                           LocalDate deadline, Long assigneeId) {

    public static TaskSnapshot of(Task task) {
        return new TaskSnapshot(task.getTitle(), task.getDescription(), task.getStatus(), task.getPriority(),
                task.getDeadline(), task.getAssignee() == null ? null : task.getAssignee().getId());
    }
}
