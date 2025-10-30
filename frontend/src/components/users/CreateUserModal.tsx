import { useState, useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  CircularProgress,
  FormHelperText,
} from '@mui/material';
import { useCreateUserMutation } from '../../features/users/usersApi';
import type { CreateUserRequest } from '../../features/users/usersApi';

interface CreateUserModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

interface CreateUserFormData {
  username: string;
  email: string;
  password: string;
  role: 'HR_ADMIN' | 'LINE_MANAGER' | 'TECH_SUPPORT' | 'ADMINISTRATOR';
}

export const CreateUserModal: React.FC<CreateUserModalProps> = ({
  open,
  onClose,
  onSuccess,
}) => {
  const [apiError, setApiError] = useState('');
  const [createUser, { isLoading }] = useCreateUserMutation();

  const {
    control,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<CreateUserFormData>({
    defaultValues: {
      username: '',
      email: '',
      password: '',
      role: 'LINE_MANAGER',
    },
  });

  // Reset form and error when modal closes
  useEffect(() => {
    if (!open) {
      reset();
      setApiError('');
    }
  }, [open, reset]);

  const onSubmit = async (data: CreateUserFormData) => {
    setApiError('');

    try {
      await createUser(data as CreateUserRequest).unwrap();
      reset();
      onSuccess();
    } catch (error: any) {
      // Handle different error types
      if (error?.status === 409) {
        setApiError('Username or email already exists');
      } else if (error?.status === 400) {
        setApiError(error?.data?.message || 'Invalid input. Please check your data.');
      } else if (error?.status === 403) {
        setApiError('Access denied: You do not have permission to create users');
      } else {
        setApiError('Failed to create user. Please try again.');
      }
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit(onSubmit)}>
        <DialogTitle>Create New User</DialogTitle>
        <DialogContent>
          {apiError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {apiError}
            </Alert>
          )}

          <Controller
            name="username"
            control={control}
            rules={{
              required: 'Username is required',
              minLength: {
                value: 3,
                message: 'Username must be at least 3 characters',
              },
            }}
            render={({ field }) => (
              <TextField
                {...field}
                label="Username"
                fullWidth
                margin="normal"
                error={!!errors.username}
                helperText={errors.username?.message}
                disabled={isLoading}
                required
              />
            )}
          />

          <Controller
            name="email"
            control={control}
            rules={{
              required: 'Email is required',
              pattern: {
                value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                message: 'Invalid email address',
              },
            }}
            render={({ field }) => (
              <TextField
                {...field}
                label="Email"
                type="email"
                fullWidth
                margin="normal"
                error={!!errors.email}
                helperText={errors.email?.message}
                disabled={isLoading}
                required
              />
            )}
          />

          <Controller
            name="password"
            control={control}
            rules={{
              required: 'Password is required',
              minLength: {
                value: 6,
                message: 'Password must be at least 6 characters',
              },
            }}
            render={({ field }) => (
              <TextField
                {...field}
                label="Password"
                type="password"
                fullWidth
                margin="normal"
                error={!!errors.password}
                helperText={errors.password?.message}
                disabled={isLoading}
                required
              />
            )}
          />

          <Controller
            name="role"
            control={control}
            rules={{
              required: 'Role is required',
            }}
            render={({ field }) => (
              <FormControl fullWidth margin="normal" error={!!errors.role} required>
                <InputLabel id="role-label">Role</InputLabel>
                <Select
                  {...field}
                  labelId="role-label"
                  label="Role"
                  disabled={isLoading}
                >
                  <MenuItem value="HR_ADMIN">HR Admin</MenuItem>
                  <MenuItem value="LINE_MANAGER">Line Manager</MenuItem>
                  <MenuItem value="TECH_SUPPORT">Tech Support</MenuItem>
                  <MenuItem value="ADMINISTRATOR">Administrator</MenuItem>
                </Select>
                {errors.role && <FormHelperText>{errors.role.message}</FormHelperText>}
              </FormControl>
            )}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} disabled={isLoading}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            color="primary"
            disabled={isLoading}
            startIcon={isLoading ? <CircularProgress size={20} /> : null}
          >
            {isLoading ? 'Creating...' : 'Create User'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};
