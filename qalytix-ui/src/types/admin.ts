import type { Plan, OrgStatus, MemberRole } from './auth'
import type { SubscriptionStatus } from './billing'

export interface AdminPlatformStats {
  totalOrgs: number
  activeSubscriptions: number
  totalUsers: number
  totalBuilds: number
  totalTestResults: number
  buildsLast24h: number
  testResultsLast24h: number
}

export interface AdminOrg {
  id: number
  name: string
  slug: string
  plan: Plan
  status: OrgStatus
  memberCount: number
  jenkinsConfigCount: number
  buildCount: number
  createdAt: string
}

export interface AdminOrgDetail extends AdminOrg {
  subscriptionStatus: SubscriptionStatus | null
  stripeCustomerId: string | null
  testResultCount: number
  members: AdminMember[]
  jenkinsConfigs: AdminJenkinsConfig[]
}

export interface AdminMember {
  id: number
  userId: number
  email: string
  fullName: string
  role: MemberRole
  status: string
  joinedAt: string
}

export interface AdminJenkinsConfig {
  id: number
  name: string
  url: string
  active: boolean
}
