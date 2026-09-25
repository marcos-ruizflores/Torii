# Torii frontend

React + TypeScript SPA for Torii, built with Vite, Tailwind CSS v4 and Untitled UI
components (React Aria under the hood).

## Scripts

```bash
npm install
npm run dev       # dev server on :5173, proxies /api to the backend on :8080
npm run build     # type check + production build
npm run lint      # oxlint
```

## Configuration

`VITE_API_URL` sets the backend base URL for production builds, where the SPA and the
API live on different origins. Leave it empty in development. See `.env.example`.

## Structure

```
src/
  api/          HTTP client and typed calls to the backend
  auth/         session context (JWT in localStorage)
  components/   app components + Untitled UI base components
  hooks/        useSearch (TanStack Query mutation) and UI hooks
  pages/        login, sign up, plans, my searches, 404
```
