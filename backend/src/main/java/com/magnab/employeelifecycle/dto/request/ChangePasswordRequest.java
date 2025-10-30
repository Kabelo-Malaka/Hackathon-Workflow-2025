package com.magnab.employeelifecycle.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    // TODO: Add password complexity validation (e.g., @Pattern with regex for min length, uppercase, lowercase, digit)
    private String newPassword;
}
