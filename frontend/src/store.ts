import { configureStore } from '@reduxjs/toolkit';

// Redux store configuration
// Feature slices will be added as stories are implemented

export const store = configureStore({
  reducer: {
    // Add slice reducers here as they are implemented
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
