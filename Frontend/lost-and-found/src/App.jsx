import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'
import { Routes  , Route} from 'react-router-dom'
import Map from './Components/Map';
function App() {
  const [count, setCount] = useState(0)

  return (
    <Routes>
      <Route path="/map" element={<Map />} />
    </Routes>
  )
}

export default App
