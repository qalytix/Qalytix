import { useState, useEffect } from 'react'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js'
import { Line, Bar } from 'react-chartjs-2'
import { AlertTriangle, TrendingDown, Layers, RefreshCw, Table2, TrendingUp, Minus } from 'lucide-react'
import { getAnalyticsSummary } from '../../api/analytics'
import type { AnalyticsSummaryResponse, JobStat } from '../../types/analytics'

ChartJS.register(
  CategoryScale, LinearScale, PointElement, LineElement,
  BarElement, Title, Tooltip, Legend, Filler
)

const DAY_OPTIONS = [7, 14, 30, 90]

function SectionHeader({ icon: Icon, title }: { icon: React.ElementType; title: string }) {
  return (
    <div className="flex items-center gap-2 mb-4">
      <Icon className="w-5 h-5 text-indigo-600" />
      <h2 className="text-lg font-semibold text-slate-800">{title}</h2>
    </div>
  )
}

function Card({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return (
    <div className={`bg-white rounded-xl border border-slate-200 p-6 ${className}`}>
      {children}
    </div>
  )
}

function FlakinessBar({ score }: { score: number }) {
  const pct = Math.round(score * 100)
  const color = pct >= 40 ? 'bg-red-500' : pct >= 20 ? 'bg-amber-400' : 'bg-emerald-500'
  return (
    <div className="flex items-center gap-2">
      <div className="flex-1 bg-slate-100 rounded-full h-2">
        <div className={`${color} h-2 rounded-full`} style={{ width: `${pct}%` }} />
      </div>
      <span className="text-xs font-medium text-slate-600 w-10 text-right">{pct}%</span>
    </div>
  )
}

const BUILD_STATUS_STYLE: Record<string, string> = {
  SUCCESS:     'bg-green-100 text-green-700',
  FAILURE:     'bg-red-100 text-red-700',
  UNSTABLE:    'bg-amber-100 text-amber-700',
  ABORTED:     'bg-slate-100 text-slate-500',
  IN_PROGRESS: 'bg-blue-100 text-blue-700',
  UNKNOWN:     'bg-slate-100 text-slate-400',
}

function BuildBadge({ status }: { status: string }) {
  const cls = BUILD_STATUS_STYLE[status] ?? BUILD_STATUS_STYLE.UNKNOWN
  return (
    <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${cls}`}>
      {status === 'IN_PROGRESS' ? 'Running' : status.charAt(0) + status.slice(1).toLowerCase()}
    </span>
  )
}

function TrendBadge({ trend }: { trend: string }) {
  if (trend === 'Up')   return <span className="flex items-center gap-1 text-green-600 text-xs font-medium"><TrendingUp className="w-3.5 h-3.5" />Up</span>
  if (trend === 'Down') return <span className="flex items-center gap-1 text-red-500 text-xs font-medium"><TrendingDown className="w-3.5 h-3.5" />Down</span>
  if (trend === 'Stable') return <span className="flex items-center gap-1 text-slate-500 text-xs font-medium"><Minus className="w-3.5 h-3.5" />Stable</span>
  return <span className="text-slate-400 text-xs">—</span>
}

function JobStatsTable({ rows }: { rows: JobStat[] }) {
  if (rows.length === 0)
    return <p className="text-slate-400 text-sm text-center py-10">No job data for this period.</p>

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-slate-500 border-b border-slate-100 text-xs uppercase tracking-wide">
            <th className="pb-3 font-medium">Job</th>
            <th className="pb-3 font-medium text-right">Tests</th>
            <th className="pb-3 font-medium">Latest Build</th>
            <th className="pb-3 font-medium text-right">Yesterday ✓</th>
            <th className="pb-3 font-medium text-right">Today ✓</th>
            <th className="pb-3 font-medium">Trend</th>
            <th className="pb-3 font-medium text-right">Pass %</th>
            <th className="pb-3 font-medium text-right">Fail %</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
              <td className="py-3 font-medium text-slate-800 max-w-[200px] truncate" title={r.jobName}>{r.jobName}</td>
              <td className="py-3 text-right text-slate-600">{r.totalTests.toLocaleString()}</td>
              <td className="py-3"><BuildBadge status={r.latestBuildStatus ?? 'UNKNOWN'} /></td>
              <td className="py-3 text-right text-slate-600">{r.yesterdayPassed}</td>
              <td className="py-3 text-right text-slate-600">{r.todayPassed}</td>
              <td className="py-3"><TrendBadge trend={r.trend} /></td>
              <td className={`py-3 text-right font-medium ${r.passPercentage >= 90 ? 'text-green-600' : r.passPercentage >= 70 ? 'text-amber-600' : 'text-red-500'}`}>
                {r.passPercentage}%
              </td>
              <td className={`py-3 text-right font-medium ${r.failPercentage <= 10 ? 'text-slate-400' : r.failPercentage <= 30 ? 'text-amber-600' : 'text-red-500'}`}>
                {r.failPercentage}%
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function PassRateBar({ rate }: { rate: number }) {
  const color = rate >= 90 ? 'bg-emerald-500' : rate >= 70 ? 'bg-amber-400' : 'bg-red-500'
  return (
    <div className="flex items-center gap-2">
      <div className="flex-1 bg-slate-100 rounded-full h-2">
        <div className={`${color} h-2 rounded-full`} style={{ width: `${rate}%` }} />
      </div>
      <span className="text-xs font-medium text-slate-600 w-12 text-right">{rate}%</span>
    </div>
  )
}

export default function AnalyticsPage() {
  const [days, setDays] = useState(30)
  const [data, setData] = useState<AnalyticsSummaryResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const result = await getAnalyticsSummary(days)
      setData(result)
    } catch {
      setError('Failed to load analytics data.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [days])

  const trendChart = data ? {
    labels: data.failureTrend.map(p => p.date),
    datasets: [
      {
        label: 'Failed',
        data: data.failureTrend.map(p => p.failed),
        borderColor: '#ef4444',
        backgroundColor: 'rgba(239,68,68,0.08)',
        fill: true,
        tension: 0.3,
      },
      {
        label: 'Passed',
        data: data.failureTrend.map(p => p.passed),
        borderColor: '#22c55e',
        backgroundColor: 'rgba(34,197,94,0.08)',
        fill: true,
        tension: 0.3,
      },
    ],
  } : null

  const topFailingChart = data ? {
    labels: data.topFailingTests.map(t => t.testName.length > 30 ? t.testName.slice(0, 28) + '…' : t.testName),
    datasets: [{
      label: 'Failures',
      data: data.topFailingTests.map(t => t.failCount),
      backgroundColor: 'rgba(239,68,68,0.8)',
      borderRadius: 4,
    }],
  } : null

  const chartOptions = {
    responsive: true,
    plugins: { legend: { position: 'top' as const } },
    scales: { y: { beginAtZero: true } },
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Analytics</h1>
          <p className="text-sm text-slate-500 mt-0.5">Test health insights across your CI pipelines</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex rounded-lg border border-slate-200 overflow-hidden text-sm">
            {DAY_OPTIONS.map(d => (
              <button
                key={d}
                onClick={() => setDays(d)}
                className={`px-3 py-1.5 font-medium transition-colors ${
                  days === d
                    ? 'bg-indigo-600 text-white'
                    : 'bg-white text-slate-600 hover:bg-slate-50'
                }`}
              >
                {d}d
              </button>
            ))}
          </div>
          <button
            onClick={load}
            disabled={loading}
            className="p-2 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-2 text-red-600 bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm">
          <AlertTriangle className="w-4 h-4 shrink-0" />
          {error}
        </div>
      )}

      {loading && !data && (
        <div className="flex items-center justify-center h-64 text-slate-400">
          <RefreshCw className="w-6 h-6 animate-spin mr-2" />
          Loading analytics…
        </div>
      )}

      {data && (
        <div className="space-y-6">
          {/* Job Stats Table */}
          <Card>
            <SectionHeader icon={Table2} title="Job Summary" />
            <JobStatsTable rows={data.jobStats ?? []} />
          </Card>

          {/* Failure Trend */}
          <Card>
            <SectionHeader icon={TrendingDown} title="Failure Trend" />
            {data.failureTrend.length === 0 ? (
              <p className="text-slate-400 text-sm text-center py-10">No data for this period.</p>
            ) : (
              <Line data={trendChart!} options={chartOptions} />
            )}
          </Card>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Top Failing Tests */}
            <Card>
              <SectionHeader icon={AlertTriangle} title="Top Failing Tests" />
              {data.topFailingTests.length === 0 ? (
                <p className="text-slate-400 text-sm text-center py-10">No failures recorded.</p>
              ) : (
                <Bar data={topFailingChart!} options={chartOptions} />
              )}
            </Card>

            {/* Module Stability */}
            <Card>
              <SectionHeader icon={Layers} title="Module Stability" />
              {data.moduleStability.length === 0 ? (
                <p className="text-slate-400 text-sm text-center py-10">No module data available.</p>
              ) : (
                <div className="space-y-3 overflow-y-auto max-h-72">
                  {data.moduleStability.map(m => (
                    <div key={m.moduleName}>
                      <div className="flex justify-between text-sm mb-1">
                        <span className="font-mono text-slate-700 truncate" title={m.moduleName}>
                          {m.moduleName}
                        </span>
                        <span className="text-slate-400 shrink-0 ml-2">{m.total} runs</span>
                      </div>
                      <PassRateBar rate={m.passRate} />
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>

          {/* Flaky Tests */}
          <Card>
            <SectionHeader icon={AlertTriangle} title="Flaky Tests" />
            {data.flakyTests.length === 0 ? (
              <p className="text-slate-400 text-sm text-center py-10">No flaky tests detected.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="text-left text-slate-500 border-b border-slate-100">
                      <th className="pb-3 font-medium">Test</th>
                      <th className="pb-3 font-medium">Suite</th>
                      <th className="pb-3 font-medium text-right">Runs</th>
                      <th className="pb-3 font-medium text-right">Fails</th>
                      <th className="pb-3 font-medium w-40">Flakiness</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.flakyTests.map((t, i) => (
                      <tr key={i} className="border-b border-slate-50 hover:bg-slate-50">
                        <td className="py-3 font-mono text-slate-800 max-w-xs truncate" title={t.testName}>
                          {t.testName}
                        </td>
                        <td className="py-3 text-slate-500 max-w-xs truncate" title={t.testSuite}>
                          {t.testSuite}
                        </td>
                        <td className="py-3 text-right text-slate-600">{t.totalRuns}</td>
                        <td className="py-3 text-right text-red-500 font-medium">{t.failCount}</td>
                        <td className="py-3 w-40">
                          <FlakinessBar score={t.flakinessScore} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </div>
      )}
    </div>
  )
}
