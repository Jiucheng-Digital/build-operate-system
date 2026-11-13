const OFFLINE_TRIGGER = { key: 'disconnect', label: '设备离线时长', unit: '分钟', min: 1, precision: 0, condition: '大于' }

const SOURCE_TRIGGERS = [
  {
    matches: ['室内环境', 'indoor_environment'],
    options: [
      { key: 'temperature', label: '室内温度', unit: '°C', precision: 1 },
      { key: 'humidity', label: '室内湿度', unit: '%', min: 0, precision: 1 },
      { key: 'co2', label: '二氧化碳浓度', unit: 'ppm', min: 0, precision: 0 }
    ]
  },
  {
    matches: ['消防水位', 'fire_water_level'],
    options: [{ key: 'waterLevel', label: '消防水位', unit: 'm', precision: 2, condition: '小于' }]
  },
  {
    matches: ['消防水压', 'fire_water_pressure'],
    options: [{ key: 'waterPressure', label: '消防水压', unit: 'MPa', precision: 2, condition: '小于' }]
  },
  {
    matches: ['消防电', 'fire_electric'],
    options: [
      { key: 'current', label: '电流', unit: 'A', precision: 2 },
      { key: 'voltage', label: '电压', unit: 'V', precision: 2 },
      { key: 'leakageCurrent', label: '漏电电流', unit: 'mA', precision: 2 }
    ]
  },
  {
    matches: ['能耗', 'energy'],
    options: [
      { key: 'power', label: '实时功率', unit: 'kW', precision: 2 },
      { key: 'energyConsumption', label: '累计能耗', unit: 'kWh', precision: 2 }
    ]
  },
  {
    matches: ['气象站', 'weather'],
    options: [
      { key: 'windSpeed', label: '风速', unit: 'm/s', precision: 1 },
      { key: 'rainfall', label: '降雨量', unit: 'mm', precision: 1 },
      { key: 'outdoorTemperature', label: '室外温度', unit: '°C', precision: 1 }
    ]
  },
  {
    matches: ['照明', 'lighting'],
    options: [
      { key: 'illumination', label: '照度', unit: 'lx', precision: 1, condition: '小于' },
      { key: 'lightingPower', label: '照明功率', unit: 'W', precision: 2 }
    ]
  },
  {
    matches: ['梯控', '电梯', 'elevator'],
    options: [
      { key: 'elevatorLoad', label: '电梯载荷', unit: '%', precision: 1 },
      { key: 'doorOpenDuration', label: '开门持续时间', unit: '秒', min: 0, precision: 0 },
      { key: 'faultCode', label: '故障码', min: 0, precision: 0, condition: '等于' }
    ]
  }
]

export function triggerOptionsForDriver(driver) {
  if (!driver) return []
  const source = `${driver.name || ''} ${driver.classifyCode || ''}`.toLowerCase()
  const match = SOURCE_TRIGGERS.find(group => group.matches.some(keyword => source.includes(keyword)))
  return [OFFLINE_TRIGGER, ...(match?.options || [])]
}

export function triggerOptionByKey(key) {
  if (key === OFFLINE_TRIGGER.key) return OFFLINE_TRIGGER
  return SOURCE_TRIGGERS.flatMap(group => group.options).find(option => option.key === key)
}
