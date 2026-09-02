package com.elotech.taskmanager.task;

import com.elotech.taskmanager.task.domain.Task;
import com.elotech.taskmanager.task.dto.TaskFilter;
import org.springframework.data.jpa.domain.Specification;

final class TaskSpecifications {

    private TaskSpecifications() {
    }

    /** Compoe apenas os predicados dos filtros efetivamente informados. */
    static Specification<Task> build(Long projectId, TaskFilter filter) {
        Specification<Task> spec = (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);

        if (filter.status() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.priority() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), filter.priority()));
        }
        if (filter.assigneeId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assignee").get("id"), filter.assigneeId()));
        }
        if (filter.from() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("deadline"), filter.from()));
        }
        if (filter.to() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("deadline"), filter.to()));
        }
        return spec;
    }
}
