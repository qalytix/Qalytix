import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Zap, Loader2, CheckCircle2 } from 'lucide-react'
import { resetPassword } from '../../api/auth'

export default function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const navigate = useNavigate()

  const [form, setForm]       = useState({ password: '', confirm: '' })
  const [loading, setLoading] = useState(false)
  const [done, setDone]       = useState(false)
  const [error, setError]     = useState('')

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    if (form.password !== form.confirm) { setError('Passwords do not match.'); return }
    if (form.password.length < 8) { setError('Password must be at least 8 characters.'); return }
    if (!token) { setError('Invalid or missing reset token.'); return }

    setLoading(true)
    try {
      await resetPassword(token, form.password)
      setDone(true)
      setTimeout(() => navigate('/login', { replace: true }), 3000)
    } catch (e: any) {
      setError(e.response?.data?.detail ?? 'Reset failed. The link may have expired.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <div className="flex items-center justify-center gap-2 mb-8">
          <Zap className="w-8 h-8 text-indigo-600" />
          <span className="text-2xl font-bold text-slate-900">Qalytix</span>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-8">
          {done ? (
            <div className="text-center py-4">
              <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto mb-4" />
              <h1 className="text-lg font-semibold text-slate-900 mb-2">Password reset!</h1>
              <p className="text-sm text-slate-500 mb-4">
                You'll be redirected to sign in shortly.
              </p>
              <Link to="/login" className="text-sm text-indigo-600 hover:underline font-medium">
                Sign in now →
              </Link>
            </div>
          ) : (
            <>
              <h1 className="text-xl font-semibold text-slate-900 mb-1">Set new password</h1>
              <p className="text-sm text-slate-500 mb-6">Choose a strong password for your account.</p>

              {error && (
                <div className="mb-4 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">New password</label>
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
                  {loading ? 'Saving…' : 'Set new password'}
                </button>
              </form>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
