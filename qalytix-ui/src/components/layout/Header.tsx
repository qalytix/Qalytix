import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { LogOut, ChevronDown, Building2, User, Shield } from 'lucide-react'
import { useAuthStore } from '../../stores/authStore'
import { logout } from '../../api/auth'

export default function Header() {
  const { user, org, role, superAdmin, clearAuth } = useAuthStore()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  const handleLogout = async () => {
    try {
      await logout()
    } finally {
      clearAuth()
      navigate('/login', { replace: true })
    }
  }

  return (
    <header className="h-14 bg-white border-b border-slate-200 flex items-center justify-between px-6 shrink-0">
      {/* Left: org name */}
      <div className="flex items-center gap-2 text-sm text-slate-600">
        <Building2 className="w-4 h-4" />
        <span className="font-medium">{org?.name}</span>
        {role && (
          <span className="text-xs px-2 py-0.5 bg-slate-100 text-slate-500 rounded-full">
            {role}
          </span>
        )}
      </div>

      {/* Right: user menu */}
      <div className="relative">
        <button
          onClick={() => setMenuOpen((o) => !o)}
          className="flex items-center gap-2 text-sm text-slate-700 hover:text-slate-900 py-1"
        >
          <div className="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center text-xs font-bold">
            {user?.fullName?.charAt(0).toUpperCase()}
          </div>
          <span className="hidden sm:block font-medium">{user?.fullName}</span>
          <ChevronDown className="w-4 h-4 text-slate-400" />
        </button>

        {menuOpen && (
          <>
            <div
              className="fixed inset-0 z-10"
              onClick={() => setMenuOpen(false)}
            />
            <div className="absolute right-0 top-10 z-20 w-48 bg-white border border-slate-200 rounded-lg shadow-lg py-1">
              <div className="px-4 py-2 border-b border-slate-100">
                <p className="text-xs text-slate-500 truncate">{user?.email}</p>
              </div>
              <button
                onClick={() => { setMenuOpen(false); navigate('/profile') }}
                className="flex items-center gap-2 w-full px-4 py-2 text-sm text-slate-700 hover:bg-slate-50 transition-colors"
              >
                <User className="w-4 h-4" />
                Profile
              </button>
              {superAdmin && (
                <button
                  onClick={() => { setMenuOpen(false); navigate('/admin') }}
                  className="flex items-center gap-2 w-full px-4 py-2 text-sm text-indigo-600 hover:bg-indigo-50 transition-colors"
                >
                  <Shield className="w-4 h-4" />
                  Admin portal
                </button>
              )}
              <div className="border-t border-slate-100 my-1" />
              <button
                onClick={handleLogout}
                className="flex items-center gap-2 w-full px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors"
              >
                <LogOut className="w-4 h-4" />
                Sign out
              </button>
            </div>
          </>
        )}
      </div>
    </header>
  )
}
