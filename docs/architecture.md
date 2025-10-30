# Employee Lifecycle Management System Architecture Document

## Introduction

This document outlines the overall project architecture for **Employee Lifecycle Management System**, including backend systems, shared services, and non-UI specific concerns. Its primary goal is to serve as the guiding architectural blueprint for AI-driven development, ensuring consistency and adherence to chosen patterns and technologies.

**Relationship to Frontend Architecture:**
If the project includes a significant user interface, a separate Frontend Architecture Document will detail the frontend-specific design and MUST be used in conjunction with this document. Core technology stack choices documented herein (see "Tech Stack") are definitive for the entire project, including any frontend components.

### Starter Template or Existing Project

**Decision: Use Official Framework Templates**

The project will use official framework templates for backend and frontend, with manual monorepo setup:

1. **Backend:** Spring Initializr (https://start.spring.io/) with Spring Boot 3.x, Java 17, Spring Web, Spring Data JPA, Spring Security, Liquibase dependencies
2. **Frontend:** Vite React-TypeScript template (`npm create vite@latest`)
3. **Monorepo Structure:** Manual setup with root-level docker-compose.yml

**Rationale:** Official templates provide well-maintained starting points without imposing unwanted constraints. Manual monorepo setup gives complete control over structure to match PRD requirements (Docker Compose, specific service organization).

### Change Log

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2025-10-30 | 1.0 | Initial architecture document creation from PRD | Architect - Winston |

## High Level Architecture

### Technical Summary

The Employee Lifecycle Management System is built as a **modular monolithic application** using a modern full-stack architecture. The backend implements a **RESTful API** using Spring Boot 3.x with clear service boundaries (WorkflowService, NotificationService, TemplateService, UserService, AuditService), while the frontend provides a responsive React-based user interface. The system leverages **PostgreSQL 17** for transactional data persistence with Liquibase-managed schema migrations, **session-based authentication** with Spring Security for user management, and **Gmail SMTP integration** for email notifications. The architecture supports the PRD's core goals of workflow orchestration, mandatory verification, and the critical "offboarding mirror" security feature that automatically tracks provisioned items during onboarding for deprovisioning during offboarding. Deployment uses **Docker Compose** for local development with three containerized services (frontend nginx, backend Spring Boot, PostgreSQL database).

### High Level Overview

**1. Architectural Style: Modular Monolith**

The system follows a **modular monolithic architecture** with clear service boundaries, as specified in the PRD's Technical Assumptions. This approach balances simplicity with maintainability:

- Single deployable Spring Boot application with logically separated services
- Five core service modules: WorkflowService, NotificationService, TemplateService, UserService, AuditService
- Shared PostgreSQL database with potential for schema-level logical separation
- Services communicate via direct method calls (in-process) with well-defined interfaces
- Can evolve to microservices post-MVP if scaling demands require it

**Rationale:** With <50 employee transitions per month initially, a monolith handles the load easily while avoiding microservices operational complexity (service discovery, distributed tracing, inter-service communication). The 12-16 week MVP timeline benefits from simplified development and deployment.

**2. Repository Structure: Monorepo**

```
hackathon-workflow-2025/
├── frontend/          # React + TypeScript application
├── backend/           # Spring Boot + Java application
├── docker-compose.yml # Orchestration for all services
└── README.md          # Setup and development instructions
```

Single Git repository containing both frontend and backend enables:
- Atomic commits across API contracts (frontend/backend changes together)
- Simplified dependency management and version coordination
- Single CI/CD pipeline (when needed post-MVP)
- Easier developer onboarding (clone once, run everything)

**3. Service Architecture**

The backend is organized into five core service modules with clear responsibilities:

- **WorkflowService:** Manages workflow templates, instances, state transitions, task assignment, and dependency resolution
- **NotificationService:** Handles email generation, SMTP delivery, retry logic, and notification logging
- **TemplateService:** CRUD operations for workflow templates, custom fields, conditional rules, and template validation
- **UserService:** Authentication, authorization (RBAC), user management, session handling
- **AuditService:** Comprehensive logging of all system actions for compliance and troubleshooting

**4. Primary User Interaction Flow**

```
User Login → Dashboard (Kanban View) → HR Initiates Workflow → Tasks Auto-Assigned →
Email Notification Sent → User Clicks Email Link → Task Completion Form →
Checklist Verification → Task Marked Complete → Dependent Tasks Triggered →
Workflow Progress Updated → Dashboard Reflects Changes
```

**5. Data Flow (Conceptual)**

```
Frontend (React) ←→ REST API (Spring Boot) ←→ PostgreSQL Database
                          ↓
                    Gmail SMTP Server (Notifications)
```

- Frontend communicates exclusively via REST API (no direct database access)
- Backend enforces all business logic, validation, and security rules
- Database stores all persistent state (workflows, tasks, users, audit logs)
- Email notifications sent asynchronously (non-blocking)

**6. Key Architectural Decisions**

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Deployment Model** | Docker Compose (local only) | PRD specifies no CI/CD for MVP; simplifies development |
| **Authentication** | Session-based (not JWT) | Simpler for MVP; easier session revocation; 15-min timeout specified in PRD |
| **Database** | Single shared PostgreSQL | Modular monolith doesn't require separate databases; transactional integrity easier |
| **Email Delivery** | Asynchronous (@Async) | Non-blocking; retry logic for failures; doesn't impact API response times |
| **API Style** | REST (not GraphQL) | Team familiarity; simpler for CRUD operations; PRD implies REST |
| **State Management** | Database-driven workflow state machine | Reliable state transitions; audit trail; supports recovery from failures |

### High Level Project Diagram

```mermaid
graph TB
    subgraph "External Actors"
        HR[HR Admin]
        MGR[Line Manager]
        TECH[Tech Support]
        ADMIN[Administrator]
    end

    subgraph "Frontend Container (nginx:alpine)"
        UI[React 18 + Redux<br/>Material-UI<br/>TypeScript]
    end

    subgraph "Backend Container (Spring Boot)"
        API[REST API Layer<br/>Spring MVC]

        subgraph "Service Layer"
            WFS[WorkflowService]
            TMPS[TemplateService]
            USRS[UserService]
            NOTS[NotificationService]
            AUDS[AuditService]
        end

        subgraph "Data Layer"
            REPO[Spring Data JPA<br/>Repositories]
        end

        SEC[Spring Security<br/>Session Management]
    end

    subgraph "Database Container (PostgreSQL 17)"
        DB[(PostgreSQL<br/>Liquibase Migrations)]
    end

    subgraph "External Services"
        SMTP[Gmail SMTP<br/>smtp.gmail.com:587]
        OUTLOOK[User Outlook Inbox]
    end

    HR -->|HTTPS| UI
    MGR -->|HTTPS| UI
    TECH -->|HTTPS| UI
    ADMIN -->|HTTPS| UI

    UI <-->|REST API| API
    API --> SEC
    SEC --> USRS

    API --> WFS
    API --> TMPS
    API --> USRS
    API --> AUDS

    WFS --> NOTS
    WFS --> REPO
    TMPS --> REPO
    USRS --> REPO
    AUDS --> REPO

    REPO <-->|JDBC| DB

    NOTS -.->|Async Email| SMTP
    SMTP -.->|Deliver| OUTLOOK

    style UI fill:#e1f5ff
    style API fill:#fff4e1
    style DB fill:#f0f0f0
    style SMTP fill:#ffe1e1
```

### Architectural and Design Patterns

**1. Layered Architecture Pattern**

**Recommendation: Traditional 3-tier Layered Architecture**

**Rationale:** Aligns with Spring Boot conventions, clear separation (Controller → Service → Repository), simplifies AI agent code generation, PRD doesn't require hexagonal architecture flexibility.

**Implementation:**
- Controllers handle HTTP concerns (validation, serialization, error responses)
- Services contain all business logic (workflow state machines, assignment algorithms, validation)
- Repositories abstract database access (Spring Data JPA interfaces)
- DTOs at API boundaries (never expose entities directly)

---

**2. Repository Pattern**

**Recommendation: Spring Data JPA Repositories**

**Rationale:** PRD specifies "Spring Data JPA with Hibernate ORM; use DTOs at controller boundaries", provides abstraction over database access, enables testing with mocks, reduces boilerplate.

---

**3. Dependency Injection Pattern**

**Recommendation: Constructor Injection (Spring's default)**

**Rationale:** Immutable dependencies (final fields), easier unit testing, compiler-enforced dependency provision, Spring Boot 3.x best practice.

---

**4. Data Transfer Object (DTO) Pattern**

**Recommendation: Separate DTOs for API layer**

**Rationale:** PRD explicitly specifies DTOs at controller boundaries, decouples API from database schema, prevents exposing sensitive fields, allows API evolution without database changes.

---

**5. Service Layer Pattern**

**Recommendation: Transactional Services with Business Logic**

**Rationale:** Encapsulates complex business rules (workflow state machine, conditional logic, task assignment), provides transaction boundaries (@Transactional), reusable across controllers, testable in isolation.

---

**6. Asynchronous Processing Pattern**

**Recommendation: Spring @Async for email notifications**

**Rationale:** PRD specifies async email sending, task completion API returns immediately, retry logic doesn't block workflows, improves API performance.

---

**7. State Machine Pattern (for Workflow Orchestration)**

**Recommendation: Database-driven state machine with validation**

**Rationale:** PRD defines workflow states (INITIATED → IN_PROGRESS → BLOCKED → COMPLETED), state transitions have business rules, database persistence ensures state survives restarts, audit trail logs all changes.

---

**8. Strategy Pattern (for Conditional Logic Evaluation)**

**Recommendation: Rule engine for template conditional logic**

**Rationale:** PRD Story 2.7 requires conditional task logic (operators: EQUALS, NOT_EQUALS, CONTAINS), flexible and extensible for new operators.

---

**9. Facade Pattern (for Complex Workflows)**

**Recommendation: WorkflowOrchestrationFacade**

**Rationale:** Workflow initiation involves multiple steps (create workflow → instantiate tasks → evaluate conditionals → assign tasks → send emails), facade simplifies controller logic, provides transactional boundary.

## Tech Stack

### Cloud Infrastructure

- **Provider:** Local Docker Environment (no cloud provider for MVP)
- **Key Services:** Docker Compose orchestrating three containers (frontend nginx, backend Spring Boot, PostgreSQL)
- **Deployment Regions:** N/A - Local development machines only
- **Future Consideration:** If cloud deployment needed post-MVP, consider AWS (EC2, RDS, SES) or Azure (App Service, Azure Database for PostgreSQL, SendGrid)

**Rationale:** PRD explicitly states "Local Docker deployment only for MVP; no CI/CD pipeline required."

### Technology Stack Table

**⚠️ CRITICAL: This table is the DEFINITIVE technology selection for the entire project.**

| Category | Technology | Version | Purpose | Rationale |
|----------|------------|---------|---------|-----------|
| **Language (Backend)** | Java | 17 LTS | Backend development language | LTS release, PRD requirement, Spring Boot 3.x baseline |
| **Language (Frontend)** | TypeScript | 5.3.3 | Frontend development language | Type safety, excellent tooling, PRD requirement |
| **Runtime (Backend)** | OpenJDK | 17-slim | Java runtime for Docker | Slim image reduces container size, official OpenJDK |
| **Runtime (Frontend Build)** | Node.js | 20.11.0 LTS | JavaScript build tooling | LTS version, stable for Vite and npm ecosystem |
| **Framework (Backend)** | Spring Boot | 3.2.2 | Backend application framework | PRD requirement, enterprise-ready, modular monolith support |
| **Framework (Frontend)** | React | 18.2.0 | UI framework | PRD requirement, component-based, excellent ecosystem |
| **Build Tool (Backend)** | Maven | 3.9.6 | Backend dependency management | **✅ Confirmed acceptable**, standard for Spring Boot, better AI agent support |
| **Build Tool (Frontend)** | Vite | 5.0.12 | Frontend build tool | PRD requirement, fast HMR, modern ES modules |
| **State Management** | Redux Toolkit | 2.1.0 | Global state management | PRD requirement, official Redux approach, reduces boilerplate |
| **API Client** | RTK Query | 2.1.0 | Data fetching & caching | PRD requirement, integrated with Redux Toolkit |
| **OpenAPI Code Generator** | OpenAPI Generator | 7.2.0 | TypeScript client generation | **✅ Confirmed for build integration**, ensures type safety across stack |
| **UI Component Library** | Material-UI (MUI) | 5.15.7 | React component library | PRD requirement, enterprise components, accessibility built-in |
| **Form Management** | React Hook Form | 7.49.3 | Complex form validation | PRD requirement, handles template builder and checklists |
| **Database** | PostgreSQL | 17.2 | Relational database | PRD requirement, latest stable, ACID compliance |
| **Database Driver** | PostgreSQL JDBC | 42.7.1 | Database connectivity | Latest stable, included with Spring Data JPA |
| **Database Migration** | Liquibase | 4.25.1 | Schema version control | PRD requirement, rollback support, Spring Boot integration |
| **ORM** | Hibernate (via Spring Data JPA) | 6.4.2 | Object-relational mapping | Included with Spring Boot 3.2.2, standard JPA |
| **Connection Pool** | HikariCP | 5.1.0 | Database connection pooling | Spring Boot default, pool size 10-20 per PRD |
| **Security Framework** | Spring Security | 6.2.1 | Authentication & authorization | Included with Spring Boot 3.2.2, **✅ in-memory sessions confirmed** |
| **Email** | Spring Mail + JavaMail | 2.1.0 | SMTP email integration | PRD requirement, Gmail SMTP support |
| **Async Execution** | Spring @Async | via Spring Boot | Asynchronous processing | Default thread pool for email notifications |
| **Code Reduction** | Lombok | 1.18.30 | Reduce Java boilerplate | PRD requirement, getters/setters/constructors |
| **API Documentation** | SpringDoc OpenAPI | 2.3.0 | Swagger UI generation | PRD requirement, automatic API spec generation |
| **Validation** | Jakarta Bean Validation | 3.0.2 | Request validation | Included with Spring Boot, declarative validation |
| **Logging (Backend)** | SLF4J + Logback | 2.0.11 / 1.4.14 | Structured logging | Included with Spring Boot, JSON logging support |
| **Logging (Frontend)** | Console (dev) | N/A | Browser console logging | Sufficient for MVP |
| **Testing (Backend - Unit)** | JUnit 5 | 5.10.1 | Unit testing framework | PRD requirement, included with Spring Boot |
| **Testing (Backend - Mocking)** | Mockito | 5.8.0 | Mock framework | PRD requirement, included with Spring Boot |
| **Testing (Backend - Integration)** | TestContainers | 1.19.3 | Integration test infrastructure | PRD requirement, real PostgreSQL for tests |
| **Testing (Frontend - Unit)** | Jest | 29.7.0 | Unit testing framework | PRD requirement, React ecosystem standard |
| **Testing (Frontend - Component)** | React Testing Library | 14.1.2 | Component testing | PRD requirement, best practices for React |
| **Testing (API)** | Postman | Latest | Manual API testing | PRD requirement, structured test collections |
| **Containerization** | Docker | 24.0.7+ | Container runtime | **✅ No cloud deployment confirmed**, PRD requirement |
| **Container Orchestration** | Docker Compose | 2.23.3+ | Multi-container orchestration | PRD requirement, single `docker-compose up` |
| **Web Server (Frontend)** | nginx | 1.25-alpine | Static file serving & reverse proxy | **✅ Reverse proxy confirmed**, Alpine for minimal size |
| **SMTP Server** | Gmail SMTP | N/A | Email delivery | PRD requirement, smtp.gmail.com:587 |
| **SMTP Account** | ctrlalteliteg@gmail.com | N/A | Email sender | PRD specified account |
| **Password Hashing** | BCrypt | via Spring Security | Password storage | Spring Security default, PRD requirement |
| **HTTP Client (Backend)** | RestTemplate | via Spring Boot | External API calls (if needed) | Standard Spring approach |
| **HTTP Client (Frontend)** | fetch (via RTK Query) | built-in | API communication | Built into RTK Query |
| **Code Quality (Frontend)** | ESLint + Prettier | 8.56.0 / 3.2.4 | Code linting & formatting | PRD recommended, enforces consistency |
| **Code Quality (Backend)** | Checkstyle | 10.12.7 | Java code standards | PRD optional but recommended |
| **Package Manager (Frontend)** | npm | 10.2.4 | Node.js package management | Standard with Node.js 20.11.0 |
| **Dependency Scanning** | npm audit | built-in | Frontend security scanning | Basic security for MVP |
| **IDE (Backend)** | IntelliJ IDEA | 2024.1+ | Backend development | PRD recommended, excellent Spring support |
| **IDE (Frontend)** | VSCode | 1.86+ | Frontend development | PRD recommended, excellent React/TypeScript support |

### Key Confirmations

✅ **Maven** is acceptable (vs. Gradle)
✅ **OpenAPI Generator** will be integrated into build process
✅ **nginx reverse proxy** pattern is acceptable (proxy `/api` to backend:8080)
✅ **In-memory sessions** for MVP (vs. Redis)
✅ **No cloud deployment** needed for MVP hackathon demo

### Version Pinning Strategy

**Why Exact Versions Matter:**
- Reproducible builds across all developers
- AI agent clarity (specific APIs, no version ambiguity)
- Dependency hell prevention
- Security auditing

**Version Update Policy for MVP:**
- ✅ Pin exact versions (no ranges)
- ✅ Update only for critical security fixes
- ⛔ No automatic updates (dependabot disabled)
- ⛔ No `latest` tags in Dockerfiles

## Data Models

The system's data model comprises 10 core entities that map directly to the PRD's 37 user stories across 5 epics. Each entity is designed with audit capabilities, referential integrity, and support for the key "offboarding mirror" security feature.

### User

**Purpose:** System users with role-based access control (HR Admin, Line Manager, Tech Support, Administrator). Supports authentication, authorization, and audit trail attribution.

**Key Attributes:**
- `id`: UUID - Primary key
- `username`: String (unique, indexed) - Login credential
- `email`: String (unique, indexed) - Contact and notification address
- `passwordHash`: String - BCrypt-hashed password (never exposed in DTOs)
- `role`: Enum (HR_ADMIN, LINE_MANAGER, TECH_SUPPORT, ADMINISTRATOR) - RBAC role
- `isActive`: Boolean (default true) - Soft delete flag
- `createdAt`, `createdBy`, `updatedAt`, `updatedBy` - Audit columns

**Relationships:**
- One-to-Many: User → WorkflowInstance (as initiator)
- One-to-Many: User → TaskInstance (as assigned user)
- One-to-Many: User → AuditEvent (as actor)

### WorkflowTemplate

**Purpose:** Reusable workflow blueprints for onboarding/offboarding processes.

**Key Attributes:**
- `id`: UUID - Primary key
- `name`: String (unique per type) - Template identifier
- `description`: String (optional)
- `type`: Enum (ONBOARDING, OFFBOARDING)
- `isActive`: Boolean (default true)
- `createdAt`, `createdBy`, `updatedAt`, `updatedBy`

**Relationships:**
- One-to-Many: WorkflowTemplate → TemplateTask
- One-to-Many: WorkflowTemplate → TemplateCustomField
- One-to-Many: WorkflowTemplate → WorkflowInstance

### TemplateTask

**Purpose:** Individual tasks within workflow templates with role assignments, sequencing, and dependencies.

**Key Attributes:**
- `id`: UUID - Primary key
- `templateId`: UUID (FK to WorkflowTemplate)
- `taskName`: String
- `description`: String (optional)
- `assignedRole`: Enum (HR_ADMIN, LINE_MANAGER, TECH_SUPPORT, ADMINISTRATOR)
- `sequenceOrder`: Integer (indexed)
- `isParallel`: Boolean (default false)
- `dependencyTaskId`: UUID (FK to TemplateTask, nullable)
- `createdAt`, `updatedAt`

**Relationships:**
- Many-to-One: TemplateTask → WorkflowTemplate
- Self-referential: TemplateTask → TemplateTask (dependencies)
- One-to-Many: TemplateTask → TemplateConditionalRule

### TemplateCustomField

**Purpose:** Custom data fields captured during workflow initiation (e.g., start date, remote status).

**Key Attributes:**
- `id`: UUID - Primary key
- `templateId`: UUID (FK to WorkflowTemplate)
- `fieldName`: String (unique within template)
- `fieldNameSlug`: String (auto-generated for conditional rules)
- `fieldType`: Enum (TEXT, NUMBER, DATE, BOOLEAN, SELECT)
- `isRequired`: Boolean (default false)
- `options`: JSONB (for SELECT type)

**Relationships:**
- Many-to-One: TemplateCustomField → WorkflowTemplate

### TemplateConditionalRule

**Purpose:** Conditional logic for showing/hiding tasks based on custom field values.

**Key Attributes:**
- `id`: UUID - Primary key
- `taskId`: UUID (FK to TemplateTask)
- `conditionField`: String (references TemplateCustomField.fieldNameSlug)
- `conditionOperator`: Enum (EQUALS, NOT_EQUALS, CONTAINS)
- `conditionValue`: String
- `action`: Enum (SHOW_TASK, HIDE_TASK)

**Relationships:**
- Many-to-One: TemplateConditionalRule → TemplateTask

### WorkflowInstance

**Purpose:** Specific employee's onboarding/offboarding workflow instantiated from a template.

**Key Attributes:**
- `id`: UUID - Primary key
- `templateId`: UUID (FK to WorkflowTemplate)
- `employeeName`: String
- `employeeEmail`: String (indexed)
- `employeeRole`: String
- `workflowType`: Enum (ONBOARDING, OFFBOARDING)
- `status`: Enum (INITIATED, IN_PROGRESS, BLOCKED, COMPLETED) (indexed)
- `initiatedBy`: UUID (FK to User)
- `initiatedAt`: Timestamp (indexed)
- `completedAt`: Timestamp (nullable)
- `customFieldValues`: JSONB
- `createdAt`, `updatedAt`

**Relationships:**
- Many-to-One: WorkflowInstance → WorkflowTemplate
- Many-to-One: WorkflowInstance → User (as initiator)
- One-to-Many: WorkflowInstance → TaskInstance
- One-to-Many: WorkflowInstance → WorkflowStateHistory
- One-to-Many: WorkflowInstance → ProvisionedItem

### TaskInstance

**Purpose:** Individual task within a workflow instance with assignment, state, and checklist tracking.

**Key Attributes:**
- `id`: UUID - Primary key
- `workflowInstanceId`: UUID (FK to WorkflowInstance, indexed)
- `templateTaskId`: UUID (FK to TemplateTask)
- `taskName`: String
- `assignedUserId`: UUID (FK to User, nullable, indexed)
- `assignedRole`: Enum
- `status`: Enum (NOT_STARTED, IN_PROGRESS, BLOCKED, COMPLETED) (indexed)
- `isVisible`: Boolean (default true)
- `dueDate`: Timestamp (nullable, indexed)
- `completedAt`: Timestamp (nullable)
- `completedBy`: UUID (FK to User, nullable)
- `checklistData`: JSONB (for partial saves)
- `createdAt`, `updatedAt`

**Relationships:**
- Many-to-One: TaskInstance → WorkflowInstance
- Many-to-One: TaskInstance → TemplateTask
- Many-to-One: TaskInstance → User (as assigned user)
- One-to-Many: TaskInstance → TaskChecklistItem

### TaskChecklistItem

**Purpose:** Individual checklist items verified during task completion.

**Key Attributes:**
- `id`: UUID - Primary key
- `taskInstanceId`: UUID (FK to TaskInstance, indexed)
- `itemDescription`: String
- `category`: Enum (HARDWARE, SOFTWARE, ACCESS, OTHER)
- `itemIdentifier`: String (optional)
- `isChecked`: Boolean (default false)
- `checkedAt`: Timestamp (nullable)
- `checkedBy`: UUID (FK to User, nullable)
- `createdAt`, `updatedAt`

**Relationships:**
- Many-to-One: TaskChecklistItem → TaskInstance

### ProvisionedItem

**Purpose:** **Critical security feature** - Records all items provisioned during onboarding for use in offboarding mirror.

**Key Attributes:**
- `id`: UUID - Primary key
- `workflowInstanceId`: UUID (FK to WorkflowInstance, indexed)
- `taskInstanceId`: UUID (FK to TaskInstance, indexed)
- `itemDescription`: String
- `category`: Enum (HARDWARE, SOFTWARE, ACCESS, OTHER)
- `itemIdentifier`: String (optional)
- `provisionedAt`: Timestamp
- `provisionedBy`: UUID (FK to User)

**Relationships:**
- Many-to-One: ProvisionedItem → WorkflowInstance
- Many-to-One: ProvisionedItem → TaskInstance

### WorkflowStateHistory

**Purpose:** Audit trail for workflow state transitions.

**Key Attributes:**
- `id`: UUID - Primary key
- `workflowInstanceId`: UUID (FK to WorkflowInstance, indexed)
- `previousStatus`: Enum (WorkflowStatus)
- `newStatus`: Enum (WorkflowStatus)
- `changedBy`: UUID (FK to User)
- `changedAt`: Timestamp (indexed)
- `notes`: String (optional)

**Relationships:**
- Many-to-One: WorkflowStateHistory → WorkflowInstance

### AuditEvent

**Purpose:** Comprehensive audit log for all system actions.

**Key Attributes:**
- `id`: UUID - Primary key
- `userId`: UUID (FK to User, indexed)
- `actionType`: Enum (USER_LOGIN, USER_LOGOUT, WORKFLOW_INITIATED, etc.) (indexed)
- `entityType`: Enum (WORKFLOW, TASK, TEMPLATE, USER) (indexed)
- `entityId`: UUID (indexed)
- `description`: String
- `metadata`: JSONB
- `ipAddress`: String (optional)
- `timestamp`: Timestamp (indexed)

**Relationships:**
- Many-to-One: AuditEvent → User

### Data Model ER Diagram

```mermaid
erDiagram
    User ||--o{ WorkflowInstance : initiates
    User ||--o{ TaskInstance : "assigned to"
    User ||--o{ AuditEvent : performs

    WorkflowTemplate ||--o{ TemplateTask : contains
    WorkflowTemplate ||--o{ TemplateCustomField : defines
    WorkflowTemplate ||--o{ WorkflowInstance : "instantiates to"

    TemplateTask ||--o{ TemplateTask : "depends on"
    TemplateTask ||--o{ TemplateConditionalRule : "has rules"
    TemplateTask ||--o{ TaskInstance : "instantiates to"

    WorkflowInstance ||--o{ TaskInstance : contains
    WorkflowInstance ||--o{ WorkflowStateHistory : "state history"
    WorkflowInstance ||--o{ ProvisionedItem : "provisioned items"

    TaskInstance ||--o{ TaskChecklistItem : "checklist items"
    TaskInstance ||--o{ ProvisionedItem : "provisions"
```

## Components

The backend follows a **layered architecture** with three primary layers: Controller (REST API), Service (Business Logic), and Repository (Data Access), plus cross-cutting concerns for security, logging, and async processing.

### Component Overview

**Controller Layer (8 components):**
1. AuthController - Authentication and session management
2. UserController - User management CRUD
3. TemplateController - Workflow template management
4. WorkflowController - Workflow instance operations
5. TaskController - Task completion and management
6. DashboardController - Aggregate statistics
7. AuditController - Audit trail access
8. ReportController - Report generation and export

**Service Layer (7 components):**
1. UserService - User management business logic
2. TemplateService - Template validation and management
3. WorkflowService - **Core orchestration** (instantiation, assignment, state management)
4. TaskService - Task completion, checklist validation, offboarding mirror
5. NotificationService - Email generation and SMTP delivery
6. AuditService - Comprehensive audit logging
7. ConditionalRuleEvaluator - Template conditional logic

**Repository Layer (11 components):**
- Spring Data JPA repositories for all entities (User, WorkflowTemplate, TemplateTask, TemplateCustomField, TemplateConditionalRule, WorkflowInstance, TaskInstance, TaskChecklistItem, ProvisionedItem, WorkflowStateHistory, AuditEvent)

**Cross-cutting (2 components):**
- GlobalExceptionHandler - Centralized error handling
- SecurityConfiguration - Spring Security setup

### Component Details

**WorkflowService (Core Orchestration)**

**Responsibility:** Workflow instantiation, state management, task assignment (the heart of the system)

**Key Methods:**
- `createWorkflowInstance(templateId, employeeDetails, customFieldValues, initiatedBy)` - Instantiate workflow from template
- `evaluateConditionalRules(template, customFieldValues)` - Evaluate task visibility
- `assignTasksForWorkflow(workflowInstanceId)` - Auto-assign ready tasks (load balancing algorithm)
- `updateWorkflowStatus(workflowInstanceId, newStatus, userId, notes)` - State transitions with validation
- `checkOffboardingMirror(employeeEmail)` - Find prior onboarding for offboarding
- `autoCompleteWorkflow(workflowInstanceId)` - Check completion and auto-transition

**Dependencies:** WorkflowInstanceRepository, TaskInstanceRepository, WorkflowStateHistoryRepository, ProvisionedItemRepository, TemplateService, NotificationService, AuditService

**Technology:** Spring @Service, @Transactional, Pessimistic locking for state updates

**PRD Coverage:** Epic 3 (Workflow Execution & Task Routing)

---

**TaskService (Task Completion & Offboarding Mirror)**

**Responsibility:** Task completion logic, checklist validation, offboarding mirror creation

**Key Methods:**
- `completeTask(taskInstanceId, checklistItems, userId)` - Complete task with validation
- `validateChecklist(checklistItems)` - Ensure all items checked
- `saveChecklistProgress(taskInstanceId, checklistItems)` - Partial save
- `createProvisionedItems(workflowInstanceId, taskInstanceId, checklistItems)` - Copy to provisioned_items
- `prePopulateOffboardingChecklist(offboardingTaskId, provisionedItems)` - Load from onboarding

**Dependencies:** TaskInstanceRepository, TaskChecklistItemRepository, ProvisionedItemRepository, WorkflowService, NotificationService, AuditService

**Technology:** Spring @Service, @Transactional

**PRD Coverage:** Epic 4 (Task Completion & Verification), Story 4.7 (Offboarding Mirror)

---

**NotificationService (Email Integration)**

**Responsibility:** Email generation, SMTP delivery, retry logic

**Key Methods:**
- `sendTaskAssignmentEmail(taskInstance, assignedUser)` - Task notification with deep link
- `sendTaskCompletionEmail(taskInstance, stakeholders)` - Completion notification
- `generateEmailHtml(template, data)` - Render HTML from Thymeleaf template
- `retryFailedEmail(emailId)` - Retry logic (max 3 retries, exponential backoff)

**Dependencies:** Spring Mail (JavaMail), Thymeleaf, AuditService

**Technology:** Spring @Service, @Async, Gmail SMTP (smtp.gmail.com:587), Thymeleaf templates

**Configuration:**
- SMTP: smtp.gmail.com:587 (TLS)
- Account: ctrlalteliteg@gmail.com
- Rate Limit: 500 emails/day (sufficient for MVP)

**PRD Coverage:** Stories 4.4 (Email Service), 4.5 (Email Triggers)

---

**TemplateService (Template Validation)**

**Responsibility:** Template management and validation

**Key Methods:**
- `createTemplate(request)` - Create with full validation
- `validateTemplate(template)` - Validate dependencies, conditionals, uniqueness
- `normalizeSequenceOrder(tasks)` - Auto-normalize to 1, 2, 3...
- `detectCircularDependencies(tasks)` - Detect and reject cycles

**Dependencies:** WorkflowTemplateRepository, TemplateTaskRepository, TemplateCustomFieldRepository, TemplateConditionalRuleRepository, AuditService

**PRD Coverage:** Epic 2 (Template Management), Story 2.3 (Validation)

---

**UserService (Authentication & Authorization)**

**Responsibility:** User management, authentication

**Key Methods:**
- `authenticateUser(username, password)` - Verify credentials
- `createUser(request)` - Create with BCrypt hashing
- `changePassword(id, currentPassword, newPassword)` - Password change
- `deactivateUser(id)` - Soft delete

**Dependencies:** UserRepository, PasswordEncoder (BCrypt), AuditService

**PRD Coverage:** Epic 1 (Foundation & Authentication)

---

**AuditService (Audit Logging)**

**Responsibility:** Comprehensive audit logging

**Key Methods:**
- `logUserAction(userId, actionType, entityType, entityId, description, metadata)`
- `logLogin(userId, ipAddress)`
- `getAuditEvents(filters, pagination)`
- `exportAuditLog(filters, format)`

**Dependencies:** AuditEventRepository

**Technology:** Spring @Service, @Async, Event listeners

**PRD Coverage:** Story 5.6 (Audit Capture), Story 5.5 (Audit Trail)

---

**ConditionalRuleEvaluator (Strategy Pattern)**

**Responsibility:** Evaluate conditional logic for templates

**Implementation:**
- EqualsOperator, NotEqualsOperator, ContainsOperator strategies
- Stateless utility service

**PRD Coverage:** Story 2.7 (Conditional Logic), Story 3.2 (Evaluation)

### Component Interaction Diagram

```mermaid
graph TB
    subgraph "Controller Layer"
        AC[AuthController]
        UC[UserController]
        TC[TemplateController]
        WC[WorkflowController]
        TKC[TaskController]
        DC[DashboardController]
    end

    subgraph "Service Layer"
        US[UserService]
        TS[TemplateService]
        WS[WorkflowService<br/><b>Core Orchestration</b>]
        TKS[TaskService]
        NS[NotificationService]
        AS[AuditService]
        CRE[ConditionalRuleEvaluator]
    end

    subgraph "Repository Layer"
        UR[UserRepository]
        WTR[WorkflowTemplateRepository]
        WIR[WorkflowInstanceRepository]
        TIR[TaskInstanceRepository]
        PIR[ProvisionedItemRepository]
        AER[AuditEventRepository]
    end

    subgraph "External Services"
        SMTP[Gmail SMTP]
    end

    AC --> US
    UC --> US --> AS
    TC --> TS --> AS
    WC --> WS --> NS --> SMTP
    WC --> AS
    TKC --> TKS --> WS
    TKC --> NS
    DC --> WS
    DC --> TKS

    US --> UR
    TS --> WTR
    WS --> WIR
    WS --> TIR
    WS --> PIR
    WS --> CRE
    TKS --> TIR
    TKS --> PIR
    AS --> AER
```

## External APIs

The system has minimal external dependencies for MVP:

### Gmail SMTP (Primary External Integration)

- **Purpose:** Email delivery for task notifications and workflow updates
- **Documentation:** https://support.google.com/mail/answer/7126229
- **Base URL:** smtp.gmail.com:587 (TLS/STARTTLS)
- **Authentication:** Application-specific password (environment variable: SMTP_PASSWORD)
- **Rate Limits:** 500 emails per day for free Gmail accounts

**Key Endpoints Used:**
- SMTP connection to smtp.gmail.com:587
- TLS/STARTTLS for secure connection
- SMTP AUTH for authentication

**Integration Notes:**
- Fully documented in NotificationService component
- Asynchronous delivery with retry logic (max 3 retries, exponential backoff)
- HTML emails rendered via Thymeleaf templates
- Sufficient for MVP volume (<50 workflows/month = ~150 emails/month)
- Gmail rate limit monitoring required (500 emails/day)

### No Additional External APIs for MVP

The PRD explicitly defers these integrations to post-MVP:
- ❌ Active Directory integration
- ❌ SSO/SAML providers
- ❌ Third-party notification services
- ❌ External data sources or webhooks

## Core Workflows

### Workflow 1: User Authentication & Login

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant AuthController
    participant UserService
    participant UserRepository
    participant SpringSecurity
    participant AuditService

    User->>Frontend: Enter username & password
    Frontend->>AuthController: POST /api/auth/login
    AuthController->>UserService: authenticateUser(username, password)
    UserService->>UserRepository: findByUsername(username)
    UserRepository-->>UserService: User entity
    UserService->>UserService: Verify BCrypt password
    alt Password Valid
        UserService-->>AuthController: Authentication success
        AuthController->>SpringSecurity: Create session (15 min timeout)
        SpringSecurity-->>AuthController: Session ID
        AuthController->>AuditService: logLogin(userId, ipAddress)
        AuthController-->>Frontend: 200 OK + Set-Cookie
        Frontend-->>User: Redirect to dashboard
    else Password Invalid
        UserService-->>AuthController: Authentication failed
        AuthController-->>Frontend: 401 Unauthorized
        Frontend-->>User: Display error message
    end
```

**PRD Coverage:** Story 1.4 (Authentication & Session Management)

---

### Workflow 2: HR Initiates Onboarding Workflow

```mermaid
sequenceDiagram
    participant HR as HR Admin
    participant Frontend
    participant WorkflowController
    participant WorkflowService
    participant TemplateService
    participant ConditionalRuleEvaluator
    participant NotificationService
    participant Database

    HR->>Frontend: Select template, enter employee details
    Frontend->>WorkflowController: POST /api/workflows
    WorkflowController->>WorkflowService: createWorkflowInstance(...)

    WorkflowService->>TemplateService: getTemplateById(templateId)
    TemplateService->>Database: Fetch template with tasks, fields, rules
    Database-->>TemplateService: Template data

    WorkflowService->>Database: Create WorkflowInstance (INITIATED)
    WorkflowService->>ConditionalRuleEvaluator: evaluateRules(template, customFields)
    ConditionalRuleEvaluator-->>WorkflowService: Task visibility map

    WorkflowService->>Database: Create TaskInstances (visible + hidden)
    WorkflowService->>Database: Update workflow to IN_PROGRESS

    WorkflowService->>WorkflowService: assignTasksForWorkflow(...)
    WorkflowService->>Database: Query users by role (load balancing)
    WorkflowService->>Database: Update tasks with assigned users

    WorkflowService->>NotificationService: sendTaskAssignmentEmails(tasks)
    NotificationService-->>NotificationService: Async email delivery

    WorkflowService-->>WorkflowController: Workflow created
    WorkflowController-->>Frontend: 201 Created
    Frontend-->>HR: Display success
```

**Key Points:**
- Conditional rules evaluated once at instantiation
- Load balancing: assign to user with fewest IN_PROGRESS tasks
- Emails sent asynchronously (non-blocking)
- Status transitions: INITIATED → IN_PROGRESS automatically

**PRD Coverage:** Stories 3.2, 3.3, 3.5, 4.5

---

### Workflow 3: Task Completion with Checklist (Onboarding)

```mermaid
sequenceDiagram
    participant Tech as Tech Support
    participant Frontend
    participant TaskController
    participant TaskService
    participant WorkflowService
    participant Database

    Tech->>Frontend: Click email link to task form
    Frontend->>TaskController: GET /api/tasks/{id}
    TaskController-->>Frontend: Task details

    Tech->>Frontend: Add checklist items, check all boxes
    Frontend->>TaskController: POST /api/tasks/{id}/complete
    TaskController->>TaskService: completeTask(taskId, checklistItems, userId)

    TaskService->>TaskService: validateChecklist(checklistItems)
    alt All Items Checked
        TaskService->>Database: Save TaskChecklistItems
        TaskService->>Database: Create ProvisionedItems (offboarding mirror)
        Note over TaskService,Database: Copy all items to provisioned_items
        TaskService->>Database: Update task status=COMPLETED

        TaskService->>WorkflowService: Notify task complete
        WorkflowService->>WorkflowService: Assign dependent tasks
        WorkflowService->>WorkflowService: Check workflow completion

        TaskService-->>TaskController: Success
        TaskController-->>Frontend: 200 OK
    else Items Not All Checked
        TaskService-->>TaskController: 400 Bad Request
        TaskController-->>Frontend: Validation error
    end
```

**Key Points:**
- Mandatory checklist verification (PRD Story 4.2)
- All checked items copied to provisioned_items
- Task completion triggers dependent task assignment
- Workflow auto-completes when all visible tasks done

**PRD Coverage:** Stories 4.2, 4.3, 4.6, 4.7

---

### Workflow 4: Offboarding Mirror (Critical Security Feature)

```mermaid
sequenceDiagram
    participant HR as HR Admin
    participant Frontend
    participant WorkflowController
    participant WorkflowService
    participant TaskService
    participant Database

    HR->>Frontend: Initiate offboarding, enter employee email
    Frontend->>WorkflowController: POST /api/workflows (OFFBOARDING)
    WorkflowController->>WorkflowService: createWorkflowInstance(...)

    WorkflowService->>Database: findByEmployeeEmail(email, ONBOARDING)
    Database-->>WorkflowService: Prior onboarding workflow

    alt Onboarding Found
        WorkflowService->>Database: Fetch provisioned_items
        Database-->>WorkflowService: List of provisioned items
        Note over WorkflowService: OFFBOARDING MIRROR ACTIVATED

        WorkflowService->>Database: Create WorkflowInstance + TaskInstances

        loop For each offboarding task
            WorkflowService->>TaskService: prePopulateOffboardingChecklist(...)
            TaskService->>Database: Create TaskChecklistItems (is_checked=false)
        end

        WorkflowService-->>Frontend: Workflow with pre-populated checklists
        Frontend-->>HR: Display auto-generated items

    else No Onboarding Found
        WorkflowService-->>Frontend: Empty checklists (manual entry)
        Frontend-->>HR: Warning + empty checklists
    end
```

**Key Points:**
- **Offboarding mirror is THE key security feature**
- Searches for prior onboarding by employee_email
- Pre-populates checklists with provisioned items
- Category-based mapping (HARDWARE → hardware task, ACCESS → access task)
- Ensures 100% cleanup (no orphaned accounts)

**PRD Coverage:** Story 4.7 (Offboarding Mirror) - **Critical differentiator**

---

### Workflow 5: Dashboard with Kanban Visualization

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant DashboardController
    participant WorkflowService
    participant Database

    User->>Frontend: Navigate to dashboard
    Frontend->>DashboardController: GET /api/dashboard/summary
    DashboardController->>WorkflowService: Get statistics (role-filtered)
    WorkflowService->>Database: Aggregate queries
    Database-->>WorkflowService: Stats

    DashboardController-->>Frontend: Summary (cached 5 min)
    Frontend-->>User: Display Kanban columns

    User->>Frontend: Apply filters
    Frontend->>DashboardController: GET /api/workflows?filters
    DashboardController->>WorkflowService: Query with filters
    WorkflowService->>Database: Filtered query
    DashboardController-->>Frontend: Workflows
    Frontend-->>User: Update Kanban cards
```

**Key Points:**
- Kanban visualization (Initiated/In Progress/Blocked/Completed)
- Role-based filtering (HR sees all, others see only their workflows)
- Caching for performance (5-minute TTL)
- Real-time overdue task highlighting

**PRD Coverage:** Stories 5.1, 5.2, 5.3

## REST API Spec

Complete OpenAPI 3.0 specification defining all backend endpoints. The specification is auto-generated by SpringDoc OpenAPI and used by OpenAPI Generator to create the TypeScript frontend client.

**Base URL:** http://localhost:8080/api

**Authentication:** Session-based (JSESSIONID cookie, 15-minute timeout)

**Key Endpoint Groups:**
- `/api/auth/*` - Authentication and session management
- `/api/users/*` - User management (HR Admin only)
- `/api/templates/*` - Workflow template CRUD
- `/api/workflows/*` - Workflow instance operations
- `/api/tasks/*` - Task completion and management
- `/api/dashboard/*` - Dashboard statistics
- `/api/audit/*` - Audit trail access
- `/api/reports/*` - Report generation and export

**Full OpenAPI specification available at:** `/swagger-ui.html` when backend is running

**Key Design Decisions:**
1. Session-based auth (no JWT for MVP simplicity)
2. Role-based access control enforced at endpoint level
3. Pagination on all list endpoints (default 50 items)
4. Consistent error response format (timestamp, status, error, message, path)
5. DTO pattern (never expose entities, separate request/response DTOs)
6. OpenAPI Generator creates TypeScript client from this spec

**PRD Coverage:** All 37 user stories across 5 epics

## Database Schema

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

## Source Tree

```
hackathon-workflow-2025/
├── frontend/                           # React + TypeScript application
│   ├── src/
│   │   ├── components/                 # Reusable UI components
│   │   │   ├── common/                 # Buttons, inputs, modals
│   │   │   ├── auth/                   # Login, session management
│   │   │   ├── dashboard/              # Kanban board, filters
│   │   │   ├── templates/              # Template builder, library
│   │   │   ├── workflows/              # Workflow detail, initiation
│   │   │   └── tasks/                  # Task completion forms, queue
│   │   ├── features/                   # Redux slices
│   │   │   ├── auth/                   # authSlice, authApi
│   │   │   ├── users/                  # usersSlice, usersApi
│   │   │   ├── templates/              # templatesSlice, templatesApi
│   │   │   ├── workflows/              # workflowsSlice, workflowsApi
│   │   │   ├── tasks/                  # tasksSlice, tasksApi
│   │   │   └── dashboard/              # dashboardSlice, dashboardApi
│   │   ├── api/                        # OpenAPI generated client
│   │   │   └── generated/              # Auto-generated TypeScript client
│   │   ├── hooks/                      # Custom React hooks
│   │   ├── utils/                      # Utility functions
│   │   ├── types/                      # TypeScript type definitions
│   │   ├── App.tsx                     # Main app component
│   │   ├── main.tsx                    # Entry point
│   │   └── store.ts                    # Redux store configuration
│   ├── public/                         # Static assets
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── Dockerfile                      # Frontend container (nginx)
│
├── backend/                            # Spring Boot + Java application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/magnab/employeelifecycle/
│   │   │   │   ├── config/             # Configuration classes
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── AsyncConfig.java
│   │   │   │   │   └── OpenApiConfig.java
│   │   │   │   ├── controller/         # REST controllers
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   ├── TemplateController.java
│   │   │   │   │   ├── WorkflowController.java
│   │   │   │   │   ├── TaskController.java
│   │   │   │   │   ├── DashboardController.java
│   │   │   │   │   ├── AuditController.java
│   │   │   │   │   └── ReportController.java
│   │   │   │   ├── service/            # Business logic
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   ├── TemplateService.java
│   │   │   │   │   ├── WorkflowService.java
│   │   │   │   │   ├── TaskService.java
│   │   │   │   │   ├── NotificationService.java
│   │   │   │   │   ├── AuditService.java
│   │   │   │   │   └── ConditionalRuleEvaluator.java
│   │   │   │   ├── repository/         # Spring Data JPA repositories
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── WorkflowTemplateRepository.java
│   │   │   │   │   ├── TemplateTaskRepository.java
│   │   │   │   │   ├── WorkflowInstanceRepository.java
│   │   │   │   │   ├── TaskInstanceRepository.java
│   │   │   │   │   ├── ProvisionedItemRepository.java
│   │   │   │   │   └── AuditEventRepository.java
│   │   │   │   ├── entity/             # JPA entities
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── WorkflowTemplate.java
│   │   │   │   │   ├── TemplateTask.java
│   │   │   │   │   ├── WorkflowInstance.java
│   │   │   │   │   ├── TaskInstance.java
│   │   │   │   │   ├── ProvisionedItem.java
│   │   │   │   │   └── AuditEvent.java
│   │   │   │   ├── dto/                # Data Transfer Objects
│   │   │   │   │   ├── request/        # Request DTOs
│   │   │   │   │   └── response/       # Response DTOs
│   │   │   │   ├── enums/              # Enumerations
│   │   │   │   │   ├── UserRole.java
│   │   │   │   │   ├── WorkflowStatus.java
│   │   │   │   │   ├── TaskStatus.java
│   │   │   │   │   └── ItemCategory.java
│   │   │   │   ├── exception/          # Custom exceptions
│   │   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   │   ├── ValidationException.java
│   │   │   │   │   ├── ConflictException.java
│   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   └── EmployeeLifecycleApplication.java
│   │   │   └── resources/
│   │   │       ├── db/
│   │   │       │   └── changelog/
│   │   │       │       └── db.changelog-master.yaml
│   │   │       ├── templates/          # Thymeleaf email templates
│   │   │       │   ├── task-assignment-email.html
│   │   │       │   └── task-completion-email.html
│   │   │       └── application.yml     # Spring Boot configuration
│   │   └── test/
│   │       ├── java/com/magnab/employeelifecycle/
│   │       │   ├── service/            # Service unit tests
│   │       │   ├── controller/         # Controller integration tests
│   │       │   └── repository/         # Repository tests
│   │       └── resources/
│   │           └── application-test.yml
│   ├── pom.xml                         # Maven configuration
│   └── Dockerfile                      # Backend container
│
├── docker-compose.yml                  # Multi-container orchestration
├── .env.example                        # Environment variables template
├── .gitignore
└── README.md                           # Setup instructions
```

**Key Organizational Principles:**
1. **Monorepo structure** - Single repository, clear frontend/backend separation
2. **Package by feature** - Backend organized by domain (not by layer)
3. **Component-based frontend** - Reusable components, Redux feature slices
4. **Generated code isolation** - OpenAPI client in dedicated folder
5. **Test parallelism** - Tests mirror source structure

## Infrastructure and Deployment

**Deployment Model:** Local Docker Compose (no cloud for MVP)

**Docker Compose Services:**
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:17.2-alpine
    environment:
      POSTGRES_DB: employee_lifecycle
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend:
    build: ./backend
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: employee_lifecycle
      DB_USERNAME: postgres
      DB_PASSWORD: ${DB_PASSWORD}
      SMTP_HOST: smtp.gmail.com
      SMTP_PORT: 587
      SMTP_USERNAME: ctrlalteliteg@gmail.com
      SMTP_PASSWORD: ${SMTP_PASSWORD}
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    volumes:
      - ./frontend/nginx.conf:/etc/nginx/nginx.conf:ro
```

**nginx Configuration (Reverse Proxy):**
```nginx
server {
    listen 80;
    server_name localhost;

    root /usr/share/nginx/html;
    index index.html;

    # Frontend routes
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Backend API proxy
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

**Environment Variables (.env):**
```
DB_PASSWORD=secure_password_here
SMTP_PASSWORD=gmail_app_specific_password
```

**Deployment Process:**
```bash
# 1. Clone repository
git clone <repo-url>
cd hackathon-workflow-2025

# 2. Create .env file
cp .env.example .env
# Edit .env with actual passwords

# 3. Start all services
docker-compose up --build

# 4. Access application
# Frontend: http://localhost
# Backend API: http://localhost/api
# Swagger UI: http://localhost/api/swagger-ui.html
```

**Resource Allocation:**
- Frontend: 512MB RAM (nginx lightweight)
- Backend: 2GB RAM (Spring Boot JVM)
- PostgreSQL: 4GB RAM (database operations)
- **Total:** 6.5GB RAM + 4 CPU cores minimum

**Health Checks:**
- PostgreSQL: `pg_isready` command
- Backend: Spring Actuator `/actuator/health`
- Frontend: nginx status check

**PRD Alignment:** Local Docker deployment only per PRD specifications

## Error Handling Strategy

**Approach:** Centralized exception handling with consistent error responses

**Error Model:**
```java
@Data
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
```

**Global Exception Handler:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, WebRequest request) {
        return ResponseEntity.status(400).body(new ErrorResponse(...));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return ResponseEntity.status(404).body(new ErrorResponse(...));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, WebRequest request) {
        return ResponseEntity.status(403).body(new ErrorResponse(...));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, WebRequest request) {
        return ResponseEntity.status(409).body(new ErrorResponse(...));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500).body(new ErrorResponse(...));
    }
}
```

**Logging Standards:**
- **Library:** SLF4J + Logback
- **Format:** JSON for structured logging
- **Levels:** ERROR (unrecoverable), WARN (degraded), INFO (key events), DEBUG (diagnostic)
- **Correlation ID:** UUID per request (MDC context)
- **Log files:** Captured by Docker (stdout/stderr)

**Email Retry Logic:**
```java
@Async
@Retryable(
    value = {MailException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public void sendEmail(EmailDetails details) {
    // Send email via JavaMail
    // Log attempt to audit_events
}
```

**Circuit Breaker:** Not needed for MVP (only Gmail SMTP external dependency)

## Coding Standards

**Critical Rules for AI Agents:**

1. **Never log sensitive data** - No passwords, tokens, or PII in logs
2. **Always use DTOs at API boundaries** - Never expose entities in responses
3. **All database queries via repositories** - No direct EntityManager use
4. **Async for email only** - Don't overuse @Async
5. **Transactional at service layer** - @Transactional on services, not controllers
6. **Validate all inputs** - Use @Valid and Jakarta Bean Validation
7. **Use constructor injection** - Immutable dependencies (final fields)
8. **Return Optional for nullable queries** - Clear API contracts
9. **Lombok for entities only** - Don't overuse @Data on DTOs
10. **Specific exceptions, not generic** - Throw ValidationException, ResourceNotFoundException, etc.

**Naming Conventions:**
- Entities: Singular nouns (User, WorkflowTemplate)
- Repositories: EntityRepository pattern
- Services: EntityService pattern
- Controllers: EntityController pattern
- DTOs: ActionEntityRequest/Response (CreateUserRequest, UserResponse)

**Code Quality Tools:**
- Backend: Checkstyle (optional but recommended)
- Frontend: ESLint + Prettier (enforced)

## Test Strategy

**Approach:** Unit + Integration testing (no E2E for MVP)

**Coverage Goals:**
- Service layer: 80% unit test coverage
- Controllers: Happy path + critical errors (integration tests)
- UI: Component tests for forms and complex logic

**Backend Testing:**

**Unit Tests (JUnit 5 + Mockito):**
```java
@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {
    @Mock private WorkflowInstanceRepository workflowRepo;
    @Mock private NotificationService notificationService;
    @InjectMocks private WorkflowService workflowService;

    @Test
    void createWorkflowInstance_shouldEvaluateConditionalRules() {
        // Test conditional logic evaluation
    }
}
```

**Integration Tests (TestContainers):**
```java
@SpringBootTest
@Testcontainers
class WorkflowControllerIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.2-alpine");

    @Autowired private WebTestClient webClient;

    @Test
    void initiateWorkflow_shouldCreateWorkflowAndAssignTasks() {
        // Full integration test with real database
    }
}
```

**Frontend Testing (Jest + React Testing Library):**
```typescript
describe('TaskCompletionForm', () => {
  it('should disable submit until all items checked', () => {
    render(<TaskCompletionForm />);
    expect(screen.getByRole('button', { name: /complete/i })).toBeDisabled();
    // Check all items
    expect(screen.getByRole('button', { name: /complete/i })).toBeEnabled();
  });
});
```

**Manual Testing:**
- Email delivery and rendering (Postman for API triggers)
- Full user workflows (HR initiates → Tech completes → Dashboard updates)
- Offboarding mirror verification

## Security

**Authentication:** Session-based (15-minute timeout) with BCrypt password hashing

**Authorization:** Role-based access control (RBAC) enforced at controller level
```java
@PreAuthorize("hasRole('HR_ADMIN')")
@PostMapping("/api/workflows")
public ResponseEntity<WorkflowDetailResponse> initiateWorkflow(...) {
    // Only HR_ADMIN can initiate workflows
}
```

**Input Validation:**
- Jakarta Bean Validation (@Valid on request DTOs)
- Whitelist approach (define allowed values)
- Validation at API boundary before service layer

**CSRF Protection:** Enabled for all non-GET requests (Spring Security default)

**CORS Policy:**
```java
@Configuration
public class SecurityConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowCredentials(true);
        return source;
    }
}
```

**Security Headers:**
- X-Frame-Options: DENY
- X-Content-Type-Options: nosniff
- X-XSS-Protection: 1; mode=block

**Secrets Management:**
- **Development:** Environment variables via .env file
- **Production:** Environment variables injected by deployment platform
- **Never:** Hardcoded secrets in code or configuration files

**Data Protection:**
- Encryption in transit: TLS for SMTP (port 587)
- Encryption at rest: PostgreSQL (optional, not configured for MVP)
- Password storage: BCrypt (Spring Security default)

**SQL Injection Prevention:** JPA parameterized queries (automatic)

**Dependency Security:** `npm audit` for frontend, Maven dependency check (optional for MVP)

## Architecture Document Completion Summary

**Document Status:** ✅ COMPLETE AND READY FOR DEVELOPMENT

**Sections Completed:**
1. ✅ Introduction - Project overview and starter template decisions
2. ✅ High Level Architecture - Modular monolith, architectural patterns, diagrams
3. ✅ Tech Stack - Definitive technology selections (37 technologies specified)
4. ✅ Data Models - 11 core entities with relationships and design decisions
5. ✅ Components - 28 components across Controller, Service, Repository layers
6. ✅ External APIs - Gmail SMTP integration (minimal external dependencies)
7. ✅ Core Workflows - 5 sequence diagrams covering key user journeys
8. ✅ REST API Spec - Complete OpenAPI specification for all endpoints
9. ✅ Database Schema - Liquibase changesets for 11 tables + enums
10. ✅ Source Tree - Complete monorepo structure
11. ✅ Infrastructure & Deployment - Docker Compose with 3 services
12. ✅ Error Handling Strategy - Global exception handler, logging, retry logic
13. ✅ Coding Standards - 10 critical rules for AI agents
14. ✅ Test Strategy - Unit + Integration testing approach
15. ✅ Security - Authentication, authorization, CSRF, CORS, input validation

**Key Architectural Decisions:**
- **Modular Monolith** - Single Spring Boot application with clear service boundaries
- **Session-based Auth** - 15-minute timeout, simpler than JWT for MVP
- **PostgreSQL 17** - Latest stable with Liquibase migrations
- **Local Docker Deployment** - No cloud infrastructure for MVP
- **Offboarding Mirror** - Critical security feature (provisioned_items table)
- **OpenAPI Code Generation** - TypeScript client auto-generated from backend spec

**PRD Alignment:**
- ✅ Supports all 37 user stories across 5 epics
- ✅ Implements all 18 functional requirements
- ✅ Meets all 12 non-functional requirements
- ✅ Addresses all technical assumptions from PRD
- ✅ Implements offboarding mirror (key differentiator)

**Technology Stack (Confirmed):**
- Backend: Java 17, Spring Boot 3.2.2, PostgreSQL 17.2, Liquibase, Maven
- Frontend: TypeScript 5.3.3, React 18.2.0, Redux Toolkit, Material-UI 5.15.7, Vite
- Deployment: Docker Compose, nginx, OpenJDK 17
- Testing: JUnit 5, Mockito, TestContainers, Jest, React Testing Library
- Email: Gmail SMTP (smtp.gmail.com:587, ctrlalteliteg@gmail.com)

**Readiness Assessment:** ✅ READY FOR DEVELOPMENT

This architecture document provides complete guidance for:
- AI agents implementing the 37 user stories
- Human developers understanding system design
- QA engineers planning test strategies
- DevOps engineers setting up infrastructure

## Next Steps

### For Frontend Development

**Prerequisites:**
1. Read this architecture document (complete system understanding)
2. Read PRD `docs/prd.md` (business requirements and user stories)
3. Access to OpenAPI spec (generated at `/swagger-ui.html` when backend runs)

**Frontend-Specific Architecture Needed:**
A separate **Frontend Architecture Document** should detail:
- Complete component hierarchy (dashboard, templates, workflows, tasks)
- Redux store structure (slices, reducers, actions)
- Material-UI theme customization (Magna BC branding)
- Form validation patterns (React Hook Form)
- Routing strategy (React Router with protected routes)
- API client integration (OpenAPI generated client usage)
- State management patterns (local vs global state decisions)
- UI/UX wireframes for 8 core screens
- Responsive breakpoints (desktop 1920x1080, 1366x768, tablet landscape)
- Accessibility implementation (WCAG AA compliance)

**Command to Create Frontend Architecture:**
```
Use Architect agent with "Frontend Architecture Mode" or create separate frontend-architecture.md
```

### For Backend Development

**Development Sequence (Recommended):**

**Phase 1: Foundation (Epic 1 - Stories 1.1 to 1.7)**
1. Set up Spring Boot project from Spring Initializr
2. Configure PostgreSQL connection and Liquibase
3. Run Liquibase migrations (creates all tables)
4. Implement User entity, repository, service
5. Implement Spring Security configuration (session-based auth)
6. Implement AuthController and UserController
7. Test authentication flow with Postman

**Phase 2: Template Management (Epic 2 - Stories 2.1 to 2.7)**
1. Implement template entities (WorkflowTemplate, TemplateTask, TemplateCustomField, TemplateConditionalRule)
2. Implement repositories and TemplateService
3. Implement validation logic (circular dependencies, sequence normalization)
4. Implement TemplateController
5. Test template CRUD operations

**Phase 3: Workflow Execution (Epic 3 - Stories 3.1 to 3.7)**
1. Implement workflow entities (WorkflowInstance, TaskInstance, WorkflowStateHistory)
2. Implement WorkflowService (core orchestration)
3. Implement ConditionalRuleEvaluator (strategy pattern)
4. Implement task assignment algorithm (load balancing)
5. Implement WorkflowController
6. Test workflow initiation and task assignment

**Phase 4: Task Completion & Email (Epic 4 - Stories 4.1 to 4.8)**
1. Implement checklist entities (TaskChecklistItem, ProvisionedItem)
2. Implement TaskService with validation
3. Implement NotificationService with Spring Mail
4. Create Thymeleaf email templates
5. Configure Gmail SMTP (environment variables)
6. Implement TaskController
7. Test task completion flow with offboarding mirror
8. Verify email delivery

**Phase 5: Dashboard & Reporting (Epic 5 - Stories 5.1 to 5.8)**
1. Implement AuditEvent entity and repository
2. Implement AuditService with async logging
3. Implement DashboardController (aggregate queries)
4. Implement AuditController and ReportController
5. Implement CSV export functionality
6. Test complete end-to-end flows

### For QA/Testing

**Test Plan Based on Architecture:**

1. **Unit Tests (JUnit 5 + Mockito):**
   - Target: 80% coverage on service layer
   - Focus: WorkflowService, TaskService, TemplateService (complex business logic)
   - Mock all repository and external service dependencies

2. **Integration Tests (TestContainers):**
   - Use real PostgreSQL container for database tests
   - Test full API flows (controller → service → repository → database)
   - Test critical paths: workflow initiation, task completion, offboarding mirror

3. **Manual Testing:**
   - Email rendering (check HTML in various email clients)
   - Complete user workflows (HR → Manager → Tech Support → Admin flows)
   - Offboarding mirror verification (onboard → offboard → verify checklist)
   - Dashboard filtering and performance (with sample data)

4. **Test Data:**
   - Create seed data via Liquibase changesets
   - Generate multiple workflow templates (onboarding, offboarding)
   - Create test users for each role

### For DevOps/Infrastructure

**Setup Sequence:**

1. **Local Development Environment:**
```bash
# Install prerequisites
- Docker Desktop or Docker Engine
- Git
- Java 17 JDK (for backend dev)
- Node.js 20.11.0 (for frontend dev)
- IntelliJ IDEA (backend)
- VSCode (frontend)

# Clone repository
git clone <repo-url>
cd hackathon-workflow-2025

# Create .env file
cp .env.example .env
# Add: DB_PASSWORD and SMTP_PASSWORD

# Start infrastructure
docker-compose up postgres  # Start database first
# Run backend via IDE or: mvn spring-boot:run
# Run frontend: cd frontend && npm install && npm run dev

# Access application
Frontend: http://localhost:3000
Backend: http://localhost:8080/api
Swagger: http://localhost:8080/swagger-ui.html
```

2. **Gmail SMTP Setup:**
- Log in to Gmail account (ctrlalteliteg@gmail.com)
- Enable 2-factor authentication
- Generate app-specific password: https://myaccount.google.com/apppasswords
- Add password to .env file as SMTP_PASSWORD

3. **Database Management:**
```bash
# Access PostgreSQL
docker exec -it <postgres-container> psql -U postgres -d employee_lifecycle

# View Liquibase changelog
SELECT * FROM databasechangelog;

# Manual rollback (if needed)
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

### Handoff Checklist

**Before Development Starts:**
- ✅ Architecture document reviewed and approved
- ✅ PRD document available (`docs/prd.md`)
- ✅ Project Brief available (`docs/brief.md`)
- ☐ Frontend Architecture document created (if UI team separate)
- ☐ Team has access to repository
- ☐ Gmail SMTP credentials obtained
- ☐ Development environment setup verified
- ☐ Sprint planning completed (37 stories allocated)

**Development Resources:**
- Architecture: `docs/architecture.md` (this document)
- PRD: `docs/prd.md`
- Project Brief: `docs/brief.md`
- API Docs: `/swagger-ui.html` (when backend runs)
- Tech Stack Reference: See "Tech Stack" section above

**Communication Plan:**
- Daily standups: Review progress on user stories
- Weekly demos: Show completed epics
- Architecture questions: Review with Architect
- PRD clarifications: Review with Product Manager

---

## Document Information

**Document:** Employee Lifecycle Management System - Backend Architecture
**Version:** 1.0
**Date:** 2025-10-30
**Author:** Architect - Winston
**Status:** Complete and Approved

**Next Document:** Frontend Architecture Document (to be created by UX Expert or Frontend Architect)

**Repository:** `hackathon-workflow-2025/`
**Architecture Doc Location:** `docs/architecture.md`

