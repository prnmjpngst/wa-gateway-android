const BASE = 'http://localhost:8888'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  })
  if (!res.ok) {
    const err = await res.text()
    throw new Error(err || `HTTP ${res.status}`)
  }
  return res.json()
}

export interface AppConfig {
  saKeyConfigured: boolean
  sheetId: string
  sheetTab: string
  messageTemplate: string
  typingEnabled: boolean
  typingMinDelay: number
  typingMaxDelay: number
  typingPauseFreq: number
  typingPauseDuration: number
  presendDelay: number
  batchDelayMin: number
  batchDelayMax: number
  maxTargetsPerBatch: number
  stopOnError: boolean
  maxRetries: number
  autoSyncHours: number
  lastSyncTimestamp: number | null
  serverPort: number
}

export interface SendStatus {
  state: 'idle' | 'running' | 'paused'
  currentTarget: string
  progress: number
  total: number
}

export interface HistoryRecord {
  id: number
  nomorHp: string
  namaPemilik: string
  status: string
  errorMessage: string | null
  timestamp: number
  attemptNumber: number
}

export interface LogEntry {
  level: 'INFO' | 'WARNING' | 'ERROR'
  message: string
  timestamp: number
}

export const api = {
  getConfig: () => request<AppConfig>('/api/config'),
  saveSaKey: (saKey: string) => request('/api/config/sa-key', { method: 'POST', body: JSON.stringify({ saKey }) }),
  saveSheet: (sheetId: string, sheetTab: string) =>
    request('/api/config/sheet', { method: 'POST', body: JSON.stringify({ sheetId, sheetTab }) }),
  saveMessageTemplate: (template: string) =>
    request('/api/config/message-template', { method: 'POST', body: JSON.stringify({ template }) }),
  saveTypingBehavior: (data: Partial<AppConfig>) =>
    request('/api/config/typing-behavior', { method: 'POST', body: JSON.stringify(data) }),
  saveSchedule: (data: { enabled: boolean; daysOfWeek: string; timeOfDay: string }) =>
    request('/api/config/schedule', { method: 'POST', body: JSON.stringify(data) }),
  getTargets: () => request<{ count: number; lastSyncTimestamp: number }>('/api/targets'),
  syncTargets: () => request<{ success: boolean; count?: number; error?: string }>('/api/targets/sync', { method: 'POST' }),
  sendNow: () => request<{ success: boolean; targets?: number; error?: string }>('/api/send/now', { method: 'POST' }),
  getSendStatus: () => request<SendStatus>('/api/send/status'),
  getHistory: (page: number = 1, status?: string) => {
    const params = new URLSearchParams({ page: String(page) })
    if (status) params.set('status', status)
    return request<{ records: HistoryRecord[]; total: number; page: number }>(`/api/history?${params}`)
  },
  deleteHistory: (id: number) => request(`/api/history/${id}`, { method: 'DELETE' }),
  getLogs: () => request<{ logs: LogEntry[] }>('/api/logs'),
  testSheets: () => request<{ success: boolean; columns?: string[]; error?: string }>('/api/sheets/test')
}
