import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'
import { Routes  , Route} from 'react-router-dom'
import Map from './Components/Map';
import Mapv2 from './Components/Mapv2';
function App() {
  const [count, setCount] = useState(0)

  return (
    <Routes>
      <Route path="/map" element={<Map />} />
      <Route path="/map-2" element={<Mapv2 />} />
    </Routes>
  )
}

export default App
