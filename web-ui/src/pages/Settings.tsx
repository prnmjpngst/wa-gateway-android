import { useEffect, useState } from 'react'
import { api, type AppConfig } from '../api'

export default function Settings() {
  const [config, setConfig] = useState<AppConfig | null>(null)
  const [template, setTemplate] = useState('')
  const [saving, setSaving] = useState('')
  const [msg, setMsg] = useState('')

  const [scheduleEnabled, setScheduleEnabled] = useState(false)
  const [scheduleTime, setScheduleTime] = useState('08:00')
  const [scheduleDays, setScheduleDays] = useState('1,2,3,4,5')

  useEffect(() => {
    api.getConfig().then(c => {
      setConfig(c)
      setTemplate(c.messageTemplate)
    }).catch(() => setMsg('Failed to load config'))
  }, [])

  const handleSaveTemplate = async () => {
    setSaving('template')
    try { await api.saveMessageTemplate(template); setMsg('Template saved') }
    catch (e: any) { setMsg(e.message) }
    finally { setSaving('') }
  }

  const handleSaveTyping = async (data: Partial<AppConfig>) => {
    setSaving('typing')
    try { await api.saveTypingBehavior(data); setMsg('Typing settings saved') }
    catch (e: any) { setMsg(e.message) }
    finally { setSaving('') }
  }

  const handleSaveSchedule = async () => {
    setSaving('schedule')
    try {
      await api.saveSchedule({ enabled: scheduleEnabled, timeOfDay: scheduleTime, daysOfWeek: scheduleDays })
      setMsg('Schedule saved')
    } catch (e: any) { setMsg(e.message) }
    finally { setSaving('') }
  }

  const updateConfig = (patch: Partial<AppConfig>) => {
    if (!config) return
    setConfig({ ...config, ...patch })
  }

  if (!config) return <p>Loading...</p>

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>Settings</h2>

      <div className="card">
        <h2>Message Template</h2>
        <p className="text-sm mb-4">Placeholders: {'{nama}'}, {'{nomor_kendaraan}'}, {'{masa_berlaku}'}, {'{hitung_hari}'}</p>
        <textarea value={template} onChange={e => setTemplate(e.target.value)} rows={6} />
        <button onClick={handleSaveTemplate} disabled={saving === 'template'}>
          {saving === 'template' ? 'Saving...' : 'Save Template'}
        </button>
      </div>

      <div className="card">
        <h2>Typing Behavior</h2>
        <label>
          <input type="checkbox" checked={config.typingEnabled}
            onChange={e => { updateConfig({ typingEnabled: e.target.checked }); handleSaveTyping({ typingEnabled: e.target.checked }) }} />
          {' '}Enable human-like typing
        </label>

        <label>Min delay per char (ms)</label>
        <input type="number" value={config.typingMinDelay}
          onChange={e => updateConfig({ typingMinDelay: +e.target.value })}
          onBlur={() => handleSaveTyping({ typingMinDelay: config.typingMinDelay })} />

        <label>Max delay per char (ms)</label>
        <input type="number" value={config.typingMaxDelay}
          onChange={e => updateConfig({ typingMaxDelay: +e.target.value })}
          onBlur={() => handleSaveTyping({ typingMaxDelay: config.typingMaxDelay })} />

        <label>Pause every N chars</label>
        <input type="number" value={config.typingPauseFreq}
          onChange={e => updateConfig({ typingPauseFreq: +e.target.value })}
          onBlur={() => handleSaveTyping({ typingPauseFreq: config.typingPauseFreq })} />

        <label>Pause duration (ms)</label>
        <input type="number" value={config.typingPauseDuration}
          onChange={e => updateConfig({ typingPauseDuration: +e.target.value })}
          onBlur={() => handleSaveTyping({ typingPauseDuration: config.typingPauseDuration })} />

        <label>Pre-send hesitation (ms)</label>
        <input type="number" value={config.presendDelay}
          onChange={e => updateConfig({ presendDelay: +e.target.value })}
          onBlur={() => handleSaveTyping({ presendDelay: config.presendDelay })} />
      </div>

      <div className="card">
        <h2>Batch Configuration</h2>

        <label>Min delay between targets (sec)</label>
        <input type="number" value={config.batchDelayMin}
          onChange={e => updateConfig({ batchDelayMin: +e.target.value })}
          onBlur={() => handleSaveTyping({ batchDelayMin: config.batchDelayMin })} />

        <label>Max delay between targets (sec)</label>
        <input type="number" value={config.batchDelayMax}
          onChange={e => updateConfig({ batchDelayMax: +e.target.value })}
          onBlur={() => handleSaveTyping({ batchDelayMax: config.batchDelayMax })} />

        <label>Max targets per batch (0 = unlimited)</label>
        <input type="number" value={config.maxTargetsPerBatch}
          onChange={e => updateConfig({ maxTargetsPerBatch: +e.target.value })}
          onBlur={() => handleSaveTyping({ maxTargetsPerBatch: config.maxTargetsPerBatch })} />

        <label>
          <input type="checkbox" checked={config.stopOnError}
            onChange={e => { updateConfig({ stopOnError: e.target.checked }); handleSaveTyping({ stopOnError: e.target.checked }) }} />
          {' '}Stop on error
        </label>

        <label>Max retries per target</label>
        <input type="number" min={1} max={3} value={config.maxRetries}
          onChange={e => updateConfig({ maxRetries: +e.target.value })}
          onBlur={() => handleSaveTyping({ maxRetries: config.maxRetries })} />
      </div>

      <div className="card">
        <h2>Schedule</h2>

        <label>
          <input type="checkbox" checked={scheduleEnabled}
            onChange={e => setScheduleEnabled(e.target.checked)} />
          {' '}Enable scheduled sending
        </label>

        <label>Time</label>
        <input type="time" value={scheduleTime} onChange={e => setScheduleTime(e.target.value)} />

        <label>Days of week (0=Sun, 1=Mon, ... 6=Sat, comma-separated)</label>
        <input value={scheduleDays} onChange={e => setScheduleDays(e.target.value)} placeholder="1,2,3,4,5" />

        <button onClick={handleSaveSchedule} disabled={saving === 'schedule'}>
          {saving === 'schedule' ? 'Saving...' : 'Save Schedule'}
        </button>
      </div>

      {msg && <div className="card"><p>{msg}</p></div>}
    </div>
  )
}
