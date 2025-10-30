# Infrastructure and Deployment

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
