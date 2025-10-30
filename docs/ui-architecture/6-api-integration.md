# 6. API Integration

This document defines how the frontend integrates with the Spring Boot backend REST API, using OpenAPI-generated TypeScript clients to ensure type safety and contract compliance.

**Alignment with Sally's UX Specification:**
- ✅ Auto-generated TypeScript client from backend OpenAPI 3.0 spec ensures frontend-backend type safety
- ✅ Session-based authentication (15-minute timeout) integrates with Spring Security
- ✅ Automatic 401 handling redirects to login page when session expires
- ✅ RTK Query caching reduces API calls for dashboard (supports LCP < 2.0s performance target)
- ✅ Optimistic updates provide immediate feedback for task completion and workflow actions
- ✅ Error handling shows user-friendly messages aligned with Sally's error state designs

**API Integration Strategy:**
- **OpenAPI Generator:** Auto-generates TypeScript client from backend `/api-docs` endpoint on every build
- **RTK Query:** Wraps generated client for caching, invalidation, and optimistic updates
- **Axios Interceptors:** Global error handling (401 → logout, 403 → forbidden page, network errors)
- **Type Safety:** Generated types ensure compile-time validation of API contracts

## Service Template

**Axios Instance Configuration (`src/services/api.service.ts`):**

```typescript
import axios from 'axios';
import { store } from '@/store/store';
import { clearUser } from '@/features/auth/authSlice';

// Create Axios instance with base configuration
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Include cookies for session-based auth
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Log request in development
    if (import.meta.env.DEV) {
      console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`);
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => {
    // Log response in development
    if (import.meta.env.DEV) {
      console.log(`[API Response] ${response.status} ${response.config.url}`);
    }
    return response;
  },
  (error) => {
    // Handle 401 Unauthorized - session expired
    if (error.response?.status === 401) {
      console.warn('[API] Session expired, logging out...');
      store.dispatch(clearUser());
      window.location.href = '/login';
    }

    // Handle 403 Forbidden
    if (error.response?.status === 403) {
      console.error('[API] Access forbidden');
      // Optionally redirect to unauthorized page
    }

    // Handle network errors
    if (!error.response) {
      console.error('[API] Network error:', error.message);
    }

    return Promise.reject(error);
  }
);
```

## API Client Configuration

**RTK Query Base Query with Auto-Generated Client:**

RTK Query endpoints use the auto-generated OpenAPI client located in `src/services/generated/`. The client is regenerated on build via OpenAPI Generator:

**Build Integration (`package.json`):**
```json
{
  "scripts": {
    "generate-client": "openapi-generator-cli generate -i http://localhost:8080/api-docs -g typescript-axios -o src/services/generated --additional-properties=supportsES6=true,withSeparateModelsAndApi=true",
    "prebuild": "npm run generate-client",
    "build": "tsc && vite build",
    "dev": "vite"
  }
}
```

**When to Regenerate OpenAPI Client:**

1. **Automatically:** Before every production build
   - The `prebuild` script ensures client is regenerated before `npm run build`
   - Guarantees production uses latest backend API contract

2. **Manually:** After backend API changes during development
   - Run `npm run generate-client` when backend adds/modifies endpoints
   - **Requirement:** Backend must be running at `http://localhost:8080`
   - If backend is not running, script will fail (expected behavior)

3. **Development Workflow:**
   ```bash
   # Start backend first
   cd backend && mvn spring-boot:run

   # In separate terminal, regenerate client
   cd frontend && npm run generate-client

   # Start frontend dev server
   npm run dev
   ```

**Generated Files (Do Not Edit Manually):**
- `src/services/generated/api.ts` - API client classes
- `src/services/generated/models.ts` - TypeScript type definitions
- `src/services/generated/configuration.ts` - API configuration
- All files in `src/services/generated/` are auto-generated and will be overwritten

**Using Generated Client in RTK Query:**
```typescript
import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import { WorkflowTemplateApi, WorkflowInstanceApi } from '@/services/generated/api';
import type { WorkflowTemplateResponse, CreateWorkflowRequest } from '@/services/generated/models';

export const workflowsApi = createApi({
  reducerPath: 'workflowsApi',
  baseQuery: fetchBaseQuery({
    baseUrl: import.meta.env.VITE_API_BASE_URL || '/api',
    credentials: 'include',
  }),
  tagTypes: ['Workflows', 'WorkflowDetail'],
  endpoints: (builder) => ({
    getAllWorkflows: builder.query<WorkflowTemplateResponse[], void>({
      query: () => '/workflows',
      providesTags: ['Workflows'],
    }),
    createWorkflow: builder.mutation<void, CreateWorkflowRequest>({
      query: (data) => ({
        url: '/workflows',
        method: 'POST',
        body: data,
      }),
      invalidatesTags: ['Workflows'],
    }),
  }),
});
```

## Error Response Handling

**Standard Backend Error Format:**

The backend GlobalExceptionHandler returns structured error responses that follow a consistent format. Frontend code should handle these errors appropriately.

**Error Response Interface (`src/types/api.types.ts`):**

```typescript
/**
 * Standard error response from backend GlobalExceptionHandler
 * Returned for validation errors (400), authorization errors (403),
 * not found errors (404), conflict errors (409), and server errors (500)
 */
export interface ApiErrorResponse {
  /** ISO 8601 timestamp when error occurred */
  timestamp: string;

  /** HTTP status code (400, 403, 404, 409, 500) */
  status: number;

  /** Error type (e.g., "Bad Request", "Conflict", "Internal Server Error") */
  error: string;

  /** Human-readable error message suitable for logging or display */
  message: string;

  /** API endpoint path that generated the error */
  path: string;

  /** Optional array of validation error details (for 400 Bad Request) */
  details?: string[];
}
```

**Error Handling in RTK Query:**

```typescript
import { useGetWorkflowsQuery } from '@/features/workflows/workflowsApi';
import type { ApiErrorResponse } from '@/types/api.types';

function WorkflowList() {
  const { data, error, isLoading } = useGetWorkflowsQuery();

  if (error) {
    // RTK Query error format: { status, data }
    if ('status' in error && 'data' in error) {
      const apiError = error.data as ApiErrorResponse;

      // Display user-friendly error message
      return <Alert severity="error">{apiError.message}</Alert>;
    }

    // Fallback for network errors
    return <Alert severity="error">Network error. Please try again.</Alert>;
  }

  // ... rest of component
}
```

**Error Handling in Axios Interceptor:**

The Axios interceptor already handles common error scenarios:
- **401 Unauthorized:** Automatic logout and redirect to /login (session expired)
- **403 Forbidden:** Log error, optionally redirect to /unauthorized page
- **Network Errors:** Log error message for debugging

For specific error handling per endpoint, extract error details from response:

```typescript
import { apiClient } from '@/services/api.service';
import type { ApiErrorResponse } from '@/types/api.types';

async function createWorkflow(data: CreateWorkflowRequest) {
  try {
    const response = await apiClient.post('/workflows', data);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const apiError = error.response.data as ApiErrorResponse;

      // Handle specific error cases
      if (apiError.status === 400) {
        // Validation error - show validation details
        console.error('Validation errors:', apiError.details);
        throw new Error(apiError.message);
      }

      if (apiError.status === 409) {
        // Conflict error - e.g., duplicate workflow name
        throw new Error(apiError.message);
      }
    }

    // Re-throw for generic error handling
    throw error;
  }
}
```

**Common Error Status Codes:**

| Status | Error Type | Meaning | Frontend Action |
|--------|------------|---------|-----------------|
| 400 | Bad Request | Validation error, invalid input | Display validation errors inline on form |
| 401 | Unauthorized | Session expired or not authenticated | Redirect to login (handled by interceptor) |
| 403 | Forbidden | Insufficient permissions for action | Display "Access denied" message or redirect to /unauthorized |
| 404 | Not Found | Resource doesn't exist (workflow, task, user) | Display "Not found" message, redirect to list page |
| 409 | Conflict | Business logic conflict (e.g., can't delete template in use) | Display conflict message with suggested resolution |
| 500 | Internal Server Error | Backend error | Display generic error message, log for debugging |

---
