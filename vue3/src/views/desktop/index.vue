<template>
  <div class="desktop">
    <a class="skip-link" href="#desktop-main">跳到主要工作区</a>
    <DesktopHeader @open="openAppByKey" />

    <div class="desktop__frame">
      <aside class="desktop__rail">
        <AppDock :active-key="isSystemManagement ? 'system' : (activeApp || 'home')" @open="onOpenApp" />
      </aside>

      <main id="desktop-main" class="desktop__workspace" tabindex="-1">
        <SystemManager v-if="isSystemManagement" />
        <template v-else>
          <OperationsWorkspace v-if="!activeApp" :spatial-space="operationalSpace" :spatial-source="operationalSource"
            :selected-layer-id="selectedLayerId" @select-layer="selectedLayerId = $event" @open="openAppByKey" />
          <DriverManager v-if="activeApp === 'driver'" @close="closeApp" @minimize="closeApp" />
          <Registry v-if="activeApp === 'registry'" @close="closeApp" @minimize="closeApp" />
          <DeviceCenter v-if="activeApp === 'device'" @close="closeApp" @minimize="closeApp" />
          <EventHub v-if="activeApp === 'event-hub'" :spatial-space="operationalSpace" :spatial-source="operationalSource"
            :selected-layer-id="selectedLayerId" @select-layer="selectedLayerId = $event" @close="closeApp" @minimize="closeApp" />
          <ResourceManager v-if="activeApp === 'resource'" @close="closeApp" @minimize="closeApp" />
          <DataCenter v-if="activeApp === 'data-center'" @close="closeApp" @minimize="closeApp" />
          <SpaceEditor v-if="activeApp === 'space-object'" :key="spaceProjectId"
            :repository="spaceConnection.repository" :publisher="spaceConnection.publish"
            @close="closeApp" @minimize="closeApp" />
        </template>
      </main>
    </div>
  </div>
</template>

<script setup name="Desktop">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { spaceApi } from '@/api/build/space'
import DesktopHeader from './components/DesktopHeader.vue'
import OperationsWorkspace from './components/OperationsWorkspace.vue'
import AppDock from './components/AppDock.vue'
import DriverManager from './apps/DriverManager/index.vue'
import Registry from './apps/Registry/index.vue'
import DeviceCenter from './apps/DeviceCenter/index.vue'
import EventHub from './apps/EventHub/index.vue'
import ResourceManager from './apps/ResourceManager/index.vue'
import DataCenter from './apps/DataCenter/index.vue'
import SpaceEditor from './apps/SpaceEditor/index.vue'
import SystemManager from './apps/SystemManager/index.vue'
import { createRemoteSpaceConnection } from './apps/SpaceEditor/remote-repository'
import { readOperationalSpace } from './operational-space'
import './styles/control-panel.scss'

const route = useRoute()
const router = useRouter()
const isSystemManagement = computed(() => Boolean(route.meta.systemManagement))
const spaceProjectId = computed(() => {
  const fromRoute = route.query.projectId
  const fromEnvironment = import.meta.env.VITE_SPACE_PROJECT_ID
  return (typeof fromRoute === 'string' && fromRoute.trim()) ||
    (typeof fromEnvironment === 'string' && fromEnvironment.trim()) || 'default'
})
const spaceConnection = computed(() => createRemoteSpaceConnection(spaceProjectId.value, spaceApi))
const activeApp = ref('')
const operationalSpace = ref(null)
const operationalSource = ref('loading')
const selectedLayerId = ref('')
let spaceRequest = 0
const APP_WINDOWS = ['driver', 'registry', 'device', 'event-hub', 'resource', 'data-center', 'space-object']

async function loadOperationalSpace() {
  const request = ++spaceRequest
  operationalSource.value = 'loading'
  try {
    const result = await readOperationalSpace(spaceProjectId.value, spaceApi)
    if (request !== spaceRequest) return
    operationalSpace.value = result.space
    operationalSource.value = result.source
    if (!operationalSpace.value?.layers.some(layer => layer.id === selectedLayerId.value)) selectedLayerId.value = ''
  } catch {
    if (request !== spaceRequest) return
    operationalSpace.value = null
    operationalSource.value = 'error'
  }
}

function onOpenApp(app) {
  if (app.key === 'system') {
    activeApp.value = ''
    router.push('/system')
    return
  }
  if (isSystemManagement.value) {
    router.push({ path: '/index', query: app.key === 'home' ? {} : { app: app.key } })
    return
  }
  if (app.key === 'home') {
    closeApp()
    return
  }
  if (APP_WINDOWS.includes(app.key)) {
    const leavingEditor = activeApp.value === 'space-object' && app.key !== 'space-object'
    activeApp.value = app.key
    if (leavingEditor || app.key === 'event-hub') loadOperationalSpace()
    return
  }
  ElMessage({ message: `${app.name}能力建设中，当前未接入空间语义数据`, type: 'info', grouping: true })
}

function openAppByKey(key) {
  const names = {
    device: '设备中心',
    'event-hub': '事件中枢',
    resource: '资源管理器',
    'data-center': '数据中心',
    registry: '注册表',
    driver: '驱动管理',
    'space-object': '空间编辑器',
    system: '系统管理'
  }
  onOpenApp({ key, name: names[key] || '功能模块' })
}

function closeApp() {
  if (activeApp.value === 'space-object') loadOperationalSpace()
  activeApp.value = ''
}

watch(() => route.query.app, key => {
  if (!isSystemManagement.value && APP_WINDOWS.includes(key)) onOpenApp({ key })
}, { immediate: true })
watch(isSystemManagement, isSystem => {
  if (isSystem) activeApp.value = ''
  else if (!operationalSpace.value) loadOperationalSpace()
})
watch(spaceProjectId, () => { selectedLayerId.value = ''; if (!isSystemManagement.value) loadOperationalSpace() }, { immediate: true })
onMounted(() => { document.body.classList.add('desktop-control-surface'); window.addEventListener('focus', loadOperationalSpace) })
onBeforeUnmount(() => { spaceRequest++; document.body.classList.remove('desktop-control-surface'); window.removeEventListener('focus', loadOperationalSpace) })
</script>

<style scoped lang="scss">
.desktop {
  --header-height: 70px;
  --rail-width: 176px;
  width: 100%;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background:
    radial-gradient(circle at 72% 2%, rgba(203, 224, 255, 0.7), transparent 28%),
    linear-gradient(145deg, #f7faff 0%, #eef4fb 100%);
  color: #13213a;
}

.desktop__frame {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: var(--rail-width) minmax(0, 1fr);
}

.desktop__rail {
  min-height: 0;
  border-right: 1px solid #dfe7f2;
  background: rgba(255, 255, 255, 0.88);
}

.desktop__workspace {
  position: relative;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  outline: none;
}

.skip-link {
  position: fixed;
  z-index: 10000;
  top: 8px;
  left: 8px;
  padding: 9px 12px;
  border-radius: 8px;
  background: #1268f3;
  color: #fff;
  transform: translateY(-140%);

  &:focus {
    transform: translateY(0);
  }
}

@media (max-width: 900px) {
  .desktop {
    --rail-width: 150px;
  }
}
</style>
