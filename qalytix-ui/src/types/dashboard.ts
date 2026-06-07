import type { BuildStatus } from './jenkins'

export interface RecentBuild {
  buildId: number
  jobId: number
  jobName: string
  buildNumber: number
  status: BuildStatus
  durationMs: number | null
  startedAt: string | null
}

export interface DashboardStats {
  activeBuilds: number
  todayTotal: number
  todaySuccess: number
  todayFailure: number
  recentBuilds: RecentBuild[]
}

export interface DayStatus {
  date: string
  status: BuildStatus | null
}

export interface JobBuildHistory {
  jobId: number
  jobName: string
  history: DayStatus[]
}

export interface BuildEvent {
  buildId: number
  jobId: number
  orgId: number
  jobName: string
  buildNumber: number
  status: BuildStatus
  durationMs: number | null
  startedAt: string | null
  finishedAt: string | null
}
