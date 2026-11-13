<template>
  <div class="spatial-twin" :class="`spatial-twin--${mode}`">
    <div v-if="space && layerRows.length" class="spatial-twin__floor" aria-label="空间图层选择">
      <div v-for="row in shownLayerRows" :key="row.layer.id" class="spatial-twin__floor-row" :class="{ 'is-muted': !row.visible }"
        :style="{ paddingLeft: `${Math.min(row.depth, 3) * 8}px` }">
        <button v-if="row.hasChildren" type="button" class="spatial-twin__floor-toggle"
          :class="{ 'is-expanded': expandedLayers.has(row.layer.id) }"
          :aria-label="`${expandedLayers.has(row.layer.id) ? '收起' : '展开'}${row.layer.name}`"
          :aria-expanded="expandedLayers.has(row.layer.id)" @click="toggleLayer(row.layer.id)"></button>
        <span v-else class="spatial-twin__floor-spacer" aria-hidden="true"></span>
        <button type="button" class="spatial-twin__floor-name"
          :class="{ 'is-active': row.layer.id === activeLayerId }"
          :title="row.visible ? row.layer.name : `${row.layer.name}（已隐藏）`" :aria-current="row.layer.id === activeLayerId ? 'true' : undefined"
          @click="emit('select-layer', row.layer.id)">{{ row.layer.name }}</button>
      </div>
    </div>

    <svg v-if="space" class="spatial-twin__model" :viewBox="viewBox" preserveAspectRatio="xMidYMid meet"
      role="group" :aria-label="`${space.name}的${activeLayerName}空间图`">
      <rect :x="-space.bounds.width / 2" :y="-space.bounds.depth / 2" :width="space.bounds.width" :height="space.bounds.depth" fill="#f5f9ff" />
      <image v-if="space.map" :href="space.map.dataUrl" :x="space.map.x - space.map.width / 2"
        :y="-space.map.y - space.map.depth / 2" :width="space.map.width" :height="space.map.depth"
        :opacity="space.map.opacity" preserveAspectRatio="none" />
      <g v-for="column in visibleColumns" :key="column.id"
        :transform="`translate(${column.x} ${-column.y}) rotate(${-column.rotation})`">
        <path :d="columnSvgPath(column)" :fill="column.fillColor" :stroke="column.strokeColor"
          :opacity="column.opacity" :stroke-width="Math.max(1, column.strokeWidth)" vector-effect="non-scaling-stroke" />
        <title>{{ column.name }}</title>
      </g>
      <g v-for="item in visiblePoiLabels" :key="item.binding.id" :transform="`translate(${item.point.x} ${-item.point.y})`">
        <g class="spatial-twin__poi-label">
          <rect :x="item.label.x" :y="item.label.y" :width="item.label.width" :height="item.label.height"
            :rx="pointRadius * 0.8" vector-effect="non-scaling-stroke" />
          <text :font-size="item.label.fontSize">
            <tspan v-for="(line, index) in item.label.lines" :key="index"
              :x="item.label.x + item.label.padding"
              :y="item.label.y + item.label.padding + item.label.fontSize * 0.85 + index * item.label.lineHeight">{{ line }}</tspan>
          </text>
        </g>
        <circle :r="pointRadius * 1.5" fill="#fff" stroke="#206f84" :stroke-width="pointRadius * 0.2" />
        <circle :r="pointRadius" :fill="item.device.status === 'offline' ? '#e45d53' : '#0aa48b'" />
        <title>{{ item.device.name || item.binding.label }}</title>
      </g>
      <g v-for="(item, index) in visibleEvents" :key="`${eventKey(item.event, 0)}-${index}`" class="spatial-twin__event-point"
        :class="[`is-${item.event.level || 'low'}`, { 'is-active': eventKey(item.event, 0) === selectedKey }]"
        :transform="`translate(${item.point.x} ${-item.point.y})`" role="button" tabindex="0"
        :aria-label="`查看事件：${eventTitle(item.event)}`" @click="emit('select', item.event)" @keydown.enter="emit('select', item.event)">
        <circle class="spatial-twin__event-halo" :r="pointRadius * 2.4" />
        <circle class="spatial-twin__event-core" :r="pointRadius * 1.35" :stroke-width="pointRadius * 0.38" />
        <text v-if="eventKey(item.event, 0) === selectedKey" text-anchor="middle" :y="-pointRadius * 3.2"
          :font-size="pointRadius * 2.2">{{ eventTitle(item.event).slice(0, 12) }}</text>
        <title>{{ eventSource(item.event) }} · {{ eventTitle(item.event) }} · {{ eventTime(item.event) }}</title>
      </g>
    </svg>

    <img v-else
      class="spatial-twin__image"
      :src="overviewImage"
      alt="建筑空间示意图"
    />

    <div v-if="space && !space.map" class="spatial-twin__map-empty">该空间尚未上传地图，当前显示图层对象</div>
    <div v-if="!space" class="spatial-twin__scan" aria-hidden="true"></div>

    <div class="spatial-twin__slot spatial-twin__slot--left"><slot name="left" /></div>
    <div class="spatial-twin__slot spatial-twin__slot--right"><slot name="right" /></div>
    <div class="spatial-twin__slot spatial-twin__slot--bottom"><slot name="bottom" /></div>

    <div class="spatial-twin__disclaimer">
      <i aria-hidden="true"></i>
      {{ sourceText }}
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import overviewImage from '../assets/spatial/building-overview-topdown.png'
import { columnSvgPath, containsLayer, deviceIdForEvent, layerTreeRows, locateEvents, placedDevices, poiLabelLayout, visibleSpatialColumns } from '../spatial-view'

const props = defineProps({
  events: { type: Array, default: () => [] },
  selected: { type: Object, default: null },
  mode: { type: String, default: 'overview' },
  space: { type: Object, default: null },
  source: { type: String, default: 'loading' },
  selectedLayerId: { type: String, default: '' }
})

const emit = defineEmits(['select', 'select-layer'])
const layerRows = computed(() => props.space ? layerTreeRows(props.space) : [])
const expandedLayers = ref(new Set())
const shownLayerRows = computed(() => props.space ? layerTreeRows(props.space, expandedLayers.value) : [])
const activeLayerId = computed(() => layerRows.value.some(row => row.layer.id === props.selectedLayerId)
  ? props.selectedLayerId : layerRows.value.find(row => row.visible)?.layer.id || layerRows.value[0]?.layer.id || '')
const activeLayerName = computed(() => layerRows.value.find(row => row.layer.id === activeLayerId.value)?.layer.name || '全部图层')
const viewBox = computed(() => props.space
  ? `${-props.space.bounds.width / 2} ${-props.space.bounds.depth / 2} ${props.space.bounds.width} ${props.space.bounds.depth}` : '')
const pointRadius = computed(() => props.space ? Math.max(0.4, Math.max(props.space.bounds.width, props.space.bounds.depth) / 170) : 0.6)
const visibleColumns = computed(() => props.space ? visibleSpatialColumns(props.space, activeLayerId.value) : [])
const visibleDevices = computed(() => props.space ? placedDevices(props.space, activeLayerId.value) : [])
const visiblePoiLabels = computed(() => props.space ? visibleDevices.value.map(item => ({ ...item,
  label: poiLabelLayout(item.device.name || item.binding.label, item.point, props.space.bounds, pointRadius.value)
})) : [])
const allDevices = computed(() => props.space ? placedDevices(props.space) : [])
const locatedEvents = computed(() => props.space ? locateEvents(props.space, props.events, activeLayerId.value) : [])
const visibleEvents = computed(() => {
  const selected = props.selected && locatedEvents.value.find(item => eventKey(item.event, 0) === selectedKey.value)
  const others = locatedEvents.value.filter(item => item !== selected)
  return selected ? [...others, selected] : others
})
const unlocatedCount = computed(() => {
  const ids = new Set(allDevices.value.map(item => item.device.deviceId))
  return props.events.filter(event => !ids.has(deviceIdForEvent(event))).length
})
const selectedKey = computed(() => eventKey(props.selected, 0))
watch(() => props.space?.id, () => {
  expandedLayers.value = new Set(layerRows.value.filter(row => row.hasChildren).map(row => row.layer.id))
}, { immediate: true })
watch(() => [props.selectedLayerId, props.space?.id], () => {
  if (!props.space || !props.selectedLayerId) return
  const next = new Set(expandedLayers.value)
  const seen = new Set()
  let parentId = props.space.layers.find(layer => layer.id === props.selectedLayerId)?.parentId
  while (parentId && !seen.has(parentId)) {
    next.add(parentId)
    seen.add(parentId)
    parentId = props.space.layers.find(layer => layer.id === parentId)?.parentId
  }
  expandedLayers.value = next
}, { immediate: true })
const sourceText = computed(() => {
  if (!props.space) return props.source === 'loading' ? '正在同步空间图' : props.source === 'error' ? '空间图加载失败，请刷新重试' : '空间尚未配置 · 当前为示意图'
  const source = props.source === 'published' ? '已发布空间（草稿不可用）' : '已保存空间'
  return `${props.space.name} · ${source}${unlocatedCount.value ? ` · ${unlocatedCount.value} 条事件未绑定点位` : ''}`
})
watch(() => [props.selected, props.space, activeLayerId.value], () => {
  if (!props.space || !props.selected) return
  const deviceId = deviceIdForEvent(props.selected)
  const item = deviceId && allDevices.value.find(value => value.device.deviceId === deviceId)
  if (item?.layerId && !containsLayer(props.space, activeLayerId.value, item.layerId)) emit('select-layer', item.layerId)
})

function toggleLayer(id) {
  const next = new Set(expandedLayers.value)
  if (next.has(id)) {
    next.delete(id)
    if (props.space && id !== activeLayerId.value && containsLayer(props.space, id, activeLayerId.value)) emit('select-layer', id)
  } else next.add(id)
  expandedLayers.value = next
}

function parseExtend(event) {
  if (!event?.extend) return {}
  if (typeof event.extend === 'object') return event.extend
  try {
    return JSON.parse(event.extend) || {}
  } catch (_error) {
    return {}
  }
}

function eventKey(event, index) {
  if (!event) return ''
  return event?.eventId || event?.id || `${event?.eventTime || 'event'}-${index}`
}

function eventTitle(event) {
  const extend = parseExtend(event)
  return event?.eventName || event?.classifyName || extend.eventName || '待确认事件'
}

function eventSource(event) {
  const extend = parseExtend(event)
  return event?.deviceName || extend.doorName || extend.devName || '空间位置待关联'
}

function eventTime(event) {
  const raw = event?.eventTime || parseExtend(event).eventTime
  if (!raw) return '时间待同步'
  const text = String(raw)
  return text.length > 16 ? text.slice(11, 16) : text
}

</script>

<style scoped lang="scss">
.spatial-twin {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background:
    radial-gradient(circle at 52% 45%, rgba(184, 222, 255, 0.22), transparent 43%),
    linear-gradient(150deg, #fbfdff 0%, #eef4fb 100%);
  isolation: isolate;
}

.spatial-twin::before {
  position: absolute;
  inset: 0;
  z-index: -1;
  background-image:
    linear-gradient(rgba(88, 141, 196, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(88, 141, 196, 0.035) 1px, transparent 1px);
  background-size: 28px 28px;
  content: '';
  mask-image: radial-gradient(circle at 50% 50%, #000 12%, transparent 78%);
}

.spatial-twin__image {
  position: absolute;
  z-index: 1;
  top: 50%;
  left: 51%;
  width: min(91%, 1080px);
  height: auto;
  max-height: 94%;
  object-fit: contain;
  mix-blend-mode: multiply;
  filter: saturate(0.96) contrast(1.01) drop-shadow(0 24px 28px rgba(42, 75, 112, 0.1));
  transform: translate(-50%, -49%);
  user-select: none;
}

.spatial-twin__model {
  position: absolute;
  z-index: 1;
  inset: 8% 5% 8% 8%;
  width: 87%;
  height: 84%;
  overflow: visible;
  filter: drop-shadow(0 20px 25px rgba(42, 75, 112, 0.12));
}

.spatial-twin__map-empty {
  position: absolute;
  z-index: 4;
  left: 50%;
  bottom: 16%;
  max-width: 70%;
  padding: 7px 12px;
  border: 1px solid #d5e4f5;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.88);
  color: #60758d;
  font-size: 12px;
  text-align: center;
  transform: translateX(-50%);
}

.spatial-twin__scan {
  position: absolute;
  z-index: 2;
  left: 17%;
  right: 8%;
  top: 45%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(33, 193, 225, 0.5), transparent);
  box-shadow: 0 0 18px rgba(33, 193, 225, 0.2);
  opacity: 0.58;
  pointer-events: none;
}

.spatial-twin__floor {
  position: absolute;
  z-index: 6;
  top: 50%;
  left: 10px;
  width: clamp(76px, 9%, 104px);
  max-height: 72%;
  display: flex;
  flex-direction: column;
  gap: 3px;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-color: rgba(133, 158, 187, 0.45) transparent;
  scrollbar-width: thin;
  transform: translateY(-50%);

  .spatial-twin__floor-row {
    min-height: 28px;
    display: flex;
    align-items: center;
  }

  .spatial-twin__floor-row.is-muted .spatial-twin__floor-name { opacity: 0.48; }

  .spatial-twin__floor-toggle,
  .spatial-twin__floor-spacer {
    width: 16px;
    height: 26px;
    flex: 0 0 16px;
  }

  .spatial-twin__floor-toggle {
    display: grid;
    place-items: center;
    padding: 0;
    border: 0;
    background: transparent;
    color: #506984;
    cursor: pointer;
  }

  .spatial-twin__floor-toggle::before {
    width: 5px;
    height: 5px;
    border-right: 1.5px solid currentColor;
    border-bottom: 1.5px solid currentColor;
    content: '';
    transform: rotate(-45deg);
  }

  .spatial-twin__floor-toggle.is-expanded::before { transform: rotate(45deg); }

  .spatial-twin__floor-name {
    min-height: 28px;
    min-width: 0;
    flex: 1 1 auto;
    overflow: hidden;
    padding: 0 5px;
    border: 0;
    border-left: 2px solid transparent;
    border-radius: 4px 6px 6px 4px;
    background: transparent;
    color: #506984;
    font: inherit;
    font-size: 11px;
    font-weight: 600;
    text-align: left;
    text-overflow: ellipsis;
    white-space: nowrap;
    text-shadow: 0 1px 3px rgba(255, 255, 255, 0.95);
    cursor: pointer;
    transition: background 0.15s ease, color 0.15s ease;
  }

  .spatial-twin__floor-name:hover { background: rgba(255, 255, 255, 0.72); color: #1268f3; }

  .spatial-twin__floor-name.is-active {
    border-left-color: #2875e6;
    background: rgba(255, 255, 255, 0.88);
    color: #1564ce;
    text-shadow: none;
    box-shadow: 0 2px 9px rgba(70, 104, 146, 0.1);
  }

  button:focus-visible { outline: 2px solid #2875e6; outline-offset: -2px; }
}

.spatial-twin__poi-label {
  pointer-events: none;

  rect {
    fill: rgba(255, 255, 255, 0.94);
    stroke: rgba(67, 110, 148, 0.45);
    stroke-width: 1;
  }

  text {
    fill: #214463;
    font-weight: 700;
    paint-order: stroke;
    stroke: rgba(255, 255, 255, 0.9);
    stroke-width: 0.12;
  }
}

.spatial-twin__event-point {
  cursor: pointer;

  .spatial-twin__event-halo { fill: rgba(67, 137, 255, 0.2); }
  .spatial-twin__event-core { fill: #4389ff; stroke: #fff; }
  &.is-middle .spatial-twin__event-halo { fill: rgba(244, 154, 50, 0.22); }
  &.is-middle .spatial-twin__event-core { fill: #f49a32; }
  &.is-high .spatial-twin__event-halo { fill: rgba(236, 88, 82, 0.24); }
  &.is-high .spatial-twin__event-core { fill: #ec5852; }
  &.is-active .spatial-twin__event-halo { stroke: #fff; stroke-width: 0.5; }

  text {
    fill: #233f5c;
    font-weight: 700;
    paint-order: stroke;
    stroke: #fff;
    stroke-width: 0.7;
    pointer-events: none;
  }

  &:focus-visible { outline: 2px solid #1268f3; }
}

.spatial-twin__slot {
  position: absolute;
  z-index: 5;
}

.spatial-twin__slot--left { top: 18%; left: 9%; }
.spatial-twin__slot--right { top: 19%; right: 4%; width: 186px; }
.spatial-twin__slot--bottom { right: 14%; bottom: 8%; width: 186px; }

.spatial-twin__disclaimer {
  position: absolute;
  z-index: 8;
  right: 12px;
  bottom: 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  border: 1px solid rgba(214, 224, 235, 0.82);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.75);
  color: #60758d;
  font-size: 13px;
  backdrop-filter: blur(8px);

  i {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: #33c1d9;
    box-shadow: 0 0 0 3px rgba(51, 193, 217, 0.12);
  }
}

@media (max-width: 1180px) {
  .spatial-twin__slot--right { right: 2%; }
  .spatial-twin__floor { width: 76px; }
}
</style>
