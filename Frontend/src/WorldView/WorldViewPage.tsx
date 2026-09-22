import { useCallback, useEffect, useRef, useState } from 'react'
import { Box, ChevronDown, Crosshair, Eye, Focus, Fullscreen, Grid3X3, MapPin, MessageSquareText, Minus, Navigation, Plus, RotateCcw, Route, Send, Settings2, SlidersHorizontal, Users } from 'lucide-react'
import { api, worldSocketUrl, type Account, type DashboardData } from '../lib/api'
import { createMockWorldProvider, mockMetadata, mockPlayer } from './mockWorld'
import { WorldScene } from './WorldScene'
import { loadNearbyChunks, makeWorldCacheKey, saveWorldChunk } from './worldCache'
import type { Vec3, WorldChunk, WorldEntity, WorldMetadata, WorldPacket, WorldPlayer } from './types'
import './world-view.css'
import './world-context.css'

type SceneApi = { focus: () => void; top: () => void; perspective: () => void; zoom: (direction: number) => void }
const Toggle = ({ active, onClick }: { active: boolean; onClick: () => void }) => <button className={`wv-toggle ${active ? 'active' : ''}`} onClick={onClick}><i /></button>
const TinyButton = ({ children, onClick, active }: { children: React.ReactNode; onClick?: () => void; active?: boolean }) => <button className={`wv-tiny-button ${active ? 'active' : ''}`} onClick={onClick}>{children}</button>

export function WorldViewPage({ data, account, onCommand }: { data: DashboardData; account: Account; onCommand: (type: string, payload?: unknown) => Promise<void> }) {
  const [chunks, setChunks] = useState(new Map<string, WorldChunk>()), [player, setPlayer] = useState<WorldPlayer>(mockPlayer), [entities, setEntities] = useState(new Map<string, WorldEntity>()), [metadata, setMetadata] = useState<WorldMetadata>(mockMetadata), [path, setPath] = useState<Vec3[]>([]), [expected, setExpected] = useState(25), [syncing, setSyncing] = useState(true)
  const [follow, setFollow] = useState(true), [showEntities, setShowEntities] = useState(true), [showPath, setShowPath] = useState(true), [showGrid, setShowGrid] = useState(false), [goalMode, setGoalMode] = useState(false), [goal, setGoal] = useState<Vec3 | null>(null), [faces, setFaces] = useState(0), [renderFps, setRenderFps] = useState(60), [filter, setFilter] = useState('All'), [source, setSource] = useState<'connecting'|'live'|'mock'|'disconnected'>('connecting')
  const [contextMenu, setContextMenu] = useState<{ point: Vec3; x: number; y: number } | null>(null)
  const apiRef = useRef<SceneApi | null>(null), viewer = useRef<HTMLDivElement>(null), syncTimer = useRef<number | undefined>(undefined), activeSession = useRef(''), cacheKey = useRef(''), expectedRef = useRef(25)
  const finishAfterQuietPeriod = useCallback(() => {
    window.clearTimeout(syncTimer.current)
    syncTimer.current = window.setTimeout(() => setSyncing(false), 1800)
  }, [])
  const packet = useCallback((message: WorldPacket) => {
    switch (message.type) {
      case 'world:init': {
        setChunks(new Map()); setEntities(new Map()); setPath([]); setExpected(message.expectedChunks); setSyncing(true)
        expectedRef.current = message.expectedChunks; activeSession.current = message.sessionId
        if (message.metadata) setMetadata((current) => ({ ...current, ...message.metadata }))
        const server = data.client.serverAddress?.trim(), dimension = message.metadata?.dimension
        if (server && server.toLowerCase() !== 'singleplayer' && dimension && Number.isFinite(message.centerX) && Number.isFinite(message.centerZ)) {
          const key = makeWorldCacheKey(account.user.username, server, dimension); cacheKey.current = key
          const radius = Math.max(1, Math.round((Math.sqrt(message.expectedChunks) - 1) / 2)), session = message.sessionId
          void loadNearbyChunks(key, message.centerX!, message.centerZ!, radius).then((cached) => {
            if (activeSession.current !== session) return
            setChunks((current) => { const next = new Map(current); cached.forEach((chunk) => { if (!next.has(chunk.key)) next.set(chunk.key, chunk) }); if (next.size >= expectedRef.current) setSyncing(false); return next })
            if (cached.length && cached.length < expectedRef.current) finishAfterQuietPeriod()
          })
        } else cacheKey.current = ''
        break
      }
      case 'world:reset': activeSession.current = ''; cacheKey.current = ''; setChunks(new Map()); setEntities(new Map()); setPath([]); setSyncing(true); break
      case 'world:chunk':
        setChunks((current) => { const next = new Map(current).set(message.chunk.key, message.chunk); if (next.size >= expectedRef.current) setSyncing(false); else finishAfterQuietPeriod(); return next })
        if (cacheKey.current) void saveWorldChunk(cacheKey.current, message.chunk)
        break
      case 'world:chunk-unload': setChunks((current) => { const next = new Map(current); next.delete(message.key); return next }); break
      case 'world:player': setPlayer(message.player); break
      case 'world:entity-spawn': case 'world:entity-update': setEntities((current) => new Map(current).set(message.entity.id, message.entity)); break
      case 'world:entity-remove': setEntities((current) => { const next = new Map(current); next.delete(message.id); return next }); break
      case 'world:metadata': setMetadata((current) => ({ ...current, ...message.metadata })); break
      case 'world:path': setPath(message.nodes); break
      case 'world:block-update':
        setChunks((current) => {
          const existing = current.get(message.chunkKey); if (!existing) return current
          const samePosition = (block: Vec3) => block.x === message.position.x && block.y === message.position.y && block.z === message.position.z
          const blocks = existing.blocks.filter((block) => !samePosition(block)); if (message.block) blocks.push(message.block)
          const updated = { ...existing, blocks, revision: existing.revision + 1 }, next = new Map(current).set(message.chunkKey, updated)
          if (cacheKey.current) void saveWorldChunk(cacheKey.current, updated)
          return next
        }); break
    }
  }, [account.user.username, data.client.serverAddress, finishAfterQuietPeriod])
  useEffect(() => {
    let disposed = false, socket: WebSocket | null = null, retryTimer: number | undefined, fallbackTimer: number | undefined, attempts = 0, stopMock: (() => void) | undefined
    let receivedWorld = false
    const connect = async () => {
      if (disposed) return
      if (attempts > 0) await api.dashboard().catch(() => undefined)
      if (disposed) return
      const url = worldSocketUrl()
      if (!url) { setSource('disconnected'); return }
      setSource((current) => current === 'mock' ? current : 'connecting')
      const current = new WebSocket(url); socket = current
      current.addEventListener('open', () => { attempts = 0; current.send(JSON.stringify({ type: 'WORLD_SUBSCRIBE', radius: 4 })) })
      current.addEventListener('message', (event) => {
        try {
          const raw = JSON.parse(String(event.data)) as { type?: string }
          if (raw.type?.startsWith('world:') && raw.type !== 'world:error') { receivedWorld = true; stopMock?.(); stopMock = undefined; window.clearTimeout(fallbackTimer); setSource('live'); packet(raw as WorldPacket) }
        } catch { /* invalid world messages are ignored */ }
      })
      current.addEventListener('close', () => {
        if (disposed || socket !== current) return
        socket = null
        setSource((value) => value === 'mock' ? value : 'disconnected')
        const delay = Math.min(15_000, 1000 * 2 ** Math.min(attempts++, 4))
        retryTimer = window.setTimeout(() => void connect(), delay)
      })
    }
    if (import.meta.env.DEV && data.client.status !== 'ONLINE') fallbackTimer = window.setTimeout(() => { if (!receivedWorld && !disposed) { setSource('mock'); stopMock = createMockWorldProvider(packet) } }, 1800)
    void connect()
    return () => { disposed = true; window.clearTimeout(retryTimer); window.clearTimeout(fallbackTimer); stopMock?.(); if (socket) { if (socket.readyState === WebSocket.OPEN) socket.send(JSON.stringify({ type: 'WORLD_UNSUBSCRIBE' })); socket.close() } }
  }, [packet, data.client.status])
  useEffect(() => () => window.clearTimeout(syncTimer.current), [])
  const onStats = useCallback((visibleFaces: number, fps: number) => { if (visibleFaces) setFaces(visibleFaces); if (fps) setRenderFps(fps) }, [])
  const confirmGoal = async () => { if (!goal) return; await onCommand('PATHFINDER_SET_GOAL', goal); setGoalMode(false) }
  const startPathfinding = async () => { if (path.length) { await onCommand('PATHFINDER_STOP'); setPath([]); return } if (!goal) { setGoalMode(true); return } await onCommand('PATHFINDER_START', goal) }
  const contextBlock = useCallback((point: Vec3, x: number, y: number) => setContextMenu({ point, x, y }), [])
  const goToBlock = async (point: Vec3) => { setGoal(point); setContextMenu(null); setGoalMode(false); await onCommand('PATHFINDER_SET_GOAL', point); await onCommand('PATHFINDER_START', point) }
  useEffect(() => { const close = () => setContextMenu(null); window.addEventListener('click', close); return () => window.removeEventListener('click', close) }, [])
  const online = data.client.status === 'ONLINE', sync = Math.min(100, Math.round(chunks.size / Math.max(1, expected) * 100))
  const logs = [['INFO','Client connected'],['WORLD',`Loaded ${chunks.size} nearby chunks`],['PLAYER',`Position updated: ${player.x.toFixed(1)}, ${player.y.toFixed(1)}, ${player.z.toFixed(1)}`],['PATH',`Route available (${path.length} nodes)`],['WORLD','World reconstruction active']]
  return <div className="world-view-page">
    <header className="wv-heading"><div><h1>Client Control</h1><p>Live tactical reconstruction of your Kairokk world.</p></div><span className={source === 'live' || source === 'mock' ? 'online' : ''}><i />{source === 'live' ? 'Live world' : source === 'mock' ? 'Development preview' : source === 'connecting' ? 'Connecting' : 'Disconnected'}</span></header>
    <nav className="wv-tabs"><button className="active"><Box/>World View</button><button><Users/>Player</button><button><SlidersHorizontal/>Modules</button><button><Settings2/>Settings</button><div/><button><Navigation/>Survival<ChevronDown/></button></nav>
    <div className="wv-main-grid"><div className="wv-primary">
      <div className="world-viewport" ref={viewer}>
        <WorldScene chunks={chunks} player={player} entities={entities} path={path} follow={follow} showEntities={showEntities} showPath={showPath} showGrid={showGrid} goalMode={goalMode} onGoal={setGoal} onContextBlock={contextBlock} apiRef={apiRef} onStats={onStats}/>
        {syncing && chunks.size < expected && <div className="wv-sync"><strong>SYNCING WORLD</strong><span>{chunks.size} / {expected} chunks</span><i><b style={{width:`${sync}%`}}/></i></div>}
        <div className="world-info"><strong>WORLD INFORMATION</strong>{[['Dimension',metadata.dimension],['Biome',metadata.biome],['Position',`X ${player.x.toFixed(1)}  Y ${player.y.toFixed(1)}  Z ${player.z.toFixed(1)}`],['Facing',`${metadata.facing} (${Math.round(player.yaw)}°)`],['Light Level',metadata.light],['Time',metadata.time],['Entities',entities.size],['Loaded Chunks',`${chunks.size} / ${expected}`]].map(([key,value])=><div key={key}><span>{key}</span><b>{value}</b></div>)}</div>
        <div className="wv-compass"><b>N</b><b>E</b><b>S</b><b>W</b><i style={{transform:`rotate(${-player.yaw}deg)`}}/></div>
        <div className="wv-camera"><TinyButton onClick={()=>apiRef.current?.top()}>TOP</TinyButton><TinyButton onClick={()=>apiRef.current?.focus()}><Focus/></TinyButton><TinyButton onClick={()=>apiRef.current?.zoom(1)}><Plus/></TinyButton><TinyButton onClick={()=>apiRef.current?.zoom(-1)}><Minus/></TinyButton><TinyButton onClick={()=>viewer.current?.requestFullscreen()}><Fullscreen/></TinyButton></div>
        <div className="wv-toolbar"><button className={follow?'active':''} onClick={()=>setFollow(!follow)}><Crosshair/>Follow Player</button><button className={showEntities?'active':''} onClick={()=>setShowEntities(!showEntities)}><Users/>Entities</button><button className={showPath?'active':''} onClick={()=>setShowPath(!showPath)}><Route/>Path</button><button className={showGrid?'active':''} onClick={()=>setShowGrid(!showGrid)}><Grid3X3/>Grid</button><button className={goalMode?'active danger':''} onClick={()=>setGoalMode(!goalMode)}><MapPin/>Set Goal</button></div>
        <div className="wv-coordinates">X: {player.x.toFixed(1)} &nbsp; Y: {player.y.toFixed(1)} &nbsp; Z: {player.z.toFixed(1)}</div>
      </div>
      <div className="wv-lower"><section className="wv-card console"><header><h2>Console</h2><select value={filter} onChange={e=>setFilter(e.target.value)}><option>All</option><option>Info</option><option>World</option><option>Pathfinder</option><option>Client</option></select><button>Clear</button></header><div className="console-lines">{logs.filter(([type])=>filter==='All'||type.toLowerCase().includes(filter.toLowerCase().replace('finder',''))).map(([type,text],index)=><p key={text}><span>[14:32:{10+index*3}]</span><b className={`log-${type.toLowerCase()}`}>[{type}]</b>{text}</p>)}</div><form onSubmit={e=>{e.preventDefault();const input=e.currentTarget.elements.namedItem('command') as HTMLInputElement;if(input.value.trim())void onCommand('SEND_CHAT',{message:input.value.trim()});input.value=''}}><input name="command" placeholder="Send a chat message…"/><button><Send/></button></form></section>
      <section className="wv-card performance"><h2>Performance</h2><div className="performance-grid"><div><span>Minecraft</span><strong>FPS <b>—</b></strong><strong>Ping <b>—</b></strong></div><div><span>World View</span><strong>Chunks <b>{chunks.size}</b></strong><strong>Visible Faces <b>{faces.toLocaleString()}</b></strong><strong>Entities <b>{entities.size}</b></strong><strong>Render FPS <b>{renderFps}</b></strong><strong>Memory <b>{Math.max(1,Math.round(faces*72/1048576))} MB</b></strong></div></div></section></div>
    </div><aside className="wv-side">
      <section className="wv-card status"><h2>Client Status</h2>{[['Status',online?'Online':'Mock data'],['Client Version',data.client.version??'v1.0.0'],['Minecraft Version','26.1.2'],['Server',data.client.serverAddress??'Singleplayer'],['World Packets',`${chunks.size} chunks`],['Device',account.devices[0]?.deviceName??'Kairokk Client']].map(([a,b])=><div key={a}><span>{a}</span><b>{b}</b></div>)}</section>
      <section className="wv-card quick"><h2>Quick Actions</h2><button onClick={()=>void onCommand('TAKE_SCREENSHOT')}><Eye/>Take Screenshot</button><button onClick={()=>document.querySelector<HTMLInputElement>('.console input')?.focus()}><MessageSquareText/>Send Chat Message</button><button onClick={()=>void onCommand('TOGGLE_HUD')}><Eye/>Toggle HUD</button><button className={path.length?'active':''} onClick={()=>void startPathfinding()}><Route/>{path.length?'Stop Pathfinding':'Start Pathfinding'}</button><button className={goalMode?'active':''} onClick={()=>setGoalMode(true)}><MapPin/>{goalMode?'Click a Block…':'Set Pathfinding Goal'}</button><button onClick={()=>apiRef.current?.perspective()}><RotateCcw/>Reset Camera</button></section>
      <section className="wv-card modules"><h2>World Layers</h2><div><span><Users/>Entities</span><Toggle active={showEntities} onClick={()=>setShowEntities(!showEntities)}/></div><div><span><Route/>Pathfinder</span><Toggle active={showPath} onClick={()=>setShowPath(!showPath)}/></div><div><span><Grid3X3/>Grid</span><Toggle active={showGrid} onClick={()=>setShowGrid(!showGrid)}/></div><div><span><Crosshair/>Follow Player</span><Toggle active={follow} onClick={()=>setFollow(!follow)}/></div></section>
    </aside></div>
    {goalMode && goal && <div className="goal-dialog"><div><MapPin/><h2>Set Pathfinding Goal</h2><p>X: {goal.x} &nbsp; Y: {goal.y} &nbsp; Z: {goal.z}</p><footer><button onClick={()=>{setGoalMode(false);setGoal(null)}}>Cancel</button><button onClick={()=>void confirmGoal()}>Set Goal</button></footer></div></div>}
    {contextMenu && <div className="wv-context-menu" style={{left:contextMenu.x,top:contextMenu.y}} onClick={event=>event.stopPropagation()}><header><MapPin/>Block {contextMenu.point.x}, {contextMenu.point.y}, {contextMenu.point.z}</header><button onClick={()=>void goToBlock(contextMenu.point)}><Route/>Go to this block</button><button onClick={()=>{setGoal(contextMenu.point);setGoalMode(true);setContextMenu(null)}}><MapPin/>Set as pathfinding goal</button></div>}
  </div>
}
