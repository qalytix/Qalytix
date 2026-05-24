import api from './axiosInstance'
import type { ApiResponse } from '../types/auth'
import type {
  JenkinsConfig,
  CreateJenkinsConfigPayload,
  UpdateJenkinsConfigPayload,
  Job,
  Build,
  PageResponse,
} from '../types/jenkins'

export const getJenkinsConfigs = () =>
  api.get<ApiResponse<JenkinsConfig[]>>('/jenkins')

export const createJenkinsConfig = (payload: CreateJenkinsConfigPayload) =>
  api.post<ApiResponse<JenkinsConfig>>('/jenkins', payload)

export const updateJenkinsConfig = (id: number, payload: UpdateJenkinsConfigPayload) =>
  api.patch<ApiResponse<JenkinsConfig>>(`/jenkins/${id}`, payload)

export const deleteJenkinsConfig = (id: number) =>
  api.delete(`/jenkins/${id}`)

export const testJenkinsConnection = (id: number) =>
  api.post<ApiResponse<void>>(`/jenkins/${id}/test-connection`)

export const syncJenkins = (id: number) =>
  api.post<ApiResponse<void>>(`/jenkins/${id}/sync`)

export const getJobs = () =>
  api.get<ApiResponse<Job[]>>('/jobs')

export const getJob = (id: number) =>
  api.get<ApiResponse<Job>>(`/jobs/${id}`)

export const getBuilds = (jobId: number, page = 0, size = 20) =>
  api.get<ApiResponse<PageResponse<Build>>>(`/jobs/${jobId}/builds`, {
    params: { page, size, sort: 'buildNumber,desc' },
  })
