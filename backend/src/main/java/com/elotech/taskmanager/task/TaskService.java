package com.elotech.taskmanager.task;

import com.elotech.taskmanager.common.exception.BusinessRuleException;
import com.elotech.taskmanager.common.exception.ResourceNotFoundException;
import com.elotech.taskmanager.project.ProjectAccessService;
import com.elotech.taskmanager.project.ProjectMembershipRepository;
import com.elotech.taskmanager.project.domain.Project;
import com.elotech.taskmanager.task.domain.Task;
import com.elotech.taskmanager.task.dto.TaskRequest;
import com.elotech.taskmanager.task.dto.TaskResponse;
import com.elotech.taskmanager.user.UserRepository;
import com.elotech.taskmanager.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectAccessService accessService;
    private final ProjectMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TaskResponse> listByProject(Long projectId, Long actorId) {
        accessService.requireMember(projectId, actorId);
        return taskRepository.findByProjectIdOrderByIdDesc(projectId).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    public TaskResponse create(Long projectId, TaskRequest request, Long actorId) {
        Project project = accessService.requireMember(projectId, actorId);
        User assignee = resolveAssignee(projectId, request.assigneeId());

        Task task = new Task(project, request.title(), request.description(), request.priority(),
                request.deadline(), assignee);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long projectId, Long taskId, TaskRequest request, Long actorId) {
        accessService.requireMember(projectId, actorId);
        Task task = findTaskInProject(projectId, taskId);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDeadline(request.deadline());
        task.setAssignee(resolveAssignee(projectId, request.assigneeId()));
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long projectId, Long taskId, Long actorId) {
        accessService.requireMember(projectId, actorId);
        taskRepository.delete(findTaskInProject(projectId, taskId));
    }

    private Task findTaskInProject(Long projectId, Long taskId) {
        return taskRepository.findById(taskId)
                .filter(task -> task.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa nao encontrada: " + taskId));
    }

    private User resolveAssignee(Long projectId, Long assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        // responsavel precisa ser membro do projeto da tarefa
        if (!membershipRepository.existsByProjectIdAndUserId(projectId, assigneeId)) {
            throw new BusinessRuleException("O responsavel precisa ser membro do projeto");
        }
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado: " + assigneeId));
    }
}
