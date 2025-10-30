import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { UserResponse } from './authSlice';

interface LoginRequest {
  username: string;
  password: string;
}

interface AuthResponse {
  username: string;
  role: string;
  message: string;
}

export const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: fetchBaseQuery({
    baseUrl: 'http://localhost:8080/api',
    credentials: 'include', // Important: sends session cookie
  }),
  endpoints: (builder) => ({
    login: builder.mutation<AuthResponse, LoginRequest>({
      query: (credentials) => ({
        url: '/auth/login',
        method: 'POST',
        body: credentials,
      }),
    }),
    logout: builder.mutation<{ message: string }, void>({
      query: () => ({
        url: '/auth/logout',
        method: 'POST',
      }),
    }),
    checkSession: builder.query<UserResponse, void>({
      query: () => '/auth/me',
    }),
  }),
});

export const { useLoginMutation, useLogoutMutation, useCheckSessionQuery } = authApi;
