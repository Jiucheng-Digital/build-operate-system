<template>
  <AppWindow title="驱动管理" @close="$emit('close')" @minimize="$emit('minimize')">
    <div class="dm">
      <!-- 左侧导航 -->
      <aside class="dm__nav">
        <button
          type="button"
          v-for="item in navs"
          :key="item.key"
          class="dm__nav-item"
          :class="{ 'is-active': activeNav === item.key }"
          @click="setActiveNav(item.key)"
        >
          <el-icon class="dm__nav-icon"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </aside>

      <!-- 右侧内容 -->
      <section class="dm__main">
        <template v-if="activeNav === 'overview'">
          <header class="dm__head">
            <h2 class="dm__title">驱动总览</h2>
            <el-button type="primary" :icon="Upload" @click="uploadVisible = true">上传驱动</el-button>
          </header>

          <div class="dm__panel">
            <div class="dm__panel-head">
              <span class="dm__panel-title"><el-icon><Grid /></el-icon> 驱动列表</span>
              <el-input
                v-model="queryParams.name"
                class="dm__search"
                placeholder="查询"
                clearable
                :prefix-icon="Search"
                @keyup.enter="handleQuery"
                @clear="handleQuery"
              />
            </div>

            <el-table v-loading="loading" :data="list" class="dm__table" height="100%">
              <el-table-column label="logo" width="72" align="center">
                <template #default="{ row }">
                  <div class="dm-logo">
                    <img :src="row.icon || defaultLogo" alt="logo" @error="e => (e.target.src = defaultLogo)" />
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="驱动名称" prop="name" min-width="140" show-overflow-tooltip />
              <el-table-column label="版本" width="84" align="center">
                <template #default="{ row }">{{ row.version || '—' }}</template>
              </el-table-column>
              <el-table-column label="所属类别" prop="classifyName" width="100" align="center">
                <template #default="{ row }">{{ row.classifyName || '—' }}</template>
              </el-table-column>
              <el-table-column label="厂商" prop="type" width="100" align="center">
                <template #default="{ row }">{{ manufacturerLabel(row.type) }}</template>
              </el-table-column>
              <el-table-column label="驱动描述" prop="remark" min-width="150" show-overflow-tooltip>
                <template #default="{ row }">{{ row.remark || '—' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="176" align="center" fixed="right" class-name="dm-op-col">
                <template #default="{ row }">
                  <template v-if="row.status === 'install_success'">
                    <el-button link type="success" size="small" :icon="RefreshRight" @click="handleUpdate(row)">更新设备</el-button>
                    <el-button link type="danger" size="small" :icon="CircleClose" @click="handleUninstall(row)">卸载</el-button>
                  </template>
                  <el-button v-else link type="primary" size="small" :icon="Download" @click="handleInstall(row)">安装</el-button>
                </template>
              </el-table-column>
            </el-table>

            <pagination
              v-show="total > 0"
              :total="total"
              v-model:page="queryParams.pageNum"
              v-model:limit="queryParams.pageSize"
              :page-sizes="[5, 10, 20, 30]"
              @pagination="fetchList"
            />
          </div>
        </template>

        <!-- 驱动开发指南 -->
        <template v-else>
          <header class="dm__head">
            <div>
              <h2 class="dm__title">驱动开发指南</h2>
<!--              <p class="dm__subtitle">接口返回 Markdown 文件后，在这里直接展示文档内容。</p>-->
            </div>
            <div class="dm__guide-actions">
              <el-button :icon="RefreshRight" :loading="guideLoading" @click="loadDevelopmentGuide(true)">刷新</el-button>
              <el-button v-if="guideUrl" :icon="Document" @click="openGuideSource">下载</el-button>
            </div>
          </header>

          <div class="dm__guide" v-loading="guideLoading">
            <div v-if="guideHtml" class="dm__guide-content markdown-body" v-html="guideHtml" />
            <el-empty v-else :description="guideError || '暂无驱动开发指南'">
              <el-button type="primary" :loading="guideLoading" @click="loadDevelopmentGuide(true)">重新加载</el-button>
            </el-empty>
          </div>
        </template>
      </section>
    </div>

    <UploadDialog v-model="uploadVisible" @success="onUploadSuccess" />
  </AppWindow>
</template>

<script setup>
import { ref, reactive, computed, onMounted, markRaw } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Search, Grid, Document, Monitor, Download, RefreshRight, CircleClose } from '@element-plus/icons-vue'
import AppWindow from '../../components/AppWindow.vue'
import UploadDialog from './UploadDialog.vue'
import { listDrive, installDrive, uninstallDrive, updateDriveDevice, getDevelopmentGuide } from '@/api/build/drive'
import { getDicts } from '@/api/system/dict/data'
import defaultLogo from '../../assets/icons/driver.svg'

// 厂商字典：type(hikvision) -> dictLabel(海康)，与设备中心一致
const manufacturerOptions = ref([])
const manufacturerMap = computed(() => {
  const map = {}
  manufacturerOptions.value.forEach(m => { map[m.dictValue] = m.dictLabel })
  return map
})
function manufacturerLabel(v) {
  if (!v) return '—'
  return manufacturerMap.value[v] || v
}

defineEmits(['close', 'minimize'])

const navs = [
  { key: 'overview', label: '驱动总览', icon: markRaw(Monitor) },
  { key: 'guide', label: '驱动开发指南', icon: markRaw(Document) }
]
const activeNav = ref('overview')

const loading = ref(false)
const list = ref([])
const total = ref(0)
const uploadVisible = ref(false)
const guideLoading = ref(false)
const guideLoaded = ref(false)
const guideUrl = ref('')
const guideHtml = ref('')
const guideError = ref('')

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: undefined
})

function fetchList() {
  loading.value = true
  listDrive(queryParams)
    .then(res => {
      list.value = res.rows || []
      total.value = res.total || 0
    })
    .catch(() => {})
    .finally(() => {
      loading.value = false
    })
}

// 查询：回到第一页
function handleQuery() {
  queryParams.pageNum = 1
  fetchList()
}

// 上传成功：刷新当前页（服务端分页，仅拉取一页，性能可控）
function onUploadSuccess() {
  queryParams.pageNum = 1
  fetchList()
}

function setActiveNav(key) {
  activeNav.value = key
  if (key === 'guide') {
    loadDevelopmentGuide()
  }
}

async function loadDevelopmentGuide(force = false) {
  if (guideLoading.value) return
  if (guideLoaded.value && !force) return

  guideLoading.value = true
  guideError.value = ''
  if (force) {
    guideHtml.value = ''
    guideLoaded.value = false
  }

  try {
    const res = await getDevelopmentGuide()
    const url = typeof res.data === 'string' ? res.data.trim() : ''
    guideUrl.value = url
    if (!url) {
      throw new Error('接口未返回驱动开发指南地址')
    }

    const fetchUrl = toGuideFetchUrl(url)
    const response = await fetch(fetchUrl, { cache: 'no-cache' })
    if (!response.ok) {
      throw new Error(`Markdown 文件加载失败（${response.status}）`)
    }

    const markdown = await response.text()
    guideHtml.value = renderMarkdown(markdown, fetchUrl)
    guideLoaded.value = true
  } catch (error) {
    guideHtml.value = ''
    guideLoaded.value = false
    guideError.value = error?.message || '驱动开发指南加载失败'
  } finally {
    guideLoading.value = false
  }
}

function openGuideSource() {
  if (!guideUrl.value) return
  window.open(encodeURI(guideUrl.value), '_blank', 'noopener,noreferrer')
}

function toGuideFetchUrl(url = '') {
  const raw = String(url).trim()
  if (!raw) return ''

  try {
    const parsed = new URL(raw, window.location.origin)
    if (parsed.pathname.startsWith('/file/')) {
      return `${parsed.pathname}${parsed.search}${parsed.hash}`
    }
    return parsed.href
  } catch (_error) {
    return raw
  }
}

function escapeHtml(value = '') {
  const text = String(value)
  const map = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }
  return text.replace(/[&<>"']/g, char => map[char])
}

function safeUrl(url = '', baseUrl = '', type = 'link') {
  const raw = String(url).trim().replace(/^["']|["']$/g, '')
  if (!raw) return '#'
  if (raw.startsWith('#')) return escapeHtml(raw)

  try {
    const parsed = new URL(raw, baseUrl)
    const allowImageData = type === 'image' && parsed.protocol === 'data:' && raw.startsWith('data:image/')
    if (['http:', 'https:', 'mailto:'].includes(parsed.protocol) || allowImageData) {
      return escapeHtml(parsed.href)
    }
  } catch (_error) {}

  return '#'
}

function renderInline(text = '', baseUrl = '') {
  const codeTokens = []
  let html = escapeHtml(text).replace(/`([^`]+)`/g, (_, code) => {
    const token = `@@CODE_${codeTokens.length}@@`
    codeTokens.push(`<code>${code}</code>`)
    return token
  })

  html = html
    .replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (_, alt, url) => `<img alt="${escapeHtml(alt)}" src="${safeUrl(url, baseUrl, 'image')}" />`)
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_, label, url) => `<a href="${safeUrl(url, baseUrl)}" target="_blank" rel="noopener noreferrer">${label}</a>`)
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/__([^_]+)__/g, '<strong>$1</strong>')
    .replace(/(^|[^*])\*([^*]+)\*/g, '$1<em>$2</em>')
    .replace(/(^|[^_])_([^_]+)_/g, '$1<em>$2</em>')

  codeTokens.forEach((code, index) => {
    html = html.replace(`@@CODE_${index}@@`, code)
  })
  return html
}

function splitTableRow(line) {
  return line.trim().replace(/^\||\|$/g, '').split('|').map(item => item.trim())
}

function isTableSeparator(line = '') {
  return /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$/.test(line)
}

function isBlockStart(line = '', nextLine = '') {
  return /^#{1,6}\s+/.test(line) ||
    /^```/.test(line) ||
    /^>\s?/.test(line) ||
    /^\s*[-*+]\s+/.test(line) ||
    /^\s*\d+[.)]\s+/.test(line) ||
    /^(-{3,}|\*{3,}|_{3,})$/.test(line.trim()) ||
    (line.includes('|') && isTableSeparator(nextLine))
}

function renderMarkdown(markdown = '', baseUrl = '') {
  const lines = String(markdown).replace(/\r\n/g, '\n').split('\n')
  const html = []
  let index = 0

  while (index < lines.length) {
    const line = lines[index]
    const trimmed = line.trim()
    const nextLine = lines[index + 1] || ''

    if (!trimmed) {
      index += 1
      continue
    }

    if (/^```/.test(trimmed)) {
      const language = trimmed.replace(/^```/, '').trim()
      const code = []
      index += 1
      while (index < lines.length && !/^```/.test(lines[index].trim())) {
        code.push(lines[index])
        index += 1
      }
      index += 1
      html.push(`<pre><code${language ? ` class="language-${escapeHtml(language)}"` : ''}>${escapeHtml(code.join('\n'))}</code></pre>`)
      continue
    }

    const heading = trimmed.match(/^(#{1,6})\s+(.+)$/)
    if (heading) {
      const level = heading[1].length
      html.push(`<h${level}>${renderInline(heading[2], baseUrl)}</h${level}>`)
      index += 1
      continue
    }

    if (/^(-{3,}|\*{3,}|_{3,})$/.test(trimmed)) {
      html.push('<hr />')
      index += 1
      continue
    }

    if (trimmed.includes('|') && isTableSeparator(nextLine)) {
      const headers = splitTableRow(trimmed)
      const rows = []
      index += 2
      while (index < lines.length && lines[index].trim().includes('|')) {
        rows.push(splitTableRow(lines[index]))
        index += 1
      }
      html.push(`<table><thead><tr>${headers.map(item => `<th>${renderInline(item, baseUrl)}</th>`).join('')}</tr></thead><tbody>${rows.map(row => `<tr>${row.map(item => `<td>${renderInline(item, baseUrl)}</td>`).join('')}</tr>`).join('')}</tbody></table>`)
      continue
    }

    if (/^\s*[-*+]\s+/.test(line)) {
      const items = []
      while (index < lines.length && /^\s*[-*+]\s+/.test(lines[index])) {
        items.push(lines[index].replace(/^\s*[-*+]\s+/, ''))
        index += 1
      }
      html.push(`<ul>${items.map(item => `<li>${renderInline(item, baseUrl)}</li>`).join('')}</ul>`)
      continue
    }

    if (/^\s*\d+[.)]\s+/.test(line)) {
      const items = []
      while (index < lines.length && /^\s*\d+[.)]\s+/.test(lines[index])) {
        items.push(lines[index].replace(/^\s*\d+[.)]\s+/, ''))
        index += 1
      }
      html.push(`<ol>${items.map(item => `<li>${renderInline(item, baseUrl)}</li>`).join('')}</ol>`)
      continue
    }

    if (/^>\s?/.test(trimmed)) {
      const quote = []
      while (index < lines.length && /^>\s?/.test(lines[index].trim())) {
        quote.push(lines[index].trim().replace(/^>\s?/, ''))
        index += 1
      }
      html.push(`<blockquote>${quote.map(item => `<p>${renderInline(item, baseUrl)}</p>`).join('')}</blockquote>`)
      continue
    }

    const paragraph = [trimmed]
    index += 1
    while (index < lines.length && lines[index].trim() && !isBlockStart(lines[index].trim(), lines[index + 1] || '')) {
      paragraph.push(lines[index].trim())
      index += 1
    }
    html.push(`<p>${renderInline(paragraph.join(' '), baseUrl)}</p>`)
  }

  return html.join('\n')
}

function handleInstall(row) {
  ElMessageBox.confirm(`确认安装驱动「${row.name}」？`, '提示', { type: 'warning' })
    .then(() => installDrive(row.id))
    .then(() => {
      ElMessage.success('安装成功')
      fetchList()
    })
    .catch(() => {})
}

function handleUninstall(row) {
  ElMessageBox.confirm(`确认卸载驱动「${row.name}」？`, '提示', { type: 'warning' })
    .then(() => uninstallDrive(row.id))
    .then(() => {
      ElMessage.success('卸载成功')
      fetchList()
    })
    .catch(() => {})
}

function handleUpdate(row) {
  ElMessageBox.confirm(`确认更新「${row.name}」的设备？`, '提示', { type: 'warning' })
    .then(() => updateDriveDevice(row.id))
    .then(() => {
      ElMessage.success('已触发更新')
      fetchList()
    })
    .catch(() => {})
}

onMounted(() => {
  fetchList()
  getDicts('drive_manufacturer').then(res => { manufacturerOptions.value = res.data || [] }).catch(() => {})
})
</script>

<style scoped lang="scss">
.dm {
  flex: 1;
  display: flex;
  min-width: 0;
  min-height: 0;
}

/* 左侧导航 */
.dm__nav {
  flex: 0 0 168px;
  padding: 14px 10px;
  background: #f6f8fb;
  border-right: 1px solid #eef1f5;
}

.dm__nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 44px;
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

.dm__nav-icon {
  font-size: 16px;
}

/* 右侧内容 */
.dm__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 18px 22px;
}

.dm__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.dm__title {
  margin: 0;
  font-size: 23px;
  font-weight: 700;
  color: #1f2d3d;
}

.dm__subtitle {
  margin: 6px 0 0;
  color: #8a97a8;
  font-size: 13px;
}

.dm__panel {
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

.dm__panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.dm__panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16.5px;
  font-weight: 600;
  color: #1f2d3d;
}

.dm__search {
  width: 220px;
}

.dm__table {
  flex: 1;
  width: 100%;
  min-height: 0;
  font-size: 15px;
}

:deep(.dm__table .el-table__header-wrapper th) {
  font-size: 15px;
}

:deep(.dm__table .cell) {
  font-size: 15px;
}

/* 操作列按钮不换行 */
:deep(.dm-op-col .cell) {
  white-space: nowrap;
}

:deep(.dm-op-col .el-button + .el-button) {
  margin-left: 8px;
}

.dm-logo {
  display: inline-flex;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  background: #f2f5f9;
  align-items: center;
  justify-content: center;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.dm__guide {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid #eef1f5;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.05);
}

.dm__guide-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dm__guide-content {
  max-width: 980px;
  margin: 0 auto;
  padding: 28px 34px 38px;
  color: #2b3a4a;
  font-size: 14px;
  line-height: 1.75;
}

.markdown-body {
  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4),
  :deep(h5),
  :deep(h6) {
    margin: 22px 0 12px;
    color: #182738;
    font-weight: 700;
    line-height: 1.35;
  }

  :deep(h1) {
    margin-top: 0;
    padding-bottom: 12px;
    border-bottom: 1px solid #edf1f6;
    font-size: 26px;
  }

  :deep(h2) {
    padding-left: 10px;
    border-left: 4px solid #2f74ff;
    font-size: 21px;
  }

  :deep(h3) {
    font-size: 18px;
  }

  :deep(p) {
    margin: 10px 0;
  }

  :deep(a) {
    color: #2f74ff;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  :deep(ul),
  :deep(ol) {
    margin: 10px 0 10px 22px;
    padding: 0;
  }

  :deep(li) {
    margin: 4px 0;
  }

  :deep(blockquote) {
    margin: 14px 0;
    padding: 10px 14px;
    border-left: 4px solid #b9cdfa;
    border-radius: 6px;
    background: #f6f9ff;
    color: #5f6d7e;
  }

  :deep(pre) {
    overflow: auto;
    margin: 14px 0;
    padding: 14px 16px;
    border-radius: 10px;
    background: #111827;
    color: #e5e7eb;
    line-height: 1.65;
  }

  :deep(code) {
    padding: 2px 6px;
    border-radius: 5px;
    background: #eef3fb;
    color: #d6336c;
    font-family: Consolas, Monaco, "Courier New", monospace;
    font-size: 13px;
  }

  :deep(pre code) {
    padding: 0;
    background: transparent;
    color: inherit;
  }

  :deep(table) {
    width: 100%;
    margin: 16px 0;
    border-collapse: collapse;
    overflow: hidden;
    border: 1px solid #e4eaf2;
    border-radius: 8px;
  }

  :deep(th),
  :deep(td) {
    padding: 10px 12px;
    border: 1px solid #e4eaf2;
    text-align: left;
  }

  :deep(th) {
    background: #f5f8fc;
    color: #26384d;
    font-weight: 700;
  }

  :deep(img) {
    max-width: 100%;
    border-radius: 8px;
    box-shadow: 0 6px 20px rgba(31, 45, 61, 0.08);
  }

  :deep(hr) {
    height: 1px;
    margin: 24px 0;
    border: 0;
    background: #edf1f6;
  }
}
</style>
