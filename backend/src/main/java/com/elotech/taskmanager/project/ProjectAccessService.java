package com.elotech.taskmanager.project;

import com.elotech.taskmanager.common.exception.ForbiddenOperationException;
import com.elotech.taskmanager.common.exception.ResourceNotFoundException;
import com.elotech.taskmanager.project.domain.Project;
import com.elotech.taskmanager.project.domain.ProjectRole;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Ponto unico de autorizacao por recurso: todo service chama um destes metodos antes de operar.
 */
@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private static final Logger log = LoggerFactory.getLogger(ProjectAccessService.class);

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository membershipRepository;

    public Project requireMember(Long projectId, Long userId) {
        Project project = findProject(projectId);
        roleOf(projectId, userId);
        return project;
    }

    public Project requireAdmin(Long projectId, Long userId) {
        Project project = findProject(projectId);
        if (roleOf(projectId, userId) != ProjectRole.ADMIN) {
            log.warn("Acesso negado: usuario {} nao e ADMIN do projeto {}", userId, projectId);
            throw new ForbiddenOperationException("Apenas administradores do projeto podem executar esta operacao");
        }
        return project;
    }

    public ProjectRole roleOf(Long projectId, Long userId) {
        return membershipRepository.findByProjectIdAndUserId(projectId, userId)
                .map(membership -> membership.getRole())
                .orElseThrow(() -> {
                    log.warn("Acesso negado: usuario {} nao e membro do projeto {}", userId, projectId);
                    return new ForbiddenOperationException("Usuario nao e membro deste projeto");
                });
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto nao encontrado: " + projectId));
    }
}
