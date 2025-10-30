package com.magnab.employeelifecycle.enums;

/**
 * Workflow status enumeration for tracking workflow instance progress.
 *
 * States:
 * - INITIATED: Workflow has been created but not yet started
 * - IN_PROGRESS: Workflow is actively being worked on
 * - BLOCKED: Workflow is blocked waiting for dependencies or approvals
 * - COMPLETED: Workflow has been successfully completed
 */
public enum WorkflowStatus {
    INITIATED,
    IN_PROGRESS,
    BLOCKED,
    COMPLETED
}
