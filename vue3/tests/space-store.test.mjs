import assert from 'node:assert/strict'
import { test } from 'node:test'
import { build } from 'esbuild'

const result = await build({
  entryPoints: ['src/views/desktop/apps/SpaceEditor/store.ts'],
  bundle: true,
  platform: 'node',
  format: 'esm',
  write: false
})
const { useSpaceStore } = await import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`)

test('command history supports more than 50 undo and redo steps and reloads saved JSON', async () => {
  let saved = null
  const repository = {
    async read() { return saved },
    async write(space) { saved = structuredClone(space) }
  }
  const store = useSpaceStore(repository)
  await store.load()
  const originalName = store.space.value.name
  for (let i = 1; i <= 60; i++) store.commit('修改名称', draft => { draft.name = `空间 ${i}` })
  assert.equal(store.history.past.length, 60)
  for (let i = 0; i < 60; i++) store.undo()
  assert.equal(store.space.value.name, originalName)
  for (let i = 0; i < 60; i++) store.redo()
  assert.equal(store.space.value.name, '空间 60')
  assert.deepEqual(await store.save(), [])
  const reopened = useSpaceStore(repository)
  await reopened.load()
  assert.equal(reopened.space.value.name, '空间 60')
  assert.equal(reopened.saveState.value, 'saved')
})

test('failed server load blocks edits and save until reload succeeds', async () => {
  let fail = true
  const repository = {
    async read() { if (fail) throw new Error('network unavailable'); return null },
    async write() { throw new Error('should not save') }
  }
  const store = useSpaceStore(repository)
  await assert.rejects(store.load(), /network unavailable/)
  assert.equal(store.loadError.value, true)
  assert.equal(store.commit('修改名称', draft => { draft.name = '错误的覆盖' }), false)
  await assert.rejects(store.save(), /空间加载失败/)
  fail = false
  await store.load()
  assert.equal(store.loadError.value, false)
  assert.equal(store.commit('修改名称', draft => { draft.name = '可编辑' }), true)
})

test('legacy default layer becomes a clinic hierarchy without losing its objects', async () => {
  const legacy = {
    version: 1, id: '40aca371-436d-4ab1-aaf4-f52e2ffaf196', name: '旧空间',
    origin: { x: 0, y: 0 }, scale: 1, unit: 'm', gridSize: 1,
    gridVisible: true, gridColor: '#b8cce2', gridOpacity: 0.65,
    bounds: { width: 100, depth: 80 }, map: null,
    layers: [{ id: 'layer-1', name: '默认图层', order: 0, parentId: null, visible: true, locked: false }],
    columns: [{ id: 'column-1', layerId: 'layer-1', type: 'box', name: '原有体块', order: 0,
      x: 0, y: 0, width: 5, depth: 5, height: 3, rotation: 0, opacity: 0.7,
      fillColor: '#4d93f7', strokeColor: '#1768d8', strokeWidth: 2, showLabel: false,
      locked: false, visible: true, points: [], notchWidth: 2, notchDepth: 2 }],
    devices: [], bindings: [], publishedAt: null
  }
  let saved = null
  const store = useSpaceStore({ async read() { return structuredClone(saved || legacy) }, async write(space) { saved = structuredClone(space) } })
  await store.load()
  assert.equal(store.space.value.layers[0].name, '门诊')
  assert.deepEqual(store.space.value.layers.slice(1).map(layer => layer.name), ['1F', '2F', '3F', '4F', '5F', '6F', '7F'])
  assert.equal(store.space.value.columns[0].layerId, 'layer-1')
  assert.equal(store.activeLayerId.value, store.space.value.layers[1].id)
  assert.equal(store.saveState.value, 'dirty')
  store.undo()
  assert.equal(store.space.value.layers.length, 1)
  store.redo()
  assert.deepEqual(await store.save(), [])
  assert.equal(saved.layers.length, 8)
  const reopened = useSpaceStore({ async read() { return structuredClone(saved) }, async write(space) { saved = structuredClone(space) } })
  await reopened.load()
  assert.equal(reopened.saveState.value, 'saved')
  assert.equal(reopened.space.value.layers.length, 8)
  const floorId = reopened.space.value.layers.find(layer => layer.name === '1F').id
  reopened.commit('删除图层', draft => {
    draft.layers = draft.layers.filter(layer => layer.id !== floorId)
  })
  assert.deepEqual(await reopened.save(), [])
  const afterDeletion = useSpaceStore({ async read() { return structuredClone(saved) }, async write() {} })
  await afterDeletion.load()
  assert.equal(afterDeletion.space.value.layers.some(layer => layer.name === '1F'), false)
  assert.equal(afterDeletion.saveState.value, 'saved')
})

test('first save repairs a legacy space ID and preserves binding and undo history', async () => {
  let saved = null
  const store = useSpaceStore({
    async read() { return null },
    async write(space) { saved = structuredClone(space) }
  })
  await store.load()
  store.commit('修改名称', draft => { draft.name = '测试空间' })
  store.space.value.id = 'legacy-space-id'
  store.commit('绑定设备', draft => {
    draft.devices.push({ id: 'point-1', deviceId: 'sensor-1', name: '传感器', type: 'sensor', status: 'online', x: 1, y: 2, z: 0 })
    draft.bindings.push({ id: 'binding-1', deviceId: 'sensor-1', targetType: 'space', targetId: draft.id,
      localX: 1, localY: 2, z: 0, label: '传感器', showLine: false })
  })
  assert.deepEqual(await store.save(), [])
  assert.match(saved.id, /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i)
  assert.equal(saved.bindings[0].targetId, saved.id)
  store.undo()
  assert.equal(store.space.value.id, saved.id)
  store.undo()
  assert.equal(store.space.value.id, saved.id)
  store.redo()
  store.redo()
  assert.equal(store.space.value.bindings[0].targetId, saved.id)
})

test('import keeps the current space ID and updates space bindings', async () => {
  const store = useSpaceStore({ async read() { return null }, async write() {} })
  await store.load()
  const currentId = store.space.value.id
  const imported = JSON.parse(JSON.stringify(store.space.value))
  imported.id = 'another-space'
  imported.publishedAt = '2026-10-28T10:05:00+08:00'
  imported.devices.push({ id: 'point-1', deviceId: 'sensor-1', name: '传感器', type: 'sensor', status: 'online', x: 1, y: 2, z: 0 })
  imported.bindings.push({ id: 'binding-1', deviceId: 'sensor-1', targetType: 'space', targetId: imported.id,
    localX: 1, localY: 2, z: 0, label: '传感器', showLine: false })
  store.replace(JSON.stringify(imported))
  assert.equal(store.space.value.id, currentId)
  assert.equal(store.space.value.bindings[0].targetId, currentId)
  assert.equal(store.space.value.publishedAt, null)
})
