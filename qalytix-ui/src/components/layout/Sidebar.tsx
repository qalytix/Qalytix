import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  BarChart3,
  GitBranch,
  Boxes,
  Users,
  Bell,
  FileText,
  CreditCard,
  Zap,
} from 'lucide-react'

const navItems = [
  { to: '/dashboard',     icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/analytics',     icon: BarChart3,        label: 'Analytics' },
  { to: '/jenkins',       icon: GitBranch,        label: 'Jenkins' },
  { to: '/jobs',          icon: Boxes,            label: 'Jobs' },
  { to: '/members',       icon: Users,            label: 'Members' },
  { to: '/notifications', icon: Bell,             label: 'Notifications' },
  { to: '/reports',       icon: FileText,         label: 'Reports' },
  { to: '/billing',       icon: CreditCard,       label: 'Billing' },
]

export default function Sidebar() {
  return (
    <aside className="flex flex-col w-64 min-h-screen bg-slate-900 text-white shrink-0">
      {/* Logo */}
      <div className="flex items-center gap-2 px-6 py-5 border-b border-slate-700">
        <Zap className="w-6 h-6 text-blue-400" />
        <span className="text-xl font-bold tracking-tight">Qalytix</span>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        {navItems.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-blue-600 text-white'
                  : 'text-slate-400 hover:bg-slate-800 hover:text-white'
              }`
            }
          >
            <Icon className="w-4 h-4 shrink-0" />
            {label}
          </NavLink>
        ))}
      </nav>

      {/* Footer */}
      <div className="px-6 py-4 border-t border-slate-700 text-xs text-slate-500">
        v0.1.0-mvp
      </div>
    </aside>
  )
}
