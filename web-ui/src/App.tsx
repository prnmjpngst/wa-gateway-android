import { Routes, Route, NavLink } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import GoogleSheetsConfig from './pages/GoogleSheetsConfig'
import Settings from './pages/Settings'
import History from './pages/History'
import Logs from './pages/Logs'

export default function App() {
  return (
    <div className="app">
      <div className="sidebar">
        <h1>WA Gateway</h1>
        <nav>
          <NavLink to="/" end>Dashboard</NavLink>
          <NavLink to="/sheets">Google Sheets</NavLink>
          <NavLink to="/settings">Settings</NavLink>
          <NavLink to="/history">History</NavLink>
          <NavLink to="/logs">Logs</NavLink>
        </nav>
      </div>
      <div className="content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/sheets" element={<GoogleSheetsConfig />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="/history" element={<History />} />
          <Route path="/logs" element={<Logs />} />
        </Routes>
      </div>
    </div>
  )
}
