import { parseSpace } from './model'
import type { Space } from './types'

export interface SpaceRepository {
  read(): Promise<Space | null>
  write(space: Space): Promise<void>
}

const STORAGE_KEY = 'neucity-space-editor-v1'

export const localSpaceRepository: SpaceRepository = {
  async read() {
    const json = localStorage.getItem(STORAGE_KEY)
    return json ? parseSpace(json) : null
  },
  async write(space) { localStorage.setItem(STORAGE_KEY, JSON.stringify(space)) }
}

export function serializeSpace(space: Space): string { return JSON.stringify(space, null, 2) }

export function downloadSpace(space: Space) {
  const blob = new Blob([serializeSpace(space)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `${space.name || 'space'}.json`
  anchor.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
