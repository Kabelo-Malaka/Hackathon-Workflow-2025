import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  List,
  ListItem,
  ListItemText,
  Chip,
  Stack,
  Box,
  CircularProgress,
  Alert,
  Divider,
} from '@mui/material';
import { useGetTemplateByIdQuery } from '../../features/templates/templatesApi';

interface TemplateDetailModalProps {
  open: boolean;
  templateId: string;
  onClose: () => void;
}

export const TemplateDetailModal: React.FC<TemplateDetailModalProps> = ({
  open,
  templateId,
  onClose,
}) => {
  const { data: template, isLoading, error } = useGetTemplateByIdQuery(templateId, {
    skip: !open, // Only fetch when modal is open
  });

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      aria-labelledby="template-detail-dialog-title"
    >
      <DialogTitle id="template-detail-dialog-title">
        {template ? template.name : 'Template Details'}
      </DialogTitle>

      <DialogContent dividers>
        {/* Loading state */}
        {isLoading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        )}

        {/* Error state */}
        {error && (
          <Alert severity="error">
            Failed to load template details. Please try again.
          </Alert>
        )}

        {/* Template details */}
        {template && (
          <Box>
            {/* Template metadata */}
            <Box sx={{ mb: 3 }}>
              <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
                <Chip
                  label={template.type}
                  color={template.type === 'ONBOARDING' ? 'primary' : 'secondary'}
                  size="small"
                />
                <Chip
                  label={template.isActive ? 'Active' : 'Inactive'}
                  color={template.isActive ? 'success' : 'default'}
                  size="small"
                />
              </Stack>

              {template.description && (
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  {template.description}
                </Typography>
              )}
            </Box>

            <Divider sx={{ my: 2 }} />

            {/* Tasks list */}
            <Typography variant="h6" gutterBottom>
              Tasks ({template.tasks.length})
            </Typography>

            {template.tasks.length === 0 ? (
              <Typography variant="body2" color="text.secondary">
                No tasks defined for this template.
              </Typography>
            ) : (
              <List>
                {template.tasks
                  .sort((a, b) => (a.sequenceOrder || 0) - (b.sequenceOrder || 0))
                  .map((task) => (
                    <ListItem
                      key={task.id}
                      sx={{
                        border: '1px solid',
                        borderColor: 'divider',
                        borderRadius: 1,
                        mb: 1,
                      }}
                    >
                      <ListItemText
                        primary={
                          <Stack direction="row" spacing={1} alignItems="center">
                            <Chip
                              label={`#${task.sequenceOrder}`}
                              size="small"
                              color="primary"
                              variant="outlined"
                            />
                            <Typography variant="subtitle2">{task.taskName}</Typography>
                            {task.isParallel && (
                              <Chip label="Parallel" size="small" color="info" />
                            )}
                          </Stack>
                        }
                        secondary={
                          <Box sx={{ mt: 1 }}>
                            {task.description && (
                              <Typography variant="body2" color="text.secondary" gutterBottom>
                                {task.description}
                              </Typography>
                            )}
                            <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 1 }}>
                              <Typography variant="caption" color="text.secondary">
                                Assigned to: <strong>{task.assignedRole}</strong>
                              </Typography>
                              {task.dependencyTaskId && (
                                <>
                                  <Typography variant="caption" color="text.secondary">
                                    •
                                  </Typography>
                                  <Typography variant="caption" color="text.secondary">
                                    Depends on: Task{' '}
                                    {template.tasks.find((t) => t.id === task.dependencyTaskId)
                                      ?.sequenceOrder || 'N/A'}
                                  </Typography>
                                </>
                              )}
                            </Stack>
                          </Box>
                        }
                      />
                    </ListItem>
                  ))}
              </List>
            )}
          </Box>
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  );
};
