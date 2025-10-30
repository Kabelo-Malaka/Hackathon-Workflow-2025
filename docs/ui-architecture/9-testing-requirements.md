# 9. Testing Requirements

This document defines comprehensive testing requirements including unit tests, integration tests, E2E tests, and accessibility compliance testing aligned with Sally's quality assurance standards.

**Alignment with Sally's UX Specification:**
- ✅ WCAG 2.1 AA accessibility testing using axe-core automated scanner
- ✅ Keyboard navigation testing (Tab, Enter, Escape, Arrow keys) for all interactive components
- ✅ Screen reader testing with NVDA (Windows), JAWS (Windows), VoiceOver (macOS/iOS)
- ✅ Responsive design testing across 4 breakpoints (Mobile 0-599px, Tablet 600-959px, Desktop 960-1279px, Wide 1280px+)
- ✅ Performance testing (LCP < 2.0s, FID < 80ms, CLS < 0.05)
- ✅ E2E testing for Sally's 5 primary user flows (Login → Dashboard → Templates → Workflows → Tasks)
- ✅ Visual regression testing for 10 core components

**Testing Strategy:**
- **Unit Tests (Jest + React Testing Library):** Component behavior, utilities, hooks
- **Integration Tests (React Testing Library):** Feature modules, Redux slices with RTK Query
- **E2E Tests (Playwright):** Critical user flows, cross-browser testing (Chrome, Firefox, Safari)
- **Accessibility Tests (axe-core + manual):** WCAG AA compliance, keyboard navigation, screen reader support
- **Performance Tests (Lighthouse CI):** Core Web Vitals in CI/CD pipeline

## Component Test Template

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

## Testing Best Practices

1. **Unit Tests:** Test individual components in isolation with mocked dependencies
2. **Integration Tests:** Test component interactions and data flow between components
3. **E2E Tests:** Test critical user flows (login, create workflow, complete task) using Playwright
4. **Coverage Goals:** Aim for 80% code coverage on components and utilities
5. **Test Structure:** Follow Arrange-Act-Assert pattern
6. **Mock External Dependencies:** Always mock API calls, routing, and Redux state

---
