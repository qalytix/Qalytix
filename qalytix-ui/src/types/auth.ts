export type MemberRole = 'OWNER' | 'ADMIN' | 'MEMBER'
export type Plan = 'FREE' | 'PRO' | 'ENTERPRISE'
export type OrgStatus = 'ACTIVE' | 'SUSPENDED' | 'CANCELLED'

export interface UserInfo {
  id: number
  email: string
  fullName: string
}

export interface OrgInfo {
  id: number
  name: string
  slug: string
  plan: Plan
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: UserInfo
  org: OrgInfo
  role: MemberRole
  superAdmin: boolean
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string
  timestamp: string
}

export interface RegisterRequest {
  email: string
  password: string
  fullName: string
  orgName: string
}

export interface LoginRequest {
  email: string
  password: string
}
