package com.elotech.taskmanager.task;

import com.elotech.taskmanager.task.domain.TaskAuditLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAuditLogRepository extends JpaRepository<TaskAuditLog, Long> {

    @EntityGraph(attributePaths = "changedBy")
    List<TaskAuditLog> findByTaskIdOrderByChangedAtDesc(Long taskId);
}
