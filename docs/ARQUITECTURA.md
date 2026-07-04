# Arquitectura de Torii (estado actual)

> Estado: MVP del algoritmo + caché Caffeine. Fuente de datos = mock (todavía no Amadeus).

## 1. Diagrama de componentes

Cómo están organizadas las piezas y quién depende de quién. Cada flecha es una
dependencia (A → B significa "A usa B").

```
                          CLIENTE
              (script buscar-oferta.sh  /  curl  /  futura UI)
                                 │  HTTP JSON
                                 ▼
┌───────────────────────────── CAPA API (com.torii.api) ─────────────────────────┐
│                                                                                 │
│   SearchController            CacheController          ApiExceptionHandler       │
│   POST /api/search            GET /api/cache/stats     (traduce errores a 400)   │
│        │                            │                        ▲                   │
│        │ usa SearchRequestDto       │                        │ captura           │
│        │ (@Valid + toDomain())──────┼────────────────────────┘                   │
└────────┼────────────────────────────┼──────────────────────────────────────────┘
         │ SearchRequest (dominio)     │ lee estadísticas
         ▼                             │
┌─── CAPA SERVICIO ───┐                │
│   SearchService     │                │
│   (orquestador;     │                │
│    futura caché de  │                │
│    búsqueda + BD)   │                │
└────────┬────────────┘                │
         │ findBestOffers()            │
         ▼                             │
┌─── CAPA ALGORITMO ──────────┐        │
│   SlidingWindowEngine       │        │
│   (ventana deslizante:      │        │
│    duraciones × fechas)     │        │
└────────┬────────────────────┘        │
         │ searchOffers(par de fechas) │
         ▼                             │
┌─── CAPA PROVIDER (interfaz FlightProvider) ───────────────────────────────────┐
│                                                                               │
│   CachingFlightProvider  ◄────────────────────────────────────┘ (@Primary)    │
│   (decorador con caché)                                                        │
│      │  · Caffeine Cache<CacheKey, List<FlightOffer>>                          │
│      │  · TripExpiry ──► TripTtlPolicy  (TTL variable según cercanía)          │
│      │                                                                         │
│      │ si MISS: delega en la fuente real                                       │
│      ▼                                                                         │
│   MockFlightProvider   (hoy: datos falsos deterministas)                       │
│   └─ mañana se sustituye por AmadeusFlightProvider, sin tocar nada más         │
└───────────────────────────────────────────────────────────────────────────────┘

  Modelo compartido (com.torii.model):  SearchRequest · FlightOffer   (records inmutables)
  Cableado (com.torii.config):          ProviderConfig  (monta caché @Primary sobre mock + Clock)
```

Idea clave: **cada capa solo conoce la interfaz de la de debajo**. El algoritmo
habla con `FlightProvider`, no con "la caché" ni con "el mock". Por eso podemos
cambiar la fuente (Amadeus) o la caché (Redis) sin tocar el resto.


## 2. Workflow de una request nueva

Qué ocurre, paso a paso, cuando llega un `POST /api/search`.

```
CLIENTE        SearchController     SearchRequestDto    SearchService   SlidingWindowEngine   CachingFlightProvider   MockFlightProvider
  │                  │                    │                  │                  │                     │                     │
  │  POST /api/search│                    │                  │                  │                     │                     │
  │  (JSON) ────────►│                    │                  │                  │                     │                     │
  │                  │ Spring deserializa │                  │                  │                     │                     │
  │                  │ JSON → DTO + @Valid │                  │                  │                     │                     │
  │                  │───────────────────►│                  │                  │                     │                     │
  │                  │                    │ (validación de formato: IATA, fechas, rangos…)             │                     │
  │                  │◄───── si falla ────┤  ──► ApiExceptionHandler ──► 400 Bad Request ──► CLIENTE   │                     │
  │                  │                    │                  │                  │                     │                     │
  │                  │ dto.toDomain()     │                  │                  │                     │                     │
  │                  │───────────────────►│ (validación cruzada: rangeEnd>start, cabe la estancia…)    │                     │
  │                  │◄── SearchRequest ──┤                  │                  │                     │                     │
  │                  │                    │                  │                  │                     │                     │
  │                  │ search(request) ──────────────────────►│                 │                     │                     │
  │                  │                    │                  │ findBestOffers() ►│                     │                     │
  │                  │                    │                  │                  │                     │                     │
  │                  │                    │          ┌───────┤  BUCLE: para cada duración (14..14+V)   │                     │
  │                  │                    │          │       │   y cada fecha de salida del rango:     │                     │
  │                  │                    │          │       │ searchOffers(ida,vuelta) ──────────────►│                     │
  │                  │                    │          │       │                     │ ¿está en caché?   │                     │
  │                  │                    │          │       │                     │  HIT → devuelve    │                     │
  │                  │                    │          │       │                     │  MISS ────────────►│ genera ofertas     │
  │                  │                    │          │       │                     │◄─── List<Offer> ───┤                     │
  │                  │                    │          │       │                     │  guarda con TTL    │                     │
  │                  │                    │          │       │                     │  (TripTtlPolicy)   │                     │
  │                  │                    │          │       │◄──── List<Offer> ───┤                     │                     │
  │                  │                    │          └───────┤  (acumula candidatas)                   │                     │
  │                  │                    │                  │                  │                     │                     │
  │                  │                    │                  │  ordena por precio ASC, toma topN        │                     │
  │                  │                    │◄── List<FlightOffer> ────────────────│                     │                     │
  │                  │◄─ List<FlightOffer>┤                  │                  │                     │                     │
  │◄── 200 OK (JSON) ┤ Spring serializa   │                  │                  │                     │                     │
  │                  │                    │                  │                  │                     │                     │
```

### Resumen en palabras

1. **Entra el JSON** → Spring lo convierte en `SearchRequestDto`.
2. **Validación de formato** (`@Valid`): IATA de 3 letras, fechas futuras, topN 1-50…
   Si falla, `ApiExceptionHandler` responde **400** y se acaba aquí.
3. **`toDomain()`**: validaciones cruzadas (fin posterior a inicio, la estancia cabe
   en el rango) y se obtiene el `SearchRequest` de dominio.
4. **`SearchService`** delega en el algoritmo (aquí vivirá, más adelante, la caché de
   búsqueda completa y el guardado en BD).
5. **`SlidingWindowEngine`** recorre todas las duraciones × fechas de salida. Para
   cada par (ida, vuelta) pide el precio a `FlightProvider`.
6. **`CachingFlightProvider`** responde desde caché (HIT) o, si no la tiene (MISS),
   llama a `MockFlightProvider` y guarda el resultado con un TTL según la cercanía
   del viaje.
7. El motor **acumula** todas las ofertas, las **ordena por precio** y devuelve las
   `topN`.
8. Sale como **JSON** al cliente.

### Dónde aparecerá Amadeus

En el paso 6, `MockFlightProvider` se sustituye por `AmadeusFlightProvider`. La caché
del paso 6 ya estará protegiendo cada llamada real → menos consumo de la cuota.
