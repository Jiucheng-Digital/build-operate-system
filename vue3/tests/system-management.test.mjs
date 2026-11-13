import test from 'node:test'
import assert from 'node:assert/strict'
import { createMemoryHistory, createRouter } from 'vue-router'
import { systemRoutes } from '../src/router/system.js'
import { systemMenus, visibleSystemMenus, withoutMigratedSystemRoutes } from '../src/views/desktop/config/systemMenus.js'

test('系统管理的全部菜单都能匹配桌面页面和对应权限', () => {
  const router = createRouter({ history: createMemoryHistory('/build/'), routes: [systemRoutes] })
  assert.equal(systemMenus.length, 10)
  const pages = systemMenus.flatMap(item => item.children || [item])
  assert.equal(pages.length, 11)
  for (const page of pages) {
    const route = router.resolve(page.path)
    assert.equal(route.meta.systemManagement, true)
    assert.equal(route.meta.systemPermission, page.permission)
    assert.equal(route.meta.title, page.title)
  }
  for (const [path, parent] of [
    ['/system/user-auth/role/104', '/system/user'],
    ['/system/role-auth/user/2', '/system/role'],
    ['/system/dict-data/index/1', '/system/dict'],
    ['/monitor/operlog', '/system/log/operlog'],
    ['/monitor/logininfor', '/system/log/logininfor']
  ]) {
    assert.equal(router.resolve(path).meta.activeMenu, parent)
    assert.equal(router.resolve(path).matched[0].path, '/system')
  }
})

test('按账号权限展示菜单，日志分组仅包含有权访问的子菜单', () => {
  assert.deepEqual(visibleSystemMenus(() => false), [])
  const grants = new Set(['system:showroom:list', 'monitor:logininfor:list'])
  const menus = visibleSystemMenus(permission => grants.has(permission))
  assert.deepEqual(menus.map(item => item.key), ['showroom', 'log'])
  assert.deepEqual(menus[1].children.map(item => item.key), ['logininfor'])
  assert.equal(systemMenus.at(-1).children.length, 2)
})

test('旧后台路由不会覆盖迁移页面，同时保留其他模块路由且不修改响应数据', () => {
  const routes = [
    { path: '/system', component: 'Layout', children: [
      { path: 'user', component: 'system/user/index' },
      { path: 'log', component: 'ParentView', children: [
        { path: 'operlog', component: 'monitor/operlog/index' },
        { path: 'logininfor', component: 'monitor/logininfor/index' }
      ] }
    ] },
    { path: '/monitor', component: 'Layout', children: [
      { path: 'operlog', component: 'monitor/operlog/index' },
      { path: 'server', component: 'monitor/server/index' }
    ] }
  ]
  const original = structuredClone(routes)
  assert.deepEqual(withoutMigratedSystemRoutes(routes), [
    { path: '/monitor', component: 'Layout', children: [{ path: 'server', component: 'monitor/server/index' }] }
  ])
  assert.deepEqual(routes, original)
})
