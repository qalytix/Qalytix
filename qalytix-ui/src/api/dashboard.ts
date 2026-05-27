import api from './axiosInstance'
import type { ApiResponse } from '../types/auth'
import type { DashboardStats } from '../types/dashboard'

export const getDashboardStats = (testJobsOnly = false) =>
  api.get<ApiResponse<DashboardStats>>('/dashboard/stats', {
    params: testJobsOnly ? { testJobsOnly: true } : undefined,
  })
