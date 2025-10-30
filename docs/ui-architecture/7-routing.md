# 7. Routing

This document defines the application routing structure using React Router v6, with protected routes and role-based access control aligned with Sally's user flows and security requirements.

**Alignment with Sally's UX Specification:**
- ✅ Implements Sally's 5 primary user flows: Login → Dashboard → Templates → Workflows → Tasks
- ✅ Protected routes enforce authentication (redirect to /login if not authenticated)
- ✅ Role-based routes implement RBAC (HR_ADMIN, LINE_MANAGER, TECH_SUPPORT, ADMINISTRATOR)
- ✅ Lazy loading reduces initial bundle size (supports < 300KB target and LCP < 2.0s)
- ✅ Breadcrumb navigation supports Sally's information architecture
- ✅ 404 and 403 error pages match Sally's error state designs

**Route Access Control:**
- **/login** - Public (unauthenticated users only)
- **/dashboard** - All authenticated users
- **/templates** - HR_ADMIN and ADMINISTRATOR only (create/edit workflow templates)
- **/workflows** - All authenticated users (view workflows assigned to them or their team)
- **/tasks** - All authenticated users (complete tasks assigned to them)
- **/users** - ADMINISTRATOR only (user management)

## Route Configuration

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
