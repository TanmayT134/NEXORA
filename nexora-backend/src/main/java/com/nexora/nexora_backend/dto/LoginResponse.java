package com.nexora.nexora_backend.dto;

import com.nexora.nexora_backend.entity.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private Long userId;
    private String fullName;
    private String email;
    private Role role;
}