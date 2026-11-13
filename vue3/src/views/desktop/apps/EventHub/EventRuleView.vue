<template>
  <div class="rule-page">
    <header class="rule-page__head">
      <div>
        <h2>事件规则</h2>
        <p>按事件来源和级别配置告警流转规则</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="createVisible = true">新增规则</el-button>
    </header>

    <div class="rule-panel">
      <div class="rule-panel__head">
        <span class="rule-panel__title"><el-icon><List /></el-icon>规则列表</span>
        <div class="rule-filters">
          <el-select
            v-model="query.status"
            class="rule-filter__select"
            placeholder="全部状态"
            clearable
            @change="search"
          >
            <el-option v-for="item in RULE_STATUSES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>

          <el-select
            v-model="query.eventLevel"
            class="rule-filter__select"
            placeholder="事件级别"
            clearable
            @change="search"
          >
            <el-option v-for="item in EVENT_LEVELS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>

          <el-input
            v-model="query.ruleName"
            class="rule-filter__search"
            placeholder="请输入规则名称"
            clearable
            :prefix-icon="Search"
            @keyup.enter="search"
            @clear="search"
          />
          <el-button :icon="Search" @click="search">查询</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="ruleList" class="rule-table" height="100%">
        <el-table-column label="规则名称" prop="ruleName" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="rule-name">{{ row.ruleName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="事件级别" prop="eventLevel" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="levelType(row.eventLevel)" size="small" effect="light">
              {{ levelLabel(row.eventLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规则描述" prop="ruleDesc" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ row.ruleDesc || '—' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="170" show-overflow-tooltip />
        <el-table-column label="状态" prop="status" width="100" align="center">
          <template #default="{ row }">
            <span class="rule-status" :class="`is-${row.status || 'unknown'}`">
              <i></i>{{ statusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              :type="statusAction(row.status).type"
              size="small"
              :icon="statusAction(row.status).icon"
              :loading="statusChangingId === String(row.id)"
              @click="changeRuleStatus(row)"
            >
              {{ statusAction(row.status).label }}
            </el-button>
            <el-button link type="primary" size="small" :icon="View" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :page-sizes="[10, 20, 30, 50]"
        @pagination="loadRules"
      />
    </div>

    <EventRuleDialog v-model="createVisible" @success="created" />

    <el-dialog
      v-model="detailVisible"
      width="760px"
      align-center
      append-to-body
      :show-close="false"
      class="rule-detail-dialog"
    >
      <template #header>
        <div class="detail-dialog__header">
          <div>
            <span>规则详情</span>
            <small>查看规则配置、触发条件和执行目标</small>
          </div>
          <button type="button" aria-label="关闭规则详情" @click="detailVisible = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </template>

      <div v-if="detailRow" class="detail-content">
        <section class="detail-overview">
          <div class="detail-overview__main">
            <span class="detail-rule-id">规则 ID：{{ detailRow.id || '—' }}</span>
            <h3>{{ detailRow.ruleName || '未命名规则' }}</h3>
            <p>{{ detailRow.ruleDesc || '暂无规则描述' }}</p>
          </div>
          <div class="detail-overview__badges">
            <span class="detail-badge" :class="`is-level-${detailRow.eventLevel}`">
              {{ levelLabel(detailRow.eventLevel) }}级事件
            </span>
            <span class="detail-badge" :class="`is-status-${detailRow.status}`">
              {{ statusLabel(detailRow.status) }}
            </span>
          </div>
        </section>

        <div class="detail-meta-grid">
          <div class="detail-meta-item">
            <el-icon><Connection /></el-icon>
            <div>
              <span>事件来源</span>
              <strong>{{ detailRow.eventSourceName || detailRow.eventSource || '—' }}</strong>
            </div>
          </div>
          <div class="detail-meta-item">
            <el-icon><User /></el-icon>
            <div>
              <span>创建人</span>
              <strong>{{ detailRow.createBy || '—' }}</strong>
            </div>
          </div>
          <div class="detail-meta-item">
            <el-icon><Clock /></el-icon>
            <div>
              <span>创建时间</span>
              <strong>{{ detailRow.createTime || '—' }}</strong>
            </div>
          </div>
          <div class="detail-meta-item">
            <el-icon><User /></el-icon>
            <div>
              <span>更新人</span>
              <strong>{{ detailRow.updateBy || '—' }}</strong>
            </div>
          </div>
          <div class="detail-meta-item">
            <el-icon><Clock /></el-icon>
            <div>
              <span>更新时间</span>
              <strong>{{ detailRow.updateTime || '—' }}</strong>
            </div>
          </div>
        </div>

        <section class="detail-card">
          <div class="detail-card__head">
            <div class="detail-card__icon is-trigger"><el-icon><Bell /></el-icon></div>
            <div>
              <h4>触发条件</h4>
              <p>条件关系：{{ relationLabel(detailRow.conditionRelation) }}</p>
            </div>
          </div>

          <div v-if="detailConditions.length" class="condition-list">
            <div v-for="(condition, index) in detailConditions" :key="index" class="condition-item">
              <span class="condition-item__index">{{ index + 1 }}</span>
              <div>
                <strong>{{ conditionKeyLabel(condition.key) }}</strong>
                <span>{{ condition.condition || '—' }} {{ condition.value ?? '—' }}{{ conditionUnit(condition.key) }}</span>
              </div>
            </div>
          </div>
          <div v-else class="detail-empty">该规则未配置触发条件</div>
        </section>

        <section class="detail-card">
          <div class="detail-card__head">
            <div class="detail-card__icon is-action"><el-icon><OfficeBuilding /></el-icon></div>
            <div>
              <h4>执行动作</h4>
              <p>{{ actionTypeLabel(detailRow.actionType) }}</p>
            </div>
          </div>

          <div class="department-list">
            <span v-for="(name, index) in detailDepartmentNames" :key="`${name}-${index}`">{{ name }}</span>
          </div>
        </section>

        <section v-if="detailRow.remark" class="detail-remark">
          <strong>备注</strong>
          <p>{{ detailRow.remark }}</p>
        </section>
      </div>

      <template #footer>
        <el-button type="primary" @click="detailVisible = false">确定</el-button>
        <el-button @click="detailVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Bell, Clock, Close, Connection, List, OfficeBuilding, Plus, Search, SwitchButton, TurnOff, User, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { editEventRuleStatus, getEventRulePage } from '@/api/build/event'
import { listDept } from '@/api/system/dept'
import EventRuleDialog from './EventRuleDialog.vue'
import { triggerOptionByKey } from './eventRuleTriggers'

const RULE_STATUSES = [
  { value: 'enable', label: '启用' },
  { value: 'disable', label: '未启用' }
]

const EVENT_LEVELS = [
  { value: 'low', label: '低' },
  { value: 'middle', label: '中' },
  { value: 'high', label: '高' }
]

const loading = ref(false)
const ruleList = ref([])
const total = ref(0)
const createVisible = ref(false)
const detailVisible = ref(false)
const detailRow = ref(null)
const deptList = ref([])
const statusChangingId = ref('')

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined,
  eventLevel: undefined,
  ruleName: ''
})

function requestParams() {
  const params = {
    pageNum: query.pageNum,
    pageSize: query.pageSize
  }
  if (query.status) params.status = query.status
  if (query.eventLevel) params.eventLevel = query.eventLevel
  if (query.ruleName.trim()) params.ruleName = query.ruleName.trim()
  return params
}

function loadRules() {
  loading.value = true
  getEventRulePage(requestParams())
    .then(response => {
      ruleList.value = response.rows || []
      total.value = Number(response.total) || 0
    })
    .catch(() => {})
    .finally(() => {
      loading.value = false
    })
}

function search() {
  query.pageNum = 1
  loadRules()
}

function created() {
  query.pageNum = 1
  loadRules()
}

function showDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

function statusAction(status) {
  if (status === 'disable') {
    return { label: '启用', type: 'primary', icon: SwitchButton }
  }
  return { label: '禁用', type: 'danger', icon: TurnOff }
}

async function changeRuleStatus(row) {
  if (!row?.id || statusChangingId.value) return

  const nextStatus = row.status === 'disable' ? 'enable' : 'disable'
  const action = statusAction(row.status)
  statusChangingId.value = String(row.id)

  try {
    await editEventRuleStatus({
      id: row.id,
      status: nextStatus
    })
    row.status = nextStatus
    ElMessage.success(`${action.label}成功`)
  } finally {
    statusChangingId.value = ''
  }
}

function levelLabel(value) {
  return EVENT_LEVELS.find(item => item.value === value)?.label || value || '—'
}

function levelType(value) {
  if (value === 'high') return 'danger'
  if (value === 'middle') return 'warning'
  return 'primary'
}

function statusLabel(value) {
  return RULE_STATUSES.find(item => item.value === value)?.label || value || '—'
}

function parseArray(value) {
  if (Array.isArray(value)) return value
  if (!value) return []
  try {
    const result = JSON.parse(value)
    return Array.isArray(result) ? result : []
  } catch (_error) {
    return []
  }
}

function relationLabel(value) {
  if (value === 'or') return '任一条件满足'
  return '全部条件满足'
}

function conditionKeyLabel(value) {
  return triggerOptionByKey(value)?.label || value || '未知条件'
}

function conditionUnit(value) {
  return triggerOptionByKey(value)?.unit || ''
}

function actionTypeLabel(value) {
  return value === 'dept' ? '流转部门' : (value || '未配置执行动作')
}

const detailConditions = computed(() => {
  return detailRow.value ? parseArray(detailRow.value.triggerConditions) : []
})

const detailDepartmentNames = computed(() => {
  if (!detailRow.value) return []

  const targets = parseArray(detailRow.value.actionTargets)
  if (!targets.length) return ['未配置流转部门']
  if (!deptList.value.length) return ['部门信息加载中']

  return targets.map(target => {
    const dept = deptList.value.find(item => String(item.deptId) === String(target))
    return dept?.deptName || '未知部门'
  })
})

function loadDepartments() {
  listDept()
    .then(response => {
      deptList.value = response.data || []
    })
    .catch(() => {})
}

onMounted(() => {
  loadRules()
  loadDepartments()
})
</script>

<style scoped lang="scss">
.rule-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.rule-page__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    color: #1f2d3d;
    font-size: 22px;
    font-weight: 700;
  }

  p {
    margin: 5px 0 0;
    color: #96a1ae;
    font-size: 13px;
  }
}

.rule-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 14px 16px;
  overflow: hidden;
  border: 1px solid #e9eef5;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 6px 20px rgba(35, 69, 111, 0.05);
}

.rule-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.rule-panel__title {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #1f2d3d;
  font-size: 16px;
  font-weight: 600;

  .el-icon {
    color: #2f74ff;
  }
}

.rule-filters {
  display: flex;
  align-items: center;
  gap: 9px;
}

.rule-filter__select {
  width: 125px;
}

.rule-filter__search {
  width: 210px;
}

.rule-table {
  flex: 1;
  width: 100%;
  min-height: 0;
}

.rule-name {
  color: #24364b;
  font-weight: 600;
}

.rule-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #7c8896;
  font-size: 13px;

  i {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #aeb7c2;
  }

  &.is-enable {
    color: #38a169;
  }

  &.is-enable i {
    background: #42bd77;
    box-shadow: 0 0 0 3px rgba(66, 189, 119, 0.12);
  }

  &.is-disable {
    color: #f56c6c;
  }

  &.is-disable i {
    background: #f56c6c;
    box-shadow: 0 0 0 3px rgba(245, 108, 108, 0.1);
  }
}

:deep(.rule-table th.el-table__cell) {
  background: #f8fafd;
  color: #718096;
  font-weight: 600;
}

:deep(.rule-table td.el-table__cell) {
  padding: 13px 0;
}

:global(.rule-detail-dialog) {
  overflow: hidden;
  border-radius: 12px;
}

:global(.rule-detail-dialog .el-dialog__header) {
  padding: 0;
  margin: 0;
}

:global(.rule-detail-dialog .el-dialog__body) {
  max-height: calc(100vh - 180px);
  padding: 22px 26px 12px;
  overflow-y: auto;
  background: #f6f8fb;
}

:global(.rule-detail-dialog .el-dialog__footer) {
  padding: 12px 32px 24px;
}

.detail-dialog__header {
  width: 100%;
  min-width: 0;
  min-height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0;
  > div {
    min-width: 0;
  }

  > div > span {
    display: block;
    color: #1f2d3d;
    font-size: 17px;
    font-weight: 700;
  }

  small {
    display: block;
    margin-top: 3px;
    color: #98a2ae;
    font-size: 13px;
    font-weight: 400;
  }

  button {
    flex: 0 0 32px;
    width: 32px;
    height: 32px;
    display: grid;
    place-items: center;
    border: 0;
    border-radius: 50%;
    background: #f1f2f4;
    color: #e05b55;
    margin-left: auto;
    cursor: pointer;

    &:hover {
      background: #fdebea;
    }
  }
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-overview {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 22px;
  border: 1px solid #dfe9fb;
  border-radius: 14px;
  background: linear-gradient(135deg, #ffffff 0%, #f0f6ff 100%);
}

.detail-overview__main {
  min-width: 0;

  h3 {
    margin: 7px 0 6px;
    color: #1e2d42;
    font-size: 20px;
    line-height: 1.35;
  }

  p {
    margin: 0;
    color: #6f7d8d;
    font-size: 13px;
    line-height: 1.7;
  }
}

.detail-rule-id {
  color: #8b98a7;
  font-size: 13px;
}

.detail-overview__badges {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 4px 10px;
  border-radius: 14px;
  background: #edf2f7;
  color: #687586;
  font-size: 13px;
  font-weight: 600;

  &.is-level-low {
    background: #eaf3ff;
    color: #3679d6;
  }

  &.is-level-middle {
    background: #fff4df;
    color: #d58a13;
  }

  &.is-level-high {
    background: #ffebeb;
    color: #e45656;
  }

  &.is-status-enable {
    background: #e9f8f0;
    color: #2da567;
  }

  &.is-status-disable {
    background: #f1f2f4;
    color: #7f8995;
  }
}

.detail-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.detail-meta-item {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid #e7ebf1;
  border-radius: 10px;
  background: #fff;

  > .el-icon {
    flex: 0 0 auto;
    width: 32px;
    height: 32px;
    border-radius: 9px;
    background: #edf4ff;
    color: #3978db;
  }

  > div {
    min-width: 0;
  }

  span,
  strong {
    display: block;
  }

  span {
    margin-bottom: 3px;
    color: #98a2ae;
    font-size: 13px;
  }

  strong {
    overflow: hidden;
    color: #354255;
    font-size: 13px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.detail-card {
  padding: 17px 18px;
  border: 1px solid #e5eaf1;
  border-radius: 12px;
  background: #fff;
}

.detail-card__head {
  display: flex;
  align-items: center;
  gap: 11px;

  h4,
  p {
    margin: 0;
  }

  h4 {
    color: #263548;
    font-size: 15px;
  }

  p {
    margin-top: 3px;
    color: #929daa;
    font-size: 13px;
  }
}

.detail-card__icon {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 10px;

  &.is-trigger {
    background: #fff3dd;
    color: #dc8b16;
  }

  &.is-action {
    background: #eaf2ff;
    color: #3478dd;
  }
}

.condition-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.condition-item {
  min-width: 180px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 9px;
  background: #f7f9fc;

  > div span,
  > div strong {
    display: block;
  }

  strong {
    color: #344256;
    font-size: 13px;
  }

  > div span {
    margin-top: 2px;
    color: #778493;
    font-size: 13px;
  }
}

.condition-item__index {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #fff0d4;
  color: #d78510;
  font-size: 13px;
  font-weight: 700;
}

.detail-empty {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f7f9fc;
  color: #929daa;
  font-size: 13px;
}

.department-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;

  span {
    padding: 7px 12px;
    border: 1px solid #cfe0fb;
    border-radius: 16px;
    background: #f1f6ff;
    color: #326fc8;
    font-size: 13px;
    font-weight: 600;
  }
}

.detail-remark {
  padding: 14px 16px;
  border-left: 3px solid #8db6f4;
  border-radius: 0 9px 9px 0;
  background: #f3f7fd;

  strong {
    color: #405069;
    font-size: 13px;
  }

  p {
    margin: 5px 0 0;
    color: #748196;
    font-size: 13px;
    line-height: 1.6;
  }
}

@media (max-width: 920px) {
  .rule-panel__head {
    align-items: flex-start;
    flex-direction: column;
  }

  .rule-filters {
    width: 100%;
    flex-wrap: wrap;
  }

  .detail-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-overview {
    flex-direction: column;
  }
}
</style>
