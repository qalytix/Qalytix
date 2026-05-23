import { create } from 'zustand'
import type { OrgInfo } from '../types/auth'

interface OrgState {
  org: OrgInfo | null
  setOrg: (org: OrgInfo) => void
  clearOrg: () => void
}

export const useOrgStore = create<OrgState>()((set) => ({
  org: null,
  setOrg: (org) => set({ org }),
  clearOrg: () => set({ org: null }),
}))
