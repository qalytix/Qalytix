import { useEffect, useState } from 'react'
import { Bell, Plus, Trash2, Send, Pencil, CheckCircle2, XCircle, Clock } from 'lucide-react'
import {
  getNotificationConfigs, createNotificationConfig, updateNotificationConfig,
  deleteNotificationConfig, testNotificationConfig, getNotificationHistory,
} from '../../api/notifications'
import type { NotificationConfig, NotificationEvent, CreateNotificationConfigRequest } from '../../types/notifications'

// ── helpers ───────────────────────────────────────────────────────────────────

const CHANNEL_LABELS = { TEAMS: 'Microsoft Teams', SLACK: 'Slack' }
const TRIGGER_LABELS: Record<string, string> = {
  BUILD_FAILURE:        'Build Failed',
  CONSECUTIVE_FAILURES: 'Consecutive Failures',
  FLAKY_THRESHOLD:      'Flaky Threshold',
  TEST_SEND:            'Test Send',
}

const EMPTY_FORM: CreateNotificationConfigRequest = {
  name: '', channel: 'TEAMS', webhookUrl: '',
  onBuildFailure: true, onConsecutiveFailures: false, consecutiveThreshold: 3,
  onFlakyThreshold: false, flakyScoreThreshold: 0.5,
}

// ── Config form ───────────────────────────────────────────────────────────────

function ConfigForm({
  initial, onSave, onCancel, saving,
}: {
  initial: CreateNotificationConfigRequest
  onSave: (data: CreateNotificationConfigRequest) => void
  onCancel: () => void
  saving: boolean
}) {
  const [form, setForm] = useState(initial)
  const set = (patch: Partial<CreateNotificationConfigRequest>) => setForm(f => ({ ...f, ...patch }))

  return (
    <div className="bg-slate-50 border border-slate-200 rounded-xl p-5 space-y-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Name</label>
          <input
            value={form.name}
            onChange={e => set({ name: e.target.value })}
            placeholder="e.g. Backend Alerts"
            className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Channel</label>
          <select
            value={form.channel}
            onChange={e => set({ channel: e.target.value as 'TEAMS' | 'SLACK' })}
            className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="TEAMS">Microsoft Teams</option>
            <option value="SLACK">Slack</option>
          </select>
        </div>
      </div>

      <div>
        <label className="block text-xs font-medium text-slate-600 mb-1">Incoming Webhook URL</label>
        <input
          value={form.webhookUrl}
          onChange={e => set({ webhookUrl: e.target.value })}
          placeholder={form.channel === 'TEAMS'
            ? 'https://xxx.webhook.office.com/webhookb2/...'
            : 'https://hooks.slack.com/services/...'}
          className="w-full px-3 py-2 text-sm border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono"
        />
      </div>

      {/* Trigger rules */}
      <div>
        <p className="text-xs font-semibold text-slate-600 uppercase tracking-wider mb-2">Trigger rules</p>
        <div className="space-y-3">
          <label className="flex items-center gap-3 cursor-pointer">
            <input type="checkbox" checked={form.onBuildFailure}
              onChange={e => set({ onBuildFailure: e.target.checked })}
              className="w-4 h-4 rounded accent-indigo-600" />
            <span className="text-sm text-slate-700">Notify on build failure</span>
          </label>

          <div className="flex items-center gap-3">
            <label className="flex items-center gap-3 cursor-pointer">
              <input type="checkbox" checked={form.onConsecutiveFailures}
                onChange={e => set({ onConsecutiveFailures: e.target.checked })}
                className="w-4 h-4 rounded accent-indigo-600" />
              <span className="text-sm text-slate-700">Notify after</span>
            </label>
            <input
              type="number" min={2} max={20}
              value={form.consecutiveThreshold}
              onChange={e => set({ consecutiveThreshold: Number(e.target.value) })}
              disabled={!form.onConsecutiveFailures}
              className="w-16 px-2 py-1 text-sm border border-slate-200 rounded-lg bg-white disabled:opacity-40 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <span className="text-sm text-slate-700">consecutive failures</span>
          </div>
        </div>
      </div>

      <div className="flex justify-end gap-2 pt-2">
        <button onClick={onCancel}
          className="px-4 py-2 text-sm text-slate-600 border border-slate-200 rounded-lg hover:bg-slate-100">
          Cancel
        </button>
        <button
          onClick={() => onSave(form)}
          disabled={saving || !form.name.trim() || !form.webhookUrl.trim()}
          className="px-4 py-2 text-sm bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 disabled:opacity-50"
        >
          {saving ? 'Saving…' : 'Save'}
        </button>
      </div>
    </div>
  )
}

// ── Config card ───────────────────────────────────────────────────────────────

function ConfigCard({
  config, onEdit, onDelete, onTest, testingId, deletingId,
}: {
  config: NotificationConfig
  onEdit: () => void
  onDelete: (id: number) => void
  onTest: (id: number) => void
  testingId: number | null
  deletingId: number | null
}) {
  const triggers = [
    config.onBuildFailure        && 'Build failure',
    config.onConsecutiveFailures && `${config.consecutiveThreshold} consecutive failures`,
  ].filter(Boolean).join(', ') || 'None'

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center gap-4">
      <div className="flex items-center gap-3 flex-1 min-w-0">
        <div className={`w-9 h-9 rounded-lg flex items-center justify-center shrink-0 ${
          config.channel === 'TEAMS' ? 'bg-purple-100' : 'bg-green-100'
        }`}>
          <Bell className={`w-4 h-4 ${config.channel === 'TEAMS' ? 'text-purple-600' : 'text-green-600'}`} />
        </div>
        <div className="min-w-0">
          <p className="font-medium text-slate-900 text-sm truncate">{config.name}</p>
          <p className="text-xs text-slate-400">{CHANNEL_LABELS[config.channel]} · {triggers}</p>
        </div>
      </div>

      <div className="flex items-center gap-2 shrink-0">
        <button
          onClick={() => onTest(config.id)}
          disabled={testingId === config.id}
          className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium border border-slate-200 text-slate-600 rounded-lg hover:bg-slate-50 disabled:opacity-50"
        >
          <Send className="w-3 h-3" />
          {testingId === config.id ? 'Sending…' : 'Test'}
        </button>
        <button onClick={onEdit}
          className="p-1.5 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg">
          <Pencil className="w-4 h-4" />
        </button>
        <button
          onClick={() => onDelete(config.id)}
          disabled={deletingId === config.id}
          className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg disabled:opacity-50"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </div>
    </div>
  )
}

// ── History row ───────────────────────────────────────────────────────────────

function HistoryRow({ event }: { event: NotificationEvent }) {
  return (
    <tr className="border-b border-slate-100 last:border-0 hover:bg-slate-50">
      <td className="py-2.5 px-4 text-sm">
        {event.success
          ? <CheckCircle2 className="w-4 h-4 text-emerald-500" />
          : <XCircle className="w-4 h-4 text-red-500" />}
      </td>
      <td className="py-2.5 px-4 text-sm text-slate-700">{CHANNEL_LABELS[event.channel]}</td>
      <td className="py-2.5 px-4 text-sm text-slate-700">{TRIGGER_LABELS[event.triggerEvent] ?? event.triggerEvent}</td>
      <td className="py-2.5 px-4 text-sm text-slate-500">{event.jobName ?? '—'}</td>
      <td className="py-2.5 px-4 text-sm text-slate-500">{event.buildNumber ? `#${event.buildNumber}` : '—'}</td>
      <td className="py-2.5 px-4 text-sm text-slate-400 whitespace-nowrap">
        {new Date(event.sentAt).toLocaleString()}
      </td>
      <td className="py-2.5 px-4 text-xs text-red-500 max-w-xs truncate">{event.errorMessage ?? ''}</td>
    </tr>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function NotificationsPage() {
  const [configs, setConfigs]       = useState<NotificationConfig[]>([])
  const [history, setHistory]       = useState<NotificationEvent[]>([])
  const [loading, setLoading]       = useState(true)
  const [showForm, setShowForm]     = useState(false)
  const [editTarget, setEditTarget] = useState<NotificationConfig | null>(null)
  const [saving, setSaving]         = useState(false)
  const [testingId, setTestingId]   = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [toast, setToast]           = useState<{ msg: string; ok: boolean } | null>(null)

  const showToast = (msg: string, ok = true) => {
    setToast({ msg, ok })
    setTimeout(() => setToast(null), 3500)
  }

  const load = () => Promise.all([
    getNotificationConfigs().then(r => setConfigs(r.data.data)),
    getNotificationHistory().then(r => setHistory(r.data.data)),
  ])

  useEffect(() => {
    load().catch(() => {}).finally(() => setLoading(false))
  }, [])

  const handleSave = async (data: CreateNotificationConfigRequest) => {
    setSaving(true)
    try {
      if (editTarget) {
        const res = await updateNotificationConfig(editTarget.id, data)
        setConfigs(cs => cs.map(c => c.id === editTarget.id ? res.data.data : c))
        showToast('Notification updated')
      } else {
        const res = await createNotificationConfig(data)
        setConfigs(cs => [res.data.data, ...cs])
        showToast('Notification created')
      }
      setShowForm(false)
      setEditTarget(null)
    } catch {
      showToast('Failed to save notification', false)
    } finally {
      setSaving(false)
    }
  }

  const handleTest = async (id: number) => {
    setTestingId(id)
    try {
      await testNotificationConfig(id)
      showToast('Test notification sent')
      await getNotificationHistory().then(r => setHistory(r.data.data))
    } catch {
      showToast('Test failed — check the webhook URL', false)
    } finally {
      setTestingId(null)
    }
  }

  const handleDelete = async (id: number) => {
    setDeletingId(id)
    try {
      await deleteNotificationConfig(id)
      setConfigs(cs => cs.filter(c => c.id !== id))
      showToast('Notification deleted')
    } catch {
      showToast('Failed to delete', false)
    } finally {
      setDeletingId(null)
    }
  }

  const openEdit = (config: NotificationConfig) => {
    setEditTarget(config)
    setShowForm(true)
  }

  const closeForm = () => { setShowForm(false); setEditTarget(null) }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-40">
        <div className="w-6 h-6 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  const formInitial: CreateNotificationConfigRequest = editTarget
    ? {
        name: editTarget.name, channel: editTarget.channel,
        webhookUrl: '',  // never pre-fill the URL for security
        onBuildFailure: editTarget.onBuildFailure,
        onConsecutiveFailures: editTarget.onConsecutiveFailures,
        consecutiveThreshold: editTarget.consecutiveThreshold,
        onFlakyThreshold: editTarget.onFlakyThreshold,
        flakyScoreThreshold: editTarget.flakyScoreThreshold,
      }
    : EMPTY_FORM

  return (
    <div className="space-y-8">
      {/* Toast */}
      {toast && (
        <div className={`fixed top-4 right-4 z-50 flex items-center gap-2 px-4 py-3 rounded-xl shadow-lg text-sm font-medium text-white transition-all ${
          toast.ok ? 'bg-emerald-600' : 'bg-red-600'
        }`}>
          {toast.ok ? <CheckCircle2 className="w-4 h-4" /> : <XCircle className="w-4 h-4" />}
          {toast.msg}
        </div>
      )}

      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Notifications</h1>
          <p className="text-slate-500 mt-1 text-sm">Send build alerts to Teams or Slack.</p>
        </div>
        {!showForm && (
          <button
            onClick={() => { setEditTarget(null); setShowForm(true) }}
            className="flex items-center gap-1.5 px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700"
          >
            <Plus className="w-4 h-4" /> Add webhook
          </button>
        )}
      </div>

      {/* Form */}
      {showForm && (
        <ConfigForm
          initial={formInitial}
          onSave={handleSave}
          onCancel={closeForm}
          saving={saving}
        />
      )}

      {/* Config list */}
      {configs.length === 0 && !showForm ? (
        <div className="text-center py-16 border border-dashed border-slate-200 rounded-xl">
          <Bell className="w-8 h-8 text-slate-300 mx-auto mb-3" />
          <p className="text-slate-500 text-sm">No webhooks configured yet.</p>
          <button
            onClick={() => setShowForm(true)}
            className="mt-3 text-sm text-indigo-600 hover:text-indigo-700 font-medium"
          >
            Add your first webhook →
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {configs.map(cfg => (
            <ConfigCard key={cfg.id} config={cfg}
              onEdit={() => openEdit(cfg)}
              onDelete={handleDelete}
              onTest={handleTest}
              testingId={testingId}
              deletingId={deletingId}
            />
          ))}
        </div>
      )}

      {/* History */}
      <div>
        <div className="flex items-center gap-2 mb-3">
          <Clock className="w-4 h-4 text-slate-400" />
          <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wider">Notification History</h2>
        </div>

        {history.length === 0 ? (
          <p className="text-sm text-slate-400 py-4">No notifications sent yet.</p>
        ) : (
          <div className="border border-slate-200 rounded-xl overflow-hidden">
            <table className="w-full text-left">
              <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                <tr>
                  <th className="py-2.5 px-4">Status</th>
                  <th className="py-2.5 px-4">Channel</th>
                  <th className="py-2.5 px-4">Trigger</th>
                  <th className="py-2.5 px-4">Job</th>
                  <th className="py-2.5 px-4">Build</th>
                  <th className="py-2.5 px-4">Sent At</th>
                  <th className="py-2.5 px-4">Error</th>
                </tr>
              </thead>
              <tbody>
                {history.map(e => <HistoryRow key={e.id} event={e} />)}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
