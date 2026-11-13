import request from '@/utils/request'

export const spaceApi = {
  async getDraft(projectId) {
    const response = await request({ url: '/build/space/draft', method: 'get', params: { projectId }, timeout: 30000 })
    return response.data
  },
  async getPublished(projectId) {
    const response = await request({ url: '/build/space/published', method: 'get', params: { projectId }, timeout: 30000 })
    return response.data
  },
  async saveDraft(projectId, payload) {
    const response = await request({
      url: '/build/space/draft', method: 'put', params: { projectId }, data: payload, timeout: 30000,
      headers: { repeatSubmit: false }
    })
    return response.data
  },
  async publishDraft(projectId, payload) {
    const response = await request({ url: '/build/space/publish', method: 'post', params: { projectId }, data: payload, timeout: 30000 })
    return response.data
  }
}
