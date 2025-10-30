# 11. Frontend Developer Standards

## Critical Coding Rules

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

## Quick Reference

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
