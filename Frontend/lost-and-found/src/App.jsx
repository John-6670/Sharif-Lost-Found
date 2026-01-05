import './App.css'
import { Routes, Route } from 'react-router-dom'
import Map from './Components/Map';
import Mapv2 from './Components/Mapv2';
import Signup from './pages/Signup/Signup';
import Login from './pages/Login/Login';
import ForgotPassword from './pages/ForgotPassword/ForgotPassword';
import ResetPassword from './pages/ResetPassword/ResetPassword';
import Home from './pages/Home/Home';
import Terms from './pages/Terms/Terms';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/home" element={<Home />} />
      <Route path="/map" element={<Map />} />
      <Route path="/map-2" element={<Mapv2 />} />
      <Route path="/signup" element={<Signup />} />
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/terms" element={<Terms />} />
    </Routes>
  )
}

export default App
