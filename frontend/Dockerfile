# Multi-stage build for React + TypeScript + Vite frontend
# NOTE: Full Docker configuration will be implemented in Story 1.2

FROM node:20.11.0-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.25-alpine
COPY --from=build /app/dist /usr/share/nginx/html
# nginx configuration will be added in Story 1.2
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
