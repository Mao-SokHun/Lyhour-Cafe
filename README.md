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

## Telegram Mini App (TMA) Setup

Lyhour Coffee includes a complete, mobile-first Telegram Mini App:
- **Browse menu**: Instant category filter, search, full item customization (Size, Sweetness 0-100%, Ice level, Special notes).
- **Fulfillment**: Pickup, Dine-in (with table selection T1–T8), or Delivery.
- **Payments**: Bakong KHQR (Cambodia QR payment with QR generation & demo mode), Cash / Pay at Pickup, Stripe.
- **Loyalty & Rewards**: Auto-binds Telegram user accounts, awards loyalty points, shows tier badges.
- **Live Order Tracking**: Real-time status stepper (Received → Brewing → Ready → Completed) with barista Telegram alerts.

### 1. Create your Telegram Bot with @BotFather
1. Open Telegram and search for `@BotFather`.
2. Send `/newbot`, name your bot (e.g. `Lyhour Coffee`) and username (e.g. `LyhourCafe_bot`).
3. Copy the HTTP API token provided by BotFather.

### 2. Configure Environment Variables
In `.env` (local) or on Render dashboard:
```properties
APP_BASE_URL=https://your-app.onrender.com
APP_TELEGRAM_ENABLED=true
APP_TELEGRAM_BOT_TOKEN=123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11
```

### 3. Add Mini App to Telegram Bot
You have two options:

#### Option A: Auto-Configure (One-Click from Admin Dashboard)
1. Log in to the Admin Dashboard (`admin` / `admin123`).
2. Go to **Settings** → click **Auto-Configure Bot Menu Button**.
3. The app calls Telegram's API to set the persistent **"☕ Order Coffee"** button and `/menu` commands!

#### Option B: Manual Setup via @BotFather
- **Set Chat Menu Button**:
  1. In `@BotFather`, send `/setmenubutton`.
  2. Select your bot `@LyhourCafe_bot`.
  3. Send the Mini App URL: `https://your-app.onrender.com/telegram-app`
  4. Send the button title: `☕ Order Coffee`
- **Or create a direct Telegram App link (`t.me/LyhourCafe_bot/menu`)**:
  1. In `@BotFather`, send `/newapp`.
  2. Select your bot `@LyhourCafe_bot`.
  3. Title: `Lyhour Coffee`
  4. Description: `Handcrafted coffee, artisan drinks, and pastries`
  5. Upload an image (640x360 px).
  6. Web App URL: `https://your-app.onrender.com/telegram-app`
  7. Short name: `menu`

### 4. Test the Mini App
- **In Telegram**: Open `@LyhourCafe_bot` and tap **"☕ Order Coffee"** or send `/start` or `/menu`.
- **In Browser (local dev)**: Open `http://localhost:8080/telegram-app` directly.

## Default logins

| Role | Username | Password |
|------|----------|----------|
| Admin | admin | admin123 |
| Manager | manager | manager123 |
| Customer | walkin | walkin123 |
