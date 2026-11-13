<template>
  <AppWindow title="系统管理" @close="router.push('/index')">
    <div class="system-manager">
      <aside class="system-manager__nav" aria-label="系统管理菜单">
        <div class="system-manager__brand"><el-icon><Setting /></el-icon><strong>系统管理</strong></div>
        <nav>
          <template v-for="item in menus" :key="item.key">
            <template v-if="item.children">
              <button class="system-manager__group" type="button" :aria-expanded="logsExpanded" @click="logsExpanded = !logsExpanded">
                <el-icon><component :is="icons[item.icon]" /></el-icon>{{ item.title }}
                <el-icon class="system-manager__arrow" :class="{ 'is-expanded': logsExpanded }"><ArrowRight /></el-icon>
              </button>
              <div v-show="logsExpanded" class="system-manager__children">
                <router-link v-for="child in item.children" :key="child.key" :to="child.path"
                  class="system-manager__link" :class="{ 'is-active': activePath === child.path }"
                  :aria-current="activePath === child.path ? 'page' : undefined">
                  <el-icon><component :is="icons[child.icon]" /></el-icon>{{ child.title }}
                </router-link>
              </div>
            </template>
            <router-link v-else :to="item.path" class="system-manager__link"
              :class="{ 'is-active': activePath === item.path }" :aria-current="activePath === item.path ? 'page' : undefined">
              <el-icon><component :is="icons[item.icon]" /></el-icon>{{ item.title }}
            </router-link>
          </template>
        </nav>
      </aside>
      <section class="system-manager__main">
        <header class="system-manager__head">
          <div><span class="system-manager__eyebrow">系统管理<span v-if="isDetail"> / {{ parentTitle }}</span></span><h2>{{ route.meta.title }}</h2></div>
          <el-button v-if="isDetail" icon="ArrowLeft" @click="router.push(activePath)">返回{{ parentTitle }}</el-button>
        </header>
        <div class="system-manager__content">
          <router-view v-slot="{ Component }"><component :is="Component" :key="route.path" /></router-view>
          <el-empty v-if="!menus.length" description="当前账号暂无系统管理权限" />
        </div>
      </section>
    </div>
  </AppWindow>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Setting, User, UserFilled, Menu, OfficeBuilding, Postcard, Collection, EditPen, Grid, ChatDotSquare, Document, DocumentChecked, Tickets } from '@element-plus/icons-vue'
import auth from '@/plugins/auth'
import AppWindow from '../../components/AppWindow.vue'
import { visibleSystemMenus } from '../../config/systemMenus'
import './system-manager.scss'

const route = useRoute()
const router = useRouter()
const icons = { User, UserFilled, Menu, OfficeBuilding, Postcard, Collection, EditPen, Grid, ChatDotSquare, Document, DocumentChecked, Tickets }
const menus = computed(() => visibleSystemMenus(permission => auth.hasPermi(permission)))
const activePath = computed(() => route.meta.activeMenu || route.path)
const logsExpanded = ref(true)
const parentTitle = computed(() => menus.value.flatMap(item => item.children || [item]).find(item => item.path === activePath.value)?.title || '')
const isDetail = computed(() => Boolean(route.meta.activeMenu) && !route.meta.activeMenu.includes('/log/'))

watch(() => route.path, path => {
  if (path === '/system') {
    const first = menus.value[0]
    if (first) router.replace(first.children?.[0].path || first.path)
  }
  if (activePath.value.includes('/log/')) logsExpanded.value = true
}, { immediate: true })
</script>
