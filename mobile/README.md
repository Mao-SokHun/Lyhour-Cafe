# Lyhour Coffee — Mobile (iOS & Android)

Native Flutter app for customers: menu, cart, orders, push notifications.

## Run locally

1. Start the Spring Boot API: `.\mvnw.cmd spring-boot:run` (port 8080)
2. From this folder:

```bash
flutter pub get
# Android emulator → host machine
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
# Physical device on same Wi‑Fi → your PC IP
flutter run --dart-define=API_BASE_URL=http://192.168.x.x:8080
# Production (Render)
flutter run --dart-define=API_BASE_URL=https://your-app.onrender.com
```

Default login (same as web): `admin` / `admin123`

## Push notifications (FCM)

1. Create a [Firebase](https://console.firebase.google.com) project
2. Add Android + iOS apps, download `google-services.json` / `GoogleService-Info.plist`
3. Run `dart pub global activate flutterfire_cli` then `flutterfire configure`
4. Add `firebase_messaging` to `pubspec.yaml` and register the FCM token in Profile
5. On Render, set:
   - `APP_PUSH_ENABLED=true`
   - `APP_FCM_SERVER_KEY=<Firebase legacy server key>`

The backend sends push when orders are placed or status changes.

## Telegram (staff alerts)

On Render:

```
APP_TELEGRAM_ENABLED=true
APP_TELEGRAM_BOT_TOKEN=123456:ABC...
APP_TELEGRAM_CHAT_ID=your_chat_id
```

Test from **Admin → Settings → Test Telegram**.

## Build release

```bash
flutter build apk --dart-define=API_BASE_URL=https://your-app.onrender.com
flutter build ios --dart-define=API_BASE_URL=https://your-app.onrender.com
```

## API used

| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/mobile/config` | App config (public) |
| `POST /api/v1/auth/login` | JWT login |
| `GET /api/v1/mobile/home` | Menu + loyalty |
| `GET /api/v1/orders` | Order history |
| `POST /api/v1/orders` | Checkout |
| `POST /api/v1/mobile/push-token` | Register FCM token |
