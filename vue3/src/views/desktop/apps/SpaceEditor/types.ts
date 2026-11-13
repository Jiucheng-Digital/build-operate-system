export type ShapeType = 'box' | 'cube' | 'lshape' | 'cylinder' | 'polygon'
export type TargetType = 'space' | 'column' | 'layer'
export type Point = { x: number; y: number }

export interface Layer {
  id: string
  name: string
  order: number
  visible: boolean
  locked: boolean
  parentId?: string | null
}

export interface Column {
  id: string
  type: ShapeType
  name: string
  layerId: string
  order: number
  x: number
  y: number
  width: number
  depth: number
  height: number
  rotation: number
  opacity: number
  fillColor: string
  strokeColor: string
  strokeWidth: number
  showLabel: boolean
  locked: boolean
  visible: boolean
  points: Point[]
  notchWidth: number
  notchDepth: number
}

export interface DevicePOI {
  id: string
  deviceId: string
  name: string
  type: string
  status: string
  x: number
  y: number
  z: number
}

export interface Binding {
  id: string
  deviceId: string
  targetType: TargetType
  targetId: string
  localX: number
  localY: number
  z: number
  label: string
  showLine: boolean
}

export interface SpaceMap {
  name: string
  dataUrl: string
  x: number
  y: number
  width: number
  depth: number
  opacity: number
  thickness: number
}

export interface Space {
  version: 1
  id: string
  name: string
  origin: Point
  scale: number
  unit: string
  gridSize: number
  gridVisible: boolean
  gridColor: string
  gridOpacity: number
  bounds: { width: number; depth: number }
  map: SpaceMap | null
  layers: Layer[]
  columns: Column[]
  devices: DevicePOI[]
  bindings: Binding[]
  publishedAt: string | null
}

export type Tool = 'select' | 'marquee' | 'pan' | 'bind' | ShapeType
export type Selection = { kind: 'column' | 'device'; id: string }
