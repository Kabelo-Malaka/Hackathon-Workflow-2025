import React, { useState } from 'react';
import {
  Card,
  CardContent,
  CardActions,
  Typography,
  Chip,
  Stack,
  IconButton,
  Tooltip,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import EditIcon from '@mui/icons-material/Edit';
import ToggleOnIcon from '@mui/icons-material/ToggleOn';
import ToggleOffIcon from '@mui/icons-material/ToggleOff';
import { format } from 'date-fns';
import type { TemplateSummaryResponse } from '../../features/templates/templatesApi';
import { useUpdateTemplateMutation } from '../../features/templates/templatesApi';
import { ConfirmationDialog } from '../common/ConfirmationDialog';

interface TemplateCardProps {
  template: TemplateSummaryResponse;
  onViewDetails: (id: string) => void;
  onEdit: (id: string) => void;
}

export const TemplateCard: React.FC<TemplateCardProps> = ({
  template,
  onViewDetails,
  onEdit,
}) => {
  const [updateTemplate] = useUpdateTemplateMutation();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const formatDate = (dateString: string) => {
    try {
      return format(new Date(dateString), 'MMM dd, yyyy');
    } catch {
      return dateString;
    }
  };

  const handleToggleActive = () => {
    setConfirmOpen(true);
  };

  const handleConfirmToggle = async () => {
    try {
      // Note: We need to fetch full template details to update
      // For now, we'll just toggle isActive - backend will handle validation
      await updateTemplate({
        id: template.id,
        name: template.name,
        type: template.type,
        isActive: !template.isActive,
        tasks: [], // Will be replaced with actual tasks on backend
      }).unwrap();

      setConfirmOpen(false);
    } catch (error) {
      console.error('Failed to toggle template status:', error);
      setConfirmOpen(false);
    }
  };

  return (
    <>
      <Card
        sx={{
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          transition: 'transform 0.2s, box-shadow 0.2s',
          '&:hover': {
            transform: 'translateY(-4px)',
            boxShadow: 4,
          },
        }}
      >
        <CardContent sx={{ flexGrow: 1 }}>
          {/* Template name */}
          <Typography variant="h6" gutterBottom noWrap>
            {template.name}
          </Typography>

          {/* Type and status chips */}
          <Stack direction="row" spacing={1} sx={{ mt: 1, mb: 2 }}>
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

          {/* Task count */}
          <Typography variant="body2" color="text.secondary" gutterBottom>
            {template.taskCount} {template.taskCount === 1 ? 'task' : 'tasks'}
          </Typography>

          {/* Last updated */}
          <Typography variant="caption" color="text.secondary">
            Last updated: {formatDate(template.updatedAt)}
          </Typography>
        </CardContent>

        {/* Action buttons */}
        <CardActions sx={{ justifyContent: 'space-between', px: 2, pb: 2 }}>
          <Stack direction="row" spacing={1}>
            <Tooltip title="View Details">
              <IconButton
                size="small"
                onClick={() => onViewDetails(template.id)}
                color="primary"
              >
                <VisibilityIcon />
              </IconButton>
            </Tooltip>

            <Tooltip title="Edit Template">
              <IconButton
                size="small"
                onClick={() => onEdit(template.id)}
                color="primary"
              >
                <EditIcon />
              </IconButton>
            </Tooltip>
          </Stack>

          <Tooltip title={template.isActive ? 'Deactivate' : 'Activate'}>
            <IconButton
              size="small"
              onClick={handleToggleActive}
              color={template.isActive ? 'primary' : 'default'}
            >
              {template.isActive ? <ToggleOnIcon /> : <ToggleOffIcon />}
            </IconButton>
          </Tooltip>
        </CardActions>
      </Card>

      {/* Confirmation dialog */}
      <ConfirmationDialog
        open={confirmOpen}
        title={`${template.isActive ? 'Deactivate' : 'Activate'} Template`}
        message={`Are you sure you want to ${
          template.isActive ? 'deactivate' : 'activate'
        } this template?`}
        onConfirm={handleConfirmToggle}
        onClose={() => setConfirmOpen(false)}
      />
    </>
  );
};
