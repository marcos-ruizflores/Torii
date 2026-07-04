# Ejemplos de la API de Torii

Peticiones y respuestas reales de `POST /api/search`, para tener a la vista el
contrato JSON mientras no haya interfaz gráfica.

| Archivo | Qué es |
|---|---|
| `request-exhaustive.json` | Petición sin `precision` → usa EXHAUSTIVE (día a día) |
| `response-exhaustive.json`| Su respuesta: mejor precio 325 € (salida 29-jul) |
| `request-fast.json` | Misma búsqueda con `"precision": "FAST"` (salta de 3 en 3 días) |
| `response-fast.json` | Su respuesta: 326 €, pero con **1/3** de las consultas |
| `response-error-validacion.json` | Cómo se ve un error 400 (destino "TOKYO" inválido) |

## Esquema de la PETICIÓN (request)

```jsonc
{
  "origin":       "BCN",          // obligatorio · código IATA, 3 letras
  "destination":  "NRT",          // obligatorio · código IATA, 3 letras
  "rangeStart":   "2026-07-01",   // obligatorio · AAAA-MM-DD · fecha futura
  "rangeEnd":     "2026-09-30",   // obligatorio · AAAA-MM-DD · posterior a rangeStart
  "baseDuration": 14,             // obligatorio · días de estancia · 1..365
  "variability":  3,              // obligatorio · +días a explorar · 0..30 (aquí: 14,15,16,17)
  "maxStops":     1,              // obligatorio · escalas máximas · 0..3
  "topN":         5,              // obligatorio · nº de ofertas a devolver · 1..50
  "precision":    "EXHAUSTIVE"    // OPCIONAL · FAST | BALANCED | EXHAUSTIVE (por defecto EXHAUSTIVE)
}
```

Restricción adicional (validación cruzada): `baseDuration + variability` debe caber
dentro del rango `rangeStart..rangeEnd`.

## Esquema de la RESPUESTA (response)

Un **array** ordenado de menor a mayor precio, con como mucho `topN` elementos:

```jsonc
[
  {
    "airline":     "Vueling",                       // aerolínea
    "price":       325.0,                           // precio (número)
    "currency":    "EUR",                           // moneda
    "stops":       1,                               // nº de escalas
    "departDate":  "2026-07-29",                    // fecha de ida
    "returnDate":  "2026-08-14",                    // fecha de vuelta
    "bookingUrl":  "https://example.com/booking..." // enlace a la reserva
  }
  // ... hasta topN ofertas
]
```

## Esquema del ERROR (HTTP 400)

Formato estándar `ProblemDetail` (RFC 7807):

```jsonc
{
  "type":     "about:blank",
  "title":    "Bad Request",
  "status":   400,
  "detail":   "destination: El destino debe ser un código IATA de 3 letras (ej. NRT)",
  "instance": "/api/search"
}
```

## Probar desde la terminal

```bash
curl -s -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d @examples/request-fast.json | python3 -m json.tool
```
