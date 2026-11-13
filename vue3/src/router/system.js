const page = (path, name, component, title, permission, extra = {}) => ({
  path, name, component,
  meta: { title, systemManagement: true, systemPermission: permission, ...extra.meta },
  ...Object.fromEntries(Object.entries(extra).filter(([key]) => key !== 'meta'))
})

export const systemRoutes = {
  path: '/system',
  component: () => import('@/views/desktop/index.vue'),
  hidden: true,
  meta: { title: '系统管理', systemManagement: true },
  children: [
    page('user', 'SystemUser', () => import('@/views/system/user/index.vue'), '用户管理', 'system:user:list'),
    page('role', 'SystemRole', () => import('@/views/system/role/index.vue'), '角色管理', 'system:role:list'),
    page('menu', 'SystemMenu', () => import('@/views/system/menu/index.vue'), '菜单管理', 'system:menu:list'),
    page('dept', 'SystemDept', () => import('@/views/system/dept/index.vue'), '部门管理', 'system:dept:list'),
    page('post', 'SystemPost', () => import('@/views/system/post/index.vue'), '岗位管理', 'system:post:list'),
    page('dict', 'SystemDict', () => import('@/views/system/dict/index.vue'), '字典管理', 'system:dict:list'),
    page('config', 'SystemConfig', () => import('@/views/system/config/index.vue'), '参数设置', 'system:config:list'),
    page('showroom', 'SystemShowroom', () => import('@/views/system/showroom/index.vue'), '展厅管理', 'system:showroom:list'),
    page('notice', 'SystemNotice', () => import('@/views/system/notice/index.vue'), '通知公告', 'system:notice:list'),
    page('log/operlog', 'SystemOperlog', () => import('@/views/monitor/operlog/index.vue'), '操作日志', 'monitor:operlog:list', {
      alias: ['/monitor/operlog', '/system/operlog'], meta: { activeMenu: '/system/log/operlog' }
    }),
    page('log/logininfor', 'SystemLogininfor', () => import('@/views/monitor/logininfor/index.vue'), '登录日志', 'monitor:logininfor:list', {
      alias: ['/monitor/logininfor', '/system/logininfor'], meta: { activeMenu: '/system/log/logininfor' }
    }),
    page('user-auth/role/:userId(\\d+)', 'AuthRole', () => import('@/views/system/user/authRole.vue'), '分配角色', 'system:user:edit', {
      meta: { activeMenu: '/system/user' }
    }),
    page('role-auth/user/:roleId(\\d+)', 'AuthUser', () => import('@/views/system/role/authUser.vue'), '分配用户', 'system:role:edit', {
      meta: { activeMenu: '/system/role' }
    }),
    page('dict-data/index/:dictId(\\d+)', 'Data', () => import('@/views/system/dict/data.vue'), '字典数据', 'system:dict:list', {
      meta: { activeMenu: '/system/dict' }
    })
  ]
}
