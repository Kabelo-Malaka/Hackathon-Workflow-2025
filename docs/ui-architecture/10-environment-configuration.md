# 10. Environment Configuration

This document defines environment-specific configuration using Vite's environment variables, supporting development, staging, and production deployments.

**Alignment with Sally's UX Specification:**
- ✅ Session timeout configuration (15 minutes = 900000ms) matches Spring Security backend
- ✅ API base URL configuration supports localhost development and production deployment
- ✅ DevTools toggle enables Redux DevTools in development only
- ✅ Environment-specific settings support Sally's performance targets (production optimizations)

**Environment Strategy:**
- **Development (`.env.development`):** Local backend at localhost:8080, DevTools enabled, verbose logging
- **Staging (`.env.staging`):** Staging API URL, DevTools disabled, error tracking enabled
- **Production (`.env.production`):** Production API URL (relative path `/api`), DevTools disabled, optimized builds

**Vite Environment Variable Rules:**
- Prefix all variables with `VITE_` to expose them to client-side code
- Never commit sensitive data (API keys, secrets) to `.env` files
- Use `import.meta.env.VITE_*` to access variables in TypeScript code
- Type variables appropriately (use `Number()`, `Boolean()` for non-string types)

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
