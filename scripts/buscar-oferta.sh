#!/usr/bin/env bash
#
# Torii - interactive offer search for manual testing.
#
# Asks for the search parameters, calls the API (POST /api/search) and prints the
# offers as a table. Press Enter on any question to accept the default shown in
# brackets.
#
# Usage:
#   ./scripts/buscar-oferta.sh
#
# Optional env vars:
#   TORII_URL=http://localhost:8080   (if you changed the port/host)

set -euo pipefail

API="${TORII_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# --- 1) Check the app is running -------------------------------------------------
if ! curl -s -o /dev/null --max-time 2 "$API/api/cache/stats"; then
  echo "⚠️  Torii no responde en $API"
  echo "    Arráncalo en otra terminal con:  ./mvnw spring-boot:run"
  exit 1
fi

# --- 2) Small helper to ask with a default value ---------------------------------
# read -rp writes the prompt to stderr, so $(...) only captures the answer.
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

# --- 3) Build the JSON request body ----------------------------------------------
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

# --- 4) Call the API and split body from HTTP status -----------------------------
response=$(curl -s -w $'\n%{http_code}' -X POST "$API/api/search" \
  -H "Content-Type: application/json" \
  -d "$payload")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

# --- 5) Print the result in a readable way ---------------------------------------
echo "$body" | python3 "$SCRIPT_DIR/_formatear_resultado.py" "$http_code"
