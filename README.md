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
- Docker Compose setup for client, server, PostgreSQL, and pgAdmin.

## Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | Angular 21, TypeScript, Bootstrap utility classes, Material Symbols |
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA |
| Database | PostgreSQL 16 |
| Storage | Cloudinary |
| API Docs | Springdoc OpenAPI / Swagger UI |
| Containers | Docker, Docker Compose, Nginx |
| Tests | Maven test profile with H2 |

## Architecture

```text
link-vault/
├── client/                 # Angular SPA served by Nginx in Docker
│   ├── src/app/core        # Shared models and API services
│   ├── src/app/features    # Auth, dashboard, vaults, folders, resources, tags
│   └── nginx.conf          # SPA routing and /api reverse proxy
├── server/                 # Spring Boot REST API
│   ├── src/main/java/com/linkvault
│   │   ├── auth            # JWT authentication
│   │   ├── dashboard       # Summary and health endpoints
│   │   ├── folders         # Nested folder management
│   │   ├── resources       # Resource CRUD, previews, uploads
│   │   ├── storage         # Cloudinary integration
│   │   ├── tags            # Tag management
│   │   └── vaults          # Vault management
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
```

The Dockerized frontend proxies `/api` requests to the backend service through Nginx.

## Local Development

### Start PostgreSQL

```bash
docker compose up -d postgres
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
| `APP_JWT_ISSUER` | JWT issuer | `link-vault-api` |
| `APP_JWT_SECRET` | JWT signing secret | development fallback |
| `APP_JWT_EXPIRATION_MINUTES` | Token lifetime in minutes | `120` |
| `APP_SEED_DEMO_DATA` | Seed demo data on startup | `false` |
| `APP_CORS_ALLOWED_ORIGINS` | Allowed browser origins | `http://localhost:4200,http://127.0.0.1:4200` |
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

- JWT secrets must be replaced before any real deployment.
- File preview is served through backend-controlled endpoints instead of exposing raw storage access in the UI.
- Link preview fetching validates URL scheme and blocks local/private network targets.
- Cloudinary credentials should be provided through environment variables and never committed.
- `.env` is intentionally excluded from version control.

## Project Status

LinkVault is ready for local demo and GitHub presentation. It includes a complete full-stack flow for authenticated resource organization, file upload/preview, smart link previews, and professional workspace navigation.
