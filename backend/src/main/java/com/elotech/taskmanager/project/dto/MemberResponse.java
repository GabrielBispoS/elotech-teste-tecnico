package com.elotech.taskmanager.project.dto;

import com.elotech.taskmanager.project.domain.ProjectMembership;
import com.elotech.taskmanager.project.domain.ProjectRole;

public record MemberResponse(Long userId, String name, String email, ProjectRole role) {

    public static MemberResponse from(ProjectMembership membership) {
        var user = membership.getUser();
        return new MemberResponse(user.getId(), user.getName(), user.getEmail(), membership.getRole());
    }
}
