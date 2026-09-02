package com.elotech.taskmanager.task.domain;

import com.elotech.taskmanager.common.exception.BusinessRuleException;
import com.elotech.taskmanager.common.exception.ForbiddenOperationException;
import com.elotech.taskmanager.project.domain.Project;
import com.elotech.taskmanager.project.domain.ProjectRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskStateMachineTest {

    @ParameterizedTest
    @CsvSource({
            "TODO, IN_PROGRESS",
            "IN_PROGRESS, DONE",
            "IN_PROGRESS, TODO",
            "DONE, IN_PROGRESS"
    })
    void permiteTransicoesValidas(TaskStatus origem, TaskStatus destino) {
        Task task = taskWith(origem, TaskPriority.MEDIUM);

        task.changeStatus(destino, ProjectRole.MEMBER);

        assertThat(task.getStatus()).isEqualTo(destino);
    }

    @ParameterizedTest
    @CsvSource({
            "DONE, TODO",
            "TODO, DONE",
            "TODO, TODO",
            "IN_PROGRESS, IN_PROGRESS",
            "DONE, DONE"
    })
    void bloqueiaTransicoesInvalidas(TaskStatus origem, TaskStatus destino) {
        Task task = taskWith(origem, TaskPriority.MEDIUM);

        assertThatThrownBy(() -> task.changeStatus(destino, ProjectRole.ADMIN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Transicao de status invalida");
        assertThat(task.getStatus()).isEqualTo(origem);
    }

    @Test
    void bloqueiaConclusaoDeTarefaCriticalPorMembro() {
        Task task = taskWith(TaskStatus.IN_PROGRESS, TaskPriority.CRITICAL);

        assertThatThrownBy(() -> task.changeStatus(TaskStatus.DONE, ProjectRole.MEMBER))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void permiteConclusaoDeTarefaCriticalPorAdmin() {
        Task task = taskWith(TaskStatus.IN_PROGRESS, TaskPriority.CRITICAL);

        task.changeStatus(TaskStatus.DONE, ProjectRole.ADMIN);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void naoAplicaTravaDeCriticalEmReaberturaDeTarefa() {
        Task task = taskWith(TaskStatus.DONE, TaskPriority.CRITICAL);

        task.changeStatus(TaskStatus.IN_PROGRESS, ProjectRole.MEMBER);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    private Task taskWith(TaskStatus status, TaskPriority priority) {
        Task task = new Task(new Project(), "Tarefa", null, priority, null, null);
        task.setStatus(status);
        return task;
    }
}
