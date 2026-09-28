// Builds src/data/airports.json from OurAirports (public domain, https://ourairports.com/data/).
//
//   npm run airports
//
// Keeps large and medium airports with scheduled service and an IATA code, which is
// what the flight APIs can actually search. Rows are compact arrays to keep the file
// small: [iata, city, airport name, ISO country, longitude, latitude, isLarge].
import { writeFileSync } from 'node:fs'

const SOURCE = 'https://davidmegginson.github.io/ourairports-data/airports.csv'
const OUT = new URL('../src/data/airports.json', import.meta.url)

// Spanish names for cities whose OurAirports name is the English or local one.
const CITY_ES = {
  London: 'Londres', Paris: 'París', Rome: 'Roma', Milan: 'Milán', Naples: 'Nápoles', Venice: 'Venecia',
  Florence: 'Florencia', Turin: 'Turín', Genoa: 'Génova', Munich: 'Múnich', 'Frankfurt am Main': 'Fráncfort',
  Frankfurt: 'Fráncfort', Cologne: 'Colonia', Berlin: 'Berlín', Hamburg: 'Hamburgo', Vienna: 'Viena',
  Zurich: 'Zúrich', Geneva: 'Ginebra', Brussels: 'Bruselas', Amsterdam: 'Ámsterdam', Copenhagen: 'Copenhague',
  Stockholm: 'Estocolmo', Oslo: 'Oslo', Helsinki: 'Helsinki', Dublin: 'Dublín', Edinburgh: 'Edimburgo',
  Lisbon: 'Lisboa', Porto: 'Oporto', Athens: 'Atenas', Prague: 'Praga', Warsaw: 'Varsovia', Krakow: 'Cracovia',
  Budapest: 'Budapest', Bucharest: 'Bucarest', Istanbul: 'Estambul', Moscow: 'Moscú', 'Saint Petersburg': 'San Petersburgo',
  Marrakesh: 'Marrakech', Cairo: 'El Cairo', 'Cape Town': 'Ciudad del Cabo', Tokyo: 'Tokio', Seoul: 'Seúl',
  Beijing: 'Pekín', Shanghai: 'Shanghái', Singapore: 'Singapur', 'New Delhi': 'Nueva Delhi', Delhi: 'Delhi',
  Dubai: 'Dubái', 'Abu Dhabi': 'Abu Dabi', Doha: 'Doha', Sydney: 'Sídney', 'New York': 'Nueva York',
  'Los Angeles': 'Los Ángeles', 'Mexico City': 'Ciudad de México', 'São Paulo': 'São Paulo', 'Sao Paulo': 'São Paulo',
  'Rio De Janeiro': 'Río de Janeiro', 'Rio de Janeiro': 'Río de Janeiro', Havana: 'La Habana',
  'Buenos Aires': 'Buenos Aires', Bogota: 'Bogotá', Lima: 'Lima', 'Santo Domingo': 'Santo Domingo',
}

// Airports whose municipality is a suburb nobody searches by (Narita, Prat de Llobregat...).
const CITY_BY_IATA = {
  BCN: 'Barcelona', MAD: 'Madrid', NRT: 'Tokio', HND: 'Tokio', LHR: 'Londres', LGW: 'Londres', STN: 'Londres',
  LTN: 'Londres', CDG: 'París', ORY: 'París', BVA: 'París', MXP: 'Milán', LIN: 'Milán', BGY: 'Milán',
  FCO: 'Roma', CIA: 'Roma', JFK: 'Nueva York', EWR: 'Nueva York', LGA: 'Nueva York', ICN: 'Seúl', PEK: 'Pekín',
  PKX: 'Pekín', PVG: 'Shanghái', SHA: 'Shanghái', DXB: 'Dubái', GRU: 'São Paulo', EZE: 'Buenos Aires',
  AGP: 'Málaga', PMI: 'Palma de Mallorca', IBZ: 'Ibiza', TFS: 'Tenerife', TFN: 'Tenerife', LPA: 'Gran Canaria',
  ACE: 'Lanzarote', FUE: 'Fuerteventura', SCQ: 'Santiago de Compostela', BIO: 'Bilbao', SVQ: 'Sevilla',
  VLC: 'Valencia', ALC: 'Alicante', GRO: 'Girona', REU: 'Reus', MAH: 'Menorca', OPO: 'Oporto',
  CPH: 'Copenhague', ARN: 'Estocolmo', OSL: 'Oslo', AMS: 'Ámsterdam', BRU: 'Bruselas', CRL: 'Bruselas',
  SYD: 'Sídney', KIX: 'Osaka', ITM: 'Osaka',
}

function parseCsv(text) {
  const rows = []
  let row = [], field = '', quoted = false
  for (let i = 0; i < text.length; i++) {
    const c = text[i]
    if (quoted) {
      if (c === '"' && text[i + 1] === '"') { field += '"'; i++ }
      else if (c === '"') quoted = false
      else field += c
    } else if (c === '"') quoted = true
    else if (c === ',') { row.push(field); field = '' }
    else if (c === '\n') { row.push(field); rows.push(row); row = []; field = '' }
    else if (c !== '\r') field += c
  }
  if (field || row.length) { row.push(field); rows.push(row) }
  return rows
}

const text = await (await fetch(SOURCE)).text()
const [header, ...data] = parseCsv(text)
const col = Object.fromEntries(header.map((h, i) => [h, i]))
const round = (n) => Math.round(Number(n) * 10000) / 10000

const airports = data
  .filter((r) => ['large_airport', 'medium_airport'].includes(r[col.type]))
  .filter((r) => r[col.scheduled_service] === 'yes' && /^[A-Z]{3}$/.test(r[col.iata_code]))
  .map((r) => {
    const iata = r[col.iata_code]
    const municipality = r[col.municipality] || r[col.name]
    const city = CITY_BY_IATA[iata] ?? CITY_ES[municipality] ?? municipality
    return [iata, city, r[col.name], r[col.iso_country], round(r[col.longitude_deg]), round(r[col.latitude_deg]),
      r[col.type] === 'large_airport' ? 1 : 0]
  })
  .sort((a, b) => a[0].localeCompare(b[0]))

// One row per code: a couple of IATA codes appear twice in the source.
const unique = [...new Map(airports.map((a) => [a[0], a])).values()]
writeFileSync(OUT, JSON.stringify(unique))
console.log(`${unique.length} airports -> ${OUT.pathname}`)
