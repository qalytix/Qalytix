import type { BuildStatus } from '../../types/jenkins'

interface Props {
  status: BuildStatus | null
}

const config: Record<string, { label: string; className: string }> = {
  SUCCESS:     { label: 'Success',     className: 'bg-green-100 text-green-700' },
  FAILURE:     { label: 'Failure',     className: 'bg-red-100 text-red-700' },
  UNSTABLE:    { label: 'Unstable',    className: 'bg-yellow-100 text-yellow-700' },
  ABORTED:     { label: 'Aborted',     className: 'bg-slate-100 text-slate-500' },
  IN_PROGRESS: { label: 'Running',     className: 'bg-blue-100 text-blue-700 animate-pulse' },
  UNKNOWN:     { label: 'Unknown',     className: 'bg-slate-100 text-slate-500' },
}

export default function BuildStatusBadge({ status }: Props) {
  const c = (status && config[status]) ?? config.UNKNOWN
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${c.className}`}>
      {c.label}
    </span>
  )
}
