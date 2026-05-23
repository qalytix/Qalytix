import axiosInstance from './axiosInstance'
import type { ApiResponse, AuthResponse, LoginRequest, RegisterRequest } from '../types/auth'

export const register = (data: RegisterRequest) =>
  axiosInstance.post<ApiResponse<AuthResponse>>('/auth/register', data)

export const login = (data: LoginRequest) =>
  axiosInstance.post<ApiResponse<AuthResponse>>('/auth/login', data)

export const logout = () =>
  axiosInstance.post<ApiResponse<void>>('/auth/logout')
