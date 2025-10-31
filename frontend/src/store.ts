import { configureStore } from '@reduxjs/toolkit';
import { authApi } from './features/auth/authApi';
import { usersApi } from './features/users/usersApi';
import { templatesApi } from './features/templates/templatesApi';
import authReducer from './features/auth/authSlice';
import usersReducer from './features/users/usersSlice';

// Redux store configuration
// Feature slices will be added as stories are implemented

export const store = configureStore({
  reducer: {
    auth: authReducer,
    users: usersReducer,
    [authApi.reducerPath]: authApi.reducer,
    [usersApi.reducerPath]: usersApi.reducer,
    [templatesApi.reducerPath]: templatesApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware()
      .concat(authApi.middleware)
      .concat(usersApi.middleware)
      .concat(templatesApi.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
