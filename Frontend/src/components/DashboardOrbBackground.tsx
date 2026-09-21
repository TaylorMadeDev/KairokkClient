import { useEffect, useRef } from 'react'

type Orb = readonly [x: number, y: number, size: number, speed: number, drift: number, phase: number, alpha: number]

// Mirrors the Minecraft title-screen field so both surfaces share the same motion signature.
const orbs: Orb[] = [
  [.06, .15, 8, .010, .014, .2, 132], [.13, .72, 5, .016, .010, 2.7, 104],
  [.20, .41, 13, .008, .018, 4.1, 148], [.27, .90, 7, .014, .012, 1.3, 122],
  [.32, .23, 5, .019, .008, 5.4, 118], [.37, .59, 10, .011, .015, 3, 138],
  [.43, .08, 6, .017, .011, .8, 112], [.48, .78, 16, .007, .020, 4.7, 158],
  [.53, .35, 7, .015, .009, 2.1, 128], [.58, .95, 5, .021, .008, 5.9, 106],
  [.63, .16, 11, .010, .017, 1.8, 145], [.69, .67, 6, .018, .011, 3.7, 120],
  [.74, .46, 18, .006, .022, .5, 154], [.79, .87, 8, .013, .013, 4.4, 126],
  [.84, .27, 5, .020, .009, 2.4, 112], [.90, .57, 12, .009, .019, 5.1, 142],
  [.95, .12, 7, .016, .010, 1.1, 120], [.98, .81, 5, .022, .008, 3.3, 102],
  [.23, .05, 4, .023, .007, 5.6, 92], [.40, .48, 5, .018, .010, 2.9, 108],
  [.57, .70, 4, .024, .007, .9, 96], [.72, .03, 6, .019, .011, 4.9, 114],
  [.87, .95, 4, .025, .006, 2, 92],
]

function drawOrb(context: CanvasRenderingContext2D, x: number, y: number, radius: number, color: string, alpha: number) {
  const glowRadius = radius * 3
  const gradient = context.createRadialGradient(x, y, 0, x, y, glowRadius)
  gradient.addColorStop(0, `rgba(${color}, ${alpha})`)
  gradient.addColorStop(.3, `rgba(${color}, ${alpha * .42})`)
  gradient.addColorStop(.6, `rgba(${color}, ${alpha * .12})`)
  gradient.addColorStop(1, `rgba(${color}, 0)`)
  context.fillStyle = gradient
  context.beginPath()
  context.arc(x, y, glowRadius, 0, Math.PI * 2)
  context.fill()
}

export function DashboardOrbBackground() {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const context = canvas.getContext('2d')
    if (!context) return
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')
    let frame = 0
    let width = 0
    let height = 0
    let pixelRatio = 1
    const resize = () => {
      width = window.innerWidth
      height = window.innerHeight
      pixelRatio = Math.min(window.devicePixelRatio || 1, 2)
      canvas.width = Math.round(width * pixelRatio)
      canvas.height = Math.round(height * pixelRatio)
      canvas.style.width = `${width}px`
      canvas.style.height = `${height}px`
      context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)
    }
    const render = (timestamp: number) => {
      const time = reducedMotion.matches ? 0 : timestamp / 1000
      context.clearRect(0, 0, width, height)
      drawOrb(context, width * .54, height * .35, Math.min(width, height) * .14, '22, 79, 154', .055)
      const scale = Math.max(.78, Math.min(1.65, Math.min(width, height) / 720))
      for (const [baseX, baseY, size, speed, drift, phase, alpha] of orbs) {
        const loop = ((baseY - time * speed) % 1 + 1) % 1
        const x = baseX * width + Math.sin(time * (.34 + speed * 8) + phase) * drift * width
        const y = loop * (height + 44) - 22
        const breath = 1 + Math.sin(time * .72 + phase * 1.7) * .1
        const radius = Math.max(1.5, size * scale * breath * .58)
        const color = (Math.floor(phase * 10) & 1) === 0 ? '42, 120, 208' : '22, 79, 154'
        drawOrb(context, x, y, radius, color, (alpha / 255) * .64)
      }
      if (!reducedMotion.matches) frame = window.requestAnimationFrame(render)
    }
    const restart = () => { window.cancelAnimationFrame(frame); render(performance.now()) }
    resize()
    render(performance.now())
    window.addEventListener('resize', resize)
    reducedMotion.addEventListener('change', restart)
    return () => { window.cancelAnimationFrame(frame); window.removeEventListener('resize', resize); reducedMotion.removeEventListener('change', restart) }
  }, [])
  return <canvas ref={canvasRef} className="dashboard-orb-canvas" aria-hidden="true" />
}
