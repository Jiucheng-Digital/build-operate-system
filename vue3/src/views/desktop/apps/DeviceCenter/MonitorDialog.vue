<template>
  <el-dialog
    :model-value="modelValue"
    width="min(1180px, 92vw)"
    align-center
    append-to-body
    :show-close="false"
    :close-on-click-modal="false"
    class="mon-dialog"
    @update:model-value="val => $emit('update:modelValue', val)"
    @opened="onOpened"
    @closed="onClosed"
  >
    <template #header>
      <div class="mon__header">
        <span class="mon__title">
          <el-icon><VideoCamera /></el-icon>
          监控画面
        </span>
        <button class="mon__close" @click="close"><el-icon><Close /></el-icon></button>
      </div>
    </template>

    <div ref="monRef" class="mon">
      <!-- 左侧操作栏 -->
      <aside class="mon__ctrl">
        <!-- 设备信息 -->
        <div class="mon-dev">
          <div class="mon-dev__icon"><el-icon><VideoCameraFilled /></el-icon></div>
          <div class="mon-dev__meta">
            <div class="mon-dev__name">{{ deviceName }}</div>
            <div class="mon-dev__status" :class="status">
              <i></i>{{ status === 'live' ? '在线' : (status === 'connecting' ? '连接中' : '离线') }}
            </div>
          </div>
        </div>

        <!-- 云台 -->
        <div class="mon-sec">
          <div class="mon-sec__title">云台控制</div>
          <div class="ptz">
            <button class="ptz__btn" :class="{ 'is-pressed': activeCmd === 'LEFT_UP' }" @pointerdown.prevent="ptzStart('LEFT_UP')"><el-icon><TopLeft /></el-icon></button>
            <button class="ptz__btn" :class="{ 'is-pressed': activeCmd === 'UP' }" @pointerdown.prevent="ptzStart('UP')"><el-icon><Top /></el-icon></button>
            <button class="ptz__btn" :class="{ 'is-pressed': activeCmd === 'RIGHT_UP' }" @pointerdown.prevent="ptzStart('RIGHT_UP')"><el-icon><TopRight /></el-icon></button>
            <button class="ptz__btn" :class="{ 'is-pressed': activeCmd === 'LEFT' }" @pointerdown.prevent="ptzStart('LEFT')"><el-icon><Back /></el-icon></button>
            <button class="ptz__btn ptz__home"><el-icon><Aim /></el-icon></button>
            <button class="ptz__btn" :class="{ 'is-pressed': activeCmd === 'RIGHT' }" @pointerdown.prevent="ptzStart('RIGHT')"><el-icon><Right /></el-icon></button>
            <button class="ptz__btn" :class="{ 'is-pressed': activeCmd === 'LEFT_DOWN' }" @pointerdown.prevent="ptzStart('LEFT_DOWN')"><el-icon><BottomLeft /></el-icon></button>
            <button class="ptz__btn" :class="{ 'is-pressed': activeCmd === 'DOWN' }" @pointerdown.prevent="ptzStart('DOWN')"><el-icon><Bottom /></el-icon></button>
            <button class="ptz__btn" :class="{ 'is-pressed': activeCmd === 'RIGHT_DOWN' }" @pointerdown.prevent="ptzStart('RIGHT_DOWN')"><el-icon><BottomRight /></el-icon></button>
          </div>
          <div class="ptz-tip">按住方向键移动，松开即停（也支持键盘方向键）</div>
        </div>

        <!-- 变倍/变焦（按住操作） -->
        <div class="mon-sec">
          <div class="mon-sec__title">变倍 / 变焦</div>
          <div class="mon-adj">
            <span class="mon-adj__label">变倍</span>
            <button class="mon-adj__btn" :class="{ 'is-pressed': activeCmd === 'ZOOM_IN' }" @pointerdown.prevent="ptzStart('ZOOM_IN')"><el-icon><ZoomIn /></el-icon></button>
            <button class="mon-adj__btn" :class="{ 'is-pressed': activeCmd === 'ZOOM_OUT' }" @pointerdown.prevent="ptzStart('ZOOM_OUT')"><el-icon><ZoomOut /></el-icon></button>
          </div>
          <div class="mon-adj">
            <span class="mon-adj__label">变焦</span>
            <button class="mon-adj__btn" :class="{ 'is-pressed': activeCmd === 'FOCUS_NEAR' }" @pointerdown.prevent="ptzStart('FOCUS_NEAR')"><el-icon><Plus /></el-icon></button>
            <button class="mon-adj__btn" :class="{ 'is-pressed': activeCmd === 'FOCUS_FAR' }" @pointerdown.prevent="ptzStart('FOCUS_FAR')"><el-icon><Minus /></el-icon></button>
          </div>
        </div>
      </aside>

      <!-- 右侧监控画面 -->
      <section class="mon__screen">
        <div ref="stageRef" class="mon-stage">
          <video ref="videoRef" class="mon-video" autoplay muted playsinline></video>

          <!-- 顶部信息条 -->
          <div class="mon-stage__top">
            <span class="mon-live" :class="status">
              <i class="mon-live__dot"></i>{{ statusText }}
            </span>
            <span class="mon-stage__name">{{ deviceName }}</span>
            <span class="mon-stage__time">{{ clock }}</span>
          </div>

          <!-- 加载/错误遮罩 -->
          <div v-if="status !== 'live'" class="mon-mask">
            <el-icon v-if="status === 'connecting'" class="is-loading mon-mask__icon"><Loading /></el-icon>
            <el-icon v-else class="mon-mask__icon mon-mask__icon--err"><VideoCameraFilled /></el-icon>
            <div class="mon-mask__text">{{ status === 'connecting' ? '正在连接监控流…' : '监控流连接失败' }}</div>
            <el-button v-if="status === 'error'" size="small" type="primary" round @click="reload">重试</el-button>
          </div>

          <!-- 底部工具条 -->
          <div class="mon-stage__bar">
            <button class="mon-tool" title="抓拍" @click="snapshot"><el-icon><Camera /></el-icon></button>
            <button class="mon-tool" :class="{ 'is-on': recording }" title="录制" @click="toggleRecord">
              <el-icon><VideoPlay /></el-icon>
            </button>
            <button class="mon-tool" title="声音" @click="toggleMute">
              <el-icon><Mute v-if="muted" /><Microphone v-else /></el-icon>
            </button>
            <div class="mon-tool__spacer"></div>
            <button class="mon-tool" title="刷新" @click="reload"><el-icon><Refresh /></el-icon></button>
            <button class="mon-tool" title="全屏" @click="toggleFullscreen"><el-icon><FullScreen /></el-icon></button>
          </div>
        </div>
      </section>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import Hls from 'hls.js/dist/hls.min.js'
import {
  VideoCamera, VideoCameraFilled, Close, Loading, Aim, Top, Bottom, Back, Right,
  TopLeft, TopRight, BottomLeft, BottomRight, ZoomIn, ZoomOut, Plus, Minus,
  Camera, VideoPlay, Refresh, FullScreen, Mute, Microphone
} from '@element-plus/icons-vue'
import { getPreviewUrl, ptzControl } from '@/api/build/drive'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  device: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue'])

// 取流开关：true=调接口取流地址(传 protocol:hls 拿 HLS)；false=用下面的死数据流地址
const USE_API_STREAM = true
// 死数据流地址（USE_API_STREAM=false 时使用）
const TEST_RTSP = 'rtsp://192.168.0.231:8554/hik01'

const monRef = ref(null)
const videoRef = ref(null)
const stageRef = ref(null)
const status = ref('connecting') // connecting | live | error
const muted = ref(true)
const recording = ref(false)
const clock = ref('')
const statusText = ref('连接中')

const deviceName = computed(() => (props.device && (props.device.name || props.device.deviceName)) || '监控设备')

let hls = null
let clockTimer = null
let mediaRecorder = null
let recordChunks = []

// 媒体服务器 HLS 端口（mediamtx 默认 8888；按实际媒体服务器改这里）
const HLS_PORT = 8888

// rtsp://host:554/path -> http://host:HLS_PORT/path/index.m3u8
function rtspToHls(rtsp) {
  const m = /^rtsps?:\/\/([^/:]+)(?::\d+)?\/(.+?)\/?$/.exec(rtsp || '')
  if (!m) return rtsp
  return `http://${m[1]}:${HLS_PORT}/${m[2]}/index.m3u8`
}

// rtsp 转 hls，已是 http(s) 则原样使用
function resolveStreamUrl(u) {
  if (!u) return ''
  return /^rtsps?:\/\//i.test(u) ? rtspToHls(u) : u
}

function setStatus(s) {
  status.value = s
  statusText.value = s === 'live' ? '直播中' : (s === 'connecting' ? '连接中' : '已断开')
}

async function play() {
  destroyHls()
  setStatus('connecting')
  const video = videoRef.value
  if (!video) return

  // 取流地址
  let raw = TEST_RTSP
  if (USE_API_STREAM) {
    if (!props.device) { setStatus('error'); return }
    try {
      const res = await getPreviewUrl({
        driveId: props.device.driveId,
        cameraIndexCode: props.device.serialNumber,
        protocol: 'hls'
      })
      raw = (res.data && (res.data.url || res.data)) || ''
      if (!raw) { console.warn('[监控] 接口未返回流地址'); setStatus('error'); return }
    } catch (e) {
      console.warn('[监控] 获取流地址失败', e)
      setStatus('error')
      return
    }
  }

  startPlay(resolveStreamUrl(raw))
}

function startPlay(url) {
  const video = videoRef.value
  if (!video || !url) { setStatus('error'); return }
  const isHls = /\.m3u8(\?|$)/i.test(url)

  if (isHls && Hls.isSupported()) {
    hls = new Hls({ lowLatencyMode: true, liveSyncDurationCount: 2, maxBufferLength: 6 })
    hls.loadSource(url)
    hls.attachMedia(video)
    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      video.play().then(() => setStatus('live')).catch(() => setStatus('live'))
    })
    hls.on(Hls.Events.ERROR, (e, data) => {
      if (data.fatal) {
        console.warn('[监控] HLS 错误', data.type, data.details)
        setStatus('error')
      }
    })
  } else {
    // 原生 HLS（Safari）或 http 直链
    video.src = url
    video.addEventListener('loadedmetadata', () => video.play(), { once: true })
    video.addEventListener('playing', () => setStatus('live'), { once: true })
    video.addEventListener('error', () => setStatus('error'), { once: true })
  }
}

function destroyHls() {
  if (hls) {
    try { hls.destroy() } catch (e) { /* ignore */ }
    hls = null
  }
}

function reload() {
  play()
}

/* —— 工具条 —— */
function snapshot() {
  const video = videoRef.value
  if (!video || !video.videoWidth) {
    ElMessage.warning('画面未就绪，无法抓拍')
    return
  }
  const canvas = document.createElement('canvas')
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height)
  const a = document.createElement('a')
  a.href = canvas.toDataURL('image/png')
  a.download = `snapshot_${Date.now()}.png`
  a.click()
  ElMessage.success('已抓拍')
}

function toggleRecord() {
  if (recording.value) {
    mediaRecorder && mediaRecorder.stop()
    return
  }
  const video = videoRef.value
  const stream = video && video.captureStream && video.captureStream()
  if (!stream) {
    ElMessage.warning('当前环境不支持录制')
    return
  }
  try {
    recordChunks = []
    mediaRecorder = new MediaRecorder(stream, { mimeType: 'video/webm' })
    mediaRecorder.ondataavailable = e => e.data.size && recordChunks.push(e.data)
    mediaRecorder.onstop = () => {
      const blob = new Blob(recordChunks, { type: 'video/webm' })
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = `record_${Date.now()}.webm`
      a.click()
      recording.value = false
      ElMessage.success('录制已保存')
    }
    mediaRecorder.start()
    recording.value = true
    ElMessage.success('开始录制')
  } catch (e) {
    ElMessage.warning('录制启动失败')
  }
}

function toggleMute() {
  muted.value = !muted.value
  if (videoRef.value) videoRef.value.muted = muted.value
}

// 全屏整个监控面板（左侧操作栏 + 画面），保证全屏后操作栏仍在
function toggleFullscreen() {
  const el = monRef.value
  if (!el) return
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    el.requestFullscreen && el.requestFullscreen()
  }
}

/* —— 云台/镜头控制（按下开始、松开/取消/失焦/超时停止） —— */
const activeCmd = ref(null) // 当前正在执行的命令（用于按钮高亮）
const MAX_HOLD = 10000 // 安全：单次最长按住时长(ms)，超时强制停
let safetyTimer = null

function ptzSend(command, action) {
  if (!props.device) return
  ptzControl({
    cameraIndexCode: props.device.serialNumber,
    action, // 0 开始 / 1 停止
    command,
    driveId: props.device.driveId
  }).catch(() => {})
}

function ptzStart(command) {
  if (!props.device) {
    ElMessage.warning('缺少设备信息，无法控制')
    return
  }
  if (activeCmd.value === command) return // 已在执行（键盘连发等）
  if (activeCmd.value) ptzStop() // 先停掉上一个方向
  activeCmd.value = command
  ptzSend(command, 0)
  // 多重保险停止：松开 / 指针取消 / 窗口失焦 / 超时
  window.addEventListener('pointerup', ptzStop)
  window.addEventListener('pointercancel', ptzStop)
  window.addEventListener('blur', ptzStop)
  clearTimeout(safetyTimer)
  safetyTimer = setTimeout(ptzStop, MAX_HOLD)
}

function ptzStop() {
  if (!activeCmd.value) return
  ptzSend(activeCmd.value, 1)
  activeCmd.value = null
  clearTimeout(safetyTimer)
  window.removeEventListener('pointerup', ptzStop)
  window.removeEventListener('pointercancel', ptzStop)
  window.removeEventListener('blur', ptzStop)
}

// 键盘方向键控制
const KEY_MAP = {
  ArrowUp: 'UP', ArrowDown: 'DOWN', ArrowLeft: 'LEFT', ArrowRight: 'RIGHT'
}
function onKeyDown(e) {
  const cmd = KEY_MAP[e.key]
  if (!cmd || e.repeat) return
  e.preventDefault()
  ptzStart(cmd)
}
function onKeyUp(e) {
  if (KEY_MAP[e.key]) ptzStop()
}

/* —— 生命周期 —— */
function startClock() {
  const update = () => {
    const n = new Date()
    const p = x => String(x).padStart(2, '0')
    clock.value = `${n.getFullYear()}-${p(n.getMonth() + 1)}-${p(n.getDate())} ${p(n.getHours())}:${p(n.getMinutes())}:${p(n.getSeconds())}`
  }
  update()
  clockTimer = setInterval(update, 1000)
}

function onOpened() {
  muted.value = true
  recording.value = false
  startClock()
  play()
  window.addEventListener('keydown', onKeyDown)
  window.addEventListener('keyup', onKeyUp)
}

function onClosed() {
  destroyHls()
  ptzStop() // 关闭时确保停止云台
  if (mediaRecorder && recording.value) {
    try { mediaRecorder.stop() } catch (e) { /* ignore */ }
  }
  recording.value = false
  if (clockTimer) { clearInterval(clockTimer); clockTimer = null }
  if (videoRef.value) videoRef.value.removeAttribute('src')
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('keyup', onKeyUp)
}

function close() {
  emit('update:modelValue', false)
}

onBeforeUnmount(onClosed)
</script>

<style scoped lang="scss">
:global(.mon-dialog),
:deep(.mon-dialog) {
  border-radius: 24px;
  overflow: hidden;
  padding: 0 !important;
  background:
    radial-gradient(circle at 18% 0%, rgba(57, 119, 255, 0.22), transparent 34%),
    linear-gradient(135deg, #121d30 0%, #0c1422 52%, #07101c 100%);
  border: 1px solid rgba(135, 170, 255, 0.22);
  box-shadow:
    0 34px 90px rgba(1, 8, 20, 0.68),
    0 0 0 1px rgba(255, 255, 255, 0.05) inset;
}

:global(.mon-dialog .el-dialog__header),
:deep(.mon-dialog .el-dialog__header) {
  padding: 0;
  margin: 0;
}

:global(.mon-dialog .el-dialog__body),
:deep(.mon-dialog .el-dialog__body) {
  padding: 0;
  background: transparent;
}

.mon__header {
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px 0 22px;
  background:
    linear-gradient(90deg, rgba(12, 20, 34, 0.96), rgba(20, 32, 52, 0.9)),
    radial-gradient(circle at 0 0, rgba(64, 132, 255, 0.26), transparent 42%);
  color: #f3f7ff;
  border-bottom: 1px solid rgba(130, 166, 255, 0.16);
}

.mon__title {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 15.5px;
  font-weight: 700;
  letter-spacing: 0.5px;

  .el-icon {
    width: 30px;
    height: 30px;
    display: inline-grid;
    place-items: center;
    border-radius: 10px;
    background: rgba(69, 132, 255, 0.16);
    color: #8fb8ff;
    box-shadow: 0 0 18px rgba(65, 125, 255, 0.25) inset;
  }
}

.mon__close {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.07);
  color: #d5def0;
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover {
    background: linear-gradient(135deg, #ff6370, #e83d56);
    border-color: transparent;
    color: #fff;
    transform: rotate(90deg);
  }
}

.mon {
  display: flex;
  height: min(660px, 76vh);
  padding: 14px;
  gap: 14px;
  background:
    linear-gradient(180deg, rgba(13, 23, 38, 0.72), rgba(6, 12, 21, 0.9)),
    radial-gradient(circle at 72% 12%, rgba(35, 101, 255, 0.16), transparent 30%);

  &:fullscreen {
    width: 100vw;
    height: 100vh;
    padding: 18px;
  }
}

/* 左侧操作栏 */
.mon__ctrl {
  flex: 0 0 258px;
  padding: 16px;
  overflow: auto;
  background:
    linear-gradient(180deg, rgba(25, 38, 61, 0.92), rgba(13, 22, 37, 0.92)),
    radial-gradient(circle at 10% 0%, rgba(75, 134, 255, 0.18), transparent 38%);
  border: 1px solid rgba(126, 157, 213, 0.18);
  border-radius: 18px;
  box-shadow:
    0 18px 42px rgba(2, 8, 18, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  scrollbar-width: thin;
  scrollbar-color: rgba(119, 151, 205, 0.35) transparent;
}

/* 设备信息卡 */
.mon-dev {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 14px;
  margin-bottom: 14px;
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(59, 130, 246, 0.18), rgba(255, 255, 255, 0.04)),
    rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(137, 173, 255, 0.16);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.mon-dev__icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  background: linear-gradient(135deg, #5b8dff, #356bff 52%, #15b8ff);
  color: #fff;
  font-size: 20px;
  box-shadow: 0 12px 28px rgba(42, 101, 255, 0.35);
}

.mon-dev__name {
  font-size: 14.5px;
  font-weight: 600;
  color: #e7eef7;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mon-dev__status {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 4px;
  font-size: 12.5px;
  color: #8a94a3;

  i {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #8a94a3;
  }

  &.live {
    color: #3ddc84;
    i { background: #3ddc84; box-shadow: 0 0 6px #3ddc84; }
  }

  &.connecting {
    color: #ffd34e;
    i { background: #ffd34e; }
  }
}

.mon-sec {
  margin-bottom: 14px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(138, 160, 205, 0.13);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);

  &__title {
    display: flex;
    align-items: center;
    gap: 7px;
    margin-bottom: 12px;
    font-size: 13px;
    font-weight: 600;
    letter-spacing: 1.2px;
    color: #98a8c2;
    text-transform: uppercase;

    &::before {
      content: '';
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #5aa7ff;
      box-shadow: 0 0 12px rgba(80, 158, 255, 0.75);
    }
  }
}

/* 云台九宫格 */
.ptz {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 9px;
}

.ptz__btn {
  height: 50px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(143, 171, 220, 0.16);
  border-radius: 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.028)),
    rgba(18, 28, 46, 0.86);
  color: #dbe7fb;
  font-size: 17px;
  cursor: pointer;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 8px 18px rgba(0, 0, 0, 0.18);
  transition: all 0.18s ease;

  &:hover {
    background: linear-gradient(135deg, #3d7dff, #19c4ff);
    color: #fff;
    border-color: transparent;
    transform: translateY(-2px);
    box-shadow: 0 12px 24px rgba(31, 117, 255, 0.32);
  }

  /* 按住激活：高亮发光 */
  &.is-pressed {
    background: linear-gradient(135deg, #3d7dff, #19c4ff);
    color: #fff;
    border-color: transparent;
    box-shadow:
      0 0 0 2px rgba(54, 132, 255, 0.45),
      0 0 24px rgba(31, 194, 255, 0.45);
    transform: scale(0.96);
  }
}

.ptz-tip {
  margin-top: 10px;
  font-size: 11.5px;
  line-height: 1.5;
  color: #78889f;
  text-align: center;
}

.ptz__home {
  background: rgba(255, 255, 255, 0.025);
  color: #677993;
  cursor: default;

  &:hover {
    background: rgba(255, 255, 255, 0.025);
    color: #677993;
    transform: none;
    box-shadow: none;
  }
}

/* 变倍/变焦 */
.mon-adj {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 9px;

  &__label {
    width: 34px;
    font-size: 13px;
    color: #9aacbf;
  }

  &__btn {
    flex: 1;
    height: 38px;
    display: grid;
    place-items: center;
    border: 1px solid rgba(143, 171, 220, 0.16);
    border-radius: 12px;
    background:
      linear-gradient(180deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.028)),
      rgba(18, 28, 46, 0.86);
    color: #dbe7fb;
    cursor: pointer;
    transition: all 0.18s ease;

    &:hover {
      background: linear-gradient(135deg, #3d7dff, #19c4ff);
      color: #fff;
      border-color: transparent;
      box-shadow: 0 10px 20px rgba(31, 117, 255, 0.26);
    }

    &.is-pressed {
      background: linear-gradient(135deg, #3d7dff, #19c4ff);
      color: #fff;
      border-color: transparent;
      box-shadow: 0 0 0 2px rgba(54, 132, 255, 0.45), 0 0 20px rgba(31, 194, 255, 0.4);
      transform: scale(0.96);
    }
  }
}

/* 预置位 */
.mon-preset {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.mon-preset__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 38px;
  border: 1px solid rgba(143, 171, 220, 0.16);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.045);
  color: #dbe7fb;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: linear-gradient(135deg, #3d7dff, #19c4ff);
    color: #fff;
    border-color: transparent;
  }
}

/* 右侧画面 */
.mon__screen {
  flex: 1;
  min-width: 0;
  padding: 0;
  display: flex;
}

.mon-stage {
  position: relative;
  flex: 1;
  min-width: 0;
  border-radius: 20px;
  overflow: hidden;
  background:
    radial-gradient(circle at center, #07101b 0%, #03070d 58%, #010307 100%);
  border: 1px solid rgba(134, 164, 220, 0.16);
  box-shadow:
    0 22px 50px rgba(0, 0, 0, 0.36),
    inset 0 0 0 1px rgba(255, 255, 255, 0.035);
}

.mon-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: transparent;
}

.mon-stage__top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px 34px;
  background: linear-gradient(180deg, rgba(1, 7, 16, 0.78), rgba(1, 7, 16, 0));
  color: #fff;
  font-size: 13px;
  backdrop-filter: blur(1px);
}

.mon-live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 11px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.13);
  border: 1px solid rgba(255, 255, 255, 0.08);

  &__dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #bbb;
  }

  &.live {
    background: rgba(34, 197, 94, 0.88);
    .mon-live__dot { background: #fff; animation: mon-blink 1.2s infinite; }
  }

  &.connecting .mon-live__dot { background: #ffd34e; }
  &.error {
    background: rgba(244, 63, 94, 0.9);
    .mon-live__dot { background: #fff; }
  }
}

@keyframes mon-blink {
  50% { opacity: 0.3; }
}

.mon-stage__name {
  font-weight: 600;
}

.mon-stage__time {
  margin-left: auto;
  font-variant-numeric: tabular-nums;
  color: rgba(255, 255, 255, 0.82);
}

.mon-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: #c4d0df;
  background:
    radial-gradient(circle at center, rgba(40, 80, 150, 0.18), rgba(3, 8, 16, 0.72) 45%, rgba(3, 7, 13, 0.82)),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.025) 0, rgba(255, 255, 255, 0.025) 1px, transparent 1px, transparent 32px);

  &__icon {
    font-size: 50px;
    filter: drop-shadow(0 12px 26px rgba(0, 0, 0, 0.35));
    &--err { color: #ff6678; }
  }

  &__text {
    font-size: 14px;
    letter-spacing: 0.2px;
  }
}

.mon-stage__bar {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 34px 16px 14px;
  background: linear-gradient(0deg, rgba(1, 7, 16, 0.82), rgba(1, 7, 16, 0));
}

.mon-tool {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 13px;
  background: rgba(16, 27, 45, 0.72);
  color: #eaf2ff;
  font-size: 17px;
  cursor: pointer;
  backdrop-filter: blur(12px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.07);
  transition: all 0.16s ease;

  &:hover {
    background: linear-gradient(135deg, rgba(61, 125, 255, 0.9), rgba(25, 196, 255, 0.9));
    border-color: transparent;
    transform: translateY(-1px);
  }

  &:active {
    transform: scale(0.94);
  }

  &.is-on {
    background: linear-gradient(135deg, #ff4d65, #f43f5e);
    box-shadow: 0 0 20px rgba(244, 63, 94, 0.35);
  }

  &__spacer {
    flex: 1;
  }
}
</style>
