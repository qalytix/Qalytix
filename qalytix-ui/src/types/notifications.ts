export type NotificationChannel = 'TEAMS' | 'SLACK'
export type TriggerEvent = 'BUILD_FAILURE' | 'CONSECUTIVE_FAILURES' | 'FLAKY_THRESHOLD' | 'TEST_SEND'

export interface NotificationConfig {
  id: number
  name: string
  channel: NotificationChannel
  onBuildFailure: boolean
  onConsecutiveFailures: boolean
  consecutiveThreshold: number
  onFlakyThreshold: boolean
  flakyScoreThreshold: number
  enabled: boolean
  createdAt: string
}

export interface NotificationEvent {
  id: number
  channel: NotificationChannel
  triggerEvent: TriggerEvent
  jobName: string | null
  buildNumber: number | null
  payloadSummary: string | null
  success: boolean
  errorMessage: string | null
  sentAt: string
}

export interface CreateNotificationConfigRequest {
  name: string
  channel: NotificationChannel
  webhookUrl: string
  onBuildFailure: boolean
  onConsecutiveFailures: boolean
  consecutiveThreshold: number
  onFlakyThreshold: boolean
  flakyScoreThreshold: number
}
