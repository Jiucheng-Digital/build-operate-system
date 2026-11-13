import assert from 'node:assert/strict'
import { test } from 'node:test'
import { build } from 'esbuild'

const result = await build({
  entryPoints: ['src/views/desktop/apps/SpaceEditor/model.ts'],
  bundle: true,
  platform: 'node',
  format: 'esm',
  write: false
})
const model = await import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`)

test('UUID generation stays valid when randomUUID is unavailable', () => {
  const original = Object.getOwnPropertyDescriptor(globalThis, 'crypto')
  try {
    Object.defineProperty(globalThis, 'crypto', {
      configurable: true,
      value: { getRandomValues(bytes) { bytes.fill(0); return bytes } }
    })
    assert.equal(model.uid(), '00000000-0000-4000-8000-000000000000')
    Object.defineProperty(globalThis, 'crypto', { configurable: true, value: undefined })
    assert.equal(model.isUuid(model.uid()), true)
  } finally {
    if (original) Object.defineProperty(globalThis, 'crypto', original)
    else delete globalThis.crypto
  }
})

test('world and local coordinates round-trip with origin and scale', () => {
  const space = model.emptySpace()
  space.origin = { x: 15, y: -10 }
  space.scale = 2.5
  const local = { x: -3.2, y: 8.4 }
  assert.deepEqual(model.worldToLocal(space, model.localToWorld(space, local)), local)
})

test('new spaces start with a clinic parent and floors 1F through 7F', () => {
  const space = model.emptySpace()
  const [clinic, ...floors] = space.layers
  assert.equal(clinic.name, '门诊')
  assert.equal(clinic.parentId, null)
  assert.deepEqual(floors.map(layer => layer.name), ['1F', '2F', '3F', '4F', '5F', '6F', '7F'])
  assert.ok(floors.every(layer => layer.parentId === clinic.id))
  assert.equal(model.validateSpace(space).length, 0)
})

test('clinic floor upgrade preserves existing custom layers and avoids duplicate floors', () => {
  const space = model.emptySpace()
  const clinic = space.layers[0]
  clinic.name = '默认图层'
  const firstFloor = space.layers[1]
  space.layers = [clinic, firstFloor, { id: 'custom', name: '候诊区', parentId: clinic.id, order: 1, visible: true, locked: false }]
  assert.equal(model.upgradeDefaultClinicLayers(space), true)
  assert.deepEqual(model.orderedLayers(space).slice(1).map(layer => layer.name),
    ['1F', '2F', '3F', '4F', '5F', '6F', '7F', '候诊区'])
  assert.equal(space.layers.find(layer => layer.id === 'custom').parentId, clinic.id)
  assert.equal(model.upgradeDefaultClinicLayers(space), false)
  model.deleteLayerTree(space, firstFloor.id)
  assert.equal(model.upgradeDefaultClinicLayers(space), false)
  assert.equal(space.layers.some(layer => layer.name === '1F'), false)
})

test('nested layers inherit visibility and locks, order by sibling, and reject cycles', () => {
  const space = model.emptySpace()
  const root = space.layers[0]
  space.layers = [root]
  const child = { id: 'child', name: '子层', parentId: root.id, order: 0, visible: true, locked: false }
  const grandchild = { id: 'grandchild', name: '孙层', parentId: child.id, order: 0, visible: true, locked: false }
  const other = { id: 'other', name: '另一根层', parentId: null, order: 1, visible: true, locked: false }
  space.layers.push(child, grandchild, other)
  assert.deepEqual(model.orderedLayers(space).map(layer => layer.id), [root.id, child.id, grandchild.id, other.id])
  assert.equal(model.layerEditable(space, grandchild.id), true)
  root.locked = true
  assert.equal(model.layerEditable(space, grandchild.id), false)
  root.locked = false
  root.visible = false
  assert.equal(model.layerVisible(space, grandchild.id), false)
  assert.equal(model.layerVisible(space, other.id), true)
  root.visible = true
  root.parentId = grandchild.id
  assert.match(model.validateSpace(space).join(';'), /循环/)
})

test('deleting a parent layer removes its descendants and bound points while keeping other layers', () => {
  const space = model.emptySpace()
  const first = space.layers.find(layer => layer.name === '1F')
  const second = space.layers.find(layer => layer.name === '2F')
  space.layers.push({ id: 'room', name: '诊室', parentId: first.id, order: 0, visible: true, locked: false })
  const column = model.newColumn('box', 'room', { x: 2, y: 3 }, 0)
  space.columns.push(column)
  const columnDevice = { id: 'point-column', deviceId: 'device-column', name: '体块设备', type: 'sensor', status: 'online', x: 2, y: 3, z: 0 }
  const layerDevice = { id: 'point-layer', deviceId: 'device-layer', name: '楼层设备', type: 'sensor', status: 'online', x: 1, y: 1, z: 0 }
  const spaceDevice = { id: 'point-space', deviceId: 'device-space', name: '空间设备', type: 'sensor', status: 'online', x: 0, y: 0, z: 0 }
  space.devices.push(columnDevice, layerDevice, spaceDevice)
  space.bindings.push(model.makeBinding(space, columnDevice, 'column', column.id, { x: 2, y: 3 }))
  space.bindings.push(model.makeBinding(space, layerDevice, 'layer', first.id, { x: 1, y: 1 }))
  space.bindings.push(model.makeBinding(space, spaceDevice, 'space', space.id, { x: 0, y: 0 }))
  const preview = model.layerDeletionPreview(space, first.id)
  assert.equal(preview.layerIds.size, 2)
  assert.equal(preview.columnIds.size, 1)
  assert.equal(preview.deviceIds.size, 2)
  assert.equal(model.deleteLayerTree(space, first.id), true)
  assert.equal(model.layerFor(space, second.id)?.name, '2F')
  assert.equal(space.columns.length, 0)
  assert.deepEqual(space.devices.map(device => device.deviceId), ['device-space'])
  assert.equal(space.bindings.length, 1)
  assert.deepEqual(model.validateSpace(space), [])
})

test('column binding follows position and rotation without changing its local offset', () => {
  const space = model.emptySpace()
  const column = model.newColumn('box', space.layers[0].id, { x: 10, y: 10 }, 0)
  space.columns.push(column)
  const device = { id: 'point-1', deviceId: 'device-1', name: '传感器', type: 'sensor', status: 'online', x: 12, y: 10, z: 0 }
  space.devices.push(device)
  const binding = model.makeBinding(space, device, 'column', column.id, { x: 12, y: 10 })
  space.bindings.push(binding)
  assert.deepEqual(model.bindingPosition(space, binding), { x: 12, y: 10 })
  column.x = 20
  column.rotation = 90
  assert.deepEqual(model.bindingPosition(space, binding), { x: 20, y: 12 })
})

test('validation finds duplicate bindings and broken targets', () => {
  const space = model.emptySpace()
  const device = { id: 'point-1', deviceId: 'device-1', name: '传感器', type: 'sensor', status: 'online', x: 0, y: 0, z: 0 }
  space.devices.push(device)
  const binding = model.makeBinding(space, device, 'space', space.id, { x: 0, y: 0 })
  space.bindings.push(binding, { ...binding, id: 'binding-2', targetType: 'column', targetId: 'missing' })
  const problems = model.validateSpace(space).join(';')
  assert.match(problems, /重复绑定/)
  assert.match(problems, /体块不存在/)
})

test('JSON round-trip preserves polygon points and rejects unsupported versions', () => {
  const space = model.emptySpace()
  const polygon = model.newColumn('polygon', space.layers[0].id, { x: 0, y: 0 }, 0)
  polygon.points = [{ x: -1, y: -1 }, { x: 1, y: -1 }, { x: 0, y: 1 }]
  space.columns.push(polygon)
  assert.deepEqual(model.parseSpace(JSON.stringify(space)).columns[0].points, polygon.points)
  assert.throws(() => model.parseSpace(JSON.stringify({ ...space, version: 2 })), /版本/)
})

test('map image and placement survive JSON round-trip and old spaces load without a map', () => {
  const space = model.emptySpace()
  space.gridVisible = false
  space.gridColor = '#123456'
  space.gridOpacity = 0.35
  space.map = { name: 'floor.png', dataUrl: 'data:image/png;base64,YQ==', x: 3, y: -2, width: 40, depth: 25, opacity: 0.75, thickness: 1.2 }
  const restored = model.parseSpace(JSON.stringify(space))
  assert.deepEqual(restored.map, space.map)
  assert.equal(restored.gridVisible, false)
  assert.equal(restored.gridColor, '#123456')
  assert.equal(restored.gridOpacity, 0.35)
  assert.equal(model.parseSpace(JSON.stringify({ ...space, map: undefined })).map, null)
  const legacy = model.parseSpace(JSON.stringify({ ...space, gridVisible: undefined, gridColor: undefined, gridOpacity: undefined, map: { ...space.map, thickness: undefined } }))
  assert.equal(legacy.gridVisible, true)
  assert.equal(legacy.gridColor, '#b8cce2')
  assert.equal(legacy.gridOpacity, 0.65)
  assert.equal(legacy.map.thickness, 1.2)
  assert.throws(() => model.parseSpace(JSON.stringify({ ...space, map: { ...space.map, dataUrl: 'https://example.com/map.png' } })), /地图/)
})
