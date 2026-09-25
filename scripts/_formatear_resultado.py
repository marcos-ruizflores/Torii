#!/usr/bin/env python3
"""
Formats the POST /api/search response as a readable table.

Used by buscar-oferta.sh: gets the HTTP status as an argument and the response body
on stdin. It lives in its own file instead of inline in the bash script to avoid
escaping headaches and keep it easy to edit.
"""
import json
import sys
from datetime import date

http_code = sys.argv[1] if len(sys.argv) > 1 else "?"
raw = sys.stdin.read()

try:
    data = json.loads(raw)
except json.JSONDecodeError:
    print(f"Respuesta no-JSON (HTTP {http_code}):\n{raw}")
    sys.exit(0)

# The API returns an object (ProblemDetail) on validation errors.
if isinstance(data, dict):
    detalle = data.get("detail", data)
    print(f"❌  Error HTTP {http_code}: {detalle}")
    sys.exit(0)

if not data:
    print("No se han encontrado ofertas para esos criterios.")
    sys.exit(0)

print(f"✅  {len(data)} mejores ofertas (HTTP {http_code}):\n")

cabecera = "  {:<2} {:>11}  {:<12} {:<4} {:<12} {:<12} {:<4}".format(
    "#", "Precio", "Aerolinea", "Esc.", "Ida", "Vuelta", "Dias")
print(cabecera)
print("  " + "-" * 66)

for i, o in enumerate(data, 1):
    ida = date.fromisoformat(o["departDate"])
    vuelta = date.fromisoformat(o["returnDate"])
    dias = (vuelta - ida).days
    precio = "{:.2f} {}".format(float(o["price"]), o["currency"])
    fila = "  {:<2} {:>11}  {:<12} {:<4} {:<12} {:<12} {:<4}".format(
        i, precio, o["airline"], o["stops"], o["departDate"], o["returnDate"], dias)
    print(fila)

print("\nEnlaces de reserva:")
for i, o in enumerate(data, 1):
    print(f"  {i}. {o['bookingUrl']}")
