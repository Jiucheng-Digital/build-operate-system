// 系统管理沿用云平台的权限标识；菜单和页面共用同一份定义。
export const systemMenus = [
  { key: 'user', title: '用户管理', path: '/system/user', permission: 'system:user:list', icon: 'User' },
  { key: 'role', title: '角色管理', path: '/system/role', permission: 'system:role:list', icon: 'UserFilled' },
  { key: 'menu', title: '菜单管理', path: '/system/menu', permission: 'system:menu:list', icon: 'Menu' },
  { key: 'dept', title: '部门管理', path: '/system/dept', permission: 'system:dept:list', icon: 'OfficeBuilding' },
  { key: 'post', title: '岗位管理', path: '/system/post', permission: 'system:post:list', icon: 'Postcard' },
  { key: 'dict', title: '字典管理', path: '/system/dict', permission: 'system:dict:list', icon: 'Collection' },
  { key: 'config', title: '参数设置', path: '/system/config', permission: 'system:config:list', icon: 'EditPen' },
  { key: 'showroom', title: '展厅管理', path: '/system/showroom', permission: 'system:showroom:list', icon: 'Grid' },
  { key: 'notice', title: '通知公告', path: '/system/notice', permission: 'system:notice:list', icon: 'ChatDotSquare' },
  { key: 'log', title: '日志管理', icon: 'Document', children: [
    { key: 'operlog', title: '操作日志', path: '/system/log/operlog', permission: 'monitor:operlog:list', icon: 'DocumentChecked' },
    { key: 'logininfor', title: '登录日志', path: '/system/log/logininfor', permission: 'monitor:logininfor:list', icon: 'Tickets' }
  ] }
]

export function visibleSystemMenus(hasPermission) {
  return systemMenus.flatMap(item => {
    if (!item.children) return hasPermission(item.permission) ? [item] : []
    const children = item.children.filter(child => hasPermission(child.permission))
    return children.length ? [{ ...item, children }] : []
  })
}

// 这些页面由桌面路由统一承载，避免后端的旧 Layout 路由覆盖桌面布局。
const migratedViews = new Set([
  ...systemMenus.flatMap(item => item.children || [item]).map(item =>
    item.key === 'operlog' || item.key === 'logininfor' ? `monitor/${item.key}/index` : `system/${item.key}/index`),
  'system/user/authRole', 'system/role/authUser', 'system/dict/data'
])

export function withoutMigratedSystemRoutes(routes) {
  return routes.flatMap(route => {
    if (migratedViews.has(route.component)) return []
    if (route.children?.length) {
      const children = withoutMigratedSystemRoutes(route.children)
      // 非本次迁移的菜单仍按原路径提供访问。
      return children.length ? [{ ...route, children }] : []
    }
    return [route]
  })
}
