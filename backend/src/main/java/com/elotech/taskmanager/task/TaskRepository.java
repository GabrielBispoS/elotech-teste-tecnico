package com.elotech.taskmanager.task;

import com.elotech.taskmanager.task.domain.Task;
import com.elotech.taskmanager.task.domain.TaskStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = "assignee")
    List<Task> findByProjectIdOrderByIdDesc(Long projectId);

    long countByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);
}
