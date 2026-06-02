import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Zap, Loader2, CheckCircle2 } from 'lucide-react'
import { forgotPassword } from '../../api/auth'

export default function ForgotPasswordPage() {
  const [email, setEmail]     = useState('')
  const [loading, setLoading] = useState(false)
  const [sent, setSent]       = useState(false)
  const [error, setError]     = useState('')

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await forgotPassword(email)
      setSent(true)
    } catch {
      // Always show success to prevent email enumeration
      setSent(true)
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
          {sent ? (
            <div className="text-center py-4">
              <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto mb-4" />
              <h1 className="text-lg font-semibold text-slate-900 mb-2">Check your email</h1>
              <p className="text-sm text-slate-500 mb-6">
                If an account exists for <strong>{email}</strong>, a password reset link has been sent. Check your inbox.
              </p>
              <Link to="/login" className="text-sm text-indigo-600 hover:underline font-medium">
                Back to sign in
              </Link>
            </div>
          ) : (
            <>
              <h1 className="text-xl font-semibold text-slate-900 mb-1">Reset your password</h1>
              <p className="text-sm text-slate-500 mb-6">
                Enter your email and we'll send you a reset link.
              </p>

              {error && (
                <div className="mb-4 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Work email</label>
                  <input
                    type="email" required
                    value={email}
                    onChange={e => setEmail(e.target.value)}
                    placeholder="jane@company.com"
                    className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <button
                  type="submit" disabled={loading}
                  className="w-full flex items-center justify-center gap-2 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-sm font-medium rounded-lg transition-colors"
                >
                  {loading && <Loader2 className="w-4 h-4 animate-spin" />}
                  {loading ? 'Sending…' : 'Send reset link'}
                </button>
              </form>
            </>
          )}
        </div>

        {!sent && (
          <p className="mt-6 text-center text-sm text-slate-500">
            Remembered it?{' '}
            <Link to="/login" className="text-indigo-600 hover:underline font-medium">Sign in</Link>
          </p>
        )}
      </div>
    </div>
  )
}
