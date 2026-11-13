import assert from 'node:assert/strict'
import { test } from 'node:test'
import { build } from 'esbuild'

const result = await build({
  entryPoints: ['src/views/desktop/apps/SpaceEditor/remote-repository.ts'],
  bundle: true,
  platform: 'node',
  format: 'esm',
  write: false
})
const { createRemoteSpaceConnection } = await import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`)

function sampleSpace() {
  return {
    version: 1, id: 'space-1', name: '一号楼', origin: { x: 0, y: 0 }, scale: 1, unit: 'm',
    gridSize: 1, gridVisible: true, gridColor: '#b8cce2', gridOpacity: 0.65,
    bounds: { width: 100, depth: 80 }, map: null,
    layers: [{ id: 'layer-1', name: '默认图层', order: 0, visible: true, locked: false }],
    columns: [], devices: [], bindings: [], publishedAt: null
  }
}

test('remote repository creates, reloads and publishes the same complete snapshot', async () => {
  let document = null
  const seen = []
  const api = {
    async getDraft(projectId) {
      assert.equal(projectId, 'building-1')
      return document && { ...structuredClone(document), revision: String(document.revision) }
    },
    async saveDraft(projectId, payload) {
      seen.push(payload.baseRevision)
      assert.equal(projectId, 'building-1')
      assert.equal(payload.baseRevision, document?.revision ?? 0)
      document = { spaceId: payload.snapshot.id, revision: payload.baseRevision + 1, snapshot: structuredClone(payload.snapshot) }
      return { spaceId: document.spaceId, revision: String(document.revision) }
    },
    async publishDraft(projectId, payload) {
      assert.equal(projectId, 'building-1')
      assert.equal(payload.baseRevision, document.revision)
      document.revision++
      document.snapshot.publishedAt = '2026-10-28T10:05:00+08:00'
      return { revision: String(document.revision), publishedRevision: String(document.revision), publishedAt: document.snapshot.publishedAt }
    }
  }

  const first = createRemoteSpaceConnection('building-1', api)
  assert.equal(await first.repository.read(), null)
  const space = sampleSpace()
  space.map = { name: 'floor.webp', dataUrl: 'data:image/webp;base64,YQ==', x: 0, y: 0,
    width: 40, depth: 30, opacity: 0.8, thickness: 1.2 }
  space.layers[0].parentId = null
  space.layers.push({ id: 'layer-child', name: '二层', parentId: space.layers[0].id, order: 0, visible: true, locked: false })
  space.columns.push({ id: 'column-1', type: 'box', name: '设备区', layerId: 'layer-child', order: 0,
    x: 10, y: 8, width: 8, depth: 6, height: 3, rotation: 0, opacity: .72,
    fillColor: '#4d93f7', strokeColor: '#1768d8', strokeWidth: 2, showLabel: false,
    locked: false, visible: true, points: [], notchWidth: 3, notchDepth: 3 })
  space.devices.push({ id: 'poi-1', deviceId: 'camera-1', name: '东门摄像机', type: 'camera', status: 'online', x: 11, y: 8, z: 2 })
  space.bindings.push({ id: 'binding-1', deviceId: 'camera-1', targetType: 'column', targetId: 'column-1', localX: 1, localY: 0, z: 2, label: '东门摄像机', showLine: true })
  await first.repository.write(space)
  assert.deepEqual(seen, [0])

  const reopened = createRemoteSpaceConnection('building-1', api)
  assert.deepEqual(await reopened.repository.read(), space)
  assert.equal((await reopened.repository.read()).map.dataUrl, space.map.dataUrl)
  assert.equal((await reopened.repository.read()).layers[1].parentId, space.layers[0].id)
  const reopenedSpace = structuredClone(space)
  reopenedSpace.name = '更新后的一号楼'
  await reopened.repository.write(reopenedSpace)
  assert.deepEqual(seen, [0, 1])
  await reopened.publish(reopenedSpace)
  assert.equal((await reopened.repository.read()).publishedAt, '2026-10-28T10:05:00+08:00')
})

test('remote repository rejects stale revisions and keeps its local revision', async () => {
  const space = sampleSpace()
  let revision = 4
  const seen = []
  const api = {
    async getDraft() { return { spaceId: space.id, revision, snapshot: structuredClone(space) } },
    async saveDraft(_projectId, payload) {
      seen.push(payload.baseRevision)
      if (payload.baseRevision !== revision) throw Object.assign(new Error('空间已被其他人修改'), { code: 409 })
      revision++
      return { spaceId: space.id, revision }
    },
    async publishDraft() { throw new Error('not used') }
  }
  const remote = createRemoteSpaceConnection('building-1', api)
  await remote.repository.read()
  revision = 5
  await assert.rejects(remote.repository.write(space), /空间已被其他人修改/)
  await assert.rejects(remote.repository.write(space), /空间已被其他人修改/)
  assert.deepEqual(seen, [4, 4])
})

test('publish accepts the documented response without a new draft revision', async () => {
  const space = sampleSpace()
  const seen = []
  const api = {
    async getDraft() { return { spaceId: space.id, revision: 3, snapshot: structuredClone(space) } },
    async saveDraft(_projectId, payload) {
      seen.push(payload.baseRevision)
      return { spaceId: space.id, revision: payload.baseRevision + 1 }
    },
    async publishDraft(_projectId, payload) {
      assert.equal(payload.baseRevision, 3)
      return { publishedRevision: 3, publishedAt: '2026-10-28T10:05:00+08:00' }
    }
  }
  const remote = createRemoteSpaceConnection('building-1', api)
  await remote.repository.read()
  await remote.publish(space)
  await remote.repository.write(space)
  assert.deepEqual(seen, [3])
})

test('remote repository rejects revision strings that exceed JavaScript safe integers', async () => {
  const space = sampleSpace()
  const remote = createRemoteSpaceConnection('building-1', {
    async getDraft() { return { spaceId: space.id, revision: '9007199254740992', snapshot: space } },
    async saveDraft() { throw new Error('should not save') },
    async publishDraft() { throw new Error('should not publish') }
  })
  await assert.rejects(remote.repository.read(), /修订号/)
})
