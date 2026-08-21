<template>
  <section class="model-governance-page">
    <div class="page-toolbar">
      <div><h2>模型治理</h2><p>通过版本快照、模拟、审批和发布控制组合评分规则。</p></div>
      <div class="toolbar-actions"><el-button :icon="Refresh" @click="load">刷新</el-button><el-button type="primary" :icon="Plus" @click="visible = true">创建模型草稿</el-button></div>
    </div>

    <section class="published-strip">
      <div><span>当前发布版本</span><strong>{{ published.version_code || '-' }}</strong><small>{{ published.version_name || '正在初始化基线版本' }}</small></div>
      <div><span>版本状态</span><el-tag type="success" effect="plain">{{ published.status || '-' }}</el-tag><small>发布于 {{ formatDate(published.published_at) }}</small></div>
      <div><span>规则数量</span><strong>{{ number(published.rule_count) }}</strong><small>动态组合评分规则</small></div>
      <div><span>最近压力测试</span><strong>{{ latestStress.scenario_name || '-' }}</strong><small>{{ formatDate(latestStress.created_at) }}</small></div>
    </section>

    <section class="governance-gates">
      <article v-for="gate in governanceGates" :key="gate.title" :class="{ pass: gate.pass }">
        <el-tag size="small" :type="gate.pass ? 'success' : 'warning'" effect="plain">{{ gate.pass ? '已满足' : '待完成' }}</el-tag>
        <strong>{{ gate.title }}</strong>
        <span>{{ gate.description }}</span>
      </article>
    </section>

    <section class="panel version-panel">
      <div class="panel-title"><div><h3>模型版本</h3><span>草稿必须完成模拟后方可提交审批</span></div></div>
      <el-table v-loading="loading" :data="data.versions || []" border max-height="350" @row-click="openVersion">
        <el-table-column prop="version_code" label="版本号" min-width="150" />
        <el-table-column prop="version_name" label="版本名称" min-width="190" show-overflow-tooltip />
        <el-table-column label="状态" width="112"><template #default="{ row }"><el-tag size="small" :type="statusType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
        <el-table-column prop="rule_count" label="规则" width="80" align="right" />
        <el-table-column prop="created_by" label="创建人" width="110" />
        <el-table-column label="模拟时间" min-width="145"><template #default="{ row }">{{ formatDate(row.simulated_at) }}</template></el-table-column>
        <el-table-column label="操作" width="300" fixed="right"><template #default="{ row }"><el-button link type="primary" @click.stop="openVersion(row)">详情</el-button><el-button v-if="row.status === 'DRAFT'" link type="primary" @click.stop="simulate(row)">模拟</el-button><el-button v-if="row.status === 'DRAFT'" link type="warning" @click.stop="workflow(row, 'submit')">提交</el-button><el-button v-if="row.status === 'IN_REVIEW'" link type="success" @click.stop="workflow(row, 'approve')">审批</el-button><el-button v-if="row.status === 'APPROVED'" link type="danger" @click.stop="workflow(row, 'publish')">发布</el-button><el-button v-if="row.status === 'RETIRED'" link type="warning" @click.stop="workflow(row, 'rollback')">回滚</el-button></template></el-table-column>
      </el-table>
    </section>

    <section v-if="selected" class="selected-grid">
      <article class="panel rule-panel">
        <div class="panel-title"><div><h3>{{ selected.version_code }} 规则快照</h3><span>{{ selected.rules?.length || 0 }} 条</span></div><el-tag :type="statusType(selected.status)" effect="plain">{{ statusLabel(selected.status) }}</el-tag></div>
        <el-table :data="selected.rules || []" border max-height="360" size="small">
          <el-table-column prop="rule_code" label="规则" min-width="135" show-overflow-tooltip />
          <el-table-column prop="metric_key" label="指标" min-width="120" />
          <el-table-column prop="operator_type" label="条件" width="75" />
          <el-table-column prop="threshold_value" label="阈值" width="96" align="right" />
          <el-table-column prop="effect_type" label="动作" width="75" />
          <el-table-column prop="score_value" label="分值" width="75" align="right" />
          <el-table-column label="启用" width="70"><template #default="{ row }"><el-tag size="small" :type="Number(row.enabled) === 1 ? 'success' : 'info'">{{ Number(row.enabled) === 1 ? '是' : '否' }}</el-tag></template></el-table-column>
          <el-table-column v-if="selected.status === 'DRAFT'" label="操作" width="70"><template #default="{ row }"><el-button link type="primary" @click="editRule(row)">编辑</el-button></template></el-table-column>
        </el-table>
      </article>
      <article class="panel simulation-panel">
        <div class="panel-title"><div><h3>模拟影响</h3><span>{{ formatDate(selected.simulated_at) }}</span></div></div>
        <template v-if="simulation">
          <div class="simulation-metrics"><div><span>受影响客户</span><strong>{{ number(simulation.changed_customer_count) }}</strong></div><div><span>高风险变化</span><strong :class="Number(simulation.high_risk_delta) > 0 ? 'danger' : 'success'">{{ signed(simulation.high_risk_delta) }}</strong></div><div><span>平均分变化</span><strong>{{ signed(simulation.average_score_delta) }}</strong></div></div>
          <el-table :data="simulation.impact_samples || []" size="small" max-height="250"><el-table-column prop="customer_no" label="客户" min-width="130" /><el-table-column prop="current_score" label="当前" width="62" align="right" /><el-table-column prop="simulated_score" label="模拟" width="62" align="right" /><el-table-column prop="score_delta" label="变化" width="62" align="right" /></el-table>
        </template>
        <el-empty v-else description="请先运行模拟" :image-size="64" />
      </article>
    </section>

    <section class="panel stress-panel">
      <div class="panel-title"><div><h3>压力测试</h3><span>基于已发布模型模拟 PD、押品和额度占用变化</span></div><div class="stress-actions"><el-select v-model="scenario" style="width: 150px"><el-option label="行业下行" value="INDUSTRY_DOWNTURN" /><el-option label="利率上行" value="RATE_SHOCK" /><el-option label="押品折减" value="COLLATERAL_HAIRCUT" /></el-select><el-button type="primary" :icon="TrendCharts" :loading="stressing" @click="stress">运行测试</el-button></div></div>
      <div v-if="stressResult" class="stress-summary"><div><span>高风险客户变化</span><strong :class="Number(stressResult.high_risk_delta) > 0 ? 'danger' : 'success'">{{ signed(stressResult.high_risk_delta) }}</strong></div><div><span>当前平均评分</span><strong>{{ decimal(stressResult.current_average_score) }}</strong></div><div><span>压力后平均评分</span><strong>{{ decimal(stressResult.stressed_average_score) }}</strong></div></div>
      <el-table v-if="stressResult" :data="stressResult.impact_samples || []" size="small" border max-height="250"><el-table-column prop="customer_no" label="客户编号" min-width="140" /><el-table-column prop="customer_name" label="客户名称" min-width="150" show-overflow-tooltip /><el-table-column prop="industry_name" label="行业" min-width="120" /><el-table-column prop="current_score" label="当前评分" width="90" align="right" /><el-table-column prop="stressed_score" label="压力评分" width="90" align="right" /><el-table-column prop="score_delta" label="变化" width="76" align="right" /></el-table>
    </section>

    <el-dialog v-model="visible" title="创建模型草稿" width="500px"><el-form :model="form" label-width="90px"><el-form-item label="版本号"><el-input v-model="form.version_code" placeholder="留空自动生成" /></el-form-item><el-form-item label="版本名称"><el-input v-model="form.version_name" placeholder="例如：2026 年三季度评分调整" /></el-form-item><el-form-item label="说明"><el-input v-model="form.comment" type="textarea" :rows="3" /></el-form-item></el-form><template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="creating" @click="createVersion">创建</el-button></template></el-dialog>
    <el-dialog v-model="ruleVisible" title="调整草稿规则" width="530px"><el-form :model="ruleForm" label-width="100px"><el-form-item label="规则"><el-input v-model="ruleForm.rule_code" disabled /></el-form-item><el-form-item label="阈值"><el-input-number v-model="ruleForm.threshold_value" :precision="6" :step="0.01" controls-position="right" /></el-form-item><el-form-item label="评分"><el-input-number v-model="ruleForm.score_value" :min="0" :max="100" controls-position="right" /></el-form-item><el-form-item label="启用"><el-switch v-model="ruleForm.enabled" /></el-form-item></el-form><template #footer><el-button @click="ruleVisible = false">取消</el-button><el-button type="primary" :loading="savingRule" @click="saveRule">保存</el-button></template></el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, TrendCharts } from '@element-plus/icons-vue'
import http from '../api/http'

const loading = ref(false)
const creating = ref(false)
const stressing = ref(false)
const savingRule = ref(false)
const visible = ref(false)
const ruleVisible = ref(false)
const selected = ref(null)
const stressResult = ref(null)
const scenario = ref('INDUSTRY_DOWNTURN')
const data = ref({ versions: [], published_version: {}, stress_test_runs: [] })
const form = reactive({ version_code: '', version_name: '', comment: '' })
const ruleForm = reactive({ rule_code: '', threshold_value: 0, score_value: 0, enabled: true })
const published = computed(() => data.value.published_version || {})
const latestStress = computed(() => (data.value.stress_test_runs || [])[0] || {})
const simulation = computed(() => parseJson(selected.value?.simulation_summary))
const governanceGates = computed(() => {
  const version = selected.value || published.value || {}
  const simulationSummary = parseJson(version.simulation_summary)
  return [
    {
      title: '规则快照',
      pass: Number(version.rule_count || 0) > 0,
      description: `当前版本包含 ${number(version.rule_count)} 条评分规则。`
    },
    {
      title: '模拟验证',
      pass: !!simulationSummary || !!version.simulated_at,
      description: simulationSummary ? `影响客户 ${number(simulationSummary.changed_customer_count)} 个，高风险变化 ${signed(simulationSummary.high_risk_delta)}。` : '提交审批前必须先完成客户组合影响模拟。'
    },
    {
      title: '审批留痕',
      pass: ['APPROVED', 'PUBLISHED', 'RETIRED'].includes(version.status),
      description: `当前状态：${statusLabel(version.status)}。发布前需完成审批。`
    },
    {
      title: '压力测试',
      pass: !!latestStress.value.scenario_name,
      description: latestStress.value.scenario_name ? `最近场景：${latestStress.value.scenario_name}` : '建议发布前至少完成一次压力测试。'
    }
  ]
})

onMounted(load)

async function load() { loading.value = true; try { data.value = await http.get('/api/risks/model-governance') } finally { loading.value = false } }
async function openVersion(row) { selected.value = await http.get(`/api/risks/model-governance/versions/${row.id}`) }
async function createVersion() { creating.value = true; try { selected.value = await http.post('/api/risks/model-governance/versions', { ...form }); visible.value = false; Object.assign(form, { version_code: '', version_name: '', comment: '' }); ElMessage.success('模型草稿已创建'); await load() } finally { creating.value = false } }
async function simulate(row) { const response = await http.post(`/api/risks/model-governance/versions/${row.id}/simulate`); selected.value = response.version; ElMessage.success('模型模拟已完成'); await load() }
async function workflow(row, action) { const title = ({ submit: '提交模型审批', approve: '审批模型版本', publish: '发布模型版本', rollback: '回滚模型版本' })[action]; const result = await ElMessageBox.prompt('请输入操作说明', title, { inputValue: action === 'approve' ? '审批通过' : '', inputPlaceholder: '填写审计说明', inputValidator: (value) => value?.trim() ? true : '请填写操作说明' }); await http.post(`/api/risks/model-governance/versions/${row.id}/${action}`, { comment: result.value.trim() }); ElMessage.success(`${title}已完成`); await load(); await openVersion(row) }
function editRule(row) { Object.assign(ruleForm, { rule_code: row.rule_code, threshold_value: Number(row.threshold_value), score_value: Number(row.score_value), enabled: Number(row.enabled) === 1 }); ruleVisible.value = true }
async function saveRule() { if (!selected.value) return; savingRule.value = true; try { selected.value = await http.put(`/api/risks/model-governance/versions/${selected.value.id}/rules/${ruleForm.rule_code}`, { threshold_value: ruleForm.threshold_value, score_value: ruleForm.score_value, enabled: ruleForm.enabled }); ruleVisible.value = false; ElMessage.success('草稿规则已更新'); await load() } finally { savingRule.value = false } }
async function stress() { stressing.value = true; try { const response = await http.post('/api/risks/stress-tests', { scenario_code: scenario.value }); stressResult.value = response.summary; ElMessage.success('压力测试已完成'); await load() } finally { stressing.value = false } }
function number(value) { return Number(value || 0) }
function decimal(value) { return Number(value || 0).toFixed(2) }
function signed(value) { const number = Number(value || 0); return `${number > 0 ? '+' : ''}${number}` }
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
function parseJson(value) { if (!value) return null; if (typeof value === 'object') return value; try { return JSON.parse(value) } catch { return null } }
function statusType(value) { return ({ DRAFT: 'info', IN_REVIEW: 'warning', APPROVED: 'success', PUBLISHED: 'success', RETIRED: 'info' })[value] || 'info' }
function statusLabel(value) { return ({ DRAFT: '草稿', IN_REVIEW: '审批中', APPROVED: '已审批', PUBLISHED: '已发布', RETIRED: '已退役' })[value] || value }
</script>

<style scoped>
.model-governance-page { display: grid; gap: 16px; }
.page-toolbar, .toolbar-actions, .panel-title, .stress-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
h2, h3, p { margin: 0; }
h2 { color: #1f2937; font-size: 18px; font-weight: 650; }
.page-toolbar p, .panel-title span { color: #64748b; font-size: 13px; margin-top: 5px; }
.published-strip { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); border: 1px solid #dce4ee; border-radius: 8px; overflow: hidden; background: #fff; }
.published-strip > div { display: grid; gap: 5px; min-height: 94px; padding: 14px; border-right: 1px solid #e9eef4; }
.published-strip > div:last-child { border-right: 0; }
.published-strip span, .published-strip small { color: #64748b; font-size: 12px; }
.published-strip strong { color: #1f2937; font-size: 18px; overflow-wrap: anywhere; }
.governance-gates { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
.governance-gates article { display: grid; gap: 7px; padding: 13px; border: 1px solid #fed7aa; border-radius: 8px; background: #fffbeb; }
.governance-gates article.pass { border-color: #bbf7d0; background: #f0fdf4; }
.governance-gates strong { color: #1f2937; font-size: 14px; }
.governance-gates span { color: #64748b; font-size: 12px; line-height: 1.5; }
.panel { padding: 16px; }
.panel-title { margin-bottom: 13px; }
.panel-title h3 { color: #334155; font-size: 15px; }
.selected-grid { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(300px, .8fr); gap: 16px; }
.simulation-metrics, .stress-summary { display: grid; grid-template-columns: repeat(3, 1fr); border: 1px solid #e5ebf1; margin-bottom: 14px; }
.simulation-metrics > div, .stress-summary > div { display: grid; gap: 5px; padding: 11px; border-right: 1px solid #e5ebf1; }
.simulation-metrics > div:last-child, .stress-summary > div:last-child { border-right: 0; }
.simulation-metrics span, .stress-summary span { color: #64748b; font-size: 12px; }
.simulation-metrics strong, .stress-summary strong { color: #1f2937; font-size: 20px; }
.danger { color: #c2413a !important; }
.success { color: #087a65 !important; }
@media (max-width: 1020px) { .published-strip, .governance-gates { grid-template-columns: repeat(2, minmax(0, 1fr)); } .selected-grid { grid-template-columns: 1fr; } }
@media (max-width: 620px) { .page-toolbar, .panel-title { align-items: flex-start; flex-direction: column; } .toolbar-actions, .stress-actions { width: 100%; } .toolbar-actions .el-button, .stress-actions .el-button { flex: 1; } .published-strip, .governance-gates, .simulation-metrics, .stress-summary { grid-template-columns: 1fr; } .published-strip > div, .simulation-metrics > div, .stress-summary > div { border-right: 0; border-bottom: 1px solid #e9eef4; } }
</style>
