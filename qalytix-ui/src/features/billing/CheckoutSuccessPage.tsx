import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { CheckCircle2 } from 'lucide-react'

/**
 * Stripe redirects here after a successful checkout.
 * We reload the org context (to pick up the new plan from the backend)
 * then send the user to the billing page.
 */
export default function CheckoutSuccessPage() {
  const navigate = useNavigate()

  useEffect(() => {
    // Small delay so the user reads the confirmation, then go to billing
    const t = setTimeout(() => navigate('/billing', { replace: true }), 3000)
    return () => clearTimeout(t)
  }, [navigate])

  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center text-center gap-4">
      <div className="w-16 h-16 bg-emerald-100 rounded-full flex items-center justify-center">
        <CheckCircle2 className="w-9 h-9 text-emerald-600" />
      </div>
      <h1 className="text-2xl font-bold text-slate-900">You're all set!</h1>
      <p className="text-slate-500 max-w-sm">
        Your subscription has been activated. You'll be redirected to billing in a moment.
      </p>
      <button
        onClick={() => navigate('/billing', { replace: true })}
        className="mt-2 text-sm text-indigo-600 hover:text-indigo-700 font-medium"
      >
        Go to billing →
      </button>
    </div>
  )
}
