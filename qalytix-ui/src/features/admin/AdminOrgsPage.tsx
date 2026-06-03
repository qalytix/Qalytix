import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, ChevronRight } from 'lucide-react'
import { getAdminOrgs } from '../../api/admin'
import type { AdminOrg } from '../../types/admin'
import type { Plan, OrgStatus } from '../../types/auth'

const PLAN_COLORS: Record<Plan, string> = {
  FREE:       'bg-slate-100 text-slate-600',
  PRO:        'bg-indigo-100 text-indigo-700',
  ENTERPRISE: 'bg-amber-100 text-amber-700',
}

const STATUS_COLORS: Record<OrgStatus, string> = {
  ACTIVE:    'bg-emerald-100 text-emerald-700',
  SUSPENDED: 'bg-red-100 text-red-700',
  CANCELLED: 'bg-slate-100 text-slate-500',
}

export default function AdminOrgsPage() {
  const [orgs, setOrgs]       = useState<AdminOrg[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch]   = useState('')
  const [planFilter, setPlanFilter]     = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const navigate = useNavigate()

  const load = (plan = planFilter, status = statusFilter) => {
    setLoading(true)
    getAdminOrgs(plan || undefined, status || undefined)
      .then(r => setOrgs(r.data.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const filtered = orgs.filter(o =>
    o.name.toLowerCase().includes(search.toLowerCase()) ||
    o.slug.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Organisations</h1>
        <p className="text-slate-500 mt-1 text-sm">{orgs.length} total</p>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3 items-center">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input
            value={search} onChange={e => setSearch(e.target.value)}
            placeholder="Search orgs…"
            className="pl-9 pr-3 py-2 text-sm border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
          />
        </div>
        <select value={planFilter}
          onChange={e => { setPlanFilter(e.target.value); load(e.target.value, statusFilter) }}
          className="px-3 py-2 text-sm border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500">
          <option value="">All plans</option>
          <option value="FREE">Free</option>
          <option value="PRO">Pro</option>
          <option value="ENTERPRISE">Enterprise</option>
        </select>
        <select value={statusFilter}
          onChange={e => { setStatusFilter(e.target.value); load(planFilter, e.target.value) }}
          className="px-3 py-2 text-sm border border-slate-200 rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500">
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="SUSPENDED">Suspended</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </div>

      {loading ? (
        <div className="flex items-center justify-center h-32">
          <div className="w-6 h-6 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : (
        <div className="border border-slate-200 rounded-xl overflow-hidden bg-white">
          <table className="w-full text-left">
            <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider">
              <tr>
                <th className="py-3 px-4">Organisation</th>
                <th className="py-3 px-4">Plan</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Members</th>
                <th className="py-3 px-4 text-right">Jenkins</th>
                <th className="py-3 px-4 text-right">Builds</th>
                <th className="py-3 px-4">Created</th>
                <th className="py-3 px-4" />
              </tr>
            </thead>
            <tbody>
              {filtered.map(org => (
                <tr key={org.id}
                  className="border-t border-slate-100 hover:bg-slate-50 cursor-pointer"
                  onClick={() => navigate(`/admin/orgs/${org.id}`)}>
                  <td className="py-3 px-4">
                    <p className="text-sm font-medium text-slate-900">{org.name}</p>
                    <p className="text-xs text-slate-400">{org.slug}</p>
                  </td>
                  <td className="py-3 px-4">
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${PLAN_COLORS[org.plan]}`}>
                      {org.plan}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${STATUS_COLORS[org.status]}`}>
                      {org.status}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-sm text-slate-600 text-right">{org.memberCount}</td>
                  <td className="py-3 px-4 text-sm text-slate-600 text-right">{org.jenkinsConfigCount}</td>
                  <td className="py-3 px-4 text-sm text-slate-600 text-right">{org.buildCount.toLocaleString()}</td>
                  <td className="py-3 px-4 text-sm text-slate-400">
                    {new Date(org.createdAt).toLocaleDateString()}
                  </td>
                  <td className="py-3 px-4">
                    <ChevronRight className="w-4 h-4 text-slate-300" />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {filtered.length === 0 && (
            <p className="text-center py-10 text-sm text-slate-400">No organisations found.</p>
          )}
        </div>
      )}
    </div>
  )
}
