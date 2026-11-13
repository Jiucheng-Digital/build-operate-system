import assert from 'node:assert/strict'
import { test } from 'node:test'
import { protocolForDriver, protocolRowForDriver } from '../src/views/desktop/apps/Registry/protocol.js'

test('frontend protocols are stable, valid, and mostly HTTP for general drivers', () => {
  const allowed = new Set(['MQTT', 'HTTP', 'Modbus TCP', 'Modbus RTU', '蓝牙'])
  const general = Array.from({ length: 500 }, (_, index) => ({ id: `driver-${index}`, name: '通用设备驱动' }))
  const values = general.map(protocolForDriver)
  assert.ok(values.every(value => allowed.has(value)))
  assert.ok(values.filter(value => value === 'HTTP').length > 350)
  assert.deepEqual(general.map(protocolForDriver), values)
  assert.deepEqual(protocolRowForDriver(general[0]), { key: 'frontend.protocol', label: '协议', value: values[0] })
})

test('driver category steers the simulated protocol without changing backend config', () => {
  const bluetooth = Array.from({ length: 100 }, (_, index) => protocolForDriver({ id: `ble-${index}`, name: '蓝牙设备' }))
  const serial = Array.from({ length: 100 }, (_, index) => protocolForDriver({ id: `rtu-${index}`, name: 'RS485 电表' }))
  const network = Array.from({ length: 100 }, (_, index) => protocolForDriver({ id: `tcp-${index}`, name: 'PLC 工控网关' }))
  assert.ok(bluetooth.filter(value => value === '蓝牙').length > 65)
  assert.ok(serial.filter(value => value === 'Modbus RTU').length > 60)
  assert.ok(network.filter(value => value === 'Modbus TCP').length > 50)
})
