# Arquitectura de Torii

> **Fase 0 completada** — buscador de ofertas de vuelo con múltiples fuentes de datos,
> caché inteligente, usuarios con planes y cuotas, e histórico de precios persistente.
>
> Los diagramas Mermaid se renderizan automáticamente en GitHub. El diagrama de casos
> de uso está en PlantUML (pégalo en [plantuml.com](https://www.plantuml.com/plantuml) o
> [planttext.com](https://www.planttext.com)).

## 1. ¿Qué hace Torii?

El usuario no busca un vuelo con fechas fijas: da un **rango de vacaciones** (ej. julio–septiembre),
una **duración base** (ej. 14 días) y una **variabilidad** (ej. +3 → estancias de 14 a 17 días).
Torii explora todas las combinaciones de fechas con un **algoritmo de ventana deslizante**
y devuelve el top-N de ofertas más baratas (precio total de ida y vuelta).

El problema central del diseño: cada combinación de fechas es una llamada a una API de pago
con cuota limitada. Toda la arquitectura gira alrededor de **minimizar y proteger esas llamadas**:
caché con TTL variable, failover entre proveedores, precisiones de búsqueda y cuotas por plan.

**Stack:** Java 21 + Spring Boot 4 (API REST) · PostgreSQL en Supabase (Flyway + JPA) ·
Spring Security + JWT · React + TypeScript + Tailwind/Untitled UI (SPA separada).

## 2. Vista de componentes

```mermaid
flowchart TB
    subgraph FRONTEND["Frontend · React SPA (origen separado, CORS)"]
        UI["Buscador · Login · Planes<br/>Mis búsquedas · Gráfico precios"]
    end

    subgraph API["Capa API (com.torii.api / auth)"]
        SC["SearchController<br/>POST /api/search"]
        AC["AuthController<br/>/api/auth/signup · /login"]
        MC["MeController<br/>/api/me · /searches · /usage · /plan"]
        PHC["PriceHistoryController<br/>GET /api/price-history"]
        SEC["SecurityConfig<br/>JWT stateless · CORS"]
    end

    subgraph SERVICIOS["Capa de servicios"]
        SS["SearchService<br/>(orquestador)"]
        QS["PlanQuotaService<br/>(cuotas por plan)"]
        AS["AuthService<br/>(BCrypt + JwtService)"]
        PHS["PriceHistoryService"]
        SHS["SearchHistoryService"]
    end

    subgraph MOTOR["El corazón: motor de búsqueda"]
        ENG["SlidingWindowEngine<br/>(ventana deslizante,<br/>virtual threads + semáforo)"]
        CACHE["CachingFlightProvider<br/>(Caffeine, TTL variable<br/>según cercanía del viaje)"]
        FAIL["FailoverFlightProvider<br/>(prueba fuentes en orden,<br/>aparca las agotadas)"]
        P1["SerpApi<br/>(Google Flights)"]
        P2["FlightAPI.io"]
        P3["Amadeus<br/>(preparado)"]
        P4["Mock<br/>(red de seguridad)"]
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
    ENG -- "searchOffers(par de fechas)" --> CACHE
    CACHE -- "miss" --> FAIL
    FAIL --> P1 & P2 & P3 & P4
    AS & QS & PHS & SHS -- "JPA/Hibernate" --> DB
```

La clave: el motor **no sabe de dónde salen los datos**. `SlidingWindowEngine` habla con la
interfaz `FlightProvider`, y detrás de ella se apilan la caché, el failover y las fuentes
reales sin que el algoritmo cambie una línea.

## 3. Casos de uso (PlantUML)

```plantuml
@startuml
left to right direction
skinparam actorStyle awesome

actor "Viajero anónimo" as Anon
actor "Viajero registrado" as User
actor "APIs de vuelos\n(SerpApi · FlightAPI · Amadeus)" as APIs
Anon <|-- User

rectangle "Torii" {
  usecase "Buscar ofertas\n(rango + duración + variabilidad)" as UC1
  usecase "Ver ruta en el mapa" as UC2
  usecase "Ver histórico de precios\nde una ruta" as UC3
  usecase "Registrarse" as UC4
  usecase "Iniciar sesión" as UC5
  usecase "Ver y repetir\nmis últimas búsquedas" as UC6
  usecase "Cambiar de plan\n(FREE / PRO / BUSINESS)" as UC7
  usecase "Controlar cuota mensual\nde consultas" as UC8
  usecase "Consultar proveedores\ncon caché y failover" as UC9
  usecase "Registrar mejor precio\ndel día (histórico)" as UC10
}

Anon --> UC1
Anon --> UC2
Anon --> UC3
Anon --> UC4
Anon --> UC5
User --> UC6
User --> UC7

UC1 ..> UC8 : <<include>>\n(solo registrados)
UC1 ..> UC9 : <<include>>
UC1 ..> UC10 : <<include>>
UC9 --> APIs
@enduml
```

## 4. El corazón: qué pasa en una búsqueda

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

    F->>C: POST /api/search (+ JWT si hay sesión)
    C->>S: search(request, userId?)
    alt usuario con sesión
        S->>Q: consume(userId, nº consultas)
        Q->>DB: plan_usage del mes
        alt cuota agotada
            Q-->>F: 429 + mensaje ("mejora tu plan...")
        end
    end
    S->>E: findBestOffers(request)
    Note over E: genera todos los pares (ida, vuelta)<br/>según precisión (FAST/BALANCED/EXHAUSTIVE)
    par cada par de fechas (virtual threads, máx. 6 a la vez)
        E->>K: searchOffers(par)
        alt en caché y no caducado
            K-->>E: ofertas cacheadas (0 llamadas)
        else miss
            K->>FO: searchOffers(par)
            FO->>X: consulta la 1ª fuente disponible
            alt cuota de la fuente agotada (429)
                FO->>FO: aparca la fuente 5 min<br/>y prueba la siguiente
            end
            X-->>FO: ofertas reales
            FO-->>K: ofertas
            K->>K: guarda con TTL según cercanía<br/>(viaje lejano 7d ... inminente 10min)
        end
    end
    E->>E: filtra maxPrice · ordena por precio · top-N
    E-->>S: mejores ofertas
    S->>DB: price_history (mejor precio del día, sin mocks)
    S->>DB: searches (historial, anónimo o del usuario)
    S-->>F: 200 · ofertas (precio total ida+vuelta)
```

Detalles que importan:

- **Paralelismo con hilos virtuales**: los pares de fechas se consultan en paralelo
  (Java 21 virtual threads) con un semáforo global (`torii.search.max-concurrency`) para
  no reventar el rate limit de las APIs. El orden final es determinista (precio → fecha →
  aerolínea) aunque las respuestas lleguen desordenadas.
- **La cuota se cobra ANTES de trabajar**: si no queda saldo, se rechaza sin gastar ni una
  llamada externa.
- **Los registros en BD son "best effort"**: si la BD fallara, la búsqueda responde igual
  (try/catch en el orquestador). La cuota NO: esa es regla de negocio.
- **maxPrice se filtra en cliente del motor**, nunca viaja a las APIs: así la caché sirve
  para cualquier presupuesto.

## 5. Diagrama de clases del motor (patrones Decorador + Composite)

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

    SlidingWindowEngine --> FlightProvider : usa
    CachingFlightProvider ..|> FlightProvider
    FailoverFlightProvider ..|> FlightProvider
    SerpApiFlightProvider ..|> FlightProvider
    FlightApiFlightProvider ..|> FlightProvider
    AmadeusFlightProvider ..|> FlightProvider
    MockFlightProvider ..|> FlightProvider
    CachingFlightProvider o--> "1" FlightProvider : decora
    CachingFlightProvider --> TripTtlPolicy
    FailoverFlightProvider o--> "1..*" FlightProvider : intenta en orden
```

La composición real (montada en `ProviderConfig`):

```
Engine → Caché( Failover( [SerpApi, FlightAPI, Amadeus*, Mock] ) )
```

- **Decorador**: `CachingFlightProvider` envuelve a otro provider y añade caché sin que
  nadie más lo sepa. La caché vive a nivel de *par de fechas*, así búsquedas solapadas
  reutilizan cientos de pares.
- **Composite/Chain**: `FailoverFlightProvider` es un provider hecho de providers: prueba
  en orden, y si una fuente agota cuota (excepción `ProviderQuotaExceededException`) la
  aparca con cooldown y sigue con la siguiente. El `Mock` va siempre último: nunca falla,
  la app siempre responde.
- **Añadir una fuente nueva = una clase + un bean.** Cero cambios en el algoritmo.

## 6. Autenticación (JWT sin estado)

```mermaid
sequenceDiagram
    autonumber
    participant F as Frontend
    participant A as AuthController
    participant AS as AuthService
    participant DB as PostgreSQL
    participant R as Resource Server<br/>(filtro de Security)

    F->>A: POST /api/auth/signup {name, email, password}
    A->>AS: signup()
    AS->>DB: INSERT users (password → hash BCrypt)
    AS-->>F: 201 {token JWT, user}
    Note over F: guarda el token (localStorage)<br/>y lo envía en cada petición

    F->>R: GET /api/me/searches · Authorization: Bearer token
    R->>R: verifica firma HS256 y caducidad<br/>(sin tocar BD: sin sesiones)
    R-->>F: 401 si inválido / caducado
    R->>DB: subject del token = id de usuario
    R-->>F: 200 · mis búsquedas
```

- Contraseñas: solo se guarda el **hash BCrypt** (lento y con sal, irreversible).
- El token es **stateless**: el backend no guarda sesiones; la firma (secreto
  `TORII_JWT_SECRET`) es la única verdad. Caduca a las 24 h.
- Buscar es **público**: con token, la búsqueda se asocia al usuario; sin él, es anónima.
  Solo `/api/me/**` exige autenticación.

## 7. Modelo de datos

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
        bigint user_id FK "NULL = anónima"
        varchar origin
        varchar destination
        date range_start
        date range_end
        int base_duration
        int variability
        varchar search_precision
        numeric max_price "NULL = sin límite"
        int queries_used
        timestamptz created_at
    }
    PRICE_HISTORY {
        bigint id PK
        varchar origin
        varchar destination
        date observed_on "UNIQUE(ruta, día)"
        numeric best_price
        varchar currency
        varchar provider
    }
    PLAN_USAGE {
        bigint user_id PK, FK
        date usage_month PK "primer día del mes"
        int queries_used
    }

    USERS ||--o{ SEARCHES : "realiza"
    USERS ||--o{ PLAN_USAGE : "consume cuota"
```

- El esquema lo gobiernan **migraciones Flyway** versionadas en git (`db/migration`);
  Hibernate solo valida (`ddl-auto=validate`). Los tests corren las mismas migraciones
  sobre H2 en memoria: sin red y validando el esquema real.
- `price_history` se **alimenta solo**: cada búsqueda real registra (o mejora) el mejor
  precio del día de su ruta — el gráfico del frontend crece gratis con el uso. Las ofertas
  del Mock se excluyen para no contaminar datos reales.

## 8. Reglas de negocio: planes y cuotas

| Plan | Consultas/mes | Nota |
|---|---|---|
| FREE | 30 | plan inicial de toda cuenta |
| PRO | 500 | |
| BUSINESS | ilimitado | |

Una **búsqueda** consume N **consultas** (una por par de fechas explorado; depende del
rango, la variabilidad y la precisión). El frontend estima N en vivo antes de buscar;
el backend calcula el número exacto (`SlidingWindowEngine.countQueries`) y lo descuenta
en `plan_usage`. Sin saldo → `429` con mensaje accionable.

## 9. Decisiones de arquitectura (resumen)

| Decisión | Por qué |
|---|---|
| Monolito modular (no microservicios) | Un solo deploy y transacciones simples; los paquetes (`provider`, `algorithm`, `history`, `auth`, `user`) marcan las costuras por si algún día hay que partirlo |
| Interfaz `FlightProvider` como frontera | El algoritmo no depende de ninguna API concreta; mock-first desde el día 1 |
| Caché por par de fechas con TTL variable | Un viaje a 6 meses vista no cambia de precio cada hora (TTL 7 días); uno inminente sí (TTL 10 min) |
| PostgreSQL (Supabase) + Flyway | Datos relacionales de libro; esquema versionado y reproducible |
| JWT stateless + BCrypt | Sin estado de sesión en el servidor; escala horizontal trivial |
| Frontend y backend en orígenes separados | SPA desplegable en CDN; CORS explícito por entorno |

## 10. Roadmap

- **Hecho (fase 0):** motor + caché + failover multi-fuente + UI completa + BD +
  usuarios/planes/cuotas + histórico de precios.
- **Siguiente:** despliegue real (SPA en Vercel/Netlify, backend dockerizado), alertas de
  bajada de precio, endpoint "calendario de precios" (convertir ~300 llamadas en 1),
  límite por IP para anónimos, pasarela de pago para los planes.
