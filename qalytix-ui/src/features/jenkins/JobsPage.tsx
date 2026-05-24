import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronRight } from 'lucide-react'
import { getJobs } from '../../api/jenkins'
import type { Job } from '../../types/jenkins'
import BuildStatusBadge from '../../components/common/BuildStatusBadge'

export default function JobsPage() {
  const [jobs, setJobs]       = useState<Job[]>([])
  const [loading, setLoading] = useState(true)
  const navigate              = useNavigate()

  useEffect(() => {
    getJobs()
      .then(({ data }) => setJobs(data.data))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-900">Jobs</h1>
        <p className="text-sm text-slate-500 mt-0.5">All synced Jenkins jobs across your connected servers</p>
      </div>

      {loading ? (
        <div className="text-sm text-slate-500">Loading…</div>
      ) : jobs.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <p className="text-slate-500 text-sm">No jobs yet. Add a Jenkins server and trigger a sync.</p>
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
                  <td className="px-5 py-4 font-medium text-slate-900">
                    {job.displayName ?? job.jenkinsJobName}
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
