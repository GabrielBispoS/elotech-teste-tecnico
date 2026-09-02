package com.elotech.taskmanager.task;

import com.elotech.taskmanager.common.security.AuthenticatedUser;
import com.elotech.taskmanager.task.dto.TaskRequest;
import com.elotech.taskmanager.task.dto.TaskResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public List<TaskResponse> list(@PathVariable Long projectId,
                                   @AuthenticationPrincipal AuthenticatedUser actor) {
        return taskService.listByProject(projectId, actor.id());
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
