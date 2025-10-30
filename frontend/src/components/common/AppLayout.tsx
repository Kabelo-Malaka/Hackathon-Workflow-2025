import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  Box,
} from '@mui/material';
import { useLogoutMutation } from '../../features/auth/authApi';
import { clearUser } from '../../features/auth/authSlice';
import type { RootState } from '../../store';

interface AppLayoutProps {
  children: React.ReactNode;
}

export const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [logout] = useLogoutMutation();
  const { user } = useSelector((state: RootState) => state.auth);

  const handleLogout = async () => {
    try {
      await logout().unwrap();
    } catch (error) {
      // Log error but still clear local state
      console.error('Logout error:', error);
    } finally {
      // Clear auth state and redirect
      dispatch(clearUser());
      navigate('/login');
    }
  };

  return (
    <Box>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Employee Lifecycle Management
          </Typography>
          {user && (
            <Typography variant="body2" sx={{ mr: 2 }}>
              {user.username} ({user.role})
            </Typography>
          )}
          <Button color="inherit" onClick={handleLogout}>
            Logout
          </Button>
        </Toolbar>
      </AppBar>
      <Box>{children}</Box>
    </Box>
  );
};
