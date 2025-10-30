# Next Steps

## For Frontend Development

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

## For Backend Development

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

## For QA/Testing

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

## For DevOps/Infrastructure

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

## Handoff Checklist

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
