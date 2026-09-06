package com.elotech.taskmanager.project.dto;

import com.elotech.taskmanager.project.domain.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
        @NotBlank @Email String email,
        @NotNull ProjectRole role) {
}
