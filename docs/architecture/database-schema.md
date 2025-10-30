# Database Schema

**Database:** PostgreSQL 17.2 | **Migration Tool:** Liquibase 4.25.1

**11 Core Tables:** users, workflow_templates, template_tasks, template_custom_fields, template_conditional_rules, workflow_instances, task_instances, task_checklist_items, provisioned_items, workflow_state_history, audit_events

**Key Design Decisions:**
1. **UUID Primary Keys** - Globally unique, prevents ID guessing, distributed-safe
2. **PostgreSQL ENUMs** - Type safety at database level (user_role, workflow_status, task_status, etc.)
3. **JSONB Columns** - Flexible data (custom_field_values, checklist_data, metadata, options)
4. **Audit Columns** - created_at, created_by, updated_at, updated_by on all business tables
5. **Soft Deletes** - is_active flag on users and workflow_templates
6. **Cascade Deletes** - ON DELETE CASCADE for template→tasks, workflow→tasks
7. **Foreign Key Constraints** - All relationships enforced at database level
8. **Strategic Indexes** - PKs, FKs, status columns, email, dates, composite indexes

**Liquibase Changelog:** `backend/src/main/resources/db/changelog/db.changelog-master.yaml`

**12 Changesets:**
1. Create enum types (user_role, workflow_status, task_status, etc.)
2. Create users table with BCrypt password_hash
3. Create workflow_templates table
4. Create template_tasks table with self-referential dependencies
5. Create template_custom_fields table with JSONB options
6. Create template_conditional_rules table
7. Create workflow_instances table with JSONB custom_field_values
8. Create task_instances table with JSONB checklist_data
9. Create task_checklist_items table
10. Create provisioned_items table (offboarding mirror)
11. Create workflow_state_history table
12. Create audit_events table with JSONB metadata
13. Seed data (default admin user)

**Connection Pool:** HikariCP with 10-20 connections (Spring Boot default)

**Migration Workflow:**
- Liquibase runs automatically on Spring Boot startup
- Validates schema on each startup
- Applies new changesets incrementally
- Rollback support via `mvn liquibase:rollback`

**PRD Coverage:** All 37 user stories across 5 epics supported by schema design
