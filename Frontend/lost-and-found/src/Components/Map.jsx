import { useEffect, useRef, useState } from "react";
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  useMapEvents,
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
  if (!res.ok) throw new Error("Failed to fetch items");
  return res.json();
}

async function createLostItem(item) {
  const res = await fetch(`${API_BASE}/lost-items`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(item),
  });
  if (!res.ok) throw new Error("Failed to create item");
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

/* ================== map helpers ================== */
function AddMarker({ bounds, onAdd }) {
  useMapEvents({
    async click(e) {
      const { lat, lng } = e.latlng;
      const [[south, west], [north, east]] = bounds;

      if (lat < south || lat > north || lng < west || lng > east) return;

      const item = {
        name: "Item",
        x: lat,
        y: lng,
        category: "other",
        timestamp: new Date().toISOString(),
      };

      const saved = await createLostItem(item);
      onAdd(saved);
    },
  });

  return null;
}

function FlyToMarker({ item }) {
  const map = useMap();

  useEffect(() => {
    if (item) {
      map.flyTo([item.x, item.y], 18, { duration: 0.8 });
    }
  }, [item, map]);

  return null;
}

/* ================== sidebar ================== */
function Sidebar({
  items,
  selectedId,
  onSelect,
  categories,
  selectedCategories,
  toggleCategory,
}) {
  return (
    <div className="sidebar">
      <h3>Lost Items</h3>

      <div className="filter-section">
        <strong>Filter by Category</strong>
        {categories.map((cat) => (
          <label key={cat} className="filter-label">
            <input
              type="checkbox"
              checked={selectedCategories.includes(cat)}
              onChange={() => toggleCategory(cat)}
            />
            {cat}
          </label>
        ))}
      </div>

      {items
        .filter((i) => selectedCategories.includes(i.category))
        .map((item) => (
          <div
            key={item.id}
            className={`item-card ${
              item.id === selectedId ? "active" : ""
            }`}
            onClick={() => onSelect(item.id)}
          >
            <div className={`badge ${item.category}`}>
              {item.category}
            </div>
            <div className="item-name">{item.name}</div>
            <div className="item-timestamp">
              {new Date(item.timestamp).toLocaleString()}
            </div>
          </div>
        ))}
    </div>
  );
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

  const markerRefs = useRef({});

  useEffect(() => {
    getLostItems()
      .then(setItems)
      .catch((err) => console.error(err));
  }, []);

  useEffect(() => {
    if (selectedId && markerRefs.current[selectedId]) {
      markerRefs.current[selectedId].openPopup();
    }
  }, [selectedId]);

  const toggleCategory = (cat) => {
    setSelectedCategories((prev) =>
      prev.includes(cat)
        ? prev.filter((c) => c !== cat)
        : [...prev, cat]
    );
  };

  const selectedItem = items.find((i) => i.id === selectedId);

  return (
    <div className="app-container">
      <Sidebar
        items={items}
        selectedId={selectedId}
        onSelect={setSelectedId}
        categories={categories}
        selectedCategories={selectedCategories}
        toggleCategory={toggleCategory}
      />

      <div className="map-wrapper">
        <MapContainer
          center={center}
          zoom={16}
          minZoom={16}
          maxZoom={18}
          maxBounds={bounds}
          style={{ width: "100%", height: "100%" }}
        >
          <TileLayer url={TILE_URL} attribution={TILE_ATTR} />

          <MarkerClusterGroup
            maxClusterRadius={50}
            iconCreateFunction={(cluster) => {
              const markers = cluster.getAllChildMarkers();
              const counts = {};

              markers.forEach((m) => {
                const cat =
                  m.options.icon.options.category || "other";
                counts[cat] = (counts[cat] || 0) + 1;
              });

              const dots = Object.entries(counts)
                .map(
                  ([cat, count]) =>
                    `<span class="cluster-dot ${cat}" title="${cat}: ${count}"></span>`
                )
                .join("");

              return L.divIcon({
                html: `<div class="custom-cluster">
                         ${dots}
                         <div class="cluster-count">
                           ${cluster.getChildCount()}
                         </div>
                       </div>`,
                className: "my-custom-cluster",
                iconSize: [40, 40],
              });
            }}
          >
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
                    <br />
                    {item.category}
                    <br />
                    {new Date(item.timestamp).toLocaleString()}
                  </Popup>
                </Marker>
              ))}
          </MarkerClusterGroup>

          <AddMarker
            bounds={bounds}
            onAdd={(item) =>
              setItems((prev) => [...prev, item])
            }
          />

          {selectedItem && <FlyToMarker item={selectedItem} />}
        </MapContainer>
      </div>
    </div>
  );
}
