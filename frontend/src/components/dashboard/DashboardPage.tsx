import { useNavigate } from 'react-router-dom';
import {
  Typography,
  Box,
  Card,
  CardContent,
  CardActionArea,
  Grid,
  Stack,
} from '@mui/material';
import PeopleIcon from '@mui/icons-material/People';
import DescriptionIcon from '@mui/icons-material/Description';

export const DashboardPage = () => {
  const navigate = useNavigate();

  const navigationCards = [
    {
      title: 'User Management',
      description: 'Manage users, roles, and permissions',
      icon: <PeopleIcon sx={{ fontSize: 48, color: 'primary.main' }} />,
      path: '/users',
    },
    {
      title: 'Template Management',
      description: 'Create and manage workflow templates',
      icon: <DescriptionIcon sx={{ fontSize: 48, color: 'primary.main' }} />,
      path: '/templates',
    },
  ];

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Welcome to the Hello Goodbye Dashboard
      </Typography>

      <Grid container spacing={3}>
        {navigationCards.map((card) => (
          <Grid item xs={12} sm={6} md={4} key={card.path}>
            <Card elevation={2}>
              <CardActionArea onClick={() => navigate(card.path)}>
                <CardContent>
                  <Stack spacing={2} alignItems="center" textAlign="center" py={2}>
                    {card.icon}
                    <Typography variant="h6" component="div">
                      {card.title}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {card.description}
                    </Typography>
                  </Stack>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
};
