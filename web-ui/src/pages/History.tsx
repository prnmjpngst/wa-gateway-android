import { useEffect, useState } from 'react'
import { api, type HistoryRecord } from '../api'

export default function History() {
  const [records, setRecords] = useState<HistoryRecord[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [filterStatus, setFilterStatus] = useState('')
  const [msg, setMsg] = useState('')
  const pageSize = 20

  const load = async (p: number) => {
    try {
      const res = filterStatus ? await api.getHistory(p, filterStatus) : await api.getHistory(p)
      setRecords(res.records)
      setTotal(res.total)
      setPage(res.page)
    } catch (e: any) { setMsg(e.message) }
  }

  useEffect(() => { load(1) }, [filterStatus])

  const handleDelete = async (id: number) => {
    try { await api.deleteHistory(id); load(page) }
    catch (e: any) { setMsg(e.message) }
  }

  const totalPages = Math.ceil(total / pageSize)

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>Send History</h2>

      <div className="card">
        <div className="flex mb-4">
          <label style={{ margin: 0 }}>Filter by status:</label>
          <select value={filterStatus} onChange={e => setFilterStatus(e.target.value)}
            style={{ width: 200, margin: 0 }}>
            <option value="">All</option>
            <option value="sent">Sent</option>
            <option value="invalid_number">Invalid Number</option>
            <option value="failed">Failed</option>
            <option value="error">Error</option>
          </select>
          <span className="text-sm">Total: {total}</span>
        </div>

        {records.length === 0 ? <p className="text-sm">No records found</p> : (
          <table>
            <thead>
              <tr>
                <th>Time</th><th>Phone</th><th>Name</th><th>Status</th><th>Error</th><th>Attempt</th><th></th>
              </tr>
            </thead>
            <tbody>
              {records.map(r => (
                <tr key={r.id}>
                  <td>{new Date(r.timestamp).toLocaleString('id-ID')}</td>
                  <td>{r.nomorHp}</td>
                  <td>{r.namaPemilik || '-'}</td>
                  <td><span className={`status-badge ${r.status}`}>{r.status}</span></td>
                  <td className="text-sm">{r.errorMessage || '-'}</td>
                  <td>{r.attemptNumber}</td>
                  <td><button className="danger" style={{ padding: '4px 8px', fontSize: 12 }}
                    onClick={() => handleDelete(r.id)}>Delete</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {totalPages > 1 && (
          <div className="flex mt-4" style={{ justifyContent: 'center' }}>
            <button disabled={page <= 1} onClick={() => load(page - 1)}>Prev</button>
            <span className="text-sm">{page} / {totalPages}</span>
            <button disabled={page >= totalPages} onClick={() => load(page + 1)}>Next</button>
          </div>
        )}
      </div>

      {msg && <div className="card"><p>{msg}</p></div>}
    </div>
  )
}
