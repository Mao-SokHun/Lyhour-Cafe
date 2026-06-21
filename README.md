# Lyhour-Cafe

Spring Boot cafe shop — menu, cart, orders, admin dashboard, POS, Stripe payments, and REST API.

## Run locally

```bash
.\mvnw.cmd spring-boot:run
```

Open http://localhost:8080

## Database

Copy `src/main/resources/application-neon.properties.example` to `application-neon.properties` and add your Neon PostgreSQL credentials.

For local H2: `--spring.profiles.active=h2`

## Default logins

| Role | Username | Password |
|------|----------|----------|
| Admin | admin | admin123 |
| Manager | manager | manager123 |
| Customer | walkin | walkin123 |
