import { parseSpace } from './model'
import type { SpaceRepository } from './persistence'
import type { Space } from './types'

export interface DraftDocument {
  spaceId: string
  revision: number | string
  snapshot: Space
}

export interface SaveResult {
  spaceId: string
  revision: number | string
}

export interface PublishResult {
  revision?: number | string
  publishedRevision: number | string
  publishedAt: string
}

export interface SpaceApi {
  getDraft(projectId: string): Promise<DraftDocument | null>
  saveDraft(projectId: string, payload: { baseRevision: number; snapshot: Space }): Promise<SaveResult>
  publishDraft(projectId: string, payload: { baseRevision: number }): Promise<PublishResult>
}

function parseRevision(value: unknown): number | null {
  if (typeof value === 'string') {
    if (!/^(0|[1-9]\d*)$/.test(value)) return null
    value = Number(value)
  }
  return Number.isSafeInteger(value) && (value as number) >= 0 ? value as number : null
}

export function createRemoteSpaceConnection(projectId: string, api: SpaceApi) {
  if (!projectId.trim()) throw new Error('缺少项目 ID，无法加载空间')
  let revision: number | null = null
  let spaceId: string | null = null

  const repository: SpaceRepository = {
    async read() {
      const document = await api.getDraft(projectId)
      if (document == null) {
        revision = 0
        spaceId = null
        return null
      }
      const loadedRevision = parseRevision(document.revision)
      if (loadedRevision === null || loadedRevision < 1 || !document.spaceId) {
        throw new Error('空间接口返回的修订号或空间 ID 无效')
      }
      const snapshot = parseSpace(JSON.stringify(document.snapshot))
      if (snapshot.id !== document.spaceId) throw new Error('空间接口返回的 ID 与快照不一致')
      revision = loadedRevision
      spaceId = document.spaceId
      return snapshot
    },
    async write(space) {
      if (revision === null) throw new Error('空间尚未加载，不能保存')
      if (spaceId && space.id !== spaceId) throw new Error('空间 ID 已变化，请重新加载后再保存')
      const result = await api.saveDraft(projectId, { baseRevision: revision, snapshot: space })
      const savedRevision = parseRevision(result?.revision)
      if (savedRevision === null || savedRevision <= revision || result.spaceId !== space.id) {
        throw new Error('空间保存接口没有返回有效的修订号或空间 ID')
      }
      revision = savedRevision
      spaceId = result.spaceId
    }
  }

  async function publish(space: Space): Promise<void> {
    if (revision === null || revision === 0 || spaceId !== space.id) throw new Error('请先保存空间草稿')
    const result = await api.publishDraft(projectId, { baseRevision: revision })
    const nextRevision = parseRevision(result?.revision ?? result?.publishedRevision)
    const publishedRevision = parseRevision(result?.publishedRevision)
    if (nextRevision === null || nextRevision < revision ||
        publishedRevision === null || publishedRevision < revision || !result.publishedAt) {
      throw new Error('空间发布接口返回的数据无效')
    }
    revision = nextRevision
  }

  return { repository, publish }
}
