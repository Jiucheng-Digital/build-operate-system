<template>
  <aside class="se-inspector">
    <div class="se-section-head"><strong>属性</strong><small>{{ selection.length ? `已选 ${selection.length}` : '空间设置' }}</small></div>
    <template v-if="selection.length > 1">
      <div class="se-inspector__group"><h3>批量操作</h3>
        <label>透明度 <input type="range" min="0.05" max="1" step="0.05" :value="selectedColumns[0]?.opacity ?? 0.7" @change="updateBatch('opacity', Number(($event.target as HTMLInputElement).value))" /></label>
        <label>所属图层 <select @change="updateBatch('layerId', ($event.target as HTMLSelectElement).value)"><option value="">选择图层</option><option v-for="layer in layerChoices" :key="layer.id" :value="layer.id">{{ layer.label }}</option></select></label>
        <div class="se-inline-actions"><button type="button" @click="updateBatch('locked', true)">锁定</button><button type="button" @click="updateBatch('locked', false)">解锁</button><button type="button" @click="updateBatch('visible', false)">隐藏</button><button type="button" @click="updateBatch('visible', true)">显示</button></div>
      </div>
    </template>
    <template v-else-if="column">
      <div class="se-inspector__group"><h3>基础信息</h3>
        <label>名称 <input type="text" :value="column.name" @change="update('name', ($event.target as HTMLInputElement).value)" /></label>
        <label>类型 <span>{{ shapeName(column.type) }}</span></label>
        <label>所属图层 <select :value="column.layerId" @change="update('layerId', ($event.target as HTMLSelectElement).value)"><option v-for="layer in layerChoices" :key="layer.id" :value="layer.id">{{ layer.label }}</option></select></label>
      </div>
      <div class="se-inspector__group"><h3>位置与尺寸 · {{ space.unit }}</h3>
        <div class="se-field-grid"><label>X <input type="number" step="0.1" :value="column.x" @change="updateNumber('x', $event)" /></label><label>Y <input type="number" step="0.1" :value="column.y" @change="updateNumber('y', $event)" /></label></div>
        <div class="se-field-grid"><label>宽 <input type="number" min="0.1" step="0.1" :value="column.width" @change="updateNumber('width', $event)" /></label><label>深 <input type="number" min="0.1" step="0.1" :value="column.depth" @change="updateNumber('depth', $event)" /></label></div>
        <div class="se-field-grid"><label>高 <input type="number" min="0" step="0.1" :value="column.height" @change="updateNumber('height', $event)" /></label><label>旋转 ° <input type="number" step="1" :value="column.rotation" @change="updateNumber('rotation', $event)" /></label></div>
        <template v-if="column.type === 'lshape'"><div class="se-field-grid"><label>缺口宽 <input type="number" min="0.2" step="0.1" :value="column.notchWidth" @change="updateNumber('notchWidth', $event)" /></label><label>缺口深 <input type="number" min="0.2" step="0.1" :value="column.notchDepth" @change="updateNumber('notchDepth', $event)" /></label></div></template>
      </div>
      <div class="se-inspector__group"><h3>样式</h3>
        <label>填充色 <input type="color" :value="column.fillColor" @change="update('fillColor', ($event.target as HTMLInputElement).value)" /></label>
        <label>描边色 <input type="color" :value="column.strokeColor" @change="update('strokeColor', ($event.target as HTMLInputElement).value)" /></label>
        <label>描边宽度 <input type="number" min="1" max="8" :value="column.strokeWidth" @change="updateNumber('strokeWidth', $event)" /></label>
        <label>透明度 {{ Math.round(column.opacity * 100) }}% <input type="range" min="0.05" max="1" step="0.05" :value="column.opacity" @change="updateNumber('opacity', $event)" /></label>
        <label>显示标签 <input type="checkbox" :checked="column.showLabel" @change="update('showLabel', ($event.target as HTMLInputElement).checked)" /></label>
        <label>锁定 <input type="checkbox" :checked="column.locked" @change="update('locked', ($event.target as HTMLInputElement).checked)" /></label>
        <label>可见 <input type="checkbox" :checked="column.visible" @change="update('visible', ($event.target as HTMLInputElement).checked)" /></label>
      </div>
      <div class="se-inspector__group"><h3>绑定设备 · {{ columnBindings.length }}</h3>
        <button v-for="binding in columnBindings" :key="binding.id" type="button" class="se-binding-row" @click="emit('device-select', binding.deviceId)">{{ binding.label }} <small>查看</small></button>
        <p v-if="!columnBindings.length" class="se-muted">从左侧拖入设备，落在当前体块内即可绑定。</p>
      </div>
    </template>
    <template v-else-if="device && binding">
      <div class="se-inspector__group"><h3>设备绑定</h3>
        <label>设备 <span>{{ device.name }}</span></label>
        <label>标签 <input type="text" :value="binding.label" @change="emit('binding-update', binding.id, 'label', ($event.target as HTMLInputElement).value)" /></label>
        <label>目标类型 <select :value="binding.targetType" @change="emit('binding-target', binding.id, ($event.target as HTMLSelectElement).value, '')"><option value="space">空间</option><option value="column">柱体 / 区域</option><option value="layer">图层</option></select></label>
        <label>绑定目标 <select :value="binding.targetId" @change="emit('binding-target', binding.id, binding.targetType, ($event.target as HTMLSelectElement).value)"><option v-for="option in targetOptions" :key="option.id" :value="option.id">{{ option.name }}</option></select></label>
        <div class="se-field-grid"><label>局部 X <input type="number" step="0.1" :value="binding.localX" @change="emit('binding-update', binding.id, 'localX', Number(($event.target as HTMLInputElement).value))" /></label><label>局部 Y <input type="number" step="0.1" :value="binding.localY" @change="emit('binding-update', binding.id, 'localY', Number(($event.target as HTMLInputElement).value))" /></label></div>
        <label>高度 Z <input type="number" min="0" step="0.1" :value="binding.z" @change="emit('binding-update', binding.id, 'z', Number(($event.target as HTMLInputElement).value))" /></label>
        <label>显示绑定线 <input type="checkbox" :checked="binding.showLine" @change="emit('binding-update', binding.id, 'showLine', ($event.target as HTMLInputElement).checked)" /></label>
        <button type="button" class="se-danger" @click="emit('unbind', binding.id)">解绑设备</button>
      </div>
    </template>
    <template v-else>
      <div class="se-inspector__group"><h3>空间坐标</h3>
        <label>空间名称 <input type="text" :value="space.name" @change="emit('space-update', 'name', ($event.target as HTMLInputElement).value)" /></label>
        <div class="se-field-grid"><label>原点 X <input type="number" step="1" :value="space.origin.x" @change="emit('space-update', 'origin.x', Number(($event.target as HTMLInputElement).value))" /></label><label>原点 Y <input type="number" step="1" :value="space.origin.y" @change="emit('space-update', 'origin.y', Number(($event.target as HTMLInputElement).value))" /></label></div>
        <div class="se-field-grid"><label>比例尺 <input type="number" min="0.01" step="0.1" :value="space.scale" @change="emit('space-update', 'scale', Number(($event.target as HTMLInputElement).value))" /></label><label>网格间距 <input type="number" min="0.1" step="0.1" :value="space.gridSize" @change="emit('space-update', 'gridSize', Number(($event.target as HTMLInputElement).value))" /></label></div>
        <div class="se-field-grid"><label>边界宽 <input type="number" min="1" :value="space.bounds.width" @change="emit('space-update', 'bounds.width', Number(($event.target as HTMLInputElement).value))" /></label><label>边界深 <input type="number" min="1" :value="space.bounds.depth" @change="emit('space-update', 'bounds.depth', Number(($event.target as HTMLInputElement).value))" /></label></div>
        <label>单位 <input type="text" :value="space.unit" @change="emit('space-update', 'unit', ($event.target as HTMLInputElement).value)" /></label>
        <p class="se-muted">空间坐标以原点和比例尺换算；保存时使用本地坐标。</p>
      </div>
      <div class="se-inspector__group"><h3>网格样式</h3>
        <label>显示网格 <input type="checkbox" :checked="space.gridVisible" @change="emit('space-update', 'gridVisible', ($event.target as HTMLInputElement).checked)" /></label>
        <label>网格颜色 <input type="color" :value="space.gridColor" @change="emit('space-update', 'gridColor', ($event.target as HTMLInputElement).value)" /></label>
        <label>网格透明度 {{ Math.round(space.gridOpacity * 100) }}% <input type="range" min="0" max="1" step="0.05" :value="space.gridOpacity" @change="emit('space-update', 'gridOpacity', Number(($event.target as HTMLInputElement).value))" /></label>
      </div>
      <div class="se-inspector__group"><h3>地图底图</h3>
        <template v-if="space.map">
          <img class="se-map-thumb" :src="space.map.dataUrl" :alt="space.map.name" />
          <label class="se-map-file">文件 <span :title="space.map.name">{{ space.map.name }}</span></label>
          <div class="se-field-grid"><label>中心 X <input type="number" step="0.1" :value="space.map.x" @change="mapNumber('x', $event)" /></label><label>中心 Y <input type="number" step="0.1" :value="space.map.y" @change="mapNumber('y', $event)" /></label></div>
          <div class="se-field-grid"><label>宽 <input type="number" min="0.1" step="0.1" :value="space.map.width" @change="mapNumber('width', $event)" /></label><label>深 <input type="number" min="0.1" step="0.1" :value="space.map.depth" @change="mapNumber('depth', $event)" /></label></div>
          <label>透明度 {{ Math.round(space.map.opacity * 100) }}% <input type="range" min="0.1" max="1" step="0.05" :value="space.map.opacity" @change="mapNumber('opacity', $event)" /></label>
          <label>预览厚度 <input type="number" min="0" step="0.1" :value="space.map.thickness" @change="mapNumber('thickness', $event)" /></label>
          <p class="se-muted">地图图片随空间草稿一起保存到服务器；点击“保存”后，重新打开会回显。</p>
          <button type="button" class="se-danger" @click="emit('map-remove')">移除地图</button>
        </template>
        <p v-else class="se-muted">点击顶部“上传地图”，选择 PNG、JPG 或 WebP 图片作为底图。</p>
      </div>
    </template>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { layerAncestors, orderedLayers } from './model'
import type { Binding, Column, Selection, ShapeType, Space } from './types'
const props = defineProps<{ space: Space; selection: Selection[] }>()
const emit = defineEmits<{
  'column-update': [ids: string[], key: string, value: unknown]; 'binding-update': [id: string, key: string, value: unknown]
  'binding-target': [id: string, type: string, targetId: string]; unbind: [id: string]
  'space-update': [key: string, value: unknown]; 'device-select': [id: string]
  'map-update': [key: string, value: number]; 'map-remove': []
}>()
const selectedColumns = computed(() => props.space.columns.filter(c => props.selection.some(s => s.kind === 'column' && s.id === c.id)))
const column = computed(() => props.selection.length === 1 ? selectedColumns.value[0] : null)
const device = computed(() => props.selection.length === 1 ? props.space.devices.find(d => props.selection[0].kind === 'device' && props.selection[0].id === d.id) : null)
const binding = computed(() => props.space.bindings.find(b => b.deviceId === device.value?.deviceId))
const columnBindings = computed(() => props.space.bindings.filter(b => b.targetType === 'column' && b.targetId === column.value?.id))
const layerChoices = computed(() => orderedLayers(props.space).map(layer => ({ id: layer.id,
  label: `${'　'.repeat(Math.max(0, layerAncestors(props.space, layer.id).length - 1))}${layer.name}` })))
const targetOptions = computed(() => binding.value?.targetType === 'column' ? props.space.columns.map(c => ({ id: c.id, name: c.name })) : binding.value?.targetType === 'layer' ? layerChoices.value.map(l => ({ id: l.id, name: l.label })) : [{ id: props.space.id, name: props.space.name }])
function update(key: string, value: unknown) { if (column.value) emit('column-update', [column.value.id], key, value) }
function updateNumber(key: string, event: Event) { update(key, Number((event.target as HTMLInputElement).value)) }
function updateBatch(key: string, value: unknown) { emit('column-update', selectedColumns.value.map(c => c.id), key, value) }
function mapNumber(key: string, event: Event) { emit('map-update', key, Number((event.target as HTMLInputElement).value)) }
function shapeName(type: ShapeType) { return ({ box: '长方体', cube: '正方体', lshape: 'L 形柱体', cylinder: '圆柱体', polygon: '自定义多边形' })[type] }
</script>
