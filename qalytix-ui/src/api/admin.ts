import api from './axiosInstance'
import type { ApiResponse } from '../types/auth'
import type { AdminPlatformStats, AdminOrg, AdminOrgDetail } from '../types/admin'
import type { Plan } from '../types/auth'

export const getPlatformStats = () =>
  api.get<ApiResponse<AdminPlatformStats>>('/admin/stats')

export const getAdminOrgs = (plan?: string, status?: string) =>
  api.get<ApiResponse<AdminOrg[]>>('/admin/orgs', {
    params: { ...(plan ? { plan } : {}), ...(status ? { status } : {}) },
  })

export const getAdminOrgDetail = (orgId: number) =>
  api.get<ApiResponse<AdminOrgDetail>>(`/admin/orgs/${orgId}`)

export const changePlan = (orgId: number, plan: Plan) =>
  api.patch<ApiResponse<AdminOrg>>(`/admin/orgs/${orgId}/plan`, { plan })
