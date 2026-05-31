import api from './axiosInstance'
import type { ApiResponse } from '../types/auth'
import type { NotificationConfig, NotificationEvent, CreateNotificationConfigRequest } from '../types/notifications'

export const getNotificationConfigs = () =>
  api.get<ApiResponse<NotificationConfig[]>>('/notifications/configs')

export const createNotificationConfig = (data: CreateNotificationConfigRequest) =>
  api.post<ApiResponse<NotificationConfig>>('/notifications/configs', data)

export const updateNotificationConfig = (id: number, data: CreateNotificationConfigRequest) =>
  api.put<ApiResponse<NotificationConfig>>(`/notifications/configs/${id}`, data)

export const deleteNotificationConfig = (id: number) =>
  api.delete<ApiResponse<void>>(`/notifications/configs/${id}`)

export const testNotificationConfig = (id: number) =>
  api.post<ApiResponse<void>>(`/notifications/configs/${id}/test`)

export const getNotificationHistory = (limit = 50) =>
  api.get<ApiResponse<NotificationEvent[]>>('/notifications/history', { params: { limit } })
