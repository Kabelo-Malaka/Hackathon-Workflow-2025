import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  CircularProgress,
  Stack,
  ToggleButtonGroup,
  ToggleButton,
  Grid,
  Alert,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { useGetTemplatesQuery } from '../../features/templates/templatesApi';
import { TemplateCard } from './TemplateCard';
import { TemplateDetailModal } from './TemplateDetailModal';

export const TemplateLibraryPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: templates, isLoading, error } = useGetTemplatesQuery();

  // Filter state
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [statusFilter, setStatusFilter] = useState<string>('all');

  // Modal state
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [selectedTemplateId, setSelectedTemplateId] = useState<string | null>(null);

  // Filter templates using useMemo for optimization
  const filteredTemplates = useMemo(() => {
    let filtered = templates || [];

    if (typeFilter !== 'all') {
      filtered = filtered.filter((t) => t.type === typeFilter);
    }

    if (statusFilter !== 'all') {
      if (statusFilter === 'active') {
        filtered = filtered.filter((t) => t.isActive === true);
      } else if (statusFilter === 'inactive') {
        filtered = filtered.filter((t) => t.isActive === false);
      }
    }

    return filtered;
  }, [templates, typeFilter, statusFilter]);

  // Action handlers
  const handleViewDetails = (id: string) => {
    setSelectedTemplateId(id);
    setDetailModalOpen(true);
  };

  const handleEdit = (id: string) => {
    navigate(`/templates/edit/${id}`);
  };

  const handleCreateNew = () => {
    navigate('/templates/new');
  };

  const handleCloseDetailModal = () => {
    setDetailModalOpen(false);
    setSelectedTemplateId(null);
  };

  // Loading state
  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  // Error state
  if (error) {
    return (
      <Box sx={{ mt: 4 }}>
        <Alert severity="error">
          Failed to load templates. Please try again later.
        </Alert>
      </Box>
    );
  }

  // Empty state
  if (!templates || templates.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', mt: 8 }}>
        <Typography variant="h6" gutterBottom>
          No templates yet
        </Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Get started by creating your first workflow template
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={handleCreateNew}
          sx={{ mt: 2 }}
        >
          Create First Template
        </Button>
      </Box>
    );
  }

  return (
    <>
      <Box sx={{ mt: 4, mb: 4 }}>
        {/* Header */}
        <Typography variant="h4" gutterBottom>
          Template Library
        </Typography>

        {/* Filter controls and Create button */}
        <Box
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            mb: 3,
            flexWrap: 'wrap',
            gap: 2,
          }}
        >
          {/* Filters */}
          <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap', gap: 2 }}>
            {/* Type filter */}
            <Box>
              <Typography variant="caption" display="block" gutterBottom>
                Type
              </Typography>
              <ToggleButtonGroup
                value={typeFilter}
                exclusive
                onChange={(_event, newValue) => {
                  if (newValue !== null) {
                    setTypeFilter(newValue);
                  }
                }}
                size="small"
              >
                <ToggleButton value="all">All</ToggleButton>
                <ToggleButton value="ONBOARDING">Onboarding</ToggleButton>
                <ToggleButton value="OFFBOARDING">Offboarding</ToggleButton>
              </ToggleButtonGroup>
            </Box>

            {/* Status filter */}
            <Box>
              <Typography variant="caption" display="block" gutterBottom>
                Status
              </Typography>
              <ToggleButtonGroup
                value={statusFilter}
                exclusive
                onChange={(_event, newValue) => {
                  if (newValue !== null) {
                    setStatusFilter(newValue);
                  }
                }}
                size="small"
              >
                <ToggleButton value="all">All</ToggleButton>
                <ToggleButton value="active">Active</ToggleButton>
                <ToggleButton value="inactive">Inactive</ToggleButton>
              </ToggleButtonGroup>
            </Box>
          </Stack>

          {/* Create button */}
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={handleCreateNew}
          >
            Create New Template
          </Button>
        </Box>

        {/* Template grid */}
        {filteredTemplates.length === 0 ? (
          <Box sx={{ textAlign: 'center', mt: 4 }}>
            <Typography variant="body1" color="text.secondary">
              No templates match the selected filters
            </Typography>
          </Box>
        ) : (
          <Grid container spacing={3}>
            {filteredTemplates.map((template) => (
              <Grid item xs={12} sm={6} md={4} key={template.id}>
                <TemplateCard
                  template={template}
                  onViewDetails={handleViewDetails}
                  onEdit={handleEdit}
                />
              </Grid>
            ))}
          </Grid>
        )}
      </Box>

      {/* Detail modal */}
      {selectedTemplateId && (
        <TemplateDetailModal
          open={detailModalOpen}
          templateId={selectedTemplateId}
          onClose={handleCloseDetailModal}
        />
      )}
    </>
  );
};
