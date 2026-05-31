import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Check, Zap } from 'lucide-react'
import { createCheckoutSession } from '../../api/billing'
import { PLAN_META } from '../../types/billing'
import type { BillingPeriod, UpgradePlan } from '../../types/billing'
import { useOrgStore } from '../../stores/orgStore'

export default function PlanComparePage() {
  const [period, setPeriod]     = useState<BillingPeriod>('MONTHLY')
  const [loading, setLoading]   = useState<string | null>(null)   // plan id being purchased
  const [error, setError]       = useState<string | null>(null)
  const navigate = useNavigate()
  const currentPlan = useOrgStore(s => s.org?.plan ?? 'FREE')

  const handleUpgrade = async (planId: UpgradePlan) => {
    setLoading(planId)
    setError(null)
    try {
      const res = await createCheckoutSession({ plan: planId, billingPeriod: period })
      window.location.href = res.data.data.checkoutUrl
    } catch {
      setError('Failed to start checkout. Please try again.')
    } finally {
      setLoading(null)
    }
  }

  const annualSaving = (monthly: number, annual: number) =>
    Math.round(((monthly - annual) / monthly) * 100)

  return (
    <div className="space-y-8">
      <div className="text-center">
        <h1 className="text-3xl font-bold text-slate-900 mb-2">Choose your plan</h1>
        <p className="text-slate-500">Scale as your team grows. Upgrade or downgrade at any time.</p>
      </div>

      {/* Billing period toggle */}
      <div className="flex justify-center">
        <div className="inline-flex items-center bg-slate-100 rounded-lg p-1 gap-1">
          <button
            onClick={() => setPeriod('MONTHLY')}
            className={`px-5 py-2 rounded-md text-sm font-medium transition-colors ${
              period === 'MONTHLY'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            Monthly
          </button>
          <button
            onClick={() => setPeriod('ANNUAL')}
            className={`flex items-center gap-1.5 px-5 py-2 rounded-md text-sm font-medium transition-colors ${
              period === 'ANNUAL'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            Annual
            <span className="bg-emerald-100 text-emerald-700 text-xs font-semibold px-1.5 py-0.5 rounded-full">
              Save 20%
            </span>
          </button>
        </div>
      </div>

      {error && (
        <p className="text-center text-sm text-red-500">{error}</p>
      )}

      {/* Plan cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto">
        {PLAN_META.map(plan => {
          const isCurrent  = plan.id === currentPlan
          const isEnterprise = plan.id === 'ENTERPRISE'
          const price = period === 'ANNUAL' ? plan.annualPrice : plan.monthlyPrice
          const isLoading = loading === plan.id

          return (
            <div
              key={plan.id}
              className={`relative flex flex-col rounded-2xl border p-6 ${
                plan.highlighted
                  ? 'border-indigo-500 bg-indigo-50 shadow-lg shadow-indigo-100'
                  : 'border-slate-200 bg-white'
              }`}
            >
              {plan.highlighted && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                  <span className="flex items-center gap-1 bg-indigo-600 text-white text-xs font-bold px-3 py-1 rounded-full">
                    <Zap className="w-3 h-3" /> Most popular
                  </span>
                </div>
              )}

              <div className="mb-4">
                <h2 className="text-lg font-bold text-slate-900 mb-1">{plan.label}</h2>
                {price !== null ? (
                  <div className="flex items-end gap-1">
                    <span className="text-4xl font-extrabold text-slate-900">${price}</span>
                    <span className="text-slate-400 text-sm pb-1">/mo</span>
                  </div>
                ) : (
                  <span className="text-2xl font-bold text-slate-900">Custom</span>
                )}
                {period === 'ANNUAL' && plan.monthlyPrice && plan.annualPrice && plan.monthlyPrice > 0 && (
                  <p className="text-xs text-emerald-600 font-medium mt-1">
                    Save {annualSaving(plan.monthlyPrice, plan.annualPrice)}% vs monthly
                  </p>
                )}
              </div>

              <ul className="space-y-2.5 flex-1 mb-6">
                {plan.features.map(f => (
                  <li key={f} className="flex items-start gap-2 text-sm text-slate-600">
                    <Check className={`w-4 h-4 mt-0.5 shrink-0 ${
                      plan.highlighted ? 'text-indigo-500' : 'text-emerald-500'
                    }`} />
                    {f}
                  </li>
                ))}
              </ul>

              {isCurrent ? (
                <div className="text-center text-sm font-medium text-slate-400 border border-slate-200 rounded-lg py-2.5">
                  Current plan
                </div>
              ) : isEnterprise ? (
                <a
                  href="mailto:hello@qalytix.io?subject=Enterprise plan enquiry"
                  className="block text-center py-2.5 px-4 rounded-lg border border-indigo-600 text-indigo-600 text-sm font-semibold hover:bg-indigo-50 transition-colors"
                >
                  Contact us
                </a>
              ) : (
                <button
                  onClick={() => handleUpgrade(plan.id as UpgradePlan)}
                  disabled={!!loading}
                  className={`py-2.5 px-4 rounded-lg text-sm font-semibold transition-colors disabled:opacity-50 ${
                    plan.highlighted
                      ? 'bg-indigo-600 text-white hover:bg-indigo-700'
                      : 'bg-slate-900 text-white hover:bg-slate-700'
                  }`}
                >
                  {isLoading ? 'Redirecting…' : `Upgrade to ${plan.label}`}
                </button>
              )}
            </div>
          )
        })}
      </div>

      <div className="text-center">
        <button
          onClick={() => navigate('/billing')}
          className="text-sm text-slate-400 hover:text-slate-600"
        >
          ← Back to billing
        </button>
      </div>
    </div>
  )
}
