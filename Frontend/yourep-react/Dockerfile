# --- Build Stage ---
FROM node:22-alpine AS build
WORKDIR /app

# Copy package.json and install dependencies (cached layer)
COPY package.json package-lock.json* ./
RUN npm ci

# Copy all source and build React production app
COPY . .
ARG REACT_APP_API_URL
ENV REACT_APP_API_URL=$REACT_APP_API_URL
RUN npm run build

# --- Run Stage ---
FROM nginx:1.27-alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
