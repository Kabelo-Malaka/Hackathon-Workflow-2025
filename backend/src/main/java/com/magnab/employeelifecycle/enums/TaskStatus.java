package com.magnab.employeelifecycle.enums;

/**
 * Task status enumeration for tracking task instance progress.
 *
 * States:
 * - NOT_STARTED: Task has not been started yet
 * - IN_PROGRESS: Task is actively being worked on
 * - BLOCKED: Task is blocked waiting for dependencies or resolution
 * - COMPLETED: Task has been successfully completed
 */
public enum TaskStatus {
    NOT_STARTED,
    IN_PROGRESS,
    BLOCKED,
    COMPLETED
}
