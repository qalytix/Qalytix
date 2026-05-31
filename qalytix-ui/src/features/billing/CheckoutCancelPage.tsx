import { useNavigate } from 'react-router-dom'
import { XCircle } from 'lucide-react'

/**
 * Stripe redirects here when the user cancels the checkout flow.
 */
export default function CheckoutCancelPage() {
  const navigate = useNavigate()

  return (
    <div className="min-h-[60vh] flex flex-col items-center justify-center text-center gap-4">
      <div className="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center">
        <XCircle className="w-9 h-9 text-slate-400" />
      </div>
      <h1 className="text-2xl font-bold text-slate-900">Checkout cancelled</h1>
      <p className="text-slate-500 max-w-sm">
        No charge was made. You can upgrade whenever you're ready.
      </p>
      <div className="flex gap-3 mt-2">
        <button
          onClick={() => navigate('/billing/upgrade')}
          className="px-4 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700"
        >
          View plans
        </button>
        <button
          onClick={() => navigate('/billing')}
          className="px-4 py-2 border border-slate-200 text-slate-700 text-sm font-medium rounded-lg hover:bg-slate-50"
        >
          Back to billing
        </button>
      </div>
    </div>
  )
}
