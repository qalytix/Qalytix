import api from './axiosInstance'
import type { ApiResponse } from '../types/auth'
import type { DashboardStats, JobBuildHistory } from '../types/dashboard'

export const getDashboardStats = (testJobsOnly = false) =>
  api.get<ApiResponse<DashboardStats>>('/dashboard/stats', {
    params: testJobsOnly ? { testJobsOnly: true } : undefined,
  })

export const getBuildHistory = (testJobsOnly = false, days = 10) =>
  api.get<ApiResponse<JobBuildHistory[]>>('/dashboard/build-history', {
    params: { testJobsOnly, days },
  })
