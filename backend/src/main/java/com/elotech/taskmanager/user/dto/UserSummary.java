package com.elotech.taskmanager.user.dto;

import com.elotech.taskmanager.user.domain.User;

public record UserSummary(Long id, String name, String email) {

    public static UserSummary from(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getName(), user.getEmail());
    }
}
