import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CreditCard, Users, Server, Clock, ExternalLink, ArrowUpCircle } from 'lucide-react'
import { getBillingInfo, createPortalSession } from '../../api/billing'
import type { BillingInfo } from '../../types/billing'
import { PLAN_META } from '../../types/billing'
import PlanLimitBanner from '../../components/common/PlanLimitBanner'

// ── helpers ───────────────────────────────────────────────────────────────────

function statusBadge(status: BillingInfo['status']) {
  const map: Record<BillingInfo['status'], string> = {
    ACTIVE:    'bg-emerald-100 text-emerald-700',
    TRIALING:  'bg-blue-100 text-blue-700',
    PAST_DUE:  'bg-amber-100 text-amber-700',
    UNPAID:    'bg-red-100 text-red-700',
    CANCELLED: 'bg-slate-100 text-slate-500',
  }
  return map[status] ?? 'bg-slate-100 text-slate-500'
}

function UsageMeter({
  label, icon: Icon, used, limit, unit,
}: {
  label: string
  icon: React.ElementType
  used: number
  limit: number   // -1 = unlimited
  unit?: string
}) {
  const unlimited = limit === -1
  const pct = unlimited ? 0 : Math.min((used / limit) * 100, 100)
  const nearLimit = !unlimited && pct >= 80
  const atLimit   = !unlimited && pct >= 100

  return (
    <div className="bg-white border border-slate-200 rounded-xl p-5">
      <div className="flex items-center gap-2 mb-3">
        <Icon className="w-4 h-4 text-slate-400" />
        <span className="text-sm font-medium text-slate-700">{label}</span>
      </div>
      <div className="flex items-end justify-between mb-2">
        <span className={`text-2xl font-bold ${atLimit ? 'text-red-600' : 'text-slate-900'}`}>
          {used}
        </span>
        <span className="text-sm text-slate-400">
          {unlimited ? '∞ unlimited' : `/ ${limit}${unit ? ' ' + unit : ''}`}
        </span>
      </div>
      {!unlimited && (
        <div className="w-full h-1.5 bg-slate-100 rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full transition-all ${
              atLimit ? 'bg-red-500' : nearLimit ? 'bg-amber-400' : 'bg-emerald-500'
            }`}
            style={{ width: `${pct}%` }}
          />
        </div>
      )}
    </div>
  )
}

// ── main component ────────────────────────────────────────────────────────────

export default function BillingPage() {
  const [billing, setBilling]   = useState<BillingInfo | null>(null)
  const [loading, setLoading]   = useState(true)
  const [portalLoading, setPortalLoading] = useState(false)
  const [error, setError]       = useState<string | null>(null)
  const navigate = useNavigate()

  useEffect(() => {
    getBillingInfo()
      .then(r => setBilling(r.data.data))
      .catch(() => setError('Failed to load billing information.'))
      .finally(() => setLoading(false))
  }, [])

  const handlePortal = async () => {
    if (!billing?.stripeConfigured) return
    setPortalLoading(true)
    try {
      const res = await createPortalSession()
      window.location.href = res.data.data.checkoutUrl
    } catch {
      setError('Failed to open billing portal.')
    } finally {
      setPortalLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-40">
        <div className="w-6 h-6 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  if (error || !billing) {
    return <p className="text-red-500 text-sm">{error ?? 'Unknown error'}</p>
  }

  const planMeta = PLAN_META.find(p => p.id === billing.plan)!
  const periodEnd = billing.currentPeriodEnd
    ? new Date(billing.currentPeriodEnd).toLocaleDateString()
    : null

  // Limit warnings
  const jenkinsAtLimit  = billing.jenkinsConnectionsLimit !== -1 &&
                          billing.jenkinsConnectionsUsed >= billing.jenkinsConnectionsLimit
  const memberAtLimit   = billing.membersLimit !== -1 &&
                          billing.membersUsed >= billing.membersLimit
  const jenkinsNearLimit = !jenkinsAtLimit && billing.jenkinsConnectionsLimit !== -1 &&
                           billing.jenkinsConnectionsUsed / billing.jenkinsConnectionsLimit >= 0.8
  const memberNearLimit  = !memberAtLimit && billing.membersLimit !== -1 &&
                           billing.membersUsed / billing.membersLimit >= 0.8

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-900">Billing & Plan</h1>
        <p className="text-slate-500 mt-1 text-sm">Manage your subscription and usage.</p>
      </div>

      {/* Limit banners */}
      {jenkinsAtLimit && (
        <PlanLimitBanner
          blocking
          message="You've reached your Jenkins connections limit."
          ctaLabel="Upgrade plan"
          onCta={() => navigate('/billing/upgrade')}
        />
      )}
      {memberAtLimit && (
        <PlanLimitBanner
          blocking
          message="You've reached your team members limit."
          ctaLabel="Upgrade plan"
          onCta={() => navigate('/billing/upgrade')}
        />
      )}
      {jenkinsNearLimit && !jenkinsAtLimit && (
        <PlanLimitBanner
          message={`You're using ${billing.jenkinsConnectionsUsed} of ${billing.jenkinsConnectionsLimit} Jenkins connections.`}
          ctaLabel="Upgrade plan"
          onCta={() => navigate('/billing/upgrade')}
        />
      )}
      {memberNearLimit && !memberAtLimit && (
        <PlanLimitBanner
          message={`You're using ${billing.membersUsed} of ${billing.membersLimit} team members.`}
          ctaLabel="Upgrade plan"
          onCta={() => navigate('/billing/upgrade')}
        />
      )}

      {/* Current plan card */}
      <div className="bg-white border border-slate-200 rounded-xl p-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <CreditCard className="w-5 h-5 text-indigo-500" />
              <h2 className="text-lg font-semibold text-slate-900">
                {planMeta.label} Plan
              </h2>
              <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${statusBadge(billing.status)}`}>
                {billing.status.replace('_', ' ')}
              </span>
            </div>
            {periodEnd && (
              <p className="text-sm text-slate-500 ml-8">
                {billing.status === 'CANCELLED' ? 'Access ends' : 'Renews'} {periodEnd}
                {' · '}{billing.billingPeriod === 'ANNUAL' ? 'Annual' : 'Monthly'} billing
              </p>
            )}
            {billing.plan === 'FREE' && (
              <p className="text-sm text-slate-500 ml-8">No credit card required</p>
            )}
          </div>

          <div className="flex gap-2 shrink-0">
            {billing.plan !== 'ENTERPRISE' && (
              <button
                onClick={() => navigate('/billing/upgrade')}
                className="flex items-center gap-1.5 px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 transition-colors"
              >
                <ArrowUpCircle className="w-4 h-4" />
                Upgrade
              </button>
            )}
            {billing.stripeConfigured && billing.plan !== 'FREE' && (
              <button
                onClick={handlePortal}
                disabled={portalLoading}
                className="flex items-center gap-1.5 px-4 py-2 border border-slate-200 text-slate-700 text-sm font-medium rounded-lg hover:bg-slate-50 transition-colors disabled:opacity-50"
              >
                <ExternalLink className="w-4 h-4" />
                {portalLoading ? 'Opening…' : 'Manage billing'}
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Usage meters */}
      <div>
        <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wider mb-3">Usage</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <UsageMeter
            label="Jenkins Connections"
            icon={Server}
            used={billing.jenkinsConnectionsUsed}
            limit={billing.jenkinsConnectionsLimit}
          />
          <UsageMeter
            label="Team Members"
            icon={Users}
            used={billing.membersUsed}
            limit={billing.membersLimit}
          />
          <div className="bg-white border border-slate-200 rounded-xl p-5">
            <div className="flex items-center gap-2 mb-3">
              <Clock className="w-4 h-4 text-slate-400" />
              <span className="text-sm font-medium text-slate-700">Data Retention</span>
            </div>
            <div className="text-2xl font-bold text-slate-900">
              {billing.dataRetentionDays}
              <span className="text-base font-normal text-slate-400 ml-1">days</span>
            </div>
          </div>
        </div>
      </div>

      {/* Plan features summary */}
      <div className="bg-slate-50 border border-slate-200 rounded-xl p-5">
        <h3 className="text-sm font-semibold text-slate-700 mb-3">What's included in {planMeta.label}</h3>
        <ul className="grid grid-cols-1 sm:grid-cols-2 gap-2">
          {planMeta.features.map(f => (
            <li key={f} className="flex items-center gap-2 text-sm text-slate-600">
              <span className="w-1.5 h-1.5 bg-indigo-500 rounded-full shrink-0" />
              {f}
            </li>
          ))}
        </ul>
        {billing.plan !== 'ENTERPRISE' && (
          <button
            onClick={() => navigate('/billing/upgrade')}
            className="mt-4 text-sm text-indigo-600 hover:text-indigo-700 font-medium"
          >
            See all plans →
          </button>
        )}
      </div>
    </div>
  )
}
