import { useEffect, useRef, useState, type RefObject } from 'react'
import { Bell, ChevronDown, LogOut, Search, UserRound } from 'lucide-react'

type TopBarProps = {
  searchRef: RefObject<HTMLInputElement | null>
  search: string
  username: string
  onSearchChange: (value: string) => void
  onSearchSubmit: () => void
  onNavigate: (page: string) => void
  onLogout: () => void
}

export function TopBar({ searchRef, search, username, onSearchChange, onSearchSubmit, onNavigate, onLogout }: TopBarProps) {
  const [accountOpen, setAccountOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  const initial = username.trim().charAt(0).toUpperCase() || 'K'

  useEffect(() => {
    const close = (event: MouseEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) {
        setAccountOpen(false)
        setNotificationsOpen(false)
      }
    }
    document.addEventListener('pointerdown', close)
    return () => document.removeEventListener('pointerdown', close)
  }, [])

  return <header className="topbar">
    <form className="search-control" onSubmit={(event) => { event.preventDefault(); onSearchSubmit() }}>
      <Search size={20} />
      <input aria-label="Search dashboard" onChange={(event) => onSearchChange(event.target.value)} placeholder="Search..." ref={searchRef} value={search} />
      <kbd>Ctrl K</kbd>
    </form>
    <div className="topbar-actions" ref={menuRef}>
      <div className="menu-wrap">
        <button aria-expanded={notificationsOpen} className="icon-button notification-button" onClick={() => { setNotificationsOpen((open) => !open); setAccountOpen(false) }} type="button" aria-label="Notifications"><Bell size={24} /><i /></button>
        {notificationsOpen && <div className="popover notification-popover"><strong>Notifications</strong><span>Your dashboard is up to date.</span><span>Client activity appears here.</span></div>}
      </div>
      <div className="menu-wrap">
        <button aria-expanded={accountOpen} className="user-menu" onClick={() => { setAccountOpen((open) => !open); setNotificationsOpen(false) }} type="button">
          <span className="avatar avatar-small">{initial}</span><span className="user-menu-copy"><strong>{username}</strong><small>Online</small></span><ChevronDown size={18} className={accountOpen ? 'chevron-open' : ''} />
        </button>
        {accountOpen && <div className="popover account-popover"><strong>{username}</strong><span>Kairokk account</span><button type="button" onClick={() => { onNavigate('Account'); setAccountOpen(false) }}><UserRound size={15} />View account</button><button type="button" onClick={onLogout}><LogOut size={15} />Log out</button></div>}
      </div>
    </div>
  </header>
}
