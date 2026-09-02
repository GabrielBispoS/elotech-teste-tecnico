package com.elotech.taskmanager.project.dto;

import com.elotech.taskmanager.project.domain.Project;
import com.elotech.taskmanager.project.domain.ProjectRole;

import java.util.List;

public record ProjectDetailResponse(Long id, String name, String description, Long ownerId,
                                    ProjectRole myRole, List<MemberResponse> members) {

    public static ProjectDetailResponse from(Project project, ProjectRole myRole, List<MemberResponse> members) {
        return new ProjectDetailResponse(project.getId(), project.getName(), project.getDescription(),
                project.getOwner().getId(), myRole, members);
    }
}
