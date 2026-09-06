package com.elotech.taskmanager.project.dto;

import com.elotech.taskmanager.project.domain.Project;
import com.elotech.taskmanager.project.domain.ProjectRole;

public record ProjectResponse(Long id, String name, String description, Long ownerId, ProjectRole myRole) {

    public static ProjectResponse from(Project project, ProjectRole myRole) {
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
                project.getOwner().getId(), myRole);
    }
}
