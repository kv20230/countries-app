# Build stage: production Angular build.
FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Run stage: nginx serves the app and forwards /api to the backend.
FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/Frontend/browser /usr/share/nginx/html
EXPOSE 80
