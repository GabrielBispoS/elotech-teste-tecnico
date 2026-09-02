package com.elotech.taskmanager.task;

import com.elotech.taskmanager.common.exception.BusinessRuleException;
import com.elotech.taskmanager.common.exception.ForbiddenOperationException;
import com.elotech.taskmanager.project.ProjectAccessService;
import com.elotech.taskmanager.project.ProjectMembershipRepository;
import com.elotech.taskmanager.project.domain.Project;
import com.elotech.taskmanager.project.domain.ProjectRole;
import com.elotech.taskmanager.task.domain.Task;
import com.elotech.taskmanager.task.domain.TaskPriority;
import com.elotech.taskmanager.task.domain.TaskStatus;
import com.elotech.taskmanager.task.dto.TaskRequest;
import com.elotech.taskmanager.task.dto.TaskStatusUpdateRequest;
import com.elotech.taskmanager.user.UserRepository;
import com.elotech.taskmanager.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long ACTOR_ID = 10L;
    private static final Long TASK_ID = 100L;

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectAccessService accessService;
    @Mock
    private ProjectMembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private User assignee;
    private Project project;

    @BeforeEach
    void setUp() {
        assignee = new User("Ana", "ana@elotech.com", "hash");
        assignee.setId(ACTOR_ID);
        project = new Project();
        project.setId(PROJECT_ID);
    }

    @Test
    void permiteIniciarTarefaQuandoResponsavelEstaAbaixoDoLimiteDeWip() {
        Task task = givenTask(TaskStatus.TODO, TaskPriority.MEDIUM, assignee);
        givenActorRole(ProjectRole.MEMBER);
        when(taskRepository.countByAssigneeIdAndStatus(ACTOR_ID, TaskStatus.IN_PROGRESS)).thenReturn(4L);

        taskService.changeStatus(TASK_ID, new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS), ACTOR_ID);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void bloqueiaInicioDeTarefaQuandoResponsavelAtingiuOLimiteDeWip() {
        Task task = givenTask(TaskStatus.TODO, TaskPriority.MEDIUM, assignee);
        givenActorRole(ProjectRole.MEMBER);
        when(taskRepository.countByAssigneeIdAndStatus(ACTOR_ID, TaskStatus.IN_PROGRESS)).thenReturn(5L);

        assertThatThrownBy(() -> taskService.changeStatus(
                TASK_ID, new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS), ACTOR_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Limite de 5 tarefas");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void naoAplicaWipLimitEmTarefaSemResponsavel() {
        Task task = givenTask(TaskStatus.TODO, TaskPriority.MEDIUM, null);
        givenActorRole(ProjectRole.MEMBER);

        taskService.changeStatus(TASK_ID, new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS), ACTOR_ID);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(taskRepository, never()).countByAssigneeIdAndStatus(anyLong(), any());
    }

    @Test
    void bloqueiaConclusaoDeTarefaCriticalPorMembro() {
        Task task = givenTask(TaskStatus.IN_PROGRESS, TaskPriority.CRITICAL, assignee);
        givenActorRole(ProjectRole.MEMBER);

        assertThatThrownBy(() -> taskService.changeStatus(
                TASK_ID, new TaskStatusUpdateRequest(TaskStatus.DONE), ACTOR_ID))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void propagaFalhaDeAutorizacaoQuandoAtorNaoEMembroDoProjeto() {
        givenTask(TaskStatus.TODO, TaskPriority.LOW, assignee);
        when(accessService.roleOf(PROJECT_ID, ACTOR_ID))
                .thenThrow(new ForbiddenOperationException("Usuario nao e membro deste projeto"));

        assertThatThrownBy(() -> taskService.changeStatus(
                TASK_ID, new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS), ACTOR_ID))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void rejeitaResponsavelQueNaoEMembroDoProjeto() {
        when(membershipRepository.existsByProjectIdAndUserId(PROJECT_ID, 99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.create(PROJECT_ID,
                new TaskRequest("Nova", null, TaskPriority.LOW, null, 99L), ACTOR_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("membro do projeto");
    }

    private Task givenTask(TaskStatus status, TaskPriority priority, User taskAssignee) {
        Task task = new Task(project, "Tarefa", null, priority, null, taskAssignee);
        task.setId(TASK_ID);
        task.setStatus(status);
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        return task;
    }

    private void givenActorRole(ProjectRole role) {
        when(accessService.roleOf(PROJECT_ID, ACTOR_ID)).thenReturn(role);
    }
}
