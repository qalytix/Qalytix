import type { DayStatus } from '../../types/dashboard'

interface Props {
  history: DayStatus[]
}

const STATUS_COLOR: Record<string, string> = {
  SUCCESS:     'bg-green-500',
  FAILURE:     'bg-red-500',
  UNSTABLE:    'bg-yellow-400',
  ABORTED:     'bg-slate-300',
  IN_PROGRESS: 'bg-blue-400 animate-pulse',
  UNKNOWN:     'bg-slate-200',
}

const STATUS_LABEL: Record<string, string> = {
  SUCCESS:     'Success',
  FAILURE:     'Failure',
  UNSTABLE:    'Unstable',
  ABORTED:     'Aborted',
  IN_PROGRESS: 'Running',
  UNKNOWN:     'Unknown',
}

function formatDate(date: string): string {
  return new Date(`${date}T00:00:00Z`).toLocaleDateString(undefined, {
    month: 'short', day: 'numeric', timeZone: 'UTC',
  })
}

export default function BuildHistoryStrip({ history }: Props) {
  return (
    <div className="flex items-center gap-1.5">
      {history.map(day => {
        const colorClass = day.status ? (STATUS_COLOR[day.status] ?? STATUS_COLOR.UNKNOWN) : 'bg-slate-100'
        const label = day.status ? STATUS_LABEL[day.status] ?? day.status : 'No build'
        return (
          <div
            key={day.date}
            title={`${formatDate(day.date)} — ${label}`}
            className={`w-4 h-4 rounded-sm ${colorClass}`}
          />
        )
      })}
    </div>
  )
}
