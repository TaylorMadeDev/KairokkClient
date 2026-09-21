import { useEffect, useRef } from 'react'
import { ArrowRight, Box, Clock3, Download, Eye, Headphones, ShieldCheck, Users, Zap } from 'lucide-react'

const navItems = ['Home', 'Features', 'Screenshots', 'Pricing', 'Community', 'Support']
const benefits = [[ShieldCheck, 'Secure'], [Zap, 'High Performance'], [Eye, 'Regular Updates'], [Users, 'Active Community']] as const
const metrics = [[Users, '50K+', 'PLAYERS WORLDWIDE'], [Clock3, '99.9%', 'UPTIME'], [Box, 'REGULAR', 'UPDATES'], [Headphones, '24/7', 'SUPPORT']] as const

function Brand() { return <a className="landing-brand" href="#home" aria-label="KAIROKK home"><img src="/kairokk-logo.png" alt="" /><span><strong>KAIROKK</strong><small>CONTROL BEYOND LIMITS.</small></span></a> }
function ParticleField() {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const context = canvas.getContext('2d')
    if (!context) return
    const motionReduced = window.matchMedia('(prefers-reduced-motion: reduce)')
    let frame = 0
    let width = 0
    let height = 0
    let pixelRatio = 1
    const resize = () => {
      const bounds = canvas.getBoundingClientRect()
      width = bounds.width
      height = bounds.height
      pixelRatio = Math.min(window.devicePixelRatio || 1, 2)
      canvas.width = Math.round(width * pixelRatio)
      canvas.height = Math.round(height * pixelRatio)
      context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)
    }
    const draw = (time: number) => {
      context.clearRect(0, 0, width, height)
      const spacing = Math.max(20, Math.min(30, width / 70))
      const seconds = motionReduced.matches ? 0 : time * 0.001
      const waveCenterX = ((seconds * 92) % (width + 360)) - 180
      for (let y = -spacing; y < height + spacing; y += spacing) {
        for (let x = -spacing; x < width + spacing; x += spacing) {
          const bandDistance = (x - waveCenterX) / 145
          const waveStrength = Math.exp(-(bandDistance * bandDistance))
          const alpha = 0.31 * (1 - waveStrength * 0.4)
          const radius = 0.92
          context.beginPath()
          context.fillStyle = `rgba(113, 148, 255, ${alpha})`
          context.arc(x, y, radius, 0, Math.PI * 2)
          context.fill()
        }
      }
      if (!motionReduced.matches) frame = window.requestAnimationFrame(draw)
    }
    resize()
    const observer = new ResizeObserver(resize)
    observer.observe(canvas)
    frame = window.requestAnimationFrame(draw)
    const onMotionChange = () => { window.cancelAnimationFrame(frame); frame = window.requestAnimationFrame(draw) }
    motionReduced.addEventListener('change', onMotionChange)
    return () => { window.cancelAnimationFrame(frame); observer.disconnect(); motionReduced.removeEventListener('change', onMotionChange) }
  }, [])
  return <canvas ref={canvasRef} className="local-dot-wave absolute inset-0 -z-10 pointer-events-none" aria-hidden="true" />
}

export function LandingPage() {
  return <main className="landing-page relative isolate" id="home"><ParticleField /><header className="landing-header"><Brand /><nav aria-label="Primary navigation">{navItems.map((item) => <a className={item === 'Home' ? 'landing-nav-active' : ''} href={`#${item.toLowerCase()}`} key={item}>{item}</a>)}</nav><div className="landing-header-actions"><a className="landing-sign-in" href="/login">Log In</a><a className="landing-get-started" href="/register">Create Account <ArrowRight size={21} /></a></div></header><section className="landing-hero"><aside className="hero-rail hero-rail-left">PLAY<br />CUSTOMISE<br />DOMINATE</aside><aside className="hero-rail hero-rail-right">SAME GAME.<br />MORE<br />POSSIBILITIES.</aside><div className="hero-copy"><p className="hero-kicker">THE ULTIMATE MINECRAFT CLIENT.</p><h1><img src="/kairokk-logo.png" alt="" />KAIROKK</h1><p className="hero-tagline">CONTROL BEYOND LIMITS.</p><p className="hero-description">A next-generation Minecraft client designed for performance,<br />control, and freedom. Built for players who want more.</p><div className="hero-actions"><a className="landing-download" href="#download">Download Client <Download size={19} /></a><a className="landing-learn-more" href="#features">Learn More</a></div><div className="benefit-row">{benefits.map(([Icon, label]) => <span key={label}><Icon size={21} />{label}</span>)}</div></div></section><section className="metric-row">{metrics.map(([Icon, value, label], index) => <article key={label} className="metric"><span className="metric-icon"><Icon size={32} /></span><div><strong>{value}</strong><small>{label}</small></div>{index < metrics.length - 1 && <i />}</article>)}</section><footer className="landing-footer"><span />MORE THAN A CLIENT. A HIGHER WAY TO PLAY.</footer></main>
}
