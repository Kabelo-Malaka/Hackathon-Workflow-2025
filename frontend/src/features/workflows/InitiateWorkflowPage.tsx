/**
 * Initiate Workflow Page - HR Admin workflow initiation form
 * Story 3.7: Initiate Workflow UI
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import {
  Container,
  Paper,
  Box,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Button,
  Alert,
  Snackbar,
  FormHelperText,
  Checkbox,
  FormControlLabel,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { PageHeader } from '../../components/common/PageHeader';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import {
  useGetActiveTemplatesQuery,
  useGetTemplateByIdQuery,
  useInitiateWorkflowMutation,
} from './workflowsApi';
import type {
  InitiateWorkflowFormData,
  TemplateCustomField,
  ConditionalRule,
} from './types';

// Validation schema
const validationSchema = yup.object({
  templateId: yup.string().required('Template is required'),
  employeeName: yup.string().required('Employee name is required'),
  employeeEmail: yup
    .string()
    .email('Invalid email address')
    .required('Employee email is required'),
  employeeRole: yup.string().required('Employee role is required'),
  customFieldValues: yup.object(),
});

/**
 * Initiate Workflow Page Component
 * Allows HR Admin to create new onboarding/offboarding workflows
 */
export const InitiateWorkflowPage: React.FC = () => {
  const navigate = useNavigate();
  const [selectedTemplateId, setSelectedTemplateId] = useState<string>('');
  const [successMessage, setSuccessMessage] = useState<string>('');
  const [errorMessage, setErrorMessage] = useState<string>('');

  // RTK Query hooks
  const { data: templates, isLoading: templatesLoading } =
    useGetActiveTemplatesQuery();
  const { data: selectedTemplate } = useGetTemplateByIdQuery(selectedTemplateId, {
    skip: !selectedTemplateId,
  });
  const [initiateWorkflow, { isLoading: isSubmitting }] =
    useInitiateWorkflowMutation();

  // React Hook Form setup
  const {
    register,
    handleSubmit,
    control,
    watch,
    setError,
    formState: { errors },
  } = useForm<InitiateWorkflowFormData>({
    resolver: yupResolver(validationSchema),
    defaultValues: {
      templateId: '',
      employeeName: '',
      employeeEmail: '',
      employeeRole: '',
      customFieldValues: {},
    },
  });

  // Watch template selection and custom field values for conditional logic
  const watchTemplateId = watch('templateId');
  const watchCustomFields = watch('customFieldValues');

  // Update selected template when templateId changes
  useEffect(() => {
    if (watchTemplateId) {
      setSelectedTemplateId(watchTemplateId);
    }
  }, [watchTemplateId]);

  /**
   * Evaluate conditional rules for a field
   * Client-side preview only - server is authoritative
   */
  const shouldShowField = (field: TemplateCustomField): boolean => {
    if (!field.conditionalRules || field.conditionalRules.length === 0) {
      return true;
    }

    // Evaluate all rules - field is visible if ANY rule matches
    return field.conditionalRules.some((rule: ConditionalRule) => {
      const fieldValue = watchCustomFields?.[rule.targetFieldName];
      const ruleValue = rule.value;

      switch (rule.operator) {
        case 'EQUALS':
          return fieldValue === ruleValue;
        case 'NOT_EQUALS':
          return fieldValue !== ruleValue;
        case 'CONTAINS':
          return (
            typeof fieldValue === 'string' &&
            fieldValue.includes(ruleValue)
          );
        default:
          return true;
      }
    });
  };

  /**
   * Handle form submission
   */
  const onSubmit = async (data: InitiateWorkflowFormData) => {
    try {
      const response = await initiateWorkflow(data).unwrap();

      // Show success message
      setSuccessMessage(
        `Workflow ${response.workflowInstanceId} created successfully`
      );

      // Navigate to workflow detail page after short delay
      setTimeout(() => {
        navigate(`/workflows/${response.workflowInstanceId}`);
      }, 1500);
    } catch (error: unknown) {
      // Handle validation errors
      if (error.status === 400 && error.data?.errors) {
        // Set field-level errors
        Object.entries(error.data.errors).forEach(([field, message]) => {
          setError(field as any, {
            type: 'manual',
            message: message as string,
          });
        });
      } else {
        // Show general error message
        setErrorMessage(
          error.data?.message || 'Failed to initiate workflow. Please try again.'
        );
      }
    }
  };

  /**
   * Handle cancel button
   */
  const handleCancel = () => {
    navigate('/dashboard');
  };

  if (templatesLoading) {
    return <LoadingSpinner />;
  }

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Container maxWidth="md">
        <Paper sx={{ p: 4, mt: 4 }}>
          <PageHeader
            title="Initiate Workflow"
            breadcrumbs={[
              { label: 'Dashboard', path: '/dashboard' },
              { label: 'Workflows', path: '/workflows' },
              { label: 'Initiate' },
            ]}
          />

          {/* General error alert */}
          {errorMessage && (
            <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage('')}>
              {errorMessage}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate>
            {/* Template Selection Dropdown (AC1) */}
            <FormControl fullWidth margin="normal" error={!!errors.templateId}>
              <InputLabel required>Template</InputLabel>
              <Controller
                name="templateId"
                control={control}
                render={({ field }) => (
                  <Select {...field} label="Template" data-testid="template-select">
                    {templates?.map((template) => (
                      <MenuItem key={template.id} value={template.id}>
                        {template.name} ({template.workflowType})
                      </MenuItem>
                    ))}
                  </Select>
                )}
              />
              {errors.templateId && (
                <FormHelperText>{errors.templateId.message}</FormHelperText>
              )}
            </FormControl>

            {/* Static Employee Fields (AC3) */}
            <TextField
              fullWidth
              margin="normal"
              label="Employee Name"
              required
              {...register('employeeName')}
              error={!!errors.employeeName}
              helperText={errors.employeeName?.message}
              data-testid="employee-name-input"
            />

            <TextField
              fullWidth
              margin="normal"
              label="Employee Email"
              type="email"
              required
              {...register('employeeEmail')}
              error={!!errors.employeeEmail}
              helperText={errors.employeeEmail?.message}
              data-testid="employee-email-input"
            />

            <FormControl
              fullWidth
              margin="normal"
              error={!!errors.employeeRole}
            >
              <InputLabel required>Employee Role</InputLabel>
              <Controller
                name="employeeRole"
                control={control}
                render={({ field }) => (
                  <Select
                    {...field}
                    label="Employee Role"
                    data-testid="employee-role-select"
                  >
                    <MenuItem value="Software Engineer">Software Engineer</MenuItem>
                    <MenuItem value="Manager">Manager</MenuItem>
                    <MenuItem value="HR Admin">HR Admin</MenuItem>
                    <MenuItem value="Tech Support">Tech Support</MenuItem>
                  </Select>
                )}
              />
              {errors.employeeRole && (
                <FormHelperText>{errors.employeeRole.message}</FormHelperText>
              )}
            </FormControl>

            {/* Dynamic Custom Fields (AC2, AC4, AC5, AC6) */}
            {selectedTemplate?.customFields?.map((field: TemplateCustomField) => {
              const isVisible = shouldShowField(field);

              if (!isVisible) return null;

              const fieldName = `customFieldValues.${field.name}`;
              const fieldError = errors.customFieldValues?.[field.name];

              // TEXT field
              if (field.fieldType === 'TEXT') {
                return (
                  <TextField
                    key={field.id}
                    fullWidth
                    margin="normal"
                    label={field.label}
                    required={field.required}
                    {...register(fieldName)}
                    error={!!fieldError}
                    helperText={fieldError?.message}
                    data-testid={`custom-field-${field.name}`}
                  />
                );
              }

              // NUMBER field
              if (field.fieldType === 'NUMBER') {
                return (
                  <TextField
                    key={field.id}
                    fullWidth
                    margin="normal"
                    type="number"
                    label={field.label}
                    required={field.required}
                    {...register(fieldName)}
                    error={!!fieldError}
                    helperText={fieldError?.message}
                    data-testid={`custom-field-${field.name}`}
                  />
                );
              }

              // DATE field
              if (field.fieldType === 'DATE') {
                return (
                  <Controller
                    key={field.id}
                    name={fieldName as keyof InitiateWorkflowFormData}
                    control={control}
                    render={({ field: controllerField }) => (
                      <DatePicker
                        label={field.label}
                        value={controllerField.value || null}
                        onChange={(date) => controllerField.onChange(date)}
                        slotProps={{
                          textField: {
                            fullWidth: true,
                            margin: 'normal',
                            required: field.required,
                            error: !!fieldError,
                            helperText: fieldError?.message,
                            'data-testid': `custom-field-${field.name}`,
                          },
                        }}
                      />
                    )}
                  />
                );
              }

              // BOOLEAN field
              if (field.fieldType === 'BOOLEAN') {
                return (
                  <FormControlLabel
                    key={field.id}
                    control={
                      <Controller
                        name={fieldName as keyof InitiateWorkflowFormData}
                        control={control}
                        render={({ field: controllerField }) => (
                          <Checkbox
                            {...controllerField}
                            checked={!!controllerField.value}
                            data-testid={`custom-field-${field.name}`}
                          />
                        )}
                      />
                    }
                    label={field.label}
                    sx={{ mt: 2, mb: 1 }}
                  />
                );
              }

              // SELECT field
              if (field.fieldType === 'SELECT') {
                return (
                  <FormControl
                    key={field.id}
                    fullWidth
                    margin="normal"
                    error={!!fieldError}
                  >
                    <InputLabel required={field.required}>
                      {field.label}
                    </InputLabel>
                    <Controller
                      name={fieldName as keyof InitiateWorkflowFormData}
                      control={control}
                      render={({ field: controllerField }) => (
                        <Select
                          {...controllerField}
                          label={field.label}
                          data-testid={`custom-field-${field.name}`}
                        >
                          {field.selectOptions?.map((option) => (
                            <MenuItem key={option} value={option}>
                              {option}
                            </MenuItem>
                          ))}
                        </Select>
                      )}
                    />
                    {fieldError && (
                      <FormHelperText>{fieldError.message}</FormHelperText>
                    )}
                  </FormControl>
                );
              }

              return null;
            })}

            {/* Action Buttons (AC7, AC10) */}
            <Box sx={{ mt: 3, display: 'flex', gap: 2 }}>
              <Button
                variant="contained"
                type="submit"
                disabled={isSubmitting}
                data-testid="submit-button"
              >
                {isSubmitting ? 'Creating...' : 'Initiate Workflow'}
              </Button>
              <Button
                variant="outlined"
                onClick={handleCancel}
                disabled={isSubmitting}
                data-testid="cancel-button"
              >
                Cancel
              </Button>
            </Box>
          </Box>
        </Paper>

        {/* Success Snackbar (AC8) */}
        <Snackbar
          open={!!successMessage}
          autoHideDuration={5000}
          onClose={() => setSuccessMessage('')}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <Alert severity="success" onClose={() => setSuccessMessage('')}>
            {successMessage}
          </Alert>
        </Snackbar>
      </Container>
    </LocalizationProvider>
  );
};
