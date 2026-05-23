import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthResponse, MemberRole, OrgInfo, UserInfo } from '../types/auth'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: UserInfo | null
  org: OrgInfo | null
  role: MemberRole | null
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
      isAuthenticated: false,

      setAuth: (response) =>
        set({
          accessToken: response.accessToken,
          refreshToken: response.refreshToken,
          user: response.user,
          org: response.org,
          role: response.role,
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
          isAuthenticated: false,
        }),
    }),
    {
      name: 'qalytix-auth',
      // Only persist refresh token and user/org info — not the access token
      partialize: (state) => ({
        refreshToken: state.refreshToken,
        user: state.user,
        org: state.org,
        role: state.role,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
)
