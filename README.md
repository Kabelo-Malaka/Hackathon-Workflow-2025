# Employee Lifecycle Management System

A web-based workflow orchestration platform for managing employee onboarding and offboarding processes.

## Prerequisites

- **Docker**: 24.0.7 or higher
- **Node.js**: 20.11.0 LTS
- **Java**: 17 LTS
- **Maven**: 3.9.6

## Setup Instructions

### Initial Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd hackathon-workflow-2025
   ```

2. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your actual configuration values
   ```

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

### Backend Setup

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

## Development Scripts

### Frontend

- `npm run dev` - Start development server with hot reload
- `npm run build` - Create production build
- `npm test` - Run test suite

### Backend

- `mvn spring-boot:run` - Start development server
- `mvn clean install` - Build project and run tests
- `mvn test` - Run test suite only

## Running Tests

The project includes comprehensive testing infrastructure for both frontend and backend components.

### Testing Frameworks

**Frontend**:
- **Jest 29.7.0** - Testing framework
- **React Testing Library 14.1.2** - Component testing utilities
- **@testing-library/user-event** - User interaction simulation
- **Coverage Threshold**: 80% across statements, branches, functions, and lines

**Backend**:
- **JUnit 5 (Jupiter)** - Unit testing framework
- **Mockito 5.8.0** - Mocking framework
- **TestContainers 1.19.3** - Integration testing with real PostgreSQL
- **Spring Security Test** - Security testing utilities

### Frontend Tests

Run all frontend tests:
```bash
cd frontend
npm test
```

Run tests with coverage report:
```bash
npm test -- --coverage
```

Run tests in watch mode (development):
```bash
npm test -- --watch
```

**Sample Tests**:
- `src/components/common/Button.test.tsx` - Demonstrates React Testing Library best practices
- Tests include: component rendering, user interactions, accessibility checks

### Backend Tests

Run all backend tests (unit + integration):
```bash
cd backend
mvn test
```

Run only unit tests:
```bash
mvn test -Dtest=*Test
```

Run only integration tests:
```bash
mvn test -Dtest=*IntegrationTest
```

Run specific test class:
```bash
mvn test -Dtest=UserServiceTest
```

**Sample Tests**:
- `src/test/java/.../service/UserServiceTest.java` - Unit tests with Mockito mocks
- `src/test/java/.../controller/UserControllerIntegrationTest.java` - Integration tests with TestContainers

**Note**: Integration tests automatically spin up a PostgreSQL container using TestContainers and run database migrations with Liquibase.

### Test Coverage Reports

**Frontend**:
Coverage reports are generated in `frontend/coverage/` directory. Open `coverage/lcov-report/index.html` in a browser to view the detailed report.

**Backend**:
JaCoCo coverage reports can be configured by adding the JaCoCo Maven plugin to `pom.xml`. By default, Maven Surefire generates basic test reports in `target/surefire-reports/`.

## Running with Docker Compose

The easiest way to run the entire application stack (frontend, backend, database) is using Docker Compose.

### Prerequisites

- **Docker**: 24.0.7 or higher
- **Docker Compose**: 2.23.3 or higher
- **System Requirements**: Minimum 6.5GB RAM and 4 CPU cores

### Setup and Run

1. **Configure environment variables**
   ```bash
   cp .env.example .env
   ```
   Edit `.env` and set your actual passwords:
   - `DB_PASSWORD` - Set a secure database password
   - `SMTP_PASSWORD` - Set your Gmail app-specific password

2. **Start all services**
   ```bash
   docker-compose up --build
   ```
   This will start three services:
   - PostgreSQL database (port 5432)
   - Spring Boot backend (port 8080)
   - React frontend with nginx (port 80)

3. **Access the application**
   - **Frontend**: http://localhost
   - **Backend API**: http://localhost/api (proxied through nginx)
   - **Backend Direct**: http://localhost:8080
   - **Swagger UI**: http://localhost/api/swagger-ui.html
   - **Health Check**: http://localhost:8080/actuator/health

### Stop Services

```bash
docker-compose down
```

### Cleanup (Remove Volumes)

To completely remove all containers and volumes (including database data):

```bash
docker-compose down -v
```

**Warning**: This will delete all database data!

## Database Schema

The application uses **PostgreSQL 17.2** with **Liquibase 4.25.1** for database migration management.

### Schema Management

- All database changes are versioned using Liquibase changesets
- Migrations run automatically on application startup
- Schema history is tracked in the `DATABASECHANGELOG` table
- Each changeset includes rollback scripts for safe schema evolution

### View Current Schema Version

Connect to the PostgreSQL database and query the changelog:

```bash
docker exec -it hackathon-workflow-2025-postgres-1 psql -U postgres -d employee_lifecycle
```

Then run:
```sql
SELECT id, author, filename, dateexecuted FROM databasechangelog ORDER BY dateexecuted;
```

### Rollback Migrations

To rollback database changes (use with caution):

```bash
cd backend
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

**Note**: Rollback requires Maven to be installed and configured with database connection details.

### Current Schema

**Tables**:
- `users` - System users with role-based access control (HR_ADMIN, LINE_MANAGER, TECH_SUPPORT, ADMINISTRATOR)

**Enumerations**:
- `user_role` - User role types for RBAC

All business tables include audit columns (`created_at`, `created_by`, `updated_at`, `updated_by`) for comprehensive tracking.

## Project Structure

```
hackathon-workflow-2025/
├── frontend/          # React + TypeScript application
├── backend/           # Spring Boot + Java application
├── docker-compose.yml # Multi-container orchestration (placeholder)
├── .env.example       # Environment variables template
├── .gitignore         # Git ignore rules
└── README.md          # This file
```

## Tech Stack

- **Frontend**: React 18.2.0, TypeScript 5.3.3, Vite 5.0.12
- **Backend**: Spring Boot 3.2.2, Java 17
- **Database**: PostgreSQL 17.2
- **Containerization**: Docker, Docker Compose

## Contributing

Follow the coding standards defined in `docs/architecture/coding-standards.md`.
