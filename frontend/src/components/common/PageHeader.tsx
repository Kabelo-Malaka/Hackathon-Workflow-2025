/**
 * Page Header component with title and optional breadcrumbs
 * Story 3.7: Initiate Workflow UI
 */

import React from 'react';
import { Typography, Breadcrumbs, Link, Box } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';

interface BreadcrumbItem {
  label: string;
  path?: string;
}

interface PageHeaderProps {
  title: string;
  breadcrumbs?: BreadcrumbItem[];
}

/**
 * Displays page title with optional breadcrumb navigation
 */
export const PageHeader: React.FC<PageHeaderProps> = ({ title, breadcrumbs }) => {
  return (
    <Box mb={3}>
      {breadcrumbs && breadcrumbs.length > 0 && (
        <Breadcrumbs
          separator={<NavigateNextIcon fontSize="small" />}
          aria-label="breadcrumb"
          sx={{ mb: 1 }}
        >
          {breadcrumbs.map((crumb, index) =>
            crumb.path ? (
              <Link
                key={index}
                component={RouterLink}
                to={crumb.path}
                color="inherit"
                underline="hover"
              >
                {crumb.label}
              </Link>
            ) : (
              <Typography key={index} color="text.primary">
                {crumb.label}
              </Typography>
            )
          )}
        </Breadcrumbs>
      )}
      <Typography variant="h4" component="h1" gutterBottom>
        {title}
      </Typography>
    </Box>
  );
};
