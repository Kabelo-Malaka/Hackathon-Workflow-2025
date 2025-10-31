/**
 * RTK Query API for Workflow operations
 * Story 3.7: Initiate Workflow UI
 */

import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type {
  InitiateWorkflowRequest,
  WorkflowInitiationResponse,
  WorkflowTemplate,
} from './types';

export const workflowsApi = createApi({
  reducerPath: 'workflowsApi',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api',
    credentials: 'include',
  }),
  tagTypes: ['Workflows', 'Templates'],
  endpoints: (builder) => ({
    getActiveTemplates: builder.query<WorkflowTemplate[], void>({
      query: () => '/templates?active=true',
      providesTags: ['Templates'],
    }),
    getTemplateById: builder.query<WorkflowTemplate, string>({
      query: (id) => `/templates/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Templates', id }],
    }),
    initiateWorkflow: builder.mutation<
      WorkflowInitiationResponse,
      InitiateWorkflowRequest
    >({
      query: (request) => ({
        url: '/workflows',
        method: 'POST',
        body: request,
      }),
      invalidatesTags: ['Workflows'],
    }),
  }),
});

export const {
  useGetActiveTemplatesQuery,
  useGetTemplateByIdQuery,
  useInitiateWorkflowMutation,
} = workflowsApi;
