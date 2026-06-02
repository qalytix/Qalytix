import axiosInstance from './axiosInstance'
import type { ApiResponse, AuthResponse, LoginRequest, RegisterRequest } from '../types/auth'

export const register = (data: RegisterRequest) =>
  axiosInstance.post<ApiResponse<AuthResponse>>('/auth/register', data)

export const login = (data: LoginRequest) =>
  axiosInstance.post<ApiResponse<AuthResponse>>('/auth/login', data)

export const logout = () =>
  axiosInstance.post<ApiResponse<void>>('/auth/logout')

export const forgotPassword = (email: string) =>
  axiosInstance.post<ApiResponse<void>>('/auth/forgot-password', { email })

export const resetPassword = (token: string, newPassword: string) =>
  axiosInstance.post<ApiResponse<void>>('/auth/reset-password', { token, newPassword })

export const changePassword = (currentPassword: string, newPassword: string) =>
  axiosInstance.post<ApiResponse<void>>('/auth/change-password', { currentPassword, newPassword })
