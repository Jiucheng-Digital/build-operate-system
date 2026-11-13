<template>
  <aside class="se-sidebar">
    <div class="se-sidebar__column se-sidebar__column--layers">
    <div class="se-section-head"><strong>图层</strong><button type="button" title="新增顶级图层" aria-label="新增顶级图层" @click="emit('layer-add', null)"><el-icon><Plus /></el-icon></button></div>
    <div class="se-layer-list">
      <div v-for="row in layerRows" :key="row.layer.id" class="se-layer" :class="{ active: activeLayerId === row.layer.id, muted: !layerVisible(space, row.layer.id) }" :style="{ paddingLeft: `${4 + Math.min(row.depth, 6) * 12}px` }">
        <button type="button" class="se-layer__toggle" :disabled="!row.hasChildren" :aria-label="`${expandedLayers.has(row.layer.id) ? '折叠' : '展开'}${row.layer.name}`" :aria-expanded="row.hasChildren ? expandedLayers.has(row.layer.id) : undefined" @click="toggleLayer(row.layer.id)"><el-icon v-if="row.hasChildren"><ArrowDown v-if="expandedLayers.has(row.layer.id)" /><ArrowRight v-else /></el-icon></button>
        <button type="button" class="se-layer__name" :title="`设为当前图层：${row.layer.name}；双击重命名`" @click="emit('layer-active', row.layer.id)" @dblclick="emit('layer-rename', row.layer.id)"><i></i><span>{{ row.layer.name }}</span></button>
        <button type="button" title="新增子图层" :aria-label="`在${row.layer.name}下新增子图层`" @click="emit('layer-add', row.layer.id)"><el-icon><Plus /></el-icon></button>
        <button type="button" :title="row.layer.visible ? '隐藏图层及子图层' : '显示图层及子图层'" :aria-label="`${row.layer.visible ? '隐藏' : '显示'}${row.layer.name}`" @click="emit('layer-update', row.layer.id, 'visible', !row.layer.visible)"><el-icon><View v-if="row.layer.visible" /><Hide v-else /></el-icon></button>
        <button type="button" :title="row.layer.locked ? '解锁图层及子图层' : '锁定图层及子图层'" :aria-label="`${row.layer.locked ? '解锁' : '锁定'}${row.layer.name}`" @click="emit('layer-update', row.layer.id, 'locked', !row.layer.locked)"><el-icon><Lock v-if="row.layer.locked" /><Unlock v-else /></el-icon></button>
      </div>
    </div>
    <div v-if="activeLayer" class="se-layer-controls">
      <label>父级 <select :value="activeLayer.parentId || ''" @change="emit('layer-parent', activeLayer.id, ($event.target as HTMLSelectElement).value || null)"><option value="">顶级图层</option><option v-for="layer in availableParents" :key="layer.id" :value="layer.id">{{ layerPath(layer.id) }}</option></select></label>
      <div><span>同级顺序</span><button type="button" title="上移当前图层" @click="emit('layer-order', activeLayer.id, 1)"><el-icon><ArrowUp /></el-icon></button><button type="button" title="下移当前图层" @click="emit('layer-order', activeLayer.id, -1)"><el-icon><ArrowDown /></el-icon></button><button type="button" class="se-layer-controls__delete" title="删除当前图层及子图层" :aria-label="`删除${activeLayer.name}及其子图层`" @click="emit('layer-delete', activeLayer.id)"><el-icon><Delete /></el-icon></button></div>
    </div>
    <div class="se-section-head"><strong>空间对象 <small>{{ space.columns.length }}</small></strong></div>
    <div class="se-object-list">
      <button v-for="column in space.columns" :key="column.id" type="button" :class="{ active: selected('column', column.id), muted: !column.visible || !layerVisible(space, column.layerId) }" @click="emit('select', { kind: 'column', id: column.id })">
        <span class="se-object-dot" :style="{ background: column.fillColor }"></span><span>{{ column.name }}</span><small>{{ shapeName(column.type) }}</small>
      </button>
      <p v-if="!space.columns.length" class="se-muted">还没有体块，从工具栏开始创建</p>
    </div>
    </div>
    <div class="se-sidebar__column se-sidebar__column--devices">
    <div class="se-section-head"><strong>设备 / POI</strong><button type="button" title="刷新分类和设备" aria-label="刷新分类和设备" @click="emit('refresh-devices')"><el-icon><Refresh /></el-icon></button></div>
    <input class="se-search" type="search" placeholder="按设备名称搜索" :value="search" @input="onSearch" />
    <div class="se-device-categories" aria-label="设备分类">
      <button type="button" class="se-category" :class="{ active: selectedCategoryCode === null }" @click="emit('category-select', null, '全部设备')"><span class="se-category__arrow"><el-icon><Grid /></el-icon></span><span>全部设备</span></button>
      <p v-if="categoriesLoading" class="se-muted">正在加载分类…</p>
      <p v-else-if="categoriesError" class="se-muted">{{ categoriesError }}</p>
      <button v-for="row in categoryRows" :key="row.category.code" type="button" class="se-category" :class="{ active: selectedCategoryCode === row.category.code }"
        :style="{ paddingLeft: `${8 + row.depth * 13}px` }" :title="row.category.name"
        :aria-expanded="row.category.children.length ? expanded.has(row.category.code) : undefined" @click="chooseCategory(row.category)">
        <span class="se-category__arrow"><el-icon v-if="row.category.children.length"><ArrowDown v-if="expanded.has(row.category.code)" /><ArrowRight v-else /></el-icon></span><span>{{ row.category.name }}</span>
      </button>
    </div>
    <div class="se-device-list-head"><strong>{{ selectedCategoryName }}</strong><small>{{ devices.length }} / {{ deviceTotal === null ? '…' : deviceTotal }}</small></div>
    <div class="se-device-list" @scroll.passive="onDeviceScroll">
      <p v-if="filteredManualDevices.length" class="se-device-list__group">手动点位</p>
      <button v-for="device in filteredManualDevices" :key="device.id" type="button" draggable="true" :class="{ active: selectedDeviceId === device.id, bound: boundIds.has(device.id) }"
        @click="emit('device-select', device.id)" @dragstart="dragStart($event, device.id)">
        <span class="se-device-status" :class="device.status"></span><span><strong>{{ device.name }}</strong><small>{{ device.serialNumber || device.id }}</small></span><em>{{ boundIds.has(device.id) ? '已绑定' : '拖入' }}</em>
      </button>
      <p v-if="devicesLoading && !devices.length" class="se-muted">正在加载设备…</p>
      <p v-else-if="devicesError && !devices.length" class="se-muted">{{ devicesError }} <button type="button" @click="emit('refresh-devices')">重试</button></p>
      <p v-else-if="!devices.length && !filteredManualDevices.length && !devicesLoading" class="se-muted">当前分类暂无设备。可选择其他分类或添加手动点位。</p>
      <button v-for="device in devices" :key="device.id" type="button" draggable="true" :class="{ active: selectedDeviceId === device.id, bound: boundIds.has(device.id) }"
        @click="emit('device-select', device.id)" @dragstart="dragStart($event, device.id)">
        <span class="se-device-status" :class="device.status"></span><span><strong>{{ device.name }}</strong><small>{{ device.serialNumber || device.id }}</small></span><em>{{ boundIds.has(device.id) ? '已绑定' : '拖入' }}</em>
      </button>
      <p v-if="devicesError && devices.length" class="se-muted">{{ devicesError }}</p>
      <button v-if="hasMore" type="button" class="se-device-more" :disabled="devicesLoading" @click="emit('device-more')">{{ devicesLoading ? '正在加载…' : devicesError ? '重试加载' : '加载更多设备' }}</button>
    </div>
    <button type="button" class="se-add-point" @click="emit('manual-device')"><el-icon><Plus /></el-icon><span>手动坐标点</span></button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ArrowDown, ArrowRight, ArrowUp, Delete, Grid, Hide, Lock, Plus, Refresh, Unlock, View } from '@element-plus/icons-vue'
import { layerAncestors, layerEditable, layerIsDescendant, layerVisible, orderedLayers } from './model'
import type { Layer, Selection, ShapeType, Space } from './types'
import type { DeviceCategory, DeviceResource } from './device-resources'
const props = defineProps<{
  space: Space; selection: Selection[]; activeLayerId: string; selectedDeviceId: string
  devices: DeviceResource[]; manualDevices: DeviceResource[]; devicesLoading: boolean; devicesError: string
  deviceTotal: number | null; hasMore: boolean; categories: DeviceCategory[]; categoriesLoading: boolean; categoriesError: string
  selectedCategoryCode: string | null; selectedCategoryName: string
}>()
const emit = defineEmits<{
  'layer-add': [parentId: string | null]; 'layer-active': [id: string]; 'layer-update': [id: string, key: 'visible' | 'locked', value: boolean]
  'layer-order': [id: string, direction: number]; 'layer-parent': [id: string, parentId: string | null]; 'layer-rename': [id: string]; 'layer-delete': [id: string]; select: [selection: Selection]; 'device-select': [id: string]
  'refresh-devices': []; 'manual-device': []; 'device-more': []; 'device-search': [query: string]
  'category-select': [code: string | null, name: string]
}>()
const search = ref('')
const expanded = ref<Set<string>>(new Set())
const expandedLayers = ref<Set<string>>(new Set())
const activeLayer = computed(() => props.space.layers.find(layer => layer.id === props.activeLayerId))
const availableParents = computed(() => activeLayer.value
  ? orderedLayers(props.space).filter(layer => !layerIsDescendant(props.space, layer.id, activeLayer.value!.id)
    && (layer.id === activeLayer.value!.parentId || layerEditable(props.space, layer.id)))
  : [])
const layerRows = computed(() => {
  const rows: { layer: Layer; depth: number; hasChildren: boolean }[] = []
  const seen = new Set<string>()
  function visit(parentId: string | null, depth: number) {
    const children = props.space.layers.filter(layer => (layer.parentId || null) === parentId).sort((a, b) => b.order - a.order)
    for (const layer of children) {
      if (seen.has(layer.id)) continue
      seen.add(layer.id)
      const hasChildren = props.space.layers.some(child => child.parentId === layer.id)
      rows.push({ layer, depth, hasChildren })
      if (hasChildren && expandedLayers.value.has(layer.id)) visit(layer.id, depth + 1)
    }
  }
  visit(null, 0)
  return rows
})
watch(() => layerAncestors(props.space, props.activeLayerId).map(layer => layer.id).join('/'), () => {
  const id = props.activeLayerId
  if (!id) return
  const next = new Set(expandedLayers.value)
  for (const ancestor of layerAncestors(props.space, id).slice(1)) next.add(ancestor.id)
  expandedLayers.value = next
}, { immediate: true })
function toggleLayer(id: string) {
  const next = new Set(expandedLayers.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedLayers.value = next
}
function layerPath(id: string) { return layerAncestors(props.space, id).reverse().map(layer => layer.name).join(' / ') }
const boundIds = computed(() => new Set(props.space.bindings.map(b => b.deviceId)))
const filteredManualDevices = computed(() => props.manualDevices.filter(d => `${d.name} ${d.id}`.toLowerCase().includes(search.value.toLowerCase())))
const categoryRows = computed(() => {
  const rows: { category: DeviceCategory; depth: number }[] = []
  function visit(categories: DeviceCategory[], depth: number) {
    for (const category of categories) {
      rows.push({ category, depth })
      if (expanded.value.has(category.code)) visit(category.children, depth + 1)
    }
  }
  visit(props.categories, 0)
  return rows
})
function chooseCategory(category: DeviceCategory) {
  if (category.children.length) {
    const next = new Set(expanded.value)
    if (next.has(category.code)) next.delete(category.code)
    else next.add(category.code)
    expanded.value = next
  } else emit('category-select', category.code, category.name)
}
function onSearch(event: Event) { search.value = (event.target as HTMLInputElement).value; emit('device-search', search.value) }
function onDeviceScroll(event: Event) {
  const el = event.target as HTMLElement
  if (props.hasMore && !props.devicesLoading && el.scrollHeight - el.scrollTop - el.clientHeight < 80) emit('device-more')
}
function selected(kind: string, id: string) { return props.selection.some(s => s.kind === kind && s.id === id) }
function shapeName(type: ShapeType) { return ({ box: '矩形', cube: '方形', lshape: 'L 形', cylinder: '圆形', polygon: '多边形' })[type] }
function dragStart(event: DragEvent, id: string) { event.dataTransfer?.setData('application/x-neucity-device', id); if (event.dataTransfer) event.dataTransfer.effectAllowed = 'copy' }
</script>
