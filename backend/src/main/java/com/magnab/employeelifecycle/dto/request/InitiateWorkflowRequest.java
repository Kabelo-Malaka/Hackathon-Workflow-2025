package com.magnab.employeelifecycle.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for initiating a new onboarding or offboarding workflow.
 * Contains employee details and custom field values for workflow creation.
 */
public class InitiateWorkflowRequest {

    @NotNull(message = "Template ID is required")
    private UUID templateId;

    @NotBlank(message = "Employee name is required")
    private String employeeName;

    @NotBlank(message = "Employee email is required")
    @Email(message = "Employee email must be valid")
    private String employeeEmail;

    @NotBlank(message = "Employee role is required")
    private String employeeRole;

    private Map<String, Object> customFieldValues;

    // Getters and Setters

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public void setEmployeeEmail(String employeeEmail) {
        this.employeeEmail = employeeEmail;
    }

    public String getEmployeeRole() {
        return employeeRole;
    }

    public void setEmployeeRole(String employeeRole) {
        this.employeeRole = employeeRole;
    }

    public Map<String, Object> getCustomFieldValues() {
        return customFieldValues;
    }

    public void setCustomFieldValues(Map<String, Object> customFieldValues) {
        this.customFieldValues = customFieldValues;
    }
}
