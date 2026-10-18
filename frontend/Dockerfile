# Stage 1: type-check and build the static files. VITE_API_URL stays empty, so the browser
# calls /api on the same origin and nginx forwards it to the API (no CORS needed).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: serve the files with nginx and proxy /api to the API container
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
