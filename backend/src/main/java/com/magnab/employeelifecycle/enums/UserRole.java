package com.magnab.employeelifecycle.enums;

/**
 * User role enumeration for role-based access control (RBAC).
 *
 * Roles:
 * - HR_ADMIN: HR administrators who manage user accounts and initiate workflows
 * - LINE_MANAGER: Line managers who oversee employee onboarding tasks
 * - TECH_SUPPORT: Technical support staff who handle provisioning and access
 * - ADMINISTRATOR: System administrators with elevated privileges
 */
public enum UserRole {
    HR_ADMIN,
    LINE_MANAGER,
    TECH_SUPPORT,
    ADMINISTRATOR
}
