package com.elotech.taskmanager.task;

import com.elotech.taskmanager.task.domain.TaskPriority;
import com.elotech.taskmanager.task.domain.TaskStatus;
import com.elotech.taskmanager.task.dto.ProjectReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Agregacao do relatorio do projeto, memorizada por projeto.
 *
 * <p>Bean separado de proposito: o cache do Spring atua no proxy, entao a chamada precisa vir de
 * fora. A checagem de acesso fica em {@link TaskReportService} e nunca e cacheada junto.</p>
 */
@Component
@RequiredArgsConstructor
public class ProjectReportCache {

    static final String REPORT_CACHE = "projectReport";

    private final TaskRepository taskRepository;

    /** Agregacao feita no banco com GROUP BY; enums sem tarefas aparecem com zero. */
    @Cacheable(cacheNames = REPORT_CACHE, key = "#projectId")
    @Transactional(readOnly = true)
    public ProjectReportResponse byProject(Long projectId) {
        Map<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
        Stream.of(TaskStatus.values()).forEach(status -> byStatus.put(status, 0L));
        taskRepository.countGroupedByStatus(projectId)
                .forEach(row -> byStatus.put(row.status(), row.total()));

        Map<TaskPriority, Long> byPriority = new EnumMap<>(TaskPriority.class);
        Stream.of(TaskPriority.values()).forEach(priority -> byPriority.put(priority, 0L));
        taskRepository.countGroupedByPriority(projectId)
                .forEach(row -> byPriority.put(row.priority(), row.total()));

        return new ProjectReportResponse(byStatus, byPriority);
    }

    /** Invalidacao explicita: toda escrita de tarefa derruba o relatorio daquele projeto. */
    @CacheEvict(cacheNames = REPORT_CACHE, key = "#projectId")
    public void invalidate(Long projectId) {
        // o efeito e a anotacao; o metodo existe para dar um ponto de chamada explicito
    }
}
