import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import {
  Paper,
  Typography,
  Button,
  TextField,
  Box,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  CircularProgress,
  Snackbar,
  Alert,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import BlockIcon from '@mui/icons-material/Block';
import type { RootState } from '../../store';
import { useGetUsersQuery } from '../../features/users/usersApi';
import { CreateUserModal } from './CreateUserModal';

export const UserManagementPage = () => {
  const navigate = useNavigate();
  const { user } = useSelector((state: RootState) => state.auth);
  const [searchFilter, setSearchFilter] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' as 'success' | 'error' | 'info' });

  const { data: users, isLoading, error, refetch } = useGetUsersQuery();

  // Role guard - redirect if not HR_ADMIN or ADMINISTRATOR
  useEffect(() => {
    if (user && user.role !== 'HR_ADMIN' && user.role !== 'ADMINISTRATOR') {
      setSnackbar({
        open: true,
        message: 'Access denied: HR Admin or Administrator role required',
        severity: 'error',
      });
      setTimeout(() => navigate('/dashboard'), 2000);
    }
  }, [user, navigate]);

  // Client-side filtering by username or email
  const filteredUsers = useMemo(() => {
    if (!users) return [];
    if (!searchFilter) return users;

    const searchLower = searchFilter.toLowerCase();
    return users.filter(
      (u) =>
        u.username.toLowerCase().includes(searchLower) ||
        u.email.toLowerCase().includes(searchLower)
    );
  }, [users, searchFilter]);

  const handleModalClose = () => {
    setModalOpen(false);
  };

  const handleCreateSuccess = () => {
    setModalOpen(false);
    refetch();
    setSnackbar({
      open: true,
      message: 'User created successfully',
      severity: 'success',
    });
  };

  const handlePlaceholderAction = (actionName: string) => {
    setSnackbar({
      open: true,
      message: `${actionName} not implemented yet`,
      severity: 'info',
    });
  };

  const handleCloseSnackbar = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  // Show unauthorized message if not HR_ADMIN or ADMINISTRATOR
  if (user && user.role !== 'HR_ADMIN' && user.role !== 'ADMINISTRATOR') {
    return (
      <Box sx={{ mt: 4 }}>
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h5" color="error">
            Access Denied
          </Typography>
          <Typography variant="body1" sx={{ mt: 2 }}>
            This page is only accessible to HR Administrators and Administrators.
          </Typography>
        </Paper>
      </Box>
    );
  }

  return (
    <Box sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" component="h1">
          User Management
        </Typography>
        <Button
          variant="contained"
          color="primary"
          onClick={() => setModalOpen(true)}
        >
          Create User
        </Button>
      </Box>

      <Paper sx={{ p: 3 }}>
        <Box sx={{ mb: 3 }}>
          <TextField
            fullWidth
            label="Search by username or email"
            variant="outlined"
            value={searchFilter}
            onChange={(e) => setSearchFilter(e.target.value)}
          />
        </Box>

        {isLoading ? (
          <Box display="flex" justifyContent="center" alignItems="center" minHeight="300px">
            <CircularProgress />
          </Box>
        ) : error ? (
          <Box textAlign="center" py={4}>
            <Typography color="error">
              Failed to load users. Please try again.
            </Typography>
          </Box>
        ) : filteredUsers.length === 0 ? (
          <Box textAlign="center" py={4}>
            <Typography color="text.secondary">
              {searchFilter ? 'No users found matching your search.' : 'No users exist yet.'}
            </Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell><strong>Username</strong></TableCell>
                  <TableCell><strong>Email</strong></TableCell>
                  <TableCell><strong>Role</strong></TableCell>
                  <TableCell><strong>Status</strong></TableCell>
                  <TableCell><strong>Actions</strong></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredUsers.map((u) => (
                  <TableRow key={u.id}>
                    <TableCell>{u.username}</TableCell>
                    <TableCell>{u.email}</TableCell>
                    <TableCell>{u.role}</TableCell>
                    <TableCell>
                      <Chip
                        label={u.isActive ? 'Active' : 'Inactive'}
                        color={u.isActive ? 'success' : 'default'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      <IconButton
                        size="small"
                        color="primary"
                        onClick={() => handlePlaceholderAction('Edit')}
                        title="Edit user"
                      >
                        <EditIcon />
                      </IconButton>
                      <IconButton
                        size="small"
                        color="warning"
                        onClick={() => handlePlaceholderAction('Deactivate')}
                        title="Deactivate user"
                      >
                        <BlockIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>

      <CreateUserModal
        open={modalOpen}
        onClose={handleModalClose}
        onSuccess={handleCreateSuccess}
      />

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={handleCloseSnackbar} severity={snackbar.severity} sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};
