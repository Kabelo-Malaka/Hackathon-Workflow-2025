/**
 * Loading Spinner component
 * Story 3.7: Initiate Workflow UI
 */

import React from 'react';
import { Box, CircularProgress } from '@mui/material';

interface LoadingSpinnerProps {
  size?: number;
}

/**
 * Centered loading spinner with optional size
 */
export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ size = 40 }) => {
  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
      <CircularProgress size={size} />
    </Box>
  );
};
