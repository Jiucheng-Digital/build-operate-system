<template>
  <el-dialog
    :model-value="modelValue"
    title="照明控制"
    width="420px"
    align-center
    append-to-body
    :close-on-click-modal="false"
    class="light-ctrl-dialog"
    @update:model-value="val => $emit('update:modelValue', val)"
    @open="reset"
  >
    <!-- 设备信息 -->
    <div class="lc-dev">
      <el-icon class="lc-dev__icon" :class="{ 'is-on': isOn }"><i class="bulb"></i></el-icon>
      <div class="lc-dev__info">
        <div class="lc-dev__name">{{ device && device.name || '—' }}</div>
        <div class="lc-dev__sn">序列号：{{ device && device.serialNumber || '—' }}</div>
      </div>
    </div>

    <!-- 开关 -->
    <div class="lc-switch" v-loading="submitting">
      <span class="lc-switch__state" :class="{ 'is-on': isOn }">{{ isOn ? '已打开' : '已关闭' }}</span>
      <el-switch
        v-model="isOn"
        size="large"
        active-text="开"
        inactive-text="关"
        inline-prompt
        style="--el-switch-on-color:#34c759;--el-switch-off-color:#c0c4cc"
        @change="onToggle"
      />
    </div>

    <template #footer>
      <el-button @click="close">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { controlLighting } from '@/api/build/drive'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  device: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'success'])

const isOn = ref(false)
const submitting = ref(false)

function isLightingOn(value) {
  return value === 'on' || value === true || value === 1 || value === '1' || value === 'true' || value === 'ture'
}

const stateOn = computed(() => {
  if (!props.device) return false
  const state = props.device.state !== undefined && props.device.state !== null && props.device.state !== ''
    ? props.device.state
    : props.device.streamSwitch
  return isLightingOn(state)
})

function reset() {
  isOn.value = stateOn.value
  submitting.value = false
}

function onToggle(val) {
  if (!props.device) return
  submitting.value = true
  controlLighting(props.device.id, val ? 'on' : 'off')
    .then(() => {
      props.device.state = val ? 'on' : 'off'
      props.device.streamSwitch = val ? 'on' : 'off'
      ElMessage.success(val ? '已打开' : '已关闭')
      emit('success')
    })
    .catch(() => {
      isOn.value = !val // 失败回滚
    })
    .finally(() => { submitting.value = false })
}

function close() {
  emit('update:modelValue', false)
}
</script>

<style scoped lang="scss">
:deep(.light-ctrl-dialog) {
  border-radius: 18px;
  overflow: hidden;
}

.lc-dev {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f4f7fb;
  margin-bottom: 20px;
}

.lc-dev__icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #e6eaf0;
  color: #9aa7b4;
  transition: background 0.2s, color 0.2s;

  .bulb {
    width: 14px;
    height: 14px;
    border-radius: 50%;
    background: currentColor;
    box-shadow: 0 0 0 3px rgba(154, 167, 180, 0.25);
  }

  &.is-on {
    background: #fff6dd;
    color: #f5a623;

    .bulb {
      box-shadow: 0 0 10px 3px rgba(245, 166, 35, 0.6);
    }
  }
}

.lc-dev__name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2d3d;
}

.lc-dev__sn {
  margin-top: 2px;
  font-size: 12.5px;
  color: #9aa7b4;
}

.lc-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 18px;
  border: 1px solid #eef1f5;
  border-radius: 12px;
}

.lc-switch__state {
  font-size: 16px;
  font-weight: 600;
  color: #909399;

  &.is-on {
    color: #34c759;
  }
}
</style>
