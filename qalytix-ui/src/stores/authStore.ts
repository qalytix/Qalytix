import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthResponse, MemberRole, OrgInfo, UserInfo } from '../types/auth'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: UserInfo | null
  org: OrgInfo | null
  role: MemberRole | null
  superAdmin: boolean
  isAuthenticated: boolean

  setAuth: (response: AuthResponse) => void
  setAccessToken: (token: string) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      org: null,
      role: null,
      superAdmin: false,
      isAuthenticated: false,

      setAuth: (response) =>
        set({
          accessToken: response.accessToken,
          refreshToken: response.refreshToken,
          user: response.user,
          org: response.org,
          role: response.role,
          superAdmin: response.superAdmin ?? false,
          isAuthenticated: true,
        }),

      setAccessToken: (token) => set({ accessToken: token }),

      clearAuth: () =>
        set({
          accessToken: null,
          refreshToken: null,
          user: null,
          org: null,
          role: null,
          superAdmin: false,
          isAuthenticated: false,
        }),
    }),
    {
      name: 'qalytix-auth',
      partialize: (state) => ({
        refreshToken: state.refreshToken,
        user: state.user,
        org: state.org,
        role: state.role,
        superAdmin: state.superAdmin,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
)
