import { CheckCircle2, X } from 'lucide-react'
export type Toast = { id: string; title: string; detail?: string }
export function ToastViewport({ toasts, onDismiss }: { toasts: Toast[]; onDismiss: (id: string) => void }) { return <div className="toast-viewport" aria-live="polite">{toasts.map((toast) => <div className="toast" key={toast.id}><CheckCircle2 size={18} /><div><strong>{toast.title}</strong>{toast.detail && <span>{toast.detail}</span>}</div><button aria-label="Dismiss notification" onClick={() => onDismiss(toast.id)} type="button"><X size={15} /></button></div>)}</div> }
