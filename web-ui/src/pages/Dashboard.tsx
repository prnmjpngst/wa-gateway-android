import { useEffect, useState } from 'react'
import { api, type AppConfig, type SendStatus, type HistoryRecord } from '../api'

export default function Dashboard() {
  const [config, setConfig] = useState<AppConfig | null>(null)
  const [status, setStatus] = useState<SendStatus | null>(null)
  const [recent, setRecent] = useState<HistoryRecord[]>([])
  const [targetCount, setTargetCount] = useState(0)
  const [msg, setMsg] = useState('')

  const load = async () => {
    try {
      const [c, s, h, t] = await Promise.all([
        api.getConfig(), api.getSendStatus(),
        api.getHistory(1), api.getTargets()
      ])
      setConfig(c); setStatus(s)
      setRecent(h.records.slice(0, 5))
      setTargetCount(t.count)
    } catch { setMsg('Failed to load dashboard') }
  }

  useEffect(() => { load(); const id = setInterval(load, 3000); return () => clearInterval(id) }, [])

  const handleSendNow = async () => {
    try {
      const res = await api.sendNow()
      setMsg(res.success ? `Sending to ${res.targets} targets...` : res.error || '')
    } catch (e: any) { setMsg(e.message) }
  }

  const handleSync = async () => {
    try {
      const res = await api.syncTargets()
      if (res.success) { setMsg(`Synced: ${res.count} targets`); load() }
      else setMsg(res.error || 'Sync failed')
    } catch (e: any) { setMsg(e.message) }
  }

  const lastSync = config?.lastSyncTimestamp
    ? new Date(config.lastSyncTimestamp).toLocaleString('id-ID')
    : 'Never'

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>Dashboard</h2>

      <div className="flex flex-wrap" style={{ gap: 16 }}>
        <div className="card" style={{ flex: 1, minWidth: 200 }}>
          <h2>Targets</h2>
          <p style={{ fontSize: 32, fontWeight: 700 }}>{targetCount}</p>
          <p className="text-sm">cached targets</p>
          <p className="text-sm">Last sync: {lastSync}</p>
        </div>

        <div className="card" style={{ flex: 1, minWidth: 200 }}>
          <h2>Status</h2>
          <p className={`text-${status?.state === 'running' ? 'green' : status?.state === 'paused' ? 'yellow' : ''}`}
             style={{ fontSize: 18, fontWeight: 600 }}>
            {status?.state || 'idle'}
          </p>
          {status?.state === 'running' && (
            <p className="text-sm">{status.progress}/{status.total} - {status.currentTarget}</p>
          )}
        </div>

        <div className="card" style={{ flex: 1, minWidth: 200 }}>
          <h2>Actions</h2>
          <div className="flex" style={{ marginTop: 8 }}>
            <button className="success" onClick={handleSync}>Sync Now</button>
            <button onClick={handleSendNow} disabled={status?.state === 'running'}>
              {status?.state === 'running' ? 'Running...' : 'Send Now'}
            </button>
          </div>
        </div>
      </div>

      {msg && <div className="card"><p>{msg}</p></div>}

      <div className="card">
        <h2>Recent Activity</h2>
        {recent.length === 0 ? <p className="text-sm">No activity yet</p> : (
          <table>
            <thead><tr><th>Time</th><th>Phone</th><th>Status</th><th>Error</th></tr></thead>
            <tbody>
              {recent.map(r => (
                <tr key={r.id}>
                  <td>{new Date(r.timestamp).toLocaleString('id-ID')}</td>
                  <td>{r.nomorHp}</td>
                  <td><span className={`status-badge ${r.status}`}>{r.status}</span></td>
                  <td className="text-sm">{r.errorMessage || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
