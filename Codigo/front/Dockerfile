FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci --include=dev

COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /dist/front /usr/share/nginx/html

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
