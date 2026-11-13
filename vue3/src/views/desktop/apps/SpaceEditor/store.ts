import { computed, reactive, ref } from 'vue'
import { clone, emptySpace, isUuid, layerEditable, orderedLayers, parseSpace, uid, upgradeDefaultClinicLayers, validateSpace } from './model'
import { localSpaceRepository, type SpaceRepository } from './persistence'
import type { Selection, Space } from './types'

interface SpaceCommand { label: string; before: string; after: string }

export function useSpaceStore(repository: SpaceRepository = localSpaceRepository) {
  const space = ref<Space>(emptySpace())
  const selection = ref<Selection[]>([])
  const activeLayerId = ref(space.value.layers.find(layer => layer.parentId)?.id || space.value.layers[0].id)
  const loading = ref(true)
  const loadError = ref(false)
  const emptyOnLoad = ref(false)
  const saveState = ref<'saved' | 'dirty' | 'saving' | 'error'>('saved')
  const canEdit = ref(true)
  const history = reactive<{ past: SpaceCommand[]; future: SpaceCommand[] }>({ past: [], future: [] })
  let lastSaved = ''
  let saving = false

  const selectedColumns = computed(() => space.value.columns.filter(c => selection.value.some(s => s.kind === 'column' && s.id === c.id)))
  const selectedDevices = computed(() => space.value.devices.filter(d => selection.value.some(s => s.kind === 'device' && s.id === d.id)))
  const dirty = computed(() => saveState.value === 'dirty' || saveState.value === 'error' && !loadError.value)

  async function load() {
    loading.value = true
    try {
      const loaded = await repository.read()
      emptyOnLoad.value = !loaded
      lastSaved = loaded ? JSON.stringify(loaded) : ''
      const next = loaded ? clone(loaded) : emptySpace()
      const upgraded = !!loaded && canEdit.value && upgradeDefaultClinicLayers(next)
      space.value = next
      saveState.value = !loaded || upgraded ? 'dirty' : 'saved'
      loadError.value = false
      const ordered = orderedLayers(space.value)
      const preferred = !loaded || upgraded
        ? ordered.find(layer => layer.parentId && layerEditable(space.value, layer.id))
        : undefined
      activeLayerId.value = preferred?.id || ordered.find(layer => layerEditable(space.value, layer.id))?.id || ordered[0]?.id || ''
      selection.value = []
      history.past.splice(0)
      history.future.splice(0)
      if (upgraded) history.past.push({ label: '创建门诊楼层', before: lastSaved, after: JSON.stringify(next) })
    } catch (error) {
      loadError.value = true
      saveState.value = 'error'
      throw error
    } finally {
      loading.value = false
    }
  }

  function commit(label: string, change: (draft: Space) => void): boolean {
    if (!canEdit.value || loading.value || loadError.value) return false
    const before = JSON.stringify(space.value)
    const next = clone(space.value)
    change(next)
    const after = JSON.stringify(next)
    if (before === after) return false
    history.past.push({ label, before, after })
    if (history.past.length > 100) history.past.shift()
    history.future.splice(0)
    space.value = next
    saveState.value = after === lastSaved ? 'saved' : 'dirty'
    return true
  }

  function undo() {
    if (!canEdit.value || loading.value || loadError.value) return
    const command = history.past.pop()
    if (!command) return
    history.future.push(command)
    space.value = parseSpace(command.before)
    saveState.value = JSON.stringify(space.value) === lastSaved ? 'saved' : 'dirty'
  }

  function redo() {
    if (!canEdit.value || loading.value || loadError.value) return
    const command = history.future.pop()
    if (!command) return
    history.past.push(command)
    space.value = parseSpace(command.after)
    saveState.value = JSON.stringify(space.value) === lastSaved ? 'saved' : 'dirty'
  }

  function repairFirstDraftId() {
    if (!emptyOnLoad.value || isUuid(space.value.id)) return
    const nextId = uid()
    const replaceId = (snapshot: Space): Space => {
      const updated = clone(snapshot)
      updated.id = nextId
      for (const binding of updated.bindings) {
        if (binding.targetType === 'space') binding.targetId = nextId
      }
      return updated
    }
    space.value = replaceId(space.value)
    for (const command of [...history.past, ...history.future]) {
      command.before = JSON.stringify(replaceId(parseSpace(command.before)))
      command.after = JSON.stringify(replaceId(parseSpace(command.after)))
    }
  }

  async function save(): Promise<string[]> {
    if (loading.value || loadError.value) throw new Error('空间加载失败，请重试后再保存')
    if (!canEdit.value) throw new Error('当前账号没有空间编辑权限')
    if (saving) throw new Error('空间正在保存，请稍候')
    repairFirstDraftId()
    const issues = validateSpace(space.value)
    if (issues.length) return issues
    saving = true
    saveState.value = 'saving'
    try {
      const serialized = JSON.stringify(space.value)
      await repository.write(parseSpace(serialized))
      emptyOnLoad.value = false
      lastSaved = serialized
      saveState.value = JSON.stringify(space.value) === serialized ? 'saved' : 'dirty'
      return []
    } catch (error) {
      saveState.value = 'error'
      throw error
    } finally {
      saving = false
    }
  }

  function replace(json: string) {
    if (loading.value || loadError.value) throw new Error('空间尚未加载，不能导入')
    const next = parseSpace(json)
    const importedId = next.id
    next.id = space.value.id
    next.publishedAt = space.value.publishedAt
    for (const binding of next.bindings) {
      if (binding.targetType === 'space' && binding.targetId === importedId) binding.targetId = next.id
    }
    const issues = validateSpace(next)
    if (issues.length) throw new Error(issues.join('；'))
    commit('导入空间', draft => Object.assign(draft, next))
    selection.value = []
    const ordered = orderedLayers(next)
    activeLayerId.value = ordered.find(layer => layerEditable(next, layer.id))?.id || ordered[0]?.id || ''
  }

  return { space, selection, activeLayerId, loading, loadError, emptyOnLoad, saveState, canEdit, history, dirty,
    selectedColumns, selectedDevices, load, commit, undo, redo, save, replace }
}
