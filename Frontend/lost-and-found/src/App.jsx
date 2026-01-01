import './App.css'
import { Routes, Route } from 'react-router-dom'
import Map from './Components/Map';
import Mapv2 from './Components/Mapv2';
import Signup from './pages/Signup/Signup';

function App() {
  return (
    <Routes>
      <Route path="/map" element={<Map />} />
      <Route path="/map-2" element={<Mapv2 />} />
      <Route path="/signup" element={<Signup />} />
      <Route path="/login" element={<LoginPlaceholder />} />
    </Routes>
  )
}

// Placeholder for login page - to be implemented
function LoginPlaceholder() {
  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      direction: 'rtl',
      fontFamily: 'Vazirmatn, Inter, system-ui, sans-serif'
    }}>
      <div style={{
        background: 'white',
        padding: '40px',
        borderRadius: '20px',
        textAlign: 'center',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)'
      }}>
        <h1 style={{ marginBottom: '16px', color: '#111827' }}>صفحه ورود</h1>
        <p style={{ color: '#6b7280' }}>این صفحه به زودی پیاده‌سازی می‌شود</p>
        <a href="/signup" style={{
          display: 'inline-block',
          marginTop: '20px',
          color: '#667eea',
          fontWeight: '600',
          textDecoration: 'none'
        }}>ثبت‌نام کنید</a>
      </div>
    </div>
  );
}

export default App
