import type { Binding, Column, DevicePOI, Layer, Point, ShapeType, Space, TargetType } from './types'

export const isUuid = (value: string) => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value)

export function uid(): string {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  const bytes = new Uint8Array(16)
  if (globalThis.crypto?.getRandomValues) globalThis.crypto.getRandomValues(bytes)
  else for (let i = 0; i < bytes.length; i++) bytes[i] = Math.floor(Math.random() * 256)
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}
export const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value))
export const round = (n: number) => Math.round(n * 100) / 100
export const fitNotch = (value: number, dimension: number) => Math.min(Math.max(Number.isFinite(value) ? value : 0.001, 0.001), dimension / 2)

function clinicFloorLayers(parentId: string): Layer[] {
  return Array.from({ length: 7 }, (_, index) => ({
    id: uid(), name: `${index + 1}F`, parentId, order: index, visible: true, locked: false
  }))
}

export function upgradeDefaultClinicLayers(space: Space): boolean {
  const root = space.layers.find(layer => !layer.parentId && layer.name === '默认图层')
  if (!root) return false
  root.name = '门诊'
  for (const floor of clinicFloorLayers(root.id)) {
    if (space.layers.some(layer => layer.parentId === root.id && layer.name === floor.name)) continue
    space.layers.push(floor)
  }
  const children = space.layers.filter(layer => layer.parentId === root.id)
  const floorNames = Array.from({ length: 7 }, (_, index) => `${index + 1}F`)
  const others = children.filter(layer => !floorNames.includes(layer.name)).sort((a, b) => a.order - b.order)
  for (const layer of children) {
    const floorIndex = floorNames.indexOf(layer.name)
    if (floorIndex >= 0) layer.order = floorIndex
  }
  others.forEach((layer, index) => { layer.order = 7 + index })
  return true
}

export function emptySpace(): Space {
  const clinicId = uid()
  return {
    version: 1, id: uid(), name: '新建空间', origin: { x: 0, y: 0 }, scale: 1,
    unit: 'm', gridSize: 1, gridVisible: true, gridColor: '#b8cce2', gridOpacity: 0.65,
    bounds: { width: 100, depth: 80 },
    map: null,
    layers: [{ id: clinicId, name: '门诊', order: 0, visible: true, locked: false, parentId: null }, ...clinicFloorLayers(clinicId)],
    columns: [], devices: [], bindings: [], publishedAt: null
  }
}

export function newColumn(type: ShapeType, layerId: string, at: Point, order: number): Column {
  const width = type === 'cube' ? 5 : type === 'cylinder' ? 5 : 8
  const depth = type === 'cube' || type === 'cylinder' ? width : 6
  return {
    id: uid(), type, name: `${{ box: '长方体', cube: '正方体', lshape: 'L 形体块', cylinder: '圆柱体', polygon: '多边形' }[type]} ${order + 1}`,
    layerId, order, x: round(at.x), y: round(at.y), width, depth, height: 3,
    rotation: 0, opacity: 0.72, fillColor: '#4d93f7', strokeColor: '#1768d8',
    strokeWidth: 2, showLabel: false, locked: false, visible: true, points: [],
    notchWidth: 3, notchDepth: 3
  }
}

export function layerFor(space: Space, id: string): Layer | undefined {
  return space.layers.find(layer => layer.id === id)
}

export function layerAncestors(space: Space, id: string): Layer[] {
  const result: Layer[] = []
  const seen = new Set<string>()
  let current = layerFor(space, id)
  while (current && !seen.has(current.id)) {
    result.push(current)
    seen.add(current.id)
    current = current.parentId ? layerFor(space, current.parentId) : undefined
  }
  return result
}

export function layerVisible(space: Space, id: string): boolean {
  const path = layerAncestors(space, id)
  return !!path.length && !path[path.length - 1].parentId && path.every(layer => layer.visible)
}

export function layerEditable(space: Space, id: string): boolean {
  const path = layerAncestors(space, id)
  return !!path.length && !path[path.length - 1].parentId && path.every(layer => layer.visible && !layer.locked)
}

export function layerIsDescendant(space: Space, id: string, ancestorId: string): boolean {
  return layerAncestors(space, id).some(layer => layer.id === ancestorId)
}

export function orderedLayers(space: Space): Layer[] {
  const result: Layer[] = []
  const visited = new Set<string>()
  function visit(parentId: string | null) {
    const children = space.layers.filter(layer => (layer.parentId || null) === parentId)
      .sort((a, b) => a.order - b.order)
    for (const layer of children) {
      if (visited.has(layer.id)) continue
      visited.add(layer.id)
      result.push(layer)
      visit(layer.id)
    }
  }
  visit(null)
  return result
}

export function layerSubtreeIds(space: Space, id: string): Set<string> {
  if (!layerFor(space, id)) return new Set()
  return new Set(space.layers.filter(layer => layerIsDescendant(space, layer.id, id)).map(layer => layer.id))
}

export function layerDeletionPreview(space: Space, id: string) {
  const layerIds = layerSubtreeIds(space, id)
  const columnIds = new Set(space.columns.filter(column => layerIds.has(column.layerId)).map(column => column.id))
  const bindings = space.bindings.filter(binding =>
    binding.targetType === 'layer' && layerIds.has(binding.targetId) ||
    binding.targetType === 'column' && columnIds.has(binding.targetId))
  return { layerIds, columnIds, bindingIds: new Set(bindings.map(binding => binding.id)),
    deviceIds: new Set(bindings.map(binding => binding.deviceId)) }
}

export function deleteLayerTree(space: Space, id: string): boolean {
  const removal = layerDeletionPreview(space, id)
  if (!removal.layerIds.size) return false
  space.layers = space.layers.filter(layer => !removal.layerIds.has(layer.id))
  space.columns = space.columns.filter(column => !removal.columnIds.has(column.id))
  space.bindings = space.bindings.filter(binding => !removal.bindingIds.has(binding.id))
  const stillBound = new Set(space.bindings.map(binding => binding.deviceId))
  space.devices = space.devices.filter(device => !removal.deviceIds.has(device.deviceId) || stillBound.has(device.deviceId))
  return true
}

export function isEditable(space: Space, column: Column): boolean {
  return layerEditable(space, column.layerId) && !column.locked && column.visible
}

export function isBindingEditable(space: Space, binding: Binding): boolean {
  if (binding.targetType === 'column') {
    const column = space.columns.find(c => c.id === binding.targetId)
    return !!column && isEditable(space, column)
  }
  if (binding.targetType === 'layer') {
    return layerEditable(space, binding.targetId)
  }
  return binding.targetType === 'space' && binding.targetId === space.id
}

export function localToWorld(space: Space, local: Point): Point {
  return { x: space.origin.x + local.x * space.scale, y: space.origin.y + local.y * space.scale }
}

export function worldToLocal(space: Space, world: Point): Point {
  return { x: round((world.x - space.origin.x) / space.scale), y: round((world.y - space.origin.y) / space.scale) }
}

export function rotate(point: Point, angle: number): Point {
  const a = angle * Math.PI / 180
  return { x: point.x * Math.cos(a) - point.y * Math.sin(a), y: point.x * Math.sin(a) + point.y * Math.cos(a) }
}

export function bindingPosition(space: Space, binding: Binding): Point {
  if (binding.targetType === 'column') {
    const target = space.columns.find(c => c.id === binding.targetId)
    if (target) {
      const offset = rotate({ x: binding.localX, y: binding.localY }, target.rotation)
      return { x: round(target.x + offset.x), y: round(target.y + offset.y) }
    }
  }
  return { x: binding.localX, y: binding.localY }
}

export function makeBinding(space: Space, device: DevicePOI, targetType: TargetType, targetId: string, point: Point): Binding {
  let local = point
  if (targetType === 'column') {
    const column = space.columns.find(c => c.id === targetId)
    if (column) local = rotate({ x: point.x - column.x, y: point.y - column.y }, -column.rotation)
  }
  return {
    id: uid(), deviceId: device.deviceId, targetType, targetId,
    localX: round(local.x), localY: round(local.y), z: device.z,
    label: device.name, showLine: true
  }
}

function validText(value: unknown, maxLength: number): boolean {
  return typeof value === 'string' && !!value.trim() && value.length <= maxLength
}
const positiveFinite = (value: number) => Number.isFinite(value) && value > 0

export function validateSpace(space: Space): string[] {
  const errors: string[] = []
  if (!isUuid(space.id)) errors.push('空间 ID 必须为 UUID')
  if (!validText(space.name, 120)) errors.push('空间名称不能为空且不能超过 120 字')
  if (!validText(space.unit, 32)) errors.push('单位不能为空且不能超过 32 字')
  if (!positiveFinite(space.scale) || !positiveFinite(space.gridSize)) errors.push('比例尺和网格尺寸必须大于 0')
  if (!Number.isFinite(space.origin.x) || !Number.isFinite(space.origin.y)) errors.push('空间原点坐标无效')
  if (typeof space.gridVisible !== 'boolean' || !/^#[0-9a-f]{6}$/i.test(space.gridColor) || !(space.gridOpacity >= 0 && space.gridOpacity <= 1)) errors.push('网格设置无效')
  if (!positiveFinite(space.bounds.width) || !positiveFinite(space.bounds.depth)) errors.push('空间边界必须大于 0')
  if (space.map && (!positiveFinite(space.map.width) || !positiveFinite(space.map.depth) || !Number.isFinite(space.map.x) || !Number.isFinite(space.map.y) || !(space.map.opacity >= 0 && space.map.opacity <= 1) || !Number.isFinite(space.map.thickness) || space.map.thickness < 0)) errors.push('地图位置、尺寸、透明度或厚度无效')
  if (space.map && (!validText(space.map.name, 255) || !/^data:image\/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+$/i.test(space.map.dataUrl))) errors.push('地图文件名称或图片数据无效')
  const layers = new Set(space.layers.map(l => l.id))
  const columns = new Map(space.columns.map(c => [c.id, c]))
  const devices = new Set(space.devices.map(d => d.deviceId))
  if (layers.size !== space.layers.length) errors.push('图层 ID 重复')
  for (const layer of space.layers) {
    if (!validText(layer.name, 120) || !Number.isInteger(layer.order)) errors.push('图层名称或顺序无效')
    if (layer.parentId != null && (typeof layer.parentId !== 'string' || !layer.parentId.trim())) errors.push(`${layer.name} 的父级图层 ID 无效`)
    if (layer.parentId && !layers.has(layer.parentId)) errors.push(`${layer.name} 的父级图层不存在`)
    if (layer.parentId && layerIsDescendant(space, layer.parentId, layer.id)) errors.push(`${layer.name} 的父子图层存在循环`)
  }
  if (columns.size !== space.columns.length) errors.push('体块 ID 重复')
  if (devices.size !== space.devices.length) errors.push('设备 ID 重复')
  const bound = new Set<string>()
  for (const c of space.columns) {
    if (!layers.has(c.layerId)) errors.push(`${c.name} 所属图层不存在`)
    if (!validText(c.name, 120) || !Number.isInteger(c.order)) errors.push(`${c.name} 名称或顺序无效`)
    if (!positiveFinite(c.width) || !positiveFinite(c.depth) || !Number.isFinite(c.height) || c.height < 0) errors.push(`${c.name} 尺寸无效`)
    if (!Number.isFinite(c.x) || !Number.isFinite(c.y) || !Number.isFinite(c.rotation)) errors.push(`${c.name} 坐标或旋转角度无效`)
    if (!(c.opacity > 0 && c.opacity <= 1)) errors.push(`${c.name} 透明度无效`)
    if (!Number.isFinite(c.strokeWidth) || c.strokeWidth < 0) errors.push(`${c.name} 描边宽度无效`)
    if (c.type === 'lshape' && (!(c.notchWidth > 0 && c.notchWidth < c.width) || !(c.notchDepth > 0 && c.notchDepth < c.depth))) errors.push(`${c.name} 缺口尺寸无效`)
    if (c.type === 'polygon' && c.points.length < 3) errors.push(`${c.name} 至少需要三个顶点`)
    if (Math.abs(c.x) > space.bounds.width / 2 || Math.abs(c.y) > space.bounds.depth / 2) errors.push(`${c.name} 超出空间边界`)
  }
  for (const d of space.devices) {
    if (!validText(d.name, 120) || !validText(d.deviceId, 128)) errors.push('设备名称或 ID 无效')
    if (![d.x, d.y, d.z].every(Number.isFinite) || Math.abs(d.x) > space.bounds.width / 2 || Math.abs(d.y) > space.bounds.depth / 2) errors.push(`${d.name} 点位坐标无效`)
  }
  for (const b of space.bindings) {
    if (bound.has(b.deviceId)) errors.push(`设备 ${b.deviceId} 重复绑定`)
    bound.add(b.deviceId)
    if (!devices.has(b.deviceId)) errors.push(`绑定 ${b.id} 的设备点位不存在`)
    if (b.targetType === 'column' && !columns.has(b.targetId)) errors.push(`绑定 ${b.id} 的体块不存在`)
    if (b.targetType === 'layer' && !layers.has(b.targetId)) errors.push(`绑定 ${b.id} 的图层不存在`)
    if (b.targetType === 'space' && b.targetId !== space.id) errors.push(`绑定 ${b.id} 的空间不存在`)
    if (!['space', 'column', 'layer'].includes(b.targetType)) errors.push(`绑定 ${b.id} 的目标类型无效`)
    const p = bindingPosition(space, b)
    if (Math.abs(p.x) > space.bounds.width / 2 || Math.abs(p.y) > space.bounds.depth / 2) errors.push(`设备 ${b.label} 超出空间边界`)
    const device = space.devices.find(d => d.deviceId === b.deviceId)
    if (!Number.isFinite(b.localX) || !Number.isFinite(b.localY) || !Number.isFinite(b.z) ||
        device && (Math.abs(device.x - p.x) > 0.011 || Math.abs(device.y - p.y) > 0.011 || Math.abs(device.z - b.z) > 0.011)) errors.push(`设备 ${b.label} 的点位与绑定坐标不一致`)
  }
  return errors
}

export function parseSpace(json: string): Space {
  const value: unknown = JSON.parse(json)
  if (!value || typeof value !== 'object') throw new Error('文件不是空间 JSON')
  const s = value as Partial<Space>
  if (s.version !== 1 || typeof s.id !== 'string' || typeof s.name !== 'string' || typeof s.unit !== 'string' || !Array.isArray(s.layers) || !Array.isArray(s.columns) || !Array.isArray(s.devices) || !Array.isArray(s.bindings)) {
    throw new Error('空间 JSON 格式或版本不受支持')
  }
  if (!s.origin || !s.bounds || typeof s.scale !== 'number' || typeof s.gridSize !== 'number') throw new Error('空间坐标配置无效')
  if (!s.layers.every(l => l && typeof l.id === 'string' && typeof l.order === 'number' && (l.parentId == null || typeof l.parentId === 'string')) ||
      !s.columns.every(c => c && typeof c.id === 'string' && typeof c.type === 'string' && Array.isArray(c.points) && typeof c.x === 'number' && typeof c.y === 'number') ||
      !s.devices.every(d => d && typeof d.deviceId === 'string') ||
      !s.bindings.every(b => b && typeof b.deviceId === 'string' && typeof b.targetId === 'string')) throw new Error('空间对象结构无效')
  if (s.map != null && (typeof s.map !== 'object' || !/^data:image\/(png|jpeg|webp);base64,/i.test(s.map.dataUrl || '') || typeof s.map.name !== 'string' ||
    !['x', 'y', 'width', 'depth', 'opacity'].every(key => typeof (s.map as unknown as Record<string, unknown>)[key] === 'number'))) throw new Error('地图数据无效')
  if (s.gridVisible !== undefined && typeof s.gridVisible !== 'boolean' ||
    s.gridColor !== undefined && !/^#[0-9a-f]{6}$/i.test(s.gridColor) ||
    s.gridOpacity !== undefined && (typeof s.gridOpacity !== 'number' || s.gridOpacity < 0 || s.gridOpacity > 1) ||
    s.map?.thickness !== undefined && (typeof s.map.thickness !== 'number' || !Number.isFinite(s.map.thickness) || s.map.thickness < 0)) throw new Error('网格或地图设置无效')
  return {
    ...clone(s as Space), gridVisible: s.gridVisible ?? true, gridColor: s.gridColor ?? '#b8cce2', gridOpacity: s.gridOpacity ?? 0.65,
    map: s.map ? { ...clone(s.map), thickness: s.map.thickness ?? 1.2 } : null
  }
}
