import { BarChart3, Box, LayoutDashboard, Radio, Settings, UserRound } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

type SidebarItemProps = { icon: LucideIcon; label: string; selected?: boolean; onClick: () => void }
export function SidebarItem({ icon: Icon, label, selected, onClick }: SidebarItemProps) { return <button aria-current={selected ? 'page' : undefined} className={`sidebar-item${selected ? ' sidebar-item-active' : ''}`} onClick={onClick} type="button"><Icon size={23} strokeWidth={2.05} /><span>{label}</span></button> }
const nav = [{ icon: LayoutDashboard, label: 'Dashboard' }, { icon: Box, label: 'Client Control' }, { icon: BarChart3, label: 'Statistics' }, { icon: Radio, label: 'Remote Control' }]
const bottomNav = [{ icon: UserRound, label: 'Account' }, { icon: Settings, label: 'Settings' }]

export function Sidebar({ selected, onSelect }: { selected: string; onSelect: (label: string) => void }) {
  return <aside className="sidebar"><div className="brand brand-main"><img src="/kairokk-logo.png" alt="KAIROKK" className="brand-mark" /><div><strong>KAIROKK</strong><span>CONTROL BEYOND LIMITS.</span></div></div><nav className="sidebar-nav" aria-label="Main navigation">{nav.map((item) => <SidebarItem key={item.label} {...item} selected={selected === item.label} onClick={() => onSelect(item.label)} />)}</nav><nav className="sidebar-bottom sidebar-account-nav" aria-label="Account navigation">{bottomNav.map((item) => <SidebarItem key={item.label} {...item} selected={selected === item.label} onClick={() => onSelect(item.label)} />)}</nav></aside>
}
