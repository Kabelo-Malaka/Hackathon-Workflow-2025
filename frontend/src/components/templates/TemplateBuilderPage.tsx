import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, Controller, useFieldArray } from 'react-hook-form';
import {
  Container,
  Box,
  Typography,
  Paper,
  Stack,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Button,
  Alert,
  Snackbar,
  FormHelperText,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
} from '@mui/material';
import { useSelector } from 'react-redux';
import type { RootState } from '../../store';
import { useCreateTemplateMutation, useGetTemplateByIdQuery, useUpdateTemplateMutation } from '../../features/templates/templatesApi';
import { TaskFormItem } from './TaskFormItem';

// Form interfaces
export interface TaskForm {
  taskName: string;
  description: string;
  assignedRole: 'HR_ADMIN' | 'LINE_MANAGER' | 'TECH_SUPPORT' | 'ADMINISTRATOR';
  isParallel: boolean;
  dependencyTaskId: string | null;
}

export interface TemplateBuilderForm {
  name: string;
  description: string;
  type: 'ONBOARDING' | 'OFFBOARDING';
  tasks: TaskForm[];
}

export const TemplateBuilderPage: React.FC = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEditMode = !!id;

  const { user } = useSelector((state: RootState) => state.auth);

  // Check if user has permission (HR_ADMIN or ADMINISTRATOR)
  const hasPermission = user?.role === 'HR_ADMIN' || user?.role === 'ADMINISTRATOR';

  // State
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });
  const [cancelDialogOpen, setCancelDialogOpen] = useState(false);

  // API hooks
  const [createTemplate, { isLoading: isCreating }] = useCreateTemplateMutation();
  const [updateTemplate, { isLoading: isUpdating }] = useUpdateTemplateMutation();
  const { isLoading: isLoadingTemplate } = useGetTemplateByIdQuery(id!, { skip: !id });

  // Form setup
  const { control, handleSubmit, formState: { errors, isDirty } } = useForm<TemplateBuilderForm>({
    defaultValues: {
      name: '',
      description: '',
      type: 'ONBOARDING',
      tasks: [],
    },
  });

  // Field array for tasks
  const { fields, append, remove, move } = useFieldArray({
    control,
    name: 'tasks',
  });

  // Redirect if no permission
  if (!hasPermission) {
    return (
      <Container maxWidth="md">
        <Box sx={{ mt: 4 }}>
          <Alert severity="error">
            Access denied: You do not have permission to access the template builder.
          </Alert>
        </Box>
      </Container>
    );
  }

  // Loading state for edit mode
  if (isEditMode && isLoadingTemplate) {
    return (
      <Container maxWidth="md">
        <Box sx={{ mt: 4, textAlign: 'center' }}>
          <Typography>Loading template...</Typography>
        </Box>
      </Container>
    );
  }

  // Transform form data to API request format
  const transformToCreateRequest = (formData: TemplateBuilderForm) => {
    let currentSequenceOrder = 1;

    const tasks = formData.tasks.map((task, index) => {
      // If parallel and not first task, use previous task's sequence order
      if (task.isParallel && index > 0) {
        // Keep same sequence order as previous task
      } else {
        // New sequence order
        currentSequenceOrder = index + 1;
      }

      return {
        taskName: task.taskName,
        description: task.description || undefined,
        assignedRole: task.assignedRole,
        sequenceOrder: currentSequenceOrder,
        isParallel: task.isParallel,
        // For new templates, dependencyTaskId must be null since tasks don't have IDs yet
        // Backend will handle dependencies based on sequence order
        dependencyTaskId: null,
      };
    });

    return {
      name: formData.name,
      description: formData.description || undefined,
      type: formData.type,
      tasks,
    };
  };

  // Submit handler
  const onSubmit = async (data: TemplateBuilderForm) => {
    try {
      const request = transformToCreateRequest(data);

      if (isEditMode && id) {
        await updateTemplate({ id, ...request, isActive: true }).unwrap();
        setSnackbar({ open: true, message: 'Template updated successfully', severity: 'success' });
      } else {
        await createTemplate(request).unwrap();
        setSnackbar({ open: true, message: 'Template created successfully', severity: 'success' });
      }

      setTimeout(() => navigate('/templates'), 500);
    } catch (err: any) {
      setErrorMessage(
        err.status === 400
          ? 'Validation error: Please check all fields'
          : err.status === 403
          ? 'Access denied: You do not have permission to create templates'
          : 'Failed to save template. Please try again.'
      );
    }
  };

  // Add task handler
  const handleAddTask = () => {
    append({
      taskName: '',
      description: '',
      assignedRole: 'HR_ADMIN',
      isParallel: false,
      dependencyTaskId: null,
    });
  };

  // Move task handlers
  const handleMoveUp = (index: number) => {
    if (index > 0) {
      move(index, index - 1);
    }
  };

  const handleMoveDown = (index: number) => {
    if (index < fields.length - 1) {
      move(index, index + 1);
    }
  };

  // Remove task handler
  const handleRemove = (index: number) => {
    if (window.confirm('Are you sure you want to remove this task?')) {
      remove(index);
    }
  };

  // Cancel handler
  const handleCancel = () => {
    if (isDirty) {
      setCancelDialogOpen(true);
    } else {
      navigate('/templates');
    }
  };

  const handleConfirmCancel = () => {
    setCancelDialogOpen(false);
    navigate('/templates');
  };

  return (
    <Container maxWidth="md">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" gutterBottom>
          {isEditMode ? 'Edit Workflow Template' : 'Create Workflow Template'}
        </Typography>

        {errorMessage && (
          <Alert severity="error" sx={{ mb: 3 }} onClose={() => setErrorMessage('')}>
            {errorMessage}
          </Alert>
        )}

        <form onSubmit={handleSubmit(onSubmit)}>
          {/* Template Information Section */}
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
              Template Information
            </Typography>
            <Stack spacing={2}>
              {/* Template Name */}
              <Controller
                name="name"
                control={control}
                rules={{
                  required: 'Template name is required',
                  maxLength: {
                    value: 255,
                    message: 'Template name must not exceed 255 characters',
                  },
                }}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Template Name"
                    required
                    error={!!errors.name}
                    helperText={errors.name?.message}
                    fullWidth
                  />
                )}
              />

              {/* Description */}
              <Controller
                name="description"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Description"
                    multiline
                    rows={4}
                    fullWidth
                  />
                )}
              />

              {/* Type */}
              <Controller
                name="type"
                control={control}
                rules={{ required: 'Type is required' }}
                render={({ field }) => (
                  <FormControl fullWidth required error={!!errors.type}>
                    <InputLabel>Type</InputLabel>
                    <Select {...field} label="Type">
                      <MenuItem value="ONBOARDING">Onboarding</MenuItem>
                      <MenuItem value="OFFBOARDING">Offboarding</MenuItem>
                    </Select>
                    {errors.type && <FormHelperText>{errors.type.message}</FormHelperText>}
                  </FormControl>
                )}
              />
            </Stack>
          </Paper>

          {/* Tasks Section */}
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
              Tasks
            </Typography>

            {errors.tasks?.root && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {errors.tasks.root.message}
              </Alert>
            )}

            {fields.length === 0 ? (
              <Box sx={{ textAlign: 'center', py: 3 }}>
                <Typography variant="body2" color="text.secondary">
                  No tasks yet. Click "Add Task" to get started.
                </Typography>
              </Box>
            ) : (
              <Stack spacing={2} sx={{ mb: 2 }}>
                {fields.map((field, index) => (
                  <TaskFormItem
                    key={field.id}
                    control={control}
                    index={index}
                    onMoveUp={handleMoveUp}
                    onMoveDown={handleMoveDown}
                    onRemove={handleRemove}
                    allTasks={fields}
                    errors={errors}
                    isFirst={index === 0}
                    isLast={index === fields.length - 1}
                  />
                ))}
              </Stack>
            )}

            <Button
              variant="outlined"
              onClick={handleAddTask}
              fullWidth
              aria-label="Add new task to template"
            >
              Add Task
            </Button>
          </Paper>

          {/* Action Buttons */}
          <Stack direction="row" spacing={2} justifyContent="flex-end">
            <Button variant="text" onClick={handleCancel} aria-label="Cancel template creation">
              Cancel
            </Button>
            <Button
              variant="contained"
              type="submit"
              disabled={isCreating || isUpdating}
              aria-label={isEditMode ? 'Update template' : 'Save template'}
            >
              {isEditMode ? 'Update Template' : 'Save Template'}
            </Button>
          </Stack>
        </form>

        {/* Cancel Confirmation Dialog */}
        <Dialog open={cancelDialogOpen} onClose={() => setCancelDialogOpen(false)}>
          <DialogTitle>Unsaved Changes</DialogTitle>
          <DialogContent>
            <DialogContentText>
              You have unsaved changes. Are you sure you want to leave?
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCancelDialogOpen(false)}>No, Stay</Button>
            <Button onClick={handleConfirmCancel} color="error">
              Yes, Leave
            </Button>
          </DialogActions>
        </Dialog>

        {/* Success/Error Snackbar */}
        <Snackbar
          open={snackbar.open}
          autoHideDuration={3000}
          onClose={() => setSnackbar({ ...snackbar, open: false })}
        >
          <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
            {snackbar.message}
          </Alert>
        </Snackbar>
      </Box>
    </Container>
  );
};
