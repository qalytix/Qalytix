import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, ExternalLink } from 'lucide-react'
import { getJob, getBuilds } from '../../api/jenkins'
import type { Job, Build, PageResponse } from '../../types/jenkins'
import BuildStatusBadge from '../../components/common/BuildStatusBadge'

function formatDuration(ms: number | null): string {
  if (ms == null) return '—'
  const s = Math.floor(ms / 1000)
  const m = Math.floor(s / 60)
  return m > 0 ? `${m}m ${s % 60}s` : `${s}s`
}

export default function BuildsPage() {
  const { jobId }             = useParams<{ jobId: string }>()
  const navigate              = useNavigate()
  const [job, setJob]         = useState<Job | null>(null)
  const [page, setPage]       = useState<PageResponse<Build> | null>(null)
  const [current, setCurrent] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!jobId) return
    Promise.all([
      getJob(Number(jobId)).then(({ data }) => setJob(data.data)),
      loadPage(0),
    ]).finally(() => setLoading(false))
  }, [jobId])

  async function loadPage(p: number) {
    if (!jobId) return
    const { data } = await getBuilds(Number(jobId), p)
    setPage(data.data)
    setCurrent(p)
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <button onClick={() => navigate('/jobs')} className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg">
          <ArrowLeft className="w-4 h-4" />
        </button>
        <div>
          <h1 className="text-xl font-semibold text-slate-900">
            {job ? (job.displayName ?? job.jenkinsJobName) : 'Builds'}
          </h1>
          <p className="text-sm text-slate-500 mt-0.5">Build history</p>
        </div>
      </div>

      {loading ? (
        <div className="text-sm text-slate-500">Loading…</div>
      ) : !page || page.content.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <p className="text-slate-500 text-sm">No builds recorded yet.</p>
        </div>
      ) : (
        <>
          <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100">
                  <th className="text-left px-5 py-3 font-medium text-slate-500">#</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Status</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Started</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Duration</th>
                  <th className="text-left px-5 py-3 font-medium text-slate-500">Triggered by</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {page.content.map(b => (
                  <tr key={b.id} className="hover:bg-slate-50">
                    <td className="px-5 py-4 font-mono text-slate-700">#{b.buildNumber}</td>
                    <td className="px-5 py-4"><BuildStatusBadge status={b.status} /></td>
                    <td className="px-5 py-4 text-slate-500">
                      {b.startedAt ? new Date(b.startedAt).toLocaleString() : '—'}
                    </td>
                    <td className="px-5 py-4 text-slate-500">{formatDuration(b.durationMs)}</td>
                    <td className="px-5 py-4 text-slate-500">{b.triggeredBy ?? '—'}</td>
                    <td className="px-5 py-4">
                      {b.url && (
                        <a href={b.url} target="_blank" rel="noreferrer" className="text-slate-400 hover:text-blue-600" onClick={e => e.stopPropagation()}>
                          <ExternalLink className="w-4 h-4" />
                        </a>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {page.totalPages > 1 && (
            <div className="flex items-center justify-between text-sm text-slate-500">
              <span>{page.totalElements} total builds</span>
              <div className="flex gap-2">
                <button disabled={current === 0} onClick={() => loadPage(current - 1)}
                  className="px-3 py-1.5 border border-slate-200 rounded-lg disabled:opacity-40 hover:bg-slate-50">
                  Previous
                </button>
                <span className="px-3 py-1.5">Page {current + 1} of {page.totalPages}</span>
                <button disabled={current >= page.totalPages - 1} onClick={() => loadPage(current + 1)}
                  className="px-3 py-1.5 border border-slate-200 rounded-lg disabled:opacity-40 hover:bg-slate-50">
                  Next
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
