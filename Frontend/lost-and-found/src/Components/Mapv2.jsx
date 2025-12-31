import { useEffect, useRef } from "react";
import maplibregl from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";

export default function Mapv2() {
  const mapContainer = useRef(null);
  const mapRef = useRef(null);

  const center = [51.3516, 35.702831]; // [lng, lat]
  const delta = 0.0055;

  // Same bounds logic as Leaflet
  const bounds = [
    [center[0] - delta, center[1] - delta], // SW
    [center[0] + delta, center[1] + delta], // NE
  ];

  useEffect(() => {
    if (mapRef.current) return;

    mapRef.current = new maplibregl.Map({
      container: mapContainer.current,
      style: {
        version: 8,
        sources: {
          osm: {
            type: "raster",
            tiles: [
              "https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
              "https://b.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
              "https://c.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
            ],
            tileSize: 256,
          },
        },
        layers: [{ id: "osm", type: "raster", source: "osm" }],
      },
      center,
      zoom: 17,
      minZoom: 17,
      maxZoom: 20,
      maxBounds: bounds,              // 🔒 restrict panning
      cooperativeGestures: true,
    });

    // 🔒 Hard lock after move (like maxBoundsViscosity = 1)
    mapRef.current.on("moveend", () => {
      const current = mapRef.current.getBounds();
      if (!contains(bounds, current)) {
        mapRef.current.fitBounds(bounds, { duration: 0 });
      }
    });

    // 📍 Add marker ONLY inside bounds
    mapRef.current.on("click", (e) => {
      const { lng, lat } = e.lngLat;

      if (
        lng < bounds[0][0] ||
        lng > bounds[1][0] ||
        lat < bounds[0][1] ||
        lat > bounds[1][1]
      )
        return;

      new maplibregl.Marker()
        .setLngLat([lng, lat])
        .setPopup(
          new maplibregl.Popup().setText("Lost / Found Item")
        )
        .addTo(mapRef.current);
    });

    return () => mapRef.current?.remove();
  }, []);

  return (
    <div
      ref={mapContainer}
      style={{ width: "90vw", height: "90vh", margin: "auto" }}
    />
  );
}

/* ---------- helpers ---------- */

function contains(outer, inner) {
  const [swO, neO] = outer;
  const swI = inner.getSouthWest();
  const neI = inner.getNorthEast();

  return (
    swI.lng >= swO[0] &&
    swI.lat >= swO[1] &&
    neI.lng <= neO[0] &&
    neI.lat <= neO[1]
  );
}
