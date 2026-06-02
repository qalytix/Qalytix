import api from './axiosInstance'
import type { ApiResponse } from '../types/auth'
import type { ReportSummary } from '../types/reports'

interface ReportParams {
  jobId?: number | null
  from?: string
  to?: string
}

export const getReportSummary = (params: ReportParams) =>
  api.get<ApiResponse<ReportSummary>>('/reports/summary', {
    params: {
      ...(params.jobId ? { jobId: params.jobId } : {}),
      ...(params.from  ? { from: params.from }   : {}),
      ...(params.to    ? { to: params.to }        : {}),
    },
  })

export const downloadReportCsv = (params: ReportParams): void => {
  const base = api.defaults.baseURL ?? '/api/v1'
  const p = new URLSearchParams()
  if (params.jobId) p.set('jobId', String(params.jobId))
  if (params.from)  p.set('from', params.from)
  if (params.to)    p.set('to', params.to)

  // Trigger browser download by navigating to the CSV endpoint
  const token = localStorage.getItem('qalytix-auth')
    ? JSON.parse(localStorage.getItem('qalytix-auth')!)?.state?.refreshToken
    : null

  // Use fetch + blob to stream the CSV with auth headers
  const url = `${base}/reports/export/csv?${p.toString()}`
  import('../api/axiosInstance').then(({ default: axiosInst }) => {
    axiosInst.get(url, { responseType: 'blob' }).then(res => {
      const blob = new Blob([res.data], { type: 'text/csv' })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = `qalytix-report-${params.from ?? 'all'}-to-${params.to ?? 'today'}.csv`
      link.click()
      URL.revokeObjectURL(link.href)
    })
  })
}
