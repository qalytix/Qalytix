export type BuildStatus = 'SUCCESS' | 'FAILURE' | 'UNSTABLE' | 'ABORTED' | 'IN_PROGRESS' | 'UNKNOWN'

export interface JenkinsConfig {
  id: number
  name: string
  url: string
  username: string
  pollInterval: string
  active: boolean
  lastSyncedAt: string | null
  createdAt: string
}

export interface CreateJenkinsConfigPayload {
  name: string
  url: string
  username: string
  apiToken: string
  pollInterval?: string
}

export interface UpdateJenkinsConfigPayload {
  name?: string
  url?: string
  username?: string
  apiToken?: string
  pollInterval?: string
  active?: boolean
}

export interface Job {
  id: number
  jenkinsConfigId: number
  jenkinsJobName: string
  displayName: string | null
  url: string | null
  lastBuildNumber: number | null
  lastBuildStatus: BuildStatus | null
  lastBuildAt: string | null
  createdAt: string
}

export interface Build {
  id: number
  jobId: number
  buildNumber: number
  status: BuildStatus
  durationMs: number | null
  startedAt: string | null
  finishedAt: string | null
  url: string | null
  triggeredBy: string | null
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
