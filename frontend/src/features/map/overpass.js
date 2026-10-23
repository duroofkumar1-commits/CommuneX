// Real-world places and buildings from OpenStreetMap via the same-origin backend proxy.

import api from "../../services/api";

export const CATEGORIES = {
  hospital: { label: "Hospitals", emoji: "🏥", color: "#e53935", minZoom: 11, filters: ['nwr["amenity"="hospital"]'] },
  clinic: { label: "Clinics & doctors", emoji: "🩺", color: "#ef6c00", minZoom: 13, filters: ['nwr["amenity"~"^(clinic|doctors)$"]'] },
  pharmacy: { label: "Medical stores", emoji: "💊", color: "#2e9e4f", minZoom: 12, filters: ['nwr["amenity"="pharmacy"]', 'nwr["shop"="chemist"]'] },
  mall: { label: "Malls", emoji: "🛍️", color: "#d81b60", minZoom: 12, filters: ['nwr["shop"="mall"]'] },
  shop: { label: "Shops & markets", emoji: "🏪", color: "#8e24aa", minZoom: 14, filters: ['nwr["shop"]', 'nwr["amenity"="marketplace"]'] },
  worship: { label: "Mosques & worship", emoji: "🕌", color: "#00897b", minZoom: 13, filters: ['nwr["amenity"="place_of_worship"]'] },
  police: { label: "Police", emoji: "👮", color: "#1e88e5", minZoom: 12, filters: ['nwr["amenity"="police"]'] },
  fire: { label: "Fire stations", emoji: "🚒", color: "#f4511e", minZoom: 11, filters: ['nwr["amenity"="fire_station"]'] },
  school: { label: "Schools & colleges", emoji: "🎓", color: "#c6a700", minZoom: 13, filters: ['nwr["amenity"~"^(school|college|university)$"]'] },
  bank: { label: "Banks & ATMs", emoji: "🏦", color: "#5e35b1", minZoom: 14, filters: ['nwr["amenity"~"^(bank|atm)$"]'] },
  fuel: { label: "Petrol pumps", emoji: "⛽", color: "#6d4c41", minZoom: 12, filters: ['nwr["amenity"="fuel"]'] },
  food: { label: "Restaurants & cafés", emoji: "🍽️", color: "#fb8c00", minZoom: 14, filters: ['nwr["amenity"~"^(restaurant|fast_food|cafe)$"]'] },
};

export const CATEGORY_KEYS = Object.keys(CATEGORIES);

/** Decide which of our categories an OSM element belongs to (first match wins). */
export function classify(tags = {}) {
  const a = tags.amenity;
  if (a === "hospital" || tags.healthcare === "hospital") return "hospital";
  if (a === "clinic" || a === "doctors") return "clinic";
  if (a === "pharmacy" || tags.shop === "chemist") return "pharmacy";
  if (tags.shop === "mall") return "mall";
  if (a === "place_of_worship") return "worship";
  if (a === "police") return "police";
  if (a === "fire_station") return "fire";
  if (a === "school" || a === "college" || a === "university") return "school";
  if (a === "bank" || a === "atm") return "bank";
  if (a === "fuel") return "fuel";
  if (a === "restaurant" || a === "fast_food" || a === "cafe") return "food";
  if (tags.shop || a === "marketplace") return "shop";
  return null;
}

export function buildPoiQuery(bbox, categoryKeys, limit = 2000) {
  const box = `${bbox.south.toFixed(5)},${bbox.west.toFixed(5)},${bbox.north.toFixed(5)},${bbox.east.toFixed(5)}`;
  const lines = categoryKeys.flatMap((k) => CATEGORIES[k].filters.map((f) => `${f}(${box});`));
  return `[out:json][timeout:25];(${lines.join("")});out tags center ${limit};`;
}

export function parsePois(json, allowed) {
  const out = [];
  for (const el of json?.elements || []) {
    const lat = el.lat ?? el.center?.lat;
    const lng = el.lon ?? el.center?.lon;
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) continue;
    const tags = el.tags || {};
    const cat = classify(tags);
    if (!cat || (allowed && !allowed.includes(cat))) continue;
    out.push({
      id: `${el.type}/${el.id}`,
      lat,
      lng,
      cat,
      name: tags.name || tags["name:en"] || tags["name:ur"] || CATEGORIES[cat].label.replace(/s$/, ""),
      tags,
    });
  }
  return out;
}

export function buildBuildingQuery(bbox, limit = 1500) {
  const box = `${bbox.south.toFixed(5)},${bbox.west.toFixed(5)},${bbox.north.toFixed(5)},${bbox.east.toFixed(5)}`;
  return `[out:json][timeout:25];way["building"](${box});out geom ${limit};`;
}

export function buildingHeight(tags = {}) {
  const h = parseFloat(String(tags.height || "").replace(",", "."));
  if (Number.isFinite(h) && h > 0) return Math.min(h, 400);
  const lv = parseFloat(tags["building:levels"]);
  if (Number.isFinite(lv) && lv > 0) return Math.min(lv * 3.2, 400);
  return 6;
}

export function parseBuildings(json) {
  const features = [];
  for (const el of json?.elements || []) {
    if (el.type !== "way" || !Array.isArray(el.geometry) || el.geometry.length < 4) continue;
    const ring = el.geometry.map((p) => [p.lon, p.lat]);
    const first = ring[0];
    const last = ring[ring.length - 1];
    if (first[0] !== last[0] || first[1] !== last[1]) ring.push([first[0], first[1]]);
    features.push({
      type: "Feature",
      id: el.id,
      properties: { h: buildingHeight(el.tags) },
      geometry: { type: "Polygon", coordinates: [ring] },
    });
  }
  return features;
}

export async function runOverpass(request, signal) {
  // The backend validates the requested layer, bounding box and POI categories,
  // then talks to Overpass server-to-server. This avoids browser CORS failures.
  const response = await api.post("/map/overpass", request, {
    signal,
    timeout: 50000,
  });
  return response.data;
}

/** Short human labels for an OSM tag set, used in the details panel. */
export function describePoi(poi) {
  const t = poi.tags || {};
  const lines = [];
  const addr = [t["addr:housenumber"], t["addr:street"], t["addr:suburb"] || t["addr:neighbourhood"], t["addr:city"]].filter(Boolean).join(", ");
  if (addr) lines.push(["Address", addr]);
  const phone = t.phone || t["contact:phone"] || t["contact:mobile"];
  if (phone) lines.push(["Phone", phone]);
  if (t.opening_hours) lines.push(["Hours", t.opening_hours]);
  if (t.emergency === "yes") lines.push(["Emergency", "Emergency service available"]);
  if (t.operator) lines.push(["Operator", t.operator]);
  if (poi.cat === "worship") {
    const faith = [t.religion, t.denomination].filter(Boolean).join(" · ");
    if (faith) lines.push(["Faith", faith]);
  }
  const site = t.website || t["contact:website"];
  if (site && /^https?:\/\//i.test(site)) lines.push(["Website", site]);
  return lines;
}
