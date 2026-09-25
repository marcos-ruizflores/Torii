# Torii

Flight deal finder for flexible travellers. Instead of searching fixed dates, you give
Torii a holiday window, how long you want to stay and how flexible you are, and it finds
the cheapest round trips across every date combination.

> Example: "BCN to Tokyo, sometime between July and September, 14 to 17 days".
> Torii checks every departure day and trip length in that window and returns the top 5.

![Java](https://img.shields.io/badge/Java_21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![React](https://img.shields.io/badge/React_19-20232A?logo=react&logoColor=61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)

## How it works

Every date combination is one call to a paid flight API with a limited quota, so most
of the design is about making as few calls as possible:

- **Sliding window search** over (departure date, trip length) pairs, run in parallel on
  Java 21 virtual threads with a semaphore to stay under the providers' rate limits.
- **Per date pair cache** (Caffeine) with a TTL that depends on how close the trip is:
  7 days for flights months away, 10 minutes for flights in the next couple of days.
- **Failover between providers** (SerpApi / Google Flights, FlightAPI.io, Amadeus). When
  one runs out of quota it gets parked for a cooldown and the next one takes over. A
  deterministic mock provider sits at the end so the app always answers.
- **Search precision levels** (FAST / BALANCED / EXHAUSTIVE) that trade coverage for
  fewer calls, tied to **per-plan monthly quotas** that are checked before any call is made.
- **Price history** per route, built for free from real searches and shown as a chart.

```
SlidingWindowEngine -> Cache( Failover( [SerpApi, FlightAPI, Amadeus, Mock] ) )
```

The engine only knows about the `FlightProvider` interface, so the cache, the failover
and each real source are stacked behind it without touching the algorithm. More detail,
with sequence, class and ER diagrams, in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Tech stack

| | |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security (stateless JWT), Spring Data JPA, Flyway, Caffeine |
| Database | PostgreSQL on Supabase, H2 in PostgreSQL mode for tests |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS v4, Untitled UI / React Aria, TanStack Query, Recharts |
| Infra | Docker (multi-stage build), deployable to Railway |
| Testing | JUnit Jupiter, AssertJ, MockRestServiceServer, Spring Boot test slices |

## Running locally

Requirements: JDK 21 and Node 24.

```bash
# Backend (http://localhost:8080)
export SUPABASE_DB_URL=...          # or any PostgreSQL JDBC URL
export SUPABASE_DB_USER=...
export SUPABASE_DB_PASSWORD=...
export SERPAPI_KEY=...              # optional, falls back to the mock provider
./mvnw spring-boot:run

# Frontend (http://localhost:5173, proxies /api to the backend)
cd frontend
npm install
npm run dev
```

Provider keys are always read from environment variables and every real provider can
be toggled in `application.properties` (`torii.<provider>.enabled`). With no keys at
all, searches are served by the mock provider.

## Tests

```bash
./mvnw test
```

Tests run offline: the database is in-memory H2 with the real Flyway migrations applied,
and the external APIs are replaced by `MockRestServiceServer` or fake providers.

## API

| Method | Endpoint | Auth |
|---|---|---|
| POST | `/api/search` | optional |
| GET | `/api/price-history?origin=&destination=&days=` | no |
| POST | `/api/auth/signup`, `/api/auth/login` | no |
| GET | `/api/me`, `/api/me/searches`, `/api/me/usage` | JWT |
| POST | `/api/me/plan` | JWT |
| GET | `/api/cache/stats` | no |

Request and response examples live in [examples/](examples/README.md).

## Roadmap

- Price drop alerts
- A "price calendar" endpoint to turn ~300 lookups into a single call
- IP rate limiting for anonymous searches
- Payment gateway for the paid plans
