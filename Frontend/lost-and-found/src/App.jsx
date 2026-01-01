import './App.css'
import { Routes, Route } from 'react-router-dom'
import Map from './Components/Map';
import Mapv2 from './Components/Mapv2';
import Signup from './pages/Signup/Signup';
import Login from './pages/Login/Login';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Map />} />
      <Route path="/map" element={<Map />} />
      <Route path="/map-2" element={<Mapv2 />} />
      <Route path="/signup" element={<Signup />} />
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPasswordPlaceholder />} />
    </Routes>
  )
}

// Placeholder for forgot password page - to be implemented
function ForgotPasswordPlaceholder() {
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
        maxWidth: '400px',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)'
      }}>
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>📧</div>
        <h1 style={{ marginBottom: '16px', color: '#111827', fontSize: '24px' }}>بازیابی رمز عبور</h1>
        <p style={{ color: '#6b7280', marginBottom: '24px' }}>این صفحه به زودی پیاده‌سازی می‌شود</p>
        <a href="/login" style={{
          display: 'inline-block',
          padding: '12px 24px',
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          color: 'white',
          borderRadius: '10px',
          fontWeight: '600',
          textDecoration: 'none'
        }}>بازگشت به صفحه ورود</a>
      </div>
    </div>
  );
}

export default App
