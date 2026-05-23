import axios from 'axios'
import { useAuthStore } from '../stores/authStore'

const axiosInstance = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// Attach access token to every request
axiosInstance.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// On 401: attempt token refresh then retry once
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config

    if (error.response?.status === 401 && !original._retry) {
      original._retry = true

      const refreshToken = useAuthStore.getState().refreshToken
      if (!refreshToken) {
        useAuthStore.getState().clearAuth()
        window.location.href = '/login'
        return Promise.reject(error)
      }

      try {
        const { data } = await axios.post('/api/v1/auth/refresh', { refreshToken })
        const newAccessToken = data.data.accessToken
        useAuthStore.getState().setAccessToken(newAccessToken)
        original.headers.Authorization = `Bearer ${newAccessToken}`
        return axiosInstance(original)
      } catch {
        useAuthStore.getState().clearAuth()
        window.location.href = '/login'
        return Promise.reject(error)
      }
    }

    return Promise.reject(error)
  },
)

export default axiosInstance
