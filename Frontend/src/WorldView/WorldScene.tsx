import { useEffect, useRef } from 'react'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { CrouchAnimation, FlyingAnimation, IdleAnimation, PlayerObject, RunningAnimation, SwimAnimation, WalkingAnimation, type PlayerAnimation } from 'skinview3d'
import { inferModelType, loadImage, loadSkinToCanvas } from 'skinview-utils'
import { minecraftSkinUrl } from '../lib/api'
import type { Vec3, WorldChunk, WorldEntity, WorldPlayer } from './types'
import { blockVisual } from './blockCatalog'

type SceneApi = { focus: () => void; top: () => void; perspective: () => void; zoom: (direction: number) => void }
type Runtime = { world: THREE.Group; entities: THREE.Group; path: THREE.Group; chunks: Map<string, THREE.Group> }
const disposeResources = (root: THREE.Object3D) => { root.traverse((object) => { if (object instanceof THREE.Mesh || object instanceof THREE.Line) { object.geometry.dispose(); const list = Array.isArray(object.material) ? object.material : [object.material]; list.forEach((material) => material.dispose()) } }) }
const dispose = (root: THREE.Object3D) => { disposeResources(root); root.removeFromParent() }
const fallbackSkin = () => {
  const canvas = document.createElement('canvas'); canvas.width = canvas.height = 64
  const context = canvas.getContext('2d')!; context.clearRect(0, 0, 64, 64)
  context.fillStyle = '#d7a476'; context.fillRect(0, 0, 32, 16); context.fillRect(40, 16, 16, 16); context.fillRect(32, 48, 16, 16)
  context.fillStyle = '#18243a'; context.fillRect(8, 8, 8, 3)
  context.fillStyle = '#388bf2'; context.fillRect(16, 16, 24, 16)
  context.fillStyle = '#1d3151'; context.fillRect(0, 16, 16, 16); context.fillRect(16, 48, 16, 16)
  return canvas
}
const textureForSkin = async (uuid?: string) => {
  const canvas = fallbackSkin()
  if (uuid) try { const image = await loadImage(minecraftSkinUrl(uuid)); loadSkinToCanvas(canvas, image) } catch { /* Keep the built-in offline skin. */ }
  const texture = new THREE.CanvasTexture(canvas); texture.magFilter = THREE.NearestFilter; texture.minFilter = THREE.NearestFilter; texture.colorSpace = THREE.SRGBColorSpace
  return { texture, modelType: inferModelType(canvas) }
}
const avatarState = (player: WorldPlayer) => {
  if (player.fallFlying) return 'flying'
  if (player.swimming) return 'swimming'
  if (player.sneaking) return 'crouching'
  const speed = Math.hypot(player.velocity.x, player.velocity.z)
  if (player.sprinting && speed > .02) return 'running'
  if (speed > .015) return 'walking'
  return 'idle'
}
const chunkObject = (chunk: WorldChunk) => {
  const group = new THREE.Group()
  const occupied = new Set(chunk.blocks.filter((block) => blockVisual(block.kind).opaque).map((block) => `${block.x},${block.y},${block.z}`))
  const visible = chunk.blocks.filter((block) => !blockVisual(block.kind).opaque || [[1,0,0],[-1,0,0],[0,1,0],[0,-1,0],[0,0,1],[0,0,-1]].some(([x,y,z]) => !occupied.has(`${block.x+x},${block.y+y},${block.z+z}`)))
  const matrix = new THREE.Matrix4()
  const batches = new Map<string, typeof visible>()
  for (const block of visible) {
    const visual = blockVisual(block.kind)
    const key = `${visual.color}:${visual.shape}:${visual.width}:${visual.height}:${visual.depth}:${visual.opacity}`
    const batch = batches.get(key) ?? []
    batch.push(block); batches.set(key, batch)
  }
  for (const blocks of batches.values()) {
    const visual = blockVisual(blocks[0].kind)
    if (visual.shape === 'cross') {
      const material = new THREE.MeshStandardMaterial({ color: visual.color, roughness: .9, side: THREE.DoubleSide })
      for (const rotation of [Math.PI / 4, -Math.PI / 4]) {
        const geometry = new THREE.PlaneGeometry(visual.width, visual.height); geometry.translate(0, visual.height / 2, 0); geometry.rotateY(rotation)
        const mesh = new THREE.InstancedMesh(geometry, material.clone(), blocks.length)
        blocks.forEach((block, index) => { matrix.makeTranslation(block.x + .5, block.y, block.z + .5); mesh.setMatrixAt(index, matrix) })
        mesh.instanceMatrix.needsUpdate = true; mesh.userData.blocks = blocks; mesh.castShadow = true; mesh.receiveShadow = true; group.add(mesh)
      }
      material.dispose()
      continue
    }
    const mesh = new THREE.InstancedMesh(new THREE.BoxGeometry(visual.width,visual.height,visual.depth), new THREE.MeshStandardMaterial({ color: visual.color, roughness: visual.transparent ? .3 : .82, transparent: visual.transparent, opacity: visual.opacity }), blocks.length)
    blocks.forEach((block, index) => { matrix.makeTranslation(block.x + .5, block.y + visual.height / 2, block.z + .5); mesh.setMatrixAt(index, matrix) })
    mesh.instanceMatrix.needsUpdate = true; mesh.userData.blocks = blocks; mesh.castShadow = !visual.transparent; mesh.receiveShadow = true; group.add(mesh)
  }
  const topBlocks = visible.filter((block) => blockVisual(block.kind).opaque && !occupied.has(`${block.x},${block.y+1},${block.z}`))
  if (topBlocks.length) {
    const lines:number[]=[]
    topBlocks.forEach(({x,y,z})=>{const top=y+1.012;lines.push(x,top,z,x+1,top,z,x+1,top,z,x+1,top,z+1,x+1,top,z+1,x,top,z+1,x,top,z+1,x,top,z)})
    const geometry=new THREE.BufferGeometry();geometry.setAttribute('position',new THREE.Float32BufferAttribute(lines,3))
    const grid=new THREE.LineSegments(geometry,new THREE.LineBasicMaterial({color:0x43a5d7,transparent:true,opacity:.42}));grid.name='block-grid';grid.raycast=()=>{};group.add(grid)
  }
  group.userData.revision = chunk.revision; group.userData.faces = visible.length * 3; return group
}

export function WorldScene({ chunks, player, entities, path, follow, showEntities, showPath, showGrid, goalMode, onGoal, onContextBlock, apiRef, onStats }: { chunks: Map<string, WorldChunk>; player: WorldPlayer; entities: Map<string, WorldEntity>; path: Vec3[]; follow: boolean; showEntities: boolean; showPath: boolean; showGrid: boolean; goalMode: boolean; onGoal: (point: Vec3) => void; onContextBlock: (point: Vec3, clientX: number, clientY: number) => void; apiRef: React.MutableRefObject<SceneApi | null>; onStats: (faces: number, fps: number) => void }) {
  const host = useRef<HTMLDivElement>(null), runtime = useRef<Runtime | null>(null), lastFaces = useRef(0), lastFps = useRef(60)
  const latest = useRef({ player, follow, goalMode, onGoal, onContextBlock, showGrid })
  useEffect(() => { latest.current = { player, follow, goalMode, onGoal, onContextBlock, showGrid } }, [player, follow, goalMode, onGoal, onContextBlock, showGrid])
  useEffect(() => {
    if (!host.current) return
    const hostElement = host.current
    const scene = new THREE.Scene(); scene.background = new THREE.Color(0x061426); scene.fog = new THREE.FogExp2(0x061426, .012)
    const camera = new THREE.PerspectiveCamera(46, 1, .1, 450); camera.position.set(34, 33, 34)
    const renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: 'high-performance' }); renderer.setPixelRatio(Math.min(devicePixelRatio, 1.7)); renderer.outputColorSpace = THREE.SRGBColorSpace; hostElement.appendChild(renderer.domElement)
    const controls = new OrbitControls(camera, renderer.domElement); controls.enableDamping = true; controls.dampingFactor = .07; controls.target.set(0, 2, 0); controls.mouseButtons = { LEFT: THREE.MOUSE.ROTATE, MIDDLE: THREE.MOUSE.PAN, RIGHT: THREE.MOUSE.PAN }; controls.minDistance = 7; controls.maxDistance = 125; controls.maxPolarAngle = Math.PI * .49
    scene.add(new THREE.HemisphereLight(0x9bc9ff, 0x18351d, 2.1)); const sun = new THREE.DirectionalLight(0xffe5bd, 3.2); sun.position.set(-24,38,18); scene.add(sun)
    const world = new THREE.Group(), entityGroup = new THREE.Group(), pathGroup = new THREE.Group(); scene.add(world, entityGroup, pathGroup)
    runtime.current = { world, entities: entityGroup, path: pathGroup, chunks: new Map() }
    const playerGroup = new THREE.Group()
    const avatar = new PlayerObject(); avatar.scale.setScalar(1 / 16)
    // skinview3d's pivot is centered; align the actual feet to the Minecraft
    // player group's origin so the model cannot sink into the floor.
    avatar.updateMatrixWorld(true)
    const avatarBounds = new THREE.Box3().setFromObject(avatar)
    const avatarFeetOffset = -avatarBounds.min.y
    avatar.position.y = avatarFeetOffset
    avatar.castShadow = true; avatar.receiveShadow = true
    const ring = new THREE.Mesh(new THREE.RingGeometry(.68,.88,32), new THREE.MeshBasicMaterial({ color: 0x26a7ff, side: THREE.DoubleSide, transparent: true, opacity: .9 })); ring.rotation.x = -Math.PI/2; ring.position.y = .04
    playerGroup.add(avatar as unknown as THREE.Object3D, ring); scene.add(playerGroup)
    const crouchAnimation = new CrouchAnimation(); crouchAnimation.runOnce = true
    const avatarAnimations: Record<string, PlayerAnimation> = { idle: new IdleAnimation(), walking: new WalkingAnimation(), running: new RunningAnimation(), crouching: crouchAnimation, flying: new FlyingAnimation(), swimming: new SwimAnimation() }
    let currentAvatarState = '', loadedSkinUuid = '', avatarTexture: THREE.Texture | null = null, active = true
    const updateAvatarSkin = async (uuid?: string) => { const key = uuid ?? 'fallback'; if (key === loadedSkinUuid) return; loadedSkinUuid = key; const loaded = await textureForSkin(uuid); if (!active || loadedSkinUuid !== key) { loaded.texture.dispose(); return } avatarTexture?.dispose(); avatarTexture = loaded.texture; avatar.skin.map = loaded.texture as never; avatar.skin.modelType = loaded.modelType }
    void updateAvatarSkin()
    const hoverMaterial = new THREE.MeshBasicMaterial({ color: 0x63e6ff, wireframe: true, transparent:true, opacity:.95 }); const hover = new THREE.Mesh(new THREE.BoxGeometry(1.045,1.045,1.045), hoverMaterial); hover.visible = false; scene.add(hover); const raycaster = new THREE.Raycaster(), pointer = new THREE.Vector2()
    const resize = () => { const w=hostElement.clientWidth,h=hostElement.clientHeight; camera.aspect=w/Math.max(1,h); camera.updateProjectionMatrix(); renderer.setSize(w,h,false) }; const observer=new ResizeObserver(resize); observer.observe(hostElement); resize()
    const focus=()=>{const p=latest.current.player; controls.target.set(p.x,p.y+1,p.z)}; apiRef.current={focus,top:()=>{const p=latest.current.player; camera.position.set(p.x,p.y+58,p.z+.01);focus()},perspective:()=>{const p=latest.current.player;camera.position.set(p.x+34,p.y+30,p.z+34);focus()},zoom:(d)=>camera.position.lerp(controls.target,d>0?.15:-.16)}
    const pick=(event:PointerEvent)=>{const rect=renderer.domElement.getBoundingClientRect();pointer.set(((event.clientX-rect.left)/rect.width)*2-1,-((event.clientY-rect.top)/rect.height)*2+1);raycaster.setFromCamera(pointer,camera);const hit=raycaster.intersectObjects(world.children,true).find(entry=>entry.object.name!=='block-grid');if(!hit){hover.visible=false;return null}const point=hit.point.clone().add(hit.face?.normal.clone().multiplyScalar(.08)??new THREE.Vector3()).floor();hover.position.copy(point).addScalar(.5);hover.visible=true;hoverMaterial.color.set(latest.current.showGrid?0x7dffea:0x63e6ff);return{x:point.x,y:point.y,z:point.z}}; const move=(e:PointerEvent)=>void pick(e); const leave=()=>{hover.visible=false}; const click=(e:PointerEvent)=>{const point=pick(e);if(point&&latest.current.goalMode)latest.current.onGoal(point)}; const menu=(e:PointerEvent)=>{e.preventDefault();const point=pick(e);if(point)latest.current.onContextBlock(point,e.clientX,e.clientY)};renderer.domElement.addEventListener('pointermove',move);renderer.domElement.addEventListener('pointerleave',leave);renderer.domElement.addEventListener('click',click);renderer.domElement.addEventListener('dblclick',focus);renderer.domElement.addEventListener('contextmenu',menu)
    let frames=0,mark=performance.now(),lastFrame=performance.now(),animation=0, positioned=false
    let renderedPlayer = new THREE.Vector3(), fromPlayer = new THREE.Vector3(), toPlayer = new THREE.Vector3()
    let fromYaw = 0, toYaw = 0, renderedYaw = 0, sampleStarted = 0, sampleDuration = 150, lastSample = latest.current.player
    const cameraTarget = new THREE.Vector3(), cameraShift = new THREE.Vector3()
    const animate=()=>{
      animation=requestAnimationFrame(animate)
      const now=performance.now(),delta=Math.min((now-lastFrame)/1000,.05);lastFrame=now
      const p=latest.current.player
      if (p!==lastSample || !positioned) {
        const nextPosition=new THREE.Vector3(p.x,p.y,p.z)
        if (!positioned || renderedPlayer.distanceTo(nextPosition)>12) {
          renderedPlayer.copy(nextPosition);fromPlayer.copy(nextPosition);toPlayer.copy(nextPosition);fromYaw=toYaw=renderedYaw=p.yaw;sampleStarted=now
          cameraTarget.set(p.x,p.y+1,p.z);controls.target.copy(cameraTarget);camera.position.copy(cameraTarget).add(new THREE.Vector3(34,29,34));positioned=true
        } else {
          fromPlayer.copy(renderedPlayer);toPlayer.copy(nextPosition)
          fromYaw=renderedYaw;toYaw=fromYaw+THREE.MathUtils.euclideanModulo(p.yaw-fromYaw+180,360)-180
          sampleDuration=Math.max(80,Math.min(220,now-sampleStarted));sampleStarted=now
        }
        lastSample=p
      }
      const blend=Math.min(1,(now-sampleStarted)/sampleDuration)
      renderedPlayer.copy(fromPlayer).lerp(toPlayer,blend)
      renderedYaw=THREE.MathUtils.lerp(fromYaw,toYaw,blend)
      void updateAvatarSkin(p.uuid)
      playerGroup.position.copy(renderedPlayer);playerGroup.rotation.y=-THREE.MathUtils.degToRad(renderedYaw)
      const nextState=avatarState(p)
      if(nextState!==currentAvatarState){avatar.resetJoints();avatarAnimations[nextState].progress=0;currentAvatarState=nextState}
      const speed=Math.hypot(p.velocity.x,p.velocity.z)
      avatarAnimations[nextState].speed=nextState==='walking'||nextState==='running'?THREE.MathUtils.clamp(speed*7,.7,2.2):1
      avatarAnimations[nextState].update(avatar,delta)
      // RunningAnimation moves the whole PlayerObject up and down by a full
      // model unit, replacing the feet offset. Keep the animated limbs but
      // anchor the model root to Minecraft's reported feet position.
      avatar.position.set(0, avatarFeetOffset, 0)
      avatar.rotation.z = 0
      if(p.usingItem){avatar.skin.rightArm.rotation.x=-1.52+Math.sin(now*.012)*.08;avatar.skin.rightArm.rotation.z=-.18;avatar.skin.leftArm.rotation.x=-.4;avatar.skin.head.rotation.x=-.12}
      if(latest.current.follow){cameraTarget.copy(renderedPlayer);cameraTarget.y+=1;cameraShift.copy(cameraTarget).sub(controls.target).multiplyScalar(1-Math.exp(-delta*8));controls.target.add(cameraShift);camera.position.add(cameraShift)}
      controls.update();renderer.render(scene,camera)
      frames++;if(now-mark>1000){lastFps.current=Math.round(frames*1000/(now-mark));frames=0;mark=now;onStats(lastFaces.current,lastFps.current)}
    };animate()
    return()=>{active=false;cancelAnimationFrame(animation);avatarTexture?.dispose();observer.disconnect();controls.dispose();renderer.domElement.removeEventListener('pointermove',move);renderer.domElement.removeEventListener('pointerleave',leave);renderer.domElement.removeEventListener('click',click);renderer.domElement.removeEventListener('contextmenu',menu);disposeResources(scene);renderer.dispose();hostElement.replaceChildren();runtime.current=null;apiRef.current=null}
  },[apiRef,onStats])
  useEffect(()=>{const value=runtime.current;if(!value)return;for(const[key,object]of value.chunks)if(!chunks.has(key)||chunks.get(key)?.revision!==object.userData.revision){dispose(object);value.chunks.delete(key)}for(const[key,chunk]of chunks)if(!value.chunks.has(key)){const object=chunkObject(chunk);value.world.add(object);value.chunks.set(key,object)}lastFaces.current=[...value.chunks.values()].reduce((sum,group)=>sum+Number(group.userData.faces??0),0);onStats(lastFaces.current,lastFps.current)},[chunks,onStats])
  useEffect(()=>{const group=runtime.current?.entities;if(!group)return;while(group.children.length)dispose(group.children[0]);if(!showEntities)return;for(const entity of entities.values()){const holder=new THREE.Group();holder.position.set(entity.x,entity.y,entity.z);const item=entity.type==='Dropped Item';const mesh=new THREE.Mesh(new THREE.BoxGeometry(item?.35:.72,item?.35:1.15,.72),new THREE.MeshStandardMaterial({color:entity.hostile?0xff496a:item?0xffcf4a:0x75d69a}));mesh.position.y=item?.25:.58;holder.add(mesh);group.add(holder)}},[entities,showEntities])
  useEffect(()=>{const group=runtime.current?.path;if(!group)return;while(group.children.length)dispose(group.children[0]);if(!showPath||path.length<2)return;group.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints(path.map(p=>new THREE.Vector3(p.x,p.y+.18,p.z))),new THREE.LineBasicMaterial({color:0x22c7ff})));const g=path.at(-1)!;const marker=new THREE.Mesh(new THREE.TorusGeometry(.7,.08,10,32),new THREE.MeshBasicMaterial({color:0x55e7ff}));marker.rotation.x=Math.PI/2;marker.position.set(g.x,g.y+.15,g.z);group.add(marker)},[path,showPath])
  useEffect(()=>{runtime.current?.world.traverse(object=>{if(object.name==='block-grid')object.visible=showGrid})},[showGrid,chunks])
  return <div className="world-canvas" ref={host}/>
}
