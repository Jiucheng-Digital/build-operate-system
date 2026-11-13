<template>
  <div ref="host" class="space-canvas" :class="`is-${tool}`"
    @pointerdown="pointerDown" @pointermove="pointerMove" @pointerup="pointerUp" @pointercancel="cancel"
    @wheel.prevent="wheel" @contextmenu.prevent="contextMenu" @dragover.prevent @drop.prevent="dropDevice">
    <div v-if="overlay" class="space-canvas__overlay" :style="overlayStyle"></div>
    <div v-if="guideX !== null && !preview" class="space-canvas__guide space-canvas__guide--x" :style="{ left: `${guideClientX}px` }"></div>
    <div v-if="guideY !== null && !preview" class="space-canvas__guide space-canvas__guide--y" :style="{ top: `${guideClientY}px` }"></div>
    <div v-if="polygonPoints.length" class="space-canvas__hint">多边形：单击添加顶点，双击或 Enter 完成 · Esc 取消</div>
    <div v-if="!space.columns.length && !space.bindings.length && !space.map" class="space-canvas__empty">
      <strong>空间画布已就绪</strong><span>上传地图或选择体块工具，在画布中单击或拖拽创建</span>
    </div>
    <div v-if="renderError" class="space-canvas__error" role="alert">{{ renderError }}</div>
    <div class="space-canvas__scale">{{ scaleLength }} {{ space.unit }} <i :style="{ width: `${scalePixels}px` }"></i></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { clone, isBindingEditable, isEditable, layerVisible, rotate, round } from './model'
import { SpaceScene } from './scene'
import type { Point, Selection, ShapeType, Space, Tool } from './types'

const props = defineProps<{
  space: Space; selection: Selection[]; tool: Tool; snap: boolean; activeLayerId: string; preview: boolean; readonly: boolean; cancelToken: number
}>()
const emit = defineEmits<{
  select: [selection: Selection[]]
  create: [type: ShapeType, at: Point, size: { width: number; depth: number } | null, points: Point[]]
  move: [selection: Selection[], delta: Point]
  resize: [id: string, width: number, depth: number]
  bind: [point: Point, target: { kind: string; id: string } | null, deviceId: string | null]
  hover: [point: Point]
  zoom: [zoom: number]
  context: [event: { x: number; y: number; hit: { kind: string; id: string } | null }]
}>()

const host = ref<HTMLElement | null>(null)
const renderError = ref('')
let scene: SpaceScene | null = null
let spaceHeld = false
let start: { client: Point; local: Point; hit: ReturnType<SpaceScene['pick']>; mode: string; selection: Selection[]; originalWidth?: number; originalDepth?: number } | null = null
const overlay = ref<{ x: number; y: number; width: number; height: number } | null>(null)
const polygonPoints = ref<Point[]>([])
const guideX = ref<number | null>(null)
const guideY = ref<number | null>(null)
const zoomValue = ref(1)
const guideClientX = computed(() => guideX.value === null || !scene || !host.value ? 0 : scene.localToClient({ x: guideX.value, y: 0 }).x - host.value.getBoundingClientRect().left)
const guideClientY = computed(() => guideY.value === null || !scene || !host.value ? 0 : scene.localToClient({ x: 0, y: guideY.value }).y - host.value.getBoundingClientRect().top)
const scaleLength = computed(() => props.space.gridSize * 5)
const scalePixels = computed(() => {
  if (props.preview && scene) {
    const a = scene.localToClient({ x: 0, y: 0 }), b = scene.localToClient({ x: scaleLength.value, y: 0 })
    return Math.max(20, Math.min(220, Math.abs(b.x - a.x)))
  }
  return Math.max(20, Math.min(220, (scaleLength.value * props.space.scale / 70) * (host.value?.clientHeight || 500) * zoomValue.value))
})
const overlayStyle = computed(() => overlay.value ? {
  left: `${overlay.value.x}px`, top: `${overlay.value.y}px`, width: `${overlay.value.width}px`, height: `${overlay.value.height}px`
} : {})

function snapped(point: Point): Point {
  if (!props.snap) return { x: round(point.x), y: round(point.y) }
  const grid = props.space.gridSize
  return { x: round(Math.round(point.x / grid) * grid), y: round(Math.round(point.y / grid) * grid) }
}

function alignedDelta(raw: Point, selection: Selection[]): Point {
  const grid = props.space.gridSize
  const first = props.space.columns.find(c => selection.some(s => s.kind === 'column' && s.id === c.id))
  const delta = props.snap && first
    ? { x: round(Math.round((first.x + raw.x) / grid) * grid - first.x), y: round(Math.round((first.y + raw.y) / grid) * grid - first.y) }
    : props.snap ? snapped(raw) : { x: round(raw.x), y: round(raw.y) }
  guideX.value = null; guideY.value = null
  if (!props.snap || !first) return delta
  const selected = new Set(selection.filter(s => s.kind === 'column').map(s => s.id))
  let bestX = Math.min(0.4, grid * 0.45), bestY = Math.min(0.4, grid * 0.45)
  const extent = (c: typeof first) => {
    const a = c.rotation * Math.PI / 180
    return { x: (Math.abs(c.width * Math.cos(a)) + Math.abs(c.depth * Math.sin(a))) / 2,
      y: (Math.abs(c.width * Math.sin(a)) + Math.abs(c.depth * Math.cos(a))) / 2 }
  }
  for (const moving of props.space.columns.filter(c => selected.has(c.id))) {
    const a = extent(moving)
    for (const other of props.space.columns.filter(c => !selected.has(c.id) && c.visible && layerVisible(props.space, c.layerId))) {
      const b = extent(other)
      for (const target of [other.x - b.x, other.x, other.x + b.x]) for (const current of [moving.x + delta.x - a.x, moving.x + delta.x, moving.x + delta.x + a.x]) {
        const gap = target - current
        if (Math.abs(gap) < bestX) { bestX = Math.abs(gap); delta.x = round(delta.x + gap); guideX.value = target }
      }
      for (const target of [other.y - b.y, other.y, other.y + b.y]) for (const current of [moving.y + delta.y - a.y, moving.y + delta.y, moving.y + delta.y + a.y]) {
        const gap = target - current
        if (Math.abs(gap) < bestY) { bestY = Math.abs(gap); delta.y = round(delta.y + gap); guideY.value = target }
      }
    }
  }
  return delta
}

function local(event: PointerEvent): Point | null { return scene?.clientToLocal(event.clientX, event.clientY) || null }
function selectionIncludes(kind: string, id: string) { return props.selection.some(s => s.kind === kind && s.id === id) }

function pointerDown(event: PointerEvent) {
  if (!scene || event.button === 2 || !host.value) return
  const point = local(event)
  if (!point) return
  const hit = scene.pick(event.clientX, event.clientY)
  const pan = props.tool === 'pan' || spaceHeld || event.button === 1
  if (props.tool === 'polygon' && !pan) {
    const next = snapped(point)
    const previous = polygonPoints.value[polygonPoints.value.length - 1]
    if (!previous || previous.x !== next.x || previous.y !== next.y) polygonPoints.value.push(next)
    if (event.detail >= 2) finishPolygon()
    return
  }
  let mode = pan ? 'pan' : props.tool === 'marquee' ? 'marquee' : props.tool === 'bind' ? 'bind' : 'create'
  let nextSelection = [...props.selection]
  if (props.tool === 'select' && !pan) {
    if (hit?.kind === 'handle') mode = 'resize'
    else if (hit?.kind === 'column' || hit?.kind === 'device') {
      mode = 'move'
      const item = { kind: hit.kind, id: hit.id } as Selection
      if (event.shiftKey || event.ctrlKey || event.metaKey) {
        nextSelection = selectionIncludes(hit.kind, hit.id) ? nextSelection.filter(s => !(s.kind === hit.kind && s.id === hit.id)) : [...nextSelection, item]
      } else if (!selectionIncludes(hit.kind, hit.id)) nextSelection = [item]
      emit('select', nextSelection)
      if (hit.kind === 'column') {
        const column = props.space.columns.find(c => c.id === hit.id)
        if (!column || !isEditable(props.space, column)) mode = 'none'
      } else {
        const device = props.space.devices.find(d => d.id === hit.id)
        const binding = props.space.bindings.find(b => b.deviceId === device?.deviceId)
        if (!binding || !isBindingEditable(props.space, binding)) mode = 'none'
      }
    } else {
      mode = 'marquee'
      if (!event.shiftKey && !event.ctrlKey && !event.metaKey) emit('select', [])
    }
  }
  if (props.readonly && ['move', 'resize', 'create', 'bind'].includes(mode)) mode = 'none'
  const column = hit?.kind === 'handle' ? props.space.columns.find(c => c.id === hit.id) : null
  start = { client: { x: event.clientX, y: event.clientY }, local: point, hit, mode, selection: nextSelection,
    originalWidth: column?.width, originalDepth: column?.depth }
  host.value.setPointerCapture(event.pointerId)
}

function pointerMove(event: PointerEvent) {
  if (!scene || !host.value) return
  const point = local(event)
  if (point) emit('hover', point)
  if (!start) return
  if (start.mode === 'pan') {
    scene.panPixels(event.movementX, event.movementY)
    return
  }
  if (start.mode === 'move' && point) {
    const delta = alignedDelta({ x: point.x - start.local.x, y: point.y - start.local.y }, start.selection)
    scene.previewMove(start.selection, delta)
    return
  }
  if (start.mode === 'marquee' || start.mode === 'create') {
    const rect = host.value.getBoundingClientRect()
    const x1 = start.client.x - rect.left, y1 = start.client.y - rect.top
    const x2 = event.clientX - rect.left, y2 = event.clientY - rect.top
    overlay.value = { x: Math.min(x1, x2), y: Math.min(y1, y2), width: Math.abs(x2 - x1), height: Math.abs(y2 - y1) }
  }
}

function pointerUp(event: PointerEvent) {
  if (!scene || !start) return
  const end = local(event)
  const drag = Math.hypot(event.clientX - start.client.x, event.clientY - start.client.y)
  if (end && start.mode === 'create' && ['box', 'cube', 'lshape', 'cylinder'].includes(props.tool)) {
    const begin = snapped(start.local), finish = snapped(end)
    const large = drag > 5
    const at = large ? { x: round((begin.x + finish.x) / 2), y: round((begin.y + finish.y) / 2) } : begin
    const width = Math.max(props.space.gridSize, Math.abs(finish.x - begin.x))
    const depth = props.tool === 'cube' || props.tool === 'cylinder' ? width : Math.max(props.space.gridSize, Math.abs(finish.y - begin.y))
    emit('create', props.tool as ShapeType, at, large ? { width, depth } : null, [])
  }
  if (end && start.mode === 'bind' && drag < 7) emit('bind', snapped(end), start.hit?.kind === 'handle' ? { kind: 'column', id: start.hit.id } : start.hit, null)
  if (end && start.mode === 'move' && drag > 3) {
    const delta = alignedDelta({ x: end.x - start.local.x, y: end.y - start.local.y }, start.selection)
    if (delta.x || delta.y) emit('move', start.selection, delta)
  }
  if (end && start.mode === 'resize' && start.hit?.kind === 'handle') {
    const column = props.space.columns.find(c => c.id === start?.hit?.id)
    if (column) {
      const delta = rotate({ x: end.x - start.local.x, y: end.y - start.local.y }, -column.rotation)
      const signX = start.hit.handle === 0 || start.hit.handle === 3 ? -1 : 1
      const signY = start.hit.handle === 0 || start.hit.handle === 1 ? -1 : 1
      const width = Math.max(props.space.gridSize, round((start.originalWidth || column.width) + delta.x * signX * 2))
      const depth = column.type === 'cube' || column.type === 'cylinder' ? width : Math.max(props.space.gridSize, round((start.originalDepth || column.depth) + delta.y * signY * 2))
      emit('resize', column.id, width, depth)
    }
  }
  if (start.mode === 'marquee') {
    const additive = event.shiftKey || event.ctrlKey || event.metaKey
    if (drag > 5) {
      const x1 = Math.min(start.client.x, event.clientX), x2 = Math.max(start.client.x, event.clientX)
      const y1 = Math.min(start.client.y, event.clientY), y2 = Math.max(start.client.y, event.clientY)
      const found: Selection[] = []
      for (const column of props.space.columns) {
        if (!isEditable(props.space, column)) continue
        const b = scene.boundsInClient(column)
        if (b.left <= x2 && b.right >= x1 && b.top <= y2 && b.bottom >= y1) found.push({ kind: 'column', id: column.id })
      }
      for (const device of props.space.devices) {
        const binding = props.space.bindings.find(b => b.deviceId === device.deviceId)
        if (!binding || !isBindingEditable(props.space, binding)) continue
        const p = scene.localToClient(device)
        if (p.x >= x1 && p.x <= x2 && p.y >= y1 && p.y <= y2) found.push({ kind: 'device', id: device.id })
      }
      emit('select', additive ? [...props.selection, ...found.filter(f => !selectionIncludes(f.kind, f.id))] : found)
    }
  }
  overlay.value = null
  guideX.value = null; guideY.value = null
  if (start.mode === 'move') scene.previewMove([], { x: 0, y: 0 })
  start = null
}

function finishPolygon() {
  const points = polygonPoints.value.slice()
  polygonPoints.value = []
  if (points.length < 3) return
  const minX = Math.min(...points.map(p => p.x)), maxX = Math.max(...points.map(p => p.x))
  const minY = Math.min(...points.map(p => p.y)), maxY = Math.max(...points.map(p => p.y))
  const center = { x: round((minX + maxX) / 2), y: round((minY + maxY) / 2) }
  emit('create', 'polygon', center, { width: Math.max(0.2, maxX - minX), depth: Math.max(0.2, maxY - minY) }, points.map(p => ({ x: round(p.x - center.x), y: round(p.y - center.y) })))
}

function cancel() { start = null; overlay.value = null; polygonPoints.value = []; guideX.value = null; guideY.value = null; scene?.previewMove([], { x: 0, y: 0 }) }
function wheel(event: WheelEvent) { scene?.zoomAt(event.clientX, event.clientY, event.deltaY > 0 ? 1.12 : 0.89) }
function contextMenu(event: MouseEvent) { emit('context', { x: event.clientX, y: event.clientY, hit: scene?.pick(event.clientX, event.clientY) || null }) }
function dropDevice(event: DragEvent) {
  const deviceId = event.dataTransfer?.getData('application/x-neucity-device')
  const point = scene?.clientToLocal(event.clientX, event.clientY)
  if (deviceId && point) emit('bind', snapped(point), scene?.pick(event.clientX, event.clientY) || null, deviceId)
}
function keyDown(event: KeyboardEvent) {
  if (event.code === 'Space' && !(event.target instanceof HTMLInputElement)) { spaceHeld = true; event.preventDefault() }
  if (event.key === 'Escape') cancel()
  if (event.key === 'Enter' && polygonPoints.value.length) finishPolygon()
}
function keyUp(event: KeyboardEvent) { if (event.code === 'Space') spaceHeld = false }

onMounted(() => {
  if (!host.value) return
  try {
    scene = new SpaceScene(host.value, zoom => { zoomValue.value = zoom; emit('zoom', zoom) })
    scene.sync(props.space, props.selection)
    scene.setPreview(props.preview)
  } catch (_error) {
    scene?.dispose()
    scene = null
    renderError.value = 'Three.js 画布无法启动，请检查浏览器 WebGL 支持并刷新重试。'
  }
  window.addEventListener('keydown', keyDown)
  window.addEventListener('keyup', keyUp)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', keyDown)
  window.removeEventListener('keyup', keyUp)
  scene?.dispose()
  scene = null
})
watch(() => [props.space, props.selection], () => scene?.sync(props.space, props.selection))
watch(() => props.preview, value => scene?.setPreview(value))
watch(() => props.cancelToken, cancel)
watch(() => props.tool, value => { if (value !== 'polygon') polygonPoints.value = [] })
</script>

<style scoped>
.space-canvas { position: relative; min-width: 0; min-height: 0; width: 100%; height: 100%; overflow: hidden; background: #f9fbff; cursor: crosshair; touch-action: none; }
.space-canvas.is-select { cursor: default; } .space-canvas.is-pan { cursor: grab; }
.space-canvas :deep(.space-canvas__webgl), .space-canvas :deep(.space-canvas__labels) { position: absolute; inset: 0; }
.space-canvas :deep(.space-canvas__labels) { z-index: 2; }
.space-canvas :deep(.space-object-label) { max-width: 128px; padding: 3px 6px; border: 1px solid #bfd4f3; border-radius: 5px; background: #ffffffdd; color: #234566; font-size: 11px; line-height: 1.3; overflow-wrap: anywhere; text-align: center; transform: translate(-50%, -145%); }
.space-canvas :deep(.space-object-label.is-selected) { border-color: #1268f3; color: #075ee8; font-weight: 700; }
.space-canvas :deep(.space-poi-label) { position: relative; width: 0; height: 0; }
.space-canvas :deep(.space-poi-label__bubble) { position: absolute; bottom: 14px; left: 50%; display: block; width: max-content; max-width: 132px; padding: 5px 7px; border: 1px solid #97dbd1; border-radius: 6px; background: #ffffffef; color: #155c55; box-shadow: 0 3px 10px #163a6220; font-size: 11px; line-height: 1.35; overflow-wrap: anywhere; text-align: center; transform: translateX(-50%); }
.space-canvas :deep(.space-origin-label) { padding: 3px 5px; border-radius: 4px; background: #285d94; color: #fff; font-size: 10px; white-space: nowrap; transform: translate(25%, -120%); }
.space-canvas__overlay { position: absolute; z-index: 4; border: 1px solid #1268f3; background: #1268f322; pointer-events: none; }
.space-canvas__guide { position: absolute; z-index: 3; pointer-events: none; background: #18b9ad; box-shadow: 0 0 0 1px #ffffff99; }
.space-canvas__guide--x { top: 0; bottom: 0; width: 1px; }.space-canvas__guide--y { left: 0; right: 0; height: 1px; }
.space-canvas__hint, .space-canvas__scale { position: absolute; z-index: 3; bottom: 14px; padding: 7px 10px; border: 1px solid #dbe7f4; border-radius: 7px; background: #ffffffdd; color: #42617f; font-size: 12px; pointer-events: none; }
.space-canvas__hint { left: 14px; } .space-canvas__scale { right: 14px; display: flex; align-items: center; gap: 7px; }
.space-canvas__scale i { display: block; height: 6px; border: 1px solid #537ca8; border-top: 0; }
.space-canvas__empty { position: absolute; z-index: 1; top: 16px; left: 50%; display: flex; flex-direction: column; align-items: center; gap: 4px; width: max-content; padding: 10px 16px; border: 1px solid #dbe7f4; border-radius: 8px; background: #fffffff0; color: #637a95; font-size: 12px; pointer-events: none; transform: translateX(-50%); }
.space-canvas__empty strong { color: #24476f; font-size: 13px; }
.space-canvas__error { position: absolute; z-index: 6; inset: 0; display: grid; place-items: center; padding: 30px; background: #f9fbff; color: #b74338; text-align: center; }
</style>
