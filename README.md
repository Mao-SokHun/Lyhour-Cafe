# Lyhour-Cafe

Spring Boot cafe shop — menu, cart, orders, admin dashboard, POS, Stripe payments, and REST API.

## Run locally

Set Neon credentials (see `.env.example`), then:

```bash
# PowerShell — set env vars from .env manually, or use your IDE env config
.\mvnw.cmd spring-boot:run
```

Open http://localhost:8080

**Local H2 (no Neon):** `--spring.profiles.active=h2`

## Deploy on Render

1. Push this repo to GitHub: https://github.com/Mao-SokHun/Lyhour-Cafe
2. Go to [Render Dashboard](https://dashboard.render.com) → **New** → **Blueprint** (or **Web Service**)
3. Connect the `Lyhour-Cafe` repo — Render reads `render.yaml` automatically for Blueprint
4. Set these **Environment Variables** (required):

| Variable | Example |
|----------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://ep-xxx-pooler.region.aws.neon.tech/neondb?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `neondb_owner` |
| `SPRING_DATASOURCE_PASSWORD` | *(your Neon password)* |

5. Optional — after first deploy, set your public URL:

| Variable | Value |
|----------|-------|
| `APP_CORS_ALLOWED_ORIGINS` | `https://your-app.onrender.com` |
| `APP_STRIPE_SUCCESS_URL` | `https://your-app.onrender.com/order/success?session_id={CHECKOUT_SESSION_ID}` |
| `APP_STRIPE_CANCEL_URL` | `https://your-app.onrender.com/cart?cancelled=true` |

6. Deploy — Render builds the Docker image and starts the app on port `$PORT`.

**Note:** Free tier sleeps after inactivity (~50s cold start on first visit).

### Manual Web Service setup (without Blueprint)

- **Runtime:** Docker
- **Dockerfile path:** `./Dockerfile`
- **Health check path:** `/home`
- Add the environment variables above

## Default logins

| Role | Username | Password |
|------|----------|----------|
| Admin | admin | admin123 |
| Manager | manager | manager123 |
| Customer | walkin | walkin123 |
