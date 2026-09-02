package com.elotech.taskmanager.task;

import com.elotech.taskmanager.task.domain.Task;
import com.elotech.taskmanager.task.domain.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Override
    @EntityGraph(attributePaths = "assignee")
    Page<Task> findAll(Specification<Task> spec, Pageable pageable);

    long countByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);

    /** Busca textual em titulo/descricao; nativa para usar o ILIKE do PostgreSQL. */
    @Query(value = """
            select * from tasks t
            where t.project_id = :projectId
              and (t.title ilike :pattern or t.description ilike :pattern)
            order by t.id desc
            """,
            countQuery = """
                    select count(*) from tasks t
                    where t.project_id = :projectId
                      and (t.title ilike :pattern or t.description ilike :pattern)
                    """,
            nativeQuery = true)
    Page<Task> search(@Param("projectId") Long projectId, @Param("pattern") String pattern, Pageable pageable);
}
