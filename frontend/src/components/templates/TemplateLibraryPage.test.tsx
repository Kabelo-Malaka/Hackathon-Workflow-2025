import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { TemplateLibraryPage } from './TemplateLibraryPage';
import { templatesApi } from '../../features/templates/templatesApi';

// Mock navigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Mock template data
const mockTemplates = [
  {
    id: '1',
    name: 'Onboarding Template',
    type: 'ONBOARDING' as const,
    isActive: true,
    taskCount: 5,
    createdAt: '2025-10-30T10:00:00Z',
    updatedAt: '2025-10-31T14:00:00Z',
  },
  {
    id: '2',
    name: 'Offboarding Template',
    type: 'OFFBOARDING' as const,
    isActive: false,
    taskCount: 3,
    createdAt: '2025-10-29T10:00:00Z',
    updatedAt: '2025-10-30T14:00:00Z',
  },
];

describe('TemplateLibraryPage', () => {
  let store: ReturnType<typeof configureStore>;

  beforeEach(() => {
    mockNavigate.mockClear();

    // Create a mock store with the templates API
    store = configureStore({
      reducer: {
        [templatesApi.reducerPath]: templatesApi.reducer,
      },
      middleware: (getDefaultMiddleware) =>
        getDefaultMiddleware().concat(templatesApi.middleware),
    });
  });

  const renderWithProviders = (component: React.ReactElement) => {
    return render(
      <Provider store={store}>
        <BrowserRouter>{component}</BrowserRouter>
      </Provider>
    );
  };

  it('renders loading spinner initially', () => {
    // Mock loading state
    vi.spyOn(templatesApi, 'useGetTemplatesQuery').mockReturnValue({
      data: undefined,
      isLoading: true,
      error: undefined,
    } as any);

    renderWithProviders(<TemplateLibraryPage />);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('displays empty state when no templates exist', () => {
    vi.spyOn(templatesApi, 'useGetTemplatesQuery').mockReturnValue({
      data: [],
      isLoading: false,
      error: undefined,
    } as any);

    renderWithProviders(<TemplateLibraryPage />);
    expect(screen.getByText(/No templates yet/i)).toBeInTheDocument();
    expect(screen.getByText(/Create First Template/i)).toBeInTheDocument();
  });

  it('displays templates in grid after loading', () => {
    vi.spyOn(templatesApi, 'useGetTemplatesQuery').mockReturnValue({
      data: mockTemplates,
      isLoading: false,
      error: undefined,
    } as any);

    renderWithProviders(<TemplateLibraryPage />);
    expect(screen.getByText('Onboarding Template')).toBeInTheDocument();
    expect(screen.getByText('Offboarding Template')).toBeInTheDocument();
  });

  it('filters templates by type', async () => {
    vi.spyOn(templatesApi, 'useGetTemplatesQuery').mockReturnValue({
      data: mockTemplates,
      isLoading: false,
      error: undefined,
    } as any);

    renderWithProviders(<TemplateLibraryPage />);

    // Click Onboarding filter
    const onboardingButton = screen.getByRole('button', { name: /Onboarding/i });
    fireEvent.click(onboardingButton);

    await waitFor(() => {
      expect(screen.getByText('Onboarding Template')).toBeInTheDocument();
      expect(screen.queryByText('Offboarding Template')).not.toBeInTheDocument();
    });
  });

  it('filters templates by status', async () => {
    vi.spyOn(templatesApi, 'useGetTemplatesQuery').mockReturnValue({
      data: mockTemplates,
      isLoading: false,
      error: undefined,
    } as any);

    renderWithProviders(<TemplateLibraryPage />);

    // Click Active filter
    const activeButton = screen.getByRole('button', { name: /^Active$/i });
    fireEvent.click(activeButton);

    await waitFor(() => {
      expect(screen.getByText('Onboarding Template')).toBeInTheDocument();
      expect(screen.queryByText('Offboarding Template')).not.toBeInTheDocument();
    });
  });

  it('navigates to create new template when button is clicked', () => {
    vi.spyOn(templatesApi, 'useGetTemplatesQuery').mockReturnValue({
      data: mockTemplates,
      isLoading: false,
      error: undefined,
    } as any);

    renderWithProviders(<TemplateLibraryPage />);

    const createButton = screen.getByRole('button', { name: /Create New Template/i });
    fireEvent.click(createButton);

    expect(mockNavigate).toHaveBeenCalledWith('/templates/new');
  });

  it('displays error message on fetch failure', () => {
    vi.spyOn(templatesApi, 'useGetTemplatesQuery').mockReturnValue({
      data: undefined,
      isLoading: false,
      error: { status: 500, data: 'Server error' },
    } as any);

    renderWithProviders(<TemplateLibraryPage />);
    expect(screen.getByText(/Failed to load templates/i)).toBeInTheDocument();
  });
});
