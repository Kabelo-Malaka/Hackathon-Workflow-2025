# Employee Lifecycle Management System - Frontend Architecture Document

<!-- Powered by BMAD™ Core -->

## 1. Template and Framework Selection

### Change Log

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2025-10-30 | 1.0 | Initial frontend architecture document | Winston (Architect) |

### Starter Template Decision

**Selected Template:** Vite React-TypeScript template (`npm create vite@latest`)

**Rationale:**

The frontend architecture is built on the **Vite React-TypeScript starter template**, which provides an optimal foundation for modern React development with several key advantages:

1. **Lightning-Fast Development Experience:**
   - Vite's native ESM-based dev server provides instant hot module replacement (HMR)
   - Sub-second cold starts compared to traditional bundlers
   - On-demand compilation ensures only modified modules are rebuilt

2. **TypeScript-First Approach:**
   - Pre-configured TypeScript support with strict mode enabled
   - Seamless integration with React's TypeScript definitions
   - Excellent IDE support and type checking out of the box

3. **Modern Build Optimization:**
   - Production builds use Rollup for optimal tree-shaking and code splitting
   - Automatic CSS code splitting and lazy loading
   - Built-in support for async chunks and dynamic imports

4. **Minimal Configuration Overhead:**
   - Zero-config setup for React + TypeScript
   - Sensible defaults that align with industry best practices
   - Easy to extend with plugins when needed

5. **Alignment with PRD Requirements:**
   - PRD explicitly specifies Vite 5.0.12 as the build tool
   - Template provides immediate productivity for hackathon timeline
   - Community-maintained template ensures stability and best practices

### Pre-installed Capabilities

The Vite React-TypeScript template provides:

**Out-of-the-Box Features:**
- React 18.2.0 with Fast Refresh enabled
- TypeScript 5.x with strict type checking
- Vite dev server with HMR
- ESLint configuration for code quality
- Production build optimization with Rollup

**File Structure:**
```
frontend/
├── public/              # Static assets
├── src/
│   ├── App.tsx         # Root component
│   ├── main.tsx        # Entry point
│   ├── index.css       # Global styles
│   └── vite-env.d.ts   # Vite type definitions
├── .eslintrc.cjs       # ESLint configuration
├── tsconfig.json       # TypeScript configuration
├── tsconfig.node.json  # TypeScript for Vite config
├── vite.config.ts      # Vite configuration
└── package.json
```

**Additional Dependencies Required:**

Based on the backend architecture's technology stack, the following dependencies will be added to the Vite starter:

1. **State Management:** Redux Toolkit 2.1.0 + RTK Query 2.1.0
2. **UI Components:** Material-UI (MUI) 5.15.7
3. **Routing:** React Router 6.21.3
4. **Form Handling:** React Hook Form 7.49.3
5. **API Client:** Auto-generated from OpenAPI spec via OpenAPI Generator 7.2.0
6. **Testing:** Jest 29.7.0 + React Testing Library 14.1.2
7. **Additional Utilities:** Date-fns, Yup validation, React Query DevTools

### Integration with Backend Architecture

The frontend template selection directly aligns with the backend architecture document (docs/architecture.md):

**API Integration Strategy:**
- Backend exposes OpenAPI 3.0 specification at `/api-docs` endpoint
- OpenAPI Generator (v7.2.0) auto-generates TypeScript client with type-safe interfaces
- Generated client is integrated into Vite build process
- RTK Query consumes generated types for data fetching and caching

**Authentication Flow:**
- Backend uses session-based authentication (Spring Security)
- Frontend receives JSESSIONID cookie (15-minute timeout)
- Axios interceptor handles 401 responses and redirects to login
- Protected routes check authentication state before rendering

**Deployment Integration:**
- Frontend builds to optimized static files via `npm run build`
- nginx serves static files and proxies `/api` requests to backend:8080
- Docker Compose orchestrates frontend (nginx), backend, and PostgreSQL
- Environment variables injected at build time for API base URL

---

## 2. Frontend Tech Stack

### Technology Stack Table

This section is synchronized with the main architecture document (docs/architecture.md) and extracts all frontend-related technologies.

| Category | Technology | Version | Purpose | Rationale |
|----------|-----------|---------|---------|-----------|
| **Language** | TypeScript | 5.3.3 | Frontend development language | Type safety, excellent tooling, PRD requirement, prevents runtime errors |
| **Runtime (Build)** | Node.js | 20.11.0 LTS | JavaScript build tooling | LTS version, stable for Vite and npm ecosystem |
| **Framework** | React | 18.2.0 | UI framework | PRD requirement, component-based architecture, excellent ecosystem, concurrent rendering |
| **Build Tool** | Vite | 5.0.12 | Frontend build tool & dev server | PRD requirement, lightning-fast HMR, native ESM support, optimized production builds |
| **State Management** | Redux Toolkit | 2.1.0 | Global state management | PRD requirement, official Redux approach, reduces boilerplate, excellent DevTools |
| **API Client** | RTK Query | 2.1.0 | Data fetching & caching | Integrated with Redux Toolkit, automatic cache invalidation, optimistic updates |
| **OpenAPI Codegen** | OpenAPI Generator | 7.2.0 | TypeScript client generation | Auto-generates type-safe API client from backend OpenAPI spec, ensures contract compliance |
| **UI Component Library** | Material-UI (MUI) | 5.15.7 | React component library | PRD requirement, enterprise-grade components, WCAG AA accessibility, Magna BC customizable theming |
| **Form Management** | React Hook Form | 7.49.3 | Complex form validation | PRD requirement, handles template builder forms, minimal re-renders, easy validation |
| **Form Validation** | Yup | 1.3.3 | Schema-based validation | Integration with React Hook Form, declarative validation rules, TypeScript support |
| **Routing** | React Router | 6.21.3 | Client-side routing | Industry standard, nested routes, protected route patterns, loader/action API |
| **HTTP Client** | Axios | 1.6.5 | HTTP requests | Interceptor support for auth, request/response transformation, timeout handling |
| **Date Handling** | date-fns | 3.2.0 | Date manipulation & formatting | Lightweight, tree-shakeable, consistent formatting across app |
| **Testing (Unit)** | Jest | 29.7.0 | Unit testing framework | PRD requirement, React ecosystem standard, snapshot testing, mocking utilities |
| **Testing (Component)** | React Testing Library | 14.1.2 | Component testing | PRD requirement, best practices for React, user-centric queries, accessibility checks |
| **Testing (E2E)** | Playwright | 1.41.2 | End-to-end testing | Cross-browser testing, auto-wait, screenshot/video capture, parallel execution |
| **Code Quality** | ESLint | 8.56.0 | Linting | Vite template default, catches bugs, enforces coding standards |
| **ESLint Config** | eslint-config-react-app | 7.0.1 | React linting rules | Create React App standards, TypeScript support |
| **Code Formatting** | Prettier | 3.2.4 | Code formatting | Consistent code style, integrates with ESLint, reduces formatting debates |
| **CSS-in-JS** | Emotion | 11.11.3 | Styling solution | MUI's default styling engine, performant, TypeScript support, SSR-compatible |
| **Icons** | Material Icons | 5.15.7 | Icon library | Bundled with MUI, 2000+ icons, SVG-based, customizable |
| **DevTools** | Redux DevTools Extension | - | State debugging | Time-travel debugging, action replay, state diff visualization |
| **DevTools** | React Developer Tools | - | Component debugging | Component tree inspection, props/state debugging, profiler |
| **IDE** | VSCode | 1.86+ | Frontend development | PRD recommended, excellent React/TypeScript support, extensions ecosystem |

### Version Pinning Strategy

**Exact Version Pinning:**
- All production dependencies use exact versions (no `^` or `~`) in `package.json`
- Ensures reproducible builds across development, CI/CD, and production environments
- Prevents unexpected breaking changes from minor/patch updates

**Update Strategy:**
- Review dependency updates monthly during maintenance windows
- Run full test suite before updating any dependency
- Update dependencies one at a time to isolate potential issues
- Use `npm outdated` to identify available updates

**Critical Dependencies (Never Auto-Update):**
- React, Redux Toolkit, Material-UI, React Hook Form
- Changes to these require thorough testing of all components

**Lock File Management:**
- Commit `package-lock.json` to version control
- Use `npm ci` in CI/CD pipelines for deterministic installs
- Developers use `npm install` for local development

### Technology Alignment with Backend

The frontend stack is carefully chosen to integrate seamlessly with the backend architecture:

**Type Safety Across Stack:**
- Backend OpenAPI 3.0 spec → OpenAPI Generator → TypeScript client
- Shared types ensure frontend and backend stay in sync
- Compile-time errors catch API contract violations

**Authentication Integration:**
- Frontend Axios interceptor detects 401 responses from Spring Security
- Automatic redirect to login page when session expires (15-minute timeout)
- JSESSIONID cookie managed transparently by browser

**State Management Integration:**
- RTK Query wraps auto-generated API client
- Automatic cache invalidation on mutations
- Optimistic updates for better UX (e.g., task completion)

**Build Process Integration:**
- OpenAPI Generator runs during `npm run build` (fetches `/api-docs` from backend)
- Generated TypeScript client placed in `src/services/generated/`
- Vite bundles application with generated client included

---

## 3. Project Structure

The frontend follows a **feature-based architecture** organized by business domain, with shared utilities and components separated. This structure promotes code reusability, maintainability, and clear boundaries between features.

```
frontend/
├── public/                           # Static assets (served as-is)
│   ├── favicon.ico
│   ├── logo-magna-bc.svg
│   └── robots.txt
│
├── src/
│   ├── main.tsx                      # Application entry point, renders <App />
│   ├── App.tsx                       # Root component with routing and providers
│   ├── vite-env.d.ts                 # Vite type definitions
│   │
│   ├── config/                       # Application configuration
│   │   ├── api.config.ts             # API base URLs, timeouts, retry logic
│   │   ├── theme.config.ts           # MUI theme customization (Magna BC branding)
│   │   └── constants.ts              # Global constants (roles, statuses, limits)
│   │
│   ├── features/                     # Feature-based modules (Redux slices)
│   │   ├── auth/
│   │   │   ├── authSlice.ts          # Redux slice for authentication state
│   │   │   ├── authApi.ts            # RTK Query API for auth endpoints
│   │   │   ├── LoginPage.tsx         # Login page component
│   │   │   ├── authSelectors.ts      # Reusable selectors
│   │   │   └── types.ts              # Auth-specific TypeScript types
│   │   │
│   │   ├── dashboard/
│   │   │   ├── dashboardSlice.ts     # Redux slice for dashboard state
│   │   │   ├── dashboardApi.ts       # RTK Query API for dashboard data
│   │   │   ├── DashboardPage.tsx     # Main dashboard view
│   │   │   ├── TaskKanbanBoard.tsx   # Kanban board component
│   │   │   ├── FilterPanel.tsx       # Dashboard filtering component
│   │   │   └── types.ts              # Dashboard-specific types
│   │   │
│   │   ├── templates/
│   │   │   ├── templatesSlice.ts     # Redux slice for template state
│   │   │   ├── templatesApi.ts       # RTK Query API for templates
│   │   │   ├── TemplateListPage.tsx  # Template list view
│   │   │   ├── TemplateEditorPage.tsx # Form-based template builder
│   │   │   ├── TemplateFormBuilder.tsx # Reusable form builder component
│   │   │   ├── ConditionalRuleEditor.tsx # Conditional logic UI
│   │   │   └── types.ts              # Template-specific types
│   │   │
│   │   ├── workflows/
│   │   │   ├── workflowsSlice.ts     # Redux slice for workflow instances
│   │   │   ├── workflowsApi.ts       # RTK Query API for workflows
│   │   │   ├── WorkflowListPage.tsx  # Workflow list view
│   │   │   ├── WorkflowDetailPage.tsx # Workflow detail with tasks
│   │   │   ├── InitiateWorkflowDialog.tsx # Workflow creation form
│   │   │   └── types.ts              # Workflow-specific types
│   │   │
│   │   ├── tasks/
│   │   │   ├── tasksSlice.ts         # Redux slice for tasks
│   │   │   ├── tasksApi.ts           # RTK Query API for tasks
│   │   │   ├── TaskDetailPage.tsx    # Task detail view
│   │   │   ├── TaskCompletionDialog.tsx # Task completion form
│   │   │   ├── ChecklistItemEditor.tsx # Checklist item component
│   │   │   └── types.ts              # Task-specific types
│   │   │
│   │   └── users/
│   │       ├── usersSlice.ts         # Redux slice for users
│   │       ├── usersApi.ts           # RTK Query API for users
│   │       ├── UserListPage.tsx      # User management view
│   │       ├── UserFormDialog.tsx    # User creation/edit form
│   │       └── types.ts              # User-specific types
│   │
│   ├── components/                   # Shared/reusable components
│   │   ├── common/
│   │   │   ├── Button.tsx            # Custom button wrapper (MUI)
│   │   │   ├── Card.tsx              # Custom card wrapper (MUI)
│   │   │   ├── DataTable.tsx         # Reusable data table component
│   │   │   ├── Dialog.tsx            # Custom dialog wrapper (MUI)
│   │   │   ├── ErrorBoundary.tsx     # Error boundary for error handling
│   │   │   ├── LoadingSpinner.tsx    # Loading indicator
│   │   │   ├── PageHeader.tsx        # Consistent page header
│   │   │   └── SearchBar.tsx         # Reusable search component
│   │   │
│   │   ├── layout/
│   │   │   ├── AppLayout.tsx         # Main layout with sidebar + header
│   │   │   ├── Sidebar.tsx           # Navigation sidebar
│   │   │   ├── Header.tsx            # Top header with user menu
│   │   │   └── Breadcrumbs.tsx       # Breadcrumb navigation
│   │   │
│   │   └── forms/
│   │       ├── FormInput.tsx         # React Hook Form input wrapper
│   │       ├── FormSelect.tsx        # React Hook Form select wrapper
│   │       ├── FormDatePicker.tsx    # React Hook Form date picker
│   │       └── FormCheckbox.tsx      # React Hook Form checkbox wrapper
│   │
│   ├── pages/                        # Page-level components (route targets)
│   │   ├── NotFoundPage.tsx          # 404 error page
│   │   └── UnauthorizedPage.tsx      # 403 error page
│   │
│   ├── routes/                       # Routing configuration
│   │   ├── AppRoutes.tsx             # Main route definitions
│   │   ├── ProtectedRoute.tsx        # Route guard for authentication
│   │   └── RoleBasedRoute.tsx        # Route guard for authorization
│   │
│   ├── services/                     # API services and utilities
│   │   ├── generated/                # Auto-generated OpenAPI client
│   │   │   ├── api.ts                # Generated API client
│   │   │   ├── models.ts             # Generated TypeScript models
│   │   │   └── configuration.ts      # Generated configuration
│   │   │
│   │   ├── api.service.ts            # Axios instance configuration
│   │   └── auth.service.ts           # Authentication utilities (session check)
│   │
│   ├── store/                        # Redux store configuration
│   │   ├── store.ts                  # Redux store setup
│   │   ├── rootReducer.ts            # Combine all slices
│   │   └── hooks.ts                  # Typed useDispatch/useSelector hooks
│   │
│   ├── hooks/                        # Custom React hooks
│   │   ├── useAuth.ts                # Authentication hook
│   │   ├── useDebounce.ts            # Debounce hook for search
│   │   ├── useLocalStorage.ts        # Local storage hook
│   │   └── usePermissions.ts         # Role-based permissions hook
│   │
│   ├── utils/                        # Utility functions
│   │   ├── formatters.ts             # Date, number, text formatters
│   │   ├── validators.ts             # Yup validation schemas
│   │   ├── constants.ts              # Shared constants
│   │   └── helpers.ts                # General helper functions
│   │
│   ├── types/                        # Shared TypeScript types
│   │   ├── index.ts                  # Re-export all shared types
│   │   ├── api.types.ts              # API request/response types
│   │   └── common.types.ts           # Common domain types
│   │
│   ├── assets/                       # Images, fonts, icons
│   │   ├── images/
│   │   ├── fonts/
│   │   └── icons/
│   │
│   └── styles/                       # Global styles
│       ├── global.css                # Global CSS (CSS reset, base styles)
│       ├── theme.ts                  # MUI theme definition (Magna BC colors)
│       └── variables.css             # CSS custom properties
│
├── tests/                            # Test files (mirrors src structure)
│   ├── __mocks__/                    # Mock data for tests
│   ├── setup.ts                      # Jest/Vitest setup
│   └── utils/                        # Test utilities
│
├── .env.development                  # Development environment variables
├── .env.production                   # Production environment variables
├── .eslintrc.cjs                     # ESLint configuration
├── .prettierrc                       # Prettier configuration
├── index.html                        # HTML entry point (Vite)
├── package.json                      # Dependencies and scripts
├── tsconfig.json                     # TypeScript configuration
├── tsconfig.node.json                # TypeScript for Vite config files
├── vite.config.ts                    # Vite configuration
└── README.md                         # Frontend documentation
```

### Key Structure Decisions

**1. Feature-Based Organization:**
- Each feature (auth, dashboard, templates, workflows, tasks, users) is self-contained
- Redux slices, API queries, components, and types live together within each feature
- Promotes high cohesion and low coupling
- Makes it easy to understand all code related to a specific feature

**2. Separation of Shared vs. Feature-Specific:**
- `components/` contains only truly reusable components used across multiple features
- Feature-specific components stay within their feature directory
- Prevents premature abstraction and over-engineering

**3. Generated Code Isolation:**
- `src/services/generated/` contains auto-generated OpenAPI client
- Never manually edit files in this directory
- Regenerated on every build via OpenAPI Generator

**4. Type Safety:**
- Each feature has its own `types.ts` for feature-specific types
- `src/types/` contains shared types used across multiple features
- Generated types from OpenAPI client are imported from `src/services/generated/models.ts`

**5. Test Colocation:**
- Test files live in `tests/` directory mirroring `src/` structure
- Alternative: Colocate tests next to source files (e.g., `Button.test.tsx` next to `Button.tsx`)
- Project uses `tests/` directory for cleaner production builds

**6. Configuration Centralization:**
- `src/config/` contains all configuration (API, theme, constants)
- Environment-specific values loaded via `.env` files
- Makes it easy to change configuration without touching business logic

---

## 4. Component Standards

### Component Template

All React components follow this standardized template to ensure consistency, type safety, and maintainability:

```typescript
import React from 'react';
import { Box, Typography } from '@mui/material';

/**
 * Props for the ExampleComponent
 */
interface ExampleComponentProps {
  /** Unique identifier for the component */
  id: string;
  /** Title to display */
  title: string;
  /** Optional description text */
  description?: string;
  /** Callback when action is triggered */
  onAction?: (id: string) => void;
  /** Child elements to render */
  children?: React.ReactNode;
  /** Additional CSS class names */
  className?: string;
}

/**
 * ExampleComponent displays a title, optional description, and children.
 *
 * @example
 * ```tsx
 * <ExampleComponent
 *   id="example-1"
 *   title="My Title"
 *   description="Optional description"
 *   onAction={(id) => console.log(id)}
 * >
 *   <p>Child content</p>
 * </ExampleComponent>
 * ```
 */
const ExampleComponent: React.FC<ExampleComponentProps> = ({
  id,
  title,
  description,
  onAction,
  children,
  className
}) => {
  // Local state (if needed)
  const [isActive, setIsActive] = React.useState(false);

  // Event handlers
  const handleClick = () => {
    setIsActive(prev => !prev);
    onAction?.(id);
  };

  // Render nothing if no title
  if (!title) {
    return null;
  }

  return (
    <Box className={className} data-testid={`example-component-${id}`}>
      <Typography variant="h6" onClick={handleClick}>
        {title}
      </Typography>
      {description && (
        <Typography variant="body2" color="text.secondary">
          {description}
        </Typography>
      )}
      {children}
    </Box>
  );
};

export default ExampleComponent;
```

### Component Standards Checklist

**Every component MUST:**
1. ✅ Define a TypeScript interface for props (named `{ComponentName}Props`)
2. ✅ Use `React.FC<PropsType>` type annotation
3. ✅ Include JSDoc comment describing the component's purpose
4. ✅ Destructure props in function signature for clarity
5. ✅ Include `data-testid` attribute for testing
6. ✅ Export as default at the end of the file
7. ✅ Place imports in this order: React, third-party, MUI, local components, utilities, types

**Every component SHOULD:**
1. ✅ Use functional components (not class components)
2. ✅ Use React hooks for state and side effects
3. ✅ Keep components focused on a single responsibility
4. ✅ Extract complex logic into custom hooks
5. ✅ Use optional chaining (`?.`) for optional callbacks
6. ✅ Provide default values for optional props if needed

**Avoid:**
1. ❌ Using `any` type (use `unknown` or specific types)
2. ❌ Inline styles (use MUI `sx` prop or styled components)
3. ❌ Direct DOM manipulation (use refs sparingly)
4. ❌ Mutating props or state directly
5. ❌ Using index as key in lists (use unique IDs)

### Naming Conventions

**Component Files:**
- Use PascalCase for component files: `TaskKanbanBoard.tsx`
- Match file name to component name exactly
- One component per file (exception: small helper components can be in same file)

**Component Names:**
- Use PascalCase: `TaskKanbanBoard`, `ConditionalRuleEditor`
- Use descriptive names that reflect purpose
- Suffix with type if ambiguous: `LoginPage`, `TaskListPage`, `UserFormDialog`

**Props Interfaces:**
- Always suffix with `Props`: `TaskKanbanBoardProps`
- Export interface if used by parent components
- Keep private if only used within the component file

**Event Handlers:**
- Prefix with `handle`: `handleClick`, `handleSubmit`, `handleChange`
- Use descriptive names: `handleTaskComplete`, `handleWorkflowInitiate`

**Boolean Props/State:**
- Prefix with `is`, `has`, `can`, `should`: `isLoading`, `hasError`, `canEdit`, `shouldShowDetails`

**Custom Hooks:**
- Prefix with `use`: `useAuth`, `useDebounce`, `usePermissions`
- Return array `[value, setter]` or object `{ value, setValue }` based on complexity

**Constants:**
- Use UPPER_SNAKE_CASE: `MAX_RETRY_ATTEMPTS`, `API_BASE_URL`
- Group related constants in objects: `WORKFLOW_STATUS.INITIATED`

**Functions/Utilities:**
- Use camelCase: `formatDate`, `validateEmail`, `calculateDueDate`
- Use verb-noun pattern: `getUserById`, `createWorkflow`, `updateTaskStatus`

**Types/Interfaces:**
- Use PascalCase: `User`, `WorkflowTemplate`, `TaskInstance`
- Prefix interfaces with `I` only if there's a class with the same name (rare in React)
- Suffix types with descriptive name: `UserRole`, `WorkflowStatus`

**Redux Slices:**
- Use camelCase for slice names: `authSlice`, `dashboardSlice`
- Use camelCase for action creators: `login`, `fetchDashboard`, `updateTaskStatus`
- Use UPPER_SNAKE_CASE for action types (auto-generated by Redux Toolkit)

**File/Folder Names:**
- Use PascalCase for components: `TaskKanbanBoard.tsx`
- Use camelCase for utilities/services: `authService.ts`, `formatters.ts`
- Use kebab-case for CSS files: `global-styles.css`
- Use lowercase for folders: `components/`, `hooks/`, `utils/`

### Import Organization

Organize imports in this order (separate each group with a blank line):

```typescript
// 1. React and React-related
import React, { useState, useEffect } from 'react';

// 2. Third-party libraries
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';

// 3. Material-UI
import { Box, Button, Typography, TextField } from '@mui/material';
import { CheckCircle as CheckCircleIcon } from '@mui/icons-material';

// 4. Redux (if used)
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { selectUser } from '@/features/auth/authSlice';

// 5. Local components
import TaskCard from '@/components/common/TaskCard';
import PageHeader from '@/components/layout/PageHeader';

// 6. Utilities and hooks
import { formatDate } from '@/utils/formatters';
import { useAuth } from '@/hooks/useAuth';

// 7. Types
import type { Task, WorkflowStatus } from '@/types';
```

---

## 5. State Management

### Store Structure

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

### State Management Template

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

### Redux Slice Template

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

### RTK Query API Template

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

### State Management Best Practices

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

## 6. API Integration

### Service Template

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

### API Client Configuration

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

## 7. Routing

### Route Configuration

**Main Routes (`src/routes/AppRoutes.tsx`):**

```typescript
import React, { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import AppLayout from '@/components/layout/AppLayout';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import ProtectedRoute from './ProtectedRoute';
import RoleBasedRoute from './RoleBasedRoute';

// Lazy load pages for code splitting
const LoginPage = lazy(() => import('@/features/auth/LoginPage'));
const DashboardPage = lazy(() => import('@/features/dashboard/DashboardPage'));
const TemplateListPage = lazy(() => import('@/features/templates/TemplateListPage'));
const TemplateEditorPage = lazy(() => import('@/features/templates/TemplateEditorPage'));
const WorkflowListPage = lazy(() => import('@/features/workflows/WorkflowListPage'));
const WorkflowDetailPage = lazy(() => import('@/features/workflows/WorkflowDetailPage'));
const TaskDetailPage = lazy(() => import('@/features/tasks/TaskDetailPage'));
const UserListPage = lazy(() => import('@/features/users/UserListPage'));
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage'));
const UnauthorizedPage = lazy(() => import('@/pages/UnauthorizedPage'));

const AppRoutes: React.FC = () => {
  return (
    <BrowserRouter>
      <Suspense fallback={<LoadingSpinner />}>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/unauthorized" element={<UnauthorizedPage />} />

          {/* Protected routes */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<DashboardPage />} />

              {/* Template routes (HR_ADMIN only) */}
              <Route element={<RoleBasedRoute allowedRoles={['HR_ADMIN', 'ADMINISTRATOR']} />}>
                <Route path="/templates" element={<TemplateListPage />} />
                <Route path="/templates/new" element={<TemplateEditorPage />} />
                <Route path="/templates/:id/edit" element={<TemplateEditorPage />} />
              </Route>

              {/* Workflow routes */}
              <Route path="/workflows" element={<WorkflowListPage />} />
              <Route path="/workflows/:id" element={<WorkflowDetailPage />} />

              {/* Task routes */}
              <Route path="/tasks/:id" element={<TaskDetailPage />} />

              {/* User management (ADMINISTRATOR only) */}
              <Route element={<RoleBasedRoute allowedRoles={['ADMINISTRATOR']} />}>
                <Route path="/users" element={<UserListPage />} />
              </Route>
            </Route>
          </Route>

          {/* 404 catch-all */}
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
};

export default AppRoutes;
```

**Protected Route Guard (`src/routes/ProtectedRoute.tsx`):**

```typescript
import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAppSelector } from '@/store/hooks';
import { selectIsAuthenticated } from '@/features/auth/authSlice';

const ProtectedRoute: React.FC = () => {
  const isAuthenticated = useAppSelector(selectIsAuthenticated);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;
```

**Role-Based Route Guard (`src/routes/RoleBasedRoute.tsx`):**

```typescript
import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAppSelector } from '@/store/hooks';
import { selectUserRole } from '@/features/auth/authSlice';

interface RoleBasedRouteProps {
  allowedRoles: string[];
}

const RoleBasedRoute: React.FC<RoleBasedRouteProps> = ({ allowedRoles }) => {
  const userRole = useAppSelector(selectUserRole);

  if (!userRole || !allowedRoles.includes(userRole)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
};

export default RoleBasedRoute;
```

---

## 8. Styling Guidelines

### Styling Approach

The application uses **Material-UI (MUI)** with **Emotion** for styling. Three styling methods are available:

1. **MUI `sx` prop** (preferred for one-off styles)
2. **MUI `styled` API** (for reusable styled components)
3. **CSS custom properties** (for global theme variables)

### Global Theme Variables

**CSS Custom Properties (`src/styles/variables.css`):**

```css
:root {
  /* Magna BC Brand Colors */
  --color-primary-main: #1976d2;
  --color-primary-light: #42a5f5;
  --color-primary-dark: #1565c0;

  --color-secondary-main: #9c27b0;
  --color-secondary-light: #ba68c8;
  --color-secondary-dark: #7b1fa2;

  --color-error: #d32f2f;
  --color-warning: #ed6c02;
  --color-info: #0288d1;
  --color-success: #2e7d32;

  /* Grays */
  --color-gray-50: #fafafa;
  --color-gray-100: #f5f5f5;
  --color-gray-200: #eeeeee;
  --color-gray-300: #e0e0e0;
  --color-gray-400: #bdbdbd;
  --color-gray-500: #9e9e9e;
  --color-gray-600: #757575;
  --color-gray-700: #616161;
  --color-gray-800: #424242;
  --color-gray-900: #212121;

  /* Spacing (8px base) */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;

  /* Typography */
  --font-family: 'Roboto', 'Helvetica', 'Arial', sans-serif;
  --font-size-xs: 0.75rem;
  --font-size-sm: 0.875rem;
  --font-size-md: 1rem;
  --font-size-lg: 1.25rem;
  --font-size-xl: 1.5rem;

  /* Shadows */
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24);
  --shadow-md: 0 3px 6px rgba(0, 0, 0, 0.15), 0 2px 4px rgba(0, 0, 0, 0.12);
  --shadow-lg: 0 10px 20px rgba(0, 0, 0, 0.15), 0 3px 6px rgba(0, 0, 0, 0.10);

  /* Border Radius */
  --border-radius-sm: 4px;
  --border-radius-md: 8px;
  --border-radius-lg: 12px;

  /* Z-index */
  --z-index-drawer: 1200;
  --z-index-modal: 1300;
  --z-index-snackbar: 1400;
  --z-index-tooltip: 1500;
}

/* Dark mode variables */
[data-theme='dark'] {
  --color-background: #121212;
  --color-surface: #1e1e1e;
  --color-text-primary: rgba(255, 255, 255, 0.87);
  --color-text-secondary: rgba(255, 255, 255, 0.60);
}
```

**MUI Theme Configuration (`src/styles/theme.ts`):**

```typescript
import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
      light: '#42a5f5',
      dark: '#1565c0',
    },
    secondary: {
      main: '#9c27b0',
      light: '#ba68c8',
      dark: '#7b1fa2',
    },
    error: {
      main: '#d32f2f',
    },
    warning: {
      main: '#ed6c02',
    },
    info: {
      main: '#0288d1',
    },
    success: {
      main: '#2e7d32',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
  spacing: 8, // Base spacing unit
  shape: {
    borderRadius: 8,
  },
});
```

---

## 9. Testing Requirements

### Component Test Template

**Example Test (`tests/components/TaskCard.test.tsx`):**

```typescript
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import TaskCard from '@/components/common/TaskCard';
import tasksReducer from '@/features/tasks/tasksSlice';

// Mock store setup
const mockStore = configureStore({
  reducer: {
    tasks: tasksReducer,
  },
});

describe('TaskCard', () => {
  const mockTask = {
    id: '123',
    title: 'Complete onboarding checklist',
    status: 'IN_PROGRESS',
    assignedTo: 'john.doe@example.com',
    dueDate: '2025-11-01',
  };

  it('renders task title and status correctly', () => {
    render(
      <Provider store={mockStore}>
        <TaskCard task={mockTask} />
      </Provider>
    );

    expect(screen.getByText('Complete onboarding checklist')).toBeInTheDocument();
    expect(screen.getByText('IN_PROGRESS')).toBeInTheDocument();
  });

  it('calls onTaskClick when card is clicked', () => {
    const handleClick = jest.fn();
    render(
      <Provider store={mockStore}>
        <TaskCard task={mockTask} onTaskClick={handleClick} />
      </Provider>
    );

    fireEvent.click(screen.getByTestId('task-card-123'));
    expect(handleClick).toHaveBeenCalledWith('123');
  });

  it('displays due date in correct format', () => {
    render(
      <Provider store={mockStore}>
        <TaskCard task={mockTask} />
      </Provider>
    );

    expect(screen.getByText('Due: Nov 1, 2025')).toBeInTheDocument();
  });
});
```

### Testing Best Practices

1. **Unit Tests:** Test individual components in isolation with mocked dependencies
2. **Integration Tests:** Test component interactions and data flow between components
3. **E2E Tests:** Test critical user flows (login, create workflow, complete task) using Playwright
4. **Coverage Goals:** Aim for 80% code coverage on components and utilities
5. **Test Structure:** Follow Arrange-Act-Assert pattern
6. **Mock External Dependencies:** Always mock API calls, routing, and Redux state

---

## 10. Environment Configuration

### Environment Variables

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

## 11. Frontend Developer Standards

### Critical Coding Rules

**Essential rules that prevent common AI mistakes:**

1. **Never use `any` type** - Always use specific types or `unknown` if type is truly unknown
2. **Always destructure props** - Makes code more readable and easier to refactor
3. **Always include `data-testid`** - Required for React Testing Library queries
4. **Never mutate state directly** - Use immutable updates (Redux Toolkit uses Immer)
5. **Always handle loading/error states** - RTK Query provides these automatically
6. **Never inline event handlers with complex logic** - Extract to named functions
7. **Always use `React.FC` type annotation** - Ensures proper typing for props and return value
8. **Never fetch data in components directly** - Use RTK Query hooks
9. **Always use MUI components** - Don't create custom HTML buttons, inputs, etc.
10. **Never hardcode colors/spacing** - Use MUI theme or CSS custom properties

### Quick Reference

**Common Commands:**
```bash
npm run dev              # Start dev server (http://localhost:5173)
npm run build            # Build for production
npm run preview          # Preview production build
npm run test             # Run unit tests
npm run test:e2e         # Run Playwright E2E tests
npm run generate-client  # Regenerate OpenAPI client
npm run lint             # Run ESLint
npm run format           # Run Prettier
```

**Key Import Patterns:**
```typescript
// Redux hooks (always use typed hooks)
import { useAppDispatch, useAppSelector } from '@/store/hooks';

// RTK Query hooks
import { useGetWorkflowsQuery, useCreateWorkflowMutation } from '@/features/workflows/workflowsApi';

// Material-UI components
import { Box, Button, Typography, TextField } from '@mui/material';

// React Router
import { useNavigate, useParams, Link } from 'react-router-dom';
```

**File Naming Conventions:**
- Components: `PascalCase.tsx` (e.g., `TaskKanbanBoard.tsx`)
- Utilities: `camelCase.ts` (e.g., `formatters.ts`)
- Tests: `PascalCase.test.tsx` (e.g., `TaskCard.test.tsx`)
- Styles: `kebab-case.css` (e.g., `global-styles.css`)

**Project-Specific Patterns:**
- All API calls go through RTK Query (never use `fetch` or `axios` directly in components)
- Session timeout is 15 minutes (managed by backend, tracked in Redux)
- All forms use React Hook Form + Yup validation
- All date formatting uses `date-fns` (not `moment` or `dayjs`)
- All icons use Material Icons from `@mui/icons-material`

---

## Document Information

**Version:** 1.0
**Status:** Complete
**Date:** 2025-10-30
**Author:** Winston (Architect)

**Document Scope:**
This frontend architecture document defines the complete technical blueprint for the Employee Lifecycle Management System frontend. It covers:

- ✅ Vite React-TypeScript starter template selection
- ✅ Complete technology stack (24 technologies with exact versions)
- ✅ Feature-based project structure with detailed file organization
- ✅ Component standards with TypeScript templates
- ✅ Redux Toolkit + RTK Query state management patterns
- ✅ API integration with auto-generated OpenAPI client
- ✅ React Router configuration with protected routes
- ✅ Material-UI styling guidelines with Magna BC theming
- ✅ Testing strategy with Jest + React Testing Library + Playwright
- ✅ Environment configuration for development and production
- ✅ Critical coding rules and quick reference for developers

**Ready for Development:** ✅
All frontend architecture decisions are finalized and align with the backend architecture document (docs/architecture.md).

**Next Steps:**
1. Initialize Vite project: `npm create vite@latest frontend -- --template react-ts`
2. Install dependencies from Technology Stack Table
3. Configure Redux store and RTK Query APIs
4. Set up Material-UI theme with Magna BC branding
5. Implement authentication flow and protected routing
6. Begin development following Epic sequence from PRD

---

**End of Document**
