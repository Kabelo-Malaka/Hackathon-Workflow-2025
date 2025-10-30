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

## Docker Compose

Full Docker Compose configuration will be available in Story 1.2. The current `docker-compose.yml` is a placeholder.

```bash
docker-compose up --build
```

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
