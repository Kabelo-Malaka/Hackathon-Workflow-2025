# 5. State Management

This document defines state management patterns using Redux Toolkit and RTK Query to support the complex user interactions and real-time updates required by Sally's UX specification.

**Alignment with Sally's UX Specification:**
- ✅ Supports real-time task updates for KanbanBoard (drag-and-drop state management)
- ✅ Manages complex filter state for FilterPanel (status, assignee, date, priority combinations)
- ✅ Handles template builder state for TemplateFormBuilder (dynamic fields, conditional rules, validation)
- ✅ Tracks workflow progress for ProgressTracker (current step, completion status, navigation)
- ✅ Caches dashboard data for fast navigation (LCP < 2.0s performance target)
- ✅ Optimistic updates for task completion (immediate UI feedback while API calls execute)
- ✅ Session management with 15-minute timeout (backend Spring Security integration)

**State Management Strategy:**
- **UI State (Redux Slices):** Filters, modal dialogs, view preferences, selected items
- **Server State (RTK Query):** API data, caching, automatic refetching, optimistic updates
- **Form State (React Hook Form):** Template builder, task completion, user forms (not Redux)
- **Component State (useState):** Accordion expand/collapse, tooltip visibility, local UI toggles

**Conditional Task Logic Evaluation Strategy:**

The system uses conditional rules to show/hide tasks based on custom field values (e.g., "If remote=yes, skip office desk assignment"). Evaluation happens at both client and server levels with clear separation of concerns:

1. **Client-Side Evaluation (Preview Only):**
   - **Where:** Template Builder (TemplateFormBuilder.tsx) and Workflow Initiation Form (InitiateWorkflowDialog.tsx)
   - **Purpose:** Provide immediate UX feedback showing which tasks will be created
   - **Implementation:** React Hook Form's `watch()` API monitors custom field changes, client-side JavaScript evaluates conditional rules to show/hide task previews
   - **Authority:** Non-authoritative - preview only, not persisted

2. **Server-Side Evaluation (Authoritative):**
   - **Where:** WorkflowService.createWorkflowInstance() in backend
   - **Purpose:** Definitively determine which tasks are visible when workflow is instantiated
   - **Implementation:** ConditionalRuleEvaluator.java evaluates all rules, sets `task_instances.is_visible` flag in database
   - **Authority:** Authoritative - this is the source of truth

3. **After Workflow Creation:**
   - Frontend always uses the server-side `is_visible` flag from API responses
   - No client-side re-evaluation after workflow is created
   - Task visibility is immutable once workflow is instantiated (reflects state at creation time)

**Example Flow:**
```typescript
// 1. Template Builder (Client Preview)
// HR sees: "If remote=true → Office Setup task will be hidden"
const { watch } = useForm();
const isRemote = watch('remote');
const showOfficeSetup = !isRemote; // Client-side preview

// 2. Workflow Initiation (Server Authority)
POST /api/workflows { employee_name, remote: true, ... }
// → Backend evaluates: remote=true → OfficeSetupTask.is_visible=false

// 3. Workflow Display (Server Truth)
GET /api/workflows/{id}
// → Returns tasks with is_visible flags
// → Frontend respects server's is_visible values (no re-evaluation)
```

## Store Structure

The application uses **Redux Toolkit** for global state management and **RTK Query** for server state (API data fetching and caching).

```
src/store/
├── store.ts              # Redux store configuration
├── rootReducer.ts        # Combines all feature slices
└── hooks.ts              # Typed useDispatch/useSelector hooks

src/features/
├── auth/
│   ├── authSlice.ts      # Local state: user, isAuthenticated, sessionExpiry
│   └── authApi.ts        # RTK Query: login, logout, checkSession
├── dashboard/
│   ├── dashboardSlice.ts # Local state: filters, view preferences
│   └── dashboardApi.ts   # RTK Query: fetchTaskSummary, fetchAssignedTasks
├── templates/
│   ├── templatesSlice.ts # Local state: currentTemplate, isDirty
│   └── templatesApi.ts   # RTK Query: CRUD operations for templates
├── workflows/
│   ├── workflowsSlice.ts # Local state: selectedWorkflow, filters
│   └── workflowsApi.ts   # RTK Query: CRUD operations for workflows
├── tasks/
│   ├── tasksSlice.ts     # Local state: selectedTask, completionDialog
│   └── tasksApi.ts       # RTK Query: CRUD operations for tasks
└── users/
    ├── usersSlice.ts     # Local state: filters, selectedUser
    └── usersApi.ts       # RTK Query: CRUD operations for users
```

## State Management Template

**Store Configuration (`src/store/store.ts`):**

```typescript
import { configureStore } from '@reduxjs/toolkit';
import { setupListeners } from '@reduxjs/toolkit/query';
import rootReducer from './rootReducer';
import { authApi } from '@/features/auth/authApi';
import { dashboardApi } from '@/features/dashboard/dashboardApi';
import { templatesApi } from '@/features/templates/templatesApi';
import { workflowsApi } from '@/features/workflows/workflowsApi';
import { tasksApi } from '@/features/tasks/tasksApi';
import { usersApi } from '@/features/users/usersApi';

export const store = configureStore({
  reducer: rootReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware()
      .concat(authApi.middleware)
      .concat(dashboardApi.middleware)
      .concat(templatesApi.middleware)
      .concat(workflowsApi.middleware)
      .concat(tasksApi.middleware)
      .concat(usersApi.middleware),
  devTools: process.env.NODE_ENV !== 'production',
});

// Enable refetchOnFocus/refetchOnReconnect behaviors
setupListeners(store.dispatch);

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

**Root Reducer (`src/store/rootReducer.ts`):**

```typescript
import { combineReducers } from '@reduxjs/toolkit';
import authReducer from '@/features/auth/authSlice';
import dashboardReducer from '@/features/dashboard/dashboardSlice';
import templatesReducer from '@/features/templates/templatesSlice';
import workflowsReducer from '@/features/workflows/workflowsSlice';
import tasksReducer from '@/features/tasks/tasksSlice';
import usersReducer from '@/features/users/usersSlice';
import { authApi } from '@/features/auth/authApi';
import { dashboardApi } from '@/features/dashboard/dashboardApi';
import { templatesApi } from '@/features/templates/templatesApi';
import { workflowsApi } from '@/features/workflows/workflowsApi';
import { tasksApi } from '@/features/tasks/tasksApi';
import { usersApi } from '@/features/users/usersApi';

const rootReducer = combineReducers({
  auth: authReducer,
  dashboard: dashboardReducer,
  templates: templatesReducer,
  workflows: workflowsReducer,
  tasks: tasksReducer,
  users: usersReducer,
  [authApi.reducerPath]: authApi.reducer,
  [dashboardApi.reducerPath]: dashboardApi.reducer,
  [templatesApi.reducerPath]: templatesApi.reducer,
  [workflowsApi.reducerPath]: workflowsApi.reducer,
  [tasksApi.reducerPath]: tasksApi.reducer,
  [usersApi.reducerPath]: usersApi.reducer,
});

export default rootReducer;
```

**Typed Hooks (`src/store/hooks.ts`):**

```typescript
import { TypedUseSelectorHook, useDispatch, useSelector } from 'react-redux';
import type { RootState, AppDispatch } from './store';

// Use throughout the app instead of plain `useDispatch` and `useSelector`
export const useAppDispatch = () => useDispatch<AppDispatch>();
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
```

## Redux Slice Template

**Example: `src/features/auth/authSlice.ts`**

```typescript
import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { RootState } from '@/store/store';

/**
 * Authentication state shape
 */
interface AuthState {
  user: {
    id: string;
    email: string;
    fullName: string;
    role: 'HR_ADMIN' | 'LINE_MANAGER' | 'TECH_SUPPORT' | 'ADMINISTRATOR';
  } | null;
  isAuthenticated: boolean;
  sessionExpiry: string | null;
}

const initialState: AuthState = {
  user: null,
  isAuthenticated: false,
  sessionExpiry: null,
};

/**
 * Authentication slice manages user session state
 */
const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setUser: (state, action: PayloadAction<AuthState['user']>) => {
      state.user = action.payload;
      state.isAuthenticated = !!action.payload;
      // Session expires in 15 minutes (backend timeout)
      state.sessionExpiry = new Date(Date.now() + 15 * 60 * 1000).toISOString();
    },
    clearUser: (state) => {
      state.user = null;
      state.isAuthenticated = false;
      state.sessionExpiry = null;
    },
    updateSessionExpiry: (state) => {
      state.sessionExpiry = new Date(Date.now() + 15 * 60 * 1000).toISOString();
    },
  },
});

// Actions
export const { setUser, clearUser, updateSessionExpiry } = authSlice.actions;

// Selectors
export const selectUser = (state: RootState) => state.auth.user;
export const selectIsAuthenticated = (state: RootState) => state.auth.isAuthenticated;
export const selectSessionExpiry = (state: RootState) => state.auth.sessionExpiry;
export const selectUserRole = (state: RootState) => state.auth.user?.role ?? null;

// Reducer
export default authSlice.reducer;
```

## RTK Query API Template

**Example: `src/features/auth/authApi.ts`**

```typescript
import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { User } from '@/types';

/**
 * Login request payload
 */
interface LoginRequest {
  email: string;
  password: string;
}

/**
 * Login response payload
 */
interface LoginResponse {
  user: User;
  message: string;
}

/**
 * Auth API handles authentication endpoints
 */
export const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: fetchBaseQuery({
    baseUrl: import.meta.env.VITE_API_BASE_URL || '/api',
    credentials: 'include', // Include cookies for session-based auth
  }),
  tagTypes: ['Auth'],
  endpoints: (builder) => ({
    login: builder.mutation<LoginResponse, LoginRequest>({
      query: (credentials) => ({
        url: '/auth/login',
        method: 'POST',
        body: credentials,
      }),
      invalidatesTags: ['Auth'],
    }),
    logout: builder.mutation<void, void>({
      query: () => ({
        url: '/auth/logout',
        method: 'POST',
      }),
      invalidatesTags: ['Auth'],
    }),
    checkSession: builder.query<{ isValid: boolean; user: User | null }, void>({
      query: () => '/auth/session',
      providesTags: ['Auth'],
    }),
  }),
});

// Export hooks for usage in components
export const {
  useLoginMutation,
  useLogoutMutation,
  useCheckSessionQuery,
} = authApi;
```

## State Management Best Practices

**1. Separation of Concerns:**
- **Redux Slices** manage UI state (filters, selections, modals, preferences)
- **RTK Query** manages server state (API data, caching, loading states)
- Never duplicate server data in Redux slices

**2. Normalized State:**
- Store entities by ID for efficient lookups
- Use `createEntityAdapter` for normalized collections
- Avoid deeply nested state structures

**3. Selectors:**
- Create reusable selectors for complex state derivations
- Use `createSelector` from Reselect for memoization
- Export selectors from slice files

**4. Async Operations:**
- Use RTK Query for all API calls (avoid `createAsyncThunk` when possible)
- Let RTK Query handle loading, error, and success states automatically
- Use `optimistic updates` for better UX

**5. Cache Invalidation:**
- Use `tagTypes` and `providesTags`/`invalidatesTags` for automatic cache invalidation
- Example: Creating a workflow invalidates `['Workflows']` tag, refetching workflow list

**6. Type Safety:**
- Always type slice state interfaces
- Export `RootState` and `AppDispatch` types
- Use typed hooks (`useAppDispatch`, `useAppSelector`)

---
