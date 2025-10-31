package com.magnab.employeelifecycle.entity;

import com.magnab.employeelifecycle.enums.WorkflowStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WorkflowStateHistory entity for audit trail of workflow status changes.
 * Tracks all status transitions with user attribution and optional notes.
 *
 * Features:
 * - UUID primary key for security and distributed systems
 * - References WorkflowInstance (parent workflow)
 * - Captures previous and new status for each transition
 * - User attribution (who made the change)
 * - Timestamp for change tracking
 * - Optional notes field for additional context
 */
@Entity
@Table(name = "workflow_state_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStateHistory {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "workflow_instance_id", nullable = false)
    private UUID workflowInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private WorkflowStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private WorkflowStatus newStatus;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }
    }
}
