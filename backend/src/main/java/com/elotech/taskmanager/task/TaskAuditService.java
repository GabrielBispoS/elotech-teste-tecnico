package com.elotech.taskmanager.task;

import com.elotech.taskmanager.task.domain.Task;
import com.elotech.taskmanager.task.domain.TaskAuditLog;
import com.elotech.taskmanager.task.dto.TaskSnapshot;
import com.elotech.taskmanager.user.UserRepository;
import com.elotech.taskmanager.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Registra, campo a campo, o que mudou em uma tarefa e quem alterou.
 */
@Service
@RequiredArgsConstructor
public class TaskAuditService {

    private final TaskAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void recordChanges(Task task, Long changedById, TaskSnapshot before) {
        TaskSnapshot after = TaskSnapshot.of(task);
        List<TaskAuditLog> entries = new ArrayList<>();
        User changedBy = userRepository.getReferenceById(changedById);

        addIfChanged(entries, task, changedBy, "title", before.title(), after.title());
        addIfChanged(entries, task, changedBy, "description", before.description(), after.description());
        addIfChanged(entries, task, changedBy, "status", before.status(), after.status());
        addIfChanged(entries, task, changedBy, "priority", before.priority(), after.priority());
        addIfChanged(entries, task, changedBy, "deadline", before.deadline(), after.deadline());
        addIfChanged(entries, task, changedBy, "assignee", before.assigneeId(), after.assigneeId());

        if (!entries.isEmpty()) {
            auditLogRepository.saveAll(entries);
        }
    }

    private void addIfChanged(List<TaskAuditLog> entries, Task task, User changedBy, String field,
                              Object oldValue, Object newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }
        entries.add(new TaskAuditLog(task, changedBy, field, asText(oldValue), asText(newValue)));
    }

    private String asText(Object value) {
        return value == null ? null : value.toString();
    }
}
