import { AlertTriangle, XCircle, X } from 'lucide-react'
import { useState } from 'react'

interface Props {
  /** If true, banner is red and the dismiss button is hidden (hard block). */
  blocking?: boolean
  message: string
  ctaLabel?: string
  onCta?: () => void
}

/**
 * Displayed when an org is approaching (warning) or has reached (blocking) a plan limit.
 *
 * Non-blocking (warning): amber, dismissible.
 * Blocking: red, cannot be dismissed — user must upgrade.
 */
export default function PlanLimitBanner({ blocking = false, message, ctaLabel, onCta }: Props) {
  const [dismissed, setDismissed] = useState(false)

  if (!blocking && dismissed) return null

  const bg    = blocking ? 'bg-red-50 border-red-200'   : 'bg-amber-50 border-amber-200'
  const text  = blocking ? 'text-red-800'               : 'text-amber-800'
  const icon  = blocking ? XCircle                      : AlertTriangle
  const Icon  = icon
  const iconC = blocking ? 'text-red-500'               : 'text-amber-500'
  const btnC  = blocking
    ? 'bg-red-600 hover:bg-red-700 text-white'
    : 'bg-amber-600 hover:bg-amber-700 text-white'

  return (
    <div className={`flex items-start gap-3 border rounded-xl px-4 py-3 ${bg}`}>
      <Icon className={`w-5 h-5 mt-0.5 shrink-0 ${iconC}`} />

      <p className={`flex-1 text-sm font-medium ${text}`}>{message}</p>

      <div className="flex items-center gap-2 shrink-0">
        {ctaLabel && onCta && (
          <button
            onClick={onCta}
            className={`text-xs font-semibold px-3 py-1.5 rounded-lg transition-colors ${btnC}`}
          >
            {ctaLabel}
          </button>
        )}
        {!blocking && (
          <button
            onClick={() => setDismissed(true)}
            className="text-amber-400 hover:text-amber-600"
            aria-label="Dismiss"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>
    </div>
  )
}
