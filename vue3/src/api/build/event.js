import request from '@/utils/request'

// 事件列表（分页）：GET /build/event/page?pageNum=&pageSize=&eventName=&level=&handleState=
// 默认只传 pageNum/pageSize 查全部；eventName、level(low/middle/high)、handleState(apply/pending/completed) 选填
export function getEventPage(query) {
  return request({
    url: '/build/event/page',
    method: 'get',
    params: query
  })
}

// 事件总览统计：GET /build/event/overview
// data: { highNum/applyHighNum/pendingHighNum, middleNum/.../, lowNum/.../ }
export function getEventOverview() {
  return request({
    url: '/build/event/overview',
    method: 'get'
  })
}

// 事件规则列表（分页）：status(enable/disable)、eventLevel(low/middle/high)、ruleName 选填
export function getEventRulePage(query) {
  return request({
    url: '/build/event/rule/page',
    method: 'get',
    params: query
  })
}

// 新增事件规则
export function addEventRule(data) {
  return request({
    url: '/build/event/rule',
    method: 'post',
    data
  })
}

export function editEventRuleStatus(data) {
  return request({
    url: '/build/event/rule/status/edit',
    method: 'put',
    data
  })
}
