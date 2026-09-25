# Torii architecture

> **Phase 0 done**: flight deal search over several data sources, smart caching, users
> with plans and quotas, and a persistent price history.
>
> Mermaid diagrams render directly on GitHub. The use case diagram is PlantUML (paste it
> into [plantuml.com](https://www.plantuml.com/plantuml) or
> [planttext.com](https://www.planttext.com)).

## 1. What Torii does

Users don't search for a flight on fixed dates. They give a **holiday window** (e.g.
July to September), a **base trip length** (e.g. 14 days) and a **variability** (e.g. +3,
so stays from 14 to 17 days). Torii goes through every date combination with a
**sliding window algorithm** and returns the top N cheapest offers (total round-trip
price).

The core design problem: every date combination is a call to a paid API with a limited
quota. The whole architecture is built around **making as few of those calls as
possible and protecting them**: a cache with variable TTL, failover between providers,
search precision levels and per-plan quotas.

**Stack:** Java 21 + Spring Boot 4 (REST API) · PostgreSQL on Supabase (Flyway + JPA) ·
Spring Security + JWT · React + TypeScript + Tailwind/Untitled UI (separate SPA).

## 2. Components

```mermaid
flowchart TB
    subgraph FRONTEND["Frontend · React SPA (separate origin, CORS)"]
        UI["Search · Login · Plans<br/>My searches · Price chart"]
    end

    subgraph API["API layer (com.torii.api / auth)"]
        SC["SearchController<br/>POST /api/search"]
        AC["AuthController<br/>/api/auth/signup · /login"]
        MC["MeController<br/>/api/me · /searches · /usage · /plan"]
        PHC["PriceHistoryController<br/>GET /api/price-history"]
        SEC["SecurityConfig<br/>stateless JWT · CORS"]
    end

    subgraph SERVICES["Service layer"]
        SS["SearchService<br/>(orchestrator)"]
        QS["PlanQuotaService<br/>(per-plan quotas)"]
        AS["AuthService<br/>(BCrypt + JwtService)"]
        PHS["PriceHistoryService"]
        SHS["SearchHistoryService"]
    end

    subgraph ENGINE["Core: search engine"]
        ENG["SlidingWindowEngine<br/>(sliding window,<br/>virtual threads + semaphore)"]
        CACHE["CachingFlightProvider<br/>(Caffeine, TTL based<br/>on how soon the trip is)"]
        FAIL["FailoverFlightProvider<br/>(tries sources in order,<br/>parks exhausted ones)"]
        P1["SerpApi<br/>(Google Flights)"]
        P2["FlightAPI.io"]
        P3["Amadeus<br/>(ready, off by default)"]
        P4["Mock<br/>(safety net)"]
    end

    DB[("PostgreSQL · Supabase<br/>users · searches<br/>price_history · plan_usage")]

    UI -- "HTTP/JSON (+ Bearer JWT)" --> API
    SC --> SS
    AC --> AS
    MC --> QS & SHS
    PHC --> PHS
    SS --> QS
    SS --> ENG
    SS --> PHS & SHS
    ENG -- "searchOffers(date pair)" --> CACHE
    CACHE -- "miss" --> FAIL
    FAIL --> P1 & P2 & P3 & P4
    AS & QS & PHS & SHS -- "JPA/Hibernate" --> DB
```

The key point: the engine **doesn't know where the data comes from**. `SlidingWindowEngine`
talks to the `FlightProvider` interface, and the cache, the failover and the real sources
are stacked behind it without the algorithm changing a single line.

## 3. Use cases (PlantUML)

```plantuml
@startuml
left to right direction
skinparam actorStyle awesome

actor "Anonymous traveller" as Anon
actor "Registered traveller" as User
actor "Flight APIs\n(SerpApi · FlightAPI · Amadeus)" as APIs
Anon <|-- User

rectangle "Torii" {
  usecase "Search offers\n(window + length + variability)" as UC1
  usecase "See route on the map" as UC2
  usecase "See route\nprice history" as UC3
  usecase "Sign up" as UC4
  usecase "Log in" as UC5
  usecase "See and repeat\nrecent searches" as UC6
  usecase "Change plan\n(FREE / PRO / BUSINESS)" as UC7
  usecase "Enforce monthly\nlookup quota" as UC8
  usecase "Query providers\nwith cache and failover" as UC9
  usecase "Record best price\nof the day (history)" as UC10
}

Anon --> UC1
Anon --> UC2
Anon --> UC3
Anon --> UC4
Anon --> UC5
User --> UC6
User --> UC7

UC1 ..> UC8 : <<include>>\n(registered only)
UC1 ..> UC9 : <<include>>
UC1 ..> UC10 : <<include>>
UC9 --> APIs
@enduml
```

## 4. What happens during a search

```mermaid
sequenceDiagram
    autonumber
    participant F as Frontend
    participant C as SearchController
    participant Q as PlanQuotaService
    participant S as SearchService
    participant E as SlidingWindowEngine
    participant K as CachingFlightProvider
    participant FO as FailoverFlightProvider
    participant X as SerpApi / FlightAPI
    participant DB as PostgreSQL

    F->>C: POST /api/search (+ JWT if logged in)
    C->>S: search(request, userId?)
    alt logged in user
        S->>Q: consume(userId, lookups)
        Q->>DB: plan_usage for the month
        alt quota exceeded
            Q-->>F: 429 + message ("upgrade your plan...")
        end
    end
    S->>E: findBestOffers(request)
    Note over E: builds every (outbound, return) pair<br/>based on precision (FAST/BALANCED/EXHAUSTIVE)
    par each date pair (virtual threads, max 6 at once)
        E->>K: searchOffers(pair)
        alt cached and not expired
            K-->>E: cached offers (0 calls)
        else miss
            K->>FO: searchOffers(pair)
            FO->>X: query the first available source
            alt source out of quota (429)
                FO->>FO: park the source for 5 min<br/>and try the next one
            end
            X-->>FO: real offers
            FO-->>K: offers
            K->>K: store with TTL based on how soon<br/>(far trip 7d ... imminent 10min)
        end
    end
    E->>E: filter maxPrice · sort by price · top N
    E-->>S: best offers
    S->>DB: price_history (best price of the day, no mocks)
    S->>DB: searches (history, anonymous or per user)
    S-->>F: 200 · offers (round-trip total)
```

Details that matter:

- **Parallelism with virtual threads**: date pairs are queried in parallel (Java 21
  virtual threads) with a global semaphore (`torii.search.max-concurrency`) so we don't
  blow through the APIs' rate limits. The final order is deterministic (price, then date,
  then airline) even though responses come back in any order.
- **Quota is charged BEFORE doing any work**: with no balance left the search is rejected
  without a single external call.
- **DB writes are best effort**: if the DB goes down the search still answers (try/catch
  in the orchestrator). The quota check is NOT best effort, it's a business rule.
- **maxPrice is filtered inside the engine** and never sent to the APIs, so the cache
  works for any budget.

## 5. Engine class diagram (Decorator + Composite)

```mermaid
classDiagram
    class FlightProvider {
        <<interface>>
        +searchOffers(origin, dest, depart, return, maxStops) List~FlightOffer~
        +name() String
    }

    class SlidingWindowEngine {
        -maxConcurrency: int
        +findBestOffers(SearchRequest) List~FlightOffer~
        +countQueries(SearchRequest) int
    }

    class CachingFlightProvider {
        -cache: Caffeine
        -ttlPolicy: TripTtlPolicy
        +stats() CacheStats
    }

    class FailoverFlightProvider {
        -providers: List~FlightProvider~
        -parkedUntil: Map~String,Instant~
        -cooldown: Duration
    }

    class TripTtlPolicy {
        +ttlFor(departDate) Duration
    }

    class SerpApiFlightProvider
    class FlightApiFlightProvider
    class AmadeusFlightProvider
    class MockFlightProvider

    SlidingWindowEngine --> FlightProvider : uses
    CachingFlightProvider ..|> FlightProvider
    FailoverFlightProvider ..|> FlightProvider
    SerpApiFlightProvider ..|> FlightProvider
    FlightApiFlightProvider ..|> FlightProvider
    AmadeusFlightProvider ..|> FlightProvider
    MockFlightProvider ..|> FlightProvider
    CachingFlightProvider o--> "1" FlightProvider : decorates
    CachingFlightProvider --> TripTtlPolicy
    FailoverFlightProvider o--> "1..*" FlightProvider : tries in order
```

How it's actually wired (in `ProviderConfig`):

```
Engine -> Cache( Failover( [SerpApi, FlightAPI, Amadeus*, Mock] ) )
```

- **Decorator**: `CachingFlightProvider` wraps another provider and adds caching without
  anyone else knowing. The cache works per *date pair*, so overlapping searches reuse
  hundreds of pairs.
- **Composite/Chain**: `FailoverFlightProvider` is a provider made of providers. It tries
  them in order, and when a source runs out of quota (`ProviderQuotaExceededException`)
  it parks it for a cooldown and moves on. The Mock always goes last: it never fails, so
  the app always answers.
- **Adding a new source = one class + one bean.** No changes to the algorithm.

## 6. Authentication (stateless JWT)

```mermaid
sequenceDiagram
    autonumber
    participant F as Frontend
    participant A as AuthController
    participant AS as AuthService
    participant DB as PostgreSQL
    participant R as Resource Server<br/>(Security filter)

    F->>A: POST /api/auth/signup {name, email, password}
    A->>AS: signup()
    AS->>DB: INSERT users (password -> BCrypt hash)
    AS-->>F: 201 {JWT, user}
    Note over F: stores the token (localStorage)<br/>and sends it on every request

    F->>R: GET /api/me/searches · Authorization: Bearer token
    R->>R: checks HS256 signature and expiry<br/>(no DB hit, no sessions)
    R-->>F: 401 if invalid / expired
    R->>DB: token subject = user id
    R-->>F: 200 · my searches
```

- Passwords: only the **BCrypt hash** is stored (slow, salted, one way).
- The token is **stateless**: the backend keeps no sessions, the signature (secret
  `TORII_JWT_SECRET`) is the only source of truth. Tokens expire after 24h.
- Searching is **public**: with a token the search is linked to the user, without one
  it's anonymous. Only `/api/me/**` requires authentication.

## 7. Data model

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar name
        varchar plan "FREE | PRO | BUSINESS"
        timestamptz created_at
    }
    SEARCHES {
        bigint id PK
        bigint user_id FK "NULL = anonymous"
        varchar origin
        varchar destination
        date range_start
        date range_end
        int base_duration
        int variability
        varchar search_precision
        numeric max_price "NULL = no limit"
        int queries_used
        timestamptz created_at
    }
    PRICE_HISTORY {
        bigint id PK
        varchar origin
        varchar destination
        date observed_on "UNIQUE(route, day)"
        numeric best_price
        varchar currency
        varchar provider
    }
    PLAN_USAGE {
        bigint user_id PK, FK
        date usage_month PK "first day of the month"
        int queries_used
    }

    USERS ||--o{ SEARCHES : "runs"
    USERS ||--o{ PLAN_USAGE : "uses quota"
```

- The schema is driven by **Flyway migrations** versioned in git (`db/migration`).
  Hibernate only validates (`ddl-auto=validate`). Tests run the same migrations on
  in-memory H2, offline and against the real schema.
- `price_history` **fills itself**: every real search records (or improves) the best
  price of the day for its route, so the frontend chart grows for free as people use it.
  Mock offers are excluded so they don't pollute real data.

## 8. Business rules: plans and quotas

| Plan | Lookups/month | Note |
|---|---|---|
| FREE | 30 | every account starts here |
| PRO | 500 | |
| BUSINESS | unlimited | |

One **search** uses N **lookups** (one per date pair explored, depending on the window,
the variability and the precision). The frontend estimates N live before searching. The
backend computes the exact number (`SlidingWindowEngine.countQueries`) and subtracts it
in `plan_usage`. No balance left means a `429` with a message telling the user what to do.

## 9. Architecture decisions

| Decision | Why |
|---|---|
| Modular monolith (no microservices) | One deploy and simple transactions. The packages (`provider`, `algorithm`, `history`, `auth`, `user`) mark the seams in case it ever needs splitting |
| `FlightProvider` interface as the boundary | The algorithm doesn't depend on any specific API. Mock first from day one |
| Cache per date pair with variable TTL | A trip 6 months out doesn't change price every hour (7 day TTL), an imminent one does (10 min TTL) |
| PostgreSQL (Supabase) + Flyway | Textbook relational data. Versioned, reproducible schema |
| Stateless JWT + BCrypt | No session state on the server, scaling out is trivial |
| Frontend and backend on separate origins | SPA can go on a CDN. CORS is explicit per environment |

## 10. Roadmap

- **Done (phase 0):** engine + cache + multi-source failover + full UI + DB +
  users/plans/quotas + price history.
- **Next:** real deployment (SPA on Vercel/Netlify, dockerized backend), price drop
  alerts, a "price calendar" endpoint (turning ~300 calls into 1), rate limiting by IP for
  anonymous users, payment gateway for the plans.
