<template>
  <el-dialog
    :model-value="modelValue"
    title="门禁控制"
    width="520px"
    align-center
    append-to-body
    :close-on-click-modal="false"
    class="ctrl-dialog"
    @update:model-value="value => $emit('update:modelValue', value)"
    @open="reset"
  >
    <div class="ctrl-dev">
      <span class="ctrl-dev__icon" aria-hidden="true"><el-icon><Lock /></el-icon></span>
      <div class="ctrl-dev__info">
        <strong>{{ device?.name || '未命名门禁设备' }}</strong>
        <span>{{ device?.address || '位置未标注' }} · 序列号 {{ device?.serialNumber || '—' }}</span>
      </div>
      <span class="device-state">{{ device?.status === 'online' ? '在线' : (device?.status || '状态未知') }}</span>
    </div>

    <fieldset class="ctrl-fieldset">
      <legend>选择控制指令</legend>
      <button
        v-for="option in STATE_OPTIONS"
        :key="option.value"
        type="button"
        class="ctrl-option"
        :class="{ 'is-active': selected === option.value, 'is-risk': option.risk === 'high' }"
        :aria-pressed="selected === option.value"
        @click="selected = option.value"
      >
        <span class="ctrl-option__radio" aria-hidden="true"><i></i></span>
        <span class="ctrl-option__copy">
          <strong>{{ option.label }}</strong>
          <small>{{ option.description }}</small>
        </span>
        <span v-if="option.risk === 'high'" class="risk-tag">持续状态</span>
      </button>
    </fieldset>

    <div v-if="selectedOption" class="control-review" :class="{ 'is-risk': isHighRisk }">
      <el-icon><Warning v-if="isHighRisk" /><InfoFilled v-else /></el-icon>
      <div>
        <strong>{{ isHighRisk ? '高影响指令，需要再次确认' : '执行范围确认' }}</strong>
        <p>{{ selectedOption.review }}</p>
      </div>
    </div>

<!--    <div class="control-boundary">-->
<!--&lt;!&ndash;      <span>当前可验证阶段</span>&ndash;&gt;-->
<!--&lt;!&ndash;      <strong>指令已提交</strong>&ndash;&gt;-->
<!--      <el-icon><ArrowRight /></el-icon>-->
<!--&lt;!&ndash;      <span class="is-pending">设备响应与物理回读尚未接入</span>&ndash;&gt;-->
<!--    </div>-->

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="selected === null" @click="submit">
        {{ isHighRisk ? '审核并执行' : '执行控制' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, InfoFilled, Lock, Warning } from '@element-plus/icons-vue'
import { doDoorControl } from '@/api/build/drive'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  device: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue'])

const STATE_OPTIONS = [
  { value: 2, label: '单次开门', description: '执行一次开门指令，随后由设备恢复', risk: 'normal', review: '仅作用于当前门禁点，不改变持续模式。' },
  { value: 1, label: '单次关门', description: '执行一次关门指令，随后由设备恢复', risk: 'normal', review: '仅作用于当前门禁点，不改变持续模式。' },
  { value: 0, label: '保持常开', description: '门禁持续开放，直至切换其他模式', risk: 'high', review: '此操作会持续降低门禁防护，仅应在确认现场安全后执行。' },
  { value: 3, label: '保持常闭', description: '门禁持续关闭，直至切换其他模式', risk: 'high', review: '此操作可能影响人员通行，请确认没有疏散与救援风险。' }
]

const selected = ref(null)
const submitting = ref(false)
const selectedOption = computed(() => STATE_OPTIONS.find(option => option.value === selected.value))
const isHighRisk = computed(() => selectedOption.value?.risk === 'high')

function reset() {
  selected.value = null
  submitting.value = false
}

function close() {
  emit('update:modelValue', false)
}

async function submit() {
  if (selected.value === null || !props.device || submitting.value) return

  if (isHighRisk.value) {
    try {
      await ElMessageBox.confirm(
        `将“${props.device.name || '当前门禁'}”设置为“${selectedOption.value.label}”。该状态会持续生效，确定继续吗？`,
        '确认高影响门禁操作',
        {
          confirmButtonText: '确认并下发',
          cancelButtonText: '返回检查',
          type: 'warning',
          distinguishCancelAndClose: true
        }
      )
    } catch (_error) {
      return
    }
  }

  submitting.value = true
  doDoorControl({
    doorIndexCodes: [props.device.serialNumber],
    controlType: selected.value,
    driveId: props.device.driveId
  })
    .then(() => {
      ElMessage.success('控制指令已下发，请刷新设备状态确认回读结果')
      close()
    })
    .catch(() => {})
    .finally(() => { submitting.value = false })
}
</script>

<style scoped lang="scss">
:global(.ctrl-dialog) {
  overflow: hidden;
  border-radius: 14px;
}

.ctrl-dev {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 14px;
  margin-bottom: 18px;
  border: 1px solid #dfe7f0;
  border-radius: 11px;
  background: #f7f9fc;
}

.ctrl-dev__icon {
  flex: 0 0 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #eaf2ff;
  color: #246bfd;
  font-size: 20px;
}

.ctrl-dev__info {
  min-width: 0;
  flex: 1;

  strong,
  span {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: #253044;
    font-size: 14px;
  }

  span {
    margin-top: 3px;
    color: #7b8796;
    font-size: 13px;
  }
}

.device-state {
  padding: 4px 7px;
  border-radius: 999px;
  background: #eaf8f1;
  color: #12805c;
  font-size: 13px;
}

.ctrl-fieldset {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px;
  margin: 0;
  padding: 0;
  border: 0;

  legend {
    grid-column: 1 / -1;
    margin-bottom: 8px;
    color: #344054;
    font-size: 13px;
    font-weight: 700;
  }
}

.ctrl-option {
  position: relative;
  min-height: 70px;
  display: flex;
  align-items: flex-start;
  gap: 9px;
  padding: 12px;
  border: 1px solid #dfe5ec;
  border-radius: 10px;
  background: #fff;
  color: #344054;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;

  &:hover {
    border-color: #9fbbed;
    background: #f8fbff;
  }

  &:focus-visible {
    outline: 3px solid rgba(36, 107, 253, 0.28);
    outline-offset: 2px;
  }

  &.is-active {
    border-color: #246bfd;
    background: #f1f6ff;

    .ctrl-option__radio {
      border-color: #246bfd;

      i {
        background: #246bfd;
      }
    }
  }

  &.is-active.is-risk {
    border-color: #f59e0b;
    background: #fff9ef;

    .ctrl-option__radio {
      border-color: #f59e0b;

      i {
        background: #f59e0b;
      }
    }
  }
}

.ctrl-option__radio {
  flex: 0 0 17px;
  height: 17px;
  display: grid;
  place-items: center;
  margin-top: 2px;
  border: 1.5px solid #b8c2ce;
  border-radius: 50%;

  i {
    width: 7px;
    height: 7px;
    border-radius: 50%;
  }
}

.ctrl-option__copy {
  min-width: 0;

  strong,
  small {
    display: block;
  }

  strong {
    font-size: 13px;
  }

  small {
    margin-top: 4px;
    color: #7b8796;
    font-size: 13px;
    line-height: 1.45;
  }
}

.risk-tag {
  position: absolute;
  top: 7px;
  right: 7px;
  padding: 2px 5px;
  border-radius: 999px;
  background: #fff1d6;
  color: #a15c00;
  font-size: 13px;
}

.control-review {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin-top: 12px;
  padding: 10px 11px;
  border-radius: 9px;
  background: #f1f6ff;
  color: #315eaa;

  &.is-risk {
    background: #fff4e5;
    color: #a15c00;
  }

  strong {
    display: block;
    font-size: 13px;
  }

  p {
    margin: 3px 0 0;
    font-size: 13px;
    line-height: 1.5;
  }
}

.control-boundary {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e7ebf0;
  color: #7b8796;
  font-size: 13px;

  strong {
    color: #246bfd;
  }

  .is-pending {
    color: #8b6d3b;
  }
}

@media (max-width: 560px) {
  .ctrl-fieldset {
    grid-template-columns: 1fr;
  }

  .control-boundary {
    align-items: flex-start;
    flex-wrap: wrap;
  }
}
</style>
