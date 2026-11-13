<template>
  <header class="se-toolbar">
    <div class="se-toolbar__brand"><span class="se-toolbar__symbol"><el-icon><Grid /></el-icon></span><div><strong>空间编辑器</strong><small>{{ name }}</small></div></div>
    <div class="se-toolbar__tools" role="toolbar" aria-label="空间编辑工具">
      <button v-for="item in tools" :key="item.key" type="button" :disabled="readonly && !['select', 'marquee', 'pan'].includes(item.key)" :class="{ active: tool === item.key }" :title="item.tip" @click="emit('tool', item.key)">{{ item.label }}</button>
      <span class="se-toolbar__divider"></span>
      <button type="button" :disabled="readonly || !canUndo" title="Ctrl+Z" @click="emit('action', 'undo')">撤销</button>
      <button type="button" :disabled="readonly || !canRedo" title="Ctrl+Shift+Z" @click="emit('action', 'redo')">重做</button>
      <button type="button" :disabled="readonly || !selected" @click="emit('action', 'copy')">复制</button>
      <button type="button" :disabled="readonly || !selected" @click="emit('action', 'delete')">删除</button>
      <span class="se-toolbar__divider"></span>
      <button type="button" :class="{ active: snap }" @click="emit('action', 'snap')">吸附</button>
      <button type="button" :class="{ active: gridVisible }" :disabled="readonly" @click="emit('action', 'grid')">{{ gridVisible ? '隐藏网格' : '显示网格' }}</button>
      <button type="button" :class="{ active: preview }" @click="emit('action', 'preview')">{{ preview ? '返回 2D' : '2.5D 预览' }}</button>
    </div>
    <div class="se-toolbar__actions">
      <button type="button" :disabled="readonly" @click="emit('action', 'map')">上传地图</button>
      <button type="button" :disabled="readonly" @click="emit('action', 'import')">导入</button>
      <button type="button" @click="emit('action', 'export')">导出</button>
      <button type="button" @click="emit('action', 'validate')">校验</button>
      <button type="button" class="primary" :disabled="readonly" @click="emit('action', 'save')">保存</button>
      <button type="button" :disabled="readonly" @click="emit('action', 'publish')">发布</button>
      <button type="button" class="se-toolbar__close" aria-label="关闭空间编辑器" title="关闭空间编辑器" @click="emit('action', 'close')"><el-icon><Close /></el-icon></button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { Close, Grid } from '@element-plus/icons-vue'
import type { Tool } from './types'
defineProps<{ name: string; tool: Tool; snap: boolean; gridVisible: boolean; preview: boolean; selected: number; canUndo: boolean; canRedo: boolean; readonly: boolean }>()
const emit = defineEmits<{ tool: [tool: Tool]; action: [action: string] }>()
const tools: { key: Tool; label: string; tip: string }[] = [
  { key: 'select', label: '选择', tip: '单击选择，拖拽移动；空白处拖拽框选' },
  { key: 'marquee', label: '框选', tip: '框选多个对象' },
  { key: 'pan', label: '平移', tip: '拖动画布；也可按住 Space' },
  { key: 'box', label: '长方体', tip: '单击或拖拽创建' },
  { key: 'cube', label: '正方体', tip: '单击或拖拽创建' },
  { key: 'lshape', label: 'L 形', tip: '单击或拖拽创建' },
  { key: 'cylinder', label: '圆柱体', tip: '单击或拖拽创建' },
  { key: 'polygon', label: '多边形', tip: '逐点创建，双击完成' },
  { key: 'bind', label: '标点绑定', tip: '先在设备列表选择设备，再点击画布' }
]
</script>
