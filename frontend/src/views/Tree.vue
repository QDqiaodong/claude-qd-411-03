<template>
  <div>
    <div class="bar">
      <h3>果树档案</h3>
      <el-select v-model="plotFilter" placeholder="按地块筛选" clearable style="width: 170px">
        <el-option v-for="p in plots" :key="p.id" :label="p.code + ' ' + (p.name || '')" :value="p.id" />
      </el-select>
      <el-button type="primary" @click="openCreate">+ 新增果树</el-button>
    </div>
    <el-table :data="filtered" border stripe>
      <el-table-column prop="code" label="编号" width="110" />
      <el-table-column prop="variety" label="品种" width="120" />
      <el-table-column label="归属地块" width="160">
        <template #default="{ row }">{{ plotName(row.plotId) }}</template>
      </el-table-column>
      <el-table-column prop="plantYear" label="定植年" width="100" />
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <el-tooltip v-if="row.status === '已清'" :content="removalTip(row)" placement="top">
            <el-tag type="info">已清</el-tag>
          </el-tooltip>
          <el-select v-else :model-value="row.status" size="small" @change="(v) => changeStatus(row, v)">
            <el-option label="正常" value="正常" />
            <el-option label="病害" value="病害" />
            <el-option label="停用" value="停用" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status !== '已清'" link type="danger" @click="openClear(row)">清树</el-button>
          <el-button v-else link type="warning" @click="withdraw(row)">撤回</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="vis" :title="form.id ? '编辑果树' : '新增果树'">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编号"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="归属地块">
          <el-select v-model="form.plotId" placeholder="选择在用地块">
            <el-option v-for="p in activePlots" :key="p.id" :label="p.code + ' ' + (p.name || '')" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="品种"><el-input v-model="form.variety" /></el-form-item>
        <el-form-item label="定植年"><el-input-number v-model="form.plantYear" :min="1980" :max="2099" /></el-form-item>
        <el-form-item label="状态">
          <el-tag v-if="form.status === '已清'" type="info">已清（只能撤回清树单恢复）</el-tag>
          <el-select v-else v-model="form.status">
            <el-option label="正常" value="正常" />
            <el-option label="病害" value="病害" />
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
    <el-dialog v-model="clearVis" :title="'清树 · ' + (cur?.code || '')" width="420px">
      <el-alert type="warning" :closable="false"
        title="清树后该地块在产株数立即减一，且不能再选入新采摘批次；已入仓库存不受影响。" />
      <el-input v-model="clearReason" placeholder="清树原因（如：病弱、枯死）" style="margin-top: 12px" />
      <template #footer>
        <el-button @click="clearVis = false">取消</el-button>
        <el-button type="danger" @click="doClear">确认清树</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api'

const trees = ref([])
const plots = ref([])
const plotFilter = ref(null)
const vis = ref(false)
const form = ref({})
const clearVis = ref(false)
const clearReason = ref('')
const cur = ref(null)

const filtered = computed(() =>
  plotFilter.value ? trees.value.filter(t => t.plotId === plotFilter.value) : trees.value
)
const activePlots = computed(() => plots.value.filter(p => p.status === '在用'))
function plotName(id) {
  const p = plots.value.find(x => x.id === id)
  return p ? p.code + ' ' + (p.name || '') : '—'
}
function removalTip(row) {
  return '清树单 #' + (row.removalId ?? '—') + (row.removalReason ? '：' + row.removalReason : '')
}
async function load() {
  ;[trees.value, plots.value] = await Promise.all([http.get('/trees'), http.get('/plots')])
}
async function changeStatus(row, v) {
  await http.put('/trees/' + row.id, { status: v })
  await load()
}
function openCreate() { form.value = { status: '正常', plantYear: 2023 }; vis.value = true }
function openEdit(t) { form.value = { ...t }; vis.value = true }
async function save() {
  if (form.value.id) await http.put('/trees/' + form.value.id, form.value)
  else await http.post('/trees', form.value)
  vis.value = false
  await load()
}
function openClear(row) { cur.value = row; clearReason.value = ''; clearVis.value = true }
async function doClear() {
  await http.post('/trees/' + cur.value.id + '/removals', { reason: clearReason.value })
  clearVis.value = false
  ElMessage.success('已清树，地块在产株数减一')
  await load()
}
async function withdraw(row) {
  try {
    await ElMessageBox.confirm('撤回后该地块在产株数加回一；已从批次摘掉的树不会自动加回。', '撤回清树单', { type: 'warning' })
  } catch { return }
  await http.post('/trees/' + row.id + '/removals/withdraw')
  ElMessage.success('清树单已撤回')
  await load()
}
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
</style>
