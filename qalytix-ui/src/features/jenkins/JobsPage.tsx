import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronRight, FlaskConical } from 'lucide-react'
import { getJobs, getJobViews } from '../../api/jenkins'
import type { Job } from '../../types/jenkins'
import BuildStatusBadge from '../../components/common/BuildStatusBadge'

export default function JobsPage() {
  const [jobs, setJobs]         = useState<Job[]>([])
  const [views, setViews]       = useState<string[]>([])
  const [activeView, setActiveView] = useState<string>('All')
  const [loading, setLoading]   = useState(true)
  const navigate                = useNavigate()

  // Fetch available views once on mount
  useEffect(() => {
    getJobViews()
      .then(({ data }) => setViews(['All', ...data.data.filter(v => v !== 'All')]))
      .catch(() => setViews(['All']))
  }, [])

  // Re-fetch jobs whenever the selected view changes
  useEffect(() => {
    setLoading(true)
    getJobs(activeView)
      .then(({ data }) => setJobs(data.data))
      .finally(() => setLoading(false))
  }, [activeView])

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Jobs</h1>
          <p className="text-sm text-slate-500 mt-0.5">All synced Jenkins jobs across your connected servers</p>
        </div>

        {/* View selector pills */}
        {views.length > 1 && (
          <div className="flex items-center gap-1.5 flex-wrap">
            {views.map(view => (
              <button
                key={view}
                onClick={() => setActiveView(view)}
                className={[
                  'px-3 py-1 rounded-full text-xs font-medium transition-colors',
                  activeView === view
                    ? 'bg-blue-600 text-white'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200',
                ].join(' ')}
              >
                {view}
              </button>
            ))}
          </div>
        )}
      </div>

      {loading ? (
        <div className="text-sm text-slate-500">Loading…</div>
      ) : jobs.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <p className="text-slate-500 text-sm">
            {activeView === 'All'
              ? 'No jobs yet. Add a Jenkins server and trigger a sync.'
              : `No jobs in the "${activeView}" view.`}
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100">
                <th className="text-left px-5 py-3 font-medium text-slate-500">Job</th>
                <th className="text-left px-5 py-3 font-medium text-slate-500">Last Build</th>
                <th className="text-left px-5 py-3 font-medium text-slate-500">Status</th>
                <th className="text-left px-5 py-3 font-medium text-slate-500">Last Run</th>
                <th className="px-5 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {jobs.map(job => (
                <tr
                  key={job.id}
                  onClick={() => navigate(`/jobs/${job.id}/builds`)}
                  className="hover:bg-slate-50 cursor-pointer"
                >
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-slate-900">
                        {job.displayName ?? job.jenkinsJobName}
                      </span>
                      {job.isTestJob && (
                        <span
                          title="This job has test results"
                          className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-medium bg-emerald-50 text-emerald-700 border border-emerald-200"
                        >
                          <FlaskConical className="w-2.5 h-2.5" />
                          Tests
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-5 py-4 text-slate-500">
                    {job.lastBuildNumber != null ? `#${job.lastBuildNumber}` : '—'}
                  </td>
                  <td className="px-5 py-4">
                    <BuildStatusBadge status={job.lastBuildStatus} />
                  </td>
                  <td className="px-5 py-4 text-slate-400">
                    {job.lastBuildAt ? new Date(job.lastBuildAt).toLocaleString() : '—'}
                  </td>
                  <td className="px-5 py-4 text-slate-400">
                    <ChevronRight className="w-4 h-4" />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
