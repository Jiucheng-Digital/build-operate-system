export interface DeviceCategory {
  code: string
  name: string
  children: DeviceCategory[]
}

export interface DeviceResource {
  id: string
  name: string
  type: string
  status: string
  serialNumber?: string
}

type RecordLike = Record<string, unknown>

export function buildDeviceCategories(input: unknown): DeviceCategory[] {
  if (!Array.isArray(input)) return []
  const nodes = new Map<string, DeviceCategory>()
  const parentCodes = new Map<string, string>()
  for (const value of input) {
    if (!value || typeof value !== 'object') continue
    const row = value as RecordLike
    const code = String(row.code ?? '').trim()
    if (!code || nodes.has(code)) continue
    nodes.set(code, { code, name: String(row.name ?? code), children: [] })
    parentCodes.set(code, String(row.parentCode ?? '0'))
  }
  const roots: DeviceCategory[] = []
  for (const node of nodes.values()) {
    const parentCode = parentCodes.get(node.code)
    const parent = parentCode && parentCode !== node.code ? nodes.get(parentCode) : null
    if (parent) parent.children.push(node)
    else roots.push(node)
  }
  return roots
}

export function normalizeDeviceRows(input: unknown, categoryCode: string | null): DeviceResource[] {
  if (!Array.isArray(input)) return []
  return input.flatMap(value => {
    if (!value || typeof value !== 'object') return []
    const row = value as RecordLike
    const id = String(row.id ?? row.deviceId ?? row.serialNumber ?? '').trim()
    if (!id) return []
    const serialNumber = String(row.serialNumber ?? '').trim()
    return [{
      id,
      name: String(row.name ?? row.deviceName ?? serialNumber ?? '未命名设备') || '未命名设备',
      type: String(row.classify ?? row.classifyCode ?? row.deviceTypeCode ?? categoryCode ?? 'device'),
      status: String(row.status ?? 'unknown'),
      serialNumber: serialNumber || undefined
    }]
  })
}

export function mergeDeviceRows(existing: DeviceResource[], incoming: DeviceResource[]): DeviceResource[] {
  const rows = new Map(existing.map(device => [device.id, device]))
  incoming.forEach(device => rows.set(device.id, device))
  return [...rows.values()]
}
