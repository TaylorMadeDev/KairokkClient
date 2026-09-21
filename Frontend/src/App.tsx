import { useEffect, useRef, useState } from 'react'
import { AuthPage } from './components/AuthPage'
import { Dashboard, DashboardPanel } from './components/Dashboard'
import { DashboardOrbBackground } from './components/DashboardOrbBackground'
import { Sidebar } from './components/Sidebar'
import { TopBar } from './components/TopBar'
import { OperationalPage } from './components/OperationalPages'
import { LandingPage } from './components/LandingPage'
import { WorldViewPage } from './WorldView/WorldViewPage'
import { api, type Account, type Config, type DashboardData, type Script } from './lib/api'
export type ClientState = 'connected' | 'disconnected' | 'reconnecting'
const navigate = (path: string) => { window.history.pushState({}, '', path); window.dispatchEvent(new PopStateEvent('popstate')) }

function CollectionPage({ kind, entries, onSave, onDelete }: { kind: 'Configs' | 'Scripts'; entries: (Config | Script)[]; onSave: (name: string, content?: string) => Promise<void>; onDelete: (id: string) => Promise<void> }) {
  const [name, setName] = useState(''); const [content, setContent] = useState('')
  return <DashboardPanel title={kind}><form className="inline-form" onSubmit={(event) => { event.preventDefault(); void onSave(name, content); setName(''); setContent('') }}><input placeholder={`${kind.slice(0, -1)} name`} value={name} onChange={(event) => setName(event.target.value)} required />{kind === 'Scripts' && <textarea placeholder="Script content (stored only; never executed by this server)" value={content} onChange={(event) => setContent(event.target.value)} />}<button className="button button-primary">Save</button></form><div className="activity-list">{entries.map((entry) => <div className="activity-item" key={entry.id}><strong>{entry.name}</strong>{'enabled' in entry && <span>{entry.enabled ? 'Enabled' : 'Disabled'}</span>}<button onClick={() => void onDelete(entry.id)} className="link-action">Delete</button></div>)}</div></DashboardPanel>
}
const dashboardPages = ['Dashboard', 'Client Control', 'Configs', 'Scripts', 'Pathfinder', 'Statistics', 'Remote Control', 'Account', 'Settings']
function DashboardApp() {
  const [selected, setSelected] = useState('Dashboard'); const [data, setData] = useState<DashboardData | null>(null); const [account, setAccount] = useState<Account | null>(null); const [configs, setConfigs] = useState<Config[]>([]); const [scripts, setScripts] = useState<Script[]>([]); const [username, setUsername] = useState(''); const [search, setSearch] = useState(''); const searchRef = useRef<HTMLInputElement>(null)
  const refresh = async () => { const [me, summary, configResult, scriptResult, accountResult] = await Promise.all([api.me(), api.dashboard(), api.configs(), api.scripts(), api.account()]); setUsername(me.user.username); setData(summary); setConfigs(configResult.configs); setScripts(scriptResult.scripts); setAccount(accountResult) }
  const logout = () => void api.logout().finally(() => { api.clearSession(); navigate('/login') })
  const submitSearch = () => { const match = dashboardPages.find((page) => page.toLowerCase().includes(search.trim().toLowerCase())); if (match) { setSelected(match); setSearch('') } }
  // The dashboard is synchronized with the account service on mount and by polling.
  // oxlint-disable-next-line react/set-state-in-effect
  useEffect(() => { void refresh().catch(() => navigate('/login')); const timer = window.setInterval(() => void api.dashboard().then(setData).catch(() => undefined), 15_000); return () => window.clearInterval(timer) }, [])
  useEffect(() => { const focusSearch = (event: KeyboardEvent) => { if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); searchRef.current?.focus() } }; window.addEventListener('keydown', focusSearch); return () => window.removeEventListener('keydown', focusSearch) }, [])
  if (!data || !account) return <main className="landing-page"><div className="dashboard-panel loading-skeleton">Loading your Kairokk dashboard…</div></main>
  const state: ClientState = data.client.status === 'ONLINE' ? 'connected' : data.client.status === 'CONNECTING' ? 'reconnecting' : 'disconnected'
  const runCommand = async (type: string, payload: unknown = {}) => { await api.command(type, payload); await refresh() }
  const page = selected === 'Dashboard' ? <Dashboard data={data} account={account} username={username} clientState={state} onCommand={runCommand} onNavigate={setSelected} /> : selected === 'Client Control' ? <WorldViewPage data={data} account={account} onCommand={runCommand} /> : selected === 'Configs' ? <CollectionPage kind="Configs" entries={configs} onSave={async (name) => { await api.saveConfig(name, {}); await refresh() }} onDelete={async (id) => { await api.removeConfig(id); await refresh() }} /> : selected === 'Scripts' ? <CollectionPage kind="Scripts" entries={scripts} onSave={async (name, content) => { await api.saveScript({ name, content: content ?? '', enabled: false }); await refresh() }} onDelete={async (id) => { await api.removeScript(id); await refresh() }} /> : <OperationalPage page={selected} data={data} account={account} refresh={refresh} />
  return <main className="app-shell"><DashboardOrbBackground /><Sidebar selected={selected} onSelect={setSelected} /><section className="workspace"><TopBar searchRef={searchRef} search={search} username={username} onSearchChange={setSearch} onSearchSubmit={submitSearch} onNavigate={setSelected} onLogout={logout} /><section className="app-page">{page}</section></section></main>
}
function App() { const [path, setPath] = useState(window.location.pathname); useEffect(() => { const handler = () => setPath(window.location.pathname); window.addEventListener('popstate', handler); return () => window.removeEventListener('popstate', handler) }, []); if (path === '/') return <LandingPage />; if (path === '/login') return <AuthPage mode="login" onSuccess={() => navigate('/dashboard')} />; if (path === '/register') return <AuthPage mode="register" onSuccess={() => navigate('/dashboard')} />; if (path === '/dashboard') return <DashboardApp />; navigate('/'); return null }
export default App
