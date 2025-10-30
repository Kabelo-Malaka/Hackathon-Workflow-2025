# 2. Frontend Tech Stack

## Technology Stack Table

This section is synchronized with the main architecture document (docs/architecture.md) and extracts all frontend-related technologies.

| Category | Technology | Version | Purpose | Rationale |
|----------|-----------|---------|---------|-----------|
| **Language** | TypeScript | 5.3.3 | Frontend development language | Type safety, excellent tooling, PRD requirement, prevents runtime errors |
| **Runtime (Build)** | Node.js | 20.11.0 LTS | JavaScript build tooling | LTS version, stable for Vite and npm ecosystem |
| **Framework** | React | 18.2.0 | UI framework | PRD requirement, component-based architecture, excellent ecosystem, concurrent rendering |
| **Build Tool** | Vite | 5.0.12 | Frontend build tool & dev server | PRD requirement, lightning-fast HMR, native ESM support, optimized production builds |
| **State Management** | Redux Toolkit | 2.1.0 | Global state management | PRD requirement, official Redux approach, reduces boilerplate, excellent DevTools |
| **API Client** | RTK Query | 2.1.0 | Data fetching & caching | Integrated with Redux Toolkit, automatic cache invalidation, optimistic updates |
| **OpenAPI Codegen** | OpenAPI Generator | 7.2.0 | TypeScript client generation | Auto-generates type-safe API client from backend OpenAPI spec, ensures contract compliance |
| **UI Component Library** | Material-UI (MUI) | 5.15.7 | React component library | PRD requirement, enterprise-grade components, WCAG AA accessibility, Magna BC customizable theming |
| **Form Management** | React Hook Form | 7.49.3 | Complex form validation | PRD requirement, handles template builder forms, minimal re-renders, easy validation |
| **Form Validation** | Yup | 1.3.3 | Schema-based validation | Integration with React Hook Form, declarative validation rules, TypeScript support |
| **Routing** | React Router | 6.21.3 | Client-side routing | Industry standard, nested routes, protected route patterns, loader/action API |
| **HTTP Client** | Axios | 1.6.5 | HTTP requests | Interceptor support for auth, request/response transformation, timeout handling |
| **Date Handling** | date-fns | 3.2.0 | Date manipulation & formatting | Lightweight, tree-shakeable, consistent formatting across app |
| **Testing (Unit)** | Jest | 29.7.0 | Unit testing framework | PRD requirement, React ecosystem standard, snapshot testing, mocking utilities |
| **Testing (Component)** | React Testing Library | 14.1.2 | Component testing | PRD requirement, best practices for React, user-centric queries, accessibility checks |
| **Testing (E2E)** | Playwright | 1.41.2 | End-to-end testing | Cross-browser testing, auto-wait, screenshot/video capture, parallel execution |
| **Code Quality** | ESLint | 8.56.0 | Linting | Vite template default, catches bugs, enforces coding standards |
| **ESLint Config** | eslint-config-react-app | 7.0.1 | React linting rules | Create React App standards, TypeScript support |
| **Code Formatting** | Prettier | 3.2.4 | Code formatting | Consistent code style, integrates with ESLint, reduces formatting debates |
| **CSS-in-JS** | Emotion | 11.11.3 | Styling solution | MUI's default styling engine, performant, TypeScript support, SSR-compatible |
| **Icons** | Material Icons | 5.15.7 | Icon library | Bundled with MUI, 2000+ icons, SVG-based, customizable |
| **DevTools** | Redux DevTools Extension | - | State debugging | Time-travel debugging, action replay, state diff visualization |
| **DevTools** | React Developer Tools | - | Component debugging | Component tree inspection, props/state debugging, profiler |
| **IDE** | VSCode | 1.86+ | Frontend development | PRD recommended, excellent React/TypeScript support, extensions ecosystem |

## Version Pinning Strategy

**Exact Version Pinning:**
- All production dependencies use exact versions (no `^` or `~`) in `package.json`
- Ensures reproducible builds across development, CI/CD, and production environments
- Prevents unexpected breaking changes from minor/patch updates

**Update Strategy:**
- Review dependency updates monthly during maintenance windows
- Run full test suite before updating any dependency
- Update dependencies one at a time to isolate potential issues
- Use `npm outdated` to identify available updates

**Critical Dependencies (Never Auto-Update):**
- React, Redux Toolkit, Material-UI, React Hook Form
- Changes to these require thorough testing of all components

**Lock File Management:**
- Commit `package-lock.json` to version control
- Use `npm ci` in CI/CD pipelines for deterministic installs
- Developers use `npm install` for local development

## Technology Alignment with Backend

The frontend stack is carefully chosen to integrate seamlessly with the backend architecture:

**Type Safety Across Stack:**
- Backend OpenAPI 3.0 spec → OpenAPI Generator → TypeScript client
- Shared types ensure frontend and backend stay in sync
- Compile-time errors catch API contract violations

**Authentication Integration:**
- Frontend Axios interceptor detects 401 responses from Spring Security
- Automatic redirect to login page when session expires (15-minute timeout)
- JSESSIONID cookie managed transparently by browser

**State Management Integration:**
- RTK Query wraps auto-generated API client
- Automatic cache invalidation on mutations
- Optimistic updates for better UX (e.g., task completion)

**Build Process Integration:**
- OpenAPI Generator runs during `npm run build` (fetches `/api-docs` from backend)
- Generated TypeScript client placed in `src/services/generated/`
- Vite bundles application with generated client included

---
