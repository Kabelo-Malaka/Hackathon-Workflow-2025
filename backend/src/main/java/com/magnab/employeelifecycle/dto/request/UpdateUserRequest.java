package com.magnab.employeelifecycle.dto.request;

import com.magnab.employeelifecycle.enums.UserRole;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Email(message = "Email must be valid")
    private String email;

    private UserRole role;

    private Boolean isActive;
}
