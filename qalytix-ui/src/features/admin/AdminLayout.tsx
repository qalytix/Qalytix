import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Shield, LayoutDashboard, Building2, LogOut } from 'lucide-react'
import { useAuthStore } from '../../stores/authStore'
import { logout } from '../../api/auth'

export default function AdminLayout() {
  const { user, clearAuth } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = async () => {
    try { await logout() } finally { clearAuth(); navigate('/login', { replace: true }) }
  }

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-2 px-3 py-2 text-sm rounded-lg transition-colors ${
      isActive ? 'bg-indigo-50 text-indigo-700 font-semibold' : 'text-slate-600 hover:bg-slate-100'
    }`

  return (
    <div className="flex h-screen bg-slate-50 overflow-hidden">
      {/* Sidebar */}
      <aside className="w-56 shrink-0 bg-white border-r border-slate-200 flex flex-col">
        <div className="px-4 py-5 border-b border-slate-100 flex items-center gap-2">
          <Shield className="w-5 h-5 text-indigo-600" />
          <span className="font-bold text-slate-900">Admin</span>
          <span className="ml-auto text-xs bg-red-100 text-red-600 font-semibold px-1.5 py-0.5 rounded">SUPER</span>
        </div>

        <nav className="flex-1 p-3 space-y-1">
          <NavLink to="/admin" end className={linkClass}>
            <LayoutDashboard className="w-4 h-4" /> Platform Stats
          </NavLink>
          <NavLink to="/admin/orgs" className={linkClass}>
            <Building2 className="w-4 h-4" /> Organisations
          </NavLink>
        </nav>

        <div className="p-3 border-t border-slate-100">
          <div className="text-xs text-slate-400 truncate mb-2 px-1">{user?.email}</div>
          <button onClick={() => navigate('/')}
            className="flex items-center gap-2 w-full px-3 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg">
            ← Back to app
          </button>
          <button onClick={handleLogout}
            className="flex items-center gap-2 w-full px-3 py-2 text-sm text-red-600 hover:bg-red-50 rounded-lg mt-1">
            <LogOut className="w-4 h-4" /> Sign out
          </button>
        </div>
      </aside>

      {/* Main */}
      <main className="flex-1 overflow-y-auto p-8">
        <Outlet />
      </main>
    </div>
  )
}
