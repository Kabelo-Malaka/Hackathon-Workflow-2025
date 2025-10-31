import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';
import { TemplateBuilderPage } from './TemplateBuilderPage';
import { templatesApi } from '../../features/templates/templatesApi';
import authReducer from '../../features/auth/authSlice';

// Mock navigate
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useParams: () => ({}),
}));

// Helper to create a test store
const createTestStore = (initialAuthState = {}) => {
  return configureStore({
    reducer: {
      auth: authReducer,
      [templatesApi.reducerPath]: templatesApi.reducer,
    },
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware().concat(templatesApi.middleware),
    preloadedState: {
      auth: {
        user: { id: '1', username: 'testuser', email: 'test@test.com', role: 'HR_ADMIN' },
        isAuthenticated: true,
        isLoading: false,
        ...initialAuthState,
      },
    },
  });
};

// Helper to render with providers
const renderWithProviders = (component: React.ReactElement, store = createTestStore()) => {
  return render(
    <Provider store={store}>
      <BrowserRouter>
        {component}
      </BrowserRouter>
    </Provider>
  );
};

describe('TemplateBuilderPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Access Control', () => {
    it('should deny access to users without HR_ADMIN or ADMINISTRATOR role', () => {
      const store = createTestStore({
        user: { id: '1', username: 'testuser', email: 'test@test.com', role: 'LINE_MANAGER' },
      });

      renderWithProviders(<TemplateBuilderPage />, store);

      expect(screen.getByText(/Access denied/i)).toBeInTheDocument();
    });

    it('should allow access to HR_ADMIN users', () => {
      renderWithProviders(<TemplateBuilderPage />);

      expect(screen.getByText(/Create Workflow Template/i)).toBeInTheDocument();
    });

    it('should allow access to ADMINISTRATOR users', () => {
      const store = createTestStore({
        user: { id: '1', username: 'admin', email: 'admin@test.com', role: 'ADMINISTRATOR' },
      });

      renderWithProviders(<TemplateBuilderPage />, store);

      expect(screen.getByText(/Create Workflow Template/i)).toBeInTheDocument();
    });
  });

  describe('Form Rendering', () => {
    it('should render template info fields and empty task list', () => {
      renderWithProviders(<TemplateBuilderPage />);

      expect(screen.getByLabelText(/Template Name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Description/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Type/i)).toBeInTheDocument();
      expect(screen.getByText(/No tasks yet/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Add Task/i })).toBeInTheDocument();
    });

    it('should have Save Template and Cancel buttons', () => {
      renderWithProviders(<TemplateBuilderPage />);

      expect(screen.getByRole('button', { name: /Save Template/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
    });
  });

  describe('Task Management', () => {
    it('should add a new task when Add Task button is clicked', async () => {
      const user = userEvent.setup();
      renderWithProviders(<TemplateBuilderPage />);

      const addButton = screen.getByRole('button', { name: /Add Task/i });
      await user.click(addButton);

      expect(screen.getByText(/Task 1/i)).toBeInTheDocument();
      expect(screen.queryByText(/No tasks yet/i)).not.toBeInTheDocument();
    });

    it('should add multiple tasks with sequential numbers', async () => {
      const user = userEvent.setup();
      renderWithProviders(<TemplateBuilderPage />);

      const addButton = screen.getByRole('button', { name: /Add Task/i });
      await user.click(addButton);
      await user.click(addButton);
      await user.click(addButton);

      expect(screen.getByText(/Task 1/i)).toBeInTheDocument();
      expect(screen.getByText(/Task 2/i)).toBeInTheDocument();
      expect(screen.getByText(/Task 3/i)).toBeInTheDocument();
    });

    it('should remove a task when delete button is clicked', async () => {
      const user = userEvent.setup();
      // Mock window.confirm
      window.confirm = jest.fn(() => true);

      renderWithProviders(<TemplateBuilderPage />);

      const addButton = screen.getByRole('button', { name: /Add Task/i });
      await user.click(addButton);
      await user.click(addButton);

      const deleteButtons = screen.getAllByTitle(/Remove Task/i);
      await user.click(deleteButtons[0]);

      await waitFor(() => {
        expect(screen.queryByText(/Task 2/i)).toBeInTheDocument();
        const taskHeaders = screen.queryAllByText(/Task 1/i);
        expect(taskHeaders.length).toBe(1);
      });
    });

    it('should not remove task if confirmation is cancelled', async () => {
      const user = userEvent.setup();
      window.confirm = jest.fn(() => false);

      renderWithProviders(<TemplateBuilderPage />);

      const addButton = screen.getByRole('button', { name: /Add Task/i });
      await user.click(addButton);

      const deleteButton = screen.getByTitle(/Remove Task/i);
      await user.click(deleteButton);

      expect(screen.getByText(/Task 1/i)).toBeInTheDocument();
    });
  });

  describe('Task Reordering', () => {
    it('should disable Move Up button for first task', async () => {
      const user = userEvent.setup();
      renderWithProviders(<TemplateBuilderPage />);

      const addButton = screen.getByRole('button', { name: /Add Task/i });
      await user.click(addButton);

      const moveUpButton = screen.getByTitle(/Move Up/i);
      expect(moveUpButton).toBeDisabled();
    });

    it('should disable Move Down button for last task', async () => {
      const user = userEvent.setup();
      renderWithProviders(<TemplateBuilderPage />);

      const addButton = screen.getByRole('button', { name: /Add Task/i });
      await user.click(addButton);

      const moveDownButton = screen.getByTitle(/Move Down/i);
      expect(moveDownButton).toBeDisabled();
    });
  });

  describe('Form Validation', () => {
    it('should show error when template name is empty on submit', async () => {
      const user = userEvent.setup();
      renderWithProviders(<TemplateBuilderPage />);

      const saveButton = screen.getByRole('button', { name: /Save Template/i });
      await user.click(saveButton);

      await waitFor(() => {
        expect(screen.getByText(/Template name is required/i)).toBeInTheDocument();
      });
    });

    it('should show error when template name exceeds 255 characters', async () => {
      const user = userEvent.setup();
      renderWithProviders(<TemplateBuilderPage />);

      const nameInput = screen.getByLabelText(/Template Name/i);
      await user.type(nameInput, 'a'.repeat(256));

      const saveButton = screen.getByRole('button', { name: /Save Template/i });
      await user.click(saveButton);

      await waitFor(() => {
        expect(screen.getByText(/must not exceed 255 characters/i)).toBeInTheDocument();
      });
    });
  });

  describe('Cancel Button', () => {
    it('should navigate back without confirmation when form is pristine', async () => {
      const user = userEvent.setup();
      renderWithProviders(<TemplateBuilderPage />);

      const cancelButton = screen.getByRole('button', { name: /Cancel/i });
      await user.click(cancelButton);

      expect(mockNavigate).toHaveBeenCalledWith('/templates');
    });

    it('should show confirmation dialog when form has unsaved changes', async () => {
      const user = userEvent.setup();
      renderWithProviders(<TemplateBuilderPage />);

      const nameInput = screen.getByLabelText(/Template Name/i);
      await user.type(nameInput, 'Test Template');

      const cancelButton = screen.getByRole('button', { name: /Cancel/i });
      await user.click(cancelButton);

      await waitFor(() => {
        expect(screen.getByText(/Unsaved Changes/i)).toBeInTheDocument();
        expect(screen.getByText(/You have unsaved changes/i)).toBeInTheDocument();
      });
    });

    it('should navigate away when user confirms unsaved changes', async () => {
      const user = userEvent.setup();
      renderWithProviders(<TemplateBuilderPage />);

      const nameInput = screen.getByLabelText(/Template Name/i);
      await user.type(nameInput, 'Test Template');

      const cancelButton = screen.getByRole('button', { name: /Cancel/i });
      await user.click(cancelButton);

      await waitFor(() => {
        expect(screen.getByText(/Unsaved Changes/i)).toBeInTheDocument();
      });

      const confirmButton = screen.getByRole('button', { name: /Yes, Leave/i });
      await user.click(confirmButton);

      expect(mockNavigate).toHaveBeenCalledWith('/templates');
    });
  });

  describe('Task Form Fields', () => {
    it('should render all task form fields when task is added', async () => {
      const user = userEvent.setup();
      renderWithProviders(<TemplateBuilderPage />);

      const addButton = screen.getByRole('button', { name: /Add Task/i });
      await user.click(addButton);

      expect(screen.getByLabelText(/Task Name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Assigned Role/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Run in Parallel/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Task Dependency/i)).toBeInTheDocument();
    });

    it('should show parallel task info text when parallel checkbox is checked', async () => {
      const user = userEvent.setup();
      renderWithProviders(<TemplateBuilderPage />);

      const addButton = screen.getByRole('button', { name: /Add Task/i });
      await user.click(addButton);

      const parallelCheckbox = screen.getByLabelText(/Run in Parallel/i);
      await user.click(parallelCheckbox);

      await waitFor(() => {
        expect(screen.getByText(/will run in parallel/i)).toBeInTheDocument();
      });
    });
  });

  describe('Task Dependencies', () => {
    it('should only show previous tasks as dependency options', async () => {
      const user = userEvent.setup();
      renderWithProviders(<TemplateBuilderPage />);

      const addButton = screen.getByRole('button', { name: /Add Task/i });
      await user.click(addButton);
      await user.click(addButton);
      await user.click(addButton);

      // Fill in task names
      const taskNameInputs = screen.getAllByLabelText(/Task Name/i);
      await user.type(taskNameInputs[0], 'First Task');
      await user.type(taskNameInputs[1], 'Second Task');
      await user.type(taskNameInputs[2], 'Third Task');

      // Check dependency dropdown for third task
      const dependencySelects = screen.getAllByLabelText(/Task Dependency/i);
      const thirdTaskDependency = dependencySelects[2];

      fireEvent.mouseDown(thirdTaskDependency);

      // Should show "No Dependency", "Task 1", and "Task 2" but not "Task 3"
      await waitFor(() => {
        expect(screen.getByText(/No Dependency/i)).toBeInTheDocument();
        expect(screen.getByText(/Task 1: First Task/i)).toBeInTheDocument();
        expect(screen.getByText(/Task 2: Second Task/i)).toBeInTheDocument();
      });
    });
  });
});
