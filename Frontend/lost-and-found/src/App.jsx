import './App.css'
import { Routes, Route } from 'react-router-dom'
import Map from './Components/Map';
import Signup from './pages/Signup/Signup';
import Login from './pages/Login/Login';
import ForgotPassword from './pages/ForgotPassword/ForgotPassword';
import ResetPassword from './pages/ResetPassword/ResetPassword';
import Home from './pages/Home/Home';
import Terms from './pages/Terms/Terms';
import VerifyEmail from './pages/VerifyEmail/VerifyEmail';
import Profile from './pages/Profile/Profile';

function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/home" element={<Home />} />
      <Route path="/map" element={<Map />} />
      <Route path="/signup" element={<Signup />} />
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />
      <Route path="/terms" element={<Terms />} />
      <Route path="/verify-email" element={<VerifyEmail />} />
      <Route path="/profile" element={<Profile />} />
    </Routes>
  )
}

export default App
