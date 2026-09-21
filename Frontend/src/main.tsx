import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './dashboard.css'
import './world-view-refresh.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
