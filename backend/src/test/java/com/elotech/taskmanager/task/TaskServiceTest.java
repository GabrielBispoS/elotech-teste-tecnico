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
import com.elotech.taskmanager.task.dto.TaskSnapshot;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private TaskAuditLogRepository auditLogRepository;
    @Mock
    private TaskAuditService auditService;
    @Mock
    private ProjectAccessService accessService;
    @Mock
    private ProjectReportCache reportCache;
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
        verify(auditService).recordChanges(eq(task), eq(ACTOR_ID), any(TaskSnapshot.class));
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
    void bloqueiaTrocaDeResponsavelQuandoNovoResponsavelJaAtingiuOLimiteDeWip() {
        Task task = givenTask(TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, assignee);
        User outro = givenMembro(20L);
        when(taskRepository.countByAssigneeIdAndStatus(outro.getId(), TaskStatus.IN_PROGRESS)).thenReturn(5L);

        assertThatThrownBy(() -> taskService.update(PROJECT_ID, TASK_ID,
                new TaskRequest("Tarefa", null, TaskPriority.MEDIUM, null, outro.getId()), ACTOR_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Limite de 5 tarefas");
        assertThat(task.getAssignee()).isEqualTo(assignee);
    }

    @Test
    void naoRevalidaWipQuandoOResponsavelDaTarefaEmAndamentoNaoMuda() {
        Task task = givenTask(TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, assignee);
        givenMembro(ACTOR_ID);

        taskService.update(PROJECT_ID, TASK_ID,
                new TaskRequest("Titulo novo", null, TaskPriority.HIGH, null, ACTOR_ID), ACTOR_ID);

        assertThat(task.getTitle()).isEqualTo("Titulo novo");
        verify(taskRepository, never()).countByAssigneeIdAndStatus(anyLong(), any());
    }

    @Test
    void invalidaORelatorioDoProjetoAoAlterarOStatusDaTarefa() {
        givenTask(TaskStatus.TODO, TaskPriority.LOW, null);
        givenActorRole(ProjectRole.MEMBER);

        taskService.changeStatus(TASK_ID, new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS), ACTOR_ID);

        verify(reportCache).invalidate(PROJECT_ID);
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

    private User givenMembro(Long userId) {
        User user = userId.equals(ACTOR_ID) ? assignee : new User("Bruno", "bruno@elotech.com", "hash");
        user.setId(userId);
        when(membershipRepository.existsByProjectIdAndUserId(PROJECT_ID, userId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        return user;
    }

    private void givenActorRole(ProjectRole role) {
        when(accessService.roleOf(PROJECT_ID, ACTOR_ID)).thenReturn(role);
    }
}
