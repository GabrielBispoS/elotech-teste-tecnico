package com.elotech.taskmanager.project;

import com.elotech.taskmanager.project.domain.ProjectMembership;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, Long> {

    Optional<ProjectMembership> findByProjectIdAndUserId(Long projectId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<ProjectMembership> findByProjectIdOrderByUserName(Long projectId);

    @EntityGraph(attributePaths = {"project", "project.owner"})
    List<ProjectMembership> findByUserIdOrderByProjectName(Long userId);

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);
}
