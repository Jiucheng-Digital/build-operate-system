const DRIVER_PROTOCOLS = new Map([
  ['梯控驱动', 'Modbus TCP'],
  ['立方门禁驱动', 'MQTT'],
  ['监控驱动', 'HTTP'],
  ['室内环境驱动', '蓝牙'],
  ['照明驱动', '蓝牙'],
  ['能耗驱动', 'Modbus RTU']
])

export function definedProtocolForDriver(driver = {}) {
  return DRIVER_PROTOCOLS.get(driver?.name?.trim())
}

const DEFAULT_WEIGHTS = [
  ['HTTP', 78], ['MQTT', 10], ['Modbus TCP', 7], ['Modbus RTU', 3], ['蓝牙', 2]
]

function stableFraction(value) {
  let hash = 2166136261
  for (let index = 0; index < value.length; index++) {
    hash = Math.imul(hash ^ value.charCodeAt(index), 16777619)
  }
  hash ^= hash >>> 16
  hash = Math.imul(hash, 0x7feb352d)
  hash ^= hash >>> 15
  hash = Math.imul(hash, 0x846ca68b)
  hash ^= hash >>> 16
  return (hash >>> 0) / 4294967296
}

function protocolWeights(driver) {
  const text = [driver.name, driver.classifyName, driver.classifyCode, driver.remark]
    .filter(Boolean).join(' ').toLowerCase()
  if (/蓝牙|bluetooth|\bble\b/i.test(text)) return [['蓝牙', 80], ['HTTP', 20]]
  if (/modbus[\s_-]*rtu|rs[\s-]?485|串口|serial/i.test(text)) return [['Modbus RTU', 75], ['HTTP', 25]]
  if (/modbus[\s_-]*tcp|modbus|\bplc\b|工控|变频|电表|仪表|采集器/i.test(text)) {
    return [['Modbus TCP', 65], ['HTTP', 30], ['Modbus RTU', 5]]
  }
  if (/mqtt|消息|环境监测|温湿度|传感|物联网|\biot\b/i.test(text)) return [['MQTT', 60], ['HTTP', 40]]
  return DEFAULT_WEIGHTS
}

// 仅用于注册表展示；优先使用驱动协议定义，其余驱动保留稳定模拟值，不参与后端配置保存。
export function protocolForDriver(driver = {}) {
  const protocol = definedProtocolForDriver(driver)
  if (protocol) return protocol
  const identity = String(driver.id ?? driver.driveId ?? driver.name ?? 'unknown')
  let pick = stableFraction(identity) * 100
  for (const [protocol, weight] of protocolWeights(driver)) {
    pick -= weight
    if (pick < 0) return protocol
  }
  return 'HTTP'
}

export function protocolRowForDriver(driver) {
  return { key: 'frontend.protocol', label: '协议', value: protocolForDriver(driver) }
}
