import { useState } from "react";
import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from "react-leaflet";
import "leaflet/dist/leaflet.css";


function AddMarker({ markers, setMarkers, bounds }) {
  useMapEvents({
    click(e) {
      const { lat, lng } = e.latlng;

      const [[south, west], [north, east]] = bounds;

      if (lat < south || lat > north || lng < west || lng > east) return;

      const newMarker = {
        id: Date.now(),
        position: [lat, lng],
        title: `Marker ${markers.length + 1}`,
      };

      setMarkers([...markers, newMarker]);
    },
  });

  return null;
}

export default function Map() {
  const center = [35.702831, 51.351600];
  const delta = 0.0045;

  const bounds = [
    [center[0] - delta, center[1] - delta],
    [center[0] + delta, center[1] + delta],
  ];

  const [markers, setMarkers] = useState([
    { id: 1, position: center, title: "University" },
  ]);

  return (
    <div style={{ width: "90vw", height: "90vh", margin: "auto" }}>
      <MapContainer
        center={center}
        zoom={17}                  
        minZoom={17}               
        maxZoom={18}              
        maxBounds={bounds}         
        //maxBoundsViscosity={1.0}   
        style={{ width: "100%", height: "100%" }}
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution="&copy; OpenStreetMap contributors"
        />

        {markers.map((marker) => (
          <Marker key={marker.id} position={marker.position}>
            <Popup>{marker.title}</Popup>
          </Marker>
        ))}

        <AddMarker markers={markers} setMarkers={setMarkers} bounds={bounds} />
      </MapContainer>
    </div>
  );
}
