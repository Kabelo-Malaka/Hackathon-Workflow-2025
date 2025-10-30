# Introduction

This document outlines the overall project architecture for **Employee Lifecycle Management System**, including backend systems, shared services, and non-UI specific concerns. Its primary goal is to serve as the guiding architectural blueprint for AI-driven development, ensuring consistency and adherence to chosen patterns and technologies.

**Relationship to Frontend Architecture:**
If the project includes a significant user interface, a separate Frontend Architecture Document will detail the frontend-specific design and MUST be used in conjunction with this document. Core technology stack choices documented herein (see "Tech Stack") are definitive for the entire project, including any frontend components.

## Starter Template or Existing Project

**Decision: Use Official Framework Templates**

The project will use official framework templates for backend and frontend, with manual monorepo setup:

1. **Backend:** Spring Initializr (https://start.spring.io/) with Spring Boot 3.x, Java 17, Spring Web, Spring Data JPA, Spring Security, Liquibase dependencies
2. **Frontend:** Vite React-TypeScript template (`npm create vite@latest`)
3. **Monorepo Structure:** Manual setup with root-level docker-compose.yml

**Rationale:** Official templates provide well-maintained starting points without imposing unwanted constraints. Manual monorepo setup gives complete control over structure to match PRD requirements (Docker Compose, specific service organization).

## Change Log

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2025-10-30 | 1.0 | Initial architecture document creation from PRD | Architect - Winston |
