<template>
  <section class="governance-page">
    <div class="page-toolbar">
      <div><h2>数据治理</h2><p>监控信贷域关联完整性、质量快照和风险数据血缘。</p></div>
      <div class="toolbar-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="DocumentChecked" :loading="capturing" @click="capture">生成质量快照</el-button>
      </div>
    </div>

    <section v-loading="loading" class="quality-summary">
      <div class="quality-score"><span>质量评分</span><strong>{{ number(live.quality_score) }}</strong><small>/ 100</small></div>
      <div class="summary-item"><span>检查项</span><strong>{{ number(live.check_total) }}</strong></div>
      <div class="summary-item success"><span>通过</span><strong>{{ number(live.pass_count) }}</strong></div>
      <div class="summary-item warning"><span>关注</span><strong>{{ number(live.warning_count) }}</strong></div>
      <div class="summary-item danger"><span>失败</span><strong>{{ number(live.failed_count) }}</strong></div>
      <div class="summary-item"><span>问题记录</span><strong>{{ number(live.issue_total) }}</strong></div>
    </section>

    <section class="panel quality-panel">
      <div class="panel-title"><div><h3>关联质量检查</h3><span>实时计算</span></div><el-tag :type="qualityTag" effect="plain">{{ qualityLabel }}</el-tag></div>
      <el-table v-loading="loading" :data="live.checks || []" border max-height="380">
        <el-table-column prop="check_name" label="检查项" min-width="200" />
        <el-table-column prop="severity" label="严重度" width="100"><template #default="{ row }"><el-tag size="small" :type="severityType(row.severity)" effect="plain">{{ row.severity }}</el-tag></template></el-table-column>
        <el-table-column prop="record_total" label="覆盖记录" width="105" align="right" />
        <el-table-column prop="issue_count" label="问题记录" width="105" align="right" />
        <el-table-column label="问题比例" width="120" align="right"><template #default="{ row }">{{ percent(row.issue_rate) }}</template></el-table-column>
        <el-table-column label="状态" width="105"><template #default="{ row }"><el-tag size="small" :type="statusType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
      </el-table>
    </section>

    <section class="panel remediation-panel">
      <div class="panel-title"><div><h3>数据整改建议</h3><span>按严重度和问题记录数排序，优先处理阻断业务口径的问题</span></div></div>
      <div class="remediation-list">
        <article v-for="item in remediationItems" :key="item.check_code || item.check_name" :class="statusType(item.status)">
          <div>
            <strong>{{ item.check_name }}</strong>
            <span>{{ item.suggestion }}</span>
          </div>
          <el-tag :type="statusType(item.status)" effect="plain">{{ number(item.issue_count) }} 条问题</el-tag>
        </article>
        <el-empty v-if="!remediationItems.length" description="暂无需要整改的数据质量问题" :image-size="64" />
      </div>
    </section>

    <section class="panel lineage-panel">
      <div class="panel-title"><div><h3>数据血缘目录</h3><span>{{ (data.lineage || []).length }} 个风险数据实体</span></div><span>最近快照：{{ formatDate(data.latest_snapshot?.captured_at) }}</span></div>
      <el-table v-loading="loading" :data="data.lineage || []" border max-height="360">
        <el-table-column prop="domain_name" label="数据域" min-width="130" />
        <el-table-column prop="entity_name" label="实体" min-width="130" />
        <el-table-column prop="source_table" label="来源表" min-width="205" show-overflow-tooltip />
        <el-table-column prop="business_key" label="业务主键" min-width="150" show-overflow-tooltip />
        <el-table-column prop="key_fields" label="关键字段" min-width="260" show-overflow-tooltip />
        <el-table-column prop="freshness_sla_hours" label="时效 SLA" width="105" align="right"><template #default="{ row }">{{ row.freshness_sla_hours }} 小时</template></el-table-column>
        <el-table-column prop="sensitivity_level" label="敏感级别" width="112"><template #default="{ row }"><el-tag size="small" type="info" effect="plain">{{ row.sensitivity_level }}</el-tag></template></el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentChecked, Refresh } from '@element-plus/icons-vue'
import http from '../api/http'

const loading = ref(false)
const capturing = ref(false)
const data = ref({ live: {}, lineage: [] })
const live = computed(() => data.value.live || {})
const qualityTag = computed(() => Number(live.value.quality_score || 0) >= 95 ? 'success' : Number(live.value.quality_score || 0) >= 85 ? 'warning' : 'danger')
const qualityLabel = computed(() => Number(live.value.quality_score || 0) >= 95 ? '质量良好' : Number(live.value.quality_score || 0) >= 85 ? '需要关注' : '需要整改')
const remediationItems = computed(() => (live.value.checks || [])
  .filter((item) => Number(item.issue_count || 0) > 0)
  .map((item) => ({ ...item, suggestion: remediationSuggestion(item) }))
  .sort((a, b) => severityRank(b.severity) - severityRank(a.severity) || Number(b.issue_count || 0) - Number(a.issue_count || 0)))

onMounted(load)
async function load() {
  loading.value = true
  try { data.value = await http.get('/api/risks/data-governance') } finally { loading.value = false }
}
async function capture() {
  capturing.value = true
  try { await http.post('/api/risks/data-governance/snapshot'); ElMessage.success('数据质量快照已生成'); await load() } finally { capturing.value = false }
}
function number(value) { return Number(value || 0) }
function percent(value) { return `${(Number(value || 0) * 100).toFixed(2)}%` }
function severityType(value) { return ({ CRITICAL: 'danger', HIGH: 'warning', MEDIUM: 'info' })[value] || 'info' }
function statusType(value) { return ({ PASS: 'success', WARNING: 'warning', FAILED: 'danger' })[value] || 'info' }
function statusLabel(value) { return ({ PASS: '通过', WARNING: '关注', FAILED: '失败' })[value] || value }
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '尚未生成' }
function severityRank(value) { return ({ CRITICAL: 3, HIGH: 2, MEDIUM: 1 })[value] || 0 }
function remediationSuggestion(item) {
  const name = `${item.check_name || ''}${item.check_code || ''}`
  if (name.includes('客户') || name.toLowerCase().includes('customer')) return '优先核对客户主数据、客户编号和客户状态，修复后再刷新组合评分。'
  if (name.includes('合同') || name.includes('债项') || name.toLowerCase().includes('contract')) return '检查合同、支用、债项之间的业务主键和日期顺序，避免风险敞口无法下钻。'
  if (name.includes('PD') || name.includes('LGD') || name.includes('EAD') || name.includes('参数')) return '复核模型参数范围和空值来源，必要时回补默认值并记录数据血缘。'
  if (name.includes('评级') || name.toLowerCase().includes('rating')) return '检查评级有效期和最新评级结果，过期评级应触发客户经理补录或模型重算。'
  return '按源表、业务主键、数据日期定位异常记录，修复后生成新的数据质量快照。'
}
</script>

<style scoped>
.governance-page { display: grid; gap: 16px; }.page-toolbar, .toolbar-actions, .panel-title { display: flex; align-items: center; justify-content: space-between; gap: 12px; }h2, h3, p { margin: 0; }h2 { color: #1f2937; font-size: 18px; font-weight: 650; }.page-toolbar p, .panel-title span { color: #64748b; font-size: 13px; margin-top: 5px; }.quality-summary { display: grid; grid-template-columns: 1.35fr repeat(5, minmax(0, 1fr)); border: 1px solid #dce4ee; border-radius: 8px; overflow: hidden; background: #fff; }.quality-score, .summary-item { display: grid; align-content: center; gap: 4px; min-height: 100px; padding: 15px; border-right: 1px solid #e9eef4; }.summary-item:last-child { border-right: 0; }.quality-score { grid-template-columns: auto auto; align-items: end; background: #f3f8fc; }.quality-score span { grid-column: 1 / -1; color: #64748b; font-size: 12px; }.quality-score strong { color: #1f5e93; font-size: 34px; }.quality-score small { padding-bottom: 5px; color: #64748b; }.summary-item span { color: #64748b; font-size: 12px; }.summary-item strong { color: #1f2937; font-size: 24px; }.summary-item.success strong { color: #087a65; }.summary-item.warning strong { color: #b45309; }.summary-item.danger strong { color: #c2413a; }.panel { padding: 16px; }.panel-title { margin-bottom: 13px; }.panel-title h3 { color: #334155; font-size: 15px; }.remediation-list { display: grid; gap: 10px; }.remediation-list article { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 12px 14px; border: 1px solid #e5ebf1; border-left: 4px solid #94a3b8; border-radius: 8px; background: #fff; }.remediation-list article.danger { border-left-color: #dc2626; background: #fff7f7; }.remediation-list article.warning { border-left-color: #d97706; background: #fffbeb; }.remediation-list strong { display: block; color: #1f2937; font-size: 14px; }.remediation-list span { display: block; margin-top: 4px; color: #64748b; font-size: 13px; line-height: 1.5; }@media (max-width: 980px) { .quality-summary { grid-template-columns: repeat(3, minmax(0, 1fr)); }.quality-score { grid-column: span 3; }.summary-item:nth-child(4) { border-left: 1px solid #e9eef4; } }@media (max-width: 620px) { .page-toolbar { align-items: flex-start; flex-direction: column; }.toolbar-actions { width: 100%; }.toolbar-actions .el-button { flex: 1; }.quality-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); }.quality-score { grid-column: span 2; }.summary-item { border-bottom: 1px solid #e9eef4; }.remediation-list article { align-items: flex-start; flex-direction: column; } }
</style>
