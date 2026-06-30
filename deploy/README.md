# Deploy Environment

This directory is used to start local or deployment dependencies for ReportGenerate.

## Services

- MySQL: business metadata storage
- Redis: cache, distributed lock, lightweight queue
- MinIO: object storage for templates, references, generated reports, and preview files
- MinIO Init: creates the default bucket

MyBatis is the Java persistence framework inside the application, not an independent Docker service.

## Usage

```bash
cd deploy
cp .env.example .env
docker compose --env-file .env -f docker-compose-env.yml up -d
```

Check services:

```bash
docker compose --env-file .env -f docker-compose-env.yml ps
```

Stop services:

```bash
docker compose --env-file .env -f docker-compose-env.yml down
```

Stop services and remove volumes:

```bash
docker compose --env-file .env -f docker-compose-env.yml down -v
```

## Application Environment

Use these variables when the application connects to the Compose environment:

```bash
export DB_URL='jdbc:mysql://localhost:3306/report_generate?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export DB_USERNAME=report
export DB_PASSWORD=report
export REDIS_HOST=localhost
export REDIS_PORT=6379
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
export MINIO_BUCKET=report-generate
```

MinIO Console:

```text
http://localhost:9001
```

## First Phase Modules

The repository now includes the first-phase foundation for file, MyBatis, Redis, and template modules:

- MyBatis: XML mapper path is `classpath*:mapper/**/*.xml`; pagination uses PageHelper.
- MySQL init script: `deploy/mysql/init/01_file_template_schema.sql`, including `file_object`, `template`, and `template_variable`.
- Redis key prefix: all keys use `dgp:`; the reserved report generation queue key is `dgp:queue:generate-task`.
- File APIs implemented from the interface document: `POST /api/files`, `GET /api/files/{fileId}`, `GET /api/files/{fileId}/access-url`, `DELETE /api/files/{fileId}`.
- Template APIs implemented from the interface document: `POST /api/templates`, `GET /api/templates`, `GET /api/templates/{templateId}`, `GET /api/templates/{templateId}/variables`, `POST /api/templates/{templateId}/enable`, `POST /api/templates/{templateId}/disable`.
- File storage: file content is stored in MinIO, MySQL only stores metadata. Download and preview access must use backend-generated presigned URLs; MinIO real URLs are not exposed directly.
- Delete policy: public file delete only logically deletes metadata. Physical MinIO object cleanup is kept as an internal compensation capability for failed upload/template creation flows and can later be moved to a scheduled cleanup task.
- Template storage path: `templates/{reportType}/{templateId}/{versionNo}/{filename}`.
- Template variable parsing supports only `${variable}` and `{{variable}}` placeholders in `.docx` files for this phase.
- Deferred template APIs: manual variable update, template download URL, preview URL, and structure query are left for later render/preview tasks.
