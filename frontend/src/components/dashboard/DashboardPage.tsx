import { Typography, Box } from '@mui/material';

export const DashboardPage = () => {
  return (
    <Box p={3}>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>
      <Typography variant="body1">
        Welcome to the Employee Lifecycle Management Dashboard. This page will display workflow status and task queues.
      </Typography>
    </Box>
  );
};
