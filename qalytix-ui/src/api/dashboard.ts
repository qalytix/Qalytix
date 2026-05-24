import api from './axiosInstance'
import type { ApiResponse } from '../types/auth'
import type { DashboardStats } from '../types/dashboard'

export const getDashboardStats = () =>
  api.get<ApiResponse<DashboardStats>>('/dashboard/stats')
