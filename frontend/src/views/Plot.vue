<template>
  <div>
    <div class="bar">
      <h3>地块台账</h3>
      <el-button type="primary" @click="openCreate">+ 新增地块</el-button>
    </div>
    <div class="cards">
      <div class="card" v-for="p in plots" :key="p.id">
        <div class="ring" :style="ringStyle(p)"><span>{{ p.area }}亩</span></div>
        <div class="info">
          <div class="code">{{ p.code }} · {{ p.name }}</div>
          <el-tag :type="p.status === '在用' ? 'success' : 'info'" size="small">{{ p.status }}</el-tag>
          <span class="trees-count">在产 {{ p.productiveTrees ?? 0 }} 株</span>
          <div class="note">{{ p.note || '—' }}</div>
          <el-button link type="primary" @click="openEdit(p)">编辑</el-button>
        </div>
      </div>
    </div>
    <el-dialog v-model="vis" :title="form.id ? '编辑地块' : '新增地块'">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编号"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="面积(亩)"><el-input-number v-model="form.area" :min="0" :step="0.5" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="在用" value="在用" />
            <el-option label="停用" value="停用" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.note" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vis = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import http from '../api'

const plots = ref([])
const vis = ref(false)
const form = ref({})

const maxArea = computed(() => Math.max(1, ...plots.value.map(p => p.area || 0)))
function ringStyle(p) {
  const pct = Math.min(100, Math.round((p.area || 0) / maxArea.value * 100))
  return { background: `conic-gradient(#00c853 ${pct}%, #e3f3e6 ${pct}%)` }
}
async function load() { plots.value = await http.get('/plots') }
function openCreate() { form.value = { status: '在用', area: 1 }; vis.value = true }
function openEdit(p) { form.value = { ...p }; vis.value = true }
async function save() {
  if (form.value.id) await http.put('/plots/' + form.value.id, form.value)
  else await http.post('/plots', form.value)
  vis.value = false
  await load()
}
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.cards { display: flex; flex-wrap: wrap; gap: 16px; }
.card { width: 260px; border: 1px solid #e3f3e6; border-radius: 12px; padding: 16px; display: flex; gap: 14px; align-items: center; background: #fff; }
.ring { width: 76px; height: 76px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 700; color: #2e7d32; flex-shrink: 0; }
.ring span { background: #fff; width: 54px; height: 54px; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.code { font-weight: 600; margin-bottom: 6px; }
.trees-count { margin-left: 8px; font-size: 13px; color: #2e7d32; font-weight: 600; }
.note { color: #8a9a8e; font-size: 13px; margin: 6px 0; }
</style>
