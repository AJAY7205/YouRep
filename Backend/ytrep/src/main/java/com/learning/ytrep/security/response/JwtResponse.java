package com.learning.ytrep.security.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
    private boolean emailVerified;

    public JwtResponse(String accessToken, Long id, String username, String email, List<String> roles, boolean emailVerified) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.emailVerified = emailVerified;
    }
}
