<template>
  <div>
    <div class="bar">
      <h3>施药记录</h3>
      <el-button type="primary" @click="openCreate">+ 补记施药</el-button>
    </div>
    <el-table :data="sprays" border stripe>
      <el-table-column label="地块" min-width="140">
        <template #default="{ row }">{{ plotName(row.plotId) }}</template>
      </el-table-column>
      <el-table-column prop="pesticide" label="药名" min-width="110" />
      <el-table-column prop="sprayDate" label="施药日" width="110" />
      <el-table-column label="间隔" width="70">
        <template #default="{ row }">{{ row.intervalDays }}天</template>
      </el-table-column>
      <el-table-column prop="safeDate" label="解禁日" width="110" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.status === '作废'" type="info">已作废</el-tag>
          <el-tag v-else-if="row.inInterval" type="warning">间隔中</el-tag>
          <el-tag v-else type="success">已解禁</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="note" label="备注" min-width="130" show-overflow-tooltip />
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button v-if="row.status === '有效'" link type="danger" @click="voidIt(row)">作废</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-alert class="tip" type="info" :closable="false"
      title="间隔中的地块不能新开采摘批次；施药日不晚于某未入仓批次采摘日的药单，会卡住该批次推进，解禁或作废后自动放行。已入仓批次与库存不受影响。" />
    <el-dialog v-model="vis" title="补记施药">
      <el-form :model="form" label-width="100px">
        <el-form-item label="归属地块">
          <el-select v-model="form.plotId" placeholder="选择在用地块">
            <el-option v-for="p in activePlots" :key="p.id" :label="p.code + ' ' + (p.name || '')" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="药名"><el-input v-model="form.pesticide" placeholder="如 氯氰菊酯" /></el-form-item>
        <el-form-item label="施药日期"><el-date-picker v-model="form.sprayDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="安全间隔(天)"><el-input-number v-model="form.intervalDays" :min="0" /></el-form-item>
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
import { ElMessageBox, ElMessage } from 'element-plus'
import http from '../api'

const sprays = ref([])
const plots = ref([])
const vis = ref(false)
const form = ref({})

const activePlots = computed(() => plots.value.filter(p => p.status === '在用'))
function plotName(id) {
  const p = plots.value.find(x => x.id === id)
  return p ? p.code + ' ' + (p.name || '') : '—'
}
async function load() {
  ;[sprays.value, plots.value] = await Promise.all([http.get('/sprays'), http.get('/plots')])
}
function openCreate() { form.value = { intervalDays: 7 }; vis.value = true }
async function save() {
  await http.post('/sprays', form.value)
  vis.value = false
  ElMessage.success('药单已补记，间隔未满的批次已同步卡住')
  await load()
}
async function voidIt(row) {
  try {
    await ElMessageBox.confirm(
      `作废后按该地块剩余有效药单重算间隔，被这张单卡住的未入仓批次将放行。确定作废「${row.pesticide} ${row.sprayDate}」？`,
      '作废药单', { type: 'warning' }
    )
  } catch { return }
  await http.put('/sprays/' + row.id + '/void')
  ElMessage.success('药单已作废')
  await load()
}
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.tip { margin-top: 12px; }
</style>
