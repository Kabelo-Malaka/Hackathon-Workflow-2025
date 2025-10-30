# 10. Environment Configuration

## Environment Variables

**Development (`.env.development`):**
```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_APP_NAME=Employee Lifecycle Management
VITE_SESSION_TIMEOUT=900000
VITE_ENABLE_DEVTOOLS=true
```

**Production (`.env.production`):**
```bash
VITE_API_BASE_URL=/api
VITE_APP_NAME=Employee Lifecycle Management
VITE_SESSION_TIMEOUT=900000
VITE_ENABLE_DEVTOOLS=false
```

**Usage in Code:**
```typescript
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
const sessionTimeout = Number(import.meta.env.VITE_SESSION_TIMEOUT);
```

---
