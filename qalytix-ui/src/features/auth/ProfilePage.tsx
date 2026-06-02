import { useState, type FormEvent } from 'react'
import { User, Lock, CheckCircle2, XCircle } from 'lucide-react'
import { changePassword } from '../../api/auth'
import { useAuthStore } from '../../stores/authStore'

export default function ProfilePage() {
  const user = useAuthStore(s => s.user)
  const org  = useAuthStore(s => s.org)
  const role = useAuthStore(s => s.role)

  const [form, setForm]       = useState({ current: '', next: '', confirm: '' })
  const [loading, setLoading] = useState(false)
  const [toast, setToast]     = useState<{ msg: string; ok: boolean } | null>(null)

  const showToast = (msg: string, ok = true) => {
    setToast({ msg, ok })
    setTimeout(() => setToast(null), 3500)
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (form.next !== form.confirm) { showToast('New passwords do not match.', false); return }
    if (form.next.length < 8) { showToast('Password must be at least 8 characters.', false); return }

    setLoading(true)
    try {
      await changePassword(form.current, form.next)
      setForm({ current: '', next: '', confirm: '' })
      showToast('Password changed successfully.')
    } catch (e: any) {
      showToast(e.response?.data?.detail ?? 'Failed to change password.', false)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-8 max-w-2xl">
      {toast && (
        <div className={`fixed top-4 right-4 z-50 flex items-center gap-2 px-4 py-3 rounded-xl shadow-lg text-sm font-medium text-white ${toast.ok ? 'bg-emerald-600' : 'bg-red-600'}`}>
          {toast.ok ? <CheckCircle2 className="w-4 h-4" /> : <XCircle className="w-4 h-4" />}
          {toast.msg}
        </div>
      )}

      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Profile</h1>
        <p className="text-slate-500 mt-1 text-sm">Your account information.</p>
      </div>

      {/* Account info */}
      <div className="bg-white border border-slate-200 rounded-xl p-6 space-y-4">
        <div className="flex items-center gap-3 mb-2">
          <User className="w-5 h-5 text-slate-400" />
          <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wider">Account</h2>
        </div>

        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-700 font-bold text-xl">
            {user?.fullName?.charAt(0).toUpperCase() ?? '?'}
          </div>
          <div>
            <p className="font-semibold text-slate-900">{user?.fullName}</p>
            <p className="text-sm text-slate-500">{user?.email}</p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 pt-2 border-t border-slate-100">
          <div>
            <p className="text-xs text-slate-400 uppercase tracking-wider mb-1">Organisation</p>
            <p className="text-sm font-medium text-slate-700">{org?.name ?? '—'}</p>
          </div>
          <div>
            <p className="text-xs text-slate-400 uppercase tracking-wider mb-1">Your role</p>
            <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
              role === 'OWNER'  ? 'bg-indigo-100 text-indigo-700' :
              role === 'ADMIN'  ? 'bg-amber-100 text-amber-700' :
              'bg-slate-100 text-slate-600'
            }`}>
              {role ?? '—'}
            </span>
          </div>
          <div>
            <p className="text-xs text-slate-400 uppercase tracking-wider mb-1">Plan</p>
            <p className="text-sm font-medium text-slate-700">{org?.plan ?? '—'}</p>
          </div>
        </div>
      </div>

      {/* Change password */}
      <div className="bg-white border border-slate-200 rounded-xl p-6">
        <div className="flex items-center gap-3 mb-5">
          <Lock className="w-5 h-5 text-slate-400" />
          <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wider">Change Password</h2>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 max-w-sm">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Current password</label>
            <input
              type="password" required
              value={form.current}
              onChange={e => setForm(f => ({ ...f, current: e.target.value }))}
              className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">New password</label>
            <input
              type="password" required minLength={8}
              value={form.next}
              onChange={e => setForm(f => ({ ...f, next: e.target.value }))}
              placeholder="At least 8 characters"
              className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Confirm new password</label>
            <input
              type="password" required
              value={form.confirm}
              onChange={e => setForm(f => ({ ...f, confirm: e.target.value }))}
              className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <button
            type="submit" disabled={loading}
            className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-sm font-medium rounded-lg transition-colors"
          >
            {loading ? 'Saving…' : 'Update password'}
          </button>
        </form>
      </div>
    </div>
  )
}
