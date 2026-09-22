FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci --include=dev

COPY . .
RUN npm run build

FROM node:22-alpine

WORKDIR /app

COPY server.mjs ./server.mjs
COPY --from=build /dist/front ./dist

EXPOSE 8080

CMD ["node", "server.mjs"]
