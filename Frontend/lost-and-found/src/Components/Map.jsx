import { useEffect, useRef, useState } from "react";
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  useMap,
} from "react-leaflet";
import MarkerClusterGroup from "react-leaflet-markercluster";
import L from "leaflet";

import "leaflet/dist/leaflet.css";
import "../assets/Map.css";

/* ================== config ================== */
const API_BASE = import.meta.env.VITE_API_BASE_URL;
const TILE_URL = import.meta.env.VITE_MAP_TILE_URL;
const TILE_ATTR = import.meta.env.VITE_MAP_ATTRIBUTION;

/* ================== api ================== */
async function getLostItems() {
  const res = await fetch(`${API_BASE}/lost-items`);
  if (!res.ok) throw new Error("Failed to load items");
  return res.json();
}

/* ================== helpers ================== */
function markerIcon(category) {
  return L.divIcon({
    className: "",
    html: `<div class="custom-marker marker-${category}"></div>`,
    iconSize: [14, 14],
    iconAnchor: [7, 7],
    category,
  });
}

function FlyToMarker({ item }) {
  const map = useMap();

  useEffect(() => {
    if (item) {
      map.flyTo([item.x, item.y], 18);
    }
  }, [item, map]);

  return null;
}

/* ================== main ================== */
export default function LostAndFoundMap() {
  const center = [35.702831, 51.3516];
  const delta = 0.0055;

  const bounds = [
    [center[0] - delta, center[1] - delta],
    [center[0] + delta, center[1] + delta],
  ];

  const categories = ["electronics", "documents", "clothing", "other"];

  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [selectedCategories, setSelectedCategories] =
    useState(categories);

  const [itemsError, setItemsError] = useState(false);
  const [mapError, setMapError] = useState(false);

  const markerRefs = useRef({});

  /* ---------- load items ---------- */
  useEffect(() => {
    getLostItems()
      .then(setItems)
      .catch(() => setItemsError(true));
  }, []);

  /* ---------- open popup ---------- */
  useEffect(() => {
    if (selectedId && markerRefs.current[selectedId]) {
      markerRefs.current[selectedId].openPopup();
    }
  }, [selectedId]);

  const selectedItem = items.find((i) => i.id === selectedId);

  return (
    <div className="app-container">
      {itemsError && (
        <div className="simple-error">
          Failed to load lost items.
        </div>
      )}

      {mapError && (
        <div className="simple-error">
          Failed to load map.
        </div>
      )}

      {!mapError && (
        <div className="map-wrapper">
          <MapContainer
            center={center}
            zoom={16}
            minZoom={16}
            maxZoom={18}
            maxBounds={bounds}
            style={{ width: "100%", height: "100%" }}
          >
            <TileLayer
              url={TILE_URL}
              attribution={TILE_ATTR}
              eventHandlers={{
                tileerror: () => setMapError(true),
              }}
            />

            <MarkerClusterGroup>
              {items
                .filter((i) =>
                  selectedCategories.includes(i.category)
                )
                .map((item) => (
                  <Marker
                    key={item.id}
                    position={[item.x, item.y]}
                    icon={markerIcon(item.category)}
                    ref={(ref) =>
                      (markerRefs.current[item.id] = ref)
                    }
                  >
                    <Popup>
                      <strong>{item.name}</strong>
                    </Popup>
                  </Marker>
                ))}
            </MarkerClusterGroup>

            {selectedItem && <FlyToMarker item={selectedItem} />}
          </MapContainer>
        </div>
      )}
    </div>
  );
}


