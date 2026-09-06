package com.elotech.taskmanager.task;

import com.elotech.taskmanager.common.security.AuthenticatedUser;
import com.elotech.taskmanager.task.dto.ProjectReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/report")
@RequiredArgsConstructor
@Tag(name = "Relatorios")
public class TaskReportController {

    private final TaskReportService reportService;

    @Operation(summary = "Resumo do projeto agregado por status e por prioridade")
    @GetMapping
    public ProjectReportResponse report(@PathVariable Long projectId,
                                        @AuthenticationPrincipal AuthenticatedUser actor) {
        return reportService.report(projectId, actor.id());
    }
}
