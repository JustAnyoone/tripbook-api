package com.tt.tripbook.dto;

import com.tt.tripbook.model.AppUser;

// The password hash is deliberately not part of this record.
public record UserResponse(Long id, String name, String email) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
