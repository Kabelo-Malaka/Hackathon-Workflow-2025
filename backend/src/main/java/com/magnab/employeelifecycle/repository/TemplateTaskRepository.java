package com.magnab.employeelifecycle.repository;

import com.magnab.employeelifecycle.entity.TemplateTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateTaskRepository extends JpaRepository<TemplateTask, UUID> {

    List<TemplateTask> findByTemplateId(UUID templateId);

    List<TemplateTask> findByTemplateIdOrderBySequenceOrder(UUID templateId);
}
