FROM node:20-alpine AS build

WORKDIR /app

ARG VITE_API_URL=http://localhost:8081
ARG VITE_WS_URL=ws://localhost:8081/ws
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_WS_URL=$VITE_WS_URL

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

FROM nginx:stable-alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
