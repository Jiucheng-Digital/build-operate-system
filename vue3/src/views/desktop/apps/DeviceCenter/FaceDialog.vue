<template>
  <el-dialog
    :model-value="modelValue"
    width="540px"
    align-center
    append-to-body
    :close-on-click-modal="false"
    class="face-dialog"
    @update:model-value="val => $emit('update:modelValue', val)"
    @opened="init"
    @closed="reset"
  >
    <template #header>
      <div class="face-dialog__title">
        <span>人脸识别布控</span>
        <small>{{ deviceName }}</small>
      </div>
    </template>

    <div class="face-config">
      <div class="face-device">
        <div>
          <span>监控设备</span>
          <strong>{{ deviceName }}</strong>
        </div>
        <el-tag :type="initialEnabled ? 'success' : 'info'" effect="light">
          {{ initialEnabled ? '已开启' : '未开启' }}
        </el-tag>
      </div>

      <el-form label-position="top">
        <el-form-item label="人脸识别开关">
          <div class="face-switch">
            <el-switch
              v-model="form.enabled"
              inline-prompt
              active-text="开"
              inactive-text="关"
              style="--el-switch-on-color:#2f80ff;--el-switch-off-color:#c6ccd6"
            />
            <span>{{ switchTip }}</span>
          </div>
        </el-form-item>

        <el-form-item v-if="showDoorSelect" label="门禁设备" required>
          <el-select
            v-model="form.doorKey"
            class="face-select"
            filterable
            remote
            reserve-keyword
            clearable
            placeholder="请输入门禁名称搜索"
            :remote-method="searchDoorDevices"
            :loading="doorLoading"
            @change="selectDoor"
          >
            <el-option
              v-for="item in doorOptions"
              :key="doorKey(item)"
              :label="doorLabel(item)"
              :value="doorKey(item)"
            >
              <div class="face-door-option">
                <span>{{ item.name || item.deviceName || item.serialNumber || '未命名门禁' }}</span>
                <small>{{ item.serialNumber || '无序列号' }}</small>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <div v-if="showDoorSelect" class="face-hint">
          确认后将获取当前监控的 RTSP 预览流地址，并开启人脸识别布控。
        </div>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { faceStart, faceStop, getDevicePage, getPreviewUrl } from '@/api/build/drive'

const DOOR_CLASSIFY = 'access_control'
const DOOR_PAGE_SIZE = 100

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  device: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'success'])

const form = reactive({
  enabled: false,
  doorKey: ''
})

const initialEnabled = ref(false)
const doorOptions = ref([])
const selectedDoor = ref(null)
const doorLoading = ref(false)
const submitting = ref(false)
let doorSearchSeq = 0

const deviceName = computed(() => {
  return (props.device && (props.device.name || props.device.deviceName || props.device.serialNumber)) || '监控设备'
})

const showDoorSelect = computed(() => !initialEnabled.value && form.enabled)

const switchTip = computed(() => {
  if (initialEnabled.value && !form.enabled) return '关闭后确认，将停止当前人脸识别流'
  if (initialEnabled.value) return '当前已开启，如需停止请关闭开关'
  if (form.enabled) return '请选择要关联的门禁设备'
  return '打开后选择门禁设备，即可开启人脸识别'
})

function boolValue(value) {
  return value === true || value === 1 || value === '1' || value === 'true' || value === 'ture'
}

function init() {
  initialEnabled.value = boolValue(props.device?.streamSwitch)
  form.enabled = initialEnabled.value
  form.doorKey = ''
  selectedDoor.value = null
  doorOptions.value = []
  if (!initialEnabled.value) searchDoorDevices('')
}

function reset() {
  form.enabled = false
  form.doorKey = ''
  initialEnabled.value = false
  selectedDoor.value = null
  doorOptions.value = []
  submitting.value = false
}

function close() {
  emit('update:modelValue', false)
}

function doorKey(item) {
  return `${item.driveId || ''}__${item.serialNumber || item.id || ''}`
}

function doorLabel(item) {
  const name = item.name || item.deviceName || '未命名门禁'
  return item.serialNumber ? `${name}（${item.serialNumber}）` : name
}

function selectDoor(value) {
  selectedDoor.value = doorOptions.value.find(item => doorKey(item) === value) || null
}

async function searchDoorDevices(keyword = '') {
  const seq = ++doorSearchSeq
  doorLoading.value = true
  try {
    const name = keyword && keyword.trim()
    const params = {
      pageNum: 1,
      pageSize: DOOR_PAGE_SIZE,
      classify: DOOR_CLASSIFY
    }
    if (name) params.name = name

    const firstRes = await getDevicePage(params)
    const total = Number(firstRes.total) || 0
    const rows = firstRes.rows || []
    const pageCount = Math.ceil(total / DOOR_PAGE_SIZE)

    if (pageCount > 1) {
      const requests = []
      for (let pageNum = 2; pageNum <= pageCount; pageNum += 1) {
        requests.push(getDevicePage({ ...params, pageNum }))
      }
      const restResults = await Promise.all(requests)
      restResults.forEach(res => {
        rows.push(...(res.rows || []))
      })
    }

    if (seq === doorSearchSeq) {
      doorOptions.value = rows
    }
  } catch (_error) {
    if (seq === doorSearchSeq) {
      doorOptions.value = []
    }
  } finally {
    if (seq === doorSearchSeq) {
      doorLoading.value = false
    }
  }
}

function resolvePreviewUrl(res) {
  const data = res?.data
  if (typeof data === 'string') return data
  if (data?.url) return data.url
  if (res?.url) return res.url
  return ''
}

async function startFace() {
  const monitor = props.device
  if (!monitor?.id) {
    ElMessage.warning('缺少监控设备ID')
    return
  }
  if (!monitor?.driveId) {
    ElMessage.warning('缺少监控驱动ID')
    return
  }
  if (!monitor?.serialNumber) {
    ElMessage.warning('缺少监控设备序列号')
    return
  }
  if (!selectedDoor.value) {
    ElMessage.warning('请选择门禁设备')
    return
  }
  if (!selectedDoor.value.serialNumber || !selectedDoor.value.driveId) {
    ElMessage.warning('门禁设备缺少序列号或驱动ID')
    return
  }

  const previewRes = await getPreviewUrl({
    driveId: monitor.driveId,
    cameraIndexCode: monitor.serialNumber,
    protocol: 'rtsp'
  })
  const streamUrl = resolvePreviewUrl(previewRes)
  if (!streamUrl) {
    ElMessage.warning('获取监控预览流地址失败')
    return
  }

  await faceStart({
    deviceId: monitor.id,
    url: streamUrl,
    doorIndexCode: selectedDoor.value.serialNumber,
    doorDriveId: selectedDoor.value.driveId
  })
  monitor.streamSwitch = true
  ElMessage.success('人脸识别已开启')
  emit('success')
  close()
}

async function stopFace() {
  const deviceId = props.device?.id
  if (!deviceId) {
    ElMessage.warning('缺少监控设备ID')
    return
  }
  await faceStop(deviceId)
  props.device.streamSwitch = false
  ElMessage.success('人脸识别已关闭')
  emit('success')
  close()
}

async function submit() {
  if (submitting.value) return

  if (initialEnabled.value && form.enabled) {
    ElMessage.info('当前人脸识别已处于开启状态')
    return
  }
  if (!initialEnabled.value && !form.enabled) {
    ElMessage.warning('请先打开人脸识别开关')
    return
  }

  submitting.value = true
  try {
    if (initialEnabled.value && !form.enabled) {
      await stopFace()
    } else {
      await startFace()
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.face-dialog__title {
  display: flex;
  flex-direction: column;
  gap: 3px;

  span {
    color: #1f2d3d;
    font-size: 17px;
    font-weight: 700;
  }

  small {
    color: #8b98a7;
    font-size: 13px;
  }
}

.face-config {
  padding: 2px 2px 0;
}

.face-device {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  padding: 14px 16px;
  border: 1px solid #e4ebf6;
  border-radius: 12px;
  background: linear-gradient(135deg, #f7fbff 0%, #eef5ff 100%);

  span,
  strong {
    display: block;
  }

  span {
    color: #8b98a7;
    font-size: 13px;
  }

  strong {
    margin-top: 4px;
    color: #27364a;
    font-size: 15px;
  }
}

.face-switch {
  display: flex;
  align-items: center;
  gap: 12px;

  span {
    color: #6d7888;
    font-size: 13px;
  }
}

.face-select {
  width: 100%;
}

.face-door-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  small {
    color: #9aa6b2;
    font-size: 13px;
  }
}

.face-hint {
  margin-top: -4px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f6f8fb;
  color: #7b8797;
  font-size: 13px;
  line-height: 1.6;
}
</style>
