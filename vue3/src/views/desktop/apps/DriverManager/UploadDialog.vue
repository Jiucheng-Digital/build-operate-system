<template>
  <el-dialog
      :model-value="modelValue"
      width="560px"
      align-center
      append-to-body
      :show-close="false"
      :close-on-click-modal="false"
      class="ios-upload-dialog"
      @update:model-value="val => $emit('update:modelValue', val)"
      @open="onOpen"
      @closed="onClosed"
  >
    <template #header>
      <div class="iud__header">
        <span class="iud__title">上传自定义驱动</span>
        <button class="iud__close" @click="close"><el-icon><Close /></el-icon></button>
      </div>
    </template>

    <!-- 步骤进度条 -->
    <div class="iud-steps">
      <template v-for="(s, i) in steps" :key="s.key">
        <div class="iud-step" :class="`is-${stepState(i + 1)}`">
          <div class="iud-step__circle">
            <el-icon v-if="stepState(i + 1) === 'success'"><Check /></el-icon>
            <el-icon v-else-if="stepState(i + 1) === 'error'"><Close /></el-icon>
            <span v-else>{{ i + 1 }}</span>
          </div>
          <div class="iud-step__label">{{ s.label }}</div>
        </div>
        <div v-if="i < steps.length - 1" class="iud-line" :class="{ 'is-done': progress >= i + 1 }"></div>
      </template>
    </div>

    <!-- 第一步：填写信息 -->
    <el-form
        v-show="phase === 'form'"
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="iud__form"
        @submit.prevent
    >
      <el-form-item label="驱动文件" prop="fileName">
        <el-upload
            ref="fileUploadRef"
            class="iud-file"
            drag
            :auto-upload="false"
            :show-file-list="false"
            :on-change="onFileChange"
            accept=".zip,.rar,.exe,.jar,.sh"
        >
          <div v-if="!form.fileName" class="iud-drop">
            <el-icon class="iud-drop__icon"><UploadFilled /></el-icon>
            <div class="iud-drop__text">点击或拖拽文件到此处上传</div>
            <div class="iud-drop__tip">支持 .zip .rar .exe .jar .sh（最大 100MB）</div>
          </div>
          <div v-else class="iud-file__picked" @click.stop>
            <el-icon class="iud-file__doc"><Document /></el-icon>
            <span class="iud-file__name">{{ form.fileName }}</span>
            <el-icon class="iud-file__remove" @click.stop="onFileRemove"><Close /></el-icon>
          </div>
        </el-upload>
      </el-form-item>

      <div class="iud-row">
        <div class="iud-logo-field iud-col">
          <span class="iud-logo-field__label">LOGO</span>
          <el-upload
              ref="logoUploadRef"
              class="iud-logo"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="onIconChange"
              accept="image/*"
          >
            <div class="iud-logo__box">
              <img v-if="iconPreview" :src="iconPreview" class="iud-logo__img" alt="logo" />
              <el-icon v-else-if="iconUploading" class="is-loading iud-logo__icon"><Loading /></el-icon>
              <el-icon v-else class="iud-logo__icon"><Plus /></el-icon>
            </div>
          </el-upload>
          <span v-if="iconPreview && !iconUploading" class="iud-logo__clear" @click="clearIcon">移除</span>
        </div>

        <el-form-item label="驱动名称" prop="name" class="iud-col">
          <el-input v-model="form.name" placeholder="请输入驱动名称" maxlength="64" />
        </el-form-item>
      </div>

      <div class="iud-row">
        <el-form-item label="版本" prop="version" class="iud-col">
          <el-input v-model="form.version" placeholder="如 V1.25" maxlength="32" />
        </el-form-item>
        <el-form-item label="厂商" prop="type" class="iud-col">
          <el-select v-model="form.type" placeholder="请选择厂商" filterable class="iud-select">
            <el-option v-for="m in manufacturerOptions" :key="m.dictValue" :label="m.dictLabel" :value="m.dictValue" />
          </el-select>
        </el-form-item>
      </div>

      <div class="iud-row">
        <el-form-item label="所属类别" prop="classifyCode" class="iud-col">
          <el-select v-model="form.classifyCode" placeholder="请选择类别" filterable class="iud-select">
            <el-option v-for="c in classifyOptions" :key="c.code" :label="c.name" :value="c.code" />
          </el-select>
        </el-form-item>
        <div class="iud-col"></div>
      </div>

      <el-form-item label="驱动描述">
        <el-input v-model="form.remark" type="textarea" :rows="2" resize="none" placeholder="请输入驱动描述" maxlength="255" show-word-limit />
      </el-form-item>
    </el-form>

    <!-- 进度/结果 -->
    <div v-if="phase !== 'form'" class="iud-status">
      <!-- 第一步上传中：显示文件实时上传进度环 -->
      <template v-if="statusType === 'loading' && progress === 0 && uploadPercent < 100">
        <el-progress type="circle" :percentage="uploadPercent" :width="92" :stroke-width="7" color="#2f6bff" />
        <div class="iud-status__title">驱动上传中… {{ uploadPercent }}%</div>
      </template>
      <!-- 其它阶段：图标 + 文案 -->
      <template v-else>
        <div class="iud-status__icon" :class="`is-${statusType}`">
          <el-icon v-if="statusType === 'error'"><CircleCloseFilled /></el-icon>
          <el-icon v-else-if="statusType === 'done'"><CircleCheckFilled /></el-icon>
          <el-icon v-else class="is-loading"><Loading /></el-icon>
        </div>
        <div class="iud-status__title">{{ statusTitle }}</div>
      </template>
      <div v-if="statusType === 'loading' && progress < 3" class="iud-status__hint">
        服务器正在安装驱动，首次安装需拉取镜像，可能需要 1~2 分钟，请耐心等待…
      </div>
      <div v-if="errorMsg" class="iud-status__msg">{{ errorMsg }}</div>
    </div>

    <template #footer>
      <div class="iud__footer">
        <template v-if="phase === 'form'">
          <button class="iud-btn iud-btn--cancel" @click="close">取消</button>
          <button class="iud-btn iud-btn--ok" :disabled="submitting" @click="submit">
            <el-icon v-if="submitting" class="is-loading"><Loading /></el-icon>
            <span>{{ submitting ? '上传中…' : '确定' }}</span>
          </button>
        </template>
        <template v-else-if="phase === 'error'">
          <button class="iud-btn iud-btn--cancel" @click="close">关闭</button>
          <button class="iud-btn iud-btn--ok" @click="retry">重新上传</button>
        </template>
        <template v-else-if="phase === 'done'">
          <button class="iud-btn iud-btn--ok" @click="close">完成</button>
        </template>
        <template v-else>
          <button class="iud-btn iud-btn--cancel" @click="close">关闭</button>
        </template>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Plus, Loading, Close, Document, Check, CircleCloseFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import { uploadDrive, uploadLogo, listClassify } from '@/api/build/drive'
import { getDicts } from '@/api/system/dict/data'
import useUserStore from '@/store/modules/user'

defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'success'])

const steps = [
  { key: 'upload', label: '驱动上传' },
  { key: 'install', label: '驱动安装' },
  { key: 'done', label: '加载完成' }
]

const formRef = ref(null)
const fileUploadRef = ref(null)
const logoUploadRef = ref(null)
const classifyOptions = ref([])
const manufacturerOptions = ref([])
const submitting = ref(false)
const submitted = ref(false)
const rawFile = ref(null)
const iconUploading = ref(false)
const iconPreview = ref('')

// 进度：progress 已完成步数(0..3)，errorStep 出错步号(0 无)
const progress = ref(0)
const errorStep = ref(0)
const errorMsg = ref('')
const uploadPercent = ref(0) // 文件实时上传进度 0~100

const form = reactive({
  fileName: '',
  name: '',
  version: '',
  type: '',
  classifyCode: '',
  remark: '',
  // icon = LOGO 的存储地址（先上传图片拿到 path 再随驱动一起提交）
  icon: ''
})

const rules = {
  fileName: [{ required: true, message: '请上传驱动文件', trigger: 'change' }],
  name: [{ required: true, message: '请输入驱动名称', trigger: 'blur' }],
  version: [{ required: true, message: '请输入版本', trigger: 'blur' }],
  type: [{ required: true, message: '请选择厂商', trigger: 'blur' }],
  classifyCode: [{ required: true, message: '请选择类别', trigger: 'blur' }]
}

const MAX_SIZE = 100 * 1024 * 1024

/* ------- 步骤状态 ------- */
const phase = computed(() => {
  if (!submitted.value) return 'form'
  if (errorStep.value) return 'error'
  if (progress.value >= 3) return 'done'
  return 'processing'
})

function activeStep() {
  if (errorStep.value || progress.value >= 3) return 0
  return submitted.value ? progress.value + 1 : 1
}

function stepState(n) {
  if (errorStep.value === n) return 'error'
  if (n <= progress.value) return 'success'
  if (n === activeStep()) return 'active'
  return 'wait'
}

const statusType = computed(() => {
  if (errorStep.value) return 'error'
  if (progress.value >= 3) return 'done'
  return 'loading'
})

const statusTitle = computed(() => {
  if (errorStep.value === 1) return '驱动上传失败'
  if (errorStep.value === 2) return '驱动安装失败'
  if (errorStep.value === 3) return '加载失败'
  if (progress.value >= 3) return '驱动加载完成'
  if (progress.value === 2) return '加载中，请稍候…'
  if (progress.value === 1) return '驱动安装中，请稍候…'
  // progress === 0：文件已传完则在等服务器处理
  return uploadPercent.value >= 100 ? '文件已上传，正在处理…' : '驱动上传中，请稍候…'
})

/* ------- WebSocket（保持在线：心跳 + 断线自动重连） ------- */
let ws = null
let wsActive = false // 弹窗打开期间为 true，决定是否需要重连
let heartbeatTimer = null
let reconnectTimer = null

function buildWsUrl() {
  const userId = useUserStore().id
  if (!userId) return ''
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const base = import.meta.env.VITE_APP_BASE_API
  return `ws://192.168.0.230:6005/api/audioVideo/common/message/websocket/${userId}`
}

function connectWs() {
  wsActive = true
  const url = buildWsUrl()
  if (!url) {
    console.warn('[驱动WS] 缺少登录用户ID，无法连接')
    return
  }
  openSocket(url)
}

function openSocket(url) {
  try {
    ws = new WebSocket(url)
  } catch (err) {
    console.warn('[驱动WS] 创建连接失败', err)
    scheduleReconnect(url)
    return
  }
  ws.onopen = () => {
    console.log('[驱动WS] 已连接', url)
    startHeartbeat()
  }
  ws.onmessage = e => handleWsMessage(e.data)
  ws.onerror = err => console.warn('[驱动WS] 连接出错', err)
  ws.onclose = e => {
    console.warn('[驱动WS] 连接关闭, code =', e && e.code)
    stopHeartbeat()
    if (wsActive) scheduleReconnect(url) // 仍在流程中则自动重连，保持在线
  }
}

function scheduleReconnect(url) {
  clearTimeout(reconnectTimer)
  reconnectTimer = setTimeout(() => {
    if (wsActive) {
      console.log('[驱动WS] 尝试重连…')
      openSocket(url)
    }
  }, 2000)
}

function startHeartbeat() {
  stopHeartbeat()
  // 定时发心跳，避免代理/容器空闲超时把连接断掉
  heartbeatTimer = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      try { ws.send(JSON.stringify({ type: 'ping' })) } catch (e) { /* ignore */ }
    }
  }, 25000)
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

function disconnectWs() {
  wsActive = false
  stopHeartbeat()
  clearTimeout(reconnectTimer)
  if (ws) {
    ws.onclose = null // 主动关闭不触发重连
    ws.onmessage = null
    ws.onerror = null
    try { ws.close() } catch (e) { /* ignore */ }
    ws = null
  }
}

function handleWsMessage(raw) {
  let msg
  try {
    msg = JSON.parse(raw)
  } catch (e) {
    console.log('[驱动WS] 收到非JSON消息:', raw)
    return
  }
  if (!msg || msg.type !== 'DRIVE_UPLOAD' || !msg.data) {
    console.log('[驱动WS] 忽略消息(非DRIVE_UPLOAD):', msg)
    return
  }
  const { cmd, message } = msg.data
  console.log('[驱动WS] 收到 cmd =', cmd, message ? '| message = ' + message : '')

  // 未提交上传时忽略（避免历史/其它会话消息误触发）
  if (!submitted.value) {
    console.log('[驱动WS] 尚未提交上传，忽略该消息')
    return
  }

  switch (cmd) {
    case 'upload_success': // 上传成功 → 第一步完成
      if (progress.value < 1) progress.value = 1
      break
    case 'install_success': // 安装成功 → 第二步完成
      if (progress.value < 2) progress.value = 2
      break
    case 'start_success': // 服务启动/加载完成 → 全部完成
      progress.value = 3
      emit('success') // 刷新列表
      break
    case 'upload_error':
      setError(1, message)
      break
    case 'install_error':
      setError(2, message)
      break
    case 'start_error':
      setError(3, message)
      break
    default:
      console.log('[驱动WS] 未知 cmd:', cmd)
      break
  }
}

function setError(step, message) {
  errorStep.value = step
  errorMsg.value = message || '操作失败，请重试'
}

/* ------- 文件 / LOGO ------- */
// el-upload 仅当文件选择器用，状态自行维护：每次选完清空其内部列表，避免再次选择不触发 change
function resetUpload(uploadRef) {
  nextTick(() => uploadRef.value && uploadRef.value.clearFiles())
}

function onFileChange(uploadFile) {
  const raw = uploadFile.raw
  if (raw && raw.size > MAX_SIZE) {
    ElMessage.warning('驱动文件不能超过 100MB')
    resetUpload(fileUploadRef)
    return
  }
  rawFile.value = raw
  form.fileName = uploadFile.name
  formRef.value && formRef.value.validateField('fileName')
  resetUpload(fileUploadRef)
}

function onFileRemove() {
  rawFile.value = null
  form.fileName = ''
  resetUpload(fileUploadRef)
}

function onIconChange(uploadFile) {
  const raw = uploadFile.raw
  resetUpload(logoUploadRef)
  if (!raw) return
  if (!raw.type.startsWith('image/')) {
    ElMessage.warning('LOGO 只能上传图片')
    return
  }
  iconPreview.value = URL.createObjectURL(raw)
  iconUploading.value = true
  uploadLogo(raw)
      .then(res => {
        form.icon = (res.data && res.data.path) || ''
        if (!form.icon) {
          ElMessage.warning('LOGO 上传未返回地址')
          clearIcon()
        }
      })
      .catch(() => clearIcon())
      .finally(() => { iconUploading.value = false })
}

function clearIcon() {
  form.icon = ''
  iconPreview.value = ''
  resetUpload(logoUploadRef)
}

/* ------- 生命周期 / 提交 ------- */
function resetState() {
  submitting.value = false
  submitted.value = false
  progress.value = 0
  errorStep.value = 0
  errorMsg.value = ''
  uploadPercent.value = 0
  rawFile.value = null
  iconUploading.value = false
  iconPreview.value = ''
  Object.assign(form, { fileName: '', name: '', version: '', type: '', classifyCode: '', remark: '', icon: '' })
  formRef.value && formRef.value.clearValidate()
  resetUpload(fileUploadRef)
  resetUpload(logoUploadRef)
}

// 类型码 / 厂商下拉数据：已加载则复用，避免每次打开都请求
function loadOptions() {
  if (!classifyOptions.value.length) {
    listClassify().then(res => { classifyOptions.value = res.data || [] }).catch(() => {})
  }
  if (!manufacturerOptions.value.length) {
    getDicts('drive_manufacturer').then(res => { manufacturerOptions.value = res.data || [] }).catch(() => {})
  }
}

function onOpen() {
  resetState()
  loadOptions()
  connectWs() // 点击「上传驱动」打开弹窗即连接
}

function onClosed() {
  disconnectWs()
}

function close() {
  emit('update:modelValue', false)
}

// 失败后重新回到表单（保留已填内容，进度归零）
function retry() {
  submitted.value = false
  progress.value = 0
  errorStep.value = 0
  errorMsg.value = ''
}

function submit() {
  formRef.value.validate(valid => {
    if (!valid) return
    if (!rawFile.value) {
      ElMessage.warning('请上传驱动文件')
      return
    }
    if (iconUploading.value) {
      ElMessage.warning('LOGO 正在上传，请稍候')
      return
    }
    const fd = new FormData()
    fd.append('file', rawFile.value)
    if (form.icon) fd.append('icon', form.icon)
    fd.append('name', form.name)
    fd.append('version', form.version)
    fd.append('type', form.type)
    fd.append('classifyCode', form.classifyCode)
    if (form.remark) fd.append('remark', form.remark)

    // 立即进入进度阶段：避免上传很快、WebSocket 消息早于 HTTP 响应到达时被漏掉
    submitting.value = true
    submitted.value = true
    progress.value = 0
    errorStep.value = 0
    errorMsg.value = ''
    uploadPercent.value = 0
    uploadDrive(fd, e => {
      // 文件实时上传进度
      const total = e.total || (e.event && e.event.total)
      if (total) {
        uploadPercent.value = Math.min(100, Math.round((e.loaded / total) * 100))
      } else if (e.progress != null) {
        uploadPercent.value = Math.round(e.progress * 100)
      }
    })
        .then(() => {
          // 上传请求已受理，后续进度由 WebSocket 消息推进
        })
        .catch(() => {
          // HTTP 上传失败：退回表单
          submitted.value = false
        })
        .finally(() => { submitting.value = false })
  })
}

onBeforeUnmount(disconnectWs)
</script>

<style scoped lang="scss">
/* iOS 风格弹窗 */
:deep(.ios-upload-dialog) {
  border-radius: 22px;
  overflow: hidden;
  padding: 0;
  box-shadow: 0 30px 80px rgba(20, 50, 90, 0.28);
  background: #fff;
}

:deep(.ios-upload-dialog .el-dialog__header) {
  padding: 0;
  margin: 0;
}

:deep(.ios-upload-dialog .el-dialog__body) {
  padding: 8px 24px 0;
  max-height: 70vh;
  overflow-y: auto;
}

:deep(.ios-upload-dialog .el-dialog__footer) {
  padding: 12px 24px 18px;
}

/* 头部 */
.iud__header {
  position: relative;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f1f3f6;
}

.iud__title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
  letter-spacing: 0.5px;
}

.iud__close {
  position: absolute;
  right: 18px;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: #f1f2f6;
  color: #8a94a3;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;

  &:hover {
    background: #f56c6c;
    color: #fff;
  }
}

/* 步骤条 */
.iud-steps {
  display: flex;
  align-items: center;
  padding: 14px 6px 18px;
}

.iud-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 0 0 auto;
}

.iud-step__circle {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 600;
  background: #e9edf2;
  color: #9aa7b4;
  transition: background 0.25s, color 0.25s;
}

.iud-step__label {
  font-size: 12.5px;
  color: #9aa7b4;
  white-space: nowrap;
  transition: color 0.25s;
}

/* 进行中：圆圈保持灰色，仅标签高亮 */
.iud-step.is-active .iud-step__label {
  color: #2f6bff;
  font-weight: 600;
}

/* 完成：蓝色实心 */
.iud-step.is-success .iud-step__circle {
  background: linear-gradient(135deg, #3b8bff, #2f6bff);
  color: #fff;
}

.iud-step.is-success .iud-step__label {
  color: #2f6bff;
}

/* 失败：红色 */
.iud-step.is-error .iud-step__circle {
  background: #f56c6c;
  color: #fff;
}

.iud-step.is-error .iud-step__label {
  color: #f56c6c;
}

/* 连接线 */
.iud-line {
  flex: 1;
  height: 3px;
  margin: 0 8px;
  margin-bottom: 22px;
  border-radius: 3px;
  background: #e9edf2;
  transition: background 0.25s;
}

.iud-line.is-done {
  background: linear-gradient(90deg, #3b8bff, #2f6bff);
}

/* 表单 */
.iud__form {
  padding-top: 2px;
}

:deep(.iud__form .el-form-item) {
  margin-bottom: 11px;
}

:deep(.iud__form .el-form-item__label) {
  padding: 0 0 4px;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.2;
  color: #6b7280;
}

:deep(.iud__form .el-input__wrapper),
:deep(.iud__form .el-textarea__inner) {
  border-radius: 12px;
  background: #f1f2f6;
  box-shadow: none;
  border: 1px solid transparent;
  transition: background 0.2s, border-color 0.2s, box-shadow 0.2s;
}

:deep(.iud__form .el-input__wrapper) {
  padding: 4px 12px;
  min-height: 40px;
}

:deep(.iud__form .el-input__wrapper.is-focus),
:deep(.iud__form .el-textarea__inner:focus) {
  background: #fff;
  border-color: #2f74ff;
  box-shadow: 0 0 0 3px rgba(47, 116, 255, 0.16);
}

:deep(.iud__form .el-textarea__inner) {
  padding: 8px 12px;
  min-height: 56px;
}

/* 下拉框：与输入框一致的 iOS 风格（Element Plus 2.13 用 .el-select__wrapper） */
.iud-select {
  width: 100%;
}

:deep(.iud__form .el-select__wrapper) {
  min-height: 40px;
  border-radius: 12px;
  background: #f1f2f6;
  box-shadow: none;
  border: 1px solid transparent;
  transition: background 0.2s, border-color 0.2s, box-shadow 0.2s;
}

:deep(.iud__form .el-select__wrapper.is-focused) {
  background: #fff;
  border-color: #2f74ff;
  box-shadow: 0 0 0 3px rgba(47, 116, 255, 0.16);
}

.iud-row {
  display: flex;
  gap: 14px;
}

.iud-col,
.iud-grow {
  flex: 1;
  min-width: 0;
}

/* LOGO：标签在左、上传框在右，与右侧"驱动名称"输入框顶部对齐 */
.iud-logo-field {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 23px;
}

.iud-logo-field__label {
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
}

/* 驱动文件拖拽区 */
.iud-file {
  width: 100%;

  :deep(.el-upload),
  :deep(.el-upload-dragger) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    padding: 0;
    border: 1.5px dashed #d4dbe6;
    border-radius: 16px;
    background: #f7f9fc;
    transition: border-color 0.2s, background 0.2s;
  }

  :deep(.el-upload-dragger:hover) {
    border-color: #2f74ff;
    background: #f0f5ff;
  }
}

.iud-drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 14px 12px;
}

.iud-drop__icon {
  font-size: 30px;
  color: #aab6c6;
  margin-bottom: 6px;
}

.iud-drop__text {
  font-size: 14px;
  color: #4a5663;
}

.iud-drop__tip {
  margin-top: 6px;
  font-size: 13px;
  color: #9aa7b4;
}

.iud-file__picked {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
}

.iud-file__doc {
  font-size: 24px;
  color: #2f74ff;
}

.iud-file__name {
  flex: 1;
  text-align: left;
  font-size: 14px;
  color: #1f2d3d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.iud-file__remove {
  color: #9aa7b4;
  cursor: pointer;

  &:hover {
    color: #f56c6c;
  }
}

/* LOGO */
.iud-logo :deep(.el-upload) {
  display: block;
}

.iud-logo__box {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  overflow: hidden;
  background: #f1f2f6;
  color: #aab6c6;
  transition: background 0.2s;

  &:hover {
    background: #e8eaf0;
  }
}

.iud-logo__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.iud-logo__icon {
  font-size: 22px;
}

.iud-logo__clear {
  font-size: 13px;
  color: #f56c6c;
  cursor: pointer;
}

/* 进度/结果视图 */
.iud-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 16px 26px;
}

.iud-status :deep(.el-progress) {
  margin-bottom: 14px;
}

.iud-status__icon {
  font-size: 54px;
  margin-bottom: 14px;
  line-height: 1;

  &.is-loading {
    color: #2f6bff;
  }

  &.is-done {
    color: #34c759;
  }

  &.is-error {
    color: #f56c6c;
  }
}

.iud-status__title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
}

.iud-status__hint {
  margin-top: 8px;
  max-width: 320px;
  font-size: 12.5px;
  line-height: 1.6;
  color: #9aa7b4;
  text-align: center;
}

.iud-status__msg {
  margin-top: 10px;
  padding: 8px 14px;
  max-width: 100%;
  border-radius: 10px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-all;
}

/* 底部按钮（iOS 胶囊） */
.iud__footer {
  display: flex;
  gap: 12px;
}

.iud-btn {
  flex: 1;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 0;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: filter 0.15s, background 0.15s, transform 0.05s;

  &:active {
    transform: scale(0.98);
  }
}

.iud-btn--cancel {
  background: #eceef3;
  color: #3a424c;

  &:hover {
    background: #e3e6ec;
  }
}

.iud-btn--ok {
  background: linear-gradient(135deg, #3b8bff, #2f6bff);
  color: #fff;
  box-shadow: 0 10px 20px rgba(47, 107, 255, 0.28);

  &:hover {
    filter: brightness(1.05);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }
}
</style>
