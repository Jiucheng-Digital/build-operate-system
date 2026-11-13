import homeIcon from '../assets/icons/home.svg'
import registryIcon from '../assets/icons/registry.svg'
import resourceIcon from '../assets/icons/resource.svg'
import dataCenterIcon from '../assets/icons/data-center.svg'
import deviceIcon from '../assets/icons/device.svg'
import eventHubIcon from '../assets/icons/event-hub.svg'
import spaceObjectIcon from '../assets/icons/space-object.svg'
import driverIcon from '../assets/icons/driver.svg'

/**
 * 桌面侧栏应用配置
 * key: 应用唯一标识；name: 显示名称；icon: 图标资源；path: 预留跳转路径（当前仅视觉）
 */
export const dockApps = [
  { key: 'home', name: '首页', icon: homeIcon, path: '' },
  { key: 'registry', name: '注册表', icon: registryIcon, path: '' },
  { key: 'resource', name: '资源管理器', icon: resourceIcon, path: '' },
  { key: 'data-center', name: '数据中心', icon: dataCenterIcon, path: '' },
  { key: 'device', name: '设备中心', icon: deviceIcon, path: '' },
  { key: 'event-hub', name: '事件中枢', icon: eventHubIcon, path: '' },
  { key: 'space-object', name: '空间对象管理', icon: spaceObjectIcon, path: '' },
  { key: 'driver', name: '驱动管理', icon: driverIcon, path: '' },
  { key: 'system', name: '系统管理', path: '/system' }
]

export default dockApps
