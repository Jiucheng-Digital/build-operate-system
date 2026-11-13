<template>
  <AppWindow title="设备中心" @close="$emit('close')" @minimize="$emit('minimize')">
    <div class="dc">
      <!-- 左侧：驱动列表（显示 classifyName） -->
      <aside class="dc__nav">
        <div class="dc__nav-group">设备管理</div>
        <button
          type="button"
          v-for="c in categories"
          :key="c.code"
          class="dc__nav-item"
          :class="{ 'is-active': view === 'device' && current && current.code === c.code }"
          @click="selectCategory(c)"
        >
          <el-icon class="dc__nav-icon"><Box /></el-icon>
          <span>{{ c.name }}</span>
        </button>

        <div class="dc__nav-group">设备操作</div>
        <button type="button" class="dc__nav-item" :class="{ 'is-active': view === 'log' }" @click="openLog">
          <el-icon class="dc__nav-icon"><Tickets /></el-icon>
          <span>操作日志</span>
        </button>
      </aside>

      <!-- 右侧 -->
      <section class="dc__main">
        <!-- 设备视图 -->
        <template v-if="view === 'device'">
        <header class="dc__head">
          <h2 class="dc__title">设备概览</h2>
          <el-button :icon="Refresh" @click="refresh">刷新状态</el-button>
        </header>

        <!-- 概览卡片 -->
        <div class="dc__cards" v-loading="overviewLoading">
          <div class="dc-card">
            <div class="dc-card__label">在线设备</div>
            <div class="dc-card__num">{{ fmt(overview.onlineNum) }}</div>
            <div class="dc-card__sub">总设备数: {{ fmt(overview.totalNum) }}</div>
          </div>
          <div class="dc-card dc-card--warn">
            <div class="dc-card__label">异常设备</div>
            <div class="dc-card__num">{{ fmt(overview.abnormalNum) }}</div>
            <div class="dc-card__sub">需及时处理</div>
          </div>
          <div class="dc-card dc-card--danger">
            <div class="dc-card__label">离线设备</div>
            <div class="dc-card__num">{{ fmt(overview.offlineNum) }}</div>
            <div class="dc-card__sub">离线时长 &gt; 1小时</div>
          </div>
        </div>

        <!-- 设备列表 -->
        <div class="dc__panel">
          <div class="dc__panel-head">
            <span class="dc__panel-title"><el-icon><Grid /></el-icon> 设备列表</span>
            <div class="dc__filters">
              <el-select
                v-model="deviceQuery.manufacturer"
                class="dc__mfr"
                placeholder="厂商"
                clearable
                @change="searchDevices"
              >
                <el-option v-for="m in manufacturerOptions" :key="m.dictValue" :label="m.dictLabel" :value="m.dictValue" />
              </el-select>
              <el-input
                v-model="deviceQuery.name"
                class="dc__search"
                placeholder="查询"
                clearable
                :prefix-icon="Search"
                @keyup.enter="searchDevices"
                @clear="searchDevices"
              />
              <!-- 照明：一键全开/全关 -->
<!--              <div v-if="isLighting" class="dc__all-light">-->
<!--                <span class="dc__all-light-label">一键照明</span>-->
<!--                <el-switch-->
<!--                  v-model="allLightOn"-->
<!--                  active-text="全开"-->
<!--                  inactive-text="全关"-->
<!--                  inline-prompt-->
<!--                  :loading="allLightLoading"-->
<!--                  style="&#45;&#45;el-switch-on-color:#34c759;&#45;&#45;el-switch-off-color:#c0c4cc"-->
<!--                  @change="onControlAllLighting"-->
<!--                />-->
<!--              </div>-->
            </div>
          </div>

          <el-table v-loading="deviceLoading" :data="deviceList" class="dc__table" height="100%">
            <el-table-column label="设备ID" prop="serialNumber" width="120" show-overflow-tooltip />
            <el-table-column label="设备名称" prop="name" min-width="150" show-overflow-tooltip />
            <el-table-column label="设备类型" prop="deviceType" width="130" show-overflow-tooltip>
              <template #default="{ row }">{{ row.deviceType || row.classify || '—' }}</template>
            </el-table-column>
            <el-table-column label="位置" prop="address" min-width="110" show-overflow-tooltip />
            <el-table-column label="厂商" prop="manufacturer" width="110" show-overflow-tooltip>
              <template #default="{ row }">{{ manufacturerLabel(row.manufacturer) }}</template>
            </el-table-column>
            <el-table-column label="设备状态" prop="status" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'online' ? 'success' : 'danger'" size="small" effect="light">
                  {{ row.status === 'online' ? '在线' : (row.status === 'offline' ? '离线' : (row.status || '—')) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="开关状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="switchStateType(row)" size="small" effect="light">
                  {{ switchStateLabel(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" :icon="Setting" @click="onControl(row)">控制</el-button>
                <el-button
                  v-if="isMonitorRow(row)"
                  link
                  type="success"
                  size="small"
                  :icon="User"
                  @click="openFace(row)"
                >
                  人脸
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-show="deviceTotal > 0"
            :total="deviceTotal"
            v-model:page="deviceQuery.pageNum"
            v-model:limit="deviceQuery.pageSize"
            :page-sizes="[10, 20, 30, 50]"
            @pagination="loadDevices"
          />
        </div>
        </template>

        <!-- 操作日志视图 -->
        <template v-else>
          <header class="dc__head">
            <h2 class="dc__title">操作日志</h2>
            <el-button :icon="Refresh" @click="loadLogs">刷新</el-button>
          </header>

          <div class="dc__panel">
            <div class="dc__panel-head">
              <span class="dc__panel-title"><el-icon><Tickets /></el-icon> 日志列表</span>
            </div>

            <el-table v-loading="logLoading" :data="logList" class="dc__table" height="100%">
              <el-table-column label="操作内容" prop="title" min-width="190" show-overflow-tooltip />
              <el-table-column label="请求方式" prop="requestMethod" width="100" align="center">
                <template #default="{ row }">
                  <el-tag size="small" effect="plain">{{ row.requestMethod || '—' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作人" prop="operName" width="100" />
              <el-table-column label="操作IP" prop="operIp" width="130" show-overflow-tooltip />
              <el-table-column label="状态" prop="status" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small" effect="light">
                    {{ row.status === 0 ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="耗时" prop="costTime" width="90" align="center">
                <template #default="{ row }">{{ row.costTime != null ? row.costTime + ' ms' : '—' }}</template>
              </el-table-column>
              <el-table-column label="操作时间" prop="operTime" width="160" align="center" />
              <el-table-column label="详情" width="80" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="showLogDetail(row)">详情</el-button>
                </template>
              </el-table-column>
            </el-table>

            <pagination
              v-show="logTotal > 0"
              :total="logTotal"
              v-model:page="logQuery.pageNum"
              v-model:limit="logQuery.pageSize"
              :page-sizes="[10, 20, 30, 50]"
              @pagination="loadLogs"
            />
          </div>
        </template>
      </section>
    </div>

    <!-- 门禁控制面板 -->
    <ControlDialog v-model="controlVisible" :device="controlDevice" />
    <!-- 监控画面 -->
    <MonitorDialog v-model="monitorVisible" :device="monitorDevice" />
    <!-- 人脸识别布控 -->
    <FaceDialog v-model="faceVisible" :device="faceDevice" @success="loadDevices" />
    <!-- 照明控制 -->
    <LightingControlDialog v-model="lightingVisible" :device="lightingDevice" @success="loadDevices" />

    <!-- 操作日志详情 -->
    <el-dialog v-model="logDetailVisible" title="操作日志详情" width="600px" align-center append-to-body class="log-detail-dialog">
      <el-descriptions v-if="currentLog" :column="1" border size="small">
        <el-descriptions-item label="操作内容">{{ currentLog.title }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentLog.requestMethod }} {{ currentLog.operUrl }}</el-descriptions-item>
        <el-descriptions-item label="操作人 / IP">{{ currentLog.operName }} / {{ currentLog.operIp }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentLog.operTime }}（耗时 {{ currentLog.costTime }} ms）</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentLog.status === 0 ? 'success' : 'danger'" size="small" effect="light">
            {{ currentLog.status === 0 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="log-pre">{{ pretty(currentLog.operParam) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentLog.status === 0" label="返回结果">
          <pre class="log-pre">{{ pretty(currentLog.jsonResult) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-else label="错误信息">
          <span class="log-err">{{ currentLog.errorMsg || '—' }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </AppWindow>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, Tickets, Refresh, Grid, Search, Setting, User } from '@element-plus/icons-vue'
import AppWindow from '../../components/AppWindow.vue'
import ControlDialog from './ControlDialog.vue'
import MonitorDialog from './MonitorDialog.vue'
import FaceDialog from './FaceDialog.vue'
import LightingControlDialog from './LightingControlDialog.vue'
import { listClassify, getDeviceOverview, getDevicePage, listDeviceOperLog, controlAllLighting } from '@/api/build/drive'
import { getDicts } from '@/api/system/dict/data'

defineEmits(['close', 'minimize'])

function isStreamOn(value) {
  return value === true || value === 1 || value === '1' || value === 'true' || value === 'ture' || value === 'on'
}

function isLightingRow(row) {
  const classify = row?.classify || row?.classifyCode || row?.deviceTypeCode
  if (classify) return classify === LIGHTING_CLASSIFY
  return current.value?.code === LIGHTING_CLASSIFY
}

function switchStateValue(row) {
  if (!isLightingRow(row)) return row?.streamSwitch
  return row?.state !== undefined && row?.state !== null && row?.state !== '' ? row.state : row?.streamSwitch
}

function switchStateLabel(row) {
  return isStreamOn(switchStateValue(row)) ? '开启' : '关闭'
}

function switchStateType(row) {
  return isStreamOn(switchStateValue(row)) ? 'success' : 'info'
}

// 控制面板按分类区分（不同类型操作界面各不相同）
const DOOR_CLASSIFY = 'access_control'
const MONITOR_CLASSIFY = 'monitor'
const LIGHTING_CLASSIFY = 'lighting'
const controlVisible = ref(false)
const controlDevice = ref(null)
const monitorVisible = ref(false)
const monitorDevice = ref(null)
const faceVisible = ref(false)
const faceDevice = ref(null)
const lightingVisible = ref(false)
const lightingDevice = ref(null)

// 照明一键开关
const allLightOn = ref(false)
const allLightLoading = ref(false)

// 右侧视图：device 设备概览 / log 操作日志
const view = ref('device')

// 左侧分类目录（取分类接口的叶子分类）
const categories = ref([])
const current = ref(null)

const overview = ref({})
const overviewLoading = ref(false)

// 厂商下拉（复用 drive_manufacturer 字典）
const manufacturerOptions = ref([])

const deviceList = ref([])
const deviceTotal = ref(0)
const deviceLoading = ref(false)
const deviceQuery = reactive({ pageNum: 1, pageSize: 10, classify: undefined, name: undefined, manufacturer: undefined })

function fmt(v) {
  return v === undefined || v === null || v === '' ? '—' : v
}

// 厂商 dictValue(hikvision) -> dictLabel(海康) 映射，字典只拉一次后复用
const manufacturerMap = computed(() => {
  const map = {}
  manufacturerOptions.value.forEach(m => { map[m.dictValue] = m.dictLabel })
  return map
})

function manufacturerLabel(val) {
  if (!val) return '—'
  return manufacturerMap.value[val] || val
}

// 左侧分类目录：取分类列表里的叶子分类（没有子分类的）
function loadCategories() {
  listClassify()
    .then(res => {
      const list = res.data || []
      categories.value = list.filter(c => !list.some(x => x.parentCode === c.code))
      if (categories.value.length) selectCategory(categories.value[0])
    })
    .catch(() => {})
}

function loadManufacturers() {
  getDicts('drive_manufacturer')
    .then(res => { manufacturerOptions.value = res.data || [] })
    .catch(() => {})
}

function selectCategory(c) {
  view.value = 'device'
  current.value = c
  deviceQuery.classify = c.code
  deviceQuery.name = undefined
  deviceQuery.manufacturer = undefined
  deviceQuery.pageNum = 1
  loadOverview()
  loadDevices()
}

function loadOverview() {
  if (!current.value) return
  overviewLoading.value = true
  getDeviceOverview(current.value.code)
    .then(res => { overview.value = res.data || {} })
    .catch(() => {})
    .finally(() => { overviewLoading.value = false })
}

function loadDevices() {
  deviceLoading.value = true
  // 只传 classify + manufacturer + name + 分页
  const params = {
    pageNum: deviceQuery.pageNum,
    pageSize: deviceQuery.pageSize,
    classify: deviceQuery.classify
  }
  if (deviceQuery.name) params.name = deviceQuery.name
  if (deviceQuery.manufacturer) params.manufacturer = deviceQuery.manufacturer
  getDevicePage(params)
    .then(res => {
      deviceList.value = res.rows || []
      deviceTotal.value = res.total || 0
    })
    .catch(() => {})
    .finally(() => { deviceLoading.value = false })
}

function searchDevices() {
  deviceQuery.pageNum = 1
  loadDevices()
}

function refresh() {
  loadOverview()
  loadDevices()
}

// 当前是否照明分类
const isLighting = computed(() => current.value && current.value.code === LIGHTING_CLASSIFY)

// 一键控制所有照明
function onControlAllLighting(val) {
  allLightLoading.value = true
  controlAllLighting(val ? 'on' : 'off')
    .then(() => {
      ElMessage.success(val ? '已全部打开' : '已全部关闭')
      loadDevices()
    })
    .catch(() => { allLightOn.value = !val }) // 失败回滚
    .finally(() => { allLightLoading.value = false })
}

function onControl(row) {
  // 按分类打开对应控制面板；未实现的类型提示开发中
  const classify = row.classify || (current.value && current.value.code)
  if (classify === DOOR_CLASSIFY) {
    controlDevice.value = row
    controlVisible.value = true
  } else if (classify === MONITOR_CLASSIFY) {
    monitorDevice.value = row
    monitorVisible.value = true
  } else if (classify === LIGHTING_CLASSIFY) {
    lightingDevice.value = row
    lightingVisible.value = true
  } else {
    ElMessage({ message: '该类型设备的控制面板开发中', type: 'info', grouping: true })
  }
}

function isMonitorRow(row) {
  const classify = row?.classify || row?.classifyCode || row?.deviceTypeCode
  if (classify) return classify === MONITOR_CLASSIFY
  return current.value?.code === MONITOR_CLASSIFY
}

function openFace(row) {
  faceDevice.value = row
  faceVisible.value = true
}

// ---- 操作日志 ----
const logList = ref([])
const logTotal = ref(0)
const logLoading = ref(false)
// title 固定为「设备-」，只取设备相关的操作日志
const logQuery = reactive({ pageNum: 1, pageSize: 10, title: '设备-' })

const logDetailVisible = ref(false)
const currentLog = ref(null)

function openLog() {
  view.value = 'log'
  if (!logList.value.length) loadLogs()
}

function loadLogs() {
  logLoading.value = true
  listDeviceOperLog({ pageNum: logQuery.pageNum, pageSize: logQuery.pageSize, title: logQuery.title })
    .then(res => {
      logList.value = res.rows || []
      logTotal.value = res.total || 0
    })
    .catch(() => {})
    .finally(() => { logLoading.value = false })
}

function showLogDetail(row) {
  currentLog.value = row
  logDetailVisible.value = true
}

// 美化 JSON 字符串（请求参数/返回结果），非 JSON 原样返回
function pretty(str) {
  if (str === null || str === undefined || str === '') return '—'
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch (e) {
    return str
  }
}

onMounted(() => {
  loadCategories()
  loadManufacturers()
})
</script>

<style scoped lang="scss">
.dc {
  flex: 1;
  display: flex;
  min-width: 0;
  min-height: 0;
}

/* 左侧导航 */
.dc__nav {
  flex: 0 0 180px;
  padding: 14px 10px;
  overflow: auto;
  background: #f6f8fb;
  border-right: 1px solid #eef1f5;
}

.dc__nav-group {
  padding: 6px 12px;
  margin-top: 6px;
  font-size: 12.5px;
  color: #9aa7b4;

  &:first-child {
    margin-top: 0;
  }
}

.dc__nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 42px;
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

.dc__nav-icon {
  font-size: 16px;
}

/* 右侧 */
.dc__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 18px 22px;
}

.dc__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.dc__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1f2d3d;
}

/* 概览卡片 */
.dc__cards {
  display: flex;
  gap: 16px;
  margin-bottom: 18px;
}

.dc-card {
  flex: 1;
  padding: 18px 20px;
  border-radius: 14px;
  background: #f6f9fe;
  border: 1px solid #eaf0f8;

  &__label {
    font-size: 14px;
    color: #6b7280;
  }

  &__num {
    margin: 8px 0 4px;
    font-size: 34px;
    font-weight: 700;
    line-height: 1.1;
    color: #1f2d3d;
  }

  &__sub {
    font-size: 12.5px;
    color: #9aa7b4;
  }

  &--warn {
    background: #fff8f0;
    border-color: #ffe6c7;

    .dc-card__num {
      color: #f5a23b;
    }
  }

  &--danger {
    background: #fff4f4;
    border-color: #ffd9d9;

    .dc-card__num {
      color: #f56c6c;
    }
  }
}

/* 设备列表 */
.dc__panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  padding: 14px 16px;
  border: 1px solid #eef1f5;
  border-radius: 10px;
  background: #fff;
}

.dc__panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.dc__panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16.5px;
  font-weight: 600;
  color: #1f2d3d;
}

.dc__filters {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dc__mfr {
  width: 150px;
}

.dc__search {
  width: 220px;
}

.dc__all-light {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 10px;
  border-left: 1px solid #e6eaf0;
}

.dc__all-light-label {
  font-size: 13px;
  color: #6b7280;
}

.dc__table {
  flex: 1;
  width: 100%;
  min-height: 0;
  font-size: 15px;
}

:deep(.dc__table th),
:deep(.dc__table .cell) {
  font-size: 15px;
}
</style>

<!-- 详情弹窗 append-to-body，需用非 scoped 样式 -->
<style lang="scss">
.log-detail-dialog {
  .log-pre {
    margin: 0;
    max-height: 220px;
    overflow: auto;
    padding: 8px 10px;
    border-radius: 6px;
    background: #f6f8fb;
    border: 1px solid #eef1f5;
    font-family: Consolas, Monaco, 'Courier New', monospace;
    font-size: 12.5px;
    line-height: 1.5;
    color: #3a4756;
    white-space: pre-wrap;
    word-break: break-all;
  }

  .log-err {
    color: #f56c6c;
    word-break: break-all;
  }
}
</style>
