<template>
  <section class="rule-page">
    <div class="page-toolbar">
      <el-input v-model="keyword" clearable :prefix-icon="Search" placeholder="规则编码、名称、指标或标签" @keyup.enter="loadRules" />
      <el-button type="primary" :icon="Plus" @click="openCreate">新增规则</el-button>
      <el-tooltip content="刷新规则"><el-button :icon="Refresh" circle @click="loadRules" /></el-tooltip>
    </div>

    <section class="rule-summary">
      <div><span>启用规则</span><strong>{{ enabledCount }}</strong></div>
      <div><span>底线规则</span><strong>{{ floorCount }}</strong></div>
      <div><span>加分规则</span><strong>{{ addCount }}</strong></div>
    </section>

    <el-table v-loading="loading" :data="rows" border height="calc(100vh - 286px)">
      <el-table-column prop="sort_order" label="顺序" width="72" align="right" />
      <el-table-column prop="rule_code" label="规则编码" min-width="150" show-overflow-tooltip />
      <el-table-column prop="rule_name" label="规则名称" min-width="145" show-overflow-tooltip />
      <el-table-column label="指标与条件" min-width="190">
        <template #default="{ row }">{{ metricLabel(row.metric_key) }} {{ operatorLabel(row.operator_type) }} {{ displayThreshold(row) }}</template>
      </el-table-column>
      <el-table-column label="得分动作" width="130">
        <template #default="{ row }"><el-tag :type="row.effect_type === 'FLOOR' ? 'danger' : 'warning'" effect="plain">{{ effectLabel(row.effect_type) }} {{ row.score_value }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="risk_tag" label="风险标签" min-width="120" show-overflow-tooltip />
      <el-table-column label="状态" width="84">
        <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="132" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="removeRule(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="loadRules" @size-change="changeSize" />
    </div>

    <el-dialog v-model="visible" :title="editingId ? '编辑评分规则' : '新增评分规则'" width="680px" destroy-on-close>
      <el-form :model="form" label-width="102px" class="rule-form">
        <el-form-item label="规则编码" required><el-input v-model="form.rule_code" :disabled="Boolean(editingId)" /></el-form-item>
        <el-form-item label="规则名称" required><el-input v-model="form.rule_name" /></el-form-item>
        <el-form-item label="风险指标" required><el-select v-model="form.metric_key" style="width: 100%"><el-option v-for="item in metricOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="比较条件" required>
          <div class="condition-row"><el-select v-model="form.operator_type"><el-option v-for="item in operatorOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select><el-input-number v-model="form.threshold_value" :precision="6" :step="thresholdStep" controls-position="right" /></div>
        </el-form-item>
        <el-form-item label="得分动作" required>
          <div class="condition-row"><el-select v-model="form.effect_type"><el-option label="加分" value="ADD" /><el-option label="评分下限" value="FLOOR" /></el-select><el-input-number v-model="form.score_value" :min="0" :max="100" controls-position="right" /></div>
        </el-form-item>
        <el-form-item label="风险标签"><el-input v-model="form.risk_tag" /></el-form-item>
        <el-form-item label="原因模板"><el-input v-model="form.reason_template" type="textarea" :rows="2" placeholder="可使用 {value} 引用指标值" /></el-form-item>
        <el-form-item label="优先顺序"><el-input-number v-model="form.sort_order" :min="0" controls-position="right" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveRule">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import http from '../api/http'

const metricOptions = [
  { value: 'blacklist_flag', label: '黑名单标记' },
  { value: 'debt_default_count', label: '有效债项违约数' },
  { value: 'max_overdue_days', label: '最大逾期天数' },
  { value: 'overdue_count', label: '逾期笔数' },
  { value: 'max_pd', label: '最大违约概率 PD' },
  { value: 'rating_numeric', label: '评级数值' },
  { value: 'utilization_rate', label: '额度使用率' },
  { value: 'coverage_rate', label: '押品覆盖率' }
]
const operatorOptions = [
  { value: 'GT', label: '大于' }, { value: 'GTE', label: '大于等于' }, { value: 'LT', label: '小于' }, { value: 'LTE', label: '小于等于' }, { value: 'EQ', label: '等于' }
]
const keyword = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const rows = ref([])
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const editingId = ref(null)
const form = reactive(defaultForm())

const enabledCount = computed(() => rows.value.filter((row) => Number(row.enabled) === 1).length)
const floorCount = computed(() => rows.value.filter((row) => row.effect_type === 'FLOOR').length)
const addCount = computed(() => rows.value.filter((row) => row.effect_type === 'ADD').length)
const thresholdStep = computed(() => ['max_pd', 'utilization_rate', 'coverage_rate'].includes(form.metric_key) ? 0.01 : 1)

watch(keyword, (value) => { if (!value) { page.value = 1; loadRules() } })
onMounted(loadRules)

async function loadRules() {
  loading.value = true
  try {
    const data = await http.get('/api/risks/scoring-rules', { params: { page: page.value, size: size.value, keyword: keyword.value.trim() || undefined } })
    rows.value = data.items || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

function changeSize() { page.value = 1; loadRules() }
function openCreate() { editingId.value = null; Object.assign(form, defaultForm()); visible.value = true }
function openEdit(row) { editingId.value = row.id; Object.assign(form, { ...defaultForm(), ...row, enabled: Number(row.enabled) === 1, threshold_value: Number(row.threshold_value), score_value: Number(row.score_value), sort_order: Number(row.sort_order) }); visible.value = true }

async function saveRule() {
  if (!form.rule_code.trim() || !form.rule_name.trim()) { ElMessage.warning('请填写规则编码和规则名称'); return }
  saving.value = true
  try {
    const payload = { ...form }
    if (editingId.value) await http.put(`/api/risks/scoring-rules/${editingId.value}`, payload)
    else await http.post('/api/risks/scoring-rules', payload)
    ElMessage.success('评分规则已保存')
    visible.value = false
    await loadRules()
  } finally { saving.value = false }
}

async function removeRule(row) {
  await ElMessageBox.confirm(`删除规则“${row.rule_name}”后将不再参与评分。`, '删除评分规则', { type: 'warning' })
  await http.delete(`/api/risks/scoring-rules/${row.id}`)
  ElMessage.success('评分规则已删除')
  await loadRules()
}

function metricLabel(value) { return metricOptions.find((item) => item.value === value)?.label || value }
function operatorLabel(value) { return operatorOptions.find((item) => item.value === value)?.label || value }
function effectLabel(value) { return value === 'FLOOR' ? '下限' : '加分' }
function displayThreshold(row) { const value = Number(row.threshold_value || 0); return ['max_pd', 'utilization_rate', 'coverage_rate'].includes(row.metric_key) ? `${(value * 100).toFixed(2)}%` : value }
function defaultForm() { return { rule_code: '', rule_name: '', metric_key: 'max_overdue_days', operator_type: 'GT', threshold_value: 0, effect_type: 'ADD', score_value: 0, risk_tag: '', reason_template: '', enabled: true, sort_order: 100 } }
</script>

<style scoped>
.rule-page { display: flex; flex-direction: column; gap: 14px; }
.page-toolbar { display: flex; align-items: center; gap: 10px; }
.page-toolbar .el-input { width: min(340px, 100%); }
.rule-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 160px)); gap: 10px; align-items: stretch; }
.rule-summary > div { display: grid; gap: 5px; padding: 12px 14px; border: 1px solid #dce3ec; border-radius: 8px; background: #fff; }
.rule-summary span { color: #64748b; font-size: 12px; }
.rule-summary strong { color: #1f2937; font-size: 22px; }
.pager { display: flex; justify-content: flex-end; }
.condition-row { display: grid; grid-template-columns: minmax(0, 1fr) minmax(150px, 0.8fr); width: 100%; gap: 10px; }
.condition-row .el-input-number { width: 100%; }
@media (max-width: 900px) { .rule-summary { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 640px) { .page-toolbar { flex-wrap: wrap; } .rule-summary { grid-template-columns: 1fr; } .condition-row { grid-template-columns: 1fr; } }
</style>
