<template>
  <section class="lgd-page">
    <div class="page-toolbar">
      <div>
        <h2>LGD 风险管理中心</h2>
        <p>以 EAD 加权口径统一呈现平均 LGD、衰退 LGD、回收率、预期损失及其证据链。</p>
      </div>
      <div class="toolbar-actions">
        <el-tag effect="plain">数据日期 {{ formatDate(summary.latest_data_date) }}</el-tag>
        <el-button :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <section class="metric-strip">
      <div><span>风险敞口 EAD</span><strong>{{ amount(summary.ead_amount_total) }}</strong><small>{{ number(summary.exposure_count) }} 笔 / {{ number(summary.customer_count) }} 户</small></div>
      <div><span>平均 LGD</span><strong>{{ percent(summary.weighted_lgd_avg) }}</strong><small>基于有效债项 EAD 加权</small></div>
      <div><span>衰退 LGD</span><strong class="emphasis">{{ percent(summary.weighted_lgd_downturn) }}</strong><small>较平均值 {{ signedPercent(lgdGap) }}</small></div>
      <div><span>衰退预期损失</span><strong>{{ amount(summary.el_downturn_amount) }}</strong><small>平均 EL {{ amount(summary.el_avg_amount) }}</small></div>
      <div><span>加权产品回收率</span><strong>{{ percent(summary.weighted_recovery_rate) }}</strong><small>低回收率 {{ number(summary.low_recovery_exposure_count) }} 笔</small></div>
      <div><span>高 LGD 敞口</span><strong class="danger">{{ amount(summary.high_lgd_ead_amount) }}</strong><small>{{ number(summary.high_lgd_exposure_count) }} 笔衰退 LGD ≥ 60%</small></div>
    </section>

    <el-tabs v-model="activeTab" class="lgd-tabs" @tab-change="onTabChange">
      <el-tab-pane label="LGD 总览" name="overview">
        <section class="insight-grid">
          <article class="panel distribution-panel">
            <div class="panel-title"><div><h3>产品 LGD 结构</h3><span>平均值、衰退值与回收率均按 EAD 加权</span></div></div>
            <div class="distribution-list">
              <div v-for="item in overview.product_distribution || []" :key="item.product_type" class="distribution-row">
                <div class="distribution-name"><strong>{{ productLabel(item.product_type) }}</strong><span>{{ number(item.exposure_count) }} 笔 · {{ amount(item.ead_amount) }}</span></div>
                <div class="bar-stack">
                  <div class="bar average" :style="{ width: barWidth(item.weighted_lgd_avg) }"><span>平均 {{ percent(item.weighted_lgd_avg) }}</span></div>
                  <div class="bar downturn" :style="{ width: barWidth(item.weighted_lgd_downturn) }"><span>衰退 {{ percent(item.weighted_lgd_downturn) }}</span></div>
                </div>
                <div class="distribution-rate"><span>回收率</span><strong>{{ percent(item.weighted_recovery_rate) }}</strong></div>
              </div>
            </div>
          </article>
          <article class="panel distribution-panel">
            <div class="panel-title"><div><h3>行业敞口与衰退损失</h3><span>识别行业集中度和损失敏感性</span></div></div>
            <el-table :data="overview.industry_distribution || []" size="small" max-height="282">
              <el-table-column prop="industry_name" label="行业" min-width="120" show-overflow-tooltip />
              <el-table-column label="EAD" min-width="105" align="right"><template #default="{ row }">{{ compactAmount(row.ead_amount) }}</template></el-table-column>
              <el-table-column label="衰退 LGD" width="92" align="right"><template #default="{ row }">{{ percent(row.weighted_lgd_downturn) }}</template></el-table-column>
              <el-table-column label="衰退 EL" min-width="110" align="right"><template #default="{ row }">{{ compactAmount(row.el_downturn_amount) }}</template></el-table-column>
            </el-table>
          </article>
        </section>

        <article class="panel exposure-panel">
          <div class="panel-title"><div><h3>高损失率债项</h3><span>按衰退 LGD、EAD 与损失暴露优先排序；点击债项核验抵押品和违约证据。</span></div><el-button text type="primary" @click="activeTab = 'ledger'">查看完整台账</el-button></div>
          <exposure-table :rows="overview.high_lgd_exposures || []" :loading="loading" @detail="openDebt" />
        </article>
      </el-tab-pane>

      <el-tab-pane label="债项 LGD 台账" name="ledger">
        <article class="panel ledger-panel">
          <div class="filter-row">
            <el-input v-model="filters.keyword" clearable :prefix-icon="Search" placeholder="客户、合同或债项编号" @keyup.enter="searchLedger" />
            <el-select v-model="filters.productType" clearable placeholder="产品类型"><el-option label="流动资金贷款" value="WORKING_CAPITAL_LOAN" /><el-option label="固定资产贷款" value="FIXED_ASSET_LOAN" /></el-select>
            <el-select v-model="filters.defaultFlag" clearable placeholder="违约状态"><el-option label="已违约" value="1" /><el-option label="未违约" value="0" /></el-select>
            <el-input-number v-model="filters.minLgdDownturn" :min="0" :max="1" :step="0.05" :precision="2" controls-position="right" placeholder="最低衰退 LGD" />
            <el-button type="primary" :icon="Search" @click="searchLedger">查询</el-button>
            <el-button @click="resetLedger">重置</el-button>
          </div>
          <exposure-table :rows="ledger.rows" :loading="ledger.loading" @detail="openDebt" />
          <div class="pagination"><span>共 {{ number(ledger.total) }} 笔风险敞口</span><el-pagination v-model:current-page="ledger.page" v-model:page-size="ledger.size" layout="prev, pager, next" :total="ledger.total" @current-change="loadLedger" /></div>
        </article>
      </el-tab-pane>

      <el-tab-pane label="参数与计算" name="governance">
        <section class="governance-strip">
          <div><span>当前模型版本</span><strong>{{ governance.published_version?.version_code || '-' }}</strong><small>{{ governance.published_version?.version_name || '未生成参数基线' }}</small></div>
          <div><span>生效日期</span><strong>{{ formatDate(governance.published_version?.effective_date) }}</strong><small>审批人 {{ governance.published_version?.approved_by || '-' }}</small></div>
          <div><span>最近计算</span><strong>{{ formatDate(governance.calculation_runs?.[0]?.created_at) }}</strong><small>{{ governance.calculation_runs?.[0]?.run_by || '-' }} 发起</small></div>
          <div class="run-action"><el-button type="primary" :loading="calculating" @click="captureCalculation">生成计算快照</el-button><small>对当前有效敞口固化审计留痕</small></div>
        </section>
        <section class="insight-grid">
          <article class="panel"><div class="panel-title"><div><h3>分段参数</h3><span>版本化的 LGD 与回收率参数</span></div></div>
            <el-table :data="governance.parameters || []" size="small" max-height="330"><el-table-column prop="segment_name" label="分段" min-width="130" /><el-table-column label="平均 LGD" width="95" align="right"><template #default="{ row }">{{ percent(row.lgd_avg) }}</template></el-table-column><el-table-column label="衰退 LGD" width="95" align="right"><template #default="{ row }">{{ percent(row.lgd_downturn) }}</template></el-table-column><el-table-column label="回收率" width="85" align="right"><template #default="{ row }">{{ percent(row.recovery_rate) }}</template></el-table-column><el-table-column label="抵押品折减" width="105" align="right"><template #default="{ row }">{{ percent(row.collateral_haircut) }}</template></el-table-column></el-table>
          </article>
          <article class="panel"><div class="panel-title"><div><h3>计算批次</h3><span>记录数据日期、模型版本、口径结果和执行人</span></div></div>
            <el-table :data="governance.calculation_runs || []" size="small" max-height="330"><el-table-column label="时间" min-width="125"><template #default="{ row }">{{ formatDate(row.created_at) }}</template></el-table-column><el-table-column prop="version_code" label="版本" min-width="120" /><el-table-column label="衰退 LGD" width="95" align="right"><template #default="{ row }">{{ percent(row.weighted_lgd_downturn) }}</template></el-table-column><el-table-column label="衰退 EL" min-width="105" align="right"><template #default="{ row }">{{ compactAmount(row.el_downturn_amount) }}</template></el-table-column><el-table-column prop="run_by" label="执行人" width="85" /></el-table>
          </article>
        </section>
      </el-tab-pane>

      <el-tab-pane label="压力测试" name="stress">
        <article class="panel stress-panel">
          <div class="panel-title"><div><h3>LGD 情景压力测试</h3><span>情景仅改变测算结果，不覆盖基线 LGD 参数和正式风险敞口。</span></div><div class="stress-actions"><el-select v-model="scenario" style="width: 180px"><el-option label="抵押品价值折减" value="COLLATERAL_HAIRCUT" /><el-option label="回收周期延长" value="RECOVERY_DELAY" /><el-option label="行业回收率下调" value="INDUSTRY_RECOVERY_DOWN" /></el-select><el-button type="primary" :icon="TrendCharts" :loading="stressing" @click="runStress">运行测试</el-button></div></div>
          <div v-if="stressResult" class="stress-summary"><div><span>衰退 LGD</span><strong>{{ percent(stressResult.current_weighted_lgd_downturn) }} → {{ percent(stressResult.stressed_weighted_lgd_downturn) }}</strong></div><div><span>衰退 EL</span><strong>{{ amount(stressResult.current_el_downturn) }} → {{ amount(stressResult.stressed_el_downturn) }}</strong></div><div><span>EL 增量</span><strong class="danger">{{ signedAmount(stressResult.el_delta) }}</strong></div><div><span>高 LGD 债项变化</span><strong>{{ signedNumber(stressResult.high_lgd_delta) }}</strong></div></div>
          <el-empty v-if="!stressResult" description="选择情景后运行压力测试" :image-size="72" />
          <el-table v-else :data="stressResult.impact_samples || []" size="small" border max-height="310"><el-table-column prop="debt_no" label="债项编号" min-width="145" /><el-table-column prop="customer_name" label="客户" min-width="130" show-overflow-tooltip /><el-table-column label="EAD" min-width="105" align="right"><template #default="{ row }">{{ compactAmount(row.ead_amount) }}</template></el-table-column><el-table-column label="衰退 LGD" width="90" align="right"><template #default="{ row }">{{ percent(row.current_lgd_downturn) }}</template></el-table-column><el-table-column label="压力 LGD" width="90" align="right"><template #default="{ row }">{{ percent(row.stressed_lgd_downturn) }}</template></el-table-column><el-table-column label="EL 增量" min-width="100" align="right"><template #default="{ row }">{{ compactAmount(row.el_delta) }}</template></el-table-column></el-table>
        </article>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="debtDrawer" size="min(720px, 94vw)" title="债项 LGD 穿透明细">
      <div v-loading="detailLoading" class="detail-drawer">
        <template v-if="debtDetail.detail">
          <div class="debt-heading"><div><strong>{{ debtDetail.detail.debt_no }}</strong><span>{{ debtDetail.detail.customer_name }} · {{ productLabel(debtDetail.detail.product_type) }}</span></div><el-tag :type="Number(debtDetail.detail.default_flag) === 1 ? 'danger' : 'info'" effect="plain">{{ Number(debtDetail.detail.default_flag) === 1 ? '违约债项' : '有效债项' }}</el-tag></div>
          <div class="debt-metrics"><div><span>EAD</span><strong>{{ amount(debtDetail.detail.ead_amount) }}</strong></div><div><span>平均 LGD</span><strong>{{ percent(debtDetail.detail.lgd_avg) }}</strong></div><div><span>衰退 LGD</span><strong>{{ percent(debtDetail.detail.lgd_downturn) }}</strong></div><div><span>回收率</span><strong>{{ percent(debtDetail.detail.product_recovery_rate) }}</strong></div><div><span>覆盖率</span><strong>{{ percent(debtDetail.detail.coverage_rate) }}</strong></div><div><span>衰退 EL</span><strong>{{ amount(debtDetail.detail.el_downturn) }}</strong></div></div>
          <h4>抵押品与担保</h4>
          <el-table :data="debtDetail.collaterals || []" size="small" max-height="180"><el-table-column prop="collateral_no" label="押品编号" min-width="130" /><el-table-column prop="collateral_type" label="类型" min-width="90" /><el-table-column label="确认价值" min-width="100" align="right"><template #default="{ row }">{{ compactAmount(row.confirmed_value) }}</template></el-table-column><el-table-column label="担保金额" min-width="100" align="right"><template #default="{ row }">{{ compactAmount(row.secured_amount) }}</template></el-table-column></el-table>
          <h4>逾期与违约证据</h4>
          <div class="evidence-grid"><div><span>逾期记录</span><strong>{{ debtDetail.overdues?.length || 0 }} 笔</strong><small>{{ maxOverdueDays }} 天最大逾期</small></div><div><span>违约记录</span><strong>{{ debtDetail.defaults?.length || 0 }} 笔</strong><small>{{ debtDetail.defaults?.[0]?.default_level || '无' }} 级</small></div><div><span>评级结果</span><strong>{{ debtDetail.detail.rating_result || '-' }}</strong><small>五级分类 {{ debtDetail.detail.five_category || '-' }}</small></div></div>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import { ElButton, ElTable, ElTableColumn } from 'element-plus'
import { Refresh, Search, TrendCharts } from '@element-plus/icons-vue'
import http from '../api/http'

const activeTab = ref('overview')
const loading = ref(false)
const calculating = ref(false)
const stressing = ref(false)
const scenario = ref('COLLATERAL_HAIRCUT')
const overview = ref({ summary: {}, product_distribution: [], industry_distribution: [], high_lgd_exposures: [] })
const governance = ref({})
const stressResult = ref(null)
const debtDrawer = ref(false)
const detailLoading = ref(false)
const debtDetail = ref({})
const filters = reactive({ keyword: '', productType: '', defaultFlag: '', minLgdDownturn: undefined })
const ledger = reactive({ rows: [], total: 0, page: 1, size: 12, loading: false })

const summary = computed(() => overview.value.summary || {})
const lgdGap = computed(() => Number(summary.value.weighted_lgd_downturn || 0) - Number(summary.value.weighted_lgd_avg || 0))
const maxOverdueDays = computed(() => Math.max(0, ...(debtDetail.value.overdues || []).map((item) => Number(item.overdue_days || 0))))

const ExposureTable = defineComponent({
  props: { rows: { type: Array, default: () => [] }, loading: Boolean },
  emits: ['detail'],
  setup(props, { emit }) {
    return () => h(ElTable, { data: props.rows, loading: props.loading, size: 'small', border: true, maxHeight: 330, onRowClick: (row) => emit('detail', row) }, {
      default: () => [
        h(ElTableColumn, { prop: 'debt_no', label: '债项编号', minWidth: 145 }),
        h(ElTableColumn, { prop: 'customer_name', label: '客户', minWidth: 130, showOverflowTooltip: true }),
        h(ElTableColumn, { prop: 'product_type', label: '产品', minWidth: 116, formatter: (row) => productLabel(row.product_type) }),
        h(ElTableColumn, { label: 'EAD', minWidth: 105, align: 'right' }, { default: ({ row }) => compactAmount(row.ead_amount) }),
        h(ElTableColumn, { label: '平均 LGD', width: 92, align: 'right' }, { default: ({ row }) => percent(row.lgd_avg) }),
        h(ElTableColumn, { label: '衰退 LGD', width: 92, align: 'right' }, { default: ({ row }) => percent(row.lgd_downturn) }),
        h(ElTableColumn, { label: '回收率', width: 84, align: 'right' }, { default: ({ row }) => percent(row.product_recovery_rate) }),
        h(ElTableColumn, { label: '覆盖率', width: 84, align: 'right' }, { default: ({ row }) => percent(row.coverage_rate) }),
        h(ElTableColumn, { label: '衰退 EL', minWidth: 105, align: 'right' }, { default: ({ row }) => compactAmount(row.el_downturn) }),
        h(ElTableColumn, { label: '核验', width: 66, fixed: 'right' }, { default: ({ row }) => h(ElButton, { link: true, type: 'primary', onClick: (event) => { event.stopPropagation(); emit('detail', row) } }, () => '明细') })
      ]
    })
  }
})

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    overview.value = await http.get('/api/risks/lgd/overview')
    if (activeTab.value === 'governance') await loadGovernance()
  } finally { loading.value = false }
}
async function onTabChange(tab) {
  if (tab === 'ledger' && !ledger.rows.length) await loadLedger()
  if (tab === 'governance' && !governance.value.published_version) await loadGovernance()
}
async function loadLedger() {
  ledger.loading = true
  try {
    const params = { page: ledger.page, size: ledger.size }
    Object.entries(filters).forEach(([key, value]) => { if (value !== '' && value !== undefined && value !== null) params[key] = value })
    const data = await http.get('/api/risks/lgd/ledger', { params })
    ledger.rows = data.items || data.records || data.list || []
    ledger.total = Number(data.total || 0)
  } finally { ledger.loading = false }
}
async function searchLedger() { ledger.page = 1; await loadLedger() }
async function resetLedger() { Object.assign(filters, { keyword: '', productType: '', defaultFlag: '', minLgdDownturn: undefined }); await searchLedger() }
async function loadGovernance() { governance.value = await http.get('/api/risks/lgd/governance') }
async function captureCalculation() { calculating.value = true; try { await http.post('/api/risks/lgd/calculation-runs'); await loadGovernance() } finally { calculating.value = false } }
async function runStress() { stressing.value = true; try { const data = await http.post('/api/risks/lgd/stress-tests', { scenario_code: scenario.value }); stressResult.value = data.summary; await loadGovernance() } finally { stressing.value = false } }
async function openDebt(row) { if (!row?.debt_no) return; debtDrawer.value = true; detailLoading.value = true; debtDetail.value = {}; try { debtDetail.value = await http.get(`/api/risks/lgd/debts/${encodeURIComponent(row.debt_no)}`) } finally { detailLoading.value = false } }

function number(value) { return Number(value || 0).toLocaleString('zh-CN') }
function amount(value) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }
function compactAmount(value) { const numberValue = Number(value || 0); return numberValue >= 100000000 ? `¥${(numberValue / 100000000).toFixed(2)}亿` : numberValue >= 10000 ? `¥${(numberValue / 10000).toFixed(2)}万` : amount(numberValue) }
function percent(value) { return `${(Number(value || 0) * 100).toFixed(2)}%` }
function signedPercent(value) { const normalized = Number(value || 0); return `${normalized >= 0 ? '+' : ''}${(normalized * 100).toFixed(2)}%` }
function signedAmount(value) { const normalized = Number(value || 0); return `${normalized >= 0 ? '+' : '-'}${amount(Math.abs(normalized))}` }
function signedNumber(value) { const normalized = Number(value || 0); return `${normalized >= 0 ? '+' : ''}${normalized}` }
function barWidth(value) { return `${Math.max(8, Math.min(100, Number(value || 0) * 100))}%` }
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
function productLabel(value) { return ({ WORKING_CAPITAL_LOAN: '流动资金贷款', FIXED_ASSET_LOAN: '固定资产贷款' })[value] || value || '-' }
</script>

<style scoped>
.lgd-page { display: grid; gap: 16px; }
.page-toolbar, .toolbar-actions, .panel-title, .stress-actions, .filter-row, .pagination, .debt-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
h2, h3, h4, p { margin: 0; }
h2 { color: #1f2937; font-size: 18px; font-weight: 650; }
.page-toolbar p, .panel-title span { color: #64748b; font-size: 13px; margin-top: 5px; }
.metric-strip { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); background: #fff; border: 1px solid #dce4ee; border-radius: 8px; overflow: hidden; }
.metric-strip > div, .governance-strip > div { display: grid; gap: 5px; min-height: 96px; padding: 14px; border-right: 1px solid #e8eef4; }
.metric-strip > div:last-child, .governance-strip > div:last-child { border-right: 0; }
.metric-strip span, .metric-strip small, .governance-strip span, .governance-strip small { color: #64748b; font-size: 12px; }
.metric-strip strong, .governance-strip strong { color: #1f2937; font-size: 19px; overflow-wrap: anywhere; }
.metric-strip .emphasis { color: #9a4b1f; }
.metric-strip .danger, .danger { color: #c2413a !important; }
.lgd-tabs :deep(.el-tabs__header) { margin-bottom: 12px; }
.insight-grid { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(360px, .8fr); gap: 16px; }
.panel { padding: 16px; }
.panel-title { margin-bottom: 13px; }
.panel-title h3 { color: #334155; font-size: 15px; }
.distribution-list { display: grid; gap: 16px; }
.distribution-row { display: grid; grid-template-columns: 145px minmax(180px, 1fr) 74px; gap: 12px; align-items: center; }
.distribution-name, .distribution-rate { display: grid; gap: 4px; }
.distribution-name strong, .distribution-rate strong { color: #334155; font-size: 13px; }
.distribution-name span, .distribution-rate span { color: #64748b; font-size: 11px; }
.bar-stack { display: grid; gap: 5px; }
.bar { display: flex; min-width: 58px; height: 18px; align-items: center; padding: 0 6px; box-sizing: border-box; color: #fff; font-size: 10px; white-space: nowrap; border-radius: 2px; }
.bar.average { background: #527ca9; }
.bar.downturn { background: #c56e32; }
.exposure-panel { margin-top: 16px; }
.ledger-panel { min-height: 480px; }
.filter-row { justify-content: flex-start; flex-wrap: wrap; margin-bottom: 14px; }
.filter-row .el-input { width: 240px; }
.filter-row .el-select, .filter-row .el-input-number { width: 150px; }
.pagination { margin-top: 14px; color: #64748b; font-size: 13px; }
.governance-strip { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); margin-bottom: 16px; background: #fff; border: 1px solid #dce4ee; border-radius: 8px; overflow: hidden; }
.run-action { align-content: center; }
.run-action .el-button { width: max-content; }
.stress-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); margin-bottom: 16px; border: 1px solid #e5ebf1; }
.stress-summary > div { display: grid; gap: 6px; padding: 13px; border-right: 1px solid #e5ebf1; }
.stress-summary > div:last-child { border-right: 0; }
.stress-summary span { color: #64748b; font-size: 12px; }
.stress-summary strong { color: #1f2937; font-size: 15px; line-height: 1.45; }
.detail-drawer { display: grid; gap: 16px; min-height: 200px; }
.debt-heading > div { display: grid; gap: 5px; }
.debt-heading strong { color: #1f2937; font-size: 17px; }
.debt-heading span { color: #64748b; font-size: 13px; }
.debt-metrics { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); border: 1px solid #e5ebf1; }
.debt-metrics > div { display: grid; gap: 5px; padding: 11px; border-right: 1px solid #e5ebf1; border-bottom: 1px solid #e5ebf1; }
.debt-metrics > div:nth-child(3n) { border-right: 0; }
.debt-metrics > div:nth-last-child(-n + 3) { border-bottom: 0; }
.debt-metrics span, .evidence-grid span, .evidence-grid small { color: #64748b; font-size: 12px; }
.debt-metrics strong, .evidence-grid strong { color: #1f2937; font-size: 15px; }
h4 { color: #334155; font-size: 14px; margin-top: 4px; }
.evidence-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); border: 1px solid #e5ebf1; }
.evidence-grid > div { display: grid; gap: 4px; padding: 11px; border-right: 1px solid #e5ebf1; }
.evidence-grid > div:last-child { border-right: 0; }
@media (max-width: 1250px) { .metric-strip { grid-template-columns: repeat(3, minmax(0, 1fr)); } .metric-strip > div:nth-child(3n) { border-right: 0; } .metric-strip > div:nth-child(-n + 3) { border-bottom: 1px solid #e8eef4; } }
@media (max-width: 980px) { .insight-grid { grid-template-columns: 1fr; } .governance-strip, .stress-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); } .governance-strip > div:nth-child(2n), .stress-summary > div:nth-child(2n) { border-right: 0; } .governance-strip > div:nth-child(-n + 2), .stress-summary > div:nth-child(-n + 2) { border-bottom: 1px solid #e8eef4; } }
@media (max-width: 640px) { .page-toolbar, .panel-title, .debt-heading { align-items: flex-start; flex-direction: column; } .toolbar-actions, .stress-actions { width: 100%; } .toolbar-actions .el-button, .stress-actions .el-button { flex: 1; } .metric-strip, .governance-strip, .stress-summary, .debt-metrics, .evidence-grid { grid-template-columns: 1fr; } .metric-strip > div, .governance-strip > div, .stress-summary > div, .debt-metrics > div, .evidence-grid > div { border-right: 0; border-bottom: 1px solid #e8eef4; } .metric-strip > div:last-child, .governance-strip > div:last-child, .stress-summary > div:last-child, .debt-metrics > div:last-child, .evidence-grid > div:last-child { border-bottom: 0; } .distribution-row { grid-template-columns: 1fr; gap: 7px; } .filter-row > * { width: 100% !important; } .pagination { align-items: flex-start; flex-direction: column; } }
</style>
