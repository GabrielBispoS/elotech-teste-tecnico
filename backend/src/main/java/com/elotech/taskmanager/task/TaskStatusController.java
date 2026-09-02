package com.elotech.taskmanager.task;

import com.elotech.taskmanager.common.security.AuthenticatedUser;
import com.elotech.taskmanager.task.dto.TaskResponse;
import com.elotech.taskmanager.task.dto.TaskStatusUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskStatusController {

    private final TaskService taskService;

    @PatchMapping("/{taskId}/status")
    public TaskResponse changeStatus(@PathVariable Long taskId,
                                     @Valid @RequestBody TaskStatusUpdateRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser actor) {
        return taskService.changeStatus(taskId, request, actor.id());
    }
}
