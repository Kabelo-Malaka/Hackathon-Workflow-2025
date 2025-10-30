# 3. Project Structure

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

## Key Structure Decisions

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
