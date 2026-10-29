package com.tt.tripbook.dto;

import com.tt.tripbook.model.AppUser;
import com.tt.tripbook.model.Role;

// The password hash is deliberately not part of this record.
public record UserResponse(Long id, String name, String email, Role role, String photoUrl) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getPhotoUrl());
    }
}
