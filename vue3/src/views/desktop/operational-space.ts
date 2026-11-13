import { parseSpace } from './apps/SpaceEditor/model'
import type { Space } from './apps/SpaceEditor/types'

interface SpaceDocument { snapshot: Space }
interface ReadApi {
  getPublished(projectId: string): Promise<SpaceDocument | null>
  getDraft(projectId: string): Promise<SpaceDocument | null>
}

export interface OperationalSpaceResult {
  space: Space | null
  source: 'published' | 'draft' | 'empty'
}

export async function readOperationalSpace(projectId: string, api: ReadApi): Promise<OperationalSpaceResult> {
  let draft: SpaceDocument | null = null
  let draftError: unknown = null
  try { draft = await api.getDraft(projectId) } catch (error) { draftError = error }
  if (draft) return { space: parseSpace(JSON.stringify(draft.snapshot)), source: 'draft' }
  try {
    const published = await api.getPublished(projectId)
    if (published) return { space: parseSpace(JSON.stringify(published.snapshot)), source: 'published' }
  } catch (error) {
    if (!draftError) throw error
  }
  if (draftError) throw draftError
  return { space: null, source: 'empty' }
}
