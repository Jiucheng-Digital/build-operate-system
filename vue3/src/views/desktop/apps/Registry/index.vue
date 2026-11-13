<template>
  <AppWindow title="注册表" @close="$emit('close')" @minimize="$emit('minimize')">
    <div class="reg">
      <!-- 左侧树 -->
      <aside class="reg__tree" v-loading="loading">
        <el-tree
          ref="treeRef"
          :data="treeData"
          :props="treeProps"
          node-key="id"
          :expand-on-click-node="false"
          highlight-current
          @node-click="onNodeClick"
        >
          <template #default="{ node, data }">
            <span class="reg-node">
              <el-icon class="reg-node__icon" :class="`is-${data.type}`">
                <Folder v-if="data.type === 'classify'" />
                <Cpu v-else-if="data.type === 'driver'" />
                <Document v-else />
              </el-icon>
              <span class="reg-node__label" :title="node.label">{{ node.label }}</span>
            </span>
          </template>
        </el-tree>
      </aside>

      <!-- 右侧内容 -->
      <section class="reg__main">
        <!-- 驱动信息（直接展示驱动表数据） -->
        <template v-if="isDrive">
          <header class="reg-detail__head">
            <h2 class="reg-detail__title">{{ detailTitle }}</h2>
          </header>
          <el-table :data="driveInfoRows" border class="reg-table">
            <el-table-column label="属性名称" prop="label" width="220" />
            <el-table-column label="值" prop="value" show-overflow-tooltip>
              <template #default="{ row }">
                <el-tag v-if="row.key === 'status'" :type="row.tagType" size="small" effect="light">{{ row.value }}</el-tag>
                <span v-else>{{ row.value || '—' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <!-- 配置信息 -->
        <template v-else-if="isConfig">
          <header class="reg-detail__head">
            <h2 class="reg-detail__title">{{ detailTitle }}</h2>
            <el-button type="primary" :icon="Edit" @click="openConfigEdit">编辑</el-button>
          </header>
          <el-table v-loading="configLoading" :data="configList" border class="reg-table">
            <el-table-column label="属性名称" prop="label" width="220" />
            <el-table-column label="值" prop="value" show-overflow-tooltip>
              <template #default="{ row }"><span :title="row.key === 'frontend.protocol' ? '前端展示值，未写入后端' : undefined">{{ row.value || '—' }}</span></template>
            </el-table-column>
          </el-table>
        </template>

        <!-- 设备信息 -->
        <template v-else-if="isDevice">
          <header class="reg-detail__head">
            <h2 class="reg-detail__title">{{ detailTitle }}</h2>
            <el-button :icon="Refresh" @click="loadDevices">刷新</el-button>
          </header>
          <el-table v-loading="deviceLoading" :data="deviceList" border class="reg-table reg-device-table">
            <el-table-column label="设备ID" prop="serialNumber" width="120" show-overflow-tooltip />
            <el-table-column label="设备名称" prop="name" min-width="150" show-overflow-tooltip />
            <el-table-column label="驱动ID" prop="driveId" width="180" show-overflow-tooltip />
            <el-table-column label="所在位置" prop="address" min-width="120" show-overflow-tooltip />
            <el-table-column label="状态" prop="status" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'online' ? 'success' : 'info'" size="small" effect="light">
                  {{ row.status === 'online' ? '在线' : (row.status === 'offline' ? '离线' : (row.status || '—')) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="首次注册时间" prop="createTime" width="160" align="center" />
            <el-table-column label="最后修改日期" prop="updateTime" width="160" align="center" />
            <el-table-column label="离线时间" prop="offlineTime" width="160" align="center">
              <template #default="{ row }">{{ row.offlineTime || '—' }}</template>
            </el-table-column>
<!--            <el-table-column label="操作" width="120" align="center" fixed="right">-->
<!--              <template #default="{ row }">-->
<!--                <el-button link type="primary" size="small" @click="onDeviceEdit(row)">编辑</el-button>-->
<!--                <el-button link type="danger" size="small" @click="onDeviceDelete(row)">删除</el-button>-->
<!--              </template>-->
<!--            </el-table-column>-->
          </el-table>
          <pagination
            v-show="deviceTotal > 0"
            :total="deviceTotal"
            v-model:page="deviceQuery.pageNum"
            v-model:limit="deviceQuery.pageSize"
            :page-sizes="[10, 20, 30, 50]"
            @pagination="loadDevices"
          />
        </template>

        <!-- 服务信息 -->
        <template v-else-if="isService">
          <header class="reg-detail__head">
            <h2 class="reg-detail__title">{{ detailTitle }}</h2>
            <el-button :icon="Refresh" @click="loadService">刷新</el-button>
          </header>
          <el-table v-loading="serviceLoading" :data="serviceList" border class="reg-table">
            <el-table-column label="请求方式" width="92" align="center">
              <template #default="{ row }">
                <el-tag :type="(row.method || '').toLowerCase() === 'get' ? 'success' : 'warning'" size="small" effect="plain">
                  {{ (row.method || '—').toUpperCase() }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="服务名称" prop="name" min-width="160" show-overflow-tooltip />
            <el-table-column label="方法名" prop="methodName" min-width="150" show-overflow-tooltip />
            <el-table-column label="请求路径" prop="path" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">{{ row.path || '—' }}</template>
            </el-table-column>
            <el-table-column label="包名" prop="packageName" min-width="190" show-overflow-tooltip />
            <el-table-column label="可用" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.access ? 'success' : 'info'" size="small" effect="light">{{ row.access ? '可用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="详情" width="80" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="showServiceDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <!-- 其它节点：占位 -->
        <div v-else class="reg-empty">
          <el-icon class="reg-empty__icon"><Files /></el-icon>
          <p class="reg-empty__title">{{ current ? current.label : '注册表' }}</p>
          <span class="reg-empty__tip">选择左侧驱动下的「驱动信息 / 配置信息 / 设备信息 / 服务信息」查看详情</span>
        </div>
      </section>
    </div>
  </AppWindow>

  <el-dialog
    v-model="configEditVisible"
    title="编辑登录配置"
    width="520px"
    append-to-body
    :close-on-click-modal="false"
  >
    <el-form label-position="top" class="reg-config-form">
      <el-form-item v-for="field in configLoginFields" :key="field.key" :label="field.label">
        <el-input-number
          v-if="field.key === 'port'"
          v-model="configEditForm[field.key]"
          :min="0"
          :max="65535"
          controls-position="right"
          style="width: 100%"
        />
        <el-input
          v-else
          v-model="configEditForm[field.key]"
          :type="isSecretLoginField(field.key) ? 'password' : 'text'"
          :show-password="isSecretLoginField(field.key)"
          clearable
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="configEditVisible = false">取消</el-button>
      <el-button type="primary" :loading="configSaving" @click="saveConfig">确定</el-button>
    </template>
  </el-dialog>

  <!-- 服务详情 -->
  <el-dialog
    v-model="serviceDetailVisible"
    :title="currentService ? currentService.name : '服务详情'"
    width="680px"
    align-center
    append-to-body
    class="svc-detail-dialog"
  >
    <template v-if="currentService">
      <el-descriptions :column="2" border size="small" class="svc-meta">
        <el-descriptions-item label="方法名">{{ currentService.methodName }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ (currentService.method || '—').toUpperCase() }}</el-descriptions-item>
        <el-descriptions-item label="类名">{{ currentService.className }}</el-descriptions-item>
        <el-descriptions-item label="可用">{{ currentService.access ? '可用' : '禁用' }}</el-descriptions-item>
        <el-descriptions-item label="请求路径" :span="2">{{ currentService.path || '—' }}</el-descriptions-item>
        <el-descriptions-item label="包名" :span="2">{{ currentService.packageName }}</el-descriptions-item>
      </el-descriptions>

      <div class="svc-sec-title">请求参数</div>
      <el-table v-if="currentService.params && currentService.params.length" :data="currentService.params" border size="small"
        row-key="_id" default-expand-all :tree-props="{ children: 'data' }">
        <el-table-column label="参数名" prop="name" min-width="150" show-overflow-tooltip />
        <el-table-column label="类型" prop="type" width="90" align="center" />
        <el-table-column label="必填" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.required != null" :type="row.required ? 'danger' : 'info'" size="small" effect="plain">{{ row.required ? '是' : '否' }}</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="description" min-width="200" show-overflow-tooltip />
      </el-table>
      <div v-else class="svc-empty">无参数</div>

      <div class="svc-sec-title">返回结果</div>
      <el-table v-if="currentService.result && currentService.result.length" :data="currentService.result" border size="small"
        row-key="_id" default-expand-all :tree-props="{ children: 'data' }">
        <el-table-column label="名称" prop="name" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" prop="type" width="90" align="center" />
        <el-table-column label="说明" prop="description" min-width="220" show-overflow-tooltip />
      </el-table>
      <div v-else class="svc-empty">无返回说明</div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Folder, Cpu, Document, Files, Edit, Refresh } from '@element-plus/icons-vue'
import AppWindow from '../../components/AppWindow.vue'
import { definedProtocolForDriver, protocolRowForDriver } from './protocol'
import { listClassify, listByClassify, getDriveConfig, updateDriveConfig, getDevicePage, getDriveApiList } from '@/api/build/drive'
import { getDicts } from '@/api/system/dict/data'

defineEmits(['close', 'minimize'])

const treeProps = { label: 'label', children: 'children' }
const treeRef = ref(null)
const treeData = ref([])
const loading = ref(false)
const current = ref(null)

// 配置信息
const configList = ref([])
const configLoading = ref(false)
const configData = ref({ login: {}, configs: [] })
const configEditVisible = ref(false)
const configSaving = ref(false)
const configLoginFields = ref([])
const configEditForm = reactive({})
let configRequestId = 0

// 设备信息（分页）
const deviceList = ref([])
const deviceTotal = ref(0)
const deviceLoading = ref(false)
const deviceQuery = reactive({ pageNum: 1, pageSize: 10, manufacturer: undefined, classify: undefined })

// 服务信息
const serviceList = ref([])
const serviceLoading = ref(false)
const serviceDetailVisible = ref(false)
const currentService = ref(null)

// 厂商字典（dictValue:hikvision -> dictLabel:海康），与设备中心一致
const manufacturerOptions = ref([])
const manufacturerMap = computed(() => {
  const map = {}
  manufacturerOptions.value.forEach(m => { map[m.dictValue] = m.dictLabel })
  return map
})
function manufacturerLabel(val) {
  if (!val) return '—'
  return manufacturerMap.value[val] || val
}

const isDrive = computed(() => current.value && current.value.type === 'info' && current.value.infoKey === 'drive')
const isService = computed(() => current.value && current.value.type === 'info' && current.value.infoKey === 'service')
const isConfig = computed(() => current.value && current.value.type === 'info' && current.value.infoKey === 'config')
const isDevice = computed(() => current.value && current.value.type === 'info' && current.value.infoKey === 'device')

const LOGIN_CONFIG_FIELDS = [
  { key: 'ip', label: '登录IP' },
  { key: 'port', label: '登录端口' },
  { key: 'username', label: '登录用户名' },
  { key: 'password', label: '登录密码' }
]

const LOGIN_CONFIG_ORDER = ['ip', 'port', 'host', 'username', 'password', 'key', 'secret']
const LOGIN_CONFIG_LABELS = {
  ip: '登录IP',
  port: '登录端口',
  host: '登录地址',
  username: '登录用户名',
  password: '登录密码',
  key: 'Key',
  secret: 'Secret'
}

const DRIVE_STATUS = {
  install_success: { text: '已安装', tag: 'success' },
  uninstall: { text: '未安装', tag: 'info' },
  install_error: { text: '安装失败', tag: 'danger' },
  installing: { text: '安装中', tag: 'warning' }
}

// 驱动信息：直接取树节点里的驱动对象（驱动表数据），无需再请求
const driveInfoRows = computed(() => {
  if (!isDrive.value) return []
  const d = current.value.driver || {}
  const st = DRIVE_STATUS[d.status] || { text: d.status || '—', tag: 'info' }
  return [
    { key: 'name', label: '驱动名称', value: d.name },
    { key: 'version', label: '版本', value: d.version },
    { key: 'type', label: '厂商', value: manufacturerLabel(d.type) },
    { key: 'classifyName', label: '所属分类', value: d.classifyName },
    { key: 'classifyCode', label: '类型码', value: d.classifyCode },
    { key: 'status', label: '状态', value: st.text, tagType: st.tag },
    { key: 'installPath', label: '安装路径', value: d.installPath },
    { key: 'url', label: '访问地址', value: d.url },
    { key: 'port', label: '端口', value: d.port },
    { key: 'remark', label: '描述', value: d.remark },
    { key: 'createBy', label: '创建人', value: d.createBy },
    { key: 'createTime', label: '创建时间', value: d.createTime },
    { key: 'updateBy', label: '更新人', value: d.updateBy },
    { key: 'updateTime', label: '更新时间', value: d.updateTime },
    { key: 'id', label: '驱动ID', value: d.id }
  ]
})

const detailTitle = computed(() => {
  const node = current.value
  if (!node || node.type !== 'info') return ''
  const d = node.driver
  const cls = node.classifyName
  const ver = d.version ? ' ' + d.version : ''
  return `${node.label} - ${d.name}${ver}${cls ? ' - ' + cls : ''}`
})

// 驱动节点下写死的四个子节点
const INFO_NODES = [
  { key: 'drive', label: '驱动信息' },
  { key: 'device', label: '设备信息' },
  { key: 'config', label: '配置信息' },
  { key: 'service', label: '服务信息' }
]

function buildDriverNode(d, classifyName) {
  return {
    id: 'd-' + d.id,
    label: d.name,
    type: 'driver',
    driverId: d.id,
    raw: d,
    classifyName,
    children: INFO_NODES.map(info => ({
      id: 'i-' + d.id + '-' + info.key,
      label: info.label,
      type: 'info',
      infoKey: info.key,
      driverId: d.id,
      driver: d,
      classifyName // 用于右侧标题展示分类名
    }))
  }
}

// 递归构建分类节点：有子分类则递归；叶子分类则拉驱动；无子节点则不给 children（无箭头）
async function buildClassifyNode(c, list) {
  const node = { id: 'c-' + c.code, label: c.name, type: 'classify', code: c.code, raw: c }
  const subs = list.filter(x => x.parentCode === c.code)
  if (subs.length) {
    node.children = await Promise.all(subs.map(s => buildClassifyNode(s, list)))
  } else {
    const drives = await listByClassify(c.code).then(r => r.data || []).catch(() => [])
    if (drives.length) {
      node.children = drives.map(d => buildDriverNode(d, c.name))
    }
  }
  return node
}

async function buildTree() {
  loading.value = true
  try {
    const res = await listClassify()
    const list = res.data || []
    const roots = list.filter(c => c.parentCode === '0')
    treeData.value = await Promise.all(roots.map(c => buildClassifyNode(c, list)))
  } catch (e) {
    treeData.value = []
  } finally {
    loading.value = false
  }
}

function onNodeClick(data) {
  current.value = data
  if (data.type !== 'info') return
  if (data.infoKey === 'config') {
    loadConfig(data.driver)
  } else if (data.infoKey === 'device') {
    deviceQuery.manufacturer = data.driver.type
    deviceQuery.classify = data.driver.classifyCode
    deviceQuery.pageNum = 1
    loadDevices()
  } else if (data.infoKey === 'service') {
    loadService()
  }
}

// 给参数/返回结构递归补唯一 _id（el-table 树形需要 row-key）
let svcSeq = 0
function withTreeKeys(arr) {
  if (!Array.isArray(arr)) return arr
  arr.forEach(item => {
    item._id = 's' + (svcSeq++)
    if (Array.isArray(item.data)) withTreeKeys(item.data)
  })
  return arr
}

function loadService() {
  const driver = current.value && current.value.driver
  const driveId = driver && (driver.id || driver.driveId)
  if (!driveId) {
    serviceList.value = []
    ElMessage.warning('缺少驱动ID，无法获取服务信息')
    return
  }
  serviceLoading.value = true
  getDriveApiList(driveId)
    .then(res => {
      const list = res.data || []
      list.forEach(s => {
        withTreeKeys(s.params)
        withTreeKeys(s.result)
      })
      serviceList.value = list
    })
    .catch(() => {})
    .finally(() => { serviceLoading.value = false })
}

function showServiceDetail(row) {
  currentService.value = row
  serviceDetailVisible.value = true
}

function loadConfig(driver) {
  const requestId = ++configRequestId
  const protocolRow = protocolRowForDriver(driver)
  const driveId = driver && (driver.id || driver.driveId)
  if (!driveId) {
    configLoading.value = false
    configData.value = { login: {}, configs: [] }
    configList.value = [protocolRow]
    ElMessage.warning('缺少驱动ID，无法获取配置信息')
    return
  }
  configLoading.value = true
  configData.value = { login: {}, configs: [] }
  configList.value = [protocolRow]
  getDriveConfig(driveId)
    .then(res => {
      if (requestId !== configRequestId) return
      configData.value = normalizeConfigData(res.data)
      configList.value = buildConfigRows(configData.value, driver)
    })
    .catch(() => {})
    .finally(() => { if (requestId === configRequestId) configLoading.value = false })
}

function normalizeConfigData(data) {
  if (Array.isArray(data)) return { login: {}, configs: data }
  if (!data || typeof data !== 'object') return { login: {}, configs: [] }
  return {
    login: data.login && typeof data.login === 'object' ? { ...data.login } : {},
    configs: Array.isArray(data.configs) ? data.configs : []
  }
}

function getLoginFields(login) {
  const keys = Object.keys(login || {})
  const orderedKeys = [
    ...LOGIN_CONFIG_ORDER.filter(key => keys.includes(key)),
    ...keys.filter(key => !LOGIN_CONFIG_ORDER.includes(key))
  ]
  return orderedKeys.map(key => ({ key, label: LOGIN_CONFIG_LABELS[key] || key }))
}

function buildConfigRows(data, driver) {
  const login = data.login || {}
  const protocol = definedProtocolForDriver(driver)
  const loginRows = getLoginFields(login).map(item => ({
    key: `login.${item.key}`,
    label: item.label,
    value: protocol && item.key.toLowerCase() === 'protocol' ? protocol : login[item.key]
  }))
  const configs = Array.isArray(data.configs) ? data.configs : []
  const isProtocolConfig = row => row?.key?.toLowerCase() === 'protocol' || row?.label === '协议'
  const displayConfigs = protocol
    ? configs.map(row => isProtocolConfig(row) ? { ...row, value: protocol } : row)
    : configs
  const existingProtocol = configs.some(isProtocolConfig) ||
    Object.keys(login).some(key => key.toLowerCase() === 'protocol')
  return [...loginRows, ...(existingProtocol ? [] : [protocolRowForDriver(driver)]), ...displayConfigs]
}

function isSecretLoginField(key) {
  return ['password', 'secret'].includes(key)
}

function loadDevices() {
  deviceLoading.value = true
  getDevicePage(deviceQuery)
    .then(res => {
      deviceList.value = res.rows || []
      deviceTotal.value = res.total || 0
    })
    .catch(() => {})
    .finally(() => { deviceLoading.value = false })
}

function onEdit() {
  ElMessage({ message: '编辑功能开发中', type: 'info', grouping: true })
}

function openConfigEdit() {
  const login = configData.value.login || {}
  configLoginFields.value = getLoginFields(login)
  if (!configLoginFields.value.length) {
    ElMessage.warning('当前配置没有可编辑的登录信息')
    return
  }
  Object.keys(configEditForm).forEach(key => delete configEditForm[key])
  configLoginFields.value.forEach(field => { configEditForm[field.key] = login[field.key] })
  configEditVisible.value = true
}

function getCurrentDriveId() {
  const driver = current.value && current.value.driver
  return driver && (driver.id || driver.driveId)
}

function normalizeLoginValue(key, value) {
  const originalValue = configData.value.login && configData.value.login[key]
  if (typeof originalValue === 'number') {
    if (value === '' || value === null || value === undefined) return value
    const numberValue = Number(value)
    return Number.isNaN(numberValue) ? value : numberValue
  }
  return value
}

function saveConfig() {
  const driveId = getCurrentDriveId()
  if (!driveId) {
    ElMessage.warning('缺少驱动ID，无法保存配置')
    return
  }

  const login = { ...(configData.value.login || {}) }
  configLoginFields.value.forEach(field => {
    login[field.key] = normalizeLoginValue(field.key, configEditForm[field.key])
  })
  const configJson = JSON.stringify({
    login,
    configs: Array.isArray(configData.value.configs) ? configData.value.configs : []
  })

  configSaving.value = true
  updateDriveConfig({ driveId, configJson })
    .then(() => {
      ElMessage.success('保存成功')
      configEditVisible.value = false
      if (current.value && current.value.driver) loadConfig(current.value.driver)
    })
    .finally(() => { configSaving.value = false })
}

function onDeviceEdit() {
  ElMessage({ message: '设备编辑功能开发中', type: 'info', grouping: true })
}

function onDeviceDelete() {
  ElMessage({ message: '设备删除功能开发中', type: 'info', grouping: true })
}

onMounted(() => {
  buildTree()
  getDicts('drive_manufacturer').then(res => { manufacturerOptions.value = res.data || [] }).catch(() => {})
})
</script>

<style scoped lang="scss">
.reg {
  flex: 1;
  display: flex;
  min-width: 0;
  min-height: 0;
}

/* 左侧树 */
.reg__tree {
  flex: 0 0 290px;
  padding: 12px 8px;
  overflow: auto;
  background: #f6f8fb;
  border-right: 1px solid #eef1f5;
}

.reg-node {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  font-size: 15.5px;
}

.reg-node__icon {
  flex: 0 0 auto;
  font-size: 17px;

  &.is-classify {
    color: #f0a23b;
  }

  &.is-driver {
    color: #2f74ff;
  }

  &.is-info {
    color: #8a94a3;
  }
}

.reg-node__label {
  min-width: 0;
  color: #41454b;
  line-height: 1.4;
  white-space: normal;
  overflow-wrap: anywhere;
}

:deep(.el-tree-node__content) {
  min-height: 42px;
  height: auto;
  border-radius: 6px;
}

/* 右侧内容 */
.reg__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 18px 24px;
  overflow: auto;
}

.reg-detail__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.reg-detail__title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
}

.reg-table {
  font-size: 15px;
}

:deep(.reg-table th) {
  background: #f4f6f9;
  color: #5b6b7c;
  font-weight: 600;
}

:deep(.reg-table .cell) {
  font-size: 15px;
  padding: 4px 14px;
}

.reg-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9aa7b4;
}

.reg-empty__icon {
  font-size: 52px;
  margin-bottom: 12px;
}

.reg-empty__title {
  margin: 0 0 6px;
  font-size: 17px;
  font-weight: 600;
  color: #5b6b7c;
}

.reg-empty__tip {
  font-size: 13px;
}

/* 服务详情 */
:deep(.svc-detail-dialog .el-dialog__body) {
  max-height: 66vh;
  overflow-y: auto;
}

.svc-meta {
  margin-bottom: 6px;
}

.svc-sec-title {
  margin: 16px 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2d3d;
  padding-left: 8px;
  border-left: 3px solid #2f74ff;
}

.svc-empty {
  padding: 10px 0;
  color: #9aa7b4;
  font-size: 13px;
}
</style>
