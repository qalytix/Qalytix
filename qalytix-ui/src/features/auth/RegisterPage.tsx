import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Zap, Loader2 } from 'lucide-react'
import { register } from '../../api/auth'
import { useAuthStore } from '../../stores/authStore'

export default function RegisterPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)

  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    orgName: '',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')

    if (form.password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }

    setLoading(true)
    try {
      const { data } = await register(form)
      setAuth(data.data)
      navigate('/dashboard', { replace: true })
    } catch (err: any) {
      const detail = err.response?.data?.detail
      const fieldErrors = err.response?.data?.errors
      if (fieldErrors) {
        setError(Object.values(fieldErrors).join('. '))
      } else {
        setError(detail ?? 'Registration failed. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  const field = (key: keyof typeof form) => ({
    value: form[key],
    onChange: (e: React.ChangeEvent<HTMLInputElement>) =>
      setForm({ ...form, [key]: e.target.value }),
  })

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="flex items-center justify-center gap-2 mb-8">
          <Zap className="w-8 h-8 text-blue-600" />
          <span className="text-2xl font-bold text-slate-900">Qalytix</span>
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-8">
          <h1 className="text-xl font-semibold text-slate-900 mb-1">Create your account</h1>
          <p className="text-sm text-slate-500 mb-6">
            Set up your organization and start monitoring tests
          </p>

          {error && (
            <div className="mb-4 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Full name</label>
              <input
                type="text"
                required
                {...field('fullName')}
                placeholder="Jane Smith"
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Work email</label>
              <input
                type="email"
                required
                {...field('email')}
                placeholder="jane@company.com"
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Password</label>
              <input
                type="password"
                required
                minLength={8}
                {...field('password')}
                placeholder="At least 8 characters"
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition"
              />
            </div>

            <div className="pt-2 border-t border-slate-100">
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Organization name
              </label>
              <input
                type="text"
                required
                {...field('orgName')}
                placeholder="Acme Corp"
                className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition"
              />
              <p className="text-xs text-slate-400 mt-1">
                You'll be the Owner of this organization.
              </p>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 py-2.5 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white text-sm font-medium rounded-lg transition-colors"
            >
              {loading && <Loader2 className="w-4 h-4 animate-spin" />}
              {loading ? 'Creating account…' : 'Get started'}
            </button>
          </form>
        </div>

        <p className="mt-6 text-center text-sm text-slate-500">
          Already have an account?{' '}
          <Link to="/login" className="text-blue-600 hover:underline font-medium">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  )
}
