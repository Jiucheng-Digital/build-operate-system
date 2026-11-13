<template>
  <el-dialog
    :model-value="modelValue"
    width="720px"
    align-center
    append-to-body
    :show-close="false"
    :close-on-click-modal="false"
    class="event-rule-dialog"
    @update:model-value="value => emit('update:modelValue', value)"
    @open="onOpen"
    @closed="onClosed"
  >
    <template #header>
      <div class="rule-dialog__header">
        <div>
          <strong>新增事件规则</strong>
          <span>配置事件触发条件和流转部门</span>
        </div>
        <button type="button" class="rule-dialog__close" @click="close">
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </template>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="92px"
      class="rule-form"
      @submit.prevent
    >
      <el-form-item label="规则名称" prop="ruleName">
        <el-input
          v-model="form.ruleName"
          maxlength="64"
          show-word-limit
          placeholder="请输入规则名称，如：门禁普通告警规则"
        />
      </el-form-item>

      <el-form-item label="规则描述" prop="ruleDesc">
        <el-input
          v-model="form.ruleDesc"
          type="textarea"
          :rows="3"
          maxlength="255"
          show-word-limit
          resize="none"
          placeholder="请输入规则描述"
        />
      </el-form-item>

      <el-form-item label="事件级别" prop="eventLevel">
        <el-select v-model="form.eventLevel" placeholder="请选择事件级别" class="rule-form__full">
          <el-option v-for="item in EVENT_LEVELS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <section class="config-card">
        <div class="config-card__title">
          <el-icon><Bell /></el-icon>
          <span>触发条件配置</span>
          <em>选填</em>
        </div>

        <el-form-item label="事件来源" prop="eventSource">
          <el-select
            v-model="form.eventSource"
            filterable
            :loading="optionLoading"
            placeholder="请选择驱动"
            class="rule-form__full"
            @change="clearTrigger"
          >
            <el-option
              v-for="driver in driverOptions"
              :key="driver.id"
              :label="driverLabel(driver)"
              :value="String(driver.id)"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="触发条件" prop="triggerValue" class="trigger-form-item">
          <div class="trigger-row">
            <el-select
              v-model="form.triggerKey"
              clearable
              :disabled="!form.eventSource"
              :placeholder="form.eventSource ? '可选：请选择触发参数' : '请先选择事件来源'"
              @change="onTriggerChange"
            >
              <el-option v-for="option in triggerOptions" :key="option.key" :label="option.label" :value="option.key" />
            </el-select>
            <el-select v-model="form.triggerCondition" :disabled="!form.triggerKey" placeholder="条件">
              <el-option label="大于" value="大于" />
              <el-option v-if="form.triggerKey !== 'disconnect'" label="小于" value="小于" />
              <el-option v-if="form.triggerKey !== 'disconnect'" label="等于" value="等于" />
            </el-select>
            <el-input-number
              v-model="form.triggerValue"
              :disabled="!form.triggerKey"
              :min="selectedTrigger?.min"
              :precision="selectedTrigger?.precision ?? 0"
              controls-position="right"
              :placeholder="selectedTrigger?.unit ? `值（${selectedTrigger.unit}）` : '阈值'"
            />
          </div>
        </el-form-item>
      </section>

      <section class="config-card config-card--action">
        <div class="config-card__title">
          <el-icon><Setting /></el-icon>
          <span>执行动作配置</span>
        </div>

        <div class="action-type">
          <el-icon><OfficeBuilding /></el-icon>
          <span>流转部门</span>
        </div>

        <el-form-item label="选择部门" prop="actionTargets" class="action-targets">
          <el-select
            v-model="form.actionTargets"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :loading="optionLoading"
            placeholder="请选择一个或多个部门"
            class="rule-form__full"
          >
            <el-option
              v-for="dept in deptOptions"
              :key="dept.deptId"
              :label="dept.deptName"
              :value="String(dept.deptId)"
            />
          </el-select>
        </el-form-item>
      </section>
    </el-form>

    <template #footer>
      <div class="rule-dialog__footer">
        <el-button @click="close">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, Close, OfficeBuilding, Setting } from '@element-plus/icons-vue'
import { addEventRule } from '@/api/build/event'
import { listDrive } from '@/api/build/drive'
import { listDept } from '@/api/system/dept'
import { triggerOptionsForDriver } from './eventRuleTriggers'

defineProps({
  modelValue: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'success'])

const EVENT_LEVELS = [
  { value: 'low', label: '低' },
  { value: 'middle', label: '中' },
  { value: 'high', label: '高' }
]

function defaultForm() {
  return {
    ruleName: '',
    ruleDesc: '',
    eventLevel: '',
    eventSource: '',
    triggerKey: '',
    triggerCondition: '大于',
    triggerValue: null,
    actionTargets: []
  }
}

const formRef = ref(null)
const form = reactive(defaultForm())
const driverOptions = ref([])
const deptOptions = ref([])
const optionLoading = ref(false)
const submitting = ref(false)
const selectedDriver = computed(() => driverOptions.value.find(driver => String(driver.id) === form.eventSource))
const triggerOptions = computed(() => triggerOptionsForDriver(selectedDriver.value))
const selectedTrigger = computed(() => triggerOptions.value.find(option => option.key === form.triggerKey))

const rules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  eventLevel: [{ required: true, message: '请选择事件级别', trigger: 'change' }],
  eventSource: [{ required: true, message: '请选择事件来源', trigger: 'change' }],
  actionTargets: [{ type: 'array', required: true, min: 1, message: '请选择流转部门', trigger: 'change' }],
  triggerValue: [{ validator: validateTriggerValue, trigger: 'change' }]
}

function validateTriggerValue(_rule, value, callback) {
  if (form.triggerKey && (value === null || value === undefined || value === '')) {
    callback(new Error(`请输入${selectedTrigger.value?.label || '触发参数'}阈值`))
    return
  }
  callback()
}

function driverLabel(driver) {
  return driver.version ? `${driver.name || driver.id}（${driver.version}）` : (driver.name || String(driver.id))
}

function clearTrigger() {
  form.triggerKey = ''
  form.triggerCondition = '大于'
  form.triggerValue = null
  nextTick(() => formRef.value?.clearValidate('triggerValue'))
}

function onTriggerChange() {
  form.triggerCondition = selectedTrigger.value?.condition || '大于'
  form.triggerValue = null
  nextTick(() => formRef.value?.clearValidate('triggerValue'))
}

async function loadOptions() {
  optionLoading.value = true
  const [driverResult, deptResult] = await Promise.allSettled([
    listDrive({ pageNum: 1, pageSize: 1000 }),
    listDept()
  ])

  driverOptions.value = driverResult.status === 'fulfilled' ? (driverResult.value.rows || []) : []
  deptOptions.value = deptResult.status === 'fulfilled' ? (deptResult.value.data || []) : []
  optionLoading.value = false
}

function normalizeDeptId(deptId) {
  const numericId = Number(deptId)
  return Number.isSafeInteger(numericId) ? numericId : String(deptId)
}

function buildPayload() {
  const payload = {
    ruleName: form.ruleName.trim(),
    ruleDesc: form.ruleDesc.trim(),
    eventLevel: form.eventLevel,
    eventSource: form.eventSource,
    conditionRelation: 'and',
    triggerConditions: form.triggerKey ? [{
      key: form.triggerKey,
      condition: form.triggerCondition,
      value: Number(form.triggerValue)
    }] : [],
    actionType: 'dept',
    actionTargets: form.actionTargets.map(normalizeDeptId)
  }

  return payload
}

async function submit() {
  if (submitting.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    submitting.value = true
    await addEventRule(buildPayload())
    ElMessage.success('新增事件规则成功')
    emit('success')
    close()
  } catch (_error) {
    // 请求错误由全局请求拦截器统一提示。
  } finally {
    submitting.value = false
  }
}

function close() {
  emit('update:modelValue', false)
}

function reset() {
  Object.assign(form, defaultForm())
  nextTick(() => formRef.value?.clearValidate())
}

function onOpen() {
  reset()
  loadOptions()
}

function onClosed() {
  reset()
}
</script>

<style scoped lang="scss">
.rule-dialog__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  strong {
    display: block;
    font-size: 18px;
    color: #1f2d3d;
  }

  span {
    display: block;
    margin-top: 5px;
    font-size: 13px;
    color: #8a97a6;
  }
}

.rule-dialog__close {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 8px;
  background: #f1f4f8;
  color: #6b7785;
  cursor: pointer;

  &:hover {
    background: #fdecec;
    color: #f56c6c;
  }
}

.rule-form {
  padding: 2px 4px 0;
}

.rule-form__full {
  width: 100%;
}

.config-card {
  padding: 16px 16px 4px;
  margin: 18px 0;
  border: 1px solid #e9eef5;
  border-radius: 12px;
  background: #f8faff;
}

.config-card__title {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 16px;
  color: #24364b;
  font-weight: 600;

  .el-icon {
    color: #2f74ff;
  }

  em {
    margin-left: 2px;
    padding: 2px 7px;
    border-radius: 10px;
    background: #edf2f8;
    color: #8794a3;
    font-size: 13px;
    font-style: normal;
    font-weight: 400;
  }
}

.trigger-row {
  width: 100%;
  display: grid;
  grid-template-columns: 1.5fr 0.8fr 1fr;
  gap: 10px;
}

.trigger-row > * {
  width: 100%;
}

.config-card--action {
  padding-bottom: 8px;
  background: #fbfcfe;
}

.action-type {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 8px 14px;
  margin: -2px 0 16px 92px;
  border: 1px solid #8fb8ff;
  border-radius: 8px;
  background: #eaf2ff;
  color: #236be8;
  font-size: 13px;
  font-weight: 600;
}

.rule-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 760px) {
  .trigger-row {
    grid-template-columns: 1fr;
  }

  .action-type {
    margin-left: 0;
  }
}
</style>
