import assert from 'node:assert/strict'
import { test } from 'node:test'
import { build } from 'esbuild'

const result = await build({ entryPoints: ['src/views/desktop/operational-space.ts'], bundle: true,
  platform: 'node', format: 'esm', write: false })
const { readOperationalSpace } = await import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`)
const snapshot = { version: 1, id: 'space-1', name: '一号楼', origin: { x: 0, y: 0 }, scale: 1, unit: 'm',
  gridSize: 1, bounds: { width: 100, depth: 80 }, map: null, layers: [], columns: [], devices: [], bindings: [] }

test('operational view uses the latest saved draft to match the editor', async () => {
  let publishedCalls = 0
  const result = await readOperationalSpace('building-1', {
    async getPublished() { publishedCalls++; return { snapshot: { ...snapshot, name: '已发布' } } },
    async getDraft(id) { assert.equal(id, 'building-1'); return { snapshot: { ...snapshot, name: '已保存草稿' } } }
  })
  assert.equal(result.source, 'draft')
  assert.equal(result.space.name, '已保存草稿')
  assert.equal(publishedCalls, 0)
})

test('operational view falls back to published when the draft is unavailable', async () => {
  const api = { async getDraft() { return null }, async getPublished() { return { snapshot } } }
  assert.equal((await readOperationalSpace('building-1', api)).source, 'published')
  api.getDraft = async () => { throw new Error('draft unavailable') }
  assert.equal((await readOperationalSpace('building-1', api)).source, 'published')
  api.getDraft = async () => null
  api.getPublished = async () => null
  assert.deepEqual(await readOperationalSpace('building-1', api), { space: null, source: 'empty' })
})
