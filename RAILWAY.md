# Railway Deployment

This repo can deploy as one Dockerized service:

- Vite builds first.
- The compiled frontend is copied into Spring Boot static resources.
- Spring Boot serves `/api/**`, Stripe webhooks, and the SPA from the same Railway domain.

## Railway Service

Create one Railway service from the repository root. Railway will auto-detect the root `Dockerfile`.

Add a Railway Postgres database to the same project, then set these backend variables on the app service:

```text
POSTGRES_HOST=${{Postgres.PGHOST}}
POSTGRES_PORT=${{Postgres.PGPORT}}
POSTGRES_DB=${{Postgres.PGDATABASE}}
POSTGRES_USER=${{Postgres.PGUSER}}
POSTGRES_PASSWORD=${{Postgres.PGPASSWORD}}
JWT_BASE64_SECRET=<openssl rand -base64 64 | tr -d '\n'>
APP_BASE_URL=https://<your-app-domain>
APP_CORS_ALLOWED_ORIGIN=https://<your-app-domain>
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=Lax
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
VITE_CLOUDINARY_CLOUD_NAME=<cloudinary-cloud-name>
VITE_CLOUDINARY_UPLOAD_PRESET=<unsigned-upload-preset>
```

Leave `SPRING_PROFILES_ACTIVE` unset for a clean production database. Use `demo` only when you intentionally want seeded demo data.

Because frontend and backend are served from the same domain in this Docker image, keep `VITE_API_BASE_URL` empty or unset.

## Seeded Demo Data

To deploy with the curated seed data, set:

```text
SPRING_PROFILES_ACTIVE=demo
```

Seeded users are inserted with a non-public random password hash. To make demo accounts loginable, provide one of these secrets:

```text
SEEDED_ACCOUNT_PASSWORD=<strong password for all @artdrop.local seeded users>
```

or, for narrower access:

```text
DEMO_ACCOUNT_PASSWORD=<strong password for the demo account>
DEMO_ADMIN_PASSWORD=<strong password for the mateo admin account>
```

`/api/auth/demo-login` uses `DEMO_ACCOUNT_PASSWORD`. If `SEEDED_ACCOUNT_PASSWORD` is set, it is also used as the fallback demo/admin password.

## Stripe Webhook

In Stripe Dashboard, add this webhook endpoint after Railway gives the service a public domain:

```text
https://<your-app-domain>/api/checkout/webhook
```

Subscribe to:

```text
checkout.session.completed
checkout.session.expired
payment_intent.payment_failed
charge.refunded
```

Copy the endpoint signing secret into `STRIPE_WEBHOOK_SECRET`.

The Stripe CLI is only needed for local development, not for Railway.

## Local Docker Build

```bash
docker build -t artdropapp .
docker run --rm -p 8089:8089 \
  --env-file ArtDrop/.env \
  -e POSTGRES_HOST=host.docker.internal \
  -e APP_BASE_URL=http://localhost:8089 \
  -e APP_CORS_ALLOWED_ORIGIN=http://localhost:8089 \
  artdropapp
```

Then open:

```text
http://localhost:8089
```
