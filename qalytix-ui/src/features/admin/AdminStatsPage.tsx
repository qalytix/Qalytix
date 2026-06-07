import { useEffect, useState } from 'react'
import { Building2, Users, GitBranch, FlaskConical, Activity } from 'lucide-react'
import { getPlatformStats } from '../../api/admin'
import type { AdminPlatformStats } from '../../types/admin'

function StatCard({ icon: Icon, label, value, sub, color = 'text-indigo-600' }: {
  icon: React.ElementType; label: string; value: number; sub?: string; color?: string
}) {
  return (
    <div className="bg-white border border-slate-200 rounded-xl p-5">
      <div className="flex items-center gap-2 mb-3">
        <Icon className={`w-4 h-4 ${color}`} />
        <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{label}</span>
      </div>
      <p className="text-3xl font-bold text-slate-900">{value.toLocaleString()}</p>
      {sub && <p className="text-xs text-slate-400 mt-1">{sub}</p>}
    </div>
  )
}

export default function AdminStatsPage() {
  const [stats, setStats]   = useState<AdminPlatformStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getPlatformStats()
      .then(r => setStats(r.data.data))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return (
    <div className="flex items-center justify-center h-40">
      <div className="w-6 h-6 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
    </div>
  )

  if (!stats) return <p className="text-red-500 text-sm">Failed to load stats.</p>

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Platform Overview</h1>
        <p className="text-slate-500 mt-1 text-sm">Live counts across all tenants.</p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard icon={Building2}   label="Organisations"       value={stats.totalOrgs} />
        <StatCard icon={Activity}    label="Paying Orgs" value={stats.activeSubscriptions} sub="Pro / Enterprise, active or trialing" color="text-emerald-600" />
        <StatCard icon={Users}       label="Total Users"         value={stats.totalUsers} color="text-amber-600" />
        <StatCard icon={GitBranch}   label="Total Builds"        value={stats.totalBuilds} color="text-slate-500" />
        <StatCard icon={FlaskConical} label="Total Test Results" value={stats.totalTestResults} color="text-purple-600" />
        <StatCard icon={GitBranch}   label="Builds (24 h)"      value={stats.buildsLast24h} sub="last 24 hours" color="text-blue-500" />
        <StatCard icon={FlaskConical} label="Test Results (24 h)" value={stats.testResultsLast24h} sub="last 24 hours" color="text-blue-500" />
      </div>
    </div>
  )
}
