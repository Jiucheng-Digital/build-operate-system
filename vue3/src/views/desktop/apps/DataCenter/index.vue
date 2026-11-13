<template>
  <AppWindow title="数据中心" @close="$emit('close')" @minimize="$emit('minimize')">
    <div class="dt">
      <!-- 左侧分类目录（同设备中心） -->
      <aside class="dt__nav">
        <button
          type="button"
          v-for="c in categories"
          :key="c.code"
          class="dt__nav-item"
          :class="{ 'is-active': current && current.code === c.code }"
          @click="selectCategory(c)"
        >
          <el-icon class="dt__nav-icon"><Box /></el-icon>
          <span>{{ c.name }}</span>
        </button>
      </aside>

      <!-- 右侧 -->
      <section class="dt__main">
        <header class="dt__head">
          <h2 class="dt__title">数据概览</h2>
          <div class="dt__filters">
            <el-select v-model="query.manufacturer" class="dt__mfr" placeholder="厂商" clearable @change="search">
              <el-option v-for="m in manufacturerOptions" :key="m.dictValue" :label="m.dictLabel" :value="m.dictValue" />
            </el-select>
            <el-input
              v-model="query.name"
              class="dt__search"
              placeholder="查询"
              clearable
              :prefix-icon="Search"
              @keyup.enter="search"
              @clear="search"
            />
            <el-button :icon="Refresh" @click="loadData">刷新</el-button>
          </div>
        </header>

        <div class="dt__body" v-loading="loading">
          <div v-if="list.length" class="dt__cards">
            <div v-for="d in list" :key="d.id" class="dt-card">
              <div class="dt-card__head">
                <div class="dt-card__title">
                  <el-icon class="dt-card__icon"><Box /></el-icon>
                  <span class="dt-card__name" :title="d.name">{{ d.name || '—' }}</span>
                </div>
                <el-tag :type="d.status === 'online' ? 'success' : 'danger'" size="small" effect="light">
                  {{ d.status === 'online' ? '在线' : (d.status === 'offline' ? '离线' : (d.status || '—')) }}
                </el-tag>
              </div>

              <div class="dt-card__grid">
                <div class="dt-metric">
                  <div class="dt-metric__label">序列号</div>
                  <div class="dt-metric__val" :title="d.serialNumber">{{ d.serialNumber || '—' }}</div>
                </div>
                <div class="dt-metric">
                  <div class="dt-metric__label">厂商</div>
                  <div class="dt-metric__val">{{ manufacturerLabel(d.manufacturer) }}</div>
                </div>
                <div class="dt-metric dt-metric--full">
                  <div class="dt-metric__label">位置</div>
                  <div class="dt-metric__val" :title="d.address">{{ d.address || '—' }}</div>
                </div>
              </div>

              <div class="dt-card__foot">
                <a class="dt-card__history" @click="onHistory(d)">+ 历史数据</a>
              </div>
            </div>
          </div>

          <el-empty v-else-if="!loading" description="暂无数据" :image-size="90" />
        </div>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="query.pageNum"
          v-model:limit="query.pageSize"
          :page-sizes="[12, 24, 48]"
          @pagination="loadData"
        />
      </section>
    </div>
  </AppWindow>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, Search, Refresh } from '@element-plus/icons-vue'
import AppWindow from '../../components/AppWindow.vue'
import { listClassify, getDevicePage } from '@/api/build/drive'
import { getDicts } from '@/api/system/dict/data'

defineEmits(['close', 'minimize'])

const categories = ref([])
const current = ref(null)
const manufacturerOptions = ref([])

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 12, classify: undefined, name: undefined, manufacturer: undefined })

// 厂商 dictValue -> dictLabel
const manufacturerMap = computed(() => {
  const map = {}
  manufacturerOptions.value.forEach(m => { map[m.dictValue] = m.dictLabel })
  return map
})
function manufacturerLabel(v) {
  if (!v) return '—'
  return manufacturerMap.value[v] || v
}

function loadCategories() {
  listClassify()
    .then(res => {
      const all = res.data || []
      categories.value = all.filter(c => !all.some(x => x.parentCode === c.code))
      if (categories.value.length) selectCategory(categories.value[0])
    })
    .catch(() => {})
}

function loadManufacturers() {
  getDicts('drive_manufacturer').then(res => { manufacturerOptions.value = res.data || [] }).catch(() => {})
}

function selectCategory(c) {
  current.value = c
  query.classify = c.code
  query.name = undefined
  query.manufacturer = undefined
  query.pageNum = 1
  loadData()
}

function loadData() {
  loading.value = true
  const params = { pageNum: query.pageNum, pageSize: query.pageSize, classify: query.classify }
  if (query.name) params.name = query.name
  if (query.manufacturer) params.manufacturer = query.manufacturer
  getDevicePage(params)
    .then(res => {
      list.value = res.rows || []
      total.value = res.total || 0
    })
    .catch(() => {})
    .finally(() => { loading.value = false })
}

function search() {
  query.pageNum = 1
  loadData()
}

function onHistory() {
  ElMessage({ message: '历史数据功能开发中', type: 'info', grouping: true })
}

onMounted(() => {
  loadCategories()
  loadManufacturers()
})
</script>

<style scoped lang="scss">
.dt {
  flex: 1;
  display: flex;
  min-width: 0;
  min-height: 0;
}

/* 左侧 */
.dt__nav {
  flex: 0 0 180px;
  padding: 14px 10px;
  overflow: auto;
  background: #f6f8fb;
  border-right: 1px solid #eef1f5;
}

.dt__nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 42px;
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

.dt__nav-icon {
  font-size: 16px;
}

/* 右侧 */
.dt__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 18px 22px;
}

.dt__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.dt__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1f2d3d;
}

.dt__filters {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dt__mfr {
  width: 140px;
}

.dt__search {
  width: 200px;
}

.dt__body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

/* 卡片网格 */
.dt__cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.dt-card {
  position: relative;
  padding: 18px 18px 16px;
  border: 1px solid #eef1f5;
  border-radius: 14px;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(31, 80, 140, 0.04);
  transition: box-shadow 0.15s, transform 0.15s;

  /* 顶部渐变样式条 */
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, #3b82f6, #2f6bff);
  }

  &:hover {
    box-shadow: 0 8px 22px rgba(31, 80, 140, 0.1);
    transform: translateY(-2px);
  }
}

.dt-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 12px;
  margin-bottom: 12px;
  border-bottom: 1px solid #f2f4f7;
}

.dt-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.dt-card__icon {
  flex: 0 0 auto;
  font-size: 18px;
  color: #2f74ff;
}

.dt-card__name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dt-card__head :deep(.el-tag) {
  flex: 0 0 auto;
}

.dt-card__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 12px;
}

.dt-metric {
  min-width: 0; /* 让省略号在 grid 里生效，避免长文本挤出卡片 */
}

.dt-metric--full {
  grid-column: 1 / -1;
}

.dt-metric__label {
  font-size: 12.5px;
  color: #9aa7b4;
}

.dt-metric__val {
  margin-top: 3px;
  font-size: 14.5px;
  font-weight: 600;
  color: #303a45;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dt-card__foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.dt-card__history {
  font-size: 13px;
  color: #2f74ff;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}
</style>
