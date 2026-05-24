export interface FailureTrendPoint {
  date: string;
  total: number;
  failed: number;
  passed: number;
}

export interface TopFailingTest {
  testSuite: string;
  testName: string;
  failCount: number;
}

export interface FlakyTest {
  testSuite: string;
  testName: string;
  totalRuns: number;
  failCount: number;
  passCount: number;
  flakinessScore: number;
}

export interface ModuleStability {
  moduleName: string;
  total: number;
  passed: number;
  passRate: number;
}

export interface AnalyticsSummaryResponse {
  failureTrend: FailureTrendPoint[];
  topFailingTests: TopFailingTest[];
  flakyTests: FlakyTest[];
  moduleStability: ModuleStability[];
}
