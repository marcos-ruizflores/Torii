// Diccionario mínimo de aeropuertos: código IATA → coordenadas y nombre.
//
// El backend no devuelve coordenadas (su FlightOffer no las tiene), así que el mapa
// las resuelve aquí a partir del código IATA que el usuario introdujo. Es un punto
// de ampliación natural: el día de mañana, con datos reales, esto se sustituiría por
// una base de datos de aeropuertos o un endpoint del propio backend.
//
// Las coordenadas son [longitud, latitud] (el orden que usa GeoJSON / react-simple-maps).

export interface Airport {
  name: string
  coordinates: [number, number]
}

export const AIRPORTS: Record<string, Airport> = {
  BCN: { name: 'Barcelona', coordinates: [2.0785, 41.2974] },
  MAD: { name: 'Madrid', coordinates: [-3.5668, 40.4983] },
  LIS: { name: 'Lisboa', coordinates: [-9.1359, 38.7742] },
  LHR: { name: 'Londres', coordinates: [-0.4543, 51.47] },
  CDG: { name: 'París', coordinates: [2.5479, 49.0097] },
  AMS: { name: 'Ámsterdam', coordinates: [4.7639, 52.3105] },
  FRA: { name: 'Fráncfort', coordinates: [8.5622, 50.0379] },
  MUC: { name: 'Múnich', coordinates: [11.7861, 48.3538] },
  BER: { name: 'Berlín', coordinates: [13.5033, 52.3667] },
  FCO: { name: 'Roma', coordinates: [12.2389, 41.8003] },
  ZRH: { name: 'Zúrich', coordinates: [8.5492, 47.4647] },
  VIE: { name: 'Viena', coordinates: [16.5697, 48.1103] },
  CPH: { name: 'Copenhague', coordinates: [12.6508, 55.6181] },
  DUB: { name: 'Dublín', coordinates: [-6.2701, 53.4264] },
  IST: { name: 'Estambul', coordinates: [28.7279, 41.2753] },
  SVO: { name: 'Moscú', coordinates: [37.4146, 55.9726] },
  JFK: { name: 'Nueva York', coordinates: [-73.7781, 40.6413] },
  MIA: { name: 'Miami', coordinates: [-80.2906, 25.7959] },
  ORD: { name: 'Chicago', coordinates: [-87.9048, 41.9742] },
  LAX: { name: 'Los Ángeles', coordinates: [-118.4085, 33.9416] },
  SFO: { name: 'San Francisco', coordinates: [-122.379, 37.6213] },
  YYZ: { name: 'Toronto', coordinates: [-79.6306, 43.6777] },
  MEX: { name: 'Ciudad de México', coordinates: [-99.0721, 19.4361] },
  GRU: { name: 'São Paulo', coordinates: [-46.4731, -23.4356] },
  EZE: { name: 'Buenos Aires', coordinates: [-58.5358, -34.8222] },
  NRT: { name: 'Tokio', coordinates: [140.386, 35.7719] },
  HND: { name: 'Tokio Haneda', coordinates: [139.7798, 35.5494] },
  ICN: { name: 'Seúl', coordinates: [126.4505, 37.4602] },
  PEK: { name: 'Pekín', coordinates: [116.5846, 40.0799] },
  HKG: { name: 'Hong Kong', coordinates: [113.9145, 22.308] },
  SIN: { name: 'Singapur', coordinates: [103.9915, 1.3592] },
  BKK: { name: 'Bangkok', coordinates: [100.7501, 13.69] },
  DEL: { name: 'Delhi', coordinates: [77.1003, 28.5562] },
  DXB: { name: 'Dubái', coordinates: [55.3644, 25.2532] },
  CAI: { name: 'El Cairo', coordinates: [31.4056, 30.1219] },
  JNB: { name: 'Johannesburgo', coordinates: [28.246, -26.1392] },
  SYD: { name: 'Sídney', coordinates: [151.1772, -33.9399] },
}

/** Devuelve el aeropuerto si lo conocemos, o undefined. */
export function lookupAirport(iata: string): Airport | undefined {
  return AIRPORTS[iata.toUpperCase()]
}
