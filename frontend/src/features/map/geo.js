// Small geo helpers: distance, formatting, tile maths, geocoding (OpenStreetMap Nominatim).

const R = 6371;
const rad = (d) => (d * Math.PI) / 180;

export function haversineKm(a, b) {
  const dLat = rad(b.lat - a.lat);
  const dLng = rad(b.lng - a.lng);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(h)));
}

export function fmtDistance(km) {
  if (!Number.isFinite(km)) return "—";
  if (km < 1) return `${Math.round(km * 1000)} m`;
  return `${km < 10 ? km.toFixed(1) : Math.round(km)} km`;
}

export function fmtDuration(minutes) {
  if (!Number.isFinite(minutes)) return "—";
  const m = Math.round(minutes);
  if (m < 1) return "< 1 min";
  if (m < 60) return `${m} min`;
  const h = Math.floor(m / 60);
  const rest = m % 60;
  if (h >= 24) return `${Math.floor(h / 24)} d ${h % 24} h`;
  return rest ? `${h} h ${rest} min` : `${h} h`;
}

export function lngToTile(lng, z) {
  return Math.floor(((lng + 180) / 360) * 2 ** z);
}

export function latToTile(lat, z) {
  const l = Math.max(-85.0511, Math.min(85.0511, lat));
  const r = rad(l);
  return Math.floor(((1 - Math.log(Math.tan(r) + 1 / Math.cos(r)) / Math.PI) / 2) * 2 ** z);
}

export function tileBounds(x, y, z) {
  const n = 2 ** z;
  const west = (x / n) * 360 - 180;
  const east = ((x + 1) / n) * 360 - 180;
  const north = (Math.atan(Math.sinh(Math.PI * (1 - (2 * y) / n))) * 180) / Math.PI;
  const south = (Math.atan(Math.sinh(Math.PI * (1 - (2 * (y + 1)) / n))) * 180) / Math.PI;
  return { west, east, north, south };
}

/** All tiles (at zoom z) that intersect a {west,south,east,north} box. */
export function tilesForBounds(b, z) {
  const x0 = lngToTile(b.west, z);
  const x1 = lngToTile(b.east, z);
  const y0 = latToTile(b.north, z);
  const y1 = latToTile(b.south, z);
  const tiles = [];
  for (let x = x0; x <= x1; x += 1) for (let y = y0; y <= y1; y += 1) tiles.push({ x, y, z });
  return tiles;
}

export function unionBounds(tiles) {
  let west = Infinity, south = Infinity, east = -Infinity, north = -Infinity;
  for (const t of tiles) {
    const b = tileBounds(t.x, t.y, t.z);
    west = Math.min(west, b.west);
    east = Math.max(east, b.east);
    south = Math.min(south, b.south);
    north = Math.max(north, b.north);
  }
  return { west, south, east, north };
}

// ---------- Geocoding (text address -> coordinates), cached in localStorage ----------

const CACHE_KEY = "communex.geocache.v1";
let memoryCache = null;

function readCache() {
  if (memoryCache) return memoryCache;
  try {
    memoryCache = JSON.parse(localStorage.getItem(CACHE_KEY) || "{}") || {};
  } catch {
    memoryCache = {};
  }
  return memoryCache;
}

function writeCache() {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify(memoryCache));
  } catch {
    /* storage full or unavailable: ignore */
  }
}

export const geoKey = (text) => String(text || "").trim().toLowerCase().replace(/\s+/g, " ");

export function isGeocoded(text) {
  return geoKey(text) in readCache();
}

export function cachedGeocode(text) {
  const hit = readCache()[geoKey(text)];
  return hit && Number.isFinite(hit.lat) ? hit : null;
}

let lastCall = 0;

/** Nominatim allows ~1 request/second, so calls are spaced out. Returns {lat,lng} or null. */
export async function geocodeText(text, signal) {
  const key = geoKey(text);
  if (!key) return null;
  const cache = readCache();
  if (key in cache) return cache[key] && Number.isFinite(cache[key].lat) ? cache[key] : null;
  const wait = Math.max(0, lastCall + 1100 - Date.now());
  if (wait) await new Promise((r) => setTimeout(r, wait));
  lastCall = Date.now();
  const url = `https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes=pk&q=${encodeURIComponent(text)}`;
  const res = await fetch(url, { signal, headers: { Accept: "application/json" } });
  if (!res.ok) throw new Error("Geocoder unavailable");
  const data = await res.json();
  const first = Array.isArray(data) ? data[0] : null;
  const value = first ? { lat: Number(first.lat), lng: Number(first.lon) } : null;
  cache[key] = value; // null is cached too, so a failed lookup is not repeated every refresh
  writeCache();
  return value;
}

/** Place search for the search box (Pakistan only). */
export async function searchPlaces(query, signal) {
  const q = String(query || "").trim();
  if (q.length < 2) return [];
  const url = `https://nominatim.openstreetmap.org/search?format=jsonv2&limit=6&countrycodes=pk&addressdetails=0&q=${encodeURIComponent(q)}`;
  const res = await fetch(url, { signal, headers: { Accept: "application/json" } });
  if (!res.ok) throw new Error("Search is unavailable right now");
  const data = await res.json();
  return (Array.isArray(data) ? data : [])
    .map((p) => ({
      id: `${p.osm_type}${p.osm_id}`,
      name: String(p.display_name || "").split(",").slice(0, 2).join(",").trim(),
      full: String(p.display_name || ""),
      lat: Number(p.lat),
      lng: Number(p.lon),
      bbox: Array.isArray(p.boundingbox) ? p.boundingbox.map(Number) : null, // [south, north, west, east]
    }))
    .filter((p) => Number.isFinite(p.lat) && Number.isFinite(p.lng));
}
