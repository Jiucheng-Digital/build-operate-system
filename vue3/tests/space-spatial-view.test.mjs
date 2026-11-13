import assert from 'node:assert/strict'
import { test } from 'node:test'
import { build } from 'esbuild'

const result = await build({ entryPoints: ['src/views/desktop/spatial-view.ts'], bundle: true,
  platform: 'node', format: 'esm', write: false })
const view = await import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`)

function space() {
  return {
    id: 'space', layers: [
      { id: 'floor-1', name: '一层', parentId: null, order: 0, visible: true, locked: false },
      { id: 'zone-1', name: '东区', parentId: 'floor-1', order: 0, visible: true, locked: false },
      { id: 'floor-2', name: '二层', parentId: null, order: 1, visible: true, locked: false }
    ], columns: [
      { id: 'column-1', layerId: 'zone-1', type: 'box', name: '东区设备', order: 0, visible: true,
        x: 10, y: 5, width: 4, depth: 2, rotation: 90, opacity: 0.7, fillColor: '#123456', strokeColor: '#654321', strokeWidth: 1, points: [] },
      { id: 'column-2', layerId: 'floor-2', type: 'box', name: '二层设备', order: 0, visible: true,
        x: 0, y: 0, width: 4, depth: 2, rotation: 0, opacity: 0.7, fillColor: '#123456', strokeColor: '#654321', strokeWidth: 1, points: [] }
    ], devices: [{ id: 'poi-1', deviceId: '42', name: '东区摄像头', status: 'online', x: 10, y: 6, z: 0 }],
    bindings: [{ id: 'binding-1', deviceId: '42', targetType: 'column', targetId: 'column-1', localX: 1, localY: 0, z: 0, label: '东区摄像头', showLine: true }]
  }
}

test('operational tree matches editor sibling order and keeps hidden layers muted', () => {
  const snapshot = space()
  assert.deepEqual(view.layerTreeRows(snapshot).map(row => [row.layer.name, row.depth]),
    [['二层', 0], ['一层', 0], ['东区', 1]])
  assert.deepEqual(view.layerTreeRows(snapshot, new Set()).map(row => row.layer.id), ['floor-2', 'floor-1'])
  assert.deepEqual(view.layerTreeRows(snapshot, new Set(['floor-1'])).map(row => row.layer.id), ['floor-2', 'floor-1', 'zone-1'])
  assert.equal(view.layerTreeRows(snapshot)[1].hasChildren, true)
  assert.deepEqual(view.visibleSpatialColumns(snapshot, 'floor-1').map(column => column.id), ['column-1'])
  assert.deepEqual(view.placedDevices(snapshot, 'floor-1').map(item => item.point), [{ x: 10, y: 6 }])
  assert.equal(view.placedDevices(snapshot, 'floor-2').length, 0)
  snapshot.layers[0].visible = false
  assert.deepEqual(view.layerTreeRows(snapshot).map(row => row.visible), [true, false, false])
  assert.equal(view.visibleSpatialColumns(snapshot, 'floor-1').length, 0)
})

test('events locate only by a matching bound device ID', () => {
  const snapshot = space()
  const events = [{ eventId: 'matched', deviceId: 42 }, { eventId: 'unmatched', deviceId: 99 }]
  assert.deepEqual(view.locateEvents(snapshot, events, 'floor-1').map(item => [item.event.eventId, item.point]),
    [['matched', { x: 10, y: 6 }]])
  assert.deepEqual(view.locateEvents(snapshot, events, 'floor-2'), [])
  assert.equal(view.deviceIdForEvent({ deviceId: 42 }), '42')
  assert.equal(view.deviceIdForEvent({ extend: '{"deviceId":"42"}' }), '42')
  assert.equal(view.deviceIdForEvent({ eventId: 'event-1', extend: '{broken' }), null)
  assert.equal(view.columnSvgPath(space().columns[0]), 'M-2,1 L2,1 L2,-1 L-2,-1 Z')
})

test('POI names stay visible in two lines and flip inside the map near an edge', () => {
  const name = '门诊大厅空气质量传感器'
  const lines = view.poiLabelLines(name)
  assert.equal(lines.join(''), name)
  assert.ok(lines.length <= 2)
  const long = view.poiLabelLines('门诊大厅空气质量传感器设备点位补充说明')
  assert.equal(long.length, 2)
  assert.ok(long[1].endsWith('…'))
  const right = view.poiLabelLayout(name, { x: 48, y: 0 }, { width: 100, depth: 80 }, 0.6)
  assert.ok(right.x < 0)
  const left = view.poiLabelLayout(name, { x: -48, y: 0 }, { width: 100, depth: 80 }, 0.6)
  assert.ok(left.x > 0)
  assert.ok(right.y >= -40)
})
