import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { ArrowLeft, Server, Users, GitBranch, FlaskConical, CreditCard } from 'lucide-react'
import { getAdminOrgDetail, changePlan } from '../../api/admin'
import type { AdminOrgDetail } from '../../types/admin'
import type { Plan } from '../../types/auth'

const PLANS: Plan[] = ['FREE', 'PRO', 'ENTERPRISE']

export default function AdminOrgDetailPage() {
  const { orgId } = useParams<{ orgId: string }>()
  const navigate  = useNavigate()
  const [org, setOrg]       = useState<AdminOrgDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [planSaving, setPlanSaving] = useState(false)
  const [toast, setToast]   = useState<string | null>(null)

  useEffect(() => {
    getAdminOrgDetail(Number(orgId))
      .then(r => setOrg(r.data.data))
      .finally(() => setLoading(false))
  }, [orgId])

  const handlePlanChange = async (plan: Plan) => {
    if (!org || plan === org.plan) return
    setPlanSaving(true)
    try {
      await changePlan(org.id, plan)
      setOrg(o => o ? { ...o, plan } : o)
      setToast(`Plan changed to ${plan}`)
      setTimeout(() => setToast(null), 3000)
    } catch {
      setToast('Failed to change plan')
      setTimeout(() => setToast(null), 3000)
    } finally {
      setPlanSaving(false)
    }
  }

  if (loading) return (
    <div className="flex items-center justify-center h-40">
      <div className="w-6 h-6 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
    </div>
  )
  if (!org) return <p className="text-red-500 text-sm">Organisation not found.</p>

  return (
    <div className="space-y-6 max-w-4xl">
      {toast && (
        <div className="fixed top-4 right-4 z-50 bg-emerald-600 text-white text-sm font-medium px-4 py-3 rounded-xl shadow-lg">
          {toast}
        </div>
      )}

      <button onClick={() => navigate('/admin/orgs')}
        className="flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700">
        <ArrowLeft className="w-4 h-4" /> Back to organisations
      </button>

      {/* Header */}
      <div className="bg-white border border-slate-200 rounded-xl p-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold text-slate-900">{org.name}</h1>
            <p className="text-sm text-slate-400">/{org.slug} · ID {org.id}</p>
            <p className="text-xs text-slate-400 mt-1">Created {new Date(org.createdAt).toLocaleDateString()}</p>
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <span className={`text-xs font-semibold px-2 py-1 rounded-full ${
              org.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'
            }`}>{org.status}</span>
          </div>
        </div>
      </div>

      {/* Usage stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {[
          { icon: Users,       label: 'Members',      value: org.memberCount },
          { icon: Server,      label: 'Jenkins',      value: org.jenkinsConfigCount },
          { icon: GitBranch,   label: 'Builds',       value: org.buildCount },
          { icon: FlaskConical, label: 'Test Results', value: org.testResultCount },
        ].map(({ icon: Icon, label, value }) => (
          <div key={label} className="bg-white border border-slate-200 rounded-xl p-4">
            <div className="flex items-center gap-2 mb-1">
              <Icon className="w-4 h-4 text-slate-400" />
              <span className="text-xs text-slate-500">{label}</span>
            </div>
            <p className="text-2xl font-bold text-slate-900">{value.toLocaleString()}</p>
          </div>
        ))}
      </div>

      {/* Plan management */}
      <div className="bg-white border border-slate-200 rounded-xl p-6">
        <div className="flex items-center gap-2 mb-4">
          <CreditCard className="w-4 h-4 text-slate-400" />
          <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wider">Plan</h2>
        </div>
        <div className="flex items-center gap-4 flex-wrap">
          <p className="text-sm text-slate-600">Current: <strong>{org.plan}</strong></p>
          {org.subscriptionStatus && (
            <p className="text-sm text-slate-600">Subscription: <strong>{org.subscriptionStatus}</strong></p>
          )}
          {org.stripeCustomerId && (
            <p className="text-sm text-slate-400 font-mono text-xs">{org.stripeCustomerId}</p>
          )}
        </div>
        <div className="flex gap-2 mt-4">
          {PLANS.map(plan => (
            <button key={plan}
              onClick={() => handlePlanChange(plan)}
              disabled={planSaving || plan === org.plan}
              className={`px-4 py-2 text-sm font-medium rounded-lg border transition-colors disabled:opacity-50 ${
                plan === org.plan
                  ? 'bg-indigo-600 text-white border-indigo-600'
                  : 'border-slate-200 text-slate-700 hover:bg-slate-50'
              }`}>
              {plan}
            </button>
          ))}
        </div>
      </div>

      {/* Members */}
      {org.members.length > 0 && (
        <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
          <div className="px-5 py-4 border-b border-slate-100 flex items-center gap-2">
            <Users className="w-4 h-4 text-slate-400" />
            <h2 className="text-sm font-semibold text-slate-700">Members</h2>
          </div>
          <table className="w-full text-left">
            <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider">
              <tr>
                <th className="py-2.5 px-5">Name</th>
                <th className="py-2.5 px-5">Email</th>
                <th className="py-2.5 px-5">Role</th>
                <th className="py-2.5 px-5">Joined</th>
              </tr>
            </thead>
            <tbody>
              {org.members.map(m => (
                <tr key={m.id} className="border-t border-slate-100">
                  <td className="py-2.5 px-5 text-sm font-medium text-slate-900">{m.fullName}</td>
                  <td className="py-2.5 px-5 text-sm text-slate-500">{m.email}</td>
                  <td className="py-2.5 px-5">
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                      m.role === 'OWNER' ? 'bg-indigo-100 text-indigo-700' :
                      m.role === 'ADMIN' ? 'bg-amber-100 text-amber-700' :
                      'bg-slate-100 text-slate-600'
                    }`}>{m.role}</span>
                  </td>
                  <td className="py-2.5 px-5 text-sm text-slate-400">
                    {new Date(m.joinedAt).toLocaleDateString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Jenkins configs */}
      {org.jenkinsConfigs.length > 0 && (
        <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
          <div className="px-5 py-4 border-b border-slate-100 flex items-center gap-2">
            <Server className="w-4 h-4 text-slate-400" />
            <h2 className="text-sm font-semibold text-slate-700">Jenkins Configurations</h2>
          </div>
          <table className="w-full text-left">
            <thead className="bg-slate-50 text-xs font-semibold text-slate-500 uppercase tracking-wider">
              <tr>
                <th className="py-2.5 px-5">Name</th>
                <th className="py-2.5 px-5">URL</th>
                <th className="py-2.5 px-5">Status</th>
              </tr>
            </thead>
            <tbody>
              {org.jenkinsConfigs.map(c => (
                <tr key={c.id} className="border-t border-slate-100">
                  <td className="py-2.5 px-5 text-sm font-medium text-slate-900">{c.name}</td>
                  <td className="py-2.5 px-5 text-sm text-slate-500 font-mono text-xs">{c.url}</td>
                  <td className="py-2.5 px-5">
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                      c.active ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-400'
                    }`}>
                      {c.active ? 'Active' : 'Inactive'}
                    </span>
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
