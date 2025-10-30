# Core Workflows

## Workflow 1: User Authentication & Login

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

## Workflow 2: HR Initiates Onboarding Workflow

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

## Workflow 3: Task Completion with Checklist (Onboarding)

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

## Workflow 4: Offboarding Mirror (Critical Security Feature)

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

## Workflow 5: Dashboard with Kanban Visualization

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
