import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom'
import LoginPage from './features/auth/LoginPage'
import RegisterPage from './features/auth/RegisterPage'
import ProtectedRoute from './components/common/ProtectedRoute'
import AppShell from './components/layout/AppShell'
import DashboardPage from './features/dashboard/DashboardPage'
import AnalyticsPage from './features/analytics/AnalyticsPage'
import JenkinsPage from './features/jenkins/JenkinsPage'
import JobsPage from './features/jenkins/JobsPage'
import BuildsPage from './features/jenkins/BuildsPage'
import MembersPage from './features/members/MembersPage'
import NotificationsPage from './features/notifications/NotificationsPage'
import ReportsPage from './features/reports/ReportsPage'
import BillingPage from './features/billing/BillingPage'
import PlanComparePage from './features/billing/PlanComparePage'
import CheckoutSuccessPage from './features/billing/CheckoutSuccessPage'
import CheckoutCancelPage from './features/billing/CheckoutCancelPage'

const router = createBrowserRouter([
  { path: '/login',    element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppShell />,
        children: [
          { index: true,                      element: <Navigate to="/dashboard" replace /> },
          { path: 'dashboard',                element: <DashboardPage /> },
          { path: 'analytics',                element: <AnalyticsPage /> },
          { path: 'jenkins',                  element: <JenkinsPage /> },
          { path: 'jobs',                     element: <JobsPage /> },
          { path: 'jobs/:jobId/builds',       element: <BuildsPage /> },
          { path: 'members',                  element: <MembersPage /> },
          { path: 'notifications',            element: <NotificationsPage /> },
          { path: 'reports',                  element: <ReportsPage /> },
          { path: 'billing',                  element: <BillingPage /> },
          { path: 'billing/upgrade',          element: <PlanComparePage /> },
          { path: 'billing/success',          element: <CheckoutSuccessPage /> },
          { path: 'billing/cancel',           element: <CheckoutCancelPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/login" replace /> },
])

export default function App() {
  return <RouterProvider router={router} />
}
