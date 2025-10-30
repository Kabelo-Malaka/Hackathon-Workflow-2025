# 5. State Management

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
