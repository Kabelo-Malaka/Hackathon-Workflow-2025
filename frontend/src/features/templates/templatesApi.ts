import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';

// DTOs matching backend API
export interface CreateTemplateTaskRequest {
  taskName: string;
  description?: string;
  assignedRole: 'HR_ADMIN' | 'LINE_MANAGER' | 'TECH_SUPPORT' | 'ADMINISTRATOR';
  sequenceOrder: number;
  isParallel: boolean;
  dependencyTaskId?: string | null;
}

export interface CreateTemplateRequest {
  name: string;
  description?: string;
  type: 'ONBOARDING' | 'OFFBOARDING';
  tasks: CreateTemplateTaskRequest[];
}

export interface UpdateTemplateRequest {
  name: string;
  description?: string;
  type: 'ONBOARDING' | 'OFFBOARDING';
  isActive: boolean;
  tasks: CreateTemplateTaskRequest[];
}

export interface TaskDetailResponse {
  id: string;
  taskName: string;
  description?: string;
  assignedRole: string;
  sequenceOrder: number;
  isParallel: boolean;
  dependencyTaskId?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TemplateDetailResponse {
  id: string;
  name: string;
  description?: string;
  type: 'ONBOARDING' | 'OFFBOARDING';
  isActive: boolean;
  tasks: TaskDetailResponse[];
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
}

export interface TemplateSummaryResponse {
  id: string;
  name: string;
  type: 'ONBOARDING' | 'OFFBOARDING';
  isActive: boolean;
  taskCount: number;
  createdAt: string;
  updatedAt: string;
}

export const templatesApi = createApi({
  reducerPath: 'templatesApi',
  baseQuery: fetchBaseQuery({
    baseUrl: 'http://localhost:8080/api',
    credentials: 'include',
  }),
  tagTypes: ['Templates'],
  endpoints: (builder) => ({
    getTemplates: builder.query<TemplateSummaryResponse[], void>({
      query: () => '/templates',
      providesTags: ['Templates'],
    }),
    getTemplateById: builder.query<TemplateDetailResponse, string>({
      query: (id) => `/templates/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Templates', id }],
    }),
    createTemplate: builder.mutation<TemplateDetailResponse, CreateTemplateRequest>({
      query: (newTemplate) => ({
        url: '/templates',
        method: 'POST',
        body: newTemplate,
      }),
      invalidatesTags: ['Templates'],
    }),
    updateTemplate: builder.mutation<TemplateDetailResponse, { id: string } & UpdateTemplateRequest>({
      query: ({ id, ...body }) => ({
        url: `/templates/${id}`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: ['Templates'],
    }),
  }),
});

export const {
  useGetTemplatesQuery,
  useGetTemplateByIdQuery,
  useCreateTemplateMutation,
  useUpdateTemplateMutation,
} = templatesApi;
