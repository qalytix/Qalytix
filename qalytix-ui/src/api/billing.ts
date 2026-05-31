import api from './axiosInstance'
import type { ApiResponse } from '../types/auth'
import type { BillingInfo, CheckoutSession, CreateCheckoutRequest } from '../types/billing'

export const getBillingInfo = () =>
  api.get<ApiResponse<BillingInfo>>('/billing/plan')

export const createCheckoutSession = (request: CreateCheckoutRequest) =>
  api.post<ApiResponse<CheckoutSession>>('/billing/checkout', request)

export const createPortalSession = () =>
  api.post<ApiResponse<CheckoutSession>>('/billing/portal')
