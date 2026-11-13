<template>
  <AppWindow title="事件中枢" @close="$emit('close')" @minimize="$emit('minimize')">
    <section class="event-cockpit" :class="{ 'is-overview': activeMenu === 'overview' }" aria-labelledby="event-cockpit-title">
      <header class="event-cockpit__bar">
        <div class="event-cockpit__identity">
          <button type="button" class="event-cockpit__back" aria-label="返回建筑态势" @click="$emit('close')">
            <el-icon><Back /></el-icon>
          </button>
          <div>
            <span>门诊楼 · 6F</span>
            <h2 id="event-cockpit-title">事件中枢</h2>
          </div>
        </div>

        <nav class="event-cockpit__tabs" aria-label="事件中枢视图">
          <button
            v-for="item in menus"
            :key="item.key"
            type="button"
            :class="{ 'is-active': activeMenu === item.key }"
            :aria-current="activeMenu === item.key ? 'page' : undefined"
            @click="activeMenu = item.key"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            {{ item.name }}
          </button>
        </nav>

        <div class="event-cockpit__actions">
          <span class="event-cockpit__live"><i aria-hidden="true"></i>数据已接入</span>
          <button type="button" class="event-cockpit__refresh" :disabled="loading || statsLoading" @click="refresh">
            <el-icon><Refresh /></el-icon>
            刷新
          </button>
        </div>
      </header>

      <EventRuleView v-if="activeMenu === 'rule'" class="event-cockpit__module" />

      <section v-else-if="activeMenu === 'follow'" class="event-follow" aria-labelledby="event-list-title">
        <header class="event-follow__head">
          <div>
            <h3 id="event-list-title">事件跟进</h3>
          </div>
          <div class="event-follow__filters">
            <el-select v-model="query.handleState" placeholder="全部状态" clearable @change="search" @clear="search">
              <el-option v-for="state in HANDLE_STATES" :key="state.value" :label="state.label" :value="state.value" />
            </el-select>
            <el-select v-model="query.level" placeholder="全部级别" clearable @change="search" @clear="search">
              <el-option v-for="level in LEVELS" :key="level.value" :label="level.label" :value="level.value" />
            </el-select>
            <el-input
              v-model="query.eventName"
              class="event-follow__search"
              placeholder="搜索事件名称或来源"
              clearable
              :prefix-icon="Search"
              @keyup.enter="search"
              @clear="search"
            />
          </div>
        </header>

        <div class="event-follow__table">
          <el-table v-loading="loading" :data="list" height="100%">
            <el-table-column label="发生时间" width="190" show-overflow-tooltip>
              <template #default="{ row }">{{ eventTimeText(row) }}</template>
            </el-table-column>
            <el-table-column label="事件类型" width="150" show-overflow-tooltip>
              <template #default="{ row }">{{ eventTypeText(row) }}</template>
            </el-table-column>
            <el-table-column label="级别" width="90" align="center">
              <template #default="{ row }">
                <span class="event-level" :class="`is-${row.level || 'low'}`">{{ levelLabel(row.level) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="事件来源" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ eventSourceText(row) }}</template>
            </el-table-column>
            <el-table-column label="处理状态" width="110" align="center">
              <template #default="{ row }">
                <span class="event-state" :class="`is-${row.handlerState || 'unknown'}`">{{ stateLabel(row.handlerState) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.handlerState === 'apply'" link type="primary" :icon="Promotion" @click="onDispatch">分发</el-button>
                <el-button link type="info" :icon="Document" @click="onDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="query.pageNum"
          v-model:limit="query.pageSize"
          :page-sizes="[10, 20, 30, 50]"
          @pagination="loadEvents"
        />
      </section>

      <template v-else>
        <div class="event-kpis" v-loading="statsLoading" aria-label="事件态势指标">
          <button type="button" class="event-kpi is-danger" @click="selectFirstByLevel('high')">
            <span class="event-kpi__icon"><el-icon><WarningFilled /></el-icon></span>
            <span class="event-kpi__copy"><small>紧急事件</small><strong>{{ number(stats.highNum) }}</strong><em>未处理 {{ number(stats.applyHighNum) }} · 处理中 {{ number(stats.pendingHighNum) }}</em></span>
            <span class="event-kpi__trend">P1</span>
          </button>
          <button type="button" class="event-kpi is-warning" @click="selectFirstByLevel('middle')">
            <span class="event-kpi__icon"><el-icon><Bell /></el-icon></span>
            <span class="event-kpi__copy"><small>告警事件</small><strong>{{ number(stats.middleNum) }}</strong><em>未处理 {{ number(stats.applyMiddleNum) }} · 处理中 {{ number(stats.pendingMiddleNum) }}</em></span>
            <span class="event-kpi__trend">P2</span>
          </button>
          <button type="button" class="event-kpi is-info" @click="selectFirstByLevel('low')">
            <span class="event-kpi__icon"><el-icon><InfoFilled /></el-icon></span>
            <span class="event-kpi__copy"><small>普通事件</small><strong>{{ number(stats.lowNum) }}</strong><em>未处理 {{ number(stats.applyLowNum) }} · 处理中 {{ number(stats.pendingLowNum) }}</em></span>
            <span class="event-kpi__trend">P3</span>
          </button>
          <button type="button" class="event-kpi is-pending" @click="activeMenu = 'follow'">
            <span class="event-kpi__icon"><el-icon><Timer /></el-icon></span>
            <span class="event-kpi__copy"><small>待响应事件</small><strong>{{ pendingTotal }}</strong><em>事件总量 {{ eventTotal }} · 查看跟进</em></span>
            <span class="event-kpi__trend"><el-icon><Right /></el-icon></span>
          </button>
        </div>

        <div class="event-cockpit__body">
          <section class="event-spatial" aria-labelledby="event-spatial-title">
            <header class="event-spatial__head">
              <div class="event-spatial__site">
                <span>空间事件态势</span>
                <h3 id="event-spatial-title">{{ spatialTitle }}</h3>
              </div>
              <div class="event-spatial__legend" aria-label="空间图层">
                <span class="is-active"><el-icon><DataAnalysis /></el-icon>事件</span>
                <span><el-icon><Connection /></el-icon>设备</span>
                <span><el-icon><MostlyCloudy /></el-icon>环境</span>
                <span><el-icon><Cpu /></el-icon>能耗</span>
              </div>
            </header>

            <div class="event-spatial__canvas">
              <SpatialTwin :events="list" :selected="selectedEvent" :space="spatialSpace" :source="spatialSource"
                :selected-layer-id="selectedLayerId" mode="event" @select="selectEvent" @select-layer="emit('select-layer', $event)" />

              <aside class="key-events" aria-labelledby="key-events-title">
                <header>
                  <div><strong id="key-events-title">关键事件</strong><span>{{ keyEvents.length }}</span></div>
                  <button type="button" @click="activeMenu = 'follow'">查看全部</button>
                </header>

                <div v-if="keyEvents.length" class="key-events__list">
                  <button
                    v-for="event in keyEvents"
                    :key="eventIdText(event)"
                    type="button"
                    :class="[`is-${event.level || 'low'}`, { 'is-active': event === selectedEvent }]"
                    @click="selectEvent(event)"
                  >
                    <span class="key-events__priority">{{ priorityLabel(event.level) }}</span>
                    <strong>{{ eventTypeText(event) }}</strong>
                    <small>{{ shortTime(eventTimeText(event)) }} · {{ eventSourceText(event) }}</small>
                    <em>{{ stateLabel(event.handlerState) }}</em>
                    <el-icon><ArrowRight /></el-icon>
                  </button>
                </div>
                <div v-else class="key-events__empty">
                  <el-icon><CircleCheckFilled /></el-icon>
                  <strong>当前没有事件</strong>
                  <span>新事件接入后将在此聚合</span>
                </div>
              </aside>
            </div>

            <footer class="event-trajectory" aria-labelledby="event-trajectory-title">
              <div class="event-trajectory__label">
                <span>闭环追踪</span>
                <strong id="event-trajectory-title">运行轨迹</strong>
              </div>
              <div class="event-trajectory__steps">
                <div v-for="(step, index) in processSteps" :key="step.key" class="trajectory-step" :class="step.state">
                  <span><el-icon><component :is="step.icon" /></el-icon></span>
                  <div><strong>{{ step.name }}</strong><small>{{ step.copy }}</small></div>
                  <i v-if="index < processSteps.length - 1" aria-hidden="true"></i>
                </div>
              </div>
            </footer>
          </section>

          <aside class="event-agent" aria-labelledby="event-agent-title">
            <header class="event-agent__head">
              <div>
                <h3 id="event-agent-title">事件智能体</h3>
              </div>
              <span class="event-agent__status"><i aria-hidden="true"></i>规则研判</span>
            </header>

            <template v-if="selectedEvent">
              <section class="agent-section agent-issue">
                <span class="agent-section__label">发现问题</span>
                <div class="agent-issue__title">
                  <span :class="`is-${selectedEvent.level || 'low'}`"><el-icon><WarningFilled /></el-icon></span>
                  <div><small>{{ priorityLabel(selectedEvent.level) }} · {{ stateLabel(selectedEvent.handlerState) }}</small><strong>{{ eventTypeText(selectedEvent) }}</strong></div>
                </div>
                <p>{{ eventContent(selectedEvent) }}</p>
              </section>

              <section class="agent-section agent-evidence">
                <span class="agent-section__label">证据</span>
                <ul>
                  <li><span>事件来源</span><strong>{{ eventSourceText(selectedEvent) }}</strong></li>
                  <li><span>发生时间</span><strong>{{ eventTimeText(selectedEvent) }}</strong></li>
                  <li><span>事件级别</span><strong>{{ levelLabel(selectedEvent.level) }}</strong></li>
                  <li><span>处理状态</span><strong>{{ stateLabel(selectedEvent.handlerState) }}</strong></li>
                </ul>
              </section>

              <section class="agent-section agent-judgement">
                <span class="agent-section__label">规则研判</span>
                <div class="agent-judgement__card">
                  <div><span><el-icon><DataAnalysis /></el-icon></span><strong>{{ judgementTitle }}</strong></div>
                  <p>{{ judgementCopy }}</p>
                  <div class="agent-progress"><span>数据完整度</span><strong>{{ dataCompleteness }}%</strong><i><b :style="{ width: `${dataCompleteness}%` }"></b></i></div>
                </div>
              </section>

              <section class="agent-section agent-action">
                <span class="agent-section__label">建议行动 · 需人工复核</span>
                <div>
                  <strong>{{ recommendedAction.title }}</strong>
                  <p>{{ recommendedAction.copy }}</p>
                </div>
                <div class="agent-action__buttons">
                  <button type="button" class="is-secondary" @click="activeMenu = 'follow'">进入跟进</button>
                  <button type="button" class="is-primary" @click="onDetail(selectedEvent)">审核详情</button>
                </div>
              </section>

              <section class="agent-section agent-related">
                <span class="agent-section__label">关联对象</span>
                <button type="button" @click="onDetail(selectedEvent)">
                  <span><el-icon><Connection /></el-icon></span>
                  <div><strong>{{ eventSourceText(selectedEvent) }}</strong><small>事件源对象 · {{ eventIdText(selectedEvent) }}</small></div>
                  <el-icon><ArrowRight /></el-icon>
                </button>
              </section>
            </template>

            <div v-else class="event-agent__empty">
              <span><el-icon><CircleCheckFilled /></el-icon></span>
              <strong>运行稳定</strong>
              <p>当前没有可研判事件，智能体将持续监听事件总线。</p>
            </div>
          </aside>
        </div>
      </template>
    </section>

    <el-dialog v-model="dispatchVisible" title="事件分发" width="440px" align-center append-to-body :close-on-click-modal="false">
      <el-form label-width="84px" @submit.prevent="confirmDispatch">
        <el-form-item label="分发系统">
          <el-select v-model="dispatchSystem" class="event-dispatch__system" aria-label="分发系统">
            <el-option label="数据中心" value="data-center" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDispatch">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" width="760px" align-center append-to-body class="event-detail-dialog" title="事件详情">
      <div v-if="detailRow" class="event-detail">
        <section class="event-detail__hero">
          <div>
<!--            <span>事件 ID · {{ eventIdText(detailRow) }}</span>-->
            <h3>{{ eventTypeText(detailRow) }}</h3>
            <p>{{ eventContent(detailRow) }}</p>
          </div>
          <div>
            <span class="event-level" :class="`is-${detailRow.level || 'low'}`">{{ levelLabel(detailRow.level) }}</span>
            <span class="event-state" :class="`is-${detailRow.handlerState || 'unknown'}`">{{ stateLabel(detailRow.handlerState) }}</span>
          </div>
        </section>

        <div class="event-detail__grid">
          <div><span>事件来源</span><strong>{{ eventSourceText(detailRow) }}</strong></div>
          <div><span>事件类型</span><strong>{{ eventClassifyText(detailRow) }}</strong></div>
          <div><span>事件信息</span><strong>{{ eventNameText(detailRow) }}</strong></div>
          <div><span>发生时间</span><strong>{{ eventTimeText(detailRow) }}</strong></div>
          <div><span>处理人员</span><strong>{{ formatValue(detailRow.handlerBy) }}</strong></div>
        </div>

        <section class="event-detail__section">
          <div v-if="detailExtendItems.length" class="event-detail__kv">
            <div v-for="item in detailExtendItems" :key="item.label"><span>{{ item.label }}</span><strong>{{ item.value }}</strong></div>
          </div>
          <div v-else class="event-detail__empty">暂无扩展信息</div>
        </section>
      </div>
      <template #footer><el-button type="primary" @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </AppWindow>
</template>

<script setup>
import { computed, markRaw, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Back,
  Bell,
  CircleCheck,
  CircleCheckFilled,
  Connection,
  Cpu,
  DataAnalysis,
  Document,
  InfoFilled,
  Lock,
  MostlyCloudy,
  Promotion,
  Refresh,
  Right,
  Search,
  Timer,
  User,
  WarningFilled
} from '@element-plus/icons-vue'
import AppWindow from '../../components/AppWindow.vue'
import SpatialTwin from '../../components/SpatialTwin.vue'
import { layerTreeRows } from '../../spatial-view'
import EventRuleView from './EventRuleView.vue'
import { getEventPage, getEventOverview } from '@/api/build/event'

const props = defineProps({
  spatialSpace: { type: Object, default: null },
  spatialSource: { type: String, default: 'loading' },
  selectedLayerId: { type: String, default: '' }
})
const emit = defineEmits(['close', 'minimize', 'select-layer'])

const menus = [
  { key: 'overview', name: '事件总览', icon: markRaw(DataAnalysis) },
  { key: 'rule', name: '事件规则', icon: markRaw(Document) },
  { key: 'follow', name: '事件跟进', icon: markRaw(User) }
]
const LEVELS = [
  { value: 'high', label: '高' },
  { value: 'middle', label: '中' },
  { value: 'low', label: '低' }
]
const HANDLE_STATES = [
  { value: 'apply', label: '待处理' },
  { value: 'pending', label: '处理中' },
  { value: 'completed', label: '已完成' }
]

const activeMenu = ref('overview')
const stats = reactive({})
const statsLoading = ref(false)
const list = ref([])
const total = ref(0)
const loading = ref(false)
const selectedEvent = ref(null)
const spatialTitle = computed(() => {
  if (!props.spatialSpace) return props.spatialSource === 'loading' ? '正在加载空间' : '空间尚未配置'
  const layers = layerTreeRows(props.spatialSpace)
  const layer = layers.find(item => item.layer.id === props.selectedLayerId)?.layer ||
    layers.find(item => item.visible)?.layer || layers[0]?.layer
  return `${props.spatialSpace.name} · ${layer?.name || '暂无图层'}`
})
const detailVisible = ref(false)
const detailRow = ref(null)
const dispatchVisible = ref(false)
const dispatchSystem = ref('data-center')
const query = reactive({ pageNum: 1, pageSize: 10, eventName: '', level: undefined, handleState: undefined })

const keyEvents = computed(() => [...list.value].sort((a, b) => levelRank(b.level) - levelRank(a.level)).slice(0, 3))
const pendingTotal = computed(() => number(stats.applyHighNum) + number(stats.applyMiddleNum) + number(stats.applyLowNum))
const eventTotal = computed(() => number(stats.highNum) + number(stats.middleNum) + number(stats.lowNum))

const dataCompleteness = computed(() => {
  if (!selectedEvent.value) return 0
  const values = [
    eventIdText(selectedEvent.value),
    eventTypeText(selectedEvent.value),
    selectedEvent.value.level,
    eventTimeText(selectedEvent.value),
    eventSourceText(selectedEvent.value),
    selectedEvent.value.handlerState
  ]
  const available = values.filter(value => value && value !== '—').length
  return Math.round((available / values.length) * 100)
})

const judgementTitle = computed(() => {
  if (!selectedEvent.value) return '等待事件'
  if (selectedEvent.value.level === 'high') return '高优先级事件，需要立即复核'
  if (selectedEvent.value.handlerState === 'apply') return '事件尚未分发，建议进入跟进'
  if (selectedEvent.value.handlerState === 'pending') return '事件正在处理中，持续跟踪'
  return '事件记录已形成闭环'
})

const judgementCopy = computed(() => {
  if (!selectedEvent.value) return '事件总线暂无可研判记录。'
  return `依据事件级别“${levelLabel(selectedEvent.value.level)}”和状态“${stateLabel(selectedEvent.value.handlerState)}”进行规则判断，未调用外部 AI 推理。`
})

const recommendedAction = computed(() => {
  if (!selectedEvent.value) return { title: '持续监听', copy: '等待事件总线推送新记录。' }
  if (selectedEvent.value.handlerState === 'apply') return { title: '核对事件来源并进入分发', copy: '先确认事件源对象与扩展字段，再由人工选择处理人。' }
  if (selectedEvent.value.handlerState === 'pending') return { title: '跟踪处理进度', copy: '检查处理人、处置记录与现场反馈是否完整。' }
  return { title: '复核闭环记录', copy: '确认处理结果与事件扩展信息已归档。' }
})

const processSteps = computed(() => {
  const state = selectedEvent.value?.handlerState
  const sensed = Boolean(selectedEvent.value)
  const analysed = sensed && dataCompleteness.value >= 50
  return [
    { key: 'sense', name: '已感知', copy: sensed ? shortTime(eventTimeText(selectedEvent.value)) : '等待事件', icon: markRaw(Bell), state: sensed ? 'is-complete' : 'is-pending' },
    { key: 'analyse', name: '已解析', copy: analysed ? '事件字段已聚合' : '等待数据', icon: markRaw(DataAnalysis), state: analysed ? 'is-complete' : 'is-pending' },
    { key: 'review', name: '待审核', copy: !sensed ? '等待事件' : state === 'apply' ? '需要人工复核' : '审核记录已更新', icon: markRaw(Lock), state: state === 'apply' ? 'is-current' : sensed ? 'is-complete' : 'is-pending' },
    { key: 'execute', name: '待执行', copy: state === 'pending' ? '正在处理中' : state === 'completed' ? '处置已完成' : '等待审核', icon: markRaw(User), state: state === 'pending' ? 'is-current' : state === 'completed' ? 'is-complete' : 'is-pending' },
    { key: 'confirm', name: '待确认', copy: state === 'completed' ? '闭环已确认' : '等待处置结果', icon: markRaw(CircleCheck), state: state === 'completed' ? 'is-complete' : 'is-pending' }
  ]
})

const detailExtend = computed(() => parseJsonObject(detailRow.value?.extend))
const detailExtendItems = computed(() => {
  const extend = detailExtend.value
  return [
    { label: '门禁点', value: extend.doorName },
    { label: '门禁编码', value: extend.doorIndexCode },
    { label: '设备名称', value: extend.devName },
    { label: '设备编码', value: extend.devIndexCode },
    { label: '读卡器', value: extend.readerDevName },
    { label: '人员姓名', value: extend.personName },
    { label: '人员 ID', value: extend.personId },
    { label: '卡号', value: extend.cardNo },
    { label: '接收时间', value: formatDateTime(extend.receiveTime) },
    { label: '事件类型码', value: extend.eventType }
  ].filter(item => item.value !== undefined && item.value !== null && item.value !== '')
})

function loadOverview() {
  statsLoading.value = true
  getEventOverview()
    .then(res => Object.assign(stats, res.data || {}))
    .catch(() => {})
    .finally(() => { statsLoading.value = false })
}

function loadEvents() {
  loading.value = true
  const params = { pageNum: query.pageNum, pageSize: query.pageSize }
  if (query.eventName.trim()) params.eventName = query.eventName.trim()
  if (query.level) params.level = query.level
  if (query.handleState) params.handleState = query.handleState
  getEventPage(params)
    .then(res => {
      list.value = res.rows || []
      total.value = res.total || 0
      if (!selectedEvent.value || !list.value.includes(selectedEvent.value)) selectedEvent.value = list.value[0] || null
    })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function search() {
  query.pageNum = 1
  loadEvents()
}

function refresh() {
  loadOverview()
  loadEvents()
}

function selectEvent(event) {
  selectedEvent.value = event
}

function selectFirstByLevel(level) {
  const event = list.value.find(item => item.level === level)
  if (event) selectedEvent.value = event
}

function onDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

function onDispatch() {
  dispatchSystem.value = 'data-center'
  dispatchVisible.value = true
}

function confirmDispatch() {
  dispatchVisible.value = false
  ElMessage.success('分发成功')
}

function parseJsonObject(value) {
  if (!value) return {}
  if (typeof value === 'object') return value
  try {
    const result = JSON.parse(value)
    return result && typeof result === 'object' && !Array.isArray(result) ? result : {}
  } catch (_error) {
    return {}
  }
}

function rowExtend(row) {
  return parseJsonObject(row?.extend)
}

function number(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function formatValue(value) {
  return value === undefined || value === null || value === '' ? '—' : value
}

function levelRank(level) {
  return { high: 3, middle: 2, low: 1 }[level] || 0
}

function priorityLabel(level) {
  return { high: 'P1', middle: 'P2', low: 'P3' }[level] || 'P3'
}

function levelLabel(value) {
  return LEVELS.find(item => item.value === value)?.label || formatValue(value)
}

function stateLabel(value) {
  return HANDLE_STATES.find(item => item.value === value)?.label || formatValue(value)
}

function eventIdText(row) {
  const extend = rowExtend(row)
  return formatValue(row?.eventId || extend.eventId || row?.id)
}

function eventTypeText(row) {
  const extend = rowExtend(row)
  return formatValue(row?.classifyName || row?.classify || row?.eventName || extend.eventName || extend.eventType)
}

function eventClassifyText(row) {
  const extend = rowExtend(row)
  return formatValue(row?.classifyName || row?.classify || extend.eventType)
}

function eventNameText(row) {
  const extend = rowExtend(row)
  return formatValue(row?.eventName || extend.eventName)
}

function eventTimeText(row) {
  const extend = rowExtend(row)
  return formatValue(row?.eventTime || formatDateTime(extend.eventTime))
}

function eventSourceText(row) {
  const extend = rowExtend(row)
  return formatValue(row?.deviceName || extend.devName || extend.doorName || row?.deviceId || row?.driveId)
}

function eventContent(row) {
  const extend = rowExtend(row)
  const source = eventSourceText(row)
  const event = row?.eventName || extend.eventName || row?.classifyName
  if (source !== '—' && event) return `${source} 发生 ${event}`
  return event || '事件内容待事件源补充'
}

function shortTime(value) {
  if (!value || value === '—') return '时间待同步'
  const text = String(value)
  return text.length >= 16 ? text.slice(11, 16) : text
}

function formatDateTime(value) {
  if (!value) return value
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = numberValue => String(numberValue).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

onMounted(() => {
  loadOverview()
  loadEvents()
})
</script>

<style scoped lang="scss" src="./cockpit.scss"></style>
