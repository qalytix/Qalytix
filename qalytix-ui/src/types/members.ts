import type { MemberRole } from './auth'

export type MemberStatus = 'PENDING' | 'ACTIVE'

export interface Member {
  id: number
  userId: number
  email: string
  fullName: string
  role: MemberRole
  status: MemberStatus
  joinedAt: string
}

export interface Invitation {
  id: number
  email: string
  role: MemberRole
  token: string
  expiresAt: string
  createdAt: string
}

export interface InviteRequest {
  email: string
  role: MemberRole
}

export interface UpdateRoleRequest {
  role: MemberRole
}
