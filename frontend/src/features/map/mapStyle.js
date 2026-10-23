// Satellite basemap with place/road labels and optional 3D terrain.
// Imagery: Esri World Imagery. Terrain: Mapzen/AWS Terrain Tiles. Labels: Esri reference layers.

const ESRI = "https://server.arcgisonline.com/ArcGIS/rest/services";

export const SATELLITE_STYLE = {
  version: 8,
  glyphs: "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf",
  sources: {
    satellite: {
      type: "raster",
      tiles: [`${ESRI}/World_Imagery/MapServer/tile/{z}/{y}/{x}`],
      tileSize: 256,
      maxzoom: 18,
      attribution: "Imagery © Esri, Maxar, Earthstar Geographics, and the GIS User Community",
    },
    transport: {
      type: "raster",
      tiles: [`${ESRI}/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}`],
      tileSize: 256,
      maxzoom: 18,
    },
    places: {
      type: "raster",
      tiles: [`${ESRI}/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}`],
      tileSize: 256,
      maxzoom: 18,
    },
    terrain: {
      type: "raster-dem",
      tiles: ["https://s3.amazonaws.com/elevation-tiles-prod/terrarium/{z}/{x}/{y}.png"],
      encoding: "terrarium",
      tileSize: 256,
      maxzoom: 14,
      attribution: "Terrain © Mapzen, AWS Open Data",
    },
  },
  layers: [
    { id: "satellite", type: "raster", source: "satellite" },
    { id: "transport", type: "raster", source: "transport", paint: { "raster-opacity": 0.9 } },
    { id: "places", type: "raster", source: "places" },
  ],
};

export const PAKISTAN_CENTER = [69.35, 30.4];
export const PAKISTAN_ZOOM = 4.9;
