# LinkVault Deployment Guide

LinkVault is a modern workspace for saving, organizing, and securely sharing links and resources for teams. This guide covers deploying LinkVault to a production environment.

## Prerequisites

Before deploying LinkVault, ensure you have the following infrastructure components available:

- **PostgreSQL 15+**: For relational data storage.
- **Redis 7+**: For caching, rate limiting, and temporary state.
- **RabbitMQ 3+**: For message queuing and asynchronous processing (link preview fetching, resource deletion).
- **Cloudinary Account**: For file and thumbnail storage.

## Environment Variables

The server requires several environment variables to be set in production.

### General & Security
- `APP_FRONTEND_URL`: URL of the frontend app (e.g. `https://app.linkvault.io`).
- `APP_JWT_SECRET`: A secure, randomly generated string (minimum 32 characters).
- `APP_JWT_EXPIRATION_MINUTES`: Expiration time for access tokens (e.g. `15`).
- `APP_REFRESH_TOKEN_EXPIRATION_DAYS`: Expiration time for refresh tokens (e.g. `7`).

### Database
- `SPRING_DATASOURCE_URL`: JDBC URL (e.g. `jdbc:postgresql://postgres-host:5432/linkvault`).
- `SPRING_DATASOURCE_USERNAME`: Database username.
- `SPRING_DATASOURCE_PASSWORD`: Database password.

### Redis
- `SPRING_DATA_REDIS_HOST`: Redis host.
- `SPRING_DATA_REDIS_PORT`: Redis port (default `6379`).
- `SPRING_DATA_REDIS_PASSWORD`: (Optional) Redis password.

### RabbitMQ
- `SPRING_RABBITMQ_HOST`: RabbitMQ host.
- `SPRING_RABBITMQ_PORT`: RabbitMQ port (default `5672`).
- `SPRING_RABBITMQ_USERNAME`: RabbitMQ username.
- `SPRING_RABBITMQ_PASSWORD`: RabbitMQ password.

### Cloudinary (File Storage)
- `CLOUDINARY_CLOUD_NAME`: Cloudinary cloud name.
- `CLOUDINARY_API_KEY`: Cloudinary API key.
- `CLOUDINARY_API_SECRET`: Cloudinary API secret.

## Deployment Options

### Option 1: Docker Compose (Single Node)

A complete `docker-compose.yml` is typically provided to run the Server, Frontend, PostgreSQL, Redis, and RabbitMQ together. Make sure to update the environment variables or pass them via an `.env` file.

1. Build the images:
   ```bash
   cd server && docker build -t linkvault-server:latest .
   cd ../client && docker build -t linkvault-client:latest .
   ```
2. Start the services:
   ```bash
   docker-compose up -d
   ```

### Option 2: Kubernetes / Cloud

1. Deploy the infrastructure components (PostgreSQL, Redis, RabbitMQ) via managed services or Helm charts.
2. Push the Docker images (`linkvault-server` and `linkvault-client`) to your container registry.
3. Deploy the backend using a Deployment and Service, ensuring the environment variables are injected via Secrets/ConfigMaps.
4. Deploy the frontend. Ensure it sits behind a CDN or Ingress that properly routes API requests (e.g., `/api/*`) to the backend service.

## CI/CD Pipeline

A GitHub Actions workflow (`.github/workflows/ci.yml`) is included for automatic testing and image building. It handles:
- Backend Maven tests with temporary Postgres/Redis/RabbitMQ containers.
- Frontend Angular tests using ChromeHeadless.
- Docker image building for both client and server upon merging to `main`.

## Security Best Practices

- **Proxy Trust**: LinkVault is configured with `server.forward-headers-strategy: framework` to correctly parse `X-Forwarded-For` from reverse proxies. Make sure your load balancer/ingress controller sets these headers securely.
- **Rate Limiting**: Public endpoints are rate-limited via Redis. Adjust limits in `RedisProperties` if necessary.
- **Token Security**: Keep `APP_JWT_SECRET` highly secure. Do not commit it to version control.
