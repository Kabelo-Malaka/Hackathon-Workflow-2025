package com.magnab.employeelifecycle.dto.request;

import com.magnab.employeelifecycle.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    // TODO: Add password complexity validation (e.g., @Pattern with regex for min length, uppercase, lowercase, digit)
    private String password;

    @NotNull(message = "Role is required")
    private UserRole role;
}
