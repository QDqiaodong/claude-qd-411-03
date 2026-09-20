<template>
  <div>
    <div class="bar">
      <h3>库存（按品种）</h3>
      <el-button type="primary" @click="openOut">出库</el-button>
      <el-button @click="openWarn">设预警线</el-button>
    </div>
    <div class="inv" v-for="i in inv" :key="i.id">
      <div class="row">
        <span class="name">{{ i.variety }}</span>
        <span class="num" :class="{ low: isLow(i) }">{{ i.stockKg }}kg</span>
        <span class="warn" :class="{ low: isLow(i) }">预警线 {{ i.warnLine }}kg</span>
      </div>
      <div class="track">
        <div class="fill" :class="{ low: isLow(i) }" :style="{ width: pct(i) }"></div>
        <div class="mark" :style="{ left: warnPct(i) }"></div>
      </div>
    </div>
    <el-dialog v-model="outVis" title="出库">
      <el-form :model="outForm" label-width="80px">
        <el-form-item label="品种">
          <el-select v-model="outForm.variety">
            <el-option v-for="i in inv" :key="i.id" :label="i.variety" :value="i.variety" />
          </el-select>
        </el-form-item>
        <el-form-item label="重量(kg)"><el-input-number v-model="outForm.kg" :min="0.1" :step="10" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="outVis = false">取消</el-button>
        <el-button type="primary" @click="doOut">确认出库</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="warnVis" title="设预警线">
      <el-form :model="warnForm" label-width="80px">
        <el-form-item label="品种">
          <el-select v-model="warnForm.variety">
            <el-option v-for="i in inv" :key="i.id" :label="i.variety" :value="i.variety" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警线(kg)"><el-input-number v-model="warnForm.warnLine" :min="0" :step="10" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="warnVis = false">取消</el-button>
        <el-button type="primary" @click="doWarn">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import http from '../api'

const inv = ref([])
const outVis = ref(false)
const warnVis = ref(false)
const outForm = ref({})
const warnForm = ref({})

const maxStock = computed(() => Math.max(1, ...inv.value.map(i => i.stockKg || 0)))
function isLow(i) { return (i.stockKg || 0) < (i.warnLine || 0) }
function pct(i) { return Math.min(100, Math.round((i.stockKg || 0) / maxStock.value * 100)) + '%' }
function warnPct(i) { return Math.min(100, Math.round((i.warnLine || 0) / maxStock.value * 100)) + '%' }
async function load() { inv.value = await http.get('/inventory') }
function openOut() { outForm.value = { kg: 10 }; outVis.value = true }
function openWarn() { warnForm.value = { warnLine: 50 }; warnVis.value = true }
async function doOut() {
  await http.post('/inventory/outbound', outForm.value)
  outVis.value = false
  await load()
}
async function doWarn() {
  await http.post('/inventory/warn-line', warnForm.value)
  warnVis.value = false
  await load()
}
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.inv { margin-bottom: 14px; }
.row { display: flex; align-items: center; gap: 14px; margin-bottom: 4px; }
.name { font-weight: 600; width: 100px; }
.num { font-size: 18px; font-weight: 700; color: #00897b; }
.num.low, .warn.low { color: #e53935; }
.warn { color: #9aa79e; font-size: 13px; }
.track { position: relative; height: 14px; background: #eef5ef; border-radius: 7px; overflow: hidden; }
.fill { height: 100%; background: #00c853; }
.fill.low { background: #ef9a9a; }
.mark { position: absolute; top: -2px; width: 2px; height: 18px; background: #e53935; }
</style>
