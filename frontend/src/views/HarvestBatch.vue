<template>
  <div>
    <div class="bar">
      <h3>采摘批次</h3>
      <el-button type="primary" @click="openCreate">+ 登记批次</el-button>
    </div>
    <div class="batches">
      <div class="batch" v-for="b in batches" :key="b.id">
        <div class="bhead">{{ plotName(b.plotId) }} · {{ b.batchDate }} · {{ b.variety }}</div>
        <el-steps :active="stepIndex(b.status)" align-center finish-status="success">
          <el-step title="待采" />
          <el-step title="采集中" />
          <el-step title="已入仓" />
        </el-steps>
        <el-alert v-if="b.blocked" class="block-tip" type="warning" :closable="false"
          :title="'🧪 药残间隔未满：' + b.blockReason" />
        <div v-if="b.treeIds && b.treeIds.length" class="btrees">
          <span class="btrees-label">果树：</span>
          <el-tag v-for="tid in b.treeIds" :key="tid" size="small" class="btree"
            :type="treeOf(tid)?.status === '已清' ? 'info' : 'success'"
            :closable="b.status !== '已入仓'" @close="dropTree(b, tid)">
            {{ treeCode(tid) }}
          </el-tag>
        </div>
        <div class="bfoot">
          <span>预估 {{ b.estimateKg }}kg / 实际 {{ b.actualKg || '—' }}kg</span>
          <el-button v-if="b.status !== '已入仓'" type="primary" size="small" :disabled="b.blocked"
            :title="b.blocked ? b.blockReason : ''" @click="advance(b)">
            推进到 {{ nextStatus(b.status) }}
          </el-button>
        </div>
        <div v-if="b.status === '已入仓'" class="reconcile">
          <span class="rec-label">入仓后调账：</span>
          <el-select v-model="adj[b.id].variety" filterable allow-create default-first-option
            size="small" placeholder="品种" class="rec-variety">
            <el-option v-for="v in varietyOptions(b.variety)" :key="v" :label="v" :value="v" />
          </el-select>
          <el-input-number v-model="adj[b.id].actualKg" size="small" :min="0.1"
            :max="b.estimateKg" :precision="1" :step="10" />
          <span class="rec-hint">最大不得超过入仓时预估 {{ b.estimateKg }}kg</span>
          <el-button type="warning" size="small" :loading="adj[b.id].saving"
            @click="saveReconcile(b)">保存调账（同步库存）</el-button>
        </div>
      </div>
    </div>
    <el-dialog v-model="vis" title="登记采摘批次">
      <el-form :model="form" label-width="90px">
        <el-form-item label="归属地块">
          <el-select v-model="form.plotId" placeholder="选择在用地块" @change="form.treeIds = []">
            <el-option v-for="p in activePlots" :key="p.id" :label="p.code + ' ' + (p.name || '')" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="采摘日期"><el-date-picker v-model="form.batchDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="品种"><el-input v-model="form.variety" /></el-form-item>
        <el-form-item label="预估产量(kg)"><el-input-number v-model="form.estimateKg" :min="1" /></el-form-item>
        <el-form-item label="采摘果树">
          <el-select v-model="form.treeIds" multiple placeholder="选本地块在产果树（已清不可选）" :disabled="!form.plotId" style="width: 100%">
            <el-option v-for="t in pickableTrees" :key="t.id" :label="t.code + ' ' + (t.variety || '')" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vis = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="storeVis" title="登记实际产量并入仓">
      <el-input-number v-model="actualKg" :min="0" :max="curBatch?.estimateKg || 99999" />
      <template #footer>
        <el-button @click="storeVis = false">取消</el-button>
        <el-button type="primary" @click="doStore">入仓</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api'

const batches = ref([])
const plots = ref([])
const trees = ref([])
const inv = ref([])
const vis = ref(false)
const storeVis = ref(false)
const form = ref({})
const curBatch = ref(null)
const actualKg = ref(0)
// 已入仓批次的调账草稿（品种 + 实际公斤），key 为批次 id；提交时带 version 做并发校验
const adj = reactive({})

const activePlots = computed(() => plots.value.filter(p => p.status === '在用'))
const pickableTrees = computed(() =>
  trees.value.filter(t => t.plotId === form.value.plotId && t.status !== '已清')
)
// 品种下拉：库存页已有的品种 + 各批次现有品种，允许输入新品种
function varietyOptions(current) {
  const set = new Set(inv.value.map(i => i.variety))
  batches.value.forEach(b => b.variety && set.add(b.variety))
  if (current) set.add(current)
  return [...set].sort()
}
function plotName(id) {
  const p = plots.value.find(x => x.id === id)
  return p ? p.code + ' ' + (p.name || '') : '—'
}
function treeOf(id) { return trees.value.find(t => t.id === id) }
function treeCode(id) { const t = treeOf(id); return t ? t.code : '#' + id }
function stepIndex(s) { return s === '待采' ? 1 : s === '采集中' ? 2 : 3 }
function nextStatus(s) { return s === '待采' ? '采集中' : '已入仓' }
function resetAdj(b) {
  adj[b.id] = { variety: b.variety, actualKg: b.actualKg, version: b.version, saving: false }
}
async function load() {
  ;[batches.value, plots.value, trees.value, inv.value] = await Promise.all([
    http.get('/harvest-batches'), http.get('/plots'), http.get('/trees'), http.get('/inventory')
  ])
  batches.value.forEach(resetAdj)
  if (curBatch.value) {
    const fresh = batches.value.find(b => b.id === curBatch.value.id)
    if (fresh) curBatch.value = fresh
  }
}
function openCreate() { form.value = { estimateKg: 100, treeIds: [] }; vis.value = true }
async function save() {
  await http.post('/harvest-batches', form.value)
  vis.value = false
  await load()
}
async function dropTree(b, tid) {
  await http.put('/harvest-batches/' + b.id, { treeIds: b.treeIds.filter(x => x !== tid) })
  await load()
}
function advance(b) {
  if (nextStatus(b.status) === '已入仓') {
    curBatch.value = b
    actualKg.value = b.estimateKg || 0
    storeVis.value = true
  } else {
    http.put('/harvest-batches/' + b.id, { status: nextStatus(b.status) }).then(load)
  }
}
async function doStore() {
  // 实际超预估会被后端拦下：await 抛错时弹窗保留，库存一行都不动，让用户改小再提交
  await http.put('/harvest-batches/' + curBatch.value.id, {
    status: '已入仓', actualKg: actualKg.value
  })
  storeVis.value = false
  await load()
}
async function saveReconcile(b) {
  const draft = adj[b.id]
  if (!draft.variety || !draft.variety.trim()) { ElMessage.error('品种必填'); return }
  if (draft.actualKg == null || draft.actualKg <= 0) { ElMessage.error('实际公斤必须大于 0'); return }
  if (draft.actualKg > b.estimateKg) {
    ElMessage.error('实际公斤不得超过入仓时预估 ' + b.estimateKg + 'kg，库存不动')
    return
  }
  if (draft.variety === b.variety && Number(draft.actualKg) === Number(b.actualKg)) {
    ElMessage.info('品种和实际公斤都没改，库存无需调整')
    return
  }
  draft.saving = true
  try {
    // version 用草稿里的：后到的一笔若版本已旧，后端要求按最新账重报，load 后界面就是最新数据
    await http.put('/harvest-batches/' + b.id, {
      variety: draft.variety, actualKg: draft.actualKg, version: draft.version
    })
    ElMessage.success('库存已按最新登记调账')
  } catch (e) {
    // 并发落败等错误：刷新出最新账，草稿回到最新，提示用户在最新数据上再改
  } finally {
    draft.saving = false
    await load()
  }
}
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.batches { display: flex; flex-direction: column; gap: 14px; }
.batch { border: 1px solid #e3f3e6; border-radius: 12px; padding: 14px 18px; background: #fff; }
.bhead { font-weight: 600; margin-bottom: 8px; }
.block-tip { margin: 8px 0; }
.btrees { margin: 8px 0; display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.btrees-label { color: #6a7a6e; font-size: 13px; }
.bfoot { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; color: #6a7a6e; }
.reconcile { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-top: 10px; padding-top: 10px; border-top: 1px dashed #d9eadc; }
.rec-label { color: #b0791f; font-size: 13px; font-weight: 600; }
.rec-variety { width: 150px; }
.rec-hint { color: #9aa79e; font-size: 12px; }
</style>
