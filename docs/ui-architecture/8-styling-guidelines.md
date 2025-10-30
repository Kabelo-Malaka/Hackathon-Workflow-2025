# 8. Styling Guidelines

## Styling Approach

The application uses **Material-UI (MUI)** with **Emotion** for styling. Three styling methods are available:

1. **MUI `sx` prop** (preferred for one-off styles)
2. **MUI `styled` API** (for reusable styled components)
3. **CSS custom properties** (for global theme variables)

## Global Theme Variables

**CSS Custom Properties (`src/styles/variables.css`):**

```css
:root {
  /* Magna BC Brand Colors */
  --color-primary-main: #1976d2;
  --color-primary-light: #42a5f5;
  --color-primary-dark: #1565c0;

  --color-secondary-main: #9c27b0;
  --color-secondary-light: #ba68c8;
  --color-secondary-dark: #7b1fa2;

  --color-error: #d32f2f;
  --color-warning: #ed6c02;
  --color-info: #0288d1;
  --color-success: #2e7d32;

  /* Grays */
  --color-gray-50: #fafafa;
  --color-gray-100: #f5f5f5;
  --color-gray-200: #eeeeee;
  --color-gray-300: #e0e0e0;
  --color-gray-400: #bdbdbd;
  --color-gray-500: #9e9e9e;
  --color-gray-600: #757575;
  --color-gray-700: #616161;
  --color-gray-800: #424242;
  --color-gray-900: #212121;

  /* Spacing (8px base) */
  --spacing-xs: 4px;
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;

  /* Typography */
  --font-family: 'Roboto', 'Helvetica', 'Arial', sans-serif;
  --font-size-xs: 0.75rem;
  --font-size-sm: 0.875rem;
  --font-size-md: 1rem;
  --font-size-lg: 1.25rem;
  --font-size-xl: 1.5rem;

  /* Shadows */
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24);
  --shadow-md: 0 3px 6px rgba(0, 0, 0, 0.15), 0 2px 4px rgba(0, 0, 0, 0.12);
  --shadow-lg: 0 10px 20px rgba(0, 0, 0, 0.15), 0 3px 6px rgba(0, 0, 0, 0.10);

  /* Border Radius */
  --border-radius-sm: 4px;
  --border-radius-md: 8px;
  --border-radius-lg: 12px;

  /* Z-index */
  --z-index-drawer: 1200;
  --z-index-modal: 1300;
  --z-index-snackbar: 1400;
  --z-index-tooltip: 1500;
}

/* Dark mode variables */
[data-theme='dark'] {
  --color-background: #121212;
  --color-surface: #1e1e1e;
  --color-text-primary: rgba(255, 255, 255, 0.87);
  --color-text-secondary: rgba(255, 255, 255, 0.60);
}
```

**MUI Theme Configuration (`src/styles/theme.ts`):**

```typescript
import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
      light: '#42a5f5',
      dark: '#1565c0',
    },
    secondary: {
      main: '#9c27b0',
      light: '#ba68c8',
      dark: '#7b1fa2',
    },
    error: {
      main: '#d32f2f',
    },
    warning: {
      main: '#ed6c02',
    },
    info: {
      main: '#0288d1',
    },
    success: {
      main: '#2e7d32',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
  },
  spacing: 8, // Base spacing unit
  shape: {
    borderRadius: 8,
  },
});
```

---
