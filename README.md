# LinkVault

LinkVault is a full-stack personal resource vault for organizing links, documents, files, notes, and code snippets in a structured workspace. It combines vaults, nested folders, tags, favorites, smart link previews, and secure in-app file previews so users can quickly save, search, inspect, and revisit important digital resources.

## Highlights

- Vault and nested folder organization for clean resource grouping.
- Resource types for links, files, notes, and code snippets.
- Smart link preview with Open Graph, Twitter Card, favicon, canonical URL, site name, title, description, and thumbnail metadata.
- Secure file upload through Cloudinary with a 20 MB limit and extension allow-list.
- In-app file preview for PDFs, DOCX text extraction, images, text/code files, and downloadable fallback for unsupported formats.
- Professional folder tree that displays folders and contained resources with file-type icons.
- Tags, favorites, archive actions, full search/filter flow, and dashboard summary.
- JWT authentication, per-user ownership checks, CORS configuration, and structured API responses.
- Consistent API response envelope with `success`, `message`, `data`, `errorCode`, `details`, and `timestamp`.
- Flyway-managed initial schema with UUID primary keys, foreign keys, unique constraints, and query indexes.
- Docker Compose setup for client, server, PostgreSQL, Redis, RabbitMQ, and pgAdmin.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | Angular 21, TypeScript, Bootstrap utility classes, Material Symbols |
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA |
| Database | PostgreSQL 16 |
| Cache / Rate Limit | Redis 7.4 |
| Message Broker | RabbitMQ 3-management |
| Storage | Cloudinary |
| API Docs | Springdoc OpenAPI / Swagger UI |
| Containers | Docker, Docker Compose, Nginx |
| Tests | Maven test profile with H2 |

## Architecture

```text
link-vault/
├── client/                 # Angular SPA served by Nginx in Docker
│   ├── src/app/core        # Auth state, guards, interceptors, HTTP client
│   ├── src/app/shared      # Shared API/error models
│   ├── src/app/features    # Feature data-access, models, route components
│   └── nginx.conf          # SPA routing and /api reverse proxy
├── server/                 # Spring Boot REST API
│   ├── src/main/java/com/linkvault
│   │   ├── auth            # JWT authentication and auth use cases
│   │   ├── common          # Response contract, error codes, config, pagination
│   │   ├── dashboard       # Summary and health endpoints
│   │   ├── folders         # Nested folder management
│   │   ├── resources       # Resource CRUD, search, mapping, previews, cleanup
│   │   ├── storage         # Cloudinary integration
│   │   ├── tags            # Tag management
│   │   └── vaults          # Vault management
│   ├── src/main/resources/db/migration
│   │   └── V1__initial_schema.sql
│   └── src/test            # Backend test configuration
├── database/               # Optional PostgreSQL init scripts
├── docker-compose.yml      # Local production-like stack
└── .env.example            # Environment template
```

## Feature Overview

### Resource Management

- Create resources inside a vault root or a selected folder.
- Upload files directly from the browser with drag-and-drop.
- Store links with auto-fetched preview metadata.
- Save notes and code snippets with language labels.
- Search by title, URL, description, and content.
- Mark resources as favorite or archived.
- Attach and detach tags.

### Smart Link Preview

LinkVault can fetch and store preview metadata for HTTP/HTTPS links:

- `previewTitle`
- `previewDescription`
- `faviconUrl`
- `siteName`
- `canonicalUrl`
- `thumbnailUrl`
- `previewFetchedAt`
- `previewStatus`
- `previewError`

The backend includes SSRF protections, redirect limits, request timeout, HTML size limits, and graceful failure handling. A link can still be saved even when metadata fetching fails.

### File Preview

Supported preview flows include:

- PDF preview through a secure backend file proxy.
- DOCX text extraction for readable document preview.
- Image preview from authenticated blob URLs.
- Text/code preview for common developer formats.
- Download fallback for unsupported formats.

Upload constraints are configured at both application and Cloudinary levels:

- Maximum file size: `20MB`
- Allowed formats: images, PDF, Office files, text/code files, archives, audio, and video formats listed in `.env.example`.

### Folder Tree

The vault sidebar shows a navigable tree with:

- Nested folders.
- Resource counts per folder branch.
- Root-level resources under `All Resources`.
- Per-resource icons for PDF, DOCX, sheets, slides, images, archives, media, links, notes, and snippets.
- Direct navigation from a tree resource to its detail page.

## Getting Started

### Prerequisites

- Node.js 24 or compatible with Angular 21.
- npm 11.
- Java 21.
- Docker and Docker Compose.
- Cloudinary account for file uploads.

## Run with Docker Compose

This is the recommended way to run the complete stack.

1. Create an environment file:

```bash
cp .env.example .env
```

2. Update `.env` with secure values:

```env
POSTGRES_PASSWORD=change-me
PGADMIN_PASSWORD=change-me
APP_JWT_SECRET=replace-with-a-long-random-secret-at-least-32-chars
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_FLYWAY_ENABLED=true
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
```

3. Build and start the stack:

```bash
docker compose up --build
```

4. Open the app:

```text
Frontend: http://localhost:4200
API:      http://localhost:8080
Swagger:  http://localhost:8080/swagger-ui.html
pgAdmin:  http://localhost:5050
RabbitMQ: http://localhost:15672 (guest/guest)
```

The Dockerized frontend proxies `/api` requests to the backend service through Nginx.

## Local Development

### Start PostgreSQL, Redis, RabbitMQ

```bash
docker compose up -d postgres redis rabbitmq
```

### Start the Backend

```bash
cd server
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
cd server
.\mvnw.cmd spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

### Start the Frontend

```bash
cd client
npm install
npm run start
```

The frontend runs at:

```text
http://localhost:4200
```

Development environment files point Angular to:

```text
http://localhost:8080/api
```

## Environment Variables

| Variable | Description | Default |
| --- | --- | --- |
| `POSTGRES_DB` | PostgreSQL database name | `link_vault_db` |
| `POSTGRES_USER` | PostgreSQL username | `linkvault` |
| `POSTGRES_PASSWORD` | PostgreSQL password | `linkvault123` |
| `PGADMIN_EMAIL` | pgAdmin login email | `admin@linkvault.dev` |
| `PGADMIN_PASSWORD` | pgAdmin login password | `admin123` |
| `REDIS_PASSWORD` | Redis password used by Docker Compose and backend | `linkvaultredis123` |
| `REDIS_DATABASE` | Redis logical database index | `0` |
| `REDIS_TIMEOUT` | Redis command timeout | `2s` |
| `APP_REDIS_ENABLED` | Enable Redis cache/rate-limit layer | `true` |
| `APP_REDIS_KEY_PREFIX` | Prefix for all Redis keys | `linkvault` |
| `APP_REDIS_RATE_LIMIT_ENABLED` | Enable Redis-backed rate limiting | `true` |
| `APP_JWT_ISSUER` | JWT issuer | `link-vault-api` |
| `APP_JWT_SECRET` | JWT signing secret | required for Docker/prod |
| `APP_JWT_EXPIRATION_MINUTES` | Token lifetime in minutes | `120` |
| `APP_SEED_DEMO_DATA` | Seed demo data on startup | `false` |
| `APP_CORS_ALLOWED_ORIGINS` | Allowed browser origins | `http://localhost:4200,http://127.0.0.1:4200` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Hibernate schema mode | `validate` |
| `SPRING_FLYWAY_ENABLED` | Enable Flyway migrations | `true` |
| `SPRING_FLYWAY_BASELINE_ON_MIGRATE` | Baseline existing non-empty dev schemas | `true` |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud name | empty |
| `CLOUDINARY_API_KEY` | Cloudinary API key | empty |
| `CLOUDINARY_API_SECRET` | Cloudinary API secret | empty |
| `CLOUDINARY_FOLDER` | Upload folder in Cloudinary | `link-vault` |
| `CLOUDINARY_USE_SECURE_URL` | Prefer HTTPS asset URLs | `true` |
| `CLOUDINARY_MAX_FILE_SIZE_MB` | Upload limit | `20` |
| `CLOUDINARY_ALLOWED_FORMATS` | Allowed upload extensions | see `.env.example` |

## API Surface

Main REST endpoints:

| Area | Endpoints |
| --- | --- |
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`, `GET /api/auth/availability` |
| Auth Sessions | `POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /api/auth/sessions`, `DELETE /api/auth/sessions/{sessionId}` |
| Workspaces | `GET/POST /api/workspaces`, `GET/PUT/DELETE /api/workspaces/{workspaceId}`, `GET /api/workspaces/{workspaceId}/usage` |
| Workspace Members | `GET /api/workspaces/{workspaceId}/members`, `PATCH /api/workspaces/{workspaceId}/members/{memberId}/role`, `DELETE /api/workspaces/{workspaceId}/members/{memberId}` |
| Workspace Invitations | `GET/POST /api/workspaces/{workspaceId}/invitations`, `DELETE /api/workspaces/{workspaceId}/invitations/{invitationId}`, `POST /api/workspace-invitations/{token}/accept`, `POST /api/workspace-invitations/{token}/decline` |
| Workspace Audit Logs | `GET /api/workspaces/{workspaceId}/audit-logs` |
| Dashboard | `GET /api/dashboard/summary` |
| Vaults | `GET/POST /api/vaults`, `GET/PUT/DELETE /api/vaults/{id}` |
| Folders | `GET /api/vaults/{vaultId}/folders`, `GET/PUT/DELETE /api/folders/{id}`, `POST /api/folders/{parentId}/children` |
| Resources | `GET /api/resources`, `GET /api/resources/search`, `GET/PUT/DELETE /api/resources/{id}` |
| Uploads | `POST /api/vaults/{vaultId}/resources/upload`, `POST /api/folders/{folderId}/resources/upload` |
| Preview | `POST /api/link-preview`, `PATCH /api/resources/{id}/refresh-preview`, `GET /api/resources/{id}/file`, `GET /api/resources/{id}/document-preview` |
| Tags | `GET/POST /api/tags`, `PUT/DELETE /api/tags/{id}` |

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

API responses use one envelope for success and errors:

```json
{
  "success": true,
  "message": "Resources loaded",
  "data": {},
  "errorCode": null,
  "details": null,
  "timestamp": "2026-06-08T00:00:00Z"
}
```

Errors return `success: false` with an `errorCode` such as `AUTH_INVALID_CREDENTIALS`, `RESOURCE_NOT_FOUND`, `VAULT_ACCESS_DENIED`, `VALIDATION_ERROR`, or `STORAGE_UPLOAD_FAILED`.

## Database Migrations

Flyway runs migrations from:

```text
server/src/main/resources/db/migration
```

The initial migration creates users, vaults, folders, resources, tags, resource tag links, and resource view history with UUID primary keys, foreign keys, and indexes for common ownership/filter queries. Existing non-empty development schemas can be baselined with `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`; production deployments should keep `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`.

## Quality Checks

Frontend build:

```bash
cd client
npm run build
```

Backend package:

```bash
cd server
./mvnw -DskipTests package
```

Backend tests:

```bash
cd server
./mvnw test
```

Docker build:

```bash
docker compose build
```

## Demo Flow

1. Register or log in.
2. Create a vault with a custom icon and color.
3. Add nested folders.
4. Upload a PDF or DOCX and preview it inside the app.
5. Add a link and fetch its smart preview metadata.
6. Add tags and mark resources as favorites.
7. Use the vault folder tree to open files/resources directly from their folder.
8. Search resources across vaults from the global search bar.

## Security Notes

- JWT secrets must be provided through `APP_JWT_SECRET` before Docker/prod startup.
- Redis is used for distributed rate limiting and short-lived cache layers for dashboard, vaults, folders, resources, tags, quotas, and link previews.
- File preview is served through backend-controlled endpoints instead of exposing raw storage access in the UI.
- Link preview fetching validates URL scheme and blocks local/private network targets.
- Cloudinary credentials should be provided through environment variables and never committed.
- `.env` is intentionally excluded from version control.

## Project Status

LinkVault is ready for local demo and GitHub presentation. It includes a complete full-stack flow for authenticated resource organization, file upload/preview, smart link previews, and professional workspace navigation.


