import { useEffect, useState, useRef } from 'react'
import { api } from '../api'

export default function GoogleSheetsConfig() {
  const [sheetId, setSheetId] = useState('')
  const [sheetTab, setSheetTab] = useState('Sheet1')
  const [saKeyConfigured, setSaKeyConfigured] = useState(false)
  const [saKeyText, setSaKeyText] = useState('')
  const [saKeyFile, setSaKeyFile] = useState<File | null>(null)
  const [testing, setTesting] = useState(false)
  const [syncing, setSyncing] = useState(false)
  const [connectionStatus, setConnectionStatus] = useState<string | null>(null)
  const [columns, setColumns] = useState<string[]>([])
  const [msg, setMsg] = useState('')
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    api.getConfig().then(c => {
      setSaKeyConfigured(c.saKeyConfigured)
      setSheetId(c.sheetId)
      setSheetTab(c.sheetTab)
    }).catch(() => setMsg('Failed to load config'))
  }, [])

  const handleUploadSaKey = async () => {
    try {
      let json: string
      if (saKeyFile) {
        json = await saKeyFile.text()
      } else if (saKeyText.trim()) {
        JSON.parse(saKeyText.trim())
        json = saKeyText.trim()
      } else {
        setMsg('Select a file or paste SA key JSON')
        return
      }
      await api.saveSaKey(json)
      setSaKeyConfigured(true)
      setMsg('SA key saved successfully')
    } catch (e: any) {
      setMsg('Invalid JSON: ' + e.message)
    }
  }

  const handleSaveSheet = async () => {
    try {
      await api.saveSheet(sheetId, sheetTab)
      setMsg('Sheet configuration saved')
    } catch (e: any) { setMsg(e.message) }
  }

  const handleTest = async () => {
    setTesting(true)
    setConnectionStatus(null)
    setColumns([])
    try {
      const res = await api.testSheets()
      if (res.success) {
        setConnectionStatus('connected')
        setColumns(res.columns || [])
        setMsg('Connected! Columns found: ' + (res.columns || []).join(', '))
      } else {
        setConnectionStatus('error')
        setMsg(res.error || 'Connection failed')
      }
    } catch (e: any) {
      setConnectionStatus('error')
      setMsg(e.message)
    } finally { setTesting(false) }
  }

  const handleSync = async () => {
    setSyncing(true)
    try {
      const res = await api.syncTargets()
      setMsg(res.success ? `Synced ${res.count} targets` : res.error || 'Sync failed')
    } catch (e: any) { setMsg(e.message) }
    finally { setSyncing(false) }
  }

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>Google Sheets Connection</h2>

      <div className="card">
        <h2>Service Account Key</h2>
        <p className="text-sm mb-4">
          Status: {saKeyConfigured ? <span className="text-green">✓ Configured</span> : <span className="text-red">Not configured</span>}
        </p>

        <label>Upload JSON file</label>
        <input type="file" accept=".json" ref={fileInputRef}
          onChange={e => {
            setSaKeyFile(e.target.files?.[0] || null)
            setSaKeyText('')
          }} />

        <p style={{ textAlign: 'center', margin: '8px 0', color: '#64748b' }}>— OR —</p>

        <label>Paste SA key JSON</label>
        <textarea value={saKeyText} onChange={e => { setSaKeyText(e.target.value); setSaKeyFile(null) }}
          placeholder='Paste your service account key JSON here...' />

        <button onClick={handleUploadSaKey}>Save SA Key</button>
      </div>

      <div className="card">
        <h2>Sheet Configuration</h2>

        <label>Sheet ID (from URL)</label>
        <input value={sheetId} onChange={e => setSheetId(e.target.value)}
          placeholder="1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms" />

        <label>Sheet Tab Name</label>
        <input value={sheetTab} onChange={e => setSheetTab(e.target.value)} placeholder="Sheet1" />

        <div className="flex">
          <button onClick={handleSaveSheet}>Save</button>
          <button onClick={handleTest} disabled={testing || !sheetId} className="warning">
            {testing ? 'Testing...' : 'Test Connection'}
          </button>
          <button onClick={handleSync} disabled={syncing || !sheetId} className="success">
            {syncing ? 'Syncing...' : 'Sync Now'}
          </button>
        </div>

        {connectionStatus === 'connected' && columns.length > 0 && (
          <div className="mt-4">
            <p className="text-green">✓ Connected</p>
            <p className="text-sm">Columns found: {columns.join(', ')}</p>
          </div>
        )}
      </div>

      {msg && <div className="card"><p>{msg}</p></div>}
    </div>
  )
}
