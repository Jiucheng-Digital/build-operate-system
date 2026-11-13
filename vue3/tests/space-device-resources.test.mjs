import assert from 'node:assert/strict'
import { test } from 'node:test'
import { build } from 'esbuild'

const result = await build({
  entryPoints: ['src/views/desktop/apps/SpaceEditor/device-resources.ts'],
  bundle: true,
  platform: 'node',
  format: 'esm',
  write: false
})
const resources = await import(`data:text/javascript;base64,${Buffer.from(result.outputFiles[0].text).toString('base64')}`)

test('device classifications keep the same parent and leaf structure as the device center', () => {
  const tree = resources.buildDeviceCategories([
    { code: 'building', name: '楼宇设备', parentCode: '0' },
    { code: 'monitor', name: '监控', parentCode: 'building' },
    { code: 'door', name: '门禁', parentCode: 'building' },
    { code: 'lighting', name: '照明', parentCode: '0' }
  ])
  assert.deepEqual(tree.map(node => node.name), ['楼宇设备', '照明'])
  assert.deepEqual(tree[0].children.map(node => node.code), ['monitor', 'door'])
})

test('pages can append more than the old 200-device cap without losing serial numbers', () => {
  let all = []
  for (let page = 0; page < 5; page++) {
    const rows = Array.from({ length: 50 }, (_, index) => ({ id: page * 50 + index + 1, name: `设备 ${page * 50 + index + 1}`, serialNumber: `SN-${page * 50 + index + 1}` }))
    all = resources.mergeDeviceRows(all, resources.normalizeDeviceRows(rows, 'monitor'))
  }
  assert.equal(all.length, 250)
  assert.equal(all[249].serialNumber, 'SN-250')
  assert.equal(all[249].type, 'monitor')
  assert.equal(resources.mergeDeviceRows(all, [all[249]]).length, 250)
})
