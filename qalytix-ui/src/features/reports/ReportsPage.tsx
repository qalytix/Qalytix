import { useEffect, useState } from 'react'
import { Download, RefreshCw, CheckCircle2, XCircle, AlertTriangle, TrendingDown } from 'lucide-react'
import { getReportSummary, downloadReportCsv } from '../../api/reports'
import { getJobs } from '../../api/jenkins'
import type { ReportSummary } from '../../types/reports'
import type { Job } from '../../types/jenkins'

// ── helpers ───────────────────────────────────────────────────────────────────

function today() {
  return new Date().toISOString().split('T')[0]
}
function daysAgo(n: number) {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return d.toISOString().split('T')[0]
}

function StatCard({ label, value, sub, color }: {
  label: string; value: string | number; sub?: string; color?: string
}) {
  return (
    <div className="bg-white border border-slate-200 rounded-xl p-5">
      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-1">{label}</p>
      <p className={`text-3xl font-bold ${color ?? 'text-slate-900'}`}>{value}</p>
      {sub && <p className="text-xs text-slate-400 mt-1">{sub}</p>}
    </div>
  )
}

function PassRateBar({ rate }: { rate: number }) {
  const color = rate >= 90 ? 'bg-emerald-500' : rate >= 70 ? 'bg-amber-400' : 'bg-red-500'
  return (
    <div className="flex items-center gap-2">
      <div className="flex-1 h-1.5 bg-slate-100 rounded-full overflow-hidden">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${rate}%` }} />
      </div>
      <span className="text-xs text-slate-600 w-10 text-right">{rate}%</span>
    </div>
  )
}

// ── main page ─────────────────────────────────────────────────────────────────

export default function ReportsPage() {
  const [jobs, setJobs]         = useState<Job[]>([])
  const [report, setReport]     = useState<ReportSummary | null>(null)
  const [loading, setLoading]   = useState(false)
  const [error, setError]       = useState<string | null>(null)

  const [filters, setFilters] = useState({
    jobId: '' as string,
    from:  daysAgo(30),
    to:    today(),
  })

  useEffect(() => {
    getJobs().then(r => setJobs(r.data.data.filter(j => j.isTestJob))).catch(() => {})
    fetchReport()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const fetchReport = async (f = filters) => {
    setLoading(true)
    setError(null)
    try {
      const res = await getReportSummary({
        jobId: f.jobId ? Number(f.jobId) : null,
        from:  f.from,
        to:    f.to,
      })
      setReport(res.data.data)
    } catch {
      setError('Failed to load report.')
    } finally {
      setLoading(false)
    }
  }

  const handleExport = () => {
    downloadReportCsv({
      jobId: filters.jobId ? Number(filters.jobId) : null,
      from:  filters.from,
      to:    filters.to,
    })
  }

  const set = (patch: Partial<typeof filters>) =>
    setFilters(f => ({ ...f, ...patch }))

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-900">Reports</h1>
          <p className="text-slate-500 mt-1 text-sm">Historical test quality summary with CSV export.</p>
        </div>
        <button
          onClick={handleExport}
          disabled={!report || report.totalRuns === 0}
          className="flex items-center gap-1.5 px-4 py-2 border border-slate-200 text-slate-700 text-sm font-medium rounded-lg hover:bg-slate-50 disabled:opacity-40"
        >
          <Download className="w-4 h-4" /> Export CSV
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white border border-slate-200 rounded-xl p-4 flex flex-wrap gap-4 items-end">
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">From</label>
          <input
            type="date" value={filters.from} max={filters.to}
            onChange={e => set({ from: e.target.value })}
            className="px-3 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">To</label>
          <input
            type="date" value={filters.to} min={filters.from} max={today()}
            onChange={e => set({ to: e.target.value })}
            className="px-3 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Job</label>
          <select
            value={filters.jobId}
            onChange={e => set({ jobId: e.target.value })}
            className="px-3 py-2 text-sm border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            <option value="">All jobs</option>
            {jobs.map(j => <option key={j.id} value={j.id}>{j.displayName || j.jenkinsJobName}</option>)}
          </select>
        </div>

        {/* Quick ranges */}
        <div className="flex gap-2">
          {[7, 14, 30, 90].map(n => (
            <button key={n}
              onClick={() => { const f = { ...filters, from: daysAgo(n), to: today() }; set(f); fetchReport(f) }}
              className="px-3 py-2 text-xs font-medium border border-slate-200 rounded-lg hover:bg-slate-50 text-slate-600"
            >
              {n}d
            </button>
          ))}
        </div>

        <button
          onClick={() => fetchReport()}
          disabled={loading}
          className="flex items-center gap-1.5 px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 disabled:opacity-50 ml-auto"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          {loading ? 'Loading…' : 'Run report'}
        </button>
      </div>

      {error && <p className="text-sm text-red-500">{error}</p>}

      {report && (
        <>
          {/* Summary stats */}
          <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-5 gap-4">
            <StatCard label="Total Runs"    value={report.totalRuns.toLocaleString()} />
            <StatCard label="Passed"        value={report.totalPassed.toLocaleString()} color="text-emerald-600" />
            <StatCard label="Failed"        value={report.totalFailed.toLocaleString()} color={report.totalFailed > 0 ? 'text-red-600' : 'text-slate-900'} />
            <StatCard label="Skipped"       value={report.totalSkipped.toLocaleString()} color="text-slate-400" />
            <StatCard label="Pass Rate"     value={`${report.overallPassRate}%`}
              color={report.overallPassRate >= 90 ? 'text-emerald-600' : report.overallPassRate >= 70 ? 'text-amber-600' : 'text-red-600'}
              sub={`${report.fromDate} → ${report.toDate}`}
            />
          </div>

          {report.totalRuns === 0 ? (
            <div className="text-center py-16 border border-dashed border-slate-200 rounded-xl">
              <p className="text-slate-400 text-sm">No test data for the selected range.</p>
            </div>
          ) : (
            <>
              {/* Job breakdown */}
              {report.jobRows.length > 0 && (
                <div>
                  <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wider mb-3 flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-slate-400" /> Job Summary
                  </h2>
                  <div className="border border-slate-200 rounded-xl overflow-hidden">
                    <table className="w-full text-left">
                      <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                        <tr>
                          <th className="py-3 px-4">Job</th>
                          <th className="py-3 px-4 text-right">Total</th>
                          <th className="py-3 px-4 text-right">Passed</th>
                          <th className="py-3 px-4 text-right">Failed</th>
                          <th className="py-3 px-4 min-w-[160px]">Pass Rate</th>
                        </tr>
                      </thead>
                      <tbody>
                        {report.jobRows.map(row => (
                          <tr key={row.jobName} className="border-t border-slate-100 hover:bg-slate-50">
                            <td className="py-3 px-4 text-sm font-medium text-slate-900">{row.jobName}</td>
                            <td className="py-3 px-4 text-sm text-slate-600 text-right">{row.totalTests.toLocaleString()}</td>
                            <td className="py-3 px-4 text-sm text-emerald-600 text-right">{row.passed.toLocaleString()}</td>
                            <td className={`py-3 px-4 text-sm text-right ${row.failed > 0 ? 'text-red-600' : 'text-slate-400'}`}>
                              {row.failed.toLocaleString()}
                            </td>
                            <td className="py-3 px-4"><PassRateBar rate={row.passRate} /></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* Module stability */}
              {report.moduleRows.length > 0 && (
                <div>
                  <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wider mb-3 flex items-center gap-2">
                    <XCircle className="w-4 h-4 text-slate-400" /> Module Stability
                  </h2>
                  <div className="border border-slate-200 rounded-xl overflow-hidden">
                    <table className="w-full text-left">
                      <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                        <tr>
                          <th className="py-3 px-4">Module / Suite</th>
                          <th className="py-3 px-4 text-right">Total</th>
                          <th className="py-3 px-4 text-right">Passed</th>
                          <th className="py-3 px-4 min-w-[160px]">Pass Rate</th>
                        </tr>
                      </thead>
                      <tbody>
                        {report.moduleRows.map(row => (
                          <tr key={row.moduleName} className="border-t border-slate-100 hover:bg-slate-50">
                            <td className="py-3 px-4 text-sm font-medium text-slate-900">{row.moduleName}</td>
                            <td className="py-3 px-4 text-sm text-slate-600 text-right">{row.totalTests.toLocaleString()}</td>
                            <td className="py-3 px-4 text-sm text-emerald-600 text-right">{row.passed.toLocaleString()}</td>
                            <td className="py-3 px-4"><PassRateBar rate={row.passRate} /></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* Flaky tests */}
              {report.flakyRows.length > 0 && (
                <div>
                  <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wider mb-3 flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 text-amber-400" /> Flaky Tests
                    <span className="text-xs font-normal text-slate-400 normal-case">(both passed and failed in range)</span>
                  </h2>
                  <div className="border border-slate-200 rounded-xl overflow-hidden">
                    <table className="w-full text-left">
                      <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                        <tr>
                          <th className="py-3 px-4">Suite</th>
                          <th className="py-3 px-4">Test</th>
                          <th className="py-3 px-4 text-right">Runs</th>
                          <th className="py-3 px-4 text-right">Failures</th>
                          <th className="py-3 px-4 text-right flex items-center gap-1">
                            <TrendingDown className="w-3 h-3" /> Score
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {report.flakyRows.map((row, i) => (
                          <tr key={i} className="border-t border-slate-100 hover:bg-slate-50">
                            <td className="py-3 px-4 text-xs text-slate-500 max-w-[160px] truncate">{row.testSuite}</td>
                            <td className="py-3 px-4 text-sm text-slate-800">{row.testName}</td>
                            <td className="py-3 px-4 text-sm text-slate-600 text-right">{row.totalRuns}</td>
                            <td className="py-3 px-4 text-sm text-red-500 text-right">{row.failCount}</td>
                            <td className="py-3 px-4 text-right">
                              <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                                row.flakinessScore >= 0.4 ? 'bg-red-100 text-red-700' :
                                row.flakinessScore >= 0.2 ? 'bg-amber-100 text-amber-700' :
                                'bg-slate-100 text-slate-600'
                              }`}>
                                {row.flakinessScore.toFixed(3)}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </>
          )}
        </>
      )}

      {!report && !loading && !error && (
        <div className="text-center py-20 text-slate-400 text-sm">
          Select a date range and click <strong>Run report</strong>.
        </div>
      )}
    </div>
  )
}
