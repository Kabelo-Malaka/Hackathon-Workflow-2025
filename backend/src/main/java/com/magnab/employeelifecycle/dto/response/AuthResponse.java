package com.magnab.employeelifecycle.dto.response;

import com.magnab.employeelifecycle.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String username;
    private UserRole role;
    private String message;
}
