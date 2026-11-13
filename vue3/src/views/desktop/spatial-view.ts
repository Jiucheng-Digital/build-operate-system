import { bindingPosition, layerIsDescendant, layerVisible, orderedLayers } from './apps/SpaceEditor/model'
import type { Binding, Column, DevicePOI, Layer, Point, Space } from './apps/SpaceEditor/types'

export interface LayerRow { layer: Layer; depth: number; hasChildren: boolean; visible: boolean }
export interface PlacedDevice { binding: Binding; device: DevicePOI; point: Point; layerId: string | null }
export interface LocatedEvent<T> { event: T; point: Point }

export function layerTreeRows(space: Space, expandedIds?: ReadonlySet<string>): LayerRow[] {
  const layers: Layer[] = []
  const seen = new Set<string>()
  function visit(parentId: string | null) {
    const children = space.layers.filter(layer => (layer.parentId || null) === parentId)
      .sort((a, b) => b.order - a.order)
    for (const layer of children) {
      if (seen.has(layer.id)) continue
      seen.add(layer.id)
      layers.push(layer)
      visit(layer.id)
    }
  }
  visit(null)
  const parentIds = new Set(layers.map(layer => layer.parentId).filter((id): id is string => !!id))
  return layers.filter(layer => {
    if (!expandedIds) return true
    return layerAncestorsOpen(space, layer, expandedIds)
  }).map(layer => ({ layer, depth: layerDepth(space, layer.id), hasChildren: parentIds.has(layer.id), visible: layerVisible(space, layer.id) }))
}

function layerAncestorsOpen(space: Space, layer: Layer, expandedIds: ReadonlySet<string>): boolean {
  const seen = new Set([layer.id])
  let parentId = layer.parentId
  while (parentId && !seen.has(parentId)) {
    if (!expandedIds.has(parentId)) return false
    seen.add(parentId)
    parentId = space.layers.find(item => item.id === parentId)?.parentId
  }
  return true
}

function layerDepth(space: Space, id: string): number {
  let depth = 0
  const seen = new Set([id])
  let parentId = space.layers.find(layer => layer.id === id)?.parentId
  while (parentId && !seen.has(parentId)) {
    seen.add(parentId)
    depth += 1
    parentId = space.layers.find(layer => layer.id === parentId)?.parentId
  }
  return depth
}

export function containsLayer(space: Space, selectedId: string, candidateId: string): boolean {
  return !selectedId || layerIsDescendant(space, candidateId, selectedId)
}

export function visibleSpatialColumns(space: Space, selectedLayerId = ''): Column[] {
  const layerOrder = new Map(orderedLayers(space).map((layer, index) => [layer.id, index]))
  return space.columns.filter(column => column.visible && layerVisible(space, column.layerId)
    && containsLayer(space, selectedLayerId, column.layerId))
    .sort((a, b) => (layerOrder.get(a.layerId) ?? 0) - (layerOrder.get(b.layerId) ?? 0) || a.order - b.order)
}

export function placedDevices(space: Space, selectedLayerId = ''): PlacedDevice[] {
  const devices = new Map(space.devices.map(device => [device.deviceId, device]))
  const columns = new Map(space.columns.map(column => [column.id, column]))
  return space.bindings.flatMap(binding => {
    const device = devices.get(binding.deviceId)
    if (!device) return []
    let layerId: string | null = null
    if (binding.targetType === 'column') {
      const column = columns.get(binding.targetId)
      if (!column?.visible) return []
      layerId = column.layerId
    } else if (binding.targetType === 'layer') layerId = binding.targetId
    if (layerId && (!layerVisible(space, layerId) || !containsLayer(space, selectedLayerId, layerId))) return []
    return [{ binding, device, point: bindingPosition(space, binding), layerId }]
  })
}

function labelWeight(value: string): number {
  return Array.from(value).reduce((total, character) => total + (/[^\u0000-\u00ff]/.test(character) ? 2 : 1), 0)
}

export function poiLabelLines(name: string): string[] {
  const characters = Array.from((name || '未命名设备').trim())
  const lines: string[] = []
  let line = ''
  let width = 0
  for (const character of characters) {
    const nextWidth = labelWeight(character)
    if (line && width + nextWidth > 14) {
      lines.push(line)
      line = ''
      width = 0
    }
    if (lines.length === 2) {
      const last = lines[1]
      lines[1] = `${Array.from(last).slice(0, -1).join('')}…`
      return lines
    }
    line += character
    width += nextWidth
  }
  if (line) lines.push(line)
  return lines.length ? lines : ['未命名设备']
}

export function poiLabelLayout(name: string, point: Point, bounds: Space['bounds'], radius: number) {
  const lines = poiLabelLines(name)
  const fontSize = radius * 2.35
  const lineHeight = fontSize * 1.22
  const padding = radius * 0.85
  const width = Math.max(...lines.map(labelWeight)) * fontSize * 0.55 + padding * 2
  const height = lines.length * lineHeight + padding * 2
  const gap = radius * 2.35
  const rightRoom = bounds.width / 2 - point.x
  const leftRoom = bounds.width / 2 + point.x
  const x = rightRoom >= gap + width || rightRoom >= leftRoom ? gap : -gap - width
  const minY = -bounds.depth / 2 + point.y
  const maxY = bounds.depth / 2 + point.y - height
  const y = Math.max(minY, Math.min(-height / 2, maxY))
  return { lines, x, y, width, height, fontSize, padding, lineHeight }
}

export function deviceIdForEvent(event: unknown): string | null {
  if (!event || typeof event !== 'object') return null
  const row = event as Record<string, unknown>
  let extend: Record<string, unknown> = {}
  if (row.extend && typeof row.extend === 'object') extend = row.extend as Record<string, unknown>
  else if (typeof row.extend === 'string') {
    try {
      const value: unknown = JSON.parse(row.extend)
      if (value && typeof value === 'object' && !Array.isArray(value)) extend = value as Record<string, unknown>
    } catch { /* 无效扩展字段不用于定位 */ }
  }
  const id = row.deviceId ?? extend.deviceId ?? extend.devIndexCode
  return id === null || id === undefined || String(id).trim() === '' ? null : String(id).trim()
}

export function locateEvents<T>(space: Space, events: T[], selectedLayerId = ''): LocatedEvent<T>[] {
  const devices = new Map(placedDevices(space, selectedLayerId).map(item => [item.device.deviceId, item.point]))
  return events.flatMap(event => {
    const deviceId = deviceIdForEvent(event)
    const point = deviceId ? devices.get(deviceId) : undefined
    return point ? [{ event, point }] : []
  })
}

export function columnOutline(column: Column): Point[] {
  const w = column.width / 2, d = column.depth / 2
  if (column.type === 'polygon' && column.points.length >= 3) return column.points
  if (column.type === 'lshape') {
    const nw = Math.min(Math.max(column.notchWidth, 0.2), column.width - 0.2)
    const nd = Math.min(Math.max(column.notchDepth, 0.2), column.depth - 0.2)
    return [{ x: -w, y: -d }, { x: w, y: -d }, { x: w, y: d - nd },
      { x: w - nw, y: d - nd }, { x: w - nw, y: d }, { x: -w, y: d }]
  }
  if (column.type === 'cylinder') return Array.from({ length: 48 }, (_, index) => {
    const angle = index * Math.PI * 2 / 48
    return { x: Math.cos(angle) * w, y: Math.sin(angle) * d }
  })
  return [{ x: -w, y: -d }, { x: w, y: -d }, { x: w, y: d }, { x: -w, y: d }]
}

export function columnSvgPath(column: Column): string {
  return columnOutline(column).map((point, index) => `${index ? 'L' : 'M'}${point.x},${-point.y}`).join(' ') + ' Z'
}
