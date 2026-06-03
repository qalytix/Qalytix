import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'

export default function SuperAdminRoute() {
  const isAuthenticated = useAuthStore(s => s.isAuthenticated)
  const superAdmin      = useAuthStore(s => s.superAdmin)

  if (!isAuthenticated) return <Navigate to="/login" replace />
  if (!superAdmin)      return <Navigate to="/dashboard" replace />
  return <Outlet />
}
