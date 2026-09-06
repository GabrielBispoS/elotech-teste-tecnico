package com.elotech.taskmanager.project;

import com.elotech.taskmanager.common.exception.BusinessRuleException;
import com.elotech.taskmanager.common.exception.ResourceNotFoundException;
import com.elotech.taskmanager.project.domain.Project;
import com.elotech.taskmanager.project.domain.ProjectMembership;
import com.elotech.taskmanager.project.domain.ProjectRole;
import com.elotech.taskmanager.project.dto.AddMemberRequest;
import com.elotech.taskmanager.project.dto.MemberResponse;
import com.elotech.taskmanager.project.dto.ProjectDetailResponse;
import com.elotech.taskmanager.project.dto.ProjectRequest;
import com.elotech.taskmanager.project.dto.ProjectResponse;
import com.elotech.taskmanager.user.UserRepository;
import com.elotech.taskmanager.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final ProjectAccessService accessService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ProjectResponse> listMyProjects(Long actorId) {
        return membershipRepository.findByUserIdOrderByProjectName(actorId).stream()
                .map(membership -> ProjectResponse.from(membership.getProject(), membership.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetailResponse findById(Long projectId, Long actorId) {
        Project project = accessService.requireMember(projectId, actorId);
        List<MemberResponse> members = membershipRepository.findByProjectIdOrderByUserName(projectId).stream()
                .map(MemberResponse::from)
                .toList();
        return ProjectDetailResponse.from(project, accessService.roleOf(projectId, actorId), members);
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request, Long actorId) {
        User owner = findUser(actorId);
        Project project = projectRepository.save(new Project(request.name(), request.description(), owner));
        // o owner sempre entra como ADMIN do proprio projeto
        membershipRepository.save(new ProjectMembership(project, owner, ProjectRole.ADMIN));
        return ProjectResponse.from(project, ProjectRole.ADMIN);
    }

    @Transactional
    public ProjectResponse update(Long projectId, ProjectRequest request, Long actorId) {
        Project project = accessService.requireAdmin(projectId, actorId);
        project.setName(request.name());
        project.setDescription(request.description());
        return ProjectResponse.from(project, ProjectRole.ADMIN);
    }

    @Transactional
    public void delete(Long projectId, Long actorId) {
        Project project = accessService.requireAdmin(projectId, actorId);
        projectRepository.delete(project);
    }

    @Transactional
    public MemberResponse addMember(Long projectId, AddMemberRequest request, Long actorId) {
        Project project = accessService.requireAdmin(projectId, actorId);
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado: " + request.email()));

        if (membershipRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new BusinessRuleException("Usuario ja e membro deste projeto");
        }

        ProjectMembership membership = membershipRepository.save(
                new ProjectMembership(project, user, request.role()));
        return MemberResponse.from(membership);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId, Long actorId) {
        Project project = accessService.requireAdmin(projectId, actorId);

        // sem essa trava o projeto poderia ficar sem nenhum ADMIN
        if (project.getOwner().getId().equals(userId)) {
            throw new BusinessRuleException("O dono do projeto nao pode ser removido");
        }

        ProjectMembership membership = membershipRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado no projeto: " + userId));
        membershipRepository.delete(membership);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado: " + userId));
    }
}
