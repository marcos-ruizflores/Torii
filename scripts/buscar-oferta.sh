#!/usr/bin/env bash
#
# Torii — buscador de ofertas en modo TEST (interactivo).
#
# Te pregunta los datos de la búsqueda igual que haría la futura interfaz gráfica,
# llama a la API (POST /api/search) y muestra las ofertas en una tabla. Pulsa Enter
# en cualquier pregunta para aceptar el valor por defecto que aparece entre corchetes.
#
# Uso:
#   ./scripts/buscar-oferta.sh
#
# Variables opcionales:
#   TORII_URL=http://localhost:8080   (por si cambias el puerto/host)

set -euo pipefail

API="${TORII_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# --- 1) Comprobar que la aplicación está levantada -------------------------------
if ! curl -s -o /dev/null --max-time 2 "$API/api/cache/stats"; then
  echo "⚠️  Torii no responde en $API"
  echo "    Arráncalo en otra terminal con:  ./mvnw spring-boot:run"
  exit 1
fi

# --- 2) Pequeña ayuda para preguntar con valor por defecto -----------------------
# El prompt (read -rp) se escribe en stderr, así que $(...) solo captura la respuesta.
ask() {
  local prompt="$1" default="$2" answer
  read -rp "$prompt [$default]: " answer
  echo "${answer:-$default}"
}

echo "🪶  Torii — búsqueda de ofertas (modo test)"
echo "    (pulsa Enter para aceptar el valor entre corchetes)"
echo

origin=$(ask      "Origen (código IATA, ej. BCN)" "BCN")
destination=$(ask "Destino (código IATA, ej. NRT)" "NRT")
rangeStart=$(ask  "Inicio del rango de vacaciones (AAAA-MM-DD)" "2026-07-01")
rangeEnd=$(ask    "Fin del rango de vacaciones    (AAAA-MM-DD)" "2026-09-30")
baseDuration=$(ask "Duración de la estancia (días)" "14")
variability=$(ask "Variabilidad (+días a explorar)" "3")
maxStops=$(ask    "Máximo de escalas (0-3)" "1")
topN=$(ask        "¿Cuántas mejores ofertas mostrar?" "5")

# --- 3) Construir el cuerpo JSON de la petición ----------------------------------
payload=$(cat <<JSON
{
  "origin": "$origin",
  "destination": "$destination",
  "rangeStart": "$rangeStart",
  "rangeEnd": "$rangeEnd",
  "baseDuration": $baseDuration,
  "variability": $variability,
  "maxStops": $maxStops,
  "topN": $topN
}
JSON
)

echo
echo "→ Buscando $origin → $destination, estancia $baseDuration-$((baseDuration+variability)) días entre $rangeStart y $rangeEnd ..."
echo

# --- 4) Llamar a la API y separar cuerpo de código HTTP --------------------------
response=$(curl -s -w $'\n%{http_code}' -X POST "$API/api/search" \
  -H "Content-Type: application/json" \
  -d "$payload")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

# --- 5) Formatear el resultado de forma legible ----------------------------------
echo "$body" | python3 "$SCRIPT_DIR/_formatear_resultado.py" "$http_code"
