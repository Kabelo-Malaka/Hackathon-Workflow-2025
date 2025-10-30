# Source Tree

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
