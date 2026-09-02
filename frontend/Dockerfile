FROM node:24-alpine AS build

WORKDIR /app

ARG VITE_API_URL=http://localhost:8081
ARG VITE_WS_URL=http://localhost:8081/ws
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_WS_URL=$VITE_WS_URL

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:stable-alpine
COPY --from=build /app/dist/client /usr/share/nginx/html
RUN cp /usr/share/nginx/html/_shell.html /usr/share/nginx/html/index.html 2>/dev/null || true
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 --start-period=10s \
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1
CMD ["nginx", "-g", "daemon off;"]
