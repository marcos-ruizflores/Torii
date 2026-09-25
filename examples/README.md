# Torii API examples

Real requests and responses for `POST /api/search`, handy for keeping the JSON contract
in view.

| File | What it is |
|---|---|
| `request-exhaustive.json` | Request without `precision`, so it uses EXHAUSTIVE (every day) |
| `response-exhaustive.json`| Its response: best price 325 EUR (departing Jul 29) |
| `request-fast.json` | Same search with `"precision": "FAST"` (steps 3 days at a time) |
| `response-fast.json` | Its response: 326 EUR, with **1/3** of the lookups |
| `response-error-validacion.json` | What a 400 looks like (invalid destination "TOKYO") |

## Request schema

```jsonc
{
  "origin":       "BCN",          // required · IATA code, 3 letters
  "destination":  "NRT",          // required · IATA code, 3 letters
  "rangeStart":   "2026-07-01",   // required · YYYY-MM-DD · in the future
  "rangeEnd":     "2026-09-30",   // required · YYYY-MM-DD · after rangeStart
  "baseDuration": 14,             // required · days of stay · 1..365
  "variability":  3,              // required · extra days to explore · 0..30 (here: 14,15,16,17)
  "maxStops":     1,              // required · max stops · 0..3
  "topN":         5,              // required · number of offers to return · 1..50
  "precision":    "EXHAUSTIVE",   // optional · FAST | BALANCED | EXHAUSTIVE (defaults to EXHAUSTIVE)
  "maxPrice":     900             // optional · budget, offers above it are dropped
}
```

Extra cross-field rule: `baseDuration + variability` has to fit inside
`rangeStart..rangeEnd`.

## Response schema

An **array** sorted from cheapest to most expensive, with at most `topN` items:

```jsonc
[
  {
    "airline":             "Vueling",                        // airline
    "price":               325.0,                            // round-trip total
    "currency":            "EUR",
    "stops":               1,                                // number of stops
    "departDate":          "2026-07-29",                     // outbound date
    "returnDate":          "2026-08-14",                     // return date
    "departureTime":       "10:30:00",                       // null if the source doesn't provide it
    "returnDepartureTime": "18:00:00",                       // null with SerpApi
    "stopovers":           ["CDG"],                          // stopover IATA codes
    "bookingUrl":          "https://example.com/booking..."  // booking link
  }
  // ... up to topN offers
]
```

## Error schema (HTTP 400)

Standard `ProblemDetail` (RFC 7807). Validation messages are in Spanish because that's
the language of the UI:

```jsonc
{
  "type":     "about:blank",
  "title":    "Bad Request",
  "status":   400,
  "detail":   "destination: El destino debe ser un código IATA de 3 letras (ej. NRT)",
  "instance": "/api/search"
}
```

## Trying it from the terminal

```bash
curl -s -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d @examples/request-fast.json | python3 -m json.tool
```

Or use the interactive script: `./scripts/buscar-oferta.sh`.
