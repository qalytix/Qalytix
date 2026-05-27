import { useState, useEffect, useCallback } from 'react'
import { Activity, CheckCircle, XCircle, Zap, FlaskConical } from 'lucide-react'
import { getDashboardStats } from '../../api/dashboard'
import { useAuthStore } from '../../stores/authStore'
import { useWebSocket } from '../../hooks/useWebSocket'
import BuildStatusBadge from '../../components/common/BuildStatusBadge'
import type { DashboardStats, BuildEvent, RecentBuild } from '../../types/dashboard'

function StatCard({ icon: Icon, label, value, color }: {
  icon: React.ElementType; label: string; value: number; color: string
}) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 flex items-center gap-4">
      <div className={`p-3 rounded-lg ${color}`}>
        <Icon className="w-5 h-5" />
      </div>
      <div>
        <p className="text-2xl font-bold text-slate-900">{value}</p>
        <p className="text-sm text-slate-500">{label}</p>
      </div>
    </div>
  )
}

function formatRelative(ts: string | null): string {
  if (!ts) return '—'
  const diff = Date.now() - new Date(ts).getTime()
  const s = Math.floor(diff / 1000)
  if (s < 60)  return `${s}s ago`
  const m = Math.floor(s / 60)
  if (m < 60)  return `${m}m ago`
  return `${Math.floor(m / 60)}h ago`
}

export default function DashboardPage() {
  const [stats, setStats]         = useState<DashboardStats | null>(null)
  const [loading, setLoading]     = useState(true)
  const [testJobsOnly, setTestJobsOnly] = useState(false)
  const org = useAuthStore((s) => s.org)

  const load = useCallback(async (testOnly: boolean) => {
    try {
      const { data } = await getDashboardStats(testOnly)
      setStats(data.data)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    setLoading(true)
    load(testJobsOnly)
  }, [load, testJobsOnly])

  const handleBuildEvent = useCallback((event: BuildEvent) => {
    setStats(prev => {
      if (!prev) return prev

      const toRecent = (e: BuildEvent): RecentBuild => ({
        buildId: e.buildId, jobId: e.jobId, jobName: e.jobName,
        buildNumber: e.buildNumber, status: e.status,
        durationMs: e.durationMs, startedAt: e.startedAt,
      })

      const existing = prev.recentBuilds.find(b => b.buildId === event.buildId)
      const recentBuilds = existing
        ? prev.recentBuilds.map(b => b.buildId === event.buildId ? toRecent(event) : b)
        : [toRecent(event), ...prev.recentBuilds].slice(0, 10)

      const activeBuilds = recentBuilds.filter(b => b.status === 'IN_PROGRESS').length

      return { ...prev, recentBuilds, activeBuilds }
    })
  }, [])

  useWebSocket({ orgId: org?.id ?? 0, onBuildEvent: handleBuildEvent })

  if (loading) return <div className="text-sm text-slate-500">Loading…</div>

  const s = stats ?? { activeBuilds: 0, todayTotal: 0, todaySuccess: 0, todayFailure: 0, recentBuilds: [] }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Dashboard</h1>
          <p className="text-sm text-slate-500 mt-0.5">Live build activity for your organization</p>
        </div>

        {/* Test jobs only toggle */}
        <button
          onClick={() => setTestJobsOnly(v => !v)}
          className={[
            'inline-flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors',
            testJobsOnly
              ? 'bg-emerald-50 text-emerald-700 border-emerald-300'
              : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50',
          ].join(' ')}
          title={testJobsOnly ? 'Showing test jobs only — click to show all' : 'Click to show test jobs only'}
        >
          <FlaskConical className="w-3.5 h-3.5" />
          {testJobsOnly ? 'Test jobs only' : 'All jobs'}
        </button>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={Activity}     label="Active builds"    value={s.activeBuilds}  color="bg-blue-50 text-blue-600" />
        <StatCard icon={Zap}          label="Builds today"     value={s.todayTotal}    color="bg-slate-100 text-slate-500" />
        <StatCard icon={CheckCircle}  label="Passed today"     value={s.todaySuccess}  color="bg-green-50 text-green-600" />
        <StatCard icon={XCircle}      label="Failed today"     value={s.todayFailure}  color="bg-red-50 text-red-500" />
      </div>

      {/* Recent activity */}
      <div className="bg-white rounded-xl border border-slate-200">
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-900">
            Recent builds{testJobsOnly && <span className="ml-2 text-emerald-600 font-normal text-xs">(test jobs)</span>}
          </h2>
          <span className="text-xs text-slate-400">Live</span>
        </div>

        {s.recentBuilds.length === 0 ? (
          <div className="p-12 text-center text-sm text-slate-500">
            {testJobsOnly ? 'No test job builds found. Run a job with test results first.' : 'No builds yet.'}
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {s.recentBuilds.map(build => (
              <div key={build.buildId} className="px-5 py-3.5 flex items-center justify-between gap-4">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-slate-900 truncate">{build.jobName}</p>
                  <p className="text-xs text-slate-400">#{build.buildNumber}</p>
                </div>
                <div className="flex items-center gap-4 flex-shrink-0">
                  <BuildStatusBadge status={build.status} />
                  <span className="text-xs text-slate-400 w-16 text-right">
                    {formatRelative(build.startedAt)}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
