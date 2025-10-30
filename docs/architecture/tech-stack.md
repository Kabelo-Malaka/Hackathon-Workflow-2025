# Tech Stack

## Cloud Infrastructure

- **Provider:** Local Docker Environment (no cloud provider for MVP)
- **Key Services:** Docker Compose orchestrating three containers (frontend nginx, backend Spring Boot, PostgreSQL)
- **Deployment Regions:** N/A - Local development machines only
- **Future Consideration:** If cloud deployment needed post-MVP, consider AWS (EC2, RDS, SES) or Azure (App Service, Azure Database for PostgreSQL, SendGrid)

**Rationale:** PRD explicitly states "Local Docker deployment only for MVP; no CI/CD pipeline required."

## Technology Stack Table

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

## Key Confirmations

✅ **Maven** is acceptable (vs. Gradle)
✅ **OpenAPI Generator** will be integrated into build process
✅ **nginx reverse proxy** pattern is acceptable (proxy `/api` to backend:8080)
✅ **In-memory sessions** for MVP (vs. Redis)
✅ **No cloud deployment** needed for MVP hackathon demo

## Version Pinning Strategy

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
