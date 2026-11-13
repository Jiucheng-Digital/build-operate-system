<template>
  <AppWindow title="空间编辑器" @close="emit('close')" @minimize="emit('minimize')">
    <div class="se" :class="{ 'is-readonly': readonly || store.loadError.value, 'is-load-error': store.loadError.value }" @keydown.esc.stop.prevent="escape">
      <div v-if="readonly" class="se-readonly" role="alert">当前账号没有空间编辑权限，可查看和导出空间。</div>
      <div v-if="store.loadError.value" class="se-readonly" role="alert">空间加载失败，已暂停编辑和保存。<span v-if="loadErrorDetail">{{ loadErrorDetail }}</span><button type="button" @click="reloadSpace">重新加载</button></div>
      <div v-if="saveConflict" class="se-readonly" role="alert">服务器空间已被其他人修改。请先导出当前编辑内容，再重新加载处理差异。<button type="button" @click="exportJson">导出当前 JSON</button><button type="button" @click="reloadSpace">重新加载</button></div>
      <div v-if="localDraftAvailable && !readonly" class="se-readonly" role="status">发现当前浏览器的旧空间草稿。<button type="button" @click="migrateLocalDraft">导入并保存到服务器</button><button type="button" @click="localDraftAvailable = false">忽略</button></div>
      <SpaceToolbar :name="store.space.value.name" :tool="tool" :snap="snap" :grid-visible="store.space.value.gridVisible" :preview="preview" :readonly="readonly || store.loading.value || store.loadError.value || publishing || store.saveState.value === 'saving'"
        :selected="store.selection.value.length" :can-undo="!!store.history.past.length" :can-redo="!!store.history.future.length"
        @tool="tool = $event" @action="handleAction" />
      <div class="se__body" v-loading="store.loading.value || publishing">
        <SpaceSidebar :space="store.space.value" :selection="store.selection.value" :active-layer-id="store.activeLayerId.value"
          :selected-device-id="selectedResourceId" :devices="visibleCatalogDevices" :manual-devices="visibleManualDevices"
          :devices-loading="devicesLoading" :devices-error="devicesError" :device-total="deviceTotal" :has-more="hasMoreDevices"
          :categories="categories" :categories-loading="categoriesLoading" :categories-error="categoriesError"
          :selected-category-code="selectedCategoryCode" :selected-category-name="selectedCategoryName"
          @layer-add="addLayer" @layer-active="store.activeLayerId.value = $event" @layer-update="updateLayer"
          @layer-order="reorderLayer" @layer-parent="moveLayer" @layer-rename="renameLayer" @layer-delete="deleteLayer" @select="selectOne" @device-select="selectedResourceId = $event"
          @refresh-devices="refreshResources" @manual-device="addManualDevice" @device-more="loadMoreDevices"
          @device-search="searchDevices" @category-select="selectDeviceCategory" />
        <main class="se__stage">
          <div class="se__stage-head"><span><b>画布</b> / {{ store.space.value.name }}</span>
            <div v-if="tool === 'bind'" class="se__bind-target">落点目标 <select v-model="bindFallback"><option value="space">空间</option><option value="layer">当前图层</option></select></div>
            <span>滚轮缩放 · Space 平移 · Shift 多选</span>
          </div>
          <SpaceCanvas :space="store.space.value" :selection="store.selection.value" :tool="tool" :snap="snap" :readonly="readonly || store.loadError.value || publishing" :cancel-token="cancelToken"
            :active-layer-id="store.activeLayerId.value" :preview="preview" @select="store.selection.value = $event"
            @create="createColumn" @move="moveSelection" @resize="resizeColumn" @bind="bindAt"
            @hover="cursor = $event" @zoom="zoom = $event" @context="openContext" />
        </main>
        <SpaceInspector :space="store.space.value" :selection="store.selection.value" @column-update="updateColumns"
          @binding-update="updateBinding" @binding-target="changeBindingTarget" @unbind="unbind"
          @space-update="updateSpace" @device-select="selectBoundDevice" @map-update="updateMap" @map-remove="removeMap" />
      </div>
      <footer class="se-status"><span>坐标 {{ cursor.x.toFixed(1) }}, {{ cursor.y.toFixed(1) }} {{ store.space.value.unit }}</span>
        <span>缩放 {{ Math.round(zoom * 100) }}%</span><span>当前层 {{ activeLayer?.name || '无' }}</span>
        <span>已选 {{ store.selection.value.length }}</span><span>吸附 {{ snap ? '开' : '关' }}</span>
        <span class="se-status__save" :class="store.saveState.value">{{ saveText }}</span>
        <span v-if="store.space.value.publishedAt">发布 {{ publishedTime }}</span>
      </footer>
      <input ref="importInput" type="file" accept=".json,application/json" hidden @change="importFile" />
      <input ref="mapInput" type="file" accept="image/png,image/jpeg,image/webp" hidden @change="importMap" />
      <div v-if="context" class="se-context" :style="{ left: `${context.x}px`, top: `${context.y}px` }" role="menu">
        <button v-for="item in contextActions" :key="item.key" type="button" role="menuitem" @click="contextAction(item.key)">{{ item.label }}</button>
      </div>
      <div v-if="issues.length" class="se-issues" role="alert"><strong>校验结果 · {{ issues.length }} 项</strong><button type="button" aria-label="关闭校验结果" @click="issues = []">✕</button><ul><li v-for="(issue, index) in issues" :key="index">{{ issue }}</li></ul></div>
    </div>
  </AppWindow>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDevicePage, listClassify } from '@/api/build/drive'
import AppWindow from '../../components/AppWindow.vue'
import SpaceToolbar from './SpaceToolbar.vue'
import SpaceSidebar from './SpaceSidebar.vue'
import { buildDeviceCategories, mergeDeviceRows, normalizeDeviceRows } from './device-resources'
import type { DeviceCategory, DeviceResource } from './device-resources'
import SpaceCanvas from './SpaceCanvas.vue'
import SpaceInspector from './SpaceInspector.vue'
import { bindingPosition, clone, deleteLayerTree, fitNotch, isBindingEditable, isEditable, layerDeletionPreview, layerEditable, layerFor, layerIsDescendant, makeBinding, newColumn, orderedLayers, round, uid, validateSpace } from './model'
import { useSpaceStore } from './store'
import { downloadSpace, localSpaceRepository, type SpaceRepository } from './persistence'
import type { Binding, Column, Point, Selection, ShapeType, Space, TargetType, Tool } from './types'
import './space-editor.scss'

const props = withDefaults(defineProps<{ readonly?: boolean; repository?: SpaceRepository; publisher?: (space: Space) => Promise<void> }>(), { readonly: false })
const emit = defineEmits(['close', 'minimize'])
const store = useSpaceStore(props.repository)
store.canEdit.value = !props.readonly
watch(() => props.readonly, value => { store.canEdit.value = !value; if (value) tool.value = 'select' })
const tool = ref<Tool>('select')
const cancelToken = ref(0)
const snap = ref(true)
const preview = ref(false)
const publishing = ref(false)
const saveConflict = ref(false)
const loadErrorDetail = ref('')
const localDraftAvailable = ref(false)
let localDraft: Space | null = null
const bindFallback = ref<'space' | 'layer'>('space')
const cursor = ref<Point>({ x: 0, y: 0 })
const zoom = ref(1)
const selectedResourceId = ref('')
const catalogDevices = ref<DeviceResource[]>([])
const manualDevices = ref<DeviceResource[]>([])
const categories = ref<DeviceCategory[]>([])
const categoriesLoading = ref(false)
const categoriesError = ref('')
const selectedCategoryCode = ref<string | null>(null)
const selectedCategoryName = ref('全部设备')
const deviceSearch = ref('')
const deviceTotal = ref<number | null>(null)
const hasMoreDevices = ref(false)
const fetchedDeviceCount = ref(0)
const devicePage = ref(0)
const devicesLoading = ref(false)
const devicesError = ref('')
const visibleManualDevices = computed(() => mergeDeviceRows(
  store.space.value.devices.filter(d => d.type === 'manual').map(d => ({ id: d.deviceId, name: d.name, type: d.type, status: d.status })), manualDevices.value
))
const visibleCatalogDevices = computed(() => {
  if (!devicesError.value || catalogDevices.value.length) return catalogDevices.value
  return store.space.value.devices.filter(d => d.type !== 'manual' && (!selectedCategoryCode.value || d.type === selectedCategoryCode.value))
    .map(d => ({ id: d.deviceId, name: d.name, type: d.type, status: d.status }))
})
const resources = computed(() => mergeDeviceRows(
  store.space.value.devices.map(d => ({ id: d.deviceId, name: d.name, type: d.type, status: d.status })),
  mergeDeviceRows(visibleCatalogDevices.value, visibleManualDevices.value)
))
const importInput = ref<HTMLInputElement | null>(null)
const mapInput = ref<HTMLInputElement | null>(null)
const issues = ref<string[]>([])
const context = ref<{ x: number; y: number } | null>(null)
const clipboard = ref<Column[]>([])
const activeLayer = computed(() => layerFor(store.space.value, store.activeLayerId.value))
const saveText = computed(() => ({ saved: '已保存', dirty: '未保存', saving: '保存中', error: '保存失败' })[store.saveState.value])
const publishedTime = computed(() => store.space.value.publishedAt
  ? new Date(store.space.value.publishedAt).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  : '')
const contextActions = [
  { key: 'copy', label: '复制' }, { key: 'delete', label: '删除' },
  { key: 'top', label: '置顶' }, { key: 'bottom', label: '置底' },
  { key: 'up', label: '上移一层' }, { key: 'down', label: '下移一层' },
  { key: 'lock', label: '锁定 / 解锁' }, { key: 'hide', label: '隐藏' },
  { key: 'bind', label: '绑定设备' }, { key: 'unbind', label: '解绑设备' }
]

function alertError(error: unknown, fallback: string) { ElMessage.error(error instanceof Error ? error.message : fallback) }
function requireEditableLayer() {
  if (!activeLayer.value) { ElMessage.warning('请先创建图层'); return false }
  if (!layerEditable(store.space.value, activeLayer.value.id)) { ElMessage.warning('当前图层或父级图层已锁定或隐藏'); return false }
  return true
}
function selectOne(selection: Selection) { store.selection.value = [selection]; context.value = null }
function createColumn(type: ShapeType, at: Point, size: { width: number; depth: number } | null, points: Point[]) {
  if (props.readonly) return
  if (!requireEditableLayer()) return
  const ids: string[] = []
  store.commit('创建体块', draft => {
    const column = newColumn(type, store.activeLayerId.value, at, draft.columns.length)
    if (size) { column.width = size.width; column.depth = size.depth }
    if (type === 'cube' || type === 'cylinder') column.depth = column.width
    if (type === 'polygon') column.points = points
    column.notchWidth = fitNotch(column.notchWidth, column.width)
    column.notchDepth = fitNotch(column.notchDepth, column.depth)
    draft.columns.push(column)
    ids.push(column.id)
  })
  store.selection.value = ids.map(id => ({ kind: 'column', id }))
}
function moveSelection(selection: Selection[], delta: Point) {
  const blocked = selection.some(s => s.kind === 'column'
    ? !!store.space.value.columns.find(c => c.id === s.id && !isEditable(store.space.value, c))
    : !!store.space.value.devices.find(d => d.id === s.id && !!store.space.value.bindings.find(b => b.deviceId === d.deviceId && !isBindingEditable(store.space.value, b))))
  if (blocked) { ElMessage.warning('选中对象中包含锁定或隐藏体块'); return }
  store.commit('移动对象', draft => {
    for (const item of selection) {
      if (item.kind === 'column') {
        const column = draft.columns.find(c => c.id === item.id)
        if (column) { column.x = round(column.x + delta.x); column.y = round(column.y + delta.y) }
      } else {
        const device = draft.devices.find(d => d.id === item.id)
        const binding = draft.bindings.find(b => b.deviceId === device?.deviceId)
        if (device && binding) {
          if (binding.targetType === 'column' && selection.some(s => s.kind === 'column' && s.id === binding.targetId)) continue
          const point = bindingPosition(draft, binding)
          const moved = { x: round(point.x + delta.x), y: round(point.y + delta.y) }
          const updated = makeBinding(draft, device, binding.targetType, binding.targetId, moved)
          binding.localX = updated.localX; binding.localY = updated.localY
          device.x = moved.x; device.y = moved.y
        }
      }
    }
    for (const binding of draft.bindings.filter(b => b.targetType === 'column')) {
      const device = draft.devices.find(d => d.deviceId === binding.deviceId)
      if (device) { const point = bindingPosition(draft, binding); device.x = point.x; device.y = point.y }
    }
  })
}
function resizeColumn(id: string, width: number, depth: number) {
  const column = store.space.value.columns.find(c => c.id === id)
  if (!column || !isEditable(store.space.value, column)) return
  store.commit('调整体块尺寸', draft => {
    const target = draft.columns.find(c => c.id === id)
    if (!target) return
    const oldWidth = target.width, oldDepth = target.depth
    target.width = width
    target.depth = target.type === 'cube' || target.type === 'cylinder' ? width : depth
    if (target.type === 'polygon') target.points = target.points.map(p => ({ x: round(p.x * width / oldWidth), y: round(p.y * target.depth / oldDepth) }))
    target.notchWidth = fitNotch(target.notchWidth, target.width)
    target.notchDepth = fitNotch(target.notchDepth, target.depth)
  })
}
function updateColumns(ids: string[], key: string, value: unknown) {
  if (!ids.length) return
  const selected = store.space.value.columns.filter(c => ids.includes(c.id))
  if (selected.some(c => !layerEditable(store.space.value, c.layerId))) { ElMessage.warning('所属图层或父级图层已锁定或隐藏'); return }
  if (!['locked', 'visible'].includes(key) && selected.some(c => !c.visible || c.locked)) {
    ElMessage.warning('请先显示并解锁体块，再修改属性'); return
  }
  if (key === 'layerId') {
    const target = layerFor(store.space.value, String(value))
    if (!target || !layerEditable(store.space.value, target.id)) { ElMessage.warning('目标图层或父级图层已锁定或隐藏'); return }
  }
  if (['x', 'y', 'width', 'depth', 'height', 'rotation', 'opacity', 'strokeWidth', 'notchWidth', 'notchDepth'].includes(key) && !Number.isFinite(Number(value)) ||
      ['width', 'depth'].includes(key) && Number(value) <= 0 || key === 'height' && Number(value) < 0 ||
      key === 'opacity' && (Number(value) <= 0 || Number(value) > 1) || key === 'strokeWidth' && Number(value) < 0) {
    ElMessage.warning('属性值超出有效范围'); return
  }
  if (key === 'name' && (!String(value).trim() || String(value).length > 120)) { ElMessage.warning('名称不能为空且不能超过 120 字'); return }
  store.commit('修改体块属性', draft => {
    draft.columns.filter(c => ids.includes(c.id)).forEach(column => {
      const oldWidth = column.width, oldDepth = column.depth
      ;(column as unknown as Record<string, unknown>)[key] = value
      if (column.type === 'cube' || column.type === 'cylinder') {
        if (key === 'width') column.depth = Number(value)
        if (key === 'depth') column.width = Number(value)
      }
      if (column.type === 'polygon' && (key === 'width' || key === 'depth')) {
        column.points = column.points.map(p => ({ x: round(p.x * column.width / oldWidth), y: round(p.y * column.depth / oldDepth) }))
      }
      column.notchWidth = fitNotch(column.notchWidth, column.width)
      column.notchDepth = fitNotch(column.notchDepth, column.depth)
    })
    if (['x', 'y', 'rotation'].includes(key)) for (const binding of draft.bindings.filter(b => b.targetType === 'column' && ids.includes(b.targetId))) {
      const device = draft.devices.find(d => d.deviceId === binding.deviceId)
      if (device) { const p = bindingPosition(draft, binding); device.x = p.x; device.y = p.y }
    }
  })
}
function updateLayer(id: string, key: 'visible' | 'locked', value: boolean) {
  const current = layerFor(store.space.value, id)
  if (!current) return
  if (current.parentId && !layerEditable(store.space.value, current.parentId)) { ElMessage.warning('父级图层已锁定或隐藏'); return }
  store.commit('修改图层', draft => { const layer = draft.layers.find(l => l.id === id); if (layer) layer[key] = value })
  if (!value && key === 'visible') store.selection.value = []
}
function addLayer(parentId: string | null = null) {
  if (props.readonly) return
  if (parentId && !layerEditable(store.space.value, parentId)) { ElMessage.warning('父级图层已锁定或隐藏'); return }
  const id = uid()
  store.commit(parentId ? '新增子图层' : '新增图层', draft => {
    const siblings = draft.layers.filter(layer => (layer.parentId || null) === parentId)
    const order = siblings.length ? Math.max(...siblings.map(layer => layer.order)) + 1 : 0
    draft.layers.push({ id, name: `图层 ${draft.layers.length + 1}`, order, visible: true, locked: false, parentId })
  })
  store.activeLayerId.value = id
}
async function deleteLayer(id: string) {
  if (props.readonly || store.loading.value || store.loadError.value) return
  const space = store.space.value
  const layer = layerFor(space, id)
  if (!layer) return
  const removal = layerDeletionPreview(space, id)
  if ([...removal.layerIds].some(layerId => !layerEditable(space, layerId))) {
    ElMessage.warning('请先显示并解锁要删除的图层及子图层')
    return
  }
  const childCount = removal.layerIds.size - 1
  const message = `删除“${layer.name}”${childCount ? `及 ${childCount} 个子图层` : ''}，将同时删除 ${removal.columnIds.size} 个体块和 ${removal.deviceIds.size} 个绑定点位。可撤销，保存后生效。`
  try {
    await ElMessageBox.confirm(message, '删除图层', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  } catch { return }
  const removedPointIds = new Set(space.devices.filter(device => removal.deviceIds.has(device.deviceId)).map(device => device.id))
  const parentId = layer.parentId
  if (!store.commit('删除图层', draft => { deleteLayerTree(draft, id) })) return
  store.selection.value = store.selection.value.filter(item => item.kind === 'column'
    ? !removal.columnIds.has(item.id) : !removedPointIds.has(item.id))
  if (!layerFor(store.space.value, store.activeLayerId.value)) {
    const fallback = parentId && layerEditable(store.space.value, parentId) ? parentId :
      orderedLayers(store.space.value).find(item => layerEditable(store.space.value, item.id))?.id
    store.activeLayerId.value = fallback || orderedLayers(store.space.value)[0]?.id || ''
  }
}
async function renameLayer(id: string) {
  const layer = layerFor(store.space.value, id)
  if (!layer || props.readonly) return
  if (!layerEditable(store.space.value, id)) { ElMessage.warning('请先显示并解锁图层'); return }
  try {
    const { value } = await ElMessageBox.prompt('图层名称', '重命名图层', { inputValue: layer.name, inputPattern: /\S+/, inputErrorMessage: '请输入名称' })
    if (value.length > 120) { ElMessage.warning('图层名称不能超过 120 字'); return }
    store.commit('重命名图层', draft => { const target = draft.layers.find(l => l.id === id); if (target) target.name = value.trim() })
  } catch (_error) { /* 用户取消 */ }
}
function reorderLayer(id: string, direction: number) {
  if (!layerEditable(store.space.value, id)) { ElMessage.warning('请先显示并解锁图层'); return }
  store.commit('调整图层顺序', draft => {
    const target = layerFor(draft, id)
    if (!target) return
    const ordered = draft.layers.filter(layer => (layer.parentId || null) === (target.parentId || null)).sort((a, b) => a.order - b.order)
    const i = ordered.findIndex(l => l.id === id), next = i + direction
    if (i < 0 || next < 0 || next >= ordered.length) return
    ;[ordered[i], ordered[next]] = [ordered[next], ordered[i]]
    ordered.forEach((layer, index) => { layer.order = index })
  })
}
function moveLayer(id: string, parentId: string | null) {
  if (props.readonly) return
  if (!layerEditable(store.space.value, id)) { ElMessage.warning('请先显示并解锁图层'); return }
  if (parentId === id || parentId && layerIsDescendant(store.space.value, parentId, id)) { ElMessage.warning('不能将图层移入自身或其子图层'); return }
  if (parentId && !layerEditable(store.space.value, parentId)) { ElMessage.warning('目标父级图层已锁定或隐藏'); return }
  store.commit('调整父级图层', draft => {
    const layer = layerFor(draft, id)
    if (!layer || (layer.parentId || null) === parentId) return
    const siblings = draft.layers.filter(item => (item.parentId || null) === parentId)
    layer.parentId = parentId
    layer.order = siblings.length ? Math.max(...siblings.map(item => item.order)) + 1 : 0
  })
}
function updateSpace(key: string, value: unknown) {
  if (['scale', 'gridSize', 'bounds.width', 'bounds.depth'].includes(key) && (!Number.isFinite(Number(value)) || Number(value) <= 0)) { ElMessage.warning('数值必须大于 0'); return }
  if (['origin.x', 'origin.y'].includes(key) && !Number.isFinite(Number(value))) { ElMessage.warning('原点坐标必须为有限数值'); return }
  if (key === 'gridOpacity' && (!Number.isFinite(Number(value)) || Number(value) < 0 || Number(value) > 1)) { ElMessage.warning('网格透明度应在 0 到 100% 之间'); return }
  if (key === 'name' && (!String(value).trim() || String(value).length > 120) || key === 'unit' && (!String(value).trim() || String(value).length > 32)) { ElMessage.warning('名称或单位不能为空，且不能超过长度限制'); return }
  store.commit('修改空间设置', draft => {
    const parts = key.split('.')
    if (parts.length === 2) ((draft as unknown as Record<string, Record<string, unknown>>)[parts[0]])[parts[1]] = value
    else (draft as unknown as Record<string, unknown>)[key] = value
  })
}

function updateMap(key: string, value: number) {
  if (!Number.isFinite(value) || (['width', 'depth'].includes(key) && value <= 0) || (key === 'opacity' && (value <= 0 || value > 1)) || (key === 'thickness' && value < 0)) {
    ElMessage.warning('地图数值无效'); return
  }
  store.commit('调整地图', draft => { if (draft.map) (draft.map as unknown as Record<string, unknown>)[key] = value })
}
function removeMap() { store.commit('移除地图', draft => { draft.map = null }) }

async function importMap(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  try {
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) throw new Error('请选择 PNG、JPG 或 WebP 图片')
    if (file.size > 25 * 1024 * 1024) throw new Error('地图文件不能超过 25 MB')
    if (file.name.length > 255) throw new Error('地图文件名不能超过 255 字')
    const bitmap = await createImageBitmap(file)
    try {
      let ratio = Math.min(1, 2200 / Math.max(bitmap.width, bitmap.height))
      let dataUrl = ''
      for (let attempt = 0; attempt < 6; attempt++) {
        const canvas = document.createElement('canvas')
        canvas.width = Math.max(1, Math.round(bitmap.width * ratio))
        canvas.height = Math.max(1, Math.round(bitmap.height * ratio))
        canvas.getContext('2d')?.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
        dataUrl = canvas.toDataURL('image/webp', 0.78)
        if (dataUrl.length <= 650_000) break
        ratio *= 0.75
      }
      if (dataUrl.length > 650_000) throw new Error('地图图片过大，请先压缩图片后重试')
      const fit = Math.min(store.space.value.bounds.width / bitmap.width, store.space.value.bounds.depth / bitmap.height)
      store.commit('上传地图', draft => {
        draft.map = { name: file.name, dataUrl, x: 0, y: 0,
          width: Math.max(0.1, round(bitmap.width * fit)), depth: Math.max(0.1, round(bitmap.height * fit)), opacity: 0.75, thickness: 1.2 }
      })
      store.selection.value = []
      ElMessage.success('地图已放置在空间底图，点击保存后可回显')
    } finally { bitmap.close() }
  } catch (error) { alertError(error, '地图上传失败') }
  finally { if (mapInput.value) mapInput.value.value = '' }
}

const devicePageSize = 50
let deviceRequestId = 0
let categoryRequestId = 0
let searchTimer: ReturnType<typeof setTimeout> | null = null

async function loadCategories() {
  const requestId = ++categoryRequestId
  categoriesLoading.value = true
  categoriesError.value = ''
  try {
    const response = await listClassify() as { data?: unknown }
    if (requestId !== categoryRequestId) return
    categories.value = buildDeviceCategories(response.data)
    if (!categories.value.length) categoriesError.value = '暂无分类，可查看全部设备'
  } catch (_error) {
    if (requestId === categoryRequestId) categoriesError.value = '分类加载失败，可查看全部设备'
  } finally { if (requestId === categoryRequestId) categoriesLoading.value = false }
}

async function loadDevices(append = false) {
  if (append && (devicesLoading.value || !hasMoreDevices.value)) return
  const requestId = ++deviceRequestId
  const pageNum = append ? devicePage.value + 1 : 1
  if (!append) {
    catalogDevices.value = []
    fetchedDeviceCount.value = 0
    deviceTotal.value = null
    hasMoreDevices.value = false
  }
  devicesLoading.value = true
  devicesError.value = ''
  try {
    const params: Record<string, string | number> = { pageNum, pageSize: devicePageSize }
    if (selectedCategoryCode.value) params.classify = selectedCategoryCode.value
    if (deviceSearch.value.trim()) params.name = deviceSearch.value.trim()
    const response = await getDevicePage(params) as { rows?: unknown; total?: number | string }
    if (requestId !== deviceRequestId) return
    const rows = normalizeDeviceRows(response.rows, selectedCategoryCode.value)
    const previous = append ? catalogDevices.value : []
    const next = mergeDeviceRows(previous, rows)
    const total = response.total == null ? null : Number(response.total)
    catalogDevices.value = next
    fetchedDeviceCount.value += rows.length
    devicePage.value = pageNum
    deviceTotal.value = total !== null && Number.isFinite(total) && total >= 0 ? total : null
    hasMoreDevices.value = deviceTotal.value === null ? rows.length > 0 : fetchedDeviceCount.value < deviceTotal.value
    if (append && next.length === previous.length) hasMoreDevices.value = false
  } catch (_error) {
    if (requestId === deviceRequestId) devicesError.value = append ? '后续设备加载失败' : '设备列表加载失败'
  } finally { if (requestId === deviceRequestId) devicesLoading.value = false }
}

function loadMoreDevices() { loadDevices(true) }
function refreshResources() {
  if (searchTimer) { clearTimeout(searchTimer); searchTimer = null }
  loadCategories()
  loadDevices()
}
function selectDeviceCategory(code: string | null, name: string) {
  if (searchTimer) { clearTimeout(searchTimer); searchTimer = null }
  selectedCategoryCode.value = code
  selectedCategoryName.value = name
  selectedResourceId.value = ''
  loadDevices()
}
function searchDevices(query: string) {
  deviceSearch.value = query
  selectedResourceId.value = ''
  if (searchTimer) clearTimeout(searchTimer)
  deviceRequestId++
  catalogDevices.value = []
  fetchedDeviceCount.value = 0
  devicePage.value = 0
  deviceTotal.value = null
  hasMoreDevices.value = false
  devicesError.value = ''
  devicesLoading.value = true
  searchTimer = setTimeout(() => { searchTimer = null; loadDevices() }, 300)
}
async function addManualDevice() {
  if (props.readonly) return
  try {
    const { value } = await ElMessageBox.prompt('输入坐标点或 POI 名称', '添加手动点位', { inputPattern: /\S+/, inputErrorMessage: '请输入名称' })
    if (value.length > 120) { ElMessage.warning('点位名称不能超过 120 字'); return }
    const id = `poi-${uid()}`
    manualDevices.value.unshift({ id, name: value, type: 'manual', status: 'online' })
    selectedResourceId.value = id
    tool.value = 'bind'
    ElMessage.info('在画布点击位置完成绑定')
  } catch (_error) { /* 用户取消 */ }
}
function bindAt(point: Point, hit: { kind: string; id: string } | null, providedDeviceId: string | null) {
  if (props.readonly) { ElMessage.warning('当前账号没有空间编辑权限'); return }
  const deviceId = providedDeviceId || selectedResourceId.value
  if (!deviceId) { ElMessage.warning('先从左侧选择设备或添加手动点位'); return }
  if (store.space.value.bindings.some(b => b.deviceId === deviceId)) { ElMessage.warning('该设备已绑定，请先解绑或在属性面板中重新定位'); return }
  const resource = resources.value.find(d => d.id === deviceId)
  if (!resource) { ElMessage.warning('设备资源不存在，请刷新设备列表'); return }
  let targetType: TargetType = bindFallback.value
  let targetId = targetType === 'space' ? store.space.value.id : store.activeLayerId.value
  if (hit?.kind === 'column') { targetType = 'column'; targetId = hit.id }
  const target = store.space.value.columns.find(c => c.id === targetId)
  if (targetType === 'column' && (!target || !isEditable(store.space.value, target))) { ElMessage.warning('目标体块已锁定或隐藏'); return }
  if (targetType === 'layer' && !layerEditable(store.space.value, targetId)) { ElMessage.warning('目标图层或父级图层已锁定或隐藏'); return }
  if (Math.abs(point.x) > store.space.value.bounds.width / 2 || Math.abs(point.y) > store.space.value.bounds.depth / 2) { ElMessage.warning('点位超出空间边界'); return }
  const id = uid()
  store.commit('绑定设备', draft => {
    const device = { id, deviceId, name: resource.name, type: resource.type, status: resource.status, x: point.x, y: point.y, z: 0 }
    draft.devices.push(device)
    draft.bindings.push(makeBinding(draft, device, targetType, targetId, point))
  })
  store.selection.value = [{ kind: 'device', id }]
  selectedResourceId.value = deviceId
  ElMessage.success('设备点位已绑定')
}
function updateBinding(id: string, key: string, value: unknown) {
  const current = store.space.value.bindings.find(b => b.id === id)
  if (!current || !isBindingEditable(store.space.value, current)) { ElMessage.warning('绑定目标已锁定或隐藏'); return }
  store.commit('修改设备绑定', draft => {
    const binding = draft.bindings.find(b => b.id === id)
    if (binding) {
      ;(binding as unknown as Record<string, unknown>)[key] = value
      const device = draft.devices.find(d => d.deviceId === binding.deviceId)
      if (device) { const p = bindingPosition(draft, binding); device.x = p.x; device.y = p.y; device.z = binding.z }
    }
  })
}
function changeBindingTarget(id: string, targetType: string, targetId: string) {
  const current = store.space.value.bindings.find(b => b.id === id)
  if (!current || !isBindingEditable(store.space.value, current)) { ElMessage.warning('绑定目标已锁定或隐藏'); return }
  const type = targetType as TargetType
  const fallbackId = type === 'space' ? store.space.value.id
    : type === 'layer' ? store.space.value.layers.find(layer => layerEditable(store.space.value, layer.id))?.id
      : store.space.value.columns.find(column => isEditable(store.space.value, column))?.id
  const nextId = targetId || fallbackId
  if (!nextId) { ElMessage.warning('请先创建可绑定目标'); return }
  if (type === 'layer' && !layerEditable(store.space.value, nextId)) { ElMessage.warning('图层或父级图层已锁定或隐藏'); return }
  if (type === 'column') {
    const column = store.space.value.columns.find(c => c.id === nextId)
    if (!column || !isEditable(store.space.value, column)) { ElMessage.warning('目标体块已锁定或隐藏'); return }
  }
  store.commit('重新绑定目标', draft => {
    const binding = draft.bindings.find(b => b.id === id)
    const device = draft.devices.find(d => d.deviceId === binding?.deviceId)
    if (!binding || !device) return
    const point = bindingPosition(draft, binding)
    const next = makeBinding(draft, device, type, nextId, point)
    Object.assign(binding, { targetType: type, targetId: nextId, localX: next.localX, localY: next.localY })
  })
}
function unbind(id: string) {
  const current = store.space.value.bindings.find(b => b.id === id)
  if (!current || !isBindingEditable(store.space.value, current)) { ElMessage.warning('绑定目标已锁定或隐藏'); return }
  store.commit('解绑设备', draft => {
    const binding = draft.bindings.find(b => b.id === id)
    draft.bindings = draft.bindings.filter(b => b.id !== id)
    if (binding) draft.devices = draft.devices.filter(d => d.deviceId !== binding.deviceId)
  })
  store.selection.value = []
}
function selectBoundDevice(deviceId: string) { const d = store.space.value.devices.find(d => d.deviceId === deviceId); if (d) store.selection.value = [{ kind: 'device', id: d.id }] }

function deleteSelection() {
  const selection = store.selection.value
  if (!selection.length) return
  if (selection.some(s => s.kind === 'column' && store.space.value.columns.some(c => c.id === s.id && !isEditable(store.space.value, c)))) {
    ElMessage.warning('选中体块已锁定或隐藏'); return
  }
  if (selection.some(s => s.kind === 'device' && store.space.value.devices.some(d => d.id === s.id && store.space.value.bindings.some(b => b.deviceId === d.deviceId && !isBindingEditable(store.space.value, b))))) {
    ElMessage.warning('选中设备的目标已锁定或隐藏'); return
  }
  store.commit('删除对象', draft => {
    const columnIds = new Set(selection.filter(s => s.kind === 'column').map(s => s.id))
    const deviceIds = new Set(selection.filter(s => s.kind === 'device').map(s => s.id))
    draft.columns = draft.columns.filter(c => !columnIds.has(c.id) || !isEditable(draft, c))
    const deletedColumnIds = new Set([...columnIds].filter(id => !draft.columns.some(c => c.id === id)))
    const deletedDeviceIds = new Set(draft.devices.filter(d => deviceIds.has(d.id)).map(d => d.deviceId))
    draft.bindings = draft.bindings.filter(b => !deletedDeviceIds.has(b.deviceId) && !(b.targetType === 'column' && deletedColumnIds.has(b.targetId)))
    const stillBound = new Set(draft.bindings.map(b => b.deviceId))
    draft.devices = draft.devices.filter(d => stillBound.has(d.deviceId))
  })
  store.selection.value = []
}
function copySelection() { clipboard.value = clone(store.space.value.columns.filter(c => store.selection.value.some(s => s.kind === 'column' && s.id === c.id))); if (clipboard.value.length) ElMessage.success(`已复制 ${clipboard.value.length} 个体块`) }
function pasteSelection() {
  if (!clipboard.value.length || !requireEditableLayer()) return
  const ids: string[] = []
  store.commit('粘贴体块', draft => {
    clipboard.value.forEach(source => {
      const copy = { ...clone(source), id: uid(), name: `${source.name} 副本`, layerId: store.activeLayerId.value, x: source.x + 2, y: source.y + 2, order: draft.columns.length }
      draft.columns.push(copy); ids.push(copy.id)
    })
  })
  store.selection.value = ids.map(id => ({ kind: 'column', id }))
}
function reorderColumns(mode: 'top' | 'bottom' | 'up' | 'down') {
  const ids = new Set(store.selection.value.filter(s => s.kind === 'column').map(s => s.id))
  if (store.space.value.columns.some(c => ids.has(c.id) && !isEditable(store.space.value, c))) { ElMessage.warning('锁定或隐藏的体块不可调整层级'); return }
  store.commit('调整对象层级', draft => {
    for (const layer of draft.layers) {
      const list = draft.columns.filter(c => c.layerId === layer.id).sort((a, b) => a.order - b.order)
      const picked = list.filter(c => ids.has(c.id))
      if (!picked.length) continue
      let ordered = list
      if (mode === 'top') ordered = [...list.filter(c => !ids.has(c.id)), ...picked]
      if (mode === 'bottom') ordered = [...picked, ...list.filter(c => !ids.has(c.id))]
      if (mode === 'up') for (let i = list.length - 2; i >= 0; i--) if (ids.has(list[i].id) && !ids.has(list[i + 1].id)) [list[i], list[i + 1]] = [list[i + 1], list[i]]
      if (mode === 'down') for (let i = 1; i < list.length; i++) if (ids.has(list[i].id) && !ids.has(list[i - 1].id)) [list[i], list[i - 1]] = [list[i - 1], list[i]]
      ordered.forEach((c, index) => { c.order = index })
    }
  })
}
function validate() { issues.value = validateSpace(store.space.value); if (!issues.value.length) ElMessage.success('校验通过：对象、图层与绑定关系有效') }
async function save(): Promise<boolean> {
  if (publishing.value || store.saveState.value === 'saving') return false
  try {
    issues.value = await store.save()
    if (issues.value.length) { ElMessage.warning('存在校验问题，请查看右下角列表'); return false }
    saveConflict.value = false
    ElMessage.success(props.repository ? '已保存到服务器' : '已保存到当前浏览器')
    return true
  } catch (error) {
    if ((error as { code?: number })?.code === 409) saveConflict.value = true
    alertError(error, '空间保存失败')
    return false
  }
}
async function publish() {
  if (publishing.value || store.loadError.value) return
  issues.value = validateSpace(store.space.value)
  if (issues.value.length) { ElMessage.warning('请先修复校验问题'); return }
  if (props.publisher) {
    if (!(await save())) return
    if (store.saveState.value !== 'saved') { ElMessage.warning('保存期间有新的修改，请再次发布'); return }
    publishing.value = true
    try {
      await props.publisher(clone(store.space.value))
      await store.load()
      ElMessage.success('空间已发布')
    } catch (error) { alertError(error, '空间发布失败，请重新加载确认状态') }
    finally { publishing.value = false }
    return
  }
  store.commit('发布空间', draft => { draft.publishedAt = new Date().toISOString() })
  if (await save()) ElMessage.info('已标记为本地发布版本；项目尚无空间发布接口')
}
async function reloadSpace() {
  localDraftAvailable.value = false
  localDraft = null
  loadErrorDetail.value = ''
  try {
    await store.load()
    saveConflict.value = false
    if (props.repository && store.emptyOnLoad.value) {
      try {
        const draft = await localSpaceRepository.read()
        if (draft) {
          localDraft = draft
          localDraftAvailable.value = true
        }
      } catch { /* 旧浏览器缓存损坏不影响服务器空间 */ }
    }
  } catch (error) {
    loadErrorDetail.value = error instanceof Error ? error.message : '空间数据加载失败'
    alertError(error, '空间数据加载失败')
  }
}
async function migrateLocalDraft() {
  if (!localDraft || props.readonly || store.loadError.value) return
  try {
    store.replace(JSON.stringify(localDraft))
    if (await save()) { localDraftAvailable.value = false; localDraft = null }
  } catch (error) { alertError(error, '导入旧空间草稿失败') }
}
function exportJson() {
  downloadSpace(store.space.value)
}
async function importFile(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  try { store.replace(await file.text()); issues.value = []; ElMessage.success('空间 JSON 已导入，点击保存后刷新可回显') }
  catch (error) { alertError(error, '导入失败') }
  if (importInput.value) importInput.value.value = ''
}
function handleAction(action: string) {
  context.value = null
  if (store.loading.value && action !== 'close') return
  if (store.loadError.value && action !== 'close') { ElMessage.warning('请先重新加载空间'); return }
  if (publishing.value && action !== 'close') return
  if (props.readonly && !['export', 'validate', 'preview', 'close'].includes(action)) { ElMessage.warning('当前账号没有空间编辑权限'); return }
  if (action === 'undo') store.undo()
  else if (action === 'redo') store.redo()
  else if (action === 'copy') copySelection()
  else if (action === 'delete') deleteSelection()
  else if (action === 'snap') snap.value = !snap.value
  else if (action === 'grid') updateSpace('gridVisible', !store.space.value.gridVisible)
  else if (action === 'preview') preview.value = !preview.value
  else if (action === 'map') mapInput.value?.click()
  else if (action === 'import') importInput.value?.click()
  else if (action === 'export') exportJson()
  else if (action === 'validate') validate()
  else if (action === 'save') save()
  else if (action === 'publish') publish()
  else if (action === 'close') emit('close')
}
function openContext(event: { x: number; y: number; hit: { kind: string; id: string } | null }) {
  if (event.hit?.kind === 'column' || event.hit?.kind === 'device') {
    if (!store.selection.value.some(s => s.kind === event.hit?.kind && s.id === event.hit?.id)) store.selection.value = [{ kind: event.hit.kind, id: event.hit.id } as Selection]
  }
  context.value = { x: Math.min(event.x, window.innerWidth - 170), y: Math.min(event.y, window.innerHeight - 320) }
}
function contextAction(action: string) {
  context.value = null
  if (['copy', 'delete'].includes(action)) { handleAction(action); return }
  if (['top', 'bottom', 'up', 'down'].includes(action)) { reorderColumns(action as 'top' | 'bottom' | 'up' | 'down'); return }
  if (action === 'lock' || action === 'hide') {
    const columns = store.selectedColumns.value
    updateColumns(columns.map(c => c.id), action === 'lock' ? 'locked' : 'visible', action === 'lock' ? !columns.every(c => c.locked) : false)
  }
  if (action === 'bind') { tool.value = 'bind'; ElMessage.info('选择设备后在画布标点') }
  if (action === 'unbind') {
    const selectedDeviceIds = new Set(store.selectedDevices.value.map(d => d.deviceId))
    const selectedColumnIds = new Set(store.selectedColumns.value.map(c => c.id))
    const bindings = store.space.value.bindings.filter(b => selectedDeviceIds.has(b.deviceId) || b.targetType === 'column' && selectedColumnIds.has(b.targetId))
    if (bindings.some(b => !isBindingEditable(store.space.value, b))) { ElMessage.warning('绑定目标已锁定或隐藏'); return }
    const ids = new Set(bindings.map(b => b.id))
    const deviceIds = new Set(bindings.map(b => b.deviceId))
    store.commit('批量解绑', draft => {
      draft.bindings = draft.bindings.filter(b => !ids.has(b.id))
      draft.devices = draft.devices.filter(d => !deviceIds.has(d.deviceId))
    })
    store.selection.value = store.selection.value.filter(s => s.kind === 'column')
  }
}
function escape() { context.value = null; issues.value = []; cancelToken.value++; tool.value = 'select' }
function keydown(event: KeyboardEvent) {
  if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement || event.target instanceof HTMLSelectElement) return
  if (store.loadError.value || publishing.value) return
  const mod = event.ctrlKey || event.metaKey
  if (props.readonly && (mod || event.key === 'Delete' || event.key === 'Backspace')) return
  if (mod && event.key.toLowerCase() === 'z') { event.preventDefault(); event.shiftKey ? store.redo() : store.undo() }
  else if (mod && event.key.toLowerCase() === 'c') { event.preventDefault(); copySelection() }
  else if (mod && event.key.toLowerCase() === 'v') { event.preventDefault(); pasteSelection() }
  else if (event.key === 'Delete' || event.key === 'Backspace') { event.preventDefault(); deleteSelection() }
  else if (event.key === 'Escape') escape()
}
function beforeUnload(event: BeforeUnloadEvent) { if (store.dirty.value) { event.preventDefault(); event.returnValue = '' } }

onMounted(() => {
  reloadSpace()
  refreshResources()
  window.addEventListener('keydown', keydown)
  window.addEventListener('beforeunload', beforeUnload)
  window.addEventListener('pointerdown', dismissContext)
})
function dismissContext(event: PointerEvent) { if (!(event.target as HTMLElement).closest('.se-context')) context.value = null }
onBeforeUnmount(() => {
  deviceRequestId++
  categoryRequestId++
  if (searchTimer) clearTimeout(searchTimer)
  window.removeEventListener('keydown', keydown)
  window.removeEventListener('beforeunload', beforeUnload)
  window.removeEventListener('pointerdown', dismissContext)
})
</script>
