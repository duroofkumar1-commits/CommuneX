import api from "../../services/api";
import { haversineKm } from "./geo";

export const MODES = [
  {
    key: "car",
    label: "Car",
    icon: "🚗",
    color: "#2f80ed",
    google: "driving",
    speedKmh: 30,
  },
  {
    key: "bike",
    label: "Motorbike",
    icon: "🏍️",
    color: "#f2994a",
    google: "two-wheeler",
    speedKmh: 35,
  },
  {
    key: "foot",
    label: "Walking",
    icon: "🚶",
    color: "#27ae60",
    google: "walking",
    speedKmh: 5,
  },
];

const OSRM_REQUEST_GAP_MS = 1100; // Public OSM routing servers ask clients to stay at or below 1 req/sec.
const OSRM_REQUEST_TIMEOUT_MS = 12000;
const OSRM_ROUTE_URLS = {
  car: "https://routing.openstreetmap.de/routed-car/route/v1/driving",
  bike: "https://routing.openstreetmap.de/routed-bike/route/v1/driving",
  foot: "https://routing.openstreetmap.de/routed-foot/route/v1/driving",
};
const VALHALLA_ROUTE_URL = "https://valhalla1.openstreetmap.de/route";
const VALHALLA_REQUEST_TIMEOUT_MS = 12000;
const VALHALLA_COSTING = {
  car: "auto",
  bike: "motorcycle",
  foot: "pedestrian",
};

// A module-level start-time gate also spaces calls if the user changes routes quickly.
let lastOsrmRequestAt = 0;
let osrmRequestQueue = Promise.resolve();

async function waitForOsrmRequestSlot(signal) {
  const previous = osrmRequestQueue;
  let release;
  osrmRequestQueue = new Promise((resolve) => { release = resolve; });
  await previous.catch(() => {});

  try {
    const delay = Math.max(0, OSRM_REQUEST_GAP_MS - (Date.now() - lastOsrmRequestAt));
    if (delay > 0) await wait(delay, signal);
    if (signal?.aborted) throw abortError();
    lastOsrmRequestAt = Date.now();
  } finally {
    release();
  }
}

/**
 * Explicitly calculate a rough straight-line estimate for places where a road
 * route is not required. This is intentionally NOT used as a route fallback:
 * a straight line must never be presented as a road-following route.
 */
export function straightLineRoute(from, to, speedKmh, reason) {
  const km = haversineKm(from, to) * 1.35;

  return {
    distanceKm: km,
    minutes: (km / speedKmh) * 60,
    geometry: {
      type: "LineString",
      coordinates: [
        [from.lng, from.lat],
        [to.lng, to.lat],
      ],
    },
    approximate: true,
    fallbackReason: reason || "Straight-line estimate only; no road route was calculated",
    source: "Straight-line estimate",
  };
}

function isAbortError(error, signal) {
  return Boolean(
    signal?.aborted ||
      error?.name === "AbortError" ||
      error?.name === "CanceledError" ||
      error?.code === "ERR_CANCELED"
  );
}

function abortError() {
  return new DOMException("Route request was cancelled", "AbortError");
}

function normalizeRoute(route, source) {
  const coordinates = route?.geometry?.coordinates;
  const validGeometry =
    route?.geometry?.type === "LineString" &&
    Array.isArray(coordinates) &&
    coordinates.length >= 2 &&
    coordinates.every(
      (point) =>
        Array.isArray(point) &&
        point.length >= 2 &&
        Number.isFinite(point[0]) &&
        Number.isFinite(point[1]) &&
        Math.abs(point[0]) <= 180 &&
        Math.abs(point[1]) <= 90
    );

  if (
    !route ||
    !Number.isFinite(route.distanceKm) ||
    route.distanceKm <= 0 ||
    !Number.isFinite(route.minutes) ||
    route.minutes < 0 ||
    !validGeometry
  ) {
    // Throw so fetchAllRoutes can try a real road-routing service instead of
    // silently replacing invalid route geometry with a direct line.
    throw new Error("The backend returned invalid road-route data.");
  }

  // GeoJSON coordinates are [longitude, latitude]. Keep this order for MapLibre.
  return {
    distanceKm: route.distanceKm,
    minutes: route.minutes,
    geometry: {
      type: "LineString",
      coordinates,
    },
    approximate: route.approximate === true,
    source: route.source || source || "CommuneX Dijkstra road graph",
  };
}

function wait(ms, signal) {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(abortError());
      return;
    }

    const finish = () => {
      signal?.removeEventListener("abort", cancel);
      resolve();
    };

    const timer = setTimeout(finish, ms);
    const cancel = () => {
      clearTimeout(timer);
      signal?.removeEventListener("abort", cancel);
      reject(abortError());
    };

    signal?.addEventListener("abort", cancel, { once: true });
  });
}

async function fetchOsrmRoute(from, to, mode, signal) {
  await waitForOsrmRequestSlot(signal);
  const baseUrl = OSRM_ROUTE_URLS[mode.key];
  const coordinates = `${from.lng},${from.lat};${to.lng},${to.lat}`;
  const url = `${baseUrl}/${coordinates}?overview=full&geometries=geojson&steps=false`;
  const timeoutSignal = AbortSignal.timeout(OSRM_REQUEST_TIMEOUT_MS);
  const requestSignal = signal
    ? AbortSignal.any([signal, timeoutSignal])
    : timeoutSignal;

  let response;
  try {
    response = await fetch(url, {
      method: "GET",
      headers: { Accept: "application/json" },
      signal: requestSignal,
    });
  } catch (error) {
    if (signal?.aborted) throw abortError();
    if (timeoutSignal.aborted) {
      throw new Error(`OpenStreetMap ${mode.label} routing timed out.`, { cause: error });
    }
    throw error;
  }

  const rawBody = await response.text();
  let data = null;
  try {
    data = rawBody ? JSON.parse(rawBody) : null;
  } catch {
    data = { message: rawBody.trim().slice(0, 240) };
  }

  if (!response.ok) {
    throw new Error(
      `OSRM ${mode.label} routing failed: ${data?.message || data?.error || `HTTP ${response.status}`}`
    );
  }

  if (data?.code !== "Ok") {
    throw new Error(
      `OSRM ${mode.label} routing failed: ${data?.message || data?.code || "No route returned"}`
    );
  }

  const route = data?.routes?.[0];
  const coordinatesOut = route?.geometry?.coordinates;
  const validGeometry =
    route?.geometry?.type === "LineString" &&
    Array.isArray(coordinatesOut) &&
    coordinatesOut.length >= 2 &&
    coordinatesOut.every(
      (point) =>
        Array.isArray(point) &&
        point.length >= 2 &&
        Number.isFinite(point[0]) &&
        Number.isFinite(point[1]) &&
        Math.abs(point[0]) <= 180 &&
        Math.abs(point[1]) <= 90
    );

  if (
    !route ||
    !Number.isFinite(route.distance) ||
    route.distance <= 0 ||
    !Number.isFinite(route.duration) ||
    route.duration < 0 ||
    !validGeometry
  ) {
    throw new Error(`OSRM ${mode.label} routing returned invalid road-route data.`);
  }

  return {
    distanceKm: route.distance / 1000,
    minutes: route.duration / 60,
    geometry: { type: "LineString", coordinates: coordinatesOut },
    approximate: false,
    source: `OpenStreetMap OSRM ${mode.label.toLowerCase()} road-routing fallback`,
  };
}

async function fetchValhallaRoute(from, to, mode, signal) {
  const costing = VALHALLA_COSTING[mode.key];
  const payload = {
    locations: [
      { lat: from.lat, lon: from.lng, type: "break" },
      { lat: to.lat, lon: to.lng, type: "break" },
    ],
    costing,
    costing_options: {
      [costing]: { shortest: true },
    },
    units: "km",
    format: "osrm",
    shape_format: "geojson",
    directions_type: "none",
  };

  const timeoutSignal = AbortSignal.timeout(VALHALLA_REQUEST_TIMEOUT_MS);
  const requestSignal = signal
    ? AbortSignal.any([signal, timeoutSignal])
    : timeoutSignal;
  let response;

  try {
    response = await fetch(VALHALLA_ROUTE_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        // The public OSM demo server asks applications to identify themselves.
        "X-Client-Id": "communex-community-map",
      },
      body: JSON.stringify(payload),
      signal: requestSignal,
    });
  } catch (error) {
    if (signal?.aborted) throw abortError();
    if (timeoutSignal.aborted) {
      throw new Error("The backup road-routing service timed out.", { cause: error });
    }
    throw error;
  }

  const rawBody = await response.text();
  let data = null;
  try {
    data = rawBody ? JSON.parse(rawBody) : null;
  } catch {
    data = { message: rawBody.trim().slice(0, 240) };
  }

  if (!response.ok) {
    const detail =
      data?.error || data?.status_message || data?.message || data?.error_description ||
      `HTTP ${response.status}`;
    throw new Error(`Valhalla road-routing backup failed: ${detail}`);
  }

  const route = data?.routes?.[0];
  const coordinates = route?.geometry?.coordinates;
  const validGeometry =
    route?.geometry?.type === "LineString" &&
    Array.isArray(coordinates) &&
    coordinates.length >= 2 &&
    coordinates.every(
      (point) =>
        Array.isArray(point) &&
        point.length >= 2 &&
        Number.isFinite(point[0]) &&
        Number.isFinite(point[1]) &&
        Math.abs(point[0]) <= 180 &&
        Math.abs(point[1]) <= 90
    );

  // Valhalla's OSRM-compatible response uses metres and seconds.
  if (
    !route ||
    !Number.isFinite(route.distance) ||
    route.distance <= 0 ||
    !Number.isFinite(route.duration) ||
    route.duration < 0 ||
    !validGeometry
  ) {
    const detail = data?.message || data?.status_message || "No connected road route was returned";
    throw new Error(detail);
  }

  return {
    distanceKm: route.distance / 1000,
    minutes: route.duration / 60,
    geometry: {
      type: "LineString",
      coordinates,
    },
    approximate: false,
    source: "Valhalla OpenStreetMap road-routing fallback",
  };
}

async function fetchRoadRoutingFallback(from, to, signal) {
  const results = {};
  const failures = [];

  // Try the worldwide OSM car/bike/foot routers first. Keep requests spaced
  // to respect the public demo service's usage policy; Valhalla is the next
  // fallback for a mode when OSRM cannot return a valid route.
  for (let index = 0; index < MODES.length; index += 1) {
    if (signal?.aborted) throw abortError();

    const mode = MODES[index];
    let osrmError;
    try {
      results[mode.key] = await fetchOsrmRoute(from, to, mode, signal);
      continue;
    } catch (error) {
      if (isAbortError(error, signal)) throw abortError();
      osrmError = error;
      console.warn(`CommuneX OSRM ${mode.label} fallback failed; trying Valhalla:`, error);
    }

    try {
      results[mode.key] = await fetchValhallaRoute(from, to, mode, signal);
    } catch (valhallaError) {
      if (isAbortError(valhallaError, signal)) throw abortError();
      failures.push(
        `${mode.label}: OSRM: ${explainError(osrmError)}; Valhalla: ${explainError(valhallaError)}`
      );
      // Preserve the actual errors in the final message instead of silently
      // drawing a straight line or claiming a route exists.
      throw new Error(
        `${mode.label} road routing failed. ${failures[failures.length - 1]}`,
        { cause: valhallaError }
      );
    }
  }

  const sources = [...new Set(Object.values(results).map((route) => route.source))];
  console.info("CommuneX used road-routing fallback:", sources.join("; "));
  return results;
}

function explainError(error) {
  const candidate =
    error?.response?.data?.message ||
    error?.response?.data?.detail ||
    error?.message;
  return typeof candidate === "string" && candidate.trim()
    ? candidate.trim().slice(0, 220)
    : "the local road-routing service was unavailable";
}

export async function fetchAllRoutes(from, to, signal) {
  let backendError;

  try {
    const response = await api.get("/map/route", {
      params: {
        fromLat: from.lat,
        fromLng: from.lng,
        toLat: to.lat,
        toLng: to.lng,
      },
      signal,
      timeout: 40000, // Switch to a road-routing fallback if Overpass/Dijkstra hangs.
    });

    const data = response?.data;

    if (!data?.car || !data?.bike || !data?.foot) {
      throw new Error("Backend response must contain car, bike and foot routes");
    }

    const car = normalizeRoute(data.car, data.engine);
    const bike = normalizeRoute(data.bike, data.engine);
    const foot = normalizeRoute(data.foot, data.engine);

    console.info("CommuneX routing response:", {
      engine: data.engine,
      car,
      bike,
      foot,
    });

    return { car, bike, foot };
  } catch (error) {
    if (isAbortError(error, signal)) throw abortError();

    backendError = error;
    console.error(
      "CommuneX road routing failed; trying the road-routing backup:",
      error?.response?.status,
      error?.response?.data || error?.message
    );
  }

  try {
    return await fetchRoadRoutingFallback(from, to, signal);
  } catch (fallbackError) {
    if (isAbortError(fallbackError, signal)) throw abortError();

    console.error("CommuneX road-routing fallback failed:", fallbackError);
    throw new Error(
      `Could not calculate a road-following route. CommuneX Dijkstra router: ${explainError(backendError)}. External road routers: ${explainError(fallbackError)}. Please retry or open Google Maps.`,
      { cause: fallbackError }
    );
  }
}

export function googleMapsDirectionsUrl(from, to, mode) {
  const selected = MODES.find((m) => m.key === mode);
  const travelMode = selected?.google || "driving";

  const origin = from
    ? `&origin=${from.lat},${from.lng}`
    : "";

  return (
    "https://www.google.com/maps/dir/?api=1" +
    origin +
    `&destination=${to.lat},${to.lng}` +
    `&travelmode=${travelMode}`
  );
}
