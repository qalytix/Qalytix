export interface JobReportRow {
  jobName: string
  totalTests: number
  passed: number
  failed: number
  passRate: number
}

export interface ModuleReportRow {
  moduleName: string
  totalTests: number
  passed: number
  passRate: number
}

export interface FlakyReportRow {
  testSuite: string
  testName: string
  totalRuns: number
  failCount: number
  flakinessScore: number
}

export interface ReportSummary {
  fromDate: string
  toDate: string
  totalRuns: number
  totalPassed: number
  totalFailed: number
  totalSkipped: number
  overallPassRate: number
  jobRows: JobReportRow[]
  moduleRows: ModuleReportRow[]
  flakyRows: FlakyReportRow[]
}
