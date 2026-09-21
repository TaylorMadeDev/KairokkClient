import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { cn } from '../lib/cn'

export function Button({ children, variant = 'outline', className, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { children: ReactNode; variant?: 'primary' | 'outline' }) {
  return <button className={cn('button', `button-${variant}`, className)} type="button" {...props}>{children}</button>
}

export function StatusBadge({ children, tone = 'green' }: { children: ReactNode; tone?: 'green' | 'blue' | 'red' }) { return <span className={`status-badge status-${tone}`}><span className="status-dot" />{children}</span> }
