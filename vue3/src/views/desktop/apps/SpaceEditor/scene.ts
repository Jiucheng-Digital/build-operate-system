import * as THREE from 'three'
import { CSS2DObject, CSS2DRenderer } from 'three/addons/renderers/CSS2DRenderer.js'
import { Line2 } from 'three/addons/lines/Line2.js'
import { LineGeometry } from 'three/addons/lines/LineGeometry.js'
import { LineMaterial } from 'three/addons/lines/LineMaterial.js'
import { bindingPosition, isEditable, layerVisible, localToWorld, orderedLayers, rotate, worldToLocal } from './model'
import type { Column, Point, Selection, Space } from './types'

type Hit = { kind: 'column' | 'device' | 'handle'; id: string; handle?: number }

function outline(column: Column): Point[] {
  const w = column.width / 2
  const d = column.depth / 2
  if (column.type === 'polygon' && column.points.length >= 3) return column.points
  if (column.type === 'lshape') {
    const nw = Math.min(Math.max(column.notchWidth, 0.2), column.width - 0.2)
    const nd = Math.min(Math.max(column.notchDepth, 0.2), column.depth - 0.2)
    return [{ x: -w, y: -d }, { x: w, y: -d }, { x: w, y: d - nd },
      { x: w - nw, y: d - nd }, { x: w - nw, y: d }, { x: -w, y: d }]
  }
  if (column.type === 'cylinder') {
    return Array.from({ length: 48 }, (_, i) => {
      const angle = i * Math.PI * 2 / 48
      return { x: Math.cos(angle) * w, y: Math.sin(angle) * d }
    })
  }
  return [{ x: -w, y: -d }, { x: w, y: -d }, { x: w, y: d }, { x: -w, y: d }]
}

function makeShape(column: Column): THREE.Shape {
  const points = outline(column)
  const shape = new THREE.Shape()
  shape.moveTo(points[0].x, points[0].y)
  for (const point of points.slice(1)) shape.lineTo(point.x, point.y)
  shape.closePath()
  return shape
}

function disposeObject(object: THREE.Object3D) {
  object.traverse(child => {
    const mesh = child as THREE.Mesh
    mesh.geometry?.dispose()
    const materials = mesh.material ? (Array.isArray(mesh.material) ? mesh.material : [mesh.material]) : []
    materials.forEach(material => material.dispose())
    if (child instanceof CSS2DObject) child.element.remove()
  })
}

export class SpaceScene {
  private renderer: THREE.WebGLRenderer
  private labelRenderer: CSS2DRenderer
  private scene = new THREE.Scene()
  private editCamera = new THREE.OrthographicCamera(-30, 30, 20, -20, 0.01, 5000)
  private previewCamera = new THREE.PerspectiveCamera(45, 1, 0.01, 5000)
  private raycaster = new THREE.Raycaster()
  private pointer = new THREE.Vector2()
  private dragPlane = new THREE.Plane(new THREE.Vector3(0, 0, 1), 0)
  private group = new THREE.Group()
  private grid = new THREE.Group()
  private mapGroup = new THREE.Group()
  private mapTexture: THREE.Texture | null = null
  private mapTextureSource = ''
  private resizeObserver: ResizeObserver
  private frame = 0
  private preview = false
  private center = new THREE.Vector3(0, 0, 0)
  private span = 70
  private space: Space | null = null
  private selected: Selection[] = []
  private columnRoots = new Map<string, THREE.Group>()
  private deviceRoots = new Map<string, THREE.Group>()
  private markerRoots = new Map<string, THREE.Group>()
  private bindingLines = new Map<string, THREE.Line>()
  private zoomCallback?: (zoom: number) => void

  private get camera(): THREE.Camera { return this.preview ? this.previewCamera : this.editCamera }

  constructor(private host: HTMLElement, onZoom?: (zoom: number) => void) {
    this.zoomCallback = onZoom
    this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false })
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    this.renderer.setClearColor('#f9fbff')
    this.renderer.outputColorSpace = THREE.SRGBColorSpace
    this.renderer.domElement.className = 'space-canvas__webgl'
    this.host.appendChild(this.renderer.domElement)
    this.labelRenderer = new CSS2DRenderer()
    this.labelRenderer.domElement.className = 'space-canvas__labels'
    this.labelRenderer.domElement.style.pointerEvents = 'none'
    this.host.appendChild(this.labelRenderer.domElement)
    this.scene.add(this.mapGroup, this.grid, this.group)
    this.scene.add(new THREE.AmbientLight('#ffffff', 0.74))
    const sunlight = new THREE.DirectionalLight('#ffffff', 0.9)
    sunlight.position.set(-30, 45, 90)
    this.scene.add(sunlight)
    this.editCamera.up.set(0, 1, 0)
    this.previewCamera.up.set(0, 1, 0)
    this.resizeObserver = new ResizeObserver(() => this.resize())
    this.resizeObserver.observe(host)
    this.resize()
    this.updateCamera()
  }

  private invalidate() {
    if (this.frame) return
    this.frame = requestAnimationFrame(() => {
      this.frame = 0
      this.renderer.render(this.scene, this.camera)
      this.labelRenderer.render(this.scene, this.camera)
    })
  }

  private resize() {
    const width = Math.max(this.host.clientWidth, 1)
    const height = Math.max(this.host.clientHeight, 1)
    const aspect = width / height
    this.editCamera.left = -this.span * aspect / 2
    this.editCamera.right = this.span * aspect / 2
    this.editCamera.top = this.span / 2
    this.editCamera.bottom = -this.span / 2
    this.editCamera.updateProjectionMatrix()
    this.previewCamera.aspect = aspect
    this.previewCamera.updateProjectionMatrix()
    this.renderer.setSize(width, height)
    this.labelRenderer.setSize(width, height)
    this.group.traverse(child => { if (child instanceof Line2) (child.material as LineMaterial).resolution.set(width, height) })
    this.refreshMarkers()
    this.refreshBindingLines()
    this.invalidate()
  }

  private updateCamera() {
    this.editCamera.position.copy(this.center).add(new THREE.Vector3(0, 0, 100))
    this.editCamera.lookAt(this.center)
    this.editCamera.updateMatrixWorld()
    const distance = this.span / (2 * Math.tan(THREE.MathUtils.degToRad(this.previewCamera.fov / 2))) * 1.35
    this.previewCamera.position.copy(this.center).add(new THREE.Vector3(0, -distance * 0.64, distance * 0.77))
    this.previewCamera.lookAt(this.center)
    this.previewCamera.updateMatrixWorld()
    this.refreshMarkers()
    this.refreshBindingLines()
    this.invalidate()
  }

  private worldPerPixelAt(position: THREE.Vector3): number {
    const height = Math.max(this.host.clientHeight, 1)
    if (!this.preview) return this.span / height
    const forward = this.previewCamera.getWorldDirection(new THREE.Vector3())
    const depth = Math.max(0.1, position.clone().sub(this.previewCamera.position).dot(forward))
    return 2 * depth * Math.tan(THREE.MathUtils.degToRad(this.previewCamera.fov / 2)) / height
  }

  private refreshMarkers() {
    for (const [id, marker] of this.markerRoots) {
      const root = this.deviceRoots.get(id)
      if (!root) continue
      marker.scale.setScalar(this.worldPerPixelAt(root.position) * 10)
      marker.quaternion.copy(this.camera.quaternion)
    }
  }

  private refreshBindingLines() {
    if (!this.space) return
    const deviceRootsByExternalId = new Map(this.space.devices.map(device => [device.deviceId, this.deviceRoots.get(device.id)]))
    for (const binding of this.space.bindings) {
      const line = this.bindingLines.get(binding.id)
      const end = deviceRootsByExternalId.get(binding.deviceId)?.position
      if (!line || !end) continue
      const target = binding.targetType === 'column' ? this.columnRoots.get(binding.targetId)?.position : null
      const start = target || new THREE.Vector3(this.space.origin.x, this.space.origin.y, 0)
      const dx = end.x - start.x, dy = end.y - start.y
      const distance = Math.hypot(dx, dy)
      const gap = this.worldPerPixelAt(end) * 14
      line.visible = distance > gap * 1.5
      if (!line.visible) continue
      const attr = line.geometry.getAttribute('position') as THREE.BufferAttribute
      attr.setXYZ(0, start.x, start.y, 0.15)
      attr.setXYZ(1, end.x - dx / distance * gap, end.y - dy / distance * gap, end.z + 0.15)
      attr.needsUpdate = true
      line.geometry.computeBoundingSphere()
      line.computeLineDistances()
    }
  }

  setPreview(preview: boolean) {
    this.preview = preview
    this.updateCamera()
    if (this.space) this.sync(this.space, this.selected)
  }

  get zoom() { return 70 / this.span }

  zoomAt(clientX: number, clientY: number, factor: number) {
    const before = this.clientToLocal(clientX, clientY)
    this.span = THREE.MathUtils.clamp(this.span * factor, 8, 280)
    this.resize()
    this.updateCamera()
    const after = this.clientToLocal(clientX, clientY)
    if (before && after && this.space) {
      this.center.x += (before.x - after.x) * this.space.scale
      this.center.y += (before.y - after.y) * this.space.scale
    }
    this.updateCamera()
    this.zoomCallback?.(this.zoom)
  }

  panPixels(dx: number, dy: number) {
    const worldPerPixel = this.worldPerPixelAt(this.center)
    this.center.x -= dx * worldPerPixel
    this.center.y += dy * worldPerPixel
    this.updateCamera()
  }

  private setPointer(clientX: number, clientY: number) {
    const rect = this.host.getBoundingClientRect()
    this.pointer.set(((clientX - rect.left) / rect.width) * 2 - 1, -((clientY - rect.top) / rect.height) * 2 + 1)
    this.raycaster.setFromCamera(this.pointer, this.camera)
  }

  clientToLocal(clientX: number, clientY: number): Point | null {
    if (!this.space) return null
    this.setPointer(clientX, clientY)
    const hit = new THREE.Vector3()
    return this.raycaster.ray.intersectPlane(this.dragPlane, hit) ? worldToLocal(this.space, { x: hit.x, y: hit.y }) : null
  }

  localToClient(point: Point): Point {
    if (!this.space) return { x: 0, y: 0 }
    const world = localToWorld(this.space, point)
    const projected = new THREE.Vector3(world.x, world.y, 0).project(this.camera)
    const rect = this.host.getBoundingClientRect()
    return { x: rect.left + (projected.x + 1) * rect.width / 2, y: rect.top + (1 - projected.y) * rect.height / 2 }
  }

  pick(clientX: number, clientY: number): Hit | null {
    this.setPointer(clientX, clientY)
    const hits = this.raycaster.intersectObjects(this.group.children, true)
      .sort((a, b) => b.object.renderOrder - a.object.renderOrder || a.distance - b.distance)
    const candidates = hits.map(hit => hit.object.userData as Hit).filter(data => data.kind)
    return candidates.find(data => data.kind === 'handle') || candidates.find(data => data.kind === 'device') || candidates[0] || null
  }

  boundsInClient(column: Column): { left: number; right: number; top: number; bottom: number } {
    const points = outline(column).map(p => {
      const rotated = rotate(p, column.rotation)
      return this.localToClient({ x: column.x + rotated.x, y: column.y + rotated.y })
    })
    return {
      left: Math.min(...points.map(p => p.x)), right: Math.max(...points.map(p => p.x)),
      top: Math.min(...points.map(p => p.y)), bottom: Math.max(...points.map(p => p.y))
    }
  }

  private label(text: string, className: string): CSS2DObject {
    const element = document.createElement('div')
    element.className = className
    element.textContent = text
    return new CSS2DObject(element)
  }

  private rebuildGrid(space: Space) {
    this.grid.children.forEach(disposeObject)
    this.grid.clear()
    const halfX = space.bounds.width * space.scale / 2
    const halfY = space.bounds.depth * space.scale / 2
    if (space.gridVisible && space.gridOpacity > 0) {
      const spacing = Math.max(space.gridSize * space.scale, 0.01)
      const vertices: number[] = []
      const step = spacing * Math.max(1, Math.ceil(Math.max(space.bounds.width, space.bounds.depth) / space.gridSize / 300))
      for (let x = -halfX; x <= halfX + 0.001; x += step) vertices.push(x, -halfY, -0.2, x, halfY, -0.2)
      for (let y = -halfY; y <= halfY + 0.001; y += step) vertices.push(-halfX, y, -0.2, halfX, y, -0.2)
      const geometry = new THREE.BufferGeometry()
      geometry.setAttribute('position', new THREE.Float32BufferAttribute(vertices, 3))
      const lines = new THREE.LineSegments(geometry, new THREE.LineBasicMaterial({ color: space.gridColor, transparent: true, opacity: space.gridOpacity, depthWrite: false }))
      lines.position.set(space.origin.x, space.origin.y, 0)
      this.grid.add(lines)
    }
    const frame = new THREE.LineLoop(new THREE.BufferGeometry().setFromPoints([
      new THREE.Vector3(-halfX, -halfY, -0.1), new THREE.Vector3(halfX, -halfY, -0.1),
      new THREE.Vector3(halfX, halfY, -0.1), new THREE.Vector3(-halfX, halfY, -0.1)
    ]), new THREE.LineBasicMaterial({ color: '#a7bfdd' }))
    frame.position.set(space.origin.x, space.origin.y, 0)
    this.grid.add(frame)
    const axes = new THREE.AxesHelper(2.5 * space.scale)
    axes.position.set(space.origin.x, space.origin.y, 0)
    this.grid.add(axes)
    const originLabel = this.label('原点 0,0', 'space-origin-label')
    originLabel.position.set(space.origin.x, space.origin.y, 0.1)
    this.grid.add(originLabel)
  }

  private rebuildMap(space: Space) {
    this.mapGroup.children.forEach(disposeObject)
    this.mapGroup.clear()
    if (!space.map) {
      this.mapTexture?.dispose()
      this.mapTexture = null
      this.mapTextureSource = ''
      return
    }
    if (this.mapTextureSource !== space.map.dataUrl) {
      this.mapTexture?.dispose()
      this.mapTextureSource = space.map.dataUrl
      this.mapTexture = new THREE.TextureLoader().load(space.map.dataUrl, () => this.invalidate())
      this.mapTexture.colorSpace = THREE.SRGBColorSpace
    }
    const map = space.map
    const center = localToWorld(space, map)
    const width = map.width * space.scale, depth = map.depth * space.scale
    if (this.preview && map.thickness > 0) {
      const thickness = map.thickness * space.scale
      const slab = new THREE.Mesh(new THREE.BoxGeometry(width, depth, thickness), new THREE.MeshLambertMaterial({ color: '#b7cce0' }))
      slab.position.set(center.x, center.y, -0.5 - thickness / 2)
      slab.renderOrder = -12
      this.mapGroup.add(slab)
      const top = new THREE.Mesh(new THREE.PlaneGeometry(width, depth), new THREE.MeshBasicMaterial({ color: '#f8fbff' }))
      top.position.set(center.x, center.y, -0.495)
      top.renderOrder = -11
      this.mapGroup.add(top)
    }
    const material = new THREE.MeshBasicMaterial({ map: this.mapTexture, transparent: true, opacity: map.opacity, depthWrite: false, side: THREE.DoubleSide })
    const mesh = new THREE.Mesh(new THREE.PlaneGeometry(width, depth), material)
    mesh.position.set(center.x, center.y, this.preview && map.thickness > 0 ? -0.49 : -0.5)
    mesh.renderOrder = -10
    this.mapGroup.add(mesh)
  }

  sync(space: Space, selection: Selection[]) {
    this.space = space
    this.selected = selection
    this.group.children.forEach(disposeObject)
    this.group.clear()
    this.columnRoots.clear()
    this.deviceRoots.clear()
    this.markerRoots.clear()
    this.bindingLines.clear()
    this.rebuildMap(space)
    this.rebuildGrid(space)
    const selectedIds = new Set(selection.filter(s => s.kind === 'column').map(s => s.id))
    const selectedDeviceIds = new Set(selection.filter(s => s.kind === 'device').map(s => s.id))
    const layers = orderedLayers(space)
    const layerIndex = new Map(layers.map((layer, index) => [layer.id, index]))
    const columnRank = new Map<string, number>()
    const columnsByLayer = new Map<string, Column[]>()
    for (const column of space.columns) {
      const group = columnsByLayer.get(column.layerId) || []
      group.push(column)
      columnsByLayer.set(column.layerId, group)
    }
    for (const group of columnsByLayer.values()) group.sort((a, b) => a.order - b.order).forEach((column, index) => columnRank.set(column.id, index))
    const layerStride = space.columns.length * 2 + 2
    const poiOrder = 10 + layers.length * layerStride + 10
    const devicesByExternalId = new Map(space.devices.map(device => [device.deviceId, device]))
    const columnsById = new Map(space.columns.map(column => [column.id, column]))
    for (const column of space.columns) {
      if (!column.visible || !layerVisible(space, column.layerId)) continue
      const shape = makeShape(column)
      const geometry = this.preview
        ? new THREE.ExtrudeGeometry(shape, { depth: Math.max(column.height * space.scale, 0.05), bevelEnabled: false, steps: 1 })
        : new THREE.ShapeGeometry(shape)
      if (this.preview) geometry.scale(space.scale, space.scale, 1)
      else geometry.scale(space.scale, space.scale, 1)
      const columnMaterial = this.preview ? new THREE.MeshLambertMaterial({
        color: column.fillColor, transparent: true, opacity: column.opacity,
        depthWrite: false, side: THREE.DoubleSide
      }) : new THREE.MeshBasicMaterial({
        color: column.fillColor, transparent: true, opacity: column.opacity,
        depthWrite: false, side: THREE.DoubleSide
      })
      const mesh = new THREE.Mesh(geometry, columnMaterial)
      mesh.userData = { kind: 'column', id: column.id }
      mesh.renderOrder = 10 + (layerIndex.get(column.layerId) || 0) * layerStride + (columnRank.get(column.id) || 0) * 2
      const outlinePoints = outline(column).map(p => new THREE.Vector3(p.x * space.scale, p.y * space.scale, this.preview ? column.height * space.scale + 0.02 : 0.02))
      const borderGeometry = new LineGeometry()
      borderGeometry.setPositions([...outlinePoints, outlinePoints[0]].flatMap(p => [p.x, p.y, p.z]))
      const borderMaterial = new LineMaterial({ color: selectedIds.has(column.id) ? '#075ee8' : column.strokeColor, linewidth: column.strokeWidth, depthTest: false, transparent: true })
      borderMaterial.resolution.set(Math.max(this.host.clientWidth, 1), Math.max(this.host.clientHeight, 1))
      const border = new Line2(borderGeometry, borderMaterial)
      border.renderOrder = mesh.renderOrder + 1
      const root = new THREE.Group()
      const position = localToWorld(space, column)
      root.position.set(position.x, position.y, 0)
      root.rotation.z = column.rotation * Math.PI / 180
      root.add(mesh, border)
      if (column.showLabel) {
        const label = this.label(column.name, `space-object-label${selectedIds.has(column.id) ? ' is-selected' : ''}`)
        label.position.set(0, (column.depth / 2 + 0.5) * space.scale, this.preview ? column.height * space.scale + 0.1 : 0.1)
        root.add(label)
      }
      this.group.add(root)
      this.columnRoots.set(column.id, root)
    }
    for (const binding of space.bindings) {
      const device = devicesByExternalId.get(binding.deviceId)
      if (!device) continue
      if (binding.targetType === 'layer' && !layerVisible(space, binding.targetId)) continue
      if (binding.targetType === 'column') {
        const target = columnsById.get(binding.targetId)
        if (!target?.visible || !layerVisible(space, target.layerId)) continue
      }
      const point = bindingPosition(space, binding)
      const world = localToWorld(space, point)
      const deviceRoot = new THREE.Group()
      deviceRoot.position.set(world.x, world.y, this.preview ? binding.z * space.scale : 0)
      const markerRoot = new THREE.Group()
      markerRoot.position.z = 0.3
      const halo = new THREE.Mesh(new THREE.CircleGeometry(1, 32), new THREE.MeshBasicMaterial({ color: selectedDeviceIds.has(device.id) ? '#1268f3' : '#174d75', depthTest: false, depthWrite: false }))
      halo.renderOrder = poiOrder
      halo.userData = { kind: 'device', id: device.id }
      const whiteRing = new THREE.Mesh(new THREE.CircleGeometry(0.84, 32), new THREE.MeshBasicMaterial({ color: '#ffffff', depthTest: false, depthWrite: false }))
      whiteRing.position.z = 0.005
      whiteRing.renderOrder = poiOrder + 1
      whiteRing.userData = halo.userData
      const core = new THREE.Mesh(new THREE.CircleGeometry(0.67, 32), new THREE.MeshBasicMaterial({ color: device.status === 'offline' ? '#e45348' : '#078e80', depthTest: false, depthWrite: false }))
      core.position.z = 0.01
      core.renderOrder = poiOrder + 2
      core.userData = halo.userData
      const centerDot = new THREE.Mesh(new THREE.CircleGeometry(0.18, 24), new THREE.MeshBasicMaterial({ color: '#ffffff', depthTest: false, depthWrite: false }))
      centerDot.position.z = 0.02
      centerDot.renderOrder = poiOrder + 3
      centerDot.userData = halo.userData
      markerRoot.add(halo, whiteRing, core, centerDot)
      deviceRoot.add(markerRoot)
      this.markerRoots.set(device.id, markerRoot)
      const label = this.label('', 'space-poi-label')
      const bubble = document.createElement('span')
      bubble.className = 'space-poi-label__bubble'
      bubble.textContent = binding.label || device.name
      label.element.appendChild(bubble)
      label.position.set(0, 0, 0.4)
      deviceRoot.add(label)
      this.group.add(deviceRoot)
      this.deviceRoots.set(device.id, deviceRoot)
      if (binding.showLine) {
          const target = binding.targetType === 'column' ? columnsById.get(binding.targetId) : null
          const start = target ? localToWorld(space, target) : space.origin
          const line = new THREE.Line(new THREE.BufferGeometry().setFromPoints([
            new THREE.Vector3(start.x, start.y, 0.15), new THREE.Vector3(world.x, world.y, this.preview ? binding.z * space.scale + 0.15 : 0.15)
          ]), new THREE.LineDashedMaterial({ color: '#318af1', dashSize: 0.5, gapSize: 0.28, depthTest: false }))
          line.computeLineDistances()
          line.renderOrder = poiOrder - 1
          this.group.add(line)
          this.bindingLines.set(binding.id, line)
      }
    }
    this.refreshMarkers()
    this.refreshBindingLines()
    if (selectedIds.size === 1 && selection.length === 1) {
      const column = space.columns.find(c => selectedIds.has(c.id))
      if (column && isEditable(space, column)) {
        const corners = [{ x: -1, y: -1 }, { x: 1, y: -1 }, { x: 1, y: 1 }, { x: -1, y: 1 }]
        corners.forEach((corner, index) => {
          const local = rotate({ x: corner.x * column.width / 2, y: corner.y * column.depth / 2 }, column.rotation)
          const world = localToWorld(space, { x: column.x + local.x, y: column.y + local.y })
          const handle = new THREE.Mesh(new THREE.PlaneGeometry(0.75 * space.scale, 0.75 * space.scale), new THREE.MeshBasicMaterial({ color: '#ffffff', depthTest: false }))
          handle.position.set(world.x, world.y, 0.6)
          handle.renderOrder = poiOrder + 4
          handle.userData = { kind: 'handle', id: column.id, handle: index }
          this.group.add(handle)
        })
      }
    }
    this.invalidate()
  }

  previewMove(selection: Selection[], delta: Point) {
    if (!this.space) return
    const movingColumns = new Set(selection.filter(s => s.kind === 'column').map(s => s.id))
    const movingDevices = new Set(selection.filter(s => s.kind === 'device').map(s => s.id))
    const devicesByExternalId = new Map(this.space.devices.map(device => [device.deviceId, device]))
    const offsetX = delta.x * this.space.scale, offsetY = delta.y * this.space.scale
    for (const column of this.space.columns) {
      const root = this.columnRoots.get(column.id)
      if (!root) continue
      const base = localToWorld(this.space, column)
      root.position.set(base.x + (movingColumns.has(column.id) ? offsetX : 0), base.y + (movingColumns.has(column.id) ? offsetY : 0), 0)
    }
    for (const binding of this.space.bindings) {
      const device = devicesByExternalId.get(binding.deviceId)
      if (!device) continue
      const followTarget = binding.targetType === 'column' && movingColumns.has(binding.targetId)
      const moved = followTarget || movingDevices.has(device.id)
      const base = localToWorld(this.space, bindingPosition(this.space, binding))
      this.deviceRoots.get(device.id)?.position.set(base.x + (moved ? offsetX : 0), base.y + (moved ? offsetY : 0), this.preview ? binding.z * this.space.scale : 0)
    }
    this.refreshBindingLines()
    this.invalidate()
  }

  dispose() {
    if (this.frame) cancelAnimationFrame(this.frame)
    this.resizeObserver.disconnect()
    this.group.children.forEach(disposeObject)
    this.grid.children.forEach(disposeObject)
    this.mapGroup.children.forEach(disposeObject)
    this.group.clear()
    this.grid.clear()
    this.mapGroup.clear()
    this.mapTexture?.dispose()
    this.renderer.dispose()
    this.renderer.forceContextLoss()
    this.renderer.domElement.remove()
    this.labelRenderer.domElement.remove()
  }
}
