<template>
  <section class="cockpit" aria-labelledby="cockpit-title">
    <h1 id="cockpit-title" class="sr-only">具身建筑智能运行态势</h1>

    <div class="kpi-strip" aria-label="建筑关键运行指标">
      <button type="button" class="kpi-card" @click="openApp('device')">
        <span class="kpi-card__icon is-blue"><el-icon><Connection /></el-icon></span>
        <span class="kpi-card__copy"><small>在线设备</small><strong>{{ value(deviceOverview.onlineNum) }}<i>/ {{ value(deviceOverview.totalNum) }}</i></strong><em>在线率 {{ onlineRate }}</em></span>
      </button>
      <button type="button" class="kpi-card" @click="openApp('resource')">
        <span class="kpi-card__icon is-cyan"><el-icon><MostlyCloudy /></el-icon></span>
        <span class="kpi-card__copy"><small>环境感知</small><strong>{{ displayEnvironment.temperature }}<i>°C</i></strong><em>湿度 {{ displayEnvironment.humidity }}%RH</em></span>
      </button>
      <button type="button" class="kpi-card" @click="openApp('resource')">
        <span class="kpi-card__icon is-violet"><el-icon><DataLine /></el-icon></span>
        <span class="kpi-card__copy"><small>核心负载</small><strong>{{ percent(displayResource.cpu.usageRate) }}</strong><em>内存 {{ percent(displayResource.memory.usageRate) }}</em></span>
      </button>
      <button type="button" class="kpi-card is-alert" @click="openApp('event-hub')">
        <span class="kpi-card__icon is-red"><el-icon><Bell /></el-icon></span>
        <span class="kpi-card__copy"><small>待响应事件</small><strong>{{ unhandledEventCount }}</strong><em>高等级 {{ highEventCount }}</em></span>
      </button>
    </div>

    <div class="cockpit__body">
      <section class="building-panel" aria-labelledby="topology-title">
        <header class="building-panel__head">
          <div>
            <h2 id="topology-title">建筑感知拓扑</h2>
          </div>
          <div class="view-tabs" aria-label="态势快捷入口">
            <button type="button" class="is-active"><el-icon><MostlyCloudy /></el-icon>环境</button>
            <button type="button" @click="openApp('device')"><el-icon><Cpu /></el-icon>设备</button>
            <button type="button" @click="openApp('event-hub')"><el-icon><Bell /></el-icon>事件</button>
          </div>
        </header>

        <div class="building-panel__content">
          <div class="topology-stage">
            <div class="topology-stage__label">
            </div>

            <SpatialTwin :events="recentEvents" :space="spatialSpace" :source="spatialSource"
              :selected-layer-id="selectedLayerId" mode="home" @select-layer="emit('select-layer', $event)">
              <template #left>
                <div class="metric-float">
                  <span><el-icon><MostlyCloudy /></el-icon></span>
                  <div><small>环境传感</small><strong>{{ displayEnvironment.temperature }}°C · {{ displayEnvironment.humidity }}%</strong></div>
                  <b>实时</b>
                </div>
              </template>
              <template #right>
                <div class="metric-float">
                  <span><el-icon><Cpu /></el-icon></span>
                  <div><small>设备节点</small><strong>{{ value(deviceOverview.onlineNum) }} 在线</strong></div>
                  <b :class="{ 'is-warning': issueDeviceCount > 0 }">{{ issueDeviceCount > 0 ? `${issueDeviceCount} 异常` : '稳定' }}</b>
                </div>
              </template>
              <template #bottom>
                <div class="metric-float">
                  <span><el-icon><DataLine /></el-icon></span>
                  <div><small>计算资源</small><strong>CPU {{ percent(displayResource.cpu.usageRate) }}</strong></div>
                  <b>已接入</b>
                </div>
              </template>
            </SpatialTwin>
          </div>

          <aside class="event-rail" aria-labelledby="event-stream-title">
            <div class="event-rail__head">
              <div><strong id="event-stream-title">关键事件</strong><span>{{ recentEvents.length }}</span></div>
              <button type="button" aria-label="查看全部事件" @click="openApp('event-hub')"><el-icon><ArrowRight /></el-icon></button>
            </div>
            <div v-if="recentEvents.length" class="event-list">
              <button v-for="event in recentEvents.slice(0, 3)" :key="eventKey(event)" type="button" @click="openApp('event-hub')">
                <span class="event-list__severity" :class="`is-${event.level || 'low'}`">{{ levelLabel(event.level) }}</span>
                <strong>{{ eventTitle(event) }}</strong>
                <small>{{ eventSource(event) }} · {{ eventTime(event) }}</small>
                <em>{{ stateLabel(event.handlerState) }}<el-icon><ArrowRight /></el-icon></em>
              </button>
            </div>
            <div v-else class="event-empty">
              <el-icon><Bell /></el-icon>
              <span>{{ loading ? '正在同步事件流' : '暂无事件记录' }}</span>
            </div>
          </aside>
        </div>

<!--        <footer class="capability-chain" aria-label="建筑控制闭环能力链路">-->
<!--          <div class="capability-chain__title"><strong>闭环能力链路</strong></div>-->
<!--          <div v-for="(step, index) in capabilitySteps" :key="step.name" class="chain-step" :class="{ 'is-ready': step.ready }">-->
<!--            <span><el-icon><component :is="step.icon" /></el-icon></span>-->
<!--            <div><strong>{{ step.name }}</strong><small>{{ step.state }}</small></div>-->
<!--            <el-icon v-if="index < capabilitySteps.length - 1" class="chain-arrow"><ArrowRight /></el-icon>-->
<!--          </div>-->
<!--        </footer>-->
      </section>

      <aside class="agent-panel" aria-labelledby="agent-title">
        <header class="agent-panel__head">
          <div><span class="agent-avatar"><el-icon><MagicStick /></el-icon></span><div><h2 id="agent-title">建筑智能体</h2><small>基于已接入运行数据</small></div></div>
          <span class="agent-state"><i></i>运行中</span>
        </header>

        <section class="agent-section">
          <span class="agent-section__label">发现问题</span>
          <div class="insight-card" :class="agentInsight.tone">
            <span><el-icon><component :is="agentInsight.icon" /></el-icon></span>
            <div><strong>{{ agentInsight.title }}</strong><small>{{ agentInsight.summary }}</small></div>
          </div>
        </section>

        <section class="agent-section evidence-card">
          <span class="agent-section__label">运行证据</span>
          <ul>
            <li v-for="item in agentEvidence" :key="item"><i></i><span>{{ item }}</span></li>
          </ul>
        </section>

        <section class="agent-section diagnosis-card">
          <span class="agent-section__label">规则判断</span>
          <strong>{{ agentInsight.judgement }}</strong>
          <div><span>数据覆盖</span><b>{{ sourceCoverage }}%</b></div>
          <div class="coverage"><i :style="{ width: `${sourceCoverage}%` }"></i></div>
        </section>

        <section class="agent-section action-card">
          <span class="agent-section__label">建议行动（需人工复核）</span>
          <strong>{{ agentInsight.action }}</strong>
          <small>系统不会在首页自动下发设备控制指令。</small>
          <button type="button" @click="openApp(agentInsight.target)">进入对应工作区<el-icon><ArrowRight /></el-icon></button>
        </section>

        <footer class="agent-panel__footer">
          <span><i :class="{ 'is-warning': failedSources.length }"></i>{{ dataStatusText }}</span>
          <button type="button" :disabled="loading" aria-label="刷新运行数据" @click="loadAll(false)"><el-icon :class="{ 'is-spinning': loading }"><Refresh /></el-icon></button>
        </footer>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, markRaw, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  ArrowRight,
  Bell,
  CircleCheckFilled,
  Connection,
  Cpu,
  DataLine,
  InfoFilled,
  MagicStick,
  MostlyCloudy,
  Operation,
  Refresh,
  Warning
} from '@element-plus/icons-vue'
import SpatialTwin from './SpatialTwin.vue'
import { demoCpu, demoEnvironment, demoMemory, withMonitorFallback } from '../monitorFallback'
import { getEventOverview, getEventPage } from '@/api/build/event'
import { getDeviceOverview, getDriveMonitor, getEnvMonitorInfo, listDeviceOperLog } from '@/api/build/drive'

defineProps({
  spatialSpace: { type: Object, default: null },
  spatialSource: { type: String, default: 'loading' },
  selectedLayerId: { type: String, default: '' }
})
const emit = defineEmits(['open', 'select-layer'])
const REFRESH_INTERVAL = 30000
const loading = ref(false)
const lastUpdated = ref('')
const failedSources = ref([])
const deviceOverview = ref({})
const eventStats = ref({})
const recentEvents = ref([])
const recentLogs = ref([])
const environmentData = ref({})
const resourceData = ref({})
let refreshTimer = null

const displayEnvironment = computed(() => withMonitorFallback(environmentData.value, demoEnvironment))
const displayResource = computed(() => ({
  cpu: withMonitorFallback(resourceData.value.cpu, demoCpu),
  memory: withMonitorFallback(resourceData.value.memory, demoMemory)
}))

const onlineRate = computed(() => {
  const total = Number(deviceOverview.value.totalNum)
  const online = Number(deviceOverview.value.onlineNum)
  return total > 0 ? `${Math.round((online / total) * 100)}%` : '—'
})
const issueDeviceCount = computed(() => Number(deviceOverview.value.abnormalNum || 0) + Number(deviceOverview.value.offlineNum || 0))
const highEventCount = computed(() => Number(eventStats.value.applyHighNum || 0) + Number(eventStats.value.pendingHighNum || 0))
const unhandledEventCount = computed(() => ['applyHighNum', 'pendingHighNum', 'applyMiddleNum', 'pendingMiddleNum', 'applyLowNum', 'pendingLowNum'].reduce((sum, key) => sum + Number(eventStats.value[key] || 0), 0))
const sourceCoverage = computed(() => Math.round(((5 - failedSources.value.length) / 5) * 100))
const dataStatusText = computed(() => {
  if (loading.value && !lastUpdated.value) return '正在同步运行数据'
  if (failedSources.value.length) return `${failedSources.value.join('、')}暂未更新`
  return lastUpdated.value ? `更新于 ${lastUpdated.value}` : '等待运行数据'
})

const capabilitySteps = computed(() => [
  { name: '感知接入', state: '已接入', ready: true, icon: markRaw(Connection) },
  { name: '事件识别', state: '已接入', ready: true, icon: markRaw(Bell) },
  { name: '人工审核', state: '操作约束', ready: true, icon: markRaw(Operation) },
  { name: '指令下发', state: '已接入', ready: true, icon: markRaw(CircleCheckFilled) },
  { name: '物理回读', state: '已接入', ready: false, icon: markRaw(InfoFilled) }
])

const agentInsight = computed(() => {
  if (!lastUpdated.value) return {
    title: '等待建筑运行数据', summary: '尚未取得可分析的实时数据', judgement: '当前无法形成运行判断', action: '检查数据接入状态', target: 'resource', tone: 'is-neutral', icon: markRaw(InfoFilled)
  }
  if (highEventCount.value > 0) return {
    title: `${highEventCount.value} 个高等级事件待响应`, summary: '需要优先核验事件来源与处置状态', judgement: '高等级事件优先级高于一般设备异常', action: '进入事件中枢复核并处理', target: 'event-hub', tone: 'is-danger', icon: markRaw(Warning)
  }
  if (issueDeviceCount.value > 0) return {
    title: `${issueDeviceCount.value} 个设备异常或离线`, summary: '建议确认设备连接与驱动状态', judgement: '设备连接质量需要人工检查', action: '进入设备中心核验状态', target: 'device', tone: 'is-warning', icon: markRaw(Warning)
  }
  return {
    title: '关键运行指标当前稳定', summary: '未发现高等级事件或异常设备', judgement: '已接入数据范围内未见关键异常', action: '继续查看设备实时状态', target: 'device', tone: 'is-success', icon: markRaw(CircleCheckFilled)
  }
})

const agentEvidence = computed(() => [
  `设备在线 ${value(deviceOverview.value.onlineNum)} / ${value(deviceOverview.value.totalNum)}`,
  `异常与离线设备 ${issueDeviceCount.value} 个`,
  `待响应事件 ${unhandledEventCount.value} 个`,
  `CPU 使用率 ${percent(displayResource.value.cpu.usageRate)}`
])

function openApp(key) {
  emit('open', key)
}

function value(input) {
  return input === undefined || input === null || input === '' ? '—' : input
}

function percent(input) {
  if (input === undefined || input === null || input === '') return '—'
  return String(input).includes('%') ? String(input) : `${input}%`
}

function eventKey(event) {
  return event.eventId || event.id || `${eventTitle(event)}-${eventTime(event)}`
}

function eventTitle(event) {
  return event.eventName || event.title || event.classifyName || '未命名事件'
}

function eventSource(event) {
  return event.deviceName || event.sourceName || event.address || '来源未标注'
}

function eventTime(event) {
  const raw = event.eventTime || event.createTime
  if (!raw) return '时间未标注'
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return String(raw)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
}

function levelLabel(level) {
  return { high: 'P1', middle: 'P2', low: 'P3' }[level] || '事件'
}

function stateLabel(state) {
  return { apply: '待处理', pending: '处理中', completed: '已完成' }[state] || '待核验'
}

async function loadAll(silent = false) {
  if (!silent) loading.value = true
  failedSources.value = []
  const sources = [
    { name: '设备概览', request: getDeviceOverview(), apply: res => { deviceOverview.value = res.data || {} } },
    { name: '事件概览', request: getEventOverview(), apply: res => { eventStats.value = res.data || {} } },
    { name: '事件流', request: getEventPage({ pageNum: 1, pageSize: 3 }), apply: res => { recentEvents.value = res.rows || [] } },
    { name: '控制记录', request: listDeviceOperLog({ pageNum: 1, pageSize: 3, title: '设备-' }), apply: res => { recentLogs.value = res.rows || [] } },
    { name: '环境资源', request: Promise.all([getEnvMonitorInfo(), getDriveMonitor()]), apply: res => {
      environmentData.value = res[0].data || {}
      resourceData.value = res[1].data || {}
    } }
  ]
  const results = await Promise.allSettled(sources.map(source => source.request))
  let successCount = 0
  results.forEach((result, index) => {
    if (result.status === 'fulfilled') {
      sources[index].apply(result.value)
      successCount += 1
    } else {
      failedSources.value.push(sources[index].name)
    }
  })
  if (successCount) lastUpdated.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  loading.value = false
}

onMounted(() => {
  loadAll(false)
  refreshTimer = window.setInterval(() => loadAll(true), REFRESH_INTERVAL)
})

onBeforeUnmount(() => {
  if (refreshTimer) window.clearInterval(refreshTimer)
})
</script>

<style scoped lang="scss">
.cockpit {
  --blue: #1268f3;
  --text: #16243c;
  --muted: #6f8096;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 12px 12px;
  overflow: hidden;
  font-size: 14px;
  line-height: 1.45;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.kpi-strip {
  flex: 0 0 112px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid #dfe7f2;
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8px 24px rgba(40, 79, 128, 0.06);
}

.kpi-card {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 13px 20px;
  border: 0;
  border-right: 1px solid #e4ebf4;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: background 0.18s ease;

  &:last-child {
    border-right: 0;
  }

  &:hover {
    background: #f7faff;
  }

  &:focus-visible {
    outline: 3px solid rgba(18, 104, 243, 0.18);
    outline-offset: -3px;
  }
}

.kpi-card__icon {
  flex: 0 0 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #edf4ff;
  color: #3a83f5;
  font-size: 21px;

  &.is-cyan { background: #e8f9f8; color: #18a99f; }
  &.is-violet { background: #f0f1fc; color: #6f7ec8; }
  &.is-red { background: #fff0ef; color: #e4514c; }
}

.kpi-card__copy {
  min-width: 0;

  small,
  strong,
  em {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    color: #53657c;
    font-size: 14px;
  }

  strong {
    margin-top: 3px;
    color: #15243d;
    font-size: 28px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;

    i {
      margin-left: 4px;
      color: #8a9aad;
      font-size: 14px;
      font-style: normal;
      font-weight: 500;
    }
  }

  em {
    margin-top: 2px;
    color: #8291a3;
    font-size: 13px;
    font-style: normal;
  }
}

.kpi-card.is-alert .kpi-card__copy strong {
  color: #cb3939;
}

.cockpit__body {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 350px;
  gap: 10px;
}

.building-panel,
.agent-panel {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #dfe7f2;
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 8px 24px rgba(40, 79, 128, 0.06);
}

.building-panel {
  display: grid;
  grid-template-rows: 60px minmax(0, 1fr) 108px;
}

.building-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 0 14px 0 18px;
  border-bottom: 1px solid #e6edf5;

  h2 {
    margin: 2px 0 0;
    color: var(--text);
    font-size: 18px;
  }
}

.eyebrow {
  display: block;
  color: #60758d;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.view-tabs {
  display: flex;
  align-items: center;
  gap: 6px;

  button {
    min-height: 36px;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 0 11px;
    border: 1px solid #dce5ef;
    border-radius: 7px;
    background: #fff;
    color: #5a6e87;
    font: inherit;
    font-size: 13px;
    cursor: pointer;

    &.is-active {
      border-color: var(--blue);
      background: var(--blue);
      color: #fff;
      box-shadow: 0 5px 12px rgba(18, 104, 243, 0.18);
    }
  }
}

.building-panel__content {
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  overflow: hidden;
}

.topology-stage {
  position: relative;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background:
    radial-gradient(circle at 48% 45%, rgba(151, 205, 255, 0.22), transparent 38%),
    linear-gradient(155deg, #fafdff 0%, #edf4fb 100%);
}

.topology-stage::before {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(112, 158, 204, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(112, 158, 204, 0.055) 1px, transparent 1px);
  background-size: 30px 30px;
  content: '';
  mask-image: radial-gradient(circle, #000 20%, transparent 80%);
}

.topology-stage__label {
  position: absolute;
  z-index: 5;
  top: 14px;
  left: 16px;

  strong,
  span {
    display: block;
  }

  strong {
    color: #273b56;
    font-size: 14px;
  }

  span {
    margin-top: 3px;
    color: #60758d;
    font-size: 13px;
  }
}

.building-model {
  position: absolute;
  top: 50%;
  left: 48%;
  width: min(58vw, 480px);
  height: 260px;
  filter: drop-shadow(0 24px 24px rgba(60, 96, 137, 0.1));
  transform: translate(-50%, -44%);
}

.floor {
  position: absolute;
  left: 50%;
  width: 380px;
  height: 210px;
  overflow: hidden;
  border: 1px solid rgba(91, 145, 200, 0.52);
  clip-path: polygon(50% 0, 100% 28%, 77% 100%, 8% 74%);
  transform: translateX(-50%);
  background:
    linear-gradient(35deg, rgba(255, 255, 255, 0.96), rgba(185, 211, 235, 0.82)),
    #eaf2fa;
  box-shadow: 0 24px 40px rgba(56, 90, 126, 0.15);

  &::before,
  &::after,
  i {
    position: absolute;
    border: 1px solid rgba(76, 132, 184, 0.3);
    background: rgba(255, 255, 255, 0.42);
    content: '';
  }

  &::before { inset: 20% 18% 38% 28%; }
  &::after { inset: 48% 29% 12% 13%; }
  i:nth-child(1) { inset: 18% 64% 48% 9%; }
  i:nth-child(2) { inset: 15% 8% 44% 72%; }
  i:nth-child(3) { inset: 58% 9% 12% 70%; }
}

.floor--base { top: 56px; opacity: 0.58; transform: translateX(-50%) translateY(45px); }
.floor--middle { top: 35px; opacity: 0.78; transform: translateX(-50%) translateY(20px); }
.floor--top { top: 10px; border-color: rgba(50, 126, 205, 0.66); }

.building-model__core {
  position: absolute;
  z-index: 4;
  top: 85px;
  left: 50%;
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  border: 8px solid rgba(18, 104, 243, 0.12);
  border-radius: 50%;
  background: #fff;
  color: var(--blue);
  font-size: 24px;
  box-shadow: 0 12px 30px rgba(18, 104, 243, 0.2);
  transform: translateX(-50%);
}

.sensor-node {
  position: absolute;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 5px;
  color: #58718e;
  font-size: 13px;
  letter-spacing: 0.08em;

  i {
    width: 10px;
    height: 10px;
    border: 3px solid #fff;
    border-radius: 50%;
    background: #3ac6dd;
    box-shadow: 0 0 0 4px rgba(58, 198, 221, 0.14);
  }
}

.node--env { top: 52px; left: 22%; }
.node--device { top: 38px; right: 17%; }
.node--event { bottom: 45px; left: 15%; }
.node--control { right: 13%; bottom: 55px; }

.metric-float {
  position: relative;
  z-index: 7;
  min-width: 186px;
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border: 1px solid rgba(211, 224, 238, 0.95);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 8px 22px rgba(63, 91, 126, 0.1);
  backdrop-filter: blur(10px);

  > span {
    width: 30px;
    height: 30px;
    display: grid;
    place-items: center;
    border-radius: 8px;
    background: #edf5ff;
    color: var(--blue);
  }

  small,
  strong {
    display: block;
    white-space: nowrap;
  }

  small { color: #60758d; font-size: 13px; }
  strong { margin-top: 3px; color: #233751; font-size: 13px; }
  > b { color: #15936c; font-size: 13px; }
  > b.is-warning { color: #dc7c24; }
}

.metric-float--env { top: 20%; left: 5%; }
.metric-float--device { top: 16%; right: 4%; }
.metric-float--resource { right: 8%; bottom: 8%; }

.event-rail {
  min-height: 0;
  padding: 12px;
  overflow-y: auto;
  border-left: 1px solid #e3eaf3;
  background: rgba(250, 252, 255, 0.88);
}

.event-rail__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;

  > div {
    display: flex;
    align-items: center;
    gap: 7px;
  }

  strong { color: #263a54; font-size: 14px; }
  span { padding: 2px 6px; border-radius: 999px; background: #eaf1fa; color: #647a93; font-size: 13px; }
  button { width: 36px; height: 36px; border: 0; border-radius: 8px; background: transparent; color: #7a8da4; cursor: pointer; }
}

.event-list {
  display: grid;
  gap: 7px;

  button {
    position: relative;
    min-width: 0;
    min-height: 96px;
    padding: 10px;
    border: 1px solid #e0e8f2;
    border-radius: 9px;
    background: #fff;
    color: inherit;
    text-align: left;
    cursor: pointer;

    > strong,
    > small {
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    > strong { margin-top: 6px; color: #2a3e58; font-size: 13px; }
    > small { margin-top: 4px; color: #60758d; font-size: 13px; }
    > em { position: absolute; right: 7px; bottom: 7px; display: flex; align-items: center; gap: 2px; color: #68809a; font-size: 13px; font-style: normal; }
  }
}

.event-list__severity {
  display: inline-block;
  padding: 2px 5px;
  border-radius: 4px;
  background: #edf4ff;
  color: #1268f3;
  font-size: 13px;
  font-weight: 700;

  &.is-high { background: #fff0ef; color: #d7433f; }
  &.is-middle { background: #fff6e8; color: #c6761d; }
}

.event-empty {
  height: calc(100% - 42px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #98a7b9;
  font-size: 13px;

  .el-icon { font-size: 24px; }
}

.capability-chain {
  display: grid;
  grid-template-columns: 150px repeat(5, minmax(0, 1fr));
  align-items: center;
  gap: 4px;
  padding: 10px 15px;
  border-top: 1px solid #e2eaf3;
  background: #fff;
}

.capability-chain__title {
  small,
  strong { display: block; }
  small { color: #60758d; font-size: 13px; font-weight: 700; letter-spacing: 0.12em; }
  strong { margin-top: 4px; color: #2b4059; font-size: 14px; }
}

.chain-step {
  position: relative;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;

  > span {
    flex: 0 0 32px;
    height: 32px;
    display: grid;
    place-items: center;
    border: 1px solid #d8e1ed;
    border-radius: 50%;
    background: #f7f9fc;
    color: #91a0b2;
  }

  &.is-ready > span { border-color: #9fc3ff; background: #edf5ff; color: #1268f3; }
  strong,
  small { display: block; white-space: nowrap; }
  strong { color: #435871; font-size: 13px; }
  small { margin-top: 3px; color: #60758d; font-size: 13px; }
}

.chain-arrow {
  position: absolute;
  right: 4px;
  color: #b7c5d5;
}

.agent-panel {
  display: flex;
  flex-direction: column;
  padding: 14px;
  overflow-y: auto;
}

.agent-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5ebf3;

  > div,
  .agent-state { display: flex; align-items: center; }
  > div { gap: 10px; }
  h2 { margin: 0; color: #1e314b; font-size: 18px; }
  small { display: block; margin-top: 2px; color: #60758d; font-size: 13px; }
}

.agent-avatar {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(145deg, #e8fff7, #dff2ff);
  color: #14956d;
  font-size: 19px;
}

.agent-state {
  gap: 6px;
  padding: 5px 8px;
  border: 1px solid #dce7ef;
  border-radius: 7px;
  color: #51677f;
  font-size: 13px;

  i { width: 6px; height: 6px; border-radius: 50%; background: #16ad7a; }
}

.agent-section {
  margin-top: 12px;
}

.agent-section__label {
  display: block;
  margin-bottom: 7px;
  color: #71849b;
  font-size: 13px;
  font-weight: 600;
}

.insight-card {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  padding: 11px;
  border: 1px solid #dfe8f2;
  border-radius: 10px;
  background: #f9fbfe;

  > span {
    flex: 0 0 28px;
    height: 28px;
    display: grid;
    place-items: center;
    border-radius: 50%;
    background: #edf4ff;
    color: #1268f3;
  }

  strong,
  small { display: block; }
  strong { color: #2c4059; font-size: 14px; line-height: 1.45; }
  small { margin-top: 4px; color: #60758d; font-size: 13px; line-height: 1.45; }
  &.is-danger > span { background: #fff0ef; color: #dc4944; }
  &.is-warning > span { background: #fff5e8; color: #d47c22; }
  &.is-success > span { background: #e9f8f2; color: #15956c; }
}

.evidence-card {
  padding: 11px;
  border: 1px solid #e1e8f1;
  border-radius: 10px;

  ul { display: grid; gap: 7px; margin: 0; padding: 0; list-style: none; }
  li { display: flex; align-items: center; gap: 7px; color: #667a91; font-size: 13px; }
  li i { width: 4px; height: 4px; border-radius: 50%; background: #7c91aa; }
}

.diagnosis-card {
  padding: 11px;
  border: 1px solid #e1e8f1;
  border-radius: 10px;
  background: #fbfcfe;

  > strong { display: block; color: #31465f; font-size: 13px; line-height: 1.45; }
  > div:not(.coverage) { display: flex; justify-content: space-between; margin-top: 10px; color: #7c8da1; font-size: 13px; }
  > div b { color: #1268f3; }
}

.coverage {
  height: 4px;
  margin-top: 5px;
  overflow: hidden;
  border-radius: 999px;
  background: #e6ecf4;

  i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #3c88f8, #1268f3); }
}

.action-card {
  padding: 11px;
  border: 1px solid #cfe0fb;
  border-radius: 10px;
  background: #f4f8ff;

  > strong,
  > small { display: block; }
  > strong { color: #27466e; font-size: 14px; line-height: 1.45; }
  > small { margin-top: 4px; color: #60758d; font-size: 13px; }
  button { width: 100%; min-height: 38px; display: flex; align-items: center; justify-content: center; gap: 6px; margin-top: 9px; border: 0; border-radius: 7px; background: #1268f3; color: #fff; font: inherit; font-size: 13px; cursor: pointer; }
}

.agent-panel__footer {
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid #e5ebf3;

  span { min-width: 0; display: flex; align-items: center; gap: 6px; overflow: hidden; color: #60758d; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
  span i { width: 6px; height: 6px; border-radius: 50%; background: #16ad7a; }
  span i.is-warning { background: #e79728; }
  button { flex: 0 0 36px; height: 36px; border: 1px solid #dce5ef; border-radius: 8px; background: #fff; color: #55708f; cursor: pointer; }
}

button:focus-visible {
  outline: 3px solid rgba(18, 104, 243, 0.2);
  outline-offset: 2px;
}

.is-spinning { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 1180px) {
  .cockpit__body { grid-template-columns: 1fr; }
  .agent-panel { display: none; }
}

@media (max-width: 880px) {
  .kpi-strip { flex-basis: 176px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .kpi-card:nth-child(2) { border-right: 0; }
  .kpi-card { padding: 8px 12px; }
  .kpi-card__icon { flex-basis: 34px; height: 34px; }
  .kpi-card__copy strong { font-size: 24px; }
  .building-panel__content { grid-template-columns: 1fr; }
  .event-rail { display: none; }
  .capability-chain { grid-template-columns: repeat(5, minmax(0, 1fr)); }
  .capability-chain__title { display: none; }
}

@media (max-height: 760px) and (min-width: 881px) {
  .cockpit { gap: 8px; padding-top: 8px; }
  .kpi-strip { flex-basis: 98px; }
  .building-panel { grid-template-rows: 54px minmax(0, 1fr) 94px; }
  .agent-section { margin-top: 9px; }
  .evidence-card ul { gap: 5px; }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
