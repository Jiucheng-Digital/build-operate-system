export const demoEnvironment = {
  wind_speed: 1.8,
  pm25: 22,
  pm10: 38,
  temperature: 24.6,
  humidity: 48,
  noise: 42
}

export const demoCpu = {
  status: '正常',
  usageRate: '27.5%',
  kernelVersion: '6.1.0',
  architecture: 'x86_64'
}

export const demoMemory = {
  status: '正常',
  usageRate: '42.8%',
  usedTotalMemory: '6.8 GB / 16 GB',
  cacheMemory: '2.1 GB',
  availableMemory: '9.2 GB'
}

export function hasMonitorValue(input) {
  if (input === undefined || input === null) return false
  const text = String(input).trim()
  return text !== '' && !['-', '--', '—', 'n/a', 'null', 'undefined'].includes(text.toLowerCase())
}

export function monitorValue(input, fallback) {
  return hasMonitorValue(input) ? input : fallback
}

export function withMonitorFallback(data, fallback) {
  const result = { ...data }
  Object.entries(fallback).forEach(([key, value]) => {
    result[key] = monitorValue(result[key], value)
  })
  return result
}
