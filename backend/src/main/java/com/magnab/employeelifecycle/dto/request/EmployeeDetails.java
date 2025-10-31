package com.magnab.employeelifecycle.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for employee details when creating a workflow instance.
 * Contains basic employee information required for workflow initiation.
 */
@Data
public class EmployeeDetails {

    @NotBlank(message = "Employee name is required")
    private String employeeName;

    @NotBlank(message = "Employee email is required")
    @Email(message = "Employee email must be valid")
    private String employeeEmail;

    @NotBlank(message = "Employee role is required")
    private String employeeRole;
}
