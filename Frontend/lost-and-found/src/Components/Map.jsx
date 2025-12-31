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

function getLostItems() {
  return Promise.resolve([
    {
      id: 1,
      name: "لپتاپ",
      x: 35.702831,
      y: 51.3516,
      timestamp: "2025-01-01T09:30:00",
      category: "electronics",
    },
    {
      id: 4,
      name: "موبایل",
      x: 35.70231,
      y: 51.3516,
      timestamp: "2025-01-01T09:30:00",
      category: "electronics",
    },
    {
      id: 2,
      name: "جزوه",
      x: 35.7041,
      y: 51.3499,
      timestamp: "2025-01-02T14:10:00",
      category: "documents",
    },
    {
      id: 3,
      name: "شلوار جین",
      x: 35.7032,
      y: 51.3504,
      timestamp: "2025-01-03T18:20:00",
      category: "clothing",
    },
  ]);
}


function markerIcon(category) {
  return L.divIcon({
    className: "",
    html: `<div class="custom-marker marker-${category}"></div>`,
    iconSize: [14, 14],
    iconAnchor: [7, 7],
    category, // for cluster icon
  });
}

/* ---------- add marker ---------- */
function AddMarker({ setItems, bounds }) {
  useMapEvents({
    click(e) {
      const { lat, lng } = e.latlng;
      const [[south, west], [north, east]] = bounds;

      if (lat < south || lat > north || lng < west || lng > east) return;

      setItems((prev) => [
        ...prev,
        {
          id: Date.now(),
          x: lat,
          y: lng,
          timestamp: new Date().toISOString(),
          category: "other",
          name: "Item",
        },
      ]);
    },
  });
  return null;
}


function FlyToMarker({ item }) {
  const map = useMap();
  useEffect(() => {
    if (item) map.flyTo([item.x, item.y], 19, { duration: 0.8 });
  }, [item, map]);
  return null;
}

/* ---------- sidebar ---------- */
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
        <strong>Filter by Category:</strong>
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
        .filter((item) => selectedCategories.includes(item.category))
        .map((item) => (
          <div
            key={item.id}
            className={`item-card ${item.id === selectedId ? "active" : ""}`}
            onClick={() => onSelect(item.id)}
          >
            <div className={`badge ${item.category}`}>{item.category}</div>
            <div className="item-name">{item.name}</div>
            <div className="item-timestamp">
              {new Date(item.timestamp).toLocaleString()}
            </div>
          </div>
        ))}
    </div>
  );
}

/* ---------- main ---------- */
export default function LostAndFoundMap() {
  const center = [35.702831, 51.3516];
  const delta = 0.0045;

  const bounds = [
    [center[0] - delta, center[1] - delta],
    [center[0] + delta, center[1] + delta],
  ];

  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const markerRefs = useRef({});
  const categories = ["electronics", "documents", "clothing", "other"];
  const [selectedCategories, setSelectedCategories] = useState([...categories]);

  useEffect(() => {
    getLostItems().then(setItems);
  }, []);

  useEffect(() => {
    if (selectedId && markerRefs.current[selectedId]) {
      markerRefs.current[selectedId].openPopup();
    }
  }, [selectedId]);

  const toggleCategory = (category) => {
    setSelectedCategories((prev) =>
      prev.includes(category)
        ? prev.filter((c) => c !== category)
        : [...prev, category]
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
        <TileLayer
  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
  attribution='&copy; OpenStreetMap contributors'
/>
  






          <MarkerClusterGroup
            spiderfyOnMaxZoom={true}
            showCoverageOnHover={true}
            maxClusterRadius={50}
            iconCreateFunction={(cluster) => {
              const markers = cluster.getAllChildMarkers();
              const categoryCounts = markers.reduce((acc, m) => {
                const cat = m.options.icon.options.category || "other";
                acc[cat] = (acc[cat] || 0) + 1;
                return acc;
              }, {});

              const dots = Object.entries(categoryCounts)
                .map(
                  ([cat, count]) =>
                    `<span class="cluster-dot ${cat}" title="${cat}: ${count}"></span>`
                )
                .join("");

              return L.divIcon({
                html: `<div class="custom-cluster">${dots}<div class="cluster-count">${cluster.getChildCount()}</div></div>`,
                className: "my-custom-cluster",
                iconSize: L.point(40, 40),
              });
            }}
          >
            {items
              .filter((item) => selectedCategories.includes(item.category))
              .map((item) => (
                <Marker
                  key={item.id}
                  position={[item.x, item.y]}
                  icon={markerIcon(item.category)}
                  ref={(ref) => (markerRefs.current[item.id] = ref)}
                >
                  <Popup>
                    <b>{item.name}</b>
                    <br />
                    Category: {item.category}
                    <br />
                    {new Date(item.timestamp).toLocaleString()}
                  </Popup>
                </Marker>
              ))}
          </MarkerClusterGroup>

          <AddMarker setItems={setItems} bounds={bounds} />
          {selectedItem && <FlyToMarker item={selectedItem} />}
        </MapContainer>
      </div>
    </div>
  );
}
