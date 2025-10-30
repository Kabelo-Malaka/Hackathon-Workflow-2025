import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { CircularProgress, Box } from '@mui/material';
import { LoginPage } from './components/auth/LoginPage';
import { DashboardPage } from './components/dashboard/DashboardPage';
import { UserManagementPage } from './components/users/UserManagementPage';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { AppLayout } from './components/common/AppLayout';
import { useCheckSessionQuery } from './features/auth/authApi';
import { setUser, clearUser, setLoading } from './features/auth/authSlice';

function App() {
  const dispatch = useDispatch();
  const { data: user, isLoading, isError } = useCheckSessionQuery();

  // Task 5: Session persistence - check session on app load
  useEffect(() => {
    if (isLoading) {
      dispatch(setLoading(true));
    } else if (isError) {
      // No active session
      dispatch(clearUser());
    } else if (user) {
      // Valid session exists
      dispatch(setUser(user));
    }
  }, [user, isLoading, isError, dispatch]);

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

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <AppLayout>
                <DashboardPage />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/users"
          element={
            <ProtectedRoute>
              <AppLayout>
                <UserManagementPage />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
