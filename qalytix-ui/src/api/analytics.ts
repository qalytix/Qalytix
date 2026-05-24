import axiosInstance from './axiosInstance';
import type { AnalyticsSummaryResponse } from '../types/analytics';

export async function getAnalyticsSummary(
  days: number,
  jobId?: number
): Promise<AnalyticsSummaryResponse> {
  const params: Record<string, unknown> = { days };
  if (jobId !== undefined) params.jobId = jobId;
  const res = await axiosInstance.get('/analytics/summary', { params });
  return res.data.data;
}
