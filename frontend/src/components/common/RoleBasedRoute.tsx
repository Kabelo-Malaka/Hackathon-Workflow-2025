/**
 * Role-Based Route Component
 * Protects routes based on user roles
 * Story 3.7: Initiate Workflow UI
 */

import React from 'react';
import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '../../store';
import { CircularProgress, Box, Alert } from '@mui/material';

interface RoleBasedRouteProps {
  children: React.ReactNode;
  allowedRoles: string[];
}

/**
 * Component that restricts access to routes based on user role
 */
export const RoleBasedRoute: React.FC<RoleBasedRouteProps> = ({
  children,
  allowedRoles,
}) => {
  const { user, isAuthenticated, isLoading } = useSelector(
    (state: RootState) => state.auth
  );

  if (isLoading) {
    return (
      <Box
        display="flex"
        justifyContent="center"
        alignItems="center"
        minHeight="100vh"
      >
        <CircularProgress />
      </Box>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (!allowedRoles.includes(user.role)) {
    return (
      <Box
        display="flex"
        justifyContent="center"
        alignItems="center"
        minHeight="100vh"
        p={2}
      >
        <Alert severity="error">
          Access denied. You do not have permission to view this page.
        </Alert>
      </Box>
    );
  }

  return <>{children}</>;
};
