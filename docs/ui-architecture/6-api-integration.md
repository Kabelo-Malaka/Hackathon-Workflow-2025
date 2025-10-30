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
    "build": "tsc && vite build"
  }
}
```

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

---
