import api from './axiosInstance'
import type { ApiResponse } from '../types/auth'
import type { Member, Invitation, InviteRequest, UpdateRoleRequest } from '../types/members'

export const getMembers = () =>
  api.get<ApiResponse<Member[]>>('/orgs/me/members')

export const updateMemberRole = (memberId: number, data: UpdateRoleRequest) =>
  api.patch<ApiResponse<Member>>(`/orgs/me/members/${memberId}/role`, data)

export const removeMember = (memberId: number) =>
  api.delete<ApiResponse<void>>(`/orgs/me/members/${memberId}`)

export const getPendingInvitations = () =>
  api.get<ApiResponse<Invitation[]>>('/orgs/me/invitations')

export const sendInvitation = (data: InviteRequest) =>
  api.post<ApiResponse<Invitation>>('/orgs/me/invitations', data)

export const revokeInvitation = (id: number) =>
  api.delete<ApiResponse<void>>(`/orgs/me/invitations/${id}`)

export const acceptInvitation = (data: { token: string; fullName?: string; password?: string }) =>
  api.post<ApiResponse<import('../types/auth').AuthResponse>>('/invitations/accept', data)
