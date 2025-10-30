# 4. Component Standards

This document defines component development standards that ensure all React components follow consistent patterns for type safety, accessibility, and maintainability. These standards apply to all components, including Sally's 10 core components from the UX specification.

**Alignment with Sally's UX Specification:**
- ✅ All components implement WCAG 2.1 AA accessibility requirements (semantic HTML, ARIA labels, keyboard navigation)
- ✅ Components use MUI base components customized with Magna BC branding (Primary Blue #1976d2, 8px spacing grid)
- ✅ Responsive design patterns support 4 breakpoints (Mobile 0-599px, Tablet 600-959px, Desktop 960-1279px, Wide 1280px+)
- ✅ Components include proper TypeScript interfaces matching Sally's 10 core component specifications
- ✅ All interactive components support keyboard navigation and screen readers

**Sally's 10 Core Components:**
1. **Button** (`src/components/common/Button.tsx`) - Primary, Secondary, Tertiary variants with 44×44px touch targets
2. **WorkflowCard** (`src/features/workflows/WorkflowCard.tsx`) - Displays workflow instances with status, progress, assignees
3. **TaskChecklist** (`src/features/tasks/TaskChecklist.tsx`) - Interactive checklist with real-time completion tracking
4. **KanbanBoard** (`src/features/dashboard/TaskKanbanBoard.tsx`) - Drag-and-drop task management with status columns
5. **FilterPanel** (`src/features/dashboard/FilterPanel.tsx`) - Multi-criteria filtering (status, assignee, date, priority)
6. **TemplateFormBuilder** (`src/features/templates/TemplateFormBuilder.tsx`) - Dynamic form creation with field types
7. **ConditionalRuleEditor** (`src/features/templates/ConditionalRuleEditor.tsx`) - If/then conditional logic UI
8. **ProgressTracker** (`src/components/common/ProgressTracker.tsx`) - Stepper component for workflow stages
9. **StatusBadge** (`src/components/common/StatusBadge.tsx`) - Visual status indicators (success, warning, error)
10. **DataTable** (`src/components/common/DataTable.tsx`) - Sortable, paginated tables with accessibility support

## Component Template

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

## Component Standards Checklist

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

## Naming Conventions

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

## Import Organization

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
