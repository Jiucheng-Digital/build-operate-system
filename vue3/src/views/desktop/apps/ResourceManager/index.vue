<template>
  <AppWindow title="资源管理器" @close="$emit('close')" @minimize="$emit('minimize')">
    <div class="rm">
      <!-- 左侧导航 -->
      <aside class="rm__nav">
        <button type="button" class="rm__nav-item" :class="{ 'is-active': activeNav === 'system' }" @click="selectNav('system')">
          <el-icon class="rm__nav-icon"><Monitor /></el-icon>
          <span>系统资源</span>
        </button>
        <button type="button" class="rm__nav-item" :class="{ 'is-active': activeNav === 'env' }" @click="selectNav('env')">
          <el-icon class="rm__nav-icon"><Sunny /></el-icon>
          <span>环境检测</span>
        </button>
      </aside>

      <!-- 右侧 -->
      <section class="rm__main" v-loading="loading">
        <header class="rm__head">
          <h2 class="rm__title">{{ activeNav === 'system' ? '系统资源监控' : '环境检测' }}</h2>
          <el-button :icon="Refresh" @click="loadActive(false)">刷新</el-button>
        </header>

        <!-- 系统资源 -->
        <div v-if="activeNav === 'system'" class="rm__cards">
          <!-- CPU -->
          <div class="rm-card">
            <div class="rm-card__head">
              <span class="rm-card__title"><el-icon><Cpu /></el-icon> CPU 资源</span>
              <el-tag :type="statusTag(cpu.status)" size="small" effect="light">{{ cpu.status }}</el-tag>
            </div>
            <div class="rm-metric">
              <div class="rm-metric__row"><span>使用率</span><span class="rm-metric__val">{{ cpu.usageRate }}</span></div>
              <div class="rm-bar"><div class="rm-bar__fill" :style="barStyle(cpu.usageRate)"></div></div>
            </div>
            <div class="rm-row"><span>内核版本</span><b>{{ cpu.kernelVersion || '—' }}</b></div>
            <div class="rm-row"><span>架构</span><b>{{ cpu.architecture || '—' }}</b></div>
          </div>

          <!-- 内存 -->
          <div class="rm-card">
            <div class="rm-card__head">
              <span class="rm-card__title"><el-icon><Coin /></el-icon> 内存资源</span>
              <el-tag :type="statusTag(memory.status)" size="small" effect="light">{{ memory.status }}</el-tag>
            </div>
            <div class="rm-metric">
              <div class="rm-metric__row"><span>已使用 / 总内存</span><span class="rm-metric__val">{{ memory.usedTotalMemory || '—' }}</span></div>
              <div class="rm-bar"><div class="rm-bar__fill" :style="barStyle(memory.usageRate)"></div></div>
            </div>
            <div class="rm-row"><span>缓存</span><b>{{ memory.cacheMemory || '—' }}</b></div>
            <div class="rm-row"><span>可用内存</span><b>{{ memory.availableMemory || '—' }}</b></div>
          </div>

          <!-- 存储 -->
          <div class="rm-card">
            <div class="rm-card__head">
              <span class="rm-card__title"><el-icon><FolderOpened /></el-icon> 存储资源</span>
              <el-tag :type="statusTag(storage.status)" size="small" effect="light">{{ storage.status || '—' }}</el-tag>
            </div>
            <div class="rm-metric">
              <div class="rm-metric__row"><span>已使用 / 总存储</span><span class="rm-metric__val">{{ storage.usedTotalStorage || '—' }}</span></div>
              <div class="rm-bar"><div class="rm-bar__fill" :style="barStyle(storage.usageRate)"></div></div>
            </div>
            <div class="rm-row"><span>数据分区</span><b>{{ storage.dataPartition || '—' }}</b></div>
            <div class="rm-row"><span>日志分区</span><b>{{ storage.logPartition || '—' }}</b></div>
          </div>

          <!-- 网络 -->
          <div class="rm-card">
            <div class="rm-card__head">
              <span class="rm-card__title"><el-icon><Connection /></el-icon> 网络资源</span>
              <el-tag :type="statusTag(network.status)" size="small" effect="light">{{ network.status || '—' }}</el-tag>
            </div>
            <div class="rm-row rm-row--top"><span>IP 地址</span><b>{{ network.ipAddress || '—' }}<i v-if="network.interfaceName"> ({{ network.interfaceName }})</i></b></div>
            <div class="rm-row"><span>上行 / 下行速度</span>
              <b>{{ network.uploadSpeed || '—' }} <span class="rm-up">↑</span> &nbsp; {{ network.downloadSpeed || '—' }} <span class="rm-down">↓</span></b>
            </div>
            <div class="rm-row"><span>网络状态</span><b>{{ network.networkStatus || '—' }}<i v-if="network.containerStatus"> ({{ network.containerStatus }})</i></b></div>
          </div>
        </div>

        <!-- 环境检测 -->
        <div v-else class="rm__env">
          <div v-for="m in envMetrics" :key="m.key" class="env-card">
            <div class="env-card__label"><i class="env-dot" :class="m.level"></i>{{ m.label }}</div>
            <div class="env-card__value">
              {{ m.value === undefined || m.value === null || m.value === '' ? '—' : m.value }}
              <span class="env-card__unit">{{ m.unit }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </AppWindow>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { Monitor, Sunny, Refresh, Cpu, Coin, FolderOpened, Connection } from '@element-plus/icons-vue'
import AppWindow from '../../components/AppWindow.vue'
import { getDriveMonitor, getEnvMonitorInfo } from '@/api/build/drive'
import { demoCpu, demoEnvironment, demoMemory, withMonitorFallback } from '../../monitorFallback'

defineEmits(['close', 'minimize'])

const REFRESH_INTERVAL = 5000 // 自动刷新间隔(ms)
const activeNav = ref('system') // system | env
const loading = ref(false)
const data = ref({})
const envData = ref({})
let timer = null

const cpu = computed(() => withMonitorFallback(data.value.cpu, demoCpu))
const memory = computed(() => withMonitorFallback(data.value.memory, demoMemory))
const storage = computed(() => data.value.storage || {})
const network = computed(() => data.value.network || {})

// 环境检测指标
const envMetrics = computed(() => {
  const d = withMonitorFallback(envData.value, demoEnvironment)
  return [
    { key: 'wind', label: '风速', value: d.wind_speed, unit: 'm/s', level: 'ok' },
    { key: 'pm25', label: 'PM2.5', value: d.pm25, unit: 'μg/m³', level: pmLevel(d.pm25, 75, 115) },
    { key: 'pm10', label: 'PM10', value: d.pm10, unit: 'μg/m³', level: pmLevel(d.pm10, 150, 250) },
    { key: 'temp', label: '温度', value: d.temperature, unit: '°C', level: 'ok' },
    { key: 'humidity', label: '湿度', value: d.humidity, unit: '%RH', level: 'ok' },
    { key: 'noise', label: '噪声', value: d.noise, unit: 'dB', level: pmLevel(d.noise, 70, 85) }
  ]
})

// 简单分级：低于 warn 绿、warn~danger 橙、超 danger 红
function pmLevel(v, warn, danger) {
  const n = parseFloat(v)
  if (isNaN(n)) return 'ok'
  if (n >= danger) return 'danger'
  if (n >= warn) return 'warn'
  return 'ok'
}

function loadSystem(silent) {
  if (!silent) loading.value = true
  getDriveMonitor()
    .then(res => { data.value = res.data || {} })
    .catch(() => {})
    .finally(() => { if (!silent) loading.value = false })
}

function loadEnv(silent) {
  if (!silent) loading.value = true
  getEnvMonitorInfo()
    .then(res => { envData.value = res.data || {} })
    .catch(() => {})
    .finally(() => { if (!silent) loading.value = false })
}

// 刷新当前选中的视图
function loadActive(silent = false) {
  if (activeNav.value === 'system') loadSystem(silent)
  else loadEnv(silent)
}

function selectNav(nav) {
  if (activeNav.value === nav) return
  activeNav.value = nav
  loadActive(false)
}

// 状态标签颜色
function statusTag(status) {
  if (!status) return 'info'
  if (status === '正常') return 'success'
  if (status.includes('异常') || status.includes('错误') || status.includes('故障')) return 'danger'
  if (status.includes('高') || status.includes('警') || status.includes('满')) return 'warning'
  return 'info'
}

// 进度条：从 "88.5%" 解析数值，按高低着色
function parsePct(v) {
  const n = parseFloat(String(v || '').replace('%', ''))
  return isNaN(n) ? 0 : Math.min(100, Math.max(0, n))
}
function barStyle(v) {
  const n = parsePct(v)
  let color = 'linear-gradient(90deg, #3b82f6, #2f6bff)'
  if (n >= 85) color = 'linear-gradient(90deg, #ff7a5c, #f5483b)'
  else if (n >= 65) color = 'linear-gradient(90deg, #ffb13b, #ff8f1f)'
  return { width: n + '%', background: color }
}

onMounted(() => {
  loadActive(false)
  timer = setInterval(() => loadActive(true), REFRESH_INTERVAL)
})

onBeforeUnmount(() => {
  if (timer) { clearInterval(timer); timer = null }
})
</script>

<style scoped lang="scss">
.rm {
  flex: 1;
  display: flex;
  min-width: 0;
  min-height: 0;
}

/* 左侧 */
.rm__nav {
  flex: 0 0 180px;
  padding: 14px 10px;
  background: #f6f8fb;
  border-right: 1px solid #eef1f5;
}

.rm__nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 12px;
  margin-bottom: 4px;
  border-radius: 8px;
  font-size: 15px;
  color: #4a5663;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;

  &:hover {
    background: #eaf0f8;
  }

  &.is-active {
    background: #e7f0ff;
    color: #2f74ff;
    font-weight: 600;
  }
}

.rm__nav-icon {
  font-size: 16px;
}

/* 右侧 */
.rm__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 18px 22px;
  overflow: auto;
}

.rm__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.rm__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1f2d3d;
}

/* 卡片网格 */
.rm__cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.rm-card {
  padding: 18px 20px;
  border: 1px solid #eef1f5;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(31, 80, 140, 0.04);
}

.rm-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.rm-card__title {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;

  .el-icon {
    font-size: 18px;
    color: #2f74ff;
  }
}

/* 主指标 + 进度条 */
.rm-metric {
  margin-bottom: 12px;
}

.rm-metric__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.rm-metric__val {
  font-weight: 700;
  color: #1f2d3d;
}

.rm-bar {
  height: 7px;
  border-radius: 6px;
  background: #eef1f6;
  overflow: hidden;

  &__fill {
    height: 100%;
    border-radius: 6px;
    transition: width 0.4s ease;
  }
}

/* 普通键值行 */
.rm-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 0;
  font-size: 14px;
  color: #6b7280;
  border-top: 1px solid #f2f4f7;

  &--top {
    border-top: 0;
  }

  b {
    font-weight: 600;
    color: #303a45;
    text-align: right;
    word-break: break-all;
  }

  i {
    font-style: normal;
    color: #9aa7b4;
  }
}

.rm-up {
  color: #2f9e44;
}

.rm-down {
  color: #2f74ff;
}

/* 环境检测卡片 */
.rm__env {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.env-card {
  padding: 18px 20px;
  border: 1px solid #eef1f5;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(31, 80, 140, 0.04);
}

.env-card__label {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 14px;
  color: #5b6b7c;
}

.env-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #34c759;

  &.warn {
    background: #ff9f1f;
  }

  &.danger {
    background: #f5483b;
  }
}

.env-card__value {
  margin-top: 12px;
  font-size: 30px;
  font-weight: 700;
  color: #2f6bff;
  line-height: 1.1;
}

.env-card__unit {
  margin-left: 4px;
  font-size: 14px;
  font-weight: 500;
  color: #9aa7b4;
}
</style>
