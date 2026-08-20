<template>
  <section class="alert-case-page">
    <div class="page-toolbar">
      <div>
        <h2>预警处置中心</h2>
        <p>自动关联风险台账、整改任务、风险事件和关键风险指标，统一跟踪案件闭环。</p>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="Refresh" @click="load">刷新列表</el-button>
        <el-button type="primary" :icon="AlarmClock" :loading="refreshing" @click="refreshCases">同步预警闭环</el-button>
      </div>
    </div>

    <section class="case-summary">
      <div><span>待处置</span><strong>{{ number(summary.open_count) }}</strong></div>
      <div><span>处理中</span><strong>{{ number(summary.in_progress_count) }}</strong></div>
      <div class="danger"><span>已逾期</span><strong>{{ number(summary.overdue_count) }}</strong></div>
      <div><span>今日到期</span><strong>{{ number(summary.due_today_count) }}</strong></div>
      <div><span>已关联闭环</span><strong class="linked">{{ number(summary.linked_count) }}</strong></div>
      <div class="success"><span>已关闭</span><strong>{{ number(summary.resolved_count) }}</strong></div>
    </section>

    <section class="panel case-panel">
      <el-tabs v-model="state" @tab-change="load">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待处置" name="OPEN" />
        <el-tab-pane label="处理中" name="IN_PROGRESS" />
        <el-tab-pane label="已逾期" name="OVERDUE" />
        <el-tab-pane label="已关闭" name="RESOLVED" />
      </el-tabs>
      <el-table v-loading="loading" :data="rows" border max-height="calc(100vh - 330px)">
        <el-table-column prop="priority" label="优先级" width="84"><template #default="{ row }"><el-tag size="small" :type="priorityType(row.priority)" effect="plain">{{ row.priority }}</el-tag></template></el-table-column>
        <el-table-column prop="customer_no" label="客户编号" min-width="145" show-overflow-tooltip />
        <el-table-column prop="customer_name" label="客户名称" min-width="155" show-overflow-tooltip />
        <el-table-column prop="risk_score" label="评分" width="68" align="right" />
        <el-table-column label="案件状态" width="100"><template #default="{ row }"><el-tag size="small" :type="stateType(row.alert_state)" effect="plain">{{ stateLabel(row.alert_state) }}</el-tag></template></el-table-column>
        <el-table-column label="整改任务" min-width="118"><template #default="{ row }"><div v-if="row.treatment_plan_id" class="linked-cell"><el-tag size="small" :type="treatmentType(row.treatment_status)" effect="plain">{{ row.treatment_status }}</el-tag><small>{{ number(row.treatment_progress) }}%</small></div><span v-else class="muted">待同步</span></template></el-table-column>
        <el-table-column label="风险事件" min-width="100"><template #default="{ row }"><el-tag v-if="row.risk_event_id" size="small" :type="eventType(row.event_status)" effect="plain">{{ row.event_status }}</el-tag><span v-else class="muted">待同步</span></template></el-table-column>
        <el-table-column label="客户 KRI" min-width="115"><template #default="{ row }"><div v-if="row.risk_indicator_id" class="linked-cell"><el-tag size="small" :type="indicatorType(row.indicator_status)" effect="plain">{{ row.indicator_status }}</el-tag><small>{{ row.indicator_current_value }}</small></div><span v-else class="muted">待同步</span></template></el-table-column>
        <el-table-column label="SLA 到期" min-width="140"><template #default="{ row }"><span :class="dueClass(row)">{{ formatDate(row.sla_due_at) }}</span></template></el-table-column>
        <el-table-column prop="owner" label="责任人" min-width="100" show-overflow-tooltip />
        <el-table-column label="操作" width="145" fixed="right"><template #default="{ row }"><el-button v-if="['OPEN', 'OVERDUE'].includes(row.alert_state)" link type="primary" @click="start(row)">开始处置</el-button><el-button v-if="['OPEN', 'IN_PROGRESS', 'OVERDUE'].includes(row.alert_state)" link type="success" @click="close(row)">关闭案件</el-button></template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="resize" /></div>
    </section>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AlarmClock, Refresh } from '@element-plus/icons-vue'
import http from '../api/http'

const loading = ref(false)
const refreshing = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const state = ref('')
const summary = ref({})

onMounted(refreshCases)

async function load() {
  loading.value = true
  try {
    const data = await http.get('/api/risks/alert-cases', { params: { state: state.value || undefined, page: page.value, size: size.value } })
    rows.value = data.items || []
    total.value = Number(data.total || 0)
  } finally { loading.value = false }
}

async function refreshCases() {
  refreshing.value = true
  try {
    const result = await http.post('/api/risks/alert-cases/refresh')
    summary.value = result.summary || {}
    ElMessage.success(`已同步 ${result.synchronized_case_count || 0} 个预警案件闭环`)
    await load()
  } finally { refreshing.value = false }
}

async function start(row) {
  const result = await http.post(`/api/risks/alert-cases/${encodeURIComponent(row.customer_no)}/start`)
  summary.value = result.summary || summary.value
  ElMessage.success('案件及关联整改任务已进入处理中')
  await load()
}

async function close(row) {
  const result = await ElMessageBox.prompt('填写风险核验和处置结论后，整改任务将完成、风险事件将进入复盘、KRI 将同步恢复正常。', `关闭案件：${row.customer_no}`, {
    inputType: 'textarea',
    inputPlaceholder: '例如：已完成现金流复核，风险信号已解除。',
    inputValidator: (value) => value?.trim() ? true : '必须填写处置结论'
  })
  const response = await http.post(`/api/risks/alert-cases/${encodeURIComponent(row.customer_no)}/close`, { comment: result.value.trim() })
  summary.value = response.summary || summary.value
  ElMessage.success('案件闭环已完成并同步关联记录')
  await load()
}

function resize() { page.value = 1; load() }
function number(value) { return Number(value || 0) }
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
function priorityType(value) { return ({ P1: 'danger', P2: 'warning', P3: '', P4: 'info' })[value] || 'info' }
function stateType(value) { return ({ OPEN: 'warning', IN_PROGRESS: '', OVERDUE: 'danger', RESOLVED: 'success' })[value] || 'info' }
function stateLabel(value) { return ({ OPEN: '待处置', IN_PROGRESS: '处理中', OVERDUE: '已逾期', RESOLVED: '已关闭' })[value] || value }
function treatmentType(value) { return ({ 已完成: 'success', 进行中: '', 逾期: 'danger', 未开始: 'warning' })[value] || 'info' }
function eventType(value) { return ({ 已复盘: 'success', 处理中: 'warning', 登记: 'info' })[value] || 'info' }
function indicatorType(value) { return ({ 正常: 'success', 预警: 'warning', 超限: 'danger' })[value] || 'info' }
function dueClass(row) { return row.alert_state === 'OVERDUE' ? 'overdue' : '' }
</script>

<style scoped>
.alert-case-page { display: grid; gap: 16px; }
.page-toolbar, .toolbar-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
h2, p { margin: 0; }
h2 { color: #1f2937; font-size: 18px; font-weight: 650; }
.page-toolbar p { color: #64748b; font-size: 13px; margin-top: 5px; }
.case-summary { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); border: 1px solid #dce4ee; border-radius: 8px; overflow: hidden; background: #fff; }
.case-summary > div { display: grid; gap: 5px; min-height: 82px; padding: 13px; border-right: 1px solid #e9eef4; }
.case-summary > div:last-child { border-right: 0; }
.case-summary span { color: #64748b; font-size: 12px; }
.case-summary strong { color: #1f2937; font-size: 22px; }
.case-summary .danger strong, .overdue { color: #c2413a; }
.case-summary .success strong { color: #087a65; }
.case-summary .linked { color: #335d8f; }
.case-panel { padding: 0 16px 14px; }
.linked-cell { display: grid; gap: 3px; justify-items: start; }
.linked-cell small, .muted { color: #64748b; font-size: 12px; }
.pager { display: flex; justify-content: flex-end; padding-top: 14px; }
@media (max-width: 1100px) { .case-summary { grid-template-columns: repeat(3, minmax(0, 1fr)); }.case-summary > div:nth-child(3n) { border-right: 0; }.case-summary > div:nth-child(-n + 3) { border-bottom: 1px solid #e9eef4; } }
@media (max-width: 600px) { .page-toolbar { align-items: flex-start; flex-direction: column; }.toolbar-actions { width: 100%; }.toolbar-actions .el-button { flex: 1; }.case-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); }.case-summary > div { border-right: 1px solid #e9eef4; border-bottom: 1px solid #e9eef4; }.case-summary > div:nth-child(2n) { border-right: 0; }.case-summary > div:nth-last-child(-n + 2) { border-bottom: 0; } }
</style>
