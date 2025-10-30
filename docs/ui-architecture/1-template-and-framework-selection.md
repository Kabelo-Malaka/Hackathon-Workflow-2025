# 1. Template and Framework Selection

## Change Log

| Date | Version | Description | Author |
|------|---------|-------------|--------|
| 2025-10-30 | 1.0 | Initial frontend architecture document | Winston (Architect) |

## Starter Template Decision

**Selected Template:** Vite React-TypeScript template (`npm create vite@latest`)

**Rationale:**

The frontend architecture is built on the **Vite React-TypeScript starter template**, which provides an optimal foundation for modern React development with several key advantages:

1. **Lightning-Fast Development Experience:**
   - Vite's native ESM-based dev server provides instant hot module replacement (HMR)
   - Sub-second cold starts compared to traditional bundlers
   - On-demand compilation ensures only modified modules are rebuilt

2. **TypeScript-First Approach:**
   - Pre-configured TypeScript support with strict mode enabled
   - Seamless integration with React's TypeScript definitions
   - Excellent IDE support and type checking out of the box

3. **Modern Build Optimization:**
   - Production builds use Rollup for optimal tree-shaking and code splitting
   - Automatic CSS code splitting and lazy loading
   - Built-in support for async chunks and dynamic imports

4. **Minimal Configuration Overhead:**
   - Zero-config setup for React + TypeScript
   - Sensible defaults that align with industry best practices
   - Easy to extend with plugins when needed

5. **Alignment with PRD Requirements:**
   - PRD explicitly specifies Vite 5.0.12 as the build tool
   - Template provides immediate productivity for hackathon timeline
   - Community-maintained template ensures stability and best practices

## Pre-installed Capabilities

The Vite React-TypeScript template provides:

**Out-of-the-Box Features:**
- React 18.2.0 with Fast Refresh enabled
- TypeScript 5.x with strict type checking
- Vite dev server with HMR
- ESLint configuration for code quality
- Production build optimization with Rollup

**File Structure:**
```
frontend/
├── public/              # Static assets
├── src/
│   ├── App.tsx         # Root component
│   ├── main.tsx        # Entry point
│   ├── index.css       # Global styles
│   └── vite-env.d.ts   # Vite type definitions
├── .eslintrc.cjs       # ESLint configuration
├── tsconfig.json       # TypeScript configuration
├── tsconfig.node.json  # TypeScript for Vite config
├── vite.config.ts      # Vite configuration
└── package.json
```

**Additional Dependencies Required:**

Based on the backend architecture's technology stack, the following dependencies will be added to the Vite starter:

1. **State Management:** Redux Toolkit 2.1.0 + RTK Query 2.1.0
2. **UI Components:** Material-UI (MUI) 5.15.7
3. **Routing:** React Router 6.21.3
4. **Form Handling:** React Hook Form 7.49.3
5. **API Client:** Auto-generated from OpenAPI spec via OpenAPI Generator 7.2.0
6. **Testing:** Jest 29.7.0 + React Testing Library 14.1.2
7. **Additional Utilities:** Date-fns, Yup validation, React Query DevTools

## Integration with Backend Architecture

The frontend template selection directly aligns with the backend architecture document (docs/architecture.md):

**API Integration Strategy:**
- Backend exposes OpenAPI 3.0 specification at `/api-docs` endpoint
- OpenAPI Generator (v7.2.0) auto-generates TypeScript client with type-safe interfaces
- Generated client is integrated into Vite build process
- RTK Query consumes generated types for data fetching and caching

**Authentication Flow:**
- Backend uses session-based authentication (Spring Security)
- Frontend receives JSESSIONID cookie (15-minute timeout)
- Axios interceptor handles 401 responses and redirects to login
- Protected routes check authentication state before rendering

**Deployment Integration:**
- Frontend builds to optimized static files via `npm run build`
- nginx serves static files and proxies `/api` requests to backend:8080
- Docker Compose orchestrates frontend (nginx), backend, and PostgreSQL
- Environment variables injected at build time for API base URL

---
