import { describe, expect, it } from "vitest";
import { fmtDistance, fmtDuration, haversineKm, tilesForBounds, unionBounds, tileBounds } from "../features/map/geo";
import { buildBuildingQuery, buildPoiQuery, classify, parseBuildings, parsePois, buildingHeight } from "../features/map/overpass";
import { straightLineRoute, googleMapsDirectionsUrl } from "../features/map/routing";

describe("geo helpers", () => {
  it("computes the distance between Karachi and Lahore", () => {
    const d = haversineKm({ lat: 24.8607, lng: 67.0011 }, { lat: 31.5204, lng: 74.3587 });
    expect(d).toBeGreaterThan(1030);
    expect(d).toBeLessThan(1050);
  });
  it("formats distance and duration", () => {
    expect(fmtDistance(0.45)).toBe("450 m");
    expect(fmtDistance(3.14)).toBe("3.1 km");
    expect(fmtDistance(42.4)).toBe("42 km");
    expect(fmtDuration(0.2)).toBe("< 1 min");
    expect(fmtDuration(35)).toBe("35 min");
    expect(fmtDuration(125)).toBe("2 h 5 min");
  });
  it("covers a box with tiles and unions them back to a containing box", () => {
    const box = { west: 69.78, east: 69.84, south: 24.72, north: 24.76 };
    const tiles = tilesForBounds(box, 13);
    expect(tiles.length).toBeGreaterThan(0);
    const u = unionBounds(tiles);
    expect(u.west).toBeLessThanOrEqual(box.west);
    expect(u.east).toBeGreaterThanOrEqual(box.east);
    expect(u.south).toBeLessThanOrEqual(box.south);
    expect(u.north).toBeGreaterThanOrEqual(box.north);
    const tb = tileBounds(tiles[0].x, tiles[0].y, 13);
    expect(tb.east).toBeGreaterThan(tb.west);
    expect(tb.north).toBeGreaterThan(tb.south);
  });
});

describe("overpass helpers", () => {
  it("classifies OSM tags", () => {
    expect(classify({ amenity: "hospital" })).toBe("hospital");
    expect(classify({ amenity: "pharmacy" })).toBe("pharmacy");
    expect(classify({ shop: "chemist" })).toBe("pharmacy");
    expect(classify({ shop: "mall" })).toBe("mall");
    expect(classify({ shop: "bakery" })).toBe("shop");
    expect(classify({ amenity: "place_of_worship", religion: "muslim" })).toBe("worship");
    expect(classify({ highway: "residential" })).toBeNull();
  });
  it("builds a bounded query", () => {
    const q = buildPoiQuery({ south: 24.7, west: 69.7, north: 24.8, east: 69.9 }, ["hospital", "pharmacy"], 500);
    expect(q).toContain('nwr["amenity"="hospital"](24.70000,69.70000,24.80000,69.90000);');
    expect(q).toContain("out tags center 500;");
    expect(buildBuildingQuery({ south: 1, west: 2, north: 3, east: 4 })).toContain('way["building"](1.00000,2.00000,3.00000,4.00000)');
  });
  it("parses nodes and ways with centres, skipping unusable elements", () => {
    const pois = parsePois({
      elements: [
        { type: "node", id: 1, lat: 24.7, lon: 69.8, tags: { amenity: "hospital", name: "Civil Hospital" } },
        { type: "way", id: 2, center: { lat: 24.71, lon: 69.81 }, tags: { amenity: "pharmacy" } },
        { type: "node", id: 3, lat: 24.7, lon: 69.8, tags: { highway: "bus_stop" } },
        { type: "node", id: 4, tags: { amenity: "hospital" } },
      ],
    });
    expect(pois.map((p) => p.id)).toEqual(["node/1", "way/2"]);
    expect(pois[0].name).toBe("Civil Hospital");
    expect(pois[1].name).toBe("Medical store");
  });
  it("turns building footprints into closed polygons with heights", () => {
    const f = parseBuildings({
      elements: [{ type: "way", id: 9, tags: { "building:levels": "3" }, geometry: [{ lat: 1, lon: 1 }, { lat: 1, lon: 2 }, { lat: 2, lon: 2 }, { lat: 2, lon: 1 }] }],
    });
    expect(f).toHaveLength(1);
    const ring = f[0].geometry.coordinates[0];
    expect(ring[0]).toEqual(ring[ring.length - 1]);
    expect(f[0].properties.h).toBeCloseTo(9.6);
    expect(buildingHeight({ height: "12 m" })).toBe(12);
    expect(buildingHeight({})).toBe(6);
  });
});

describe("routing helpers", () => {
  it("creates a flagged straight-line estimate only when explicitly requested", () => {
    const r = straightLineRoute({ lat: 24.7, lng: 69.8 }, { lat: 24.8, lng: 69.9 }, 30);
    expect(r.approximate).toBe(true);
    expect(r.distanceKm).toBeGreaterThan(10);
    expect(r.geometry.coordinates).toHaveLength(2);
  });
  it("builds a Google Maps link with the right travel mode", () => {
    const url = googleMapsDirectionsUrl({ lat: 1, lng: 2 }, { lat: 3, lng: 4 }, "bike");
    expect(url).toContain("origin=1,2");
    expect(url).toContain("destination=3,4");
    expect(url).toContain("travelmode=two-wheeler");
  });
});
