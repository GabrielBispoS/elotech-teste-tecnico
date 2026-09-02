package com.elotech.taskmanager.task;

import com.elotech.taskmanager.common.security.AuthenticatedUser;
import com.elotech.taskmanager.task.dto.ProjectReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/report")
@RequiredArgsConstructor
public class TaskReportController {

    private final TaskService taskService;

    @GetMapping
    public ProjectReportResponse report(@PathVariable Long projectId,
                                        @AuthenticationPrincipal AuthenticatedUser actor) {
        return taskService.report(projectId, actor.id());
    }
}
