import { useEffect, useState, useRef } from 'react'
import { api, type LogEntry } from '../api'

export default function Logs() {
  const [logs, setLogs] = useState<LogEntry[]>([])
  const containerRef = useRef<HTMLDivElement>(null)
  const wsRef = useRef<WebSocket | null>(null)

  useEffect(() => {
    api.getLogs().then(res => setLogs(res.logs)).catch(() => {})

    const ws = new WebSocket('ws://localhost:8888/ws/logs')
    ws.onmessage = (e) => {
      try {
        const entry: LogEntry = JSON.parse(e.data)
        setLogs(prev => [...prev.slice(-500), entry])
      } catch {}
    }
    ws.onerror = () => {}
    wsRef.current = ws

    return () => ws.close()
  }, [])

  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight
    }
  }, [logs])

  const handleClear = () => setLogs([])

  return (
    <div>
      <div className="flex mb-4" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ margin: 0 }}>Real-time Logs</h2>
        <button onClick={handleClear} className="danger">Clear</button>
      </div>

      <div className="log-container" ref={containerRef}>
        {logs.length === 0 ? (
          <p className="text-sm">No logs yet</p>
        ) : (
          logs.map((entry, i) => (
            <div key={i} className={`log-entry ${entry.level}`}>
              <span className="text-sm">{new Date(entry.timestamp).toLocaleTimeString('id-ID')}</span>
              {' '}[{entry.level}] {entry.message}
            </div>
          ))
        )}
      </div>
    </div>
  )
}
