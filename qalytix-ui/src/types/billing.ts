import type { Plan } from './auth'

export type SubscriptionStatus = 'TRIALING' | 'ACTIVE' | 'PAST_DUE' | 'CANCELLED' | 'UNPAID'
export type BillingPeriod = 'MONTHLY' | 'ANNUAL'

export interface BillingInfo {
  plan: Plan
  status: SubscriptionStatus
  billingPeriod: BillingPeriod
  currentPeriodEnd: string | null
  trialEndsAt: string | null
  stripeConfigured: boolean

  // Usage
  jenkinsConnectionsUsed: number
  jenkinsConnectionsLimit: number   // -1 = unlimited
  membersUsed: number
  membersLimit: number              // -1 = unlimited
  dataRetentionDays: number
}

export interface CheckoutSession {
  checkoutUrl: string
}

export type UpgradePlan = 'PRO' | 'ENTERPRISE'

export interface CreateCheckoutRequest {
  plan: UpgradePlan
  billingPeriod: BillingPeriod
}

// Plan metadata for the comparison table
export interface PlanMeta {
  id: Plan
  label: string
  monthlyPrice: number | null   // null = custom/contact
  annualPrice: number | null
  jenkinsLimit: number | null   // null = unlimited
  memberLimit: number | null
  retentionDays: number
  features: string[]
  highlighted: boolean
}

export const PLAN_META: PlanMeta[] = [
  {
    id: 'FREE',
    label: 'Free',
    monthlyPrice: 0,
    annualPrice: 0,
    jenkinsLimit: 1,
    memberLimit: 3,
    retentionDays: 7,
    features: ['1 Jenkins connection', '3 team members', '7-day data retention', 'Basic dashboard'],
    highlighted: false,
  },
  {
    id: 'PRO',
    label: 'Pro',
    monthlyPrice: 49,
    annualPrice: 39,
    jenkinsLimit: 5,
    memberLimit: 15,
    retentionDays: 90,
    features: ['5 Jenkins connections', '15 team members', '90-day data retention', 'Full analytics', 'Failure trends & flaky tests', 'Email notifications'],
    highlighted: true,
  },
  {
    id: 'ENTERPRISE',
    label: 'Enterprise',
    monthlyPrice: null,
    annualPrice: null,
    jenkinsLimit: null,
    memberLimit: null,
    retentionDays: 365,
    features: ['Unlimited Jenkins connections', 'Unlimited members', '1-year data retention', 'All Pro features', 'SSO / SAML', 'Audit log', 'Priority support'],
    highlighted: false,
  },
]
