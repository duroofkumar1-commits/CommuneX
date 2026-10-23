import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import maplibregl from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";
import "./MapExplorer.css";
import api, { errorMessage } from "../services/api";
import { SATELLITE_STYLE, PAKISTAN_CENTER, PAKISTAN_ZOOM } from "../features/map/mapStyle";
import {
  CATEGORIES, CATEGORY_KEYS, parsePois, parseBuildings, runOverpass, describePoi,
} from "../features/map/overpass";
import {
  cachedGeocode, fmtDistance, fmtDuration, geocodeText, haversineKm, isGeocoded, searchPlaces, tilesForBounds, unionBounds,
} from "../features/map/geo";
import { MODES, fetchAllRoutes, googleMapsDirectionsUrl } from "../features/map/routing";

const EMPTY_FC = { type: "FeatureCollection", features: [] };
const EMPTY_OVERVIEW = { helpRequests: [], events: [], resources: [], facilities: [], volunteers: [], sharingLocation: false };

const KINDS = {
  help: { label: "Help requests", emoji: "🆘" },
  event: { label: "Events", emoji: "📅" },
  volunteer: { label: "Volunteers", emoji: "🙋" },
  resource: { label: "Inventory", emoji: "📦" },
  facility: { label: "Facilities", emoji: "🏥" },
};
const PIN_TYPE = { help: "help-requests", event: "events", resource: "resources" };
const DEFAULT_LAYERS = { help: true, event: true, volunteer: true, resource: true, facility: true };
const DEFAULT_CATS = ["hospital", "pharmacy", "clinic"];

function fmtTime(value) {
  if (!value) return "";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? "" : d.toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
}

function selectionFromPoint(p) {
  const it = p.item;
  const base = { key: p.key, kind: p.kind, lat: p.lat, lng: p.lng, approx: p.approx, point: p, emoji: KINDS[p.kind].emoji };
  switch (p.kind) {
    case "help":
      return {
        ...base, title: it.title, subtitle: `Help request · ${it.category}`, description: it.description,
        badges: [it.priority, it.status],
        rows: [["Address given", it.locationText], ["Requested by", it.ownerName], ["Volunteer", it.extra || "Not assigned yet"], ["Created", fmtTime(it.time)]],
      };
    case "event":
      return {
        ...base, title: it.title, subtitle: "Community event", description: it.description, badges: [it.extra].filter(Boolean),
        rows: [["Where", it.locationText], ["When", fmtTime(it.time)], ["Organizer", it.ownerName]],
      };
    case "resource":
      return {
        ...base, title: it.title, subtitle: `Inventory · ${it.category}`, description: it.description, badges: [it.status],
        rows: [["Stored at", it.locationText], ["Quantity", it.extra], ["Updated", fmtTime(it.time)]],
      };
    case "facility":
      return {
        ...base, title: it.title, subtitle: `Facility · ${it.category}`, description: it.description, badges: [it.status],
        rows: [["Address", it.locationText], ["Phone", it.extra]],
      };
    default:
      return {
        ...base, title: it.name, subtitle: "Volunteer (sharing live location)", description: it.skills ? `Skills: ${it.skills}` : "",
        badges: ["ONLINE"], rows: [["Last update", fmtTime(it.updatedAt)], ["GPS accuracy", it.accuracy ? `± ${Math.round(it.accuracy)} m` : ""]],
      };
  }
}

export default function MapExplorer() {
  const wrapRef = useRef(null);
  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const markersRef = useRef(new Map());
  const userMarkerRef = useRef(null);
  const startMarkerRef = useRef(null);
  const poiStoreRef = useRef(new Map());
  const buildingStoreRef = useRef(new Map());
  const fetchedRef = useRef(new Set());
  const buildingFetchedRef = useRef(new Set());
  const poiAbortRef = useRef(null);
  const buildingAbortRef = useRef(null);
  const catsRef = useRef(new Set(DEFAULT_CATS));
  const modeRef = useRef({ pin: null, start: false });
  const threeDRef = useRef(true);
  const commitRef = useRef(() => {});
  const selectPoiRef = useRef(() => {});
  const userLocRef = useRef(null);
  const watchRef = useRef(null);
  const firstFixRef = useRef(true);
  const buildingsFnRef = useRef(() => {});
  const routeCacheRef = useRef(new Map());
  const fitKeyRef = useRef("");

  const [mapReady, setMapReady] = useState(false);
  const [me, setMe] = useState(null);
  const [overview, setOverview] = useState(EMPTY_OVERVIEW);
  const [geoTick, setGeoTick] = useState(0);
  const [layers, setLayers] = useState(DEFAULT_LAYERS);
  const [cats, setCats] = useState(() => new Set(DEFAULT_CATS));
  const [poiList, setPoiList] = useState([]);
  const [poiBusy, setPoiBusy] = useState(false);
  const [poiError, setPoiError] = useState("");
  const [zoomHint, setZoomHint] = useState("");
  const [threeD, setThreeD] = useState(true);
  const [selection, setSelection] = useState(null);
  const [locState, setLocState] = useState("idle");
  const [userLoc, setUserLoc] = useState(null);
  const [customStart, setCustomStart] = useState(null);
  const [startMode, setStartMode] = useState(false);
  const [pinTarget, setPinTarget] = useState(null);
  const [routeState, setRouteState] = useState({ key: null, data: null, error: "" });
  const [activeMode, setActiveMode] = useState("car");
  const [sharing, setSharing] = useState(false);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busyAction, setBusyAction] = useState(false);

  const origin = customStart || userLoc;
  const canShare = ["VOLUNTEER", "RESPONDER"].includes(me?.role);

  // ---------------------------------------------------------------- data
  const loadOverview = useCallback(async () => {
    try {
      const { data } = await api.get("/map/overview");
      setOverview({ ...EMPTY_OVERVIEW, ...data });
      setSharing(Boolean(data?.sharingLocation));
      setError("");
    } catch (e) {
      setError(errorMessage(e, "Could not load community map data."));
    }
  }, []);

  useEffect(() => {
    let alive = true;
    api.get("/users/me").then(({ data }) => { if (alive) setMe(data); }).catch(() => {});
    const first = setTimeout(loadOverview, 0);
    const timer = setInterval(loadOverview, 30000);
    return () => { alive = false; clearTimeout(first); clearInterval(timer); };
  }, [loadOverview]);

  // Items that only have a typed address are placed approximately by geocoding it once (cached in the browser).
  useEffect(() => {
    const texts = [...new Set(
      [...overview.helpRequests, ...overview.events, ...overview.resources]
        .filter((i) => !i.pinned && i.locationText).map((i) => i.locationText),
    )].filter((t) => !isGeocoded(t)).slice(0, 25);
    if (!texts.length) return undefined;
    const ctrl = new AbortController();
    (async () => {
      for (const text of texts) {
        if (ctrl.signal.aborted) return;
        try {
          await geocodeText(text, ctrl.signal);
          setGeoTick((t) => t + 1);
        } catch (e) {
          if (e?.name === "AbortError") return;
          return; // geocoder down: stop quietly, exact pins still work
        }
      }
    })();
    return () => ctrl.abort();
  }, [overview]);

  const points = useMemo(() => {
    void geoTick;
    const list = [];
    const add = (kind, item) => {
      let lat = item.latitude;
      let lng = item.longitude;
      let approx = false;
      if (lat == null || lng == null) {
        const g = item.locationText ? cachedGeocode(item.locationText) : null;
        if (!g) return;
        lat = g.lat; lng = g.lng; approx = true;
      }
      list.push({ key: `${kind}:${item.id}`, kind, item, lat, lng, approx });
    };
    overview.helpRequests.forEach((i) => add("help", i));
    overview.events.forEach((i) => add("event", i));
    overview.resources.forEach((i) => add("resource", i));
    overview.facilities.forEach((i) => add("facility", i));
    overview.volunteers.forEach((v) => list.push({ key: `volunteer:${v.userId}`, kind: "volunteer", item: v, lat: v.latitude, lng: v.longitude, approx: false }));
    return list;
  }, [overview, geoTick]);

  const unpinnedMine = useMemo(
    () => [
      ...overview.helpRequests.map((i) => ({ ...i, kind: "help" })),
      ...overview.events.map((i) => ({ ...i, kind: "event" })),
      ...overview.resources.map((i) => ({ ...i, kind: "resource" })),
    ].filter((i) => !i.pinned && i.canPin),
    [overview],
  );

  // ---------------------------------------------------------------- map places (OpenStreetMap)
  const refreshPois = useCallback(() => {
    const map = mapRef.current;
    const source = map?.getSource("pois");
    if (!source) return;
    const features = [];
    for (const p of poiStoreRef.current.values()) {
      if (!catsRef.current.has(p.cat)) continue;
      features.push({ type: "Feature", properties: { id: p.id, cat: p.cat, name: p.name }, geometry: { type: "Point", coordinates: [p.lng, p.lat] } });
    }
    source.setData({ type: "FeatureCollection", features });
    setPoiList([...poiStoreRef.current.values()]);
  }, []);

  const loadPois = useCallback(async () => {
    const map = mapRef.current;
    if (!map || !map.getSource("pois")) return;
    const zoom = map.getZoom();
    const wanted = [...catsRef.current];
    const active = wanted.filter((k) => zoom >= CATEGORIES[k].minZoom);
    const tooFar = wanted.filter((k) => zoom < CATEGORIES[k].minZoom);
    setZoomHint(tooFar.length ? `Zoom in to see: ${tooFar.map((k) => CATEGORIES[k].label).join(", ")}` : "");
    if (!active.length) return;
    const tz = Math.max(11, Math.min(14, Math.floor(zoom)));
    const b = map.getBounds();
    const tiles = tilesForBounds({ west: b.getWest(), east: b.getEast(), south: b.getSouth(), north: b.getNorth() }, tz);
    if (tiles.length > 60) { setZoomHint("Zoom in a little more to load places in this area."); return; }
    const missing = [];
    const need = new Set();
    for (const t of tiles) {
      for (const k of active) {
        const key = `${k}|${t.z}/${t.x}/${t.y}`;
        if (!fetchedRef.current.has(key)) { missing.push({ t, k, key }); need.add(k); }
      }
    }
    if (!missing.length) return;
    const uniqueTiles = [...new Map(missing.map((m) => [`${m.t.x}/${m.t.y}`, m.t])).values()];
    poiAbortRef.current?.abort();
    const ctrl = new AbortController();
    poiAbortRef.current = ctrl;
    setPoiBusy(true);
    try {
      const json = await runOverpass({
        type: "pois",
        bbox: unionBounds(uniqueTiles),
        categories: [...need],
      }, ctrl.signal);
      for (const p of parsePois(json, [...need])) poiStoreRef.current.set(p.id, p);
      missing.forEach((m) => fetchedRef.current.add(m.key));
      setPoiError("");
      refreshPois();
    } catch (e) {
      if (e?.name !== "AbortError" && e?.name !== "CanceledError" && e?.code !== "ERR_CANCELED") {
        console.error("CommuneX Overpass POI request failed:", {
          status: e?.response?.status,
          response: e?.response?.data,
          bbox: unionBounds(uniqueTiles),
          categories: [...need],
        });
        setPoiError(e?.response?.data?.message || "The places service is busy. Move the map or toggle a layer to retry.");
      }
    } finally {
      if (poiAbortRef.current === ctrl) setPoiBusy(false);
    }
  }, [refreshPois]);

  const loadBuildings = useCallback(async () => {
    const map = mapRef.current;
    if (!map || !map.getSource("buildings") || !threeDRef.current || map.getZoom() < 16) return;
    const b = map.getBounds();
    const c = map.getCenter();
    const tiles = tilesForBounds({ west: b.getWest(), east: b.getEast(), south: b.getSouth(), north: b.getNorth() }, 16)
      .filter((t) => !buildingFetchedRef.current.has(`${t.x}/${t.y}`));
    if (!tiles.length) return;
    const centre = tilesForBounds({ west: c.lng, east: c.lng, south: c.lat, north: c.lat }, 16)[0];
    tiles.sort((a, z) => Math.hypot(a.x - centre.x, a.y - centre.y) - Math.hypot(z.x - centre.x, z.y - centre.y));
    const batch = tiles.slice(0, 4);
    buildingAbortRef.current?.abort();
    const ctrl = new AbortController();
    buildingAbortRef.current = ctrl;
    try {
      const json = await runOverpass({
        type: "buildings",
        bbox: unionBounds(batch),
      }, ctrl.signal);
      for (const f of parseBuildings(json)) buildingStoreRef.current.set(f.id, f);
      batch.forEach((t) => buildingFetchedRef.current.add(`${t.x}/${t.y}`));
      map.getSource("buildings")?.setData({ type: "FeatureCollection", features: [...buildingStoreRef.current.values()] });
      if (tiles.length > batch.length) setTimeout(() => buildingsFnRef.current(), 1500);
    } catch (e) {
      console.error("CommuneX Overpass building request failed:", {
        status: e?.response?.status,
        response: e?.response?.data,
      });
      /* buildings are decoration; ignore failures */
    }
  }, []);

  useEffect(() => { buildingsFnRef.current = loadBuildings; }, [loadBuildings]);

  // ---------------------------------------------------------------- map setup
  useEffect(() => {
    const map = new maplibregl.Map({
      container: containerRef.current,
      style: SATELLITE_STYLE,
      center: PAKISTAN_CENTER,
      zoom: PAKISTAN_ZOOM,
      maxPitch: 80,
      maxZoom: 19.5,
      attributionControl: false,
    });
    mapRef.current = map;
    map.addControl(new maplibregl.AttributionControl({ compact: true }), "bottom-right");
    map.addControl(new maplibregl.NavigationControl({ visualizePitch: true }), "top-right");
    map.addControl(new maplibregl.FullscreenControl({ container: wrapRef.current }), "top-right");
    map.addControl(new maplibregl.ScaleControl({ unit: "metric" }), "bottom-left");

    map.on("load", () => {
      map.addSource("buildings", { type: "geojson", data: EMPTY_FC });
      map.addLayer({
        id: "buildings-3d", type: "fill-extrusion", source: "buildings", minzoom: 15.5,
        paint: { "fill-extrusion-color": "#eadfca", "fill-extrusion-height": ["get", "h"], "fill-extrusion-base": 0, "fill-extrusion-opacity": 0.8 },
      });
      map.addSource("route", { type: "geojson", data: EMPTY_FC });
      map.addLayer({ id: "route-casing", type: "line", source: "route", layout: { "line-cap": "round", "line-join": "round" }, paint: { "line-color": "#ffffff", "line-width": 9, "line-opacity": 0.95 } });
      map.addLayer({ id: "route-line", type: "line", source: "route", layout: { "line-cap": "round", "line-join": "round" }, paint: { "line-color": ["coalesce", ["get", "color"], "#2f80ed"], "line-width": 5.5 } });
      map.addSource("pois", { type: "geojson", data: EMPTY_FC });
      const colorMatch = ["match", ["get", "cat"], ...CATEGORY_KEYS.flatMap((k) => [k, CATEGORIES[k].color]), "#888888"];
      map.addLayer({
        id: "pois-circle", type: "circle", source: "pois",
        paint: { "circle-radius": ["interpolate", ["linear"], ["zoom"], 10, 4, 16, 9], "circle-color": colorMatch, "circle-stroke-color": "#ffffff", "circle-stroke-width": 1.8 },
      });
      map.addLayer({
        id: "pois-label", type: "symbol", source: "pois", minzoom: 15,
        layout: { "text-field": ["get", "name"], "text-font": ["Open Sans Semibold"], "text-size": 11, "text-offset": [0, 1.2], "text-anchor": "top", "text-optional": true },
        paint: { "text-color": "#ffffff", "text-halo-color": "#0b1d20", "text-halo-width": 1.5 },
      });
      map.setTerrain({ source: "terrain", exaggeration: 1.4 });
      setMapReady(true);
    });

    map.on("mouseenter", "pois-circle", () => { if (!modeRef.current.pin && !modeRef.current.start) map.getCanvas().style.cursor = "pointer"; });
    map.on("mouseleave", "pois-circle", () => { if (!modeRef.current.pin && !modeRef.current.start) map.getCanvas().style.cursor = ""; });
    map.on("click", (e) => {
      if (modeRef.current.pin) { commitRef.current(e.lngLat); return; }
      if (modeRef.current.start) { commitRef.current(e.lngLat, "start"); return; }
      const hit = map.getLayer("pois-circle") ? map.queryRenderedFeatures(e.point, { layers: ["pois-circle"] })[0] : null;
      if (hit) selectPoiRef.current(hit.properties.id);
    });

    let timer = null;
    map.on("moveend", () => {
      clearTimeout(timer);
      timer = setTimeout(() => { loadPois(); loadBuildings(); }, 500);
    });

    const markers = markersRef.current;
    return () => {
      clearTimeout(timer);
      poiAbortRef.current?.abort();
      buildingAbortRef.current?.abort();
      markers.forEach((m) => m.marker.remove());
      markers.clear();
      userMarkerRef.current?.remove();
      startMarkerRef.current?.remove();
      map.remove();
      mapRef.current = null;
    };
  }, [loadPois, loadBuildings]);

  // 3D toggle: terrain, tilt and building extrusions
  useEffect(() => {
    threeDRef.current = threeD;
    const map = mapRef.current;
    if (!map || !mapReady) return;
    map.setTerrain(threeD ? { source: "terrain", exaggeration: 1.4 } : null);
    map.setLayoutProperty("buildings-3d", "visibility", threeD ? "visible" : "none");
    map.easeTo({ pitch: threeD ? (map.getZoom() >= 9 ? 60 : 0) : 0, duration: 700 });
    if (threeD) loadBuildings();
  }, [threeD, mapReady, loadBuildings]);

  // Place layer toggles
  useEffect(() => {
    catsRef.current = cats;
    if (!mapReady) return;
    refreshPois();
    loadPois();
  }, [cats, mapReady, refreshPois, loadPois]);

  // ---------------------------------------------------------------- markers for CommuneX data
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !mapReady) return undefined;
    const store = markersRef.current;
    store.forEach((m) => m.marker.remove());
    store.clear();
    for (const p of points) {
      if (!layers[p.kind]) continue;
      const el = document.createElement("button");
      el.type = "button";
      const urgent = p.kind === "help" && ["HIGH", "CRITICAL"].includes(p.item.priority);
      el.className = `mx-marker mx-${p.kind}${urgent ? " urgent" : ""}${p.approx ? " approx" : ""}`;
      el.textContent = KINDS[p.kind].emoji;
      el.title = p.kind === "volunteer" ? p.item.name : p.item.title;
      el.addEventListener("click", (ev) => {
        ev.stopPropagation();
        if (modeRef.current.pin || modeRef.current.start) return;
        setSelection({ key: p.key, kind: p.kind });
      });
      const marker = new maplibregl.Marker({ element: el, anchor: "center" }).setLngLat([p.lng, p.lat]).addTo(map);
      store.set(p.key, { marker, el });
    }
    return () => { store.forEach((m) => m.marker.remove()); store.clear(); };
  }, [points, layers, mapReady]);

  useEffect(() => {
    markersRef.current.forEach((m, key) => m.el.classList.toggle("active", key === selection?.key));
  }, [selection, points, layers]);

  // ---------------------------------------------------------------- the user's own location
  const requestLocation = useCallback(() => {
    if (!navigator.geolocation) { setLocState("unsupported"); return; }
    setLocState("asking");
    if (watchRef.current != null) navigator.geolocation.clearWatch(watchRef.current);
    watchRef.current = navigator.geolocation.watchPosition(
      (pos) => {
        const next = { lat: pos.coords.latitude, lng: pos.coords.longitude, accuracy: pos.coords.accuracy };
        setLocState("granted");
        const prev = userLocRef.current;
        if (prev && haversineKm(prev, next) < 0.02) return; // ignore GPS jitter under 20 m
        userLocRef.current = next;
        setUserLoc(next);
      },
      (err) => setLocState(err.code === 1 ? "denied" : "error"),
      { enableHighAccuracy: true, maximumAge: 10000, timeout: 20000 },
    );
  }, []);

  useEffect(() => {
    const t = setTimeout(requestLocation, 0); // the browser shows its own permission prompt
    return () => {
      clearTimeout(t);
      if (watchRef.current != null && navigator.geolocation) navigator.geolocation.clearWatch(watchRef.current);
    };
  }, [requestLocation]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || !mapReady) return;
    if (!userLoc) { userMarkerRef.current?.remove(); userMarkerRef.current = null; return; }
    if (!userMarkerRef.current) {
      const el = document.createElement("div");
      el.className = "mx-me";
      el.title = "You are here";
      userMarkerRef.current = new maplibregl.Marker({ element: el }).setLngLat([userLoc.lng, userLoc.lat]).addTo(map);
    } else {
      userMarkerRef.current.setLngLat([userLoc.lng, userLoc.lat]);
    }
    if (firstFixRef.current) {
      firstFixRef.current = false;
      map.flyTo({ center: [userLoc.lng, userLoc.lat], zoom: 14.5, pitch: threeDRef.current ? 55 : 0, duration: 1800 });
    }
  }, [userLoc, mapReady]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || !mapReady) return;
    if (!customStart) { startMarkerRef.current?.remove(); startMarkerRef.current = null; return; }
    if (!startMarkerRef.current) {
      const el = document.createElement("div");
      el.className = "mx-start";
      el.textContent = "🚩";
      el.title = "Custom start point";
      startMarkerRef.current = new maplibregl.Marker({ element: el, anchor: "bottom" }).setLngLat([customStart.lng, customStart.lat]).addTo(map);
    } else {
      startMarkerRef.current.setLngLat([customStart.lng, customStart.lat]);
    }
  }, [customStart, mapReady]);

  // Volunteers/responders can opt in to share their live position with the community.
  useEffect(() => {
    if (!sharing || !canShare) return undefined;
    const send = () => {
      const loc = userLocRef.current;
      if (!loc) return;
      api.put("/map/me/location", { latitude: loc.lat, longitude: loc.lng, accuracy: loc.accuracy }).catch(() => {});
    };
    send();
    const timer = setInterval(send, 45000);
    return () => clearInterval(timer);
  }, [sharing, canShare, userLoc]);

  async function toggleSharing() {
    setError(""); setNotice("");
    try {
      if (sharing) {
        await api.delete("/map/me/location");
        setSharing(false);
        setNotice("You are no longer visible on the community map.");
      } else {
        if (!userLocRef.current) { setError("Allow location access first so your position can be shared."); return; }
        const loc = userLocRef.current;
        await api.put("/map/me/location", { latitude: loc.lat, longitude: loc.lng, accuracy: loc.accuracy });
        setSharing(true);
        setNotice("Community members can now see your live location while you are available.");
      }
      loadOverview();
    } catch (e) {
      setError(errorMessage(e, "Could not change location sharing."));
    }
  }

  // ---------------------------------------------------------------- selection
  const selectPoi = useCallback((id) => {
    const p = poiStoreRef.current.get(id);
    if (!p) return;
    const cat = CATEGORIES[p.cat];
    setSelection({
      key: `poi:${p.id}`, kind: "poi",
      payload: { key: `poi:${p.id}`, kind: "poi", title: p.name, subtitle: cat.label.replace(/s$/, ""), emoji: cat.emoji, lat: p.lat, lng: p.lng, rows: describePoi(p), badges: [] },
    });
  }, []);
  useEffect(() => { selectPoiRef.current = selectPoi; }, [selectPoi]);

  const selected = useMemo(() => {
    if (!selection) return null;
    if (selection.payload) return selection.payload;
    const p = points.find((x) => x.key === selection.key);
    return p ? selectionFromPoint(p) : null;
  }, [selection, points]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map || !mapReady || !selected) return;
    if (!origin) map.flyTo({ center: [selected.lng, selected.lat], zoom: Math.max(map.getZoom(), 15), duration: 900 });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected?.key, mapReady]);

  const nearby = useMemo(() => {
    if (!selected) return [];
    const here = { lat: selected.lat, lng: selected.lng };
    const out = [];
    for (const cat of ["hospital", "pharmacy"]) {
      let best = null;
      for (const p of poiList) {
        if (p.cat !== cat || selected.key === `poi:${p.id}`) continue;
        const d = haversineKm(here, p);
        if (!best || d < best.d) best = { p, d };
      }
      if (best) out.push({ id: best.p.id, label: `Nearest ${CATEGORIES[cat].label.toLowerCase().replace(/s$/, "")}`, emoji: CATEGORIES[cat].emoji, name: best.p.name, km: best.d, poi: true });
    }
    if (selected.kind === "help") {
      let best = null;
      for (const v of overview.volunteers) {
        const d = haversineKm(here, { lat: v.latitude, lng: v.longitude });
        if (!best || d < best.d) best = { v, d };
      }
      if (best) out.push({ id: `volunteer:${best.v.userId}`, label: "Nearest volunteer", emoji: "🙋", name: best.v.name, km: best.d, poi: false });
    }
    return out;
  }, [selected, poiList, overview.volunteers]);

  function openNearby(n) {
    if (n.poi) {
      selectPoi(n.id);
      const p = poiStoreRef.current.get(n.id);
      if (p) mapRef.current?.flyTo({ center: [p.lng, p.lat], zoom: Math.max(mapRef.current.getZoom(), 15.5) });
    } else {
      setSelection({ key: n.id, kind: "volunteer" });
    }
  }

  // ---------------------------------------------------------------- routes
  const routeKey = origin && selected
    ? `${origin.lat.toFixed(4)},${origin.lng.toFixed(4)}>${selected.lat.toFixed(5)},${selected.lng.toFixed(5)}`
    : null;

  useEffect(() => {
    if (!routeKey) return undefined;
    const cached = routeCacheRef.current.get(routeKey);
    if (cached) { queueMicrotask(() => setRouteState({ key: routeKey, data: cached, error: "" })); return undefined; }
    const ctrl = new AbortController();
    fetchAllRoutes({ lat: origin.lat, lng: origin.lng }, { lat: selected.lat, lng: selected.lng }, ctrl.signal)
      .then((data) => {
        routeCacheRef.current.set(routeKey, data);
        setRouteState({ key: routeKey, data, error: "" });
      })
      .catch((e) => {
        if (e?.name !== "AbortError") {
          setRouteState({
            key: routeKey,
            data: null,
            error: e?.message || "Could not calculate a road-following route. Please retry or open Google Maps.",
          });
        }
      });
    return () => ctrl.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [routeKey]);

  const routes = routeState.key === routeKey ? routeState.data : null;
  const routeLoading = Boolean(routeKey) && routeState.key !== routeKey;
  const routeError = routeState.key === routeKey ? routeState.error : "";

  useEffect(() => {
    const map = mapRef.current;
    const source = map && mapReady ? map.getSource("route") : null;
    if (!source) return;
    const route = routes?.[activeMode];
    if (!route) { source.setData(EMPTY_FC); return; }
    const color = MODES.find((m) => m.key === activeMode)?.color;
    source.setData({ type: "Feature", properties: { color }, geometry: route.geometry });
    if (fitKeyRef.current !== routeKey) {
      fitKeyRef.current = routeKey;
      const coords = route.geometry.coordinates;
      const bounds = coords.reduce((b, c) => b.extend(c), new maplibregl.LngLatBounds(coords[0], coords[0]));
      const right = window.innerWidth > 900 ? 400 : 40;
      map.fitBounds(bounds, { padding: { top: 70, bottom: 70, left: 50, right }, maxZoom: 16.5, duration: 1000 });
    }
  }, [routes, activeMode, mapReady, routeKey]);

  // ---------------------------------------------------------------- pin / start-point modes
  useEffect(() => {
    modeRef.current = { pin: pinTarget, start: startMode };
    const map = mapRef.current;
    if (map) map.getCanvas().style.cursor = pinTarget || startMode ? "crosshair" : "";
  }, [pinTarget, startMode]);

  useEffect(() => {
    commitRef.current = async (lngLat, mode) => {
      if (mode === "start") {
        setCustomStart({ lat: lngLat.lat, lng: lngLat.lng });
        setStartMode(false);
        return;
      }
      const target = modeRef.current.pin;
      if (!target) return;
      setBusyAction(true); setError(""); setNotice("");
      try {
        await api.put(`/map/locations/${PIN_TYPE[target.kind]}/${target.id}`, { latitude: lngLat.lat, longitude: lngLat.lng });
        setNotice(`Exact location saved for “${target.title}”.`);
        setPinTarget(null);
        await loadOverview();
      } catch (e) {
        setError(errorMessage(e, "Could not save the location."));
      } finally {
        setBusyAction(false);
      }
    };
  }, [loadOverview]);

  async function acceptRequest(id) {
    setBusyAction(true); setError(""); setNotice("");
    try {
      await api.post(`/requests/${id}/accept`);
      setNotice("Request accepted. The route to the person is shown on the map.");
      await loadOverview();
    } catch (e) {
      setError(errorMessage(e, "Could not accept this request."));
    } finally {
      setBusyAction(false);
    }
  }

  // ---------------------------------------------------------------- search
  useEffect(() => {
    const q = query.trim();
    if (q.length < 3) return undefined;
    const ctrl = new AbortController();
    const t = setTimeout(async () => {
      setSearching(true);
      try { setResults(await searchPlaces(q, ctrl.signal)); }
      catch (e) { if (e?.name !== "AbortError") setResults([]); }
      finally { setSearching(false); }
    }, 450);
    return () => { clearTimeout(t); ctrl.abort(); };
  }, [query]);

  function openPlace(r) {
    const map = mapRef.current;
    setResults([]);
    setQuery(r.name);
    setSelection({ key: `place:${r.id}`, kind: "place", payload: { key: `place:${r.id}`, kind: "place", title: r.name || "Searched place", subtitle: "Search result", emoji: "📍", lat: r.lat, lng: r.lng, rows: [["Full address", r.full]], badges: [] } });
    if (!map) return;
    if (r.bbox && r.bbox.length === 4) map.fitBounds([[r.bbox[2], r.bbox[0]], [r.bbox[3], r.bbox[1]]], { padding: 80, maxZoom: 16, duration: 1200 });
    else map.flyTo({ center: [r.lng, r.lat], zoom: 15, duration: 1200 });
  }

  function recenter() {
    const map = mapRef.current;
    if (!map) return;
    if (userLoc) map.flyTo({ center: [userLoc.lng, userLoc.lat], zoom: 15, pitch: threeD ? 55 : 0, duration: 1200 });
    else requestLocation();
  }

  function resetPakistan() {
    mapRef.current?.flyTo({ center: PAKISTAN_CENTER, zoom: PAKISTAN_ZOOM, pitch: 0, bearing: 0, duration: 1400 });
  }

  const toggleLayer = (k) => setLayers((l) => ({ ...l, [k]: !l[k] }));
  const toggleCat = (k) => setCats((s) => { const n = new Set(s); if (n.has(k)) n.delete(k); else n.add(k); return n; });
  const counts = { help: overview.helpRequests.length, event: overview.events.length, volunteer: overview.volunteers.length, resource: overview.resources.length, facility: overview.facilities.length };
  const selectedPoint = selected?.point;
  const selectedItem = selectedPoint?.item;
  const locationMessage = {
    idle: "Location not requested yet.",
    asking: "Waiting for your browser’s permission…",
    granted: userLoc ? `Live location on${userLoc.accuracy ? ` (± ${Math.round(userLoc.accuracy)} m)` : ""}.` : "Locating you…",
    denied: "Location is blocked. Click the lock icon in the address bar, allow Location, then press Retry.",
    unsupported: "This browser does not support location.",
    error: "Could not read your location. Check GPS/Wi-Fi and retry. (Needs HTTPS or localhost.)",
  }[locState];

  // ---------------------------------------------------------------- render
  return (
    <div className="mx-page">
      <div className="mx-layout" ref={wrapRef}>
        <aside className="mx-sidebar">
          <div className="mx-brand"><span>🛰️</span><div><strong>Community map</strong><small>Satellite · 3D · live help</small></div></div>

          <div className="mx-search">
            <input value={query} onChange={(e) => { setQuery(e.target.value); if (e.target.value.trim().length < 3) setResults([]); }}
              placeholder="Search any place in Pakistan…" aria-label="Search places" />
            {searching && <span className="mx-spin" aria-label="Searching" />}
            {results.length > 0 && (
              <ul className="mx-results">
                {results.map((r) => (
                  <li key={r.id}><button type="button" onClick={() => openPlace(r)}><strong>{r.name}</strong><small>{r.full}</small></button></li>
                ))}
              </ul>
            )}
          </div>

          {error && <div className="mx-alert error" role="alert">{error}</div>}
          {notice && <div className="mx-alert ok">{notice}</div>}

          <section className="mx-card">
            <h3>📡 My location</h3>
            <p className={`mx-loc ${locState}`}>{locationMessage}</p>
            <div className="mx-row">
              <button type="button" className="mx-btn" onClick={locState === "granted" ? recenter : requestLocation}>{locState === "granted" ? "Go to me" : locState === "denied" || locState === "error" ? "Retry" : "Allow location"}</button>
              <button type="button" className={`mx-btn ghost ${startMode ? "on" : ""}`} onClick={() => { setStartMode((v) => !v); setPinTarget(null); }}>{startMode ? "Cancel" : "🚩 Set start on map"}</button>
            </div>
            {customStart && <p className="mx-note">Routes start from your custom point. <button type="button" className="mx-link" onClick={() => setCustomStart(null)}>Use my location</button></p>}
            {canShare && (
              <label className="mx-switch">
                <input type="checkbox" checked={sharing} onChange={toggleSharing} />
                <span>Share my live location as a volunteer</span>
              </label>
            )}
            {canShare && <p className="mx-note">Only signed-in members see it, only while sharing is on, and it expires after 6 hours without updates.</p>}
          </section>

          <section className="mx-card">
            <h3>🧭 CommuneX layers</h3>
            <div className="mx-chips">
              {Object.entries(KINDS).map(([k, v]) => (
                <button key={k} type="button" className={`mx-chip ${layers[k] ? "on" : ""}`} onClick={() => toggleLayer(k)}>
                  <span>{v.emoji}</span>{v.label}<b>{counts[k]}</b>
                </button>
              ))}
            </div>
          </section>

          <section className="mx-card">
            <h3>🗺️ Places (OpenStreetMap)</h3>
            <div className="mx-chips">
              {CATEGORY_KEYS.map((k) => (
                <button key={k} type="button" className={`mx-chip ${cats.has(k) ? "on" : ""}`} onClick={() => toggleCat(k)} style={cats.has(k) ? { borderColor: CATEGORIES[k].color, background: `${CATEGORIES[k].color}22` } : undefined}>
                  <span>{CATEGORIES[k].emoji}</span>{CATEGORIES[k].label}
                </button>
              ))}
            </div>
            {poiBusy && <p className="mx-note">Loading places in this area…</p>}
            {zoomHint && <p className="mx-note">{zoomHint}</p>}
            {poiError && <p className="mx-note warn">{poiError}</p>}
          </section>

          <section className="mx-card">
            <h3>🛠️ View</h3>
            <div className="mx-row">
              <button type="button" className={`mx-btn ${threeD ? "" : "ghost"}`} onClick={() => setThreeD((v) => !v)}>{threeD ? "3D on" : "3D off"}</button>
              <button type="button" className="mx-btn ghost" onClick={resetPakistan}>All Pakistan</button>
            </div>
            <p className="mx-note">Tilt with Ctrl + drag (or right-drag). Zoom in past level 16 to see 3D buildings.</p>
          </section>

          {unpinnedMine.length > 0 && (
            <section className="mx-card">
              <h3>📍 Add exact location</h3>
              <p className="mx-note">These have only a typed address. Pin them precisely so helpers can route to the right spot.</p>
              <ul className="mx-list">
                {unpinnedMine.slice(0, 8).map((i) => (
                  <li key={`${i.kind}:${i.id}`}>
                    <span>{KINDS[i.kind].emoji} {i.title}</span>
                    <button type="button" className="mx-btn small" onClick={() => { setPinTarget({ kind: i.kind, id: i.id, title: i.title }); setStartMode(false); }}>Pin</button>
                  </li>
                ))}
              </ul>
            </section>
          )}

          <p className="mx-credit">Imagery © Esri &amp; partners · Places © OpenStreetMap contributors · Routes by OSM-based servers. Estimates only — not for emergency navigation.</p>
        </aside>

        <div className="mx-mapwrap">
          <div ref={containerRef} className="mx-map" />

          {(pinTarget || startMode) && (
            <div className="mx-banner">
              {pinTarget ? `Click the map to place “${pinTarget.title}”` : "Click the map to choose your start point"}
              <button type="button" onClick={() => { setPinTarget(null); setStartMode(false); }}>Cancel</button>
            </div>
          )}

          {selected && (
            <aside className="mx-details">
              <header>
                <span className="mx-emoji">{selected.emoji}</span>
                <div><h2>{selected.title}</h2><p>{selected.subtitle}</p></div>
                <button type="button" className="mx-close" onClick={() => setSelection(null)} aria-label="Close details">×</button>
              </header>

              {selected.badges?.length > 0 && <div className="mx-badges">{selected.badges.filter(Boolean).map((b) => <span key={b} className={`mx-badge ${String(b).toLowerCase()}`}>{String(b).replace(/_/g, " ")}</span>)}</div>}
              {selected.approx && <p className="mx-note warn">Approximate position found from the typed address.{selectedItem?.canPin ? " Pin the exact spot below." : ""}</p>}
              {selected.description && <p className="mx-desc">{selected.description}</p>}
              <dl className="mx-rows">
                {selected.rows?.filter(([, v]) => v).map(([k, v]) => (
                  <div key={k}><dt>{k}</dt><dd>{/^https?:\/\//.test(v) ? <a href={v} target="_blank" rel="noreferrer noopener">{v}</a> : v}</dd></div>
                ))}
              </dl>

              <div className="mx-actions">
                {selected.kind === "help" && ["VOLUNTEER", "RESPONDER"].includes(me?.role) && selectedItem?.status === "OPEN" && (
                  <button type="button" className="mx-btn" disabled={busyAction} onClick={() => acceptRequest(selectedItem.id)}>Accept this request</button>
                )}
                {selected.kind === "help" && <Link className="mx-btn ghost" to="/help-requests">Open in Help requests</Link>}
                {selected.kind === "event" && <Link className="mx-btn ghost" to="/events">Open in Events</Link>}
                {selected.kind === "resource" && <Link className="mx-btn ghost" to="/resources">Open in Inventory</Link>}
                {selected.kind === "facility" && <Link className="mx-btn ghost" to="/facilities">Open in Facilities</Link>}
                {selectedItem?.canPin && (
                  <button type="button" className="mx-btn ghost" onClick={() => setPinTarget({ kind: selected.kind, id: selectedItem.id, title: selectedItem.title })}>
                    {selectedItem.pinned ? "Move pin" : "📍 Set exact location"}
                  </button>
                )}
              </div>

              {nearby.length > 0 && (
                <div className="mx-sec">
                  <h4>Close to this spot</h4>
                  <ul className="mx-nearby">
                    {nearby.map((n) => (
                      <li key={n.label}><button type="button" onClick={() => openNearby(n)}><span>{n.emoji}</span><div><strong>{n.name}</strong><small>{n.label} · {fmtDistance(n.km)} straight line</small></div></button></li>
                    ))}
                  </ul>
                </div>
              )}

              <div className="mx-sec">
                <h4>How to get there</h4>
                {!origin && (
                  <div>
                    <p className="mx-note">{locState === "denied" ? "Location is blocked, so choose a start point on the map." : "Allow your location, or choose a start point on the map."}</p>
                    <div className="mx-row">
                      {locState !== "granted" && <button type="button" className="mx-btn" onClick={requestLocation}>Allow location</button>}
                      <button type="button" className="mx-btn ghost" onClick={() => setStartMode(true)}>🚩 Set start on map</button>
                    </div>
                  </div>
                )}
                {origin && (
                  <>
                    <p className="mx-note">From {customStart ? "your chosen start point" : "your current location"}{routes && " · straight-line distance " + fmtDistance(haversineKm(origin, selected))}</p>
                    {routeLoading && <p className="mx-note">Calculating routes…</p>}
                    {routeError && <p className="mx-note warn">{routeError}</p>}
                    {routes && (
                      <div className="mx-modes">
                        {MODES.map((m) => (
                          <button key={m.key} type="button" className={`mx-mode ${activeMode === m.key ? "on" : ""}`} style={activeMode === m.key ? { borderColor: m.color } : undefined} onClick={() => setActiveMode(m.key)}>
                            <span className="mx-mode-icon">{m.icon}</span>
                            <strong>{routes[m.key].approximate ? "≈ " : ""}{fmtDuration(routes[m.key].minutes)}</strong>
                            <small>{fmtDistance(routes[m.key].distanceKm)} · {m.label}</small>
                          </button>
                        ))}
                      </div>
                    )}
                    {routes && Object.values(routes).some((r) => r.approximate) && <p className="mx-note warn">A road route could not be calculated for at least one mode, so ≈ values are rough straight-line estimates.</p>}
                    {routes && Object.values(routes).some((r) => String(r.source || "").toLowerCase().includes("valhalla")) && <p className="mx-note">The local route service was unavailable, so this route follows mapped roads using the backup routing service.</p>}
                    {(routes || routeError) && (
                      <a className="mx-btn ghost wide" target="_blank" rel="noreferrer noopener" href={googleMapsDirectionsUrl(origin, selected, activeMode)}>Open turn-by-turn in Google Maps ↗</a>
                    )}
                    <p className="mx-note">Motorbike time is an estimate based on the car route. Traffic and road closures are not included.</p>
                  </>
                )}
              </div>
            </aside>
          )}

          <div className="mx-legend" aria-hidden="true">
            <span><i className="dot help" />Help</span><span><i className="dot event" />Event</span><span><i className="dot volunteer" />Volunteer</span><span><i className="dot resource" />Inventory</span><span><i className="dot facility" />Facility</span>
          </div>
        </div>
      </div>
    </div>
  );
}
