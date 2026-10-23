package com.communex.users.dto;

import com.communex.users.AuthProvider;
import com.communex.users.Role;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        AuthProvider provider,
        String profileImageUrl
) {

}