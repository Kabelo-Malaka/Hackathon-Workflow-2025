/**
 * Unit tests for InitiateWorkflowPage component
 * Story 3.7: Initiate Workflow UI
 */

import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';
import { InitiateWorkflowPage } from '../InitiateWorkflowPage';
import { workflowsApi } from '../workflowsApi';

// Mock navigate
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

// Test data
const mockTemplates = [
  {
    id: '1',
    name: 'Onboarding Template',
    workflowType: 'ONBOARDING',
    isActive: true,
    customFields: [
      {
        id: 'cf1',
        name: 'startDate',
        label: 'Start Date',
        fieldType: 'DATE',
        required: true,
      },
      {
        id: 'cf2',
        name: 'department',
        label: 'Department',
        fieldType: 'SELECT',
        required: true,
        selectOptions: ['Engineering', 'Sales', 'HR'],
      },
    ],
  },
  {
    id: '2',
    name: 'Offboarding Template',
    workflowType: 'OFFBOARDING',
    isActive: true,
    customFields: [],
  },
];

const mockTemplate = mockTemplates[0];

// Helper to create test store
const createTestStore = () => {
  return configureStore({
    reducer: {
      [workflowsApi.reducerPath]: workflowsApi.reducer,
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(workflowsApi.middleware),
  });
};

// Helper to render component with providers
const renderWithProviders = (component: React.ReactElement) => {
  const store = createTestStore();
  return render(
    <Provider store={store}>
      <BrowserRouter>{component}</BrowserRouter>
    </Provider>
  );
};

describe('InitiateWorkflowPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Template Selection (AC1)', () => {
    it('renders template dropdown with active templates', async () => {
      // Mock API response
      jest
        .spyOn(workflowsApi.endpoints.getActiveTemplates, 'useQuery')
        .mockReturnValue({
          data: mockTemplates,
          isLoading: false,
          isError: false,
        } as unknown);

      renderWithProviders(<InitiateWorkflowPage />);

      // Check dropdown exists
      const dropdown = screen.getByTestId('template-select');
      expect(dropdown).toBeInTheDocument();

      // Open dropdown and check options
      await userEvent.click(dropdown);
      await waitFor(() => {
        expect(
          screen.getByText('Onboarding Template (ONBOARDING)')
        ).toBeInTheDocument();
        expect(
          screen.getByText('Offboarding Template (OFFBOARDING)')
        ).toBeInTheDocument();
      });
    });

    it('shows loading spinner while fetching templates', () => {
      jest
        .spyOn(workflowsApi.endpoints.getActiveTemplates, 'useQuery')
        .mockReturnValue({
          data: undefined,
          isLoading: true,
          isError: false,
        } as unknown);

      renderWithProviders(<InitiateWorkflowPage />);

      expect(screen.getByRole('progressbar')).toBeInTheDocument();
    });
  });

  describe('Static Employee Fields (AC3)', () => {
    beforeEach(() => {
      jest
        .spyOn(workflowsApi.endpoints.getActiveTemplates, 'useQuery')
        .mockReturnValue({
          data: mockTemplates,
          isLoading: false,
          isError: false,
        } as unknown);
    });

    it('renders all required static fields', () => {
      renderWithProviders(<InitiateWorkflowPage />);

      expect(screen.getByTestId('employee-name-input')).toBeInTheDocument();
      expect(screen.getByTestId('employee-email-input')).toBeInTheDocument();
      expect(screen.getByTestId('employee-role-select')).toBeInTheDocument();
    });

    it('displays employee role options', async () => {
      renderWithProviders(<InitiateWorkflowPage />);

      const roleSelect = screen.getByTestId('employee-role-select');
      await userEvent.click(roleSelect);

      await waitFor(() => {
        expect(screen.getByText('Software Engineer')).toBeInTheDocument();
        expect(screen.getByText('Manager')).toBeInTheDocument();
        expect(screen.getByText('HR Admin')).toBeInTheDocument();
        expect(screen.getByText('Tech Support')).toBeInTheDocument();
      });
    });
  });

  describe('Dynamic Custom Fields (AC2, AC4)', () => {
    beforeEach(() => {
      jest
        .spyOn(workflowsApi.endpoints.getActiveTemplates, 'useQuery')
        .mockReturnValue({
          data: mockTemplates,
          isLoading: false,
          isError: false,
        } as unknown);

      jest
        .spyOn(workflowsApi.endpoints.getTemplateById, 'useQuery')
        .mockReturnValue({
          data: mockTemplate,
          isLoading: false,
          isError: false,
        } as unknown);
    });

    it('renders custom fields when template is selected', async () => {
      renderWithProviders(<InitiateWorkflowPage />);

      // Select template
      const templateSelect = screen.getByTestId('template-select');
      await userEvent.click(templateSelect);
      await userEvent.click(screen.getByText('Onboarding Template (ONBOARDING)'));

      // Wait for custom fields to appear
      await waitFor(() => {
        expect(screen.getByTestId('custom-field-startDate')).toBeInTheDocument();
        expect(screen.getByTestId('custom-field-department')).toBeInTheDocument();
      });
    });
  });

  describe('Form Validation (AC5)', () => {
    beforeEach(() => {
      jest
        .spyOn(workflowsApi.endpoints.getActiveTemplates, 'useQuery')
        .mockReturnValue({
          data: mockTemplates,
          isLoading: false,
          isError: false,
        } as unknown);
    });

    it('displays validation errors for required fields', async () => {
      renderWithProviders(<InitiateWorkflowPage />);

      // Submit form without filling required fields
      const submitButton = screen.getByTestId('submit-button');
      await userEvent.click(submitButton);

      // Check for validation error messages
      await waitFor(() => {
        expect(screen.getByText('Template is required')).toBeInTheDocument();
        expect(screen.getByText('Employee name is required')).toBeInTheDocument();
        expect(
          screen.getByText('Employee email is required')
        ).toBeInTheDocument();
        expect(screen.getByText('Employee role is required')).toBeInTheDocument();
      });
    });

    it('validates email format', async () => {
      renderWithProviders(<InitiateWorkflowPage />);

      const emailInput = screen.getByTestId('employee-email-input');
      await userEvent.type(emailInput, 'invalid-email');

      const submitButton = screen.getByTestId('submit-button');
      await userEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('Invalid email address')).toBeInTheDocument();
      });
    });
  });

  describe('Form Submission (AC7, AC8)', () => {
    const mockInitiateWorkflow = jest.fn();

    beforeEach(() => {
      jest
        .spyOn(workflowsApi.endpoints.getActiveTemplates, 'useQuery')
        .mockReturnValue({
          data: mockTemplates,
          isLoading: false,
          isError: false,
        } as unknown);

      jest
        .spyOn(workflowsApi.endpoints.initiateWorkflow, 'useMutation')
        .mockReturnValue([
          mockInitiateWorkflow,
          { isLoading: false },
        ] as unknown);
    });

    it('submits form with correct data', async () => {
      mockInitiateWorkflow.mockResolvedValue({
        data: { workflowInstanceId: '123', message: 'Success' },
      });

      renderWithProviders(<InitiateWorkflowPage />);

      // Fill form
      const templateSelect = screen.getByTestId('template-select');
      await userEvent.click(templateSelect);
      await userEvent.click(screen.getByText('Offboarding Template (OFFBOARDING)'));

      await userEvent.type(
        screen.getByTestId('employee-name-input'),
        'John Doe'
      );
      await userEvent.type(
        screen.getByTestId('employee-email-input'),
        'john@example.com'
      );

      const roleSelect = screen.getByTestId('employee-role-select');
      await userEvent.click(roleSelect);
      await userEvent.click(screen.getByText('Software Engineer'));

      // Submit
      const submitButton = screen.getByTestId('submit-button');
      await userEvent.click(submitButton);

      await waitFor(() => {
        expect(mockInitiateWorkflow).toHaveBeenCalledWith(
          expect.objectContaining({
            templateId: '2',
            employeeName: 'John Doe',
            employeeEmail: 'john@example.com',
            employeeRole: 'Software Engineer',
          })
        );
      });
    });

    it('shows success message and navigates on successful submission', async () => {
      mockInitiateWorkflow.mockResolvedValue({
        unwrap: () =>
          Promise.resolve({
            workflowInstanceId: '123',
            message: 'Success',
          }),
      });

      renderWithProviders(<InitiateWorkflowPage />);

      // Fill and submit form
      const templateSelect = screen.getByTestId('template-select');
      await userEvent.click(templateSelect);
      await userEvent.click(screen.getByText('Offboarding Template (OFFBOARDING)'));

      await userEvent.type(
        screen.getByTestId('employee-name-input'),
        'John Doe'
      );
      await userEvent.type(
        screen.getByTestId('employee-email-input'),
        'john@example.com'
      );

      const roleSelect = screen.getByTestId('employee-role-select');
      await userEvent.click(roleSelect);
      await userEvent.click(screen.getByText('Software Engineer'));

      const submitButton = screen.getByTestId('submit-button');
      await userEvent.click(submitButton);

      // Check success message
      await waitFor(() => {
        expect(
          screen.getByText('Workflow 123 created successfully')
        ).toBeInTheDocument();
      });

      // Check navigation (with delay)
      setTimeout(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/workflows/123');
      }, 1500);
    });
  });

  describe('Error Handling (AC9)', () => {
    const mockInitiateWorkflow = jest.fn();

    beforeEach(() => {
      jest
        .spyOn(workflowsApi.endpoints.getActiveTemplates, 'useQuery')
        .mockReturnValue({
          data: mockTemplates,
          isLoading: false,
          isError: false,
        } as unknown);

      jest
        .spyOn(workflowsApi.endpoints.initiateWorkflow, 'useMutation')
        .mockReturnValue([
          mockInitiateWorkflow,
          { isLoading: false },
        ] as unknown);
    });

    it('displays API error messages', async () => {
      mockInitiateWorkflow.mockRejectedValue({
        unwrap: () =>
          Promise.reject({
            status: 500,
            data: { message: 'Server error' },
          }),
      });

      renderWithProviders(<InitiateWorkflowPage />);

      // Fill and submit form
      const templateSelect = screen.getByTestId('template-select');
      await userEvent.click(templateSelect);
      await userEvent.click(screen.getByText('Offboarding Template (OFFBOARDING)'));

      await userEvent.type(
        screen.getByTestId('employee-name-input'),
        'John Doe'
      );
      await userEvent.type(
        screen.getByTestId('employee-email-input'),
        'john@example.com'
      );

      const roleSelect = screen.getByTestId('employee-role-select');
      await userEvent.click(roleSelect);
      await userEvent.click(screen.getByText('Software Engineer'));

      const submitButton = screen.getByTestId('submit-button');
      await userEvent.click(submitButton);

      await waitFor(() => {
        expect(
          screen.getByText(/Failed to initiate workflow/)
        ).toBeInTheDocument();
      });
    });
  });

  describe('Cancel Button (AC10)', () => {
    beforeEach(() => {
      jest
        .spyOn(workflowsApi.endpoints.getActiveTemplates, 'useQuery')
        .mockReturnValue({
          data: mockTemplates,
          isLoading: false,
          isError: false,
        } as unknown);
    });

    it('navigates to dashboard when cancel is clicked', async () => {
      renderWithProviders(<InitiateWorkflowPage />);

      const cancelButton = screen.getByTestId('cancel-button');
      await userEvent.click(cancelButton);

      expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });
  });
});
