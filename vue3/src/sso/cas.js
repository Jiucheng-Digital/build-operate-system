import request from '@/utils/request'
import { setToken } from '@/utils/auth'

const CAS_API_PREFIX = '/system/sso/cas'

/** 当前前端公开入口；必须与门户及后台白名单登记值完全一致。 */
export function getCasServiceUrl() {
  return new URL(import.meta.env.BASE_URL || '/', window.location.origin).href
}

/** 获取CAS重定向携带的服务票据。 */
export function getCasTicket(route) {
  const routeTicket = route?.query?.ticket
  if (Array.isArray(routeTicket)) return routeTicket[0]
  if (routeTicket) return routeTicket
  return new URLSearchParams(window.location.search).get('ticket')
}

/** 用CAS服务票据换取本系统令牌。 */
export async function exchangeCasTicket(ticket, userStore) {
  const service = getCasServiceUrl()
  const response = await request({
    url: `${CAS_API_PREFIX}/callback`,
    method: 'get',
    headers: { isToken: false },
    params: { ticket, service }
  })
  if (!response?.token) {
    throw new Error('统一认证未返回有效的登录令牌')
  }
  setToken(response.token)
  userStore.token = response.token
}

/** 清除地址栏中的一次性票据，防止刷新时重复消费。 */
export function routeWithoutCasTicket(route) {
  const browserUrl = new URL(window.location.href)
  browserUrl.searchParams.delete('ticket')
  window.history.replaceState(
    window.history.state,
    '',
    `${browserUrl.pathname}${browserUrl.search}${browserUrl.hash}`
  )

  const query = { ...route.query }
  delete query.ticket
  return { path: route.path, query, hash: route.hash, replace: true }
}
