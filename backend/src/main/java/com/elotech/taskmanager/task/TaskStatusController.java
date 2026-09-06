package com.elotech.taskmanager.task;

import com.elotech.taskmanager.common.security.AuthenticatedUser;
import com.elotech.taskmanager.task.dto.TaskResponse;
import com.elotech.taskmanager.task.dto.TaskStatusUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Tarefas")
public class TaskStatusController {

    private final TaskService taskService;

    @Operation(summary = "Transiciona o status da tarefa respeitando a maquina de estados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status alterado"),
            @ApiResponse(responseCode = "403", description = "Nao e membro ou tentou concluir CRITICAL sem ser ADMIN",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Transicao invalida ou WIP limit atingido",
                    content = @Content)
    })
    @PatchMapping("/{taskId}/status")
    public TaskResponse changeStatus(@PathVariable Long taskId,
                                     @Valid @RequestBody TaskStatusUpdateRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser actor) {
        return taskService.changeStatus(taskId, request, actor.id());
    }
}
