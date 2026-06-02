import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Zap, Loader2 } from 'lucide-react'
import { acceptInvitation } from '../../api/members'
import { useAuthStore } from '../../stores/authStore'

export default function AcceptInvitationPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const navigate = useNavigate()
  const setAuth = useAuthStore(s => s.setAuth)
  const isAuthenticated = useAuthStore(s => s.isAuthenticated)

  const [form, setForm] = useState({ fullName: '', password: '', confirm: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token) setError('Invalid invitation link.')
  }, [token])

  // If already logged in, accept directly (joining as existing user)
  useEffect(() => {
    if (isAuthenticated && token) {
      setLoading(true)
      acceptInvitation({ token })
        .then(r => { setAuth(r.data.data); navigate('/dashboard', { replace: true }) })
        .catch(e => setError(e.response?.data?.detail ?? 'Failed to accept invitation.'))
        .finally(() => setLoading(false))
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    if (form.password !== form.confirm) { setError('Passwords do not match.'); return }
    if (form.password.length < 8) { setError('Password must be at least 8 characters.'); return }

    setLoading(true)
    try {
      const res = await acceptInvitation({ token, fullName: form.fullName, password: form.password })
      setAuth(res.data.data)
      navigate('/dashboard', { replace: true })
    } catch (e: any) {
      setError(e.response?.data?.detail ?? 'Failed to accept invitation.')
    } finally {
      setLoading(false)
    }
  }

  if (isAuthenticated) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center">
        <div className="flex flex-col items-center gap-3 text-slate-500">
          <Loader2 className="w-6 h-6 animate-spin" />
          <p>Joining organisation…</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="flex items-center justify-center gap-2 mb-8">
          <Zap className="w-8 h-8 text-indigo-600" />
          <span className="text-2xl font-bold text-slate-900">Qalytix</span>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-8">
          <h1 className="text-xl font-semibold text-slate-900 mb-1">Accept your invitation</h1>
          <p className="text-sm text-slate-500 mb-6">Create your account to join the team.</p>

          {error && (
            <div className="mb-4 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Full name</label>
              <input
                type="text" required
                value={form.fullName}
                onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))}
                placeholder="Jane Smith"
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Password</label>
              <input
                type="password" required minLength={8}
                value={form.password}
                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                placeholder="At least 8 characters"
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Confirm password</label>
              <input
                type="password" required
                value={form.confirm}
                onChange={e => setForm(f => ({ ...f, confirm: e.target.value }))}
                placeholder="Repeat password"
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <button
              type="submit" disabled={loading || !token}
              className="w-full flex items-center justify-center gap-2 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-sm font-medium rounded-lg transition-colors"
            >
              {loading && <Loader2 className="w-4 h-4 animate-spin" />}
              {loading ? 'Joining…' : 'Join organisation'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
