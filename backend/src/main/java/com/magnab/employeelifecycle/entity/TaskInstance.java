package com.magnab.employeelifecycle.entity;

import com.magnab.employeelifecycle.enums.TaskStatus;
import com.magnab.employeelifecycle.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * TaskInstance entity representing individual task executions within workflow instances.
 * Each instance tracks a specific task's assignment, status, and completion data.
 *
 * Features:
 * - UUID primary key for security and distributed systems
 * - References WorkflowInstance (parent workflow)
 * - References TemplateTask (template definition)
 * - User assignment with role-based routing
 * - Status tracking (NOT_STARTED, IN_PROGRESS, BLOCKED, COMPLETED)
 * - Conditional visibility support (is_visible flag)
 * - Due date tracking for SLA management
 * - Checklist data stored as JSONB for partial saves
 * - Complete audit trail with completion tracking
 */
@Entity
@Table(name = "task_instances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskInstance {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "workflow_instance_id", nullable = false)
    private UUID workflowInstanceId;

    @Column(name = "template_task_id", nullable = false)
    private UUID templateTaskId;

    @Column(name = "task_name", nullable = false, length = 255)
    private String taskName;

    @Column(name = "assigned_user_id")
    private UUID assignedUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_role", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private UserRole assignedRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private TaskStatus status = TaskStatus.NOT_STARTED;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by")
    private UUID completedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist_data", columnDefinition = "jsonb")
    private Map<String, Object> checklistData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
