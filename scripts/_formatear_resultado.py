#!/usr/bin/env python3
"""
Formatea la respuesta de POST /api/search en una tabla legible.

Lo usa buscar-oferta.sh: recibe el código HTTP como argumento y el cuerpo de la
respuesta por stdin. Está en un archivo aparte (en vez de incrustado en el bash)
para evitar problemas de escape y poder leerlo/editarlo con comodidad.
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

# La API devuelve un objeto (ProblemDetail) cuando hay error de validación.
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
