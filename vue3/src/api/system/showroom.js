import request from '@/utils/request'

// 查询展厅列表
export function listShowroom(query) {
  return request({
    url: '/system/showroom/list',
    method: 'get',
    params: query
  })
}

// 查询展厅详细
export function getShowroom(id) {
  return request({
    url: '/system/showroom/' + id,
    method: 'get'
  })
}

// 新增展厅
export function addShowroom(data) {
  return request({
    url: '/system/showroom',
    method: 'post',
    data: data
  })
}

// 修改展厅
export function updateShowroom(data) {
  return request({
    url: '/system/showroom',
    method: 'put',
    data: data
  })
}
