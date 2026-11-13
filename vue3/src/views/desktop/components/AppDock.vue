<template>
  <nav class="app-dock" aria-label="主要功能">
    <ul class="app-dock__list">
      <li v-for="app in apps" :key="app.key" class="dock-item" :class="{ 'is-active': activeKey === app.key }">
        <button
          type="button"
          :aria-label="app.name"
          :aria-current="activeKey === app.key ? 'page' : undefined"
          @click="handleClick(app)"
        >
          <span class="dock-item__icon" aria-hidden="true">
            <el-icon><component :is="iconMap[app.key]" /></el-icon>
          </span>
          <span class="dock-item__label">{{ shortLabels[app.key] || app.name }}</span>
        </button>
      </li>
    </ul>
  </nav>
</template>

<script setup>
import { computed, markRaw } from 'vue'
import { Bell, Box, Coin, Connection, Cpu, DataBoard, House, MapLocation, Setting } from '@element-plus/icons-vue'
import dockApps from '../config/dockApps'
import auth from '@/plugins/auth'
import { visibleSystemMenus } from '../config/systemMenus'

defineProps({
  activeKey: { type: String, default: 'home' },
  minimizedKey: { type: String, default: '' }
})

const apps = computed(() => dockApps.filter(app => app.key !== 'system' || visibleSystemMenus(permission => auth.hasPermi(permission)).length))
const emit = defineEmits(['open'])
const iconMap = {
  home: markRaw(House),
  registry: markRaw(Connection),
  resource: markRaw(DataBoard),
  'data-center': markRaw(Coin),
  device: markRaw(Cpu),
  'event-hub': markRaw(Bell),
  'space-object': markRaw(MapLocation),
  driver: markRaw(Box),
  system: markRaw(Setting)
}
const shortLabels = {
  home: '态势',
  registry: '注册',
  resource: '资源',
  'data-center': '数据',
  device: '设备',
  'event-hub': '事件',
  'space-object': '空间',
  driver: '驱动',
  system: '系统管理'
}

function handleClick(app) {
  emit('open', app)
}
</script>

<style scoped lang="scss">
.app-dock {
  width: 100%;
  height: 100%;
  padding: 12px 9px;
  overflow: auto;
}

.app-dock__list {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.dock-item {
  position: relative;

  &:nth-child(7) {
    margin-top: auto;
  }

  button {
    position: relative;
    width: 100%;
    min-height: 58px;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
    gap: 10px;
    padding: 8px 13px;
    border: 0;
    border-radius: 12px;
    background: transparent;
    color: #60748f;
    font: inherit;
    cursor: pointer;
    transition: background 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;

    &:hover {
      background: #f0f6ff;
      color: #1268f3;
    }

    &:focus-visible {
      outline: 3px solid rgba(18, 104, 243, 0.2);
      outline-offset: 1px;
    }
  }

  &.is-active button {
    background: linear-gradient(145deg, #edf5ff 0%, #f8fbff 100%);
    color: #1268f3;
    box-shadow: 0 7px 20px rgba(18, 104, 243, 0.09);

    &::before {
      position: absolute;
      left: 0;
      top: 16px;
      bottom: 16px;
      width: 3px;
      border-radius: 0 999px 999px 0;
      background: #1268f3;
      content: '';
    }
  }
}

.dock-item__icon {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  display: grid;
  place-items: center;
  font-size: 25px;
}

.dock-item__label {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.35;
  white-space: nowrap;
}

.dock-item__badge {
  margin-left: auto;
  flex: 0 0 auto;
  padding: 2px 4px;
  border-radius: 999px;
  background: #fff6e8;
  color: #b7791f;
  font-size: 10px;
  white-space: nowrap;
}

@media (max-height: 740px) {
  .dock-item button {
    min-height: 48px;
  }
}

@media (max-width: 900px) {
  .app-dock { padding-inline: 6px; }
  .dock-item button { gap: 7px; padding-inline: 10px; }
  .dock-item__label { font-size: 13px; }
  .dock-item__badge { font-size: 9px; }
}

@media (prefers-reduced-motion: reduce) {
  .dock-item button {
    transition: none;
  }
}
</style>
