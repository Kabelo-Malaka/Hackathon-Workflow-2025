import { useMemo } from 'react';
import { Controller } from 'react-hook-form';
import type { Control, FieldErrors, FieldArrayWithId } from 'react-hook-form';
import {
  Card,
  CardContent,
  Box,
  Typography,
  Stack,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  IconButton,
  Checkbox,
  FormControlLabel,
  FormHelperText,
} from '@mui/material';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import DeleteIcon from '@mui/icons-material/Delete';
import type { TemplateBuilderForm } from './TemplateBuilderPage';

interface TaskFormItemProps {
  control: Control<TemplateBuilderForm>;
  index: number;
  onMoveUp: (index: number) => void;
  onMoveDown: (index: number) => void;
  onRemove: (index: number) => void;
  allTasks: FieldArrayWithId<TemplateBuilderForm, "tasks", "id">[];
  errors: FieldErrors<TemplateBuilderForm>;
  isFirst: boolean;
  isLast: boolean;
}

export const TaskFormItem: React.FC<TaskFormItemProps> = ({
  control,
  index,
  onMoveUp,
  onMoveDown,
  onRemove,
  allTasks,
  errors,
  isFirst,
  isLast,
}) => {
  // Get dependency options (only tasks before current task)
  // Memoized to avoid recalculating on every render
  const dependencyOptions = useMemo(() => {
    return allTasks
      .slice(0, index)
      .map((task, taskIndex) => {
        // FieldArrayWithId includes both id and the field values
        const taskData = task as FieldArrayWithId<TemplateBuilderForm, "tasks", "id"> & { taskName?: string };
        return {
          value: taskIndex.toString(),
          label: `Task ${taskIndex + 1}: ${taskData.taskName || 'Untitled'}`,
        };
      });
  }, [allTasks, index]);

  const taskError = errors.tasks?.[index];

  return (
    <Card sx={{ mb: 2 }}>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="subtitle1" fontWeight="bold">
            Task {index + 1}
          </Typography>
          <Box>
            <IconButton
              onClick={() => onMoveUp(index)}
              disabled={isFirst}
              size="small"
              title="Move Up"
              aria-label={`Move task ${index + 1} up`}
            >
              <ArrowUpwardIcon />
            </IconButton>
            <IconButton
              onClick={() => onMoveDown(index)}
              disabled={isLast}
              size="small"
              title="Move Down"
              aria-label={`Move task ${index + 1} down`}
            >
              <ArrowDownwardIcon />
            </IconButton>
            <IconButton
              onClick={() => onRemove(index)}
              color="error"
              size="small"
              title="Remove Task"
              aria-label={`Remove task ${index + 1}`}
            >
              <DeleteIcon />
            </IconButton>
          </Box>
        </Box>

        <Stack spacing={2}>
          {/* Task Name */}
          <Controller
            name={`tasks.${index}.taskName`}
            control={control}
            rules={{
              required: 'Task name is required',
              maxLength: {
                value: 255,
                message: 'Task name must not exceed 255 characters',
              },
            }}
            render={({ field }) => (
              <TextField
                {...field}
                label="Task Name"
                required
                error={!!taskError?.taskName}
                helperText={taskError?.taskName?.message}
                fullWidth
              />
            )}
          />

          {/* Task Description */}
          <Controller
            name={`tasks.${index}.description`}
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Description"
                multiline
                rows={2}
                fullWidth
              />
            )}
          />

          {/* Assigned Role */}
          <Controller
            name={`tasks.${index}.assignedRole`}
            control={control}
            rules={{ required: 'Assigned role is required' }}
            render={({ field }) => (
              <FormControl fullWidth required error={!!taskError?.assignedRole}>
                <InputLabel>Assigned Role</InputLabel>
                <Select {...field} label="Assigned Role">
                  <MenuItem value="HR_ADMIN">HR Admin</MenuItem>
                  <MenuItem value="LINE_MANAGER">Line Manager</MenuItem>
                  <MenuItem value="TECH_SUPPORT">Tech Support</MenuItem>
                  <MenuItem value="ADMINISTRATOR">Administrator</MenuItem>
                </Select>
                {taskError?.assignedRole && (
                  <FormHelperText>{taskError.assignedRole.message}</FormHelperText>
                )}
              </FormControl>
            )}
          />

          {/* Run in Parallel */}
          <Controller
            name={`tasks.${index}.isParallel`}
            control={control}
            render={({ field: { value, onChange, ...field } }) => (
              <Box>
                <FormControlLabel
                  control={
                    <Checkbox
                      {...field}
                      checked={value}
                      onChange={(e) => onChange(e.target.checked)}
                    />
                  }
                  label="Run in Parallel"
                />
                {value && (
                  <Typography variant="caption" color="text.secondary" display="block" sx={{ ml: 4 }}>
                    This task will run in parallel with other tasks at the same sequence order
                  </Typography>
                )}
              </Box>
            )}
          />

          {/* Task Dependency */}
          <Controller
            name={`tasks.${index}.dependencyTaskId`}
            control={control}
            render={({ field }) => (
              <FormControl fullWidth>
                <InputLabel>Task Dependency</InputLabel>
                <Select
                  {...field}
                  value={field.value || ''}
                  onChange={(e) => field.onChange(e.target.value || null)}
                  label="Task Dependency"
                >
                  <MenuItem value="">No Dependency</MenuItem>
                  {dependencyOptions.map((option: { value: string; label: string }) => (
                    <MenuItem key={option.value} value={option.value}>
                      {option.label}
                    </MenuItem>
                  ))}
                </Select>
                <FormHelperText>
                  Select a previous task that must be completed before this task
                </FormHelperText>
              </FormControl>
            )}
          />
        </Stack>
      </CardContent>
    </Card>
  );
};
