package com.magnab.employeelifecycle.repository;

import com.magnab.employeelifecycle.entity.WorkflowTemplate;
import com.magnab.employeelifecycle.enums.WorkflowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {

    Optional<WorkflowTemplate> findByTemplateName(String templateName);

    List<WorkflowTemplate> findByWorkflowType(WorkflowType workflowType);

    List<WorkflowTemplate> findByTemplateNameAndIsActive(String templateName, Boolean isActive);
}
