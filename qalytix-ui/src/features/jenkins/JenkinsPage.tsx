import { useState, useEffect } from 'react'
import { Plus, Trash2, RefreshCw, Wifi, WifiOff, Pencil } from 'lucide-react'
import {
  getJenkinsConfigs,
  createJenkinsConfig,
  updateJenkinsConfig,
  deleteJenkinsConfig,
  testJenkinsConnection,
  syncJenkins,
} from '../../api/jenkins'
import type { JenkinsConfig, CreateJenkinsConfigPayload } from '../../types/jenkins'

const DEFAULT_FORM = { name: '', url: '', username: '', apiToken: '', pollInterval: '*/5 * * * *' }

export default function JenkinsPage() {
  const [configs, setConfigs]     = useState<JenkinsConfig[]>([])
  const [loading, setLoading]     = useState(true)
  const [showForm, setShowForm]   = useState(false)
  const [editing, setEditing]     = useState<JenkinsConfig | null>(null)
  const [form, setForm]           = useState(DEFAULT_FORM)
  const [saving, setSaving]       = useState(false)
  const [error, setError]         = useState('')
  const [testingId, setTestingId] = useState<number | null>(null)
  const [syncingId, setSyncingId] = useState<number | null>(null)
  const [feedback, setFeedback]   = useState<{ id: number; msg: string; ok: boolean } | null>(null)

  useEffect(() => { load() }, [])

  async function load() {
    try {
      const { data } = await getJenkinsConfigs()
      setConfigs(data.data)
    } finally {
      setLoading(false)
    }
  }

  function openCreate() { setEditing(null); setForm(DEFAULT_FORM); setError(''); setShowForm(true) }
  function openEdit(c: JenkinsConfig) {
    setEditing(c)
    setForm({ name: c.name, url: c.url, username: c.username, apiToken: '', pollInterval: c.pollInterval })
    setError('')
    setShowForm(true)
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      if (editing) {
        // Omit apiToken entirely when left blank — backend keeps the existing token
        const { apiToken, ...rest } = form
        const payload = apiToken.trim() ? { ...rest, apiToken } : rest
        const { data } = await updateJenkinsConfig(editing.id, payload)
        setConfigs(cs => cs.map(c => c.id === editing.id ? data.data : c))
      } else {
        const { data } = await createJenkinsConfig(form as CreateJenkinsConfigPayload)
        setConfigs(cs => [...cs, data.data])
      }
      setShowForm(false)
    } catch (err: any) {
      setError(err.response?.data?.detail ?? 'Failed to save.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this Jenkins config?')) return
    await deleteJenkinsConfig(id)
    setConfigs(cs => cs.filter(c => c.id !== id))
  }

  async function handleTest(id: number) {
    setTestingId(id)
    try {
      await testJenkinsConnection(id)
      setFeedback({ id, msg: 'Connection successful', ok: true })
    } catch (err: any) {
      const msg = err.response?.data?.detail ?? err.response?.data?.message ?? 'Connection failed'
      setFeedback({ id, msg, ok: false })
    } finally {
      setTestingId(null)
      setTimeout(() => setFeedback(null), 6000)
    }
  }

  async function handleSync(id: number) {
    setSyncingId(id)
    try {
      await syncJenkins(id)
      setFeedback({ id, msg: 'Sync triggered', ok: true })
      await load()
    } catch (err: any) {
      const msg = err.response?.data?.detail ?? err.response?.data?.message ?? 'Sync failed'
      setFeedback({ id, msg, ok: false })
    } finally {
      setSyncingId(null)
      setTimeout(() => setFeedback(null), 6000)
    }
  }

  const field = (key: keyof typeof form) => ({
    value: form[key],
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [key]: e.target.value }),
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Jenkins</h1>
          <p className="text-sm text-slate-500 mt-0.5">Manage your Jenkins server connections</p>
        </div>
        <button onClick={openCreate} className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg transition-colors">
          <Plus className="w-4 h-4" /> Add Server
        </button>
      </div>

      {showForm && (
        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h2 className="text-base font-semibold text-slate-900 mb-4">
            {editing ? 'Edit Jenkins Server' : 'Add Jenkins Server'}
          </h2>
          {error && <div className="mb-4 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">{error}</div>}
          <form onSubmit={handleSubmit} className="grid grid-cols-2 gap-4">
            {([
              ['name', 'Name', 'text', 'My Jenkins'],
              ['url', 'Jenkins URL', 'url', 'http://localhost:8080'],
              ['username', 'Username', 'text', 'admin'],
              ['apiToken', editing ? 'API Token (leave blank to keep)' : 'API Token', 'password', ''],
            ] as const).map(([key, label, type, placeholder]) => (
              <div key={key}>
                <label className="block text-sm font-medium text-slate-700 mb-1">{label}</label>
                <input
                  type={type}
                  required={key !== 'apiToken' || !editing}
                  placeholder={key === 'apiToken' && editing ? 'Leave blank to keep existing token' : placeholder}
                  autoComplete={key === 'apiToken' ? 'new-password' : 'off'}
                  {...field(key)}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                {key === 'username' && (
                  <p className="mt-1 text-xs text-slate-400">Your Jenkins login ID — check <span className="font-mono">Jenkins → your avatar → Configure</span> for the exact value.</p>
                )}
                {key === 'apiToken' && editing && (
                  <p className="mt-1 text-xs text-slate-400">Token is saved securely — only enter a new value to replace it.</p>
                )}
              </div>
            ))}
            <div className="col-span-2">
              <label className="block text-sm font-medium text-slate-700 mb-1">Poll Interval</label>
              <input
                type="text"
                required
                placeholder="*/5 * * * *"
                {...field('pollInterval')}
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono"
              />
              <p className="mt-1.5 text-xs text-slate-500">
                Cron expression: <span className="font-mono text-slate-600">minute  hour  day  month  weekday</span>
              </p>
              <div className="mt-1 grid grid-cols-3 gap-x-4 gap-y-0.5 text-xs text-slate-400">
                {[
                  ['*/5 * * * *', 'Every 5 minutes'],
                  ['*/15 * * * *', 'Every 15 minutes'],
                  ['0 * * * *', 'Every hour'],
                  ['0 */2 * * *', 'Every 2 hours'],
                  ['0 8-18 * * 1-5', 'Hourly, business hours'],
                  ['H/5 * * * *', 'Every 5 min (Jenkins hash)'],
                ].map(([expr, desc]) => (
                  <button
                    key={expr}
                    type="button"
                    onClick={() => setForm(f => ({ ...f, pollInterval: expr }))}
                    className="text-left hover:text-indigo-600 transition-colors py-0.5"
                  >
                    <span className="font-mono text-slate-500">{expr}</span>
                    <span className="ml-1 text-slate-400">— {desc}</span>
                  </button>
                ))}
              </div>
            </div>
            <div className="col-span-2 flex gap-3 justify-end">
              <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 text-sm text-slate-600 border border-slate-300 rounded-lg hover:bg-slate-50">Cancel</button>
              <button type="submit" disabled={saving} className="px-4 py-2 text-sm bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-medium rounded-lg transition-colors">
                {saving ? 'Saving…' : editing ? 'Update' : 'Add Server'}
              </button>
            </div>
          </form>
        </div>
      )}

      {loading ? (
        <div className="text-sm text-slate-500">Loading…</div>
      ) : configs.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <p className="text-slate-500 text-sm">No Jenkins servers configured yet.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {configs.map(c => (
            <div key={c.id} className="bg-white rounded-xl border border-slate-200 p-5 flex items-center justify-between gap-4">
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-slate-900 text-sm">{c.name}</span>
                  <span className={`text-xs px-1.5 py-0.5 rounded ${c.active ? 'bg-green-100 text-green-700' : 'bg-slate-100 text-slate-500'}`}>
                    {c.active ? 'Active' : 'Inactive'}
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-0.5 truncate">{c.url}</p>
                {c.lastSyncedAt && (
                  <p className="text-xs text-slate-400 mt-0.5">
                    Last synced {new Date(c.lastSyncedAt).toLocaleString()}
                  </p>
                )}
                {feedback?.id === c.id && (
                  <p className={`text-xs mt-1 font-medium ${feedback.ok ? 'text-green-600' : 'text-red-600'}`}>{feedback.msg}</p>
                )}
              </div>
              <div className="flex items-center gap-2 flex-shrink-0">
                <button onClick={() => handleTest(c.id)} disabled={testingId === c.id} title="Test connection" className="p-2 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors">
                  {testingId === c.id ? <WifiOff className="w-4 h-4 animate-pulse" /> : <Wifi className="w-4 h-4" />}
                </button>
                <button onClick={() => handleSync(c.id)} disabled={syncingId === c.id} title="Sync now" className="p-2 text-slate-400 hover:text-green-600 hover:bg-green-50 rounded-lg transition-colors">
                  <RefreshCw className={`w-4 h-4 ${syncingId === c.id ? 'animate-spin' : ''}`} />
                </button>
                <button onClick={() => openEdit(c)} title="Edit" className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors">
                  <Pencil className="w-4 h-4" />
                </button>
                <button onClick={() => handleDelete(c.id)} title="Delete" className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors">
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
