<template>
  <header class="desktop-header">
    <div class="desktop-header__brand">
      <img src="@/assets/logo/logo_bule.png" class="brand-logo" alt="Neucity 九城数字" />
      <span class="brand-divider" aria-hidden="true"></span>
      <div class="site-identity" title="当前接入站点">
        <el-icon><OfficeBuilding /></el-icon>
        <span>具身建筑智能运营中心</span>
      </div>
    </div>

    <form class="quick-entry" role="search" @submit.prevent="handleSearch">
      <el-icon aria-hidden="true"><Search /></el-icon>
      <input v-model.trim="quickQuery" type="search" aria-label="快速进入业务模块" placeholder="快速进入设备、事件或模块" />
      <button type="submit">进入</button>
    </form>

    <div class="desktop-header__tools">
      <div class="system-health" aria-label="平台运行数据状态">
        <span class="system-health__icon"><el-icon><CircleCheckFilled /></el-icon></span>
        <span><small>建筑运行态</small><strong>数据已接入</strong></span>
      </div>
      <time class="desktop-clock">{{ time }}</time>
      <el-dropdown trigger="click" @command="handleCommand">
        <button type="button" class="user-button" aria-label="打开用户菜单">
          <img v-if="avatar" :src="avatar" class="user-avatar" alt="用户头像" />
          <span v-else class="user-avatar user-avatar--fallback">{{ userInitial }}</span>
          <span class="user-copy"><strong>{{ displayName }}</strong><small>运行管理员</small></span>
          <el-icon><ArrowDown /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, CircleCheckFilled, OfficeBuilding, Search } from '@element-plus/icons-vue'
import { useClock } from '../composables/useClock'
import useUserStore from '@/store/modules/user'

const emit = defineEmits(['open'])
const router = useRouter()
const userStore = useUserStore()
const { time } = useClock()
const quickQuery = ref('')

const avatar = computed(() => userStore.avatar)
const displayName = computed(() => userStore.nickName || userStore.name || '管理员')
const userInitial = computed(() => displayName.value.slice(0, 1))

function handleSearch() {
  const keyword = quickQuery.value.toLowerCase()
  if (!keyword) return
  const routes = [
    { keys: ['系统', '用户', '角色', '菜单', '部门', '岗位', '字典', '参数', '展厅', '公告', '日志', 'system'], target: 'system' },
    { keys: ['事件', '告警', 'event'], target: 'event-hub' },
    { keys: ['资源', '环境', 'resource'], target: 'resource' },
    { keys: ['数据', 'data'], target: 'data-center' },
    { keys: ['驱动', 'driver'], target: 'driver' },
    { keys: ['注册', 'registry'], target: 'registry' },
    { keys: ['设备', '门禁', '照明', '视频', 'device'], target: 'device' }
  ]
  const matched = routes.find(item => item.keys.some(key => keyword.includes(key)))
  if (matched) {
    emit('open', matched.target)
    quickQuery.value = ''
  } else {
    ElMessage({ message: '未匹配到模块，可从左侧功能轨进入', type: 'info', grouping: true })
  }
}

function handleCommand(command) {
  if (command === 'logout') logout()
}

function logout() {
  ElMessageBox.confirm('确定退出当前账号吗？', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await userStore.logOut()
      router.replace('/login')
    })
    .catch(() => {})
}
</script>

<style scoped lang="scss">
.desktop-header {
  position: relative;
  z-index: 30;
  flex: 0 0 70px;
  display: grid;
  grid-template-columns: minmax(430px, 1fr) minmax(340px, 500px) minmax(420px, 1fr);
  align-items: center;
  gap: 24px;
  padding: 0 22px;
  border-bottom: 1px solid #dce5f0;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 4px 18px rgba(31, 70, 120, 0.06);
  backdrop-filter: blur(18px);
}

.desktop-header__brand,
.desktop-header__tools,
.site-identity,
.quick-entry,
.system-health,
.user-button {
  display: flex;
  align-items: center;
}

.desktop-header__brand {
  min-width: 0;
  gap: 14px;
}

.brand-logo {
  width: 174px;
  height: 30px;
  object-fit: contain;
  object-position: left center;
}

.brand-divider {
  width: 1px;
  height: 36px;
  background: #e1e8f1;
}

.site-identity {
  min-width: 0;
  gap: 8px;
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid #dce5ef;
  border-radius: 9px;
  background: #fbfdff;
  color: #40536c;
  font-size: 13px;
  font-weight: 600;

  .el-icon {
    color: #1268f3;
    font-size: 18px;
  }

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.quick-entry {
  height: 44px;
  gap: 9px;
  padding: 0 4px 0 13px;
  border: 1px solid #d8e2ef;
  border-radius: 9px;
  background: #fbfdff;
  color: #73859c;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;

  &:focus-within {
    border-color: #8db7f8;
    box-shadow: 0 0 0 3px rgba(18, 104, 243, 0.1);
  }

  input {
    min-width: 0;
    flex: 1;
    border: 0;
    outline: 0;
    background: transparent;
    color: #21324b;
    font: inherit;
    font-size: 14px;

    &::placeholder {
      color: #93a2b5;
    }
  }

  button {
    height: 34px;
    padding: 0 13px;
    border: 0;
    border-radius: 7px;
    background: #edf4ff;
    color: #1268f3;
    font: inherit;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
  }
}

.desktop-header__tools {
  justify-content: flex-end;
  gap: 18px;
}

.system-health {
  gap: 9px;
  padding-right: 16px;
  border-right: 1px solid #e1e8f1;

  > span:last-child {
    small,
    strong {
      display: block;
      line-height: 1.2;
    }

    small {
      color: #8696a9;
      font-size: 11px;
    }

    strong {
      margin-top: 3px;
      color: #19845f;
      font-size: 13px;
    }
  }
}

.system-health__icon {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #e8f8f2;
  color: #18a879;
  font-size: 18px;
}

.desktop-clock {
  color: #1c2c45;
  font-size: 15px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.user-button {
  min-height: 44px;
  gap: 8px;
  padding: 3px 4px 3px 7px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: #60748b;
  cursor: pointer;

  &:hover {
    background: #f1f6fc;
  }

  &:focus-visible {
    outline: 3px solid rgba(18, 104, 243, 0.2);
  }
}

.user-avatar {
  width: 34px;
  height: 34px;
  border: 2px solid #e7edf5;
  border-radius: 50%;
  object-fit: cover;
}

.user-avatar--fallback {
  display: grid;
  place-items: center;
  background: #edf4ff;
  color: #1268f3;
  font-size: 13px;
  font-weight: 700;
}

.user-copy {
  max-width: 90px;
  text-align: left;

  strong,
  small {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: #263852;
    font-size: 13px;
  }

  small {
    margin-top: 2px;
    color: #91a0b2;
    font-size: 11px;
  }
}

@media (max-width: 1220px) {
  .desktop-header {
    grid-template-columns: minmax(300px, 1fr) minmax(260px, 390px) auto;
  }

  .system-health,
  .user-copy {
    display: none;
  }
}

@media (max-width: 820px) {
  .desktop-header {
    grid-template-columns: 1fr auto;
  }

  .quick-entry,
  .site-identity,
  .brand-divider {
    display: none;
  }
}
</style>
