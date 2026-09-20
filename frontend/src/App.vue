<template>
  <el-container class="app">
    <el-header class="topbar">
      <div class="brand">🌳 果园管理系统</div>
      <el-menu mode="horizontal" :default-active="active" class="topmenu" router>
        <el-menu-item v-for="m in modules" :key="m.path" :index="m.path">{{ m.label }}</el-menu-item>
      </el-menu>
    </el-header>
    <el-container>
      <el-aside width="172px" class="side">
        <div class="side-title">分类</div>
        <el-menu :default-active="active" router class="sidemenu">
          <el-menu-item v-for="m in modules" :key="m.path" :index="m.path">
            <span class="dot" :style="{ background: m.color }"></span>{{ m.label }}
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const modules = [
  { path: '/plots', label: '地块', color: '#00c853' },
  { path: '/trees', label: '果树', color: '#43a047' },
  { path: '/sprays', label: '施药记录', color: '#ef6c00' },
  { path: '/harvest-batches', label: '采摘批次', color: '#7cb342' },
  { path: '/inventory', label: '库存', color: '#00897b' }
]
const route = useRoute()
const active = computed(() => route.path)
</script>

<style>
html, body, #app { margin: 0; height: 100%; }
.app { height: 100vh; }
.topbar { display: flex; align-items: center; background: #00c853; color: #fff; padding: 0 16px; }
.brand { font-weight: 700; font-size: 18px; margin-right: 24px; white-space: nowrap; }
.topmenu { background: transparent; border-bottom: none; flex: 1; }
.topmenu .el-menu-item { color: #fff !important; }
.topmenu .el-menu-item.is-active { background: rgba(255,255,255,0.2); border-bottom-color: #fff !important; }
.side { background: #f5fbf6; border-right: 1px solid #e3f3e6; }
.side-title { padding: 12px 16px 4px; font-size: 12px; color: #8aa890; }
.sidemenu { border-right: none; background: transparent; }
.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 8px; }
</style>
