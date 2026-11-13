import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isHttp, isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useUserStore from '@/store/modules/user'
import useLockStore from '@/store/modules/lock'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'
import {
  exchangeCasTicket,
  getCasTicket,
  routeWithoutCasTicket
} from '@/sso/cas'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

router.beforeEach(async (to, from) => {
  NProgress.start()
  const userStore = useUserStore()
  const ticket = getCasTicket(to)

  if (ticket) {
    try {
      await exchangeCasTicket(ticket, userStore)
      useLockStore().unlockScreen()
      return routeWithoutCasTicket(to)
    } catch (error) {
      const cleanRoute = routeWithoutCasTicket(to)
      await userStore.resetToken()
      const message = error?.message || '统一认证登录失败，请重新登录'
      ElMessage.error(message)
      return { path: '/login', query: { redirect: cleanRoute.path }, replace: true }
    }
  }

  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title)
    const isLock = useLockStore().isLock
    if (to.path === '/login') {
      NProgress.done()
      return { path: '/' }
    }
    if (isWhiteList(to.path)) {
      return true
    }
    if (isLock && to.path !== '/lock') {
      NProgress.done()
      return { path: '/lock' }
    }
    if (!isLock && to.path === '/lock') {
      NProgress.done()
      return { path: '/' }
    }
    if (userStore.roles.length === 0) {
      isRelogin.show = true
      try {
        // 拉取user_info信息
        await userStore.getInfo()
        isRelogin.show = false
        // 根据roles权限生成可访问的路由
        const accessRoutes = await usePermissionStore().generateRoutes()
        accessRoutes.forEach(route => {
          if (!isHttp(route.path)) {
            router.addRoute(route)
          }
        })
        // 重新导航到目标路由，确保动态路由已注册
        return { ...to, replace: true }
      } catch (err) {
        await userStore.resetToken()
        isRelogin.show = false
        ElMessage.error(err)
        return `/login?redirect=${to.fullPath}`
      }
    }
    return true
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      // 在免登录白名单，直接进入
      return true
    }
    NProgress.done()
    return `/login?redirect=${to.fullPath}` // 否则全部重定向到登录页
  }
})

router.afterEach(() => {
  NProgress.done()
})
