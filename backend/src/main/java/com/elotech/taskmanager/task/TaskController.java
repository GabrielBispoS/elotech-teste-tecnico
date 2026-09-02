package com.elotech.taskmanager.task;

import com.elotech.taskmanager.common.dto.PageResponse;
import com.elotech.taskmanager.common.security.AuthenticatedUser;
import com.elotech.taskmanager.task.domain.TaskPriority;
import com.elotech.taskmanager.task.domain.TaskStatus;
import com.elotech.taskmanager.task.dto.TaskFilter;
import com.elotech.taskmanager.task.dto.TaskRequest;
import com.elotech.taskmanager.task.dto.TaskResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public PageResponse<TaskResponse> list(
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long assignee,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        TaskFilter filter = new TaskFilter(status, priority, assignee, from, to);
        return taskService.listByProject(projectId, filter, pageable, actor.id());
    }

    @GetMapping("/search")
    public PageResponse<TaskResponse> search(
            @PathVariable Long projectId,
            @RequestParam @NotBlank @Size(max = 255) String q,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return taskService.search(projectId, q, pageable, actor.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@PathVariable Long projectId,
                               @Valid @RequestBody TaskRequest request,
                               @AuthenticationPrincipal AuthenticatedUser actor) {
        return taskService.create(projectId, request, actor.id());
    }

    @PutMapping("/{taskId}")
    public TaskResponse update(@PathVariable Long projectId, @PathVariable Long taskId,
                               @Valid @RequestBody TaskRequest request,
                               @AuthenticationPrincipal AuthenticatedUser actor) {
        return taskService.update(projectId, taskId, request, actor.id());
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId, @PathVariable Long taskId,
                       @AuthenticationPrincipal AuthenticatedUser actor) {
        taskService.delete(projectId, taskId, actor.id());
    }
}
