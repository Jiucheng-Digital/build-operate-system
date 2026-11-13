import request from '@/utils/request'

// 驱动列表（分页）：GET /api/build/drive/list?pageNum=&pageSize=
export function listDrive(query) {
  return request({
    url: '/build/drive/list',
    method: 'get',
    params: query
  })
}

// 类型码（分类）列表：GET /build/classify/list，下拉用 name 展示、code 提交
export function listClassify() {
  return request({
    url: '/build/classify/list',
    method: 'get'
  })
}

// 按分类查询驱动列表（注册表树第二层）：GET /build/drive/listByClassify/{classifyCode}
export function listByClassify(classifyCode) {
  return request({
    url: `/build/drive/listByClassify/${classifyCode}`,
    method: 'get'
  })
}

// 驱动服务信息（接口/方法列表）：GET /build/drive/api?driveId=
export function getDriveApiList(driveId) {
  return request({
    url: '/build/drive/api',
    method: 'get',
    params: { driveId }
  })
}

// 驱动配置信息：GET /build/drive/getConfig?driveId=
export function getDriveConfig(driveId) {
  return request({
    url: '/build/drive/getConfig',
    method: 'get',
    params: { driveId }
  })
}

// 驱动开发指南：GET /build/drive/getDevelopmentGuide，返回 markdown 文件地址
export function getDevelopmentGuide() {
  return request({
    url: '/build/drive/getDevelopmentGuide',
    method: 'get'
  })
}

export function updateDriveConfig(data) {
  return request({
    url: '/build/drive/updateConfig',
    method: 'put',
    data
  })
}

// 设备信息（分页）：GET /build/device/page?pageNum=&pageSize=&manufacturer=&classify=
// manufacturer 取驱动的 type，classify 取驱动的 classifyCode
export function getDevicePage(query) {
  return request({
    url: '/build/device/page',
    method: 'get',
    params: query
  })
}

// 获取监控预览流地址：POST /build/monitor/previewURLs，{driveId, cameraIndexCode(序列号), protocol}，取 res.data.url
export function getPreviewUrl(data) {
  return request({
    url: '/build/monitor/previewURLs',
    method: 'post',
    data
  })
}

// 云台/镜头控制：POST /build/monitor/ptzs/control，{cameraIndexCode(序列号), action(0开始/1停止), command, driveId}
// 高频指令：关闭防重复提交，否则 1 秒内相同请求会被拦截器丢弃，导致控制不精准
export function ptzControl(data) {
  return request({
    url: '/build/monitor/ptzs/control',
    method: 'post',
    headers: { repeatSubmit: false },
    data
  })
}

// 系统资源监控（资源管理器）：GET /build/drive/monitor，无参
export function getDriveMonitor() {
  return request({
    url: '/build/drive/monitor',
    method: 'get'
  })
}

// 设备操作日志：GET /system/operlog/list?pageNum=&pageSize=&title=设备-
export function listDeviceOperLog(query) {
  return request({
    url: '/system/operlog/list',
    method: 'get',
    params: query
  })
}

// 环境检测：GET /build/drive/getEnvMonitorInfo，无参
export function getEnvMonitorInfo() {
  return request({
    url: '/build/drive/getEnvMonitorInfo',
    method: 'get'
  })
}

// 设备概览：GET /build/device/overview?classify=（classify 取驱动的 classifyCode）
export function getDeviceOverview(classify) {
  return request({
    url: '/build/device/overview',
    method: 'get',
    params: { classify }
  })
}

// 门禁控制：POST /build/door/doControl
// body: { doorIndexCodes: [序列号...](最多10个), controlType: 0常开/1门闭/2门开/3常闭, driveId }
export function doDoorControl(data) {
  return request({
    url: '/build/door/doControl',
    method: 'post',
    data
  })
}

// 开启人脸识别布控：POST /build/ai/face/start
// body: { deviceId(监控设备id), url(流地址), doorIndexCode(门禁serialNumber), doorDriveId(门禁driveId) }
export function faceStart(data) {
  return request({
    url: '/build/ai/face/start',
    method: 'post',
    data
  })
}

// 关闭人脸识别布控：POST /build/ai/face/stop/{deviceId}（deviceId=监控设备id，path 传参）
export function faceStop(deviceId) {
  return request({
    url: `/build/ai/face/stop/${deviceId}`,
    method: 'post'
  })
}

// 一键控制所有照明：POST /build/lighting/controlAllSensor，{ vvv: 'on'打开 / 'off'关闭 }
export function controlAllLighting(vvv) {
  return request({
    url: '/build/lighting/controlAllSensor',
    method: 'post',
    data: { vvv }
  })
}

// 控制单个照明：POST /build/lighting/controlSensor，{ deviceId: 主键id, vvv: 'on'/'off' }
export function controlLighting(deviceId, vvv) {
  return request({
    url: '/build/lighting/controlSensor',
    method: 'post',
    data: { deviceId, vvv }
  })
}

// 上传 LOGO 图片，返回存储地址（res.data.path）；与 ui3 一致走 /system/file/upload/{type}
export function uploadLogo(file, type = 'common') {
  const fd = new FormData()
  fd.append('file', file)
  return request({
    url: `/system/file/upload/${type}`,
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false },
    timeout: 120000,
    data: fd
  })
}

// 上传自定义驱动（multipart：file 驱动文件、icon LOGO存储地址(字符串)、name、version、type 厂商、classifyCode 类型码、remark 描述）
// 驱动文件可达 100MB，给足超时时间，避免大文件上传被默认 10s 超时打断
// onUploadProgress：文件上传实时进度回调（axios 原生）
export function uploadDrive(data, onUploadProgress) {
  return request({
    url: '/build/drive/upload',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false },
    timeout: 600000,
    onUploadProgress,
    data
  })
}

// 安装驱动：PUT /build/drive/install/{driveId}（driveId 为该行 id，path 传参）
export function installDrive(driveId) {
  return request({ url: `/build/drive/install/${driveId}`, method: 'put' })
}

// 卸载驱动：DELETE /build/drive/remove/{driveId}（driveId 为该行 id，path 传参）
export function uninstallDrive(driveId) {
  return request({ url: `/build/drive/remove/${driveId}`, method: 'delete' })
}

// 更新驱动设备：后端尚未给出确切地址，先占位，待确认后调整
export function updateDriveDevice(id) {
  return request({ url: '/build/drive/update', method: 'post', data: { id } })
}
