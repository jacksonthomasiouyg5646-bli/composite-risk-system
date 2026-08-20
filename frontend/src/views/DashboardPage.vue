<template>
  <section class="cockpit-page">
    <header class="cockpit-header">
      <div>
        <h1>组合风险驾驶舱</h1>
        <p>{{ generatedAtText }} · {{ overview.analysis_model || 'COMPOSITE_EARLY_WARNING_RULE_V1' }}</p>
      </div>
      <div class="cockpit-actions">
        <el-button plain :icon="Search" @click="router.push('/risks/credit-domain-query')">进入查询中心</el-button>
        <el-tooltip content="刷新驾驶舱" placement="bottom">
          <el-button :icon="Refresh" circle :loading="loading" @click="loadOverview" />
        </el-tooltip>
      </div>
    </header>

    <div class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-item" :class="item.tone">
        <div class="metric-title">
          <span>{{ item.label }}</span>
          <el-popover placement="top" width="300" trigger="hover">
            <template #reference>
              <el-button class="metric-info" :icon="InfoFilled" text circle size="small" :aria-label="`${item.label}口径说明`" />
            </template>
            <div class="metric-popover">
              <strong>{{ item.label }}</strong>
              <p>{{ item.definition }}</p>
              <dl>
                <dt>数据来源</dt>
                <dd>{{ item.source }}</dd>
                <dt>计算口径</dt>
                <dd>{{ item.formula }}</dd>
                <dt>刷新频率</dt>
                <dd>{{ item.frequency }}</dd>
              </dl>
            </div>
          </el-popover>
        </div>
        <strong :title="item.fullValue || item.value" :class="{ compact: item.compact }">{{ item.value }}</strong>
        <small>{{ item.detail }}</small>
      </div>
    </div>

    <div class="cockpit-grid primary-grid">
      <section class="surface alert-surface">
        <div class="surface-heading">
          <div>
            <h2>今日重点预警</h2>
            <span>{{ alerts.length }} 个客户达到组合预警阈值</span>
          </div>
          <el-tag type="danger" effect="plain">风险评分 ≥ 45</el-tag>
        </div>
        <el-table v-loading="loading" :data="alerts" height="400" class="alert-table">
          <el-table-column label="优先级" width="76">
            <template #default="{ row }">
              <el-tag size="small" :type="priorityType(row.priority)">{{ row.priority }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="customer_no" label="客户编号" min-width="146" show-overflow-tooltip />
          <el-table-column prop="customer_name" label="客户名称" min-width="176" show-overflow-tooltip />
          <el-table-column label="风险等级" width="106">
            <template #default="{ row }">
              <span class="risk-pill" :class="riskClass(row.risk_level)">{{ row.risk_level }}</span>
            </template>
          </el-table-column>
          <el-table-column label="30日预测" width="106">
            <template #default="{ row }">
              <span class="risk-pill" :class="riskClass(row.forecast_level)">{{ row.forecast_level }}</span>
            </template>
          </el-table-column>
          <el-table-column label="评分" width="74" align="right">
            <template #default="{ row }">{{ formatInteger(row.risk_score) }}</template>
          </el-table-column>
          <el-table-column prop="alert_type" label="主信号" min-width="116" show-overflow-tooltip />
          <el-table-column label="风险证据" min-width="245" show-overflow-tooltip>
            <template #default="{ row }">{{ row.risk_signals || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openCustomer(row)">客户 360</el-button>
              <el-button link type="success" @click="openScoring(row)">评分拆解</el-button>
              <el-button
                v-if="canCreateTreatment"
                link
                type="warning"
                :loading="creatingCustomerNo === row.customer_no"
                @click="createTreatment(row)"
              >处置</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <aside class="side-stack">
        <section class="surface industry-surface">
          <div class="surface-heading compact">
            <div>
              <h2>行业风险分布</h2>
              <span>按高风险客户数排序</span>
            </div>
          </div>
          <div class="industry-list">
            <div v-for="item in industryDistribution" :key="item.industry_name" class="industry-row">
              <div class="industry-name" :title="item.industry_name">{{ item.industry_name }}</div>
              <div class="industry-track"><i :style="{ width: `${item.percent}%` }"></i></div>
              <strong>{{ formatInteger(item.high_risk_count) }}</strong>
            </div>
            <el-empty v-if="!industryDistribution.length" :image-size="54" description="暂无行业风险数据" />
          </div>
        </section>

        <section class="surface trend-surface">
          <div class="surface-heading compact">
            <div>
              <h2>近 14 个风险日期</h2>
              <span><i class="legend overdue"></i>逾期 <i class="legend default"></i>违约</span>
            </div>
          </div>
          <div class="mini-chart" aria-label="逾期与违约趋势">
            <div v-for="item in trendBars" :key="item.stat_date" class="mini-bar-group">
              <div class="mini-bars">
                <i class="mini-bar overdue" :style="{ height: `${item.overdueHeight}%` }" :title="`${item.stat_date} 逾期 ${item.overdue_count}`"></i>
                <i class="mini-bar default" :style="{ height: `${item.defaultHeight}%` }" :title="`${item.stat_date} 违约 ${item.debt_default_count}`"></i>
              </div>
              <span>{{ String(item.stat_date).slice(5) }}</span>
            </div>
            <el-empty v-if="!trendBars.length" :image-size="54" description="暂无趋势数据" />
          </div>
        </section>
      </aside>
    </div>

    <section class="surface workflow-surface">
      <div class="surface-heading">
        <div>
          <h2>组合处置闭环</h2>
          <span>预警识别 · 人工核验 · 整改复核</span>
        </div>
        <el-button text type="primary" :icon="ArrowRight" @click="router.push('/risks/treatments')">查看整改任务</el-button>
      </div>
      <div class="workflow-steps">
        <div><b>1</b><span>组合预警</span><small>违约、逾期、评级、PD、额度、押品</small></div>
        <div><b>2</b><span>客户 360</span><small>风险证据、业务时间轴与 AI 建议</small></div>
        <div><b>3</b><span>处置任务</span><small>按客户自动关联风险台账与责任人</small></div>
        <div><b>4</b><span>复核关闭</span><small>处理进度回流至风险管理台账</small></div>
      </div>
    </section>

    <section class="surface migration-surface">
      <div class="surface-heading">
        <div>
          <h2>30 天风险迁移预测</h2>
          <span>当前风险等级与预测风险等级对比</span>
        </div>
        <el-tag type="warning" effect="plain">上迁 {{ formatInteger(overview.summary?.forecast_upgrade_count) }} 个客户</el-tag>
      </div>
      <div class="migration-grid">
        <div v-for="item in migrationRows" :key="item.risk_level" class="migration-item" :class="riskClass(item.risk_level)">
          <span>{{ item.risk_level }}</span>
          <strong>{{ formatInteger(item.forecast_count) }}</strong>
          <small>当前 {{ formatInteger(item.current_count) }} · 上迁 {{ formatInteger(item.upgrade_count) }}</small>
        </div>
      </div>
    </section>

    <el-drawer v-model="scoringVisible" size="min(760px, 100%)" :with-header="false" class="customer-drawer scoring-drawer">
      <div class="drawer-header">
        <div>
          <span>组合评分拆解</span>
          <h2>{{ scoringDetail?.customer_name || selectedScoringAlert?.customer_name || '-' }}</h2>
          <small>{{ scoringDetail?.customer_no || selectedScoringAlert?.customer_no || '-' }}</small>
        </div>
        <el-button :icon="Close" circle @click="scoringVisible = false" />
      </div>

      <div v-loading="scoringLoading" class="drawer-body">
        <template v-if="scoringDetail">
          <section class="drawer-risk-summary">
            <div>
              <span>当前评分</span>
              <strong :class="riskClass(scoringDetail.risk_level)">{{ scoringDetail.risk_score }}</strong>
            </div>
            <div>
              <span>30日预测</span>
              <strong :class="riskClass(scoringDetail.forecast_level)">{{ scoringDetail.forecast_score }}</strong>
            </div>
            <div>
              <span>命中规则</span>
              <strong>{{ hitFactors.length }} / {{ scoringDetail.rule_count || scoringFactors.length }}</strong>
            </div>
          </section>

          <p class="drawer-conclusion">
            {{ scoringDetail.risk_level }} · {{ scoringDetail.forecast_change }} · 基准分 {{ scoringDetail.base_score }}，预测加分 {{ scoringDetail.forecast_boost }}
          </p>

          <section class="explain-section">
            <h3>命中因子</h3>
            <el-table :data="hitFactors" size="small" empty-text="暂无命中规则">
              <el-table-column prop="risk_tag" label="因子" min-width="120" show-overflow-tooltip />
              <el-table-column prop="metric_display" label="当前值" width="100" />
              <el-table-column label="阈值" width="120">
                <template #default="{ row }">{{ row.operator_label }} {{ row.threshold_display }}</template>
              </el-table-column>
              <el-table-column label="贡献" width="82" align="right">
                <template #default="{ row }">+{{ row.contribution }}</template>
              </el-table-column>
              <el-table-column prop="reason" label="解释" min-width="210" show-overflow-tooltip />
            </el-table>
          </section>

          <section class="explain-section">
            <h3>30日预测因子</h3>
            <div class="forecast-factor-list">
              <div v-for="item in scoringDetail.forecast_factors || []" :key="item.factor_name" :class="{ hit: item.hit }">
                <span>{{ item.factor_name }}</span>
                <small>{{ item.metric_value }} / {{ item.threshold }}</small>
                <b>+{{ item.contribution }}</b>
              </div>
            </div>
          </section>

          <section class="explain-section">
            <h3>评分公式说明</h3>
            <ul>
              <li v-for="note in scoringDetail.formula_notes || []" :key="note">{{ note }}</li>
            </ul>
          </section>
        </template>
      </div>
    </el-drawer>

    <el-drawer v-model="customerVisible" size="min(860px, 100%)" :with-header="false" class="customer-drawer">
      <div class="drawer-header">
        <div>
          <span>客户 360 风险画像</span>
          <h2>{{ customerAnalysis?.customer?.customer_name || selectedAlert?.customer_name || '-' }}</h2>
          <small>{{ customerAnalysis?.customer?.customer_no || selectedAlert?.customer_no || '-' }}</small>
        </div>
        <el-button :icon="Close" circle @click="customerVisible = false" />
      </div>

      <div v-loading="customerLoading" class="drawer-body">
        <template v-if="customerAnalysis">
          <section class="drawer-risk-summary">
            <div>
              <span>当前风险等级</span>
              <strong :class="riskClass(customerAnalysis.risk_level)">{{ customerAnalysis.risk_level }}</strong>
            </div>
            <div>
              <span>综合风险评分</span>
              <strong>{{ customerAnalysis.risk_score }}</strong>
            </div>
            <div>
              <span>外部数据状态</span>
              <strong class="source-state">{{ customerAnalysis.external_data?.status_label || '仅本地分析' }}</strong>
            </div>
          </section>

          <p class="drawer-conclusion">{{ customerAnalysis.risk_conclusion }}</p>

          <div class="drawer-tags">
            <el-tag v-for="tag in customerAnalysis.risk_tags || []" :key="tag" effect="plain" :type="tagType(customerAnalysis.risk_level)">
              {{ tag }}
            </el-tag>
          </div>

          <div class="drawer-grid">
            <section>
              <h3>风险证据</h3>
              <ul>
                <li v-for="item in customerAnalysis.risk_reasons || []" :key="item">{{ item }}</li>
              </ul>
            </section>
            <section>
              <h3>建议动作</h3>
              <ul>
                <li v-for="item in customerAnalysis.recommendations || []" :key="item">{{ item }}</li>
              </ul>
            </section>
          </div>

          <section class="timeline-section">
            <h3>业务与风险时间轴</h3>
            <el-timeline>
              <el-timeline-item
                v-for="item in customerAnalysis.customer_timeline || []"
                :key="`${item.event_date}-${item.event_title}-${item.event_type}`"
                :timestamp="item.event_date"
                :type="timelineType(item.risk_marker)"
                placement="top"
              >
                <strong>{{ item.event_type }} · {{ item.event_title }}</strong>
                <p>{{ item.event_detail }}</p>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-if="!(customerAnalysis.customer_timeline || []).length" :image-size="54" description="暂无客户业务时间轴" />
          </section>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, Close, InfoFilled, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const customerLoading = ref(false)
const scoringLoading = ref(false)
const creatingCustomerNo = ref('')
const customerVisible = ref(false)
const scoringVisible = ref(false)
const selectedAlert = ref(null)
const selectedScoringAlert = ref(null)
const customerAnalysis = ref(null)
const scoringDetail = ref(null)
const overview = ref({ summary: {}, alerts: [], warning_trend: [], industry_distribution: [] })

const alerts = computed(() => overview.value.alerts || [])
const canCreateTreatment = computed(() => auth.hasPermission('risk:treat'))
const scoringFactors = computed(() => scoringDetail.value?.factors || [])
const hitFactors = computed(() => scoringFactors.value.filter((item) => item.hit && !item.ignored))
const metricGlossary = {
  监测客户: {
    definition: '纳入组合风险驾驶舱监测范围的信贷域有效客户数量。',
    source: 'risk customer feature 视图 / 数据库实时聚合',
    formula: '去重 customer_no 后统计客户总数',
    frequency: '页面刷新时实时读取'
  },
  极高风险: {
    definition: '当前组合风险评分达到 P1 处置阈值的客户。',
    source: '评分规则表 + 客户风险特征',
    formula: 'risk_score ≥ 85 的客户数',
    frequency: '页面刷新时实时计算'
  },
  高风险: {
    definition: '当前组合风险评分达到 P2 跟踪阈值但未进入 P1 的客户。',
    source: '评分规则表 + 客户风险特征',
    formula: '65 ≤ risk_score < 85 的客户数',
    frequency: '页面刷新时实时计算'
  },
  预警敞口: {
    definition: '已触发组合预警客户对应的风险暴露余额。',
    source: '客户风险特征中的 EAD 字段',
    formula: 'risk_score ≥ 45 客户的 ead_amount_total 求和',
    frequency: '页面刷新时实时计算'
  },
  待办处置: {
    definition: '风险整改计划中仍未关闭的待处理任务。',
    source: '风险处置计划表',
    formula: '状态非关闭/完成的处置计划数量',
    frequency: '页面刷新时实时统计'
  },
  '30天风险上迁': {
    definition: '30 日预测风险等级高于当前等级的客户。',
    source: '当前评分 + 预测因子',
    formula: 'forecast_level 排名高于 risk_level 的客户数',
    frequency: '页面刷新时实时计算'
  },
  行业集中度: {
    definition: '预测高风险敞口中，最高行业敞口占全部预测高风险敞口的比例。',
    source: '客户行业、EAD 与预测评分',
    formula: 'top_industry_high_risk_ead / all_high_risk_ead',
    frequency: '页面刷新时实时计算'
  }
}
const generatedAtText = computed(() => {
  const value = overview.value.generated_at
  if (!value) return '实时组合预警'
  return `更新于 ${String(value).replace('T', ' ').slice(0, 16)}`
})
const metrics = computed(() => {
  const summary = overview.value.summary || {}
  return [
    { label: '监测客户', value: formatInteger(summary.customer_total), detail: '信贷域有效客户', tone: 'neutral' },
    { label: '极高风险', value: formatInteger(summary.extreme_risk_count), detail: '优先处置 P1', tone: 'critical' },
    { label: '高风险', value: formatInteger(summary.high_risk_count), detail: '重点跟踪 P2', tone: 'warning' },
    {
      label: '预警敞口',
      value: formatCompactAmount(summary.warning_ead_amount),
      fullValue: formatAmount(summary.warning_ead_amount),
      detail: `${formatInteger(summary.warning_customer_count)} 个预警客户`,
      tone: 'attention',
      compact: true
    },
    { label: '待办处置', value: formatInteger(summary.open_treatment_count), detail: '风险整改计划', tone: 'success' },
    { label: '30天风险上迁', value: formatInteger(summary.forecast_upgrade_count), detail: `预测高风险 ${formatInteger(summary.forecast_high_risk_count)} 个`, tone: 'warning' },
    { label: '行业集中度', value: formatPercent(summary.top_industry_concentration), detail: summary.top_industry_name || '-', tone: 'attention' }
  ].map((item) => ({ ...metricGlossary[item.label], ...item }))
})
const industryDistribution = computed(() => {
  const rows = overview.value.industry_distribution || []
  const max = Math.max(1, ...rows.map((item) => toNumber(item.high_risk_count)))
  return rows.map((item) => ({
    ...item,
    percent: Math.max(5, Math.round((toNumber(item.high_risk_count) / max) * 100))
  }))
})
const trendBars = computed(() => {
  const rows = overview.value.warning_trend || []
  const max = Math.max(1, ...rows.flatMap((item) => [toNumber(item.overdue_count), toNumber(item.debt_default_count)]))
  return rows.map((item) => ({
    ...item,
    overdueHeight: Math.max(4, Math.round((toNumber(item.overdue_count) / max) * 100)),
    defaultHeight: Math.max(4, Math.round((toNumber(item.debt_default_count) / max) * 100))
  }))
})
const migrationRows = computed(() => overview.value.risk_migration || [])

onMounted(loadOverview)

async function loadOverview() {
  loading.value = true
  try {
    overview.value = await http.get('/api/risks/composite-dashboard')
  } finally {
    loading.value = false
  }
}

async function openCustomer(alert) {
  selectedAlert.value = alert
  customerAnalysis.value = null
  customerVisible.value = true
  customerLoading.value = true
  try {
    customerAnalysis.value = await http.get('/api/risks/ai-analysis/customer', {
      params: { customer: alert.customer_no, includeExternal: true }
    })
  } finally {
    customerLoading.value = false
  }
}

async function openScoring(alert) {
  selectedScoringAlert.value = alert
  scoringDetail.value = null
  scoringVisible.value = true
  scoringLoading.value = true
  try {
    scoringDetail.value = await http.get(`/api/risks/composite-dashboard/customers/${encodeURIComponent(alert.customer_no)}/scoring-explanation`)
  } finally {
    scoringLoading.value = false
  }
}

async function createTreatment(alert) {
  creatingCustomerNo.value = alert.customer_no
  try {
    const result = await http.post(`/api/risks/composite-dashboard/alerts/${encodeURIComponent(alert.customer_no)}/treatment`)
    ElMessage.success(result.created ? '已生成处置任务' : '该客户已有处置任务')
    await loadOverview()
  } finally {
    creatingCustomerNo.value = ''
  }
}

function priorityType(priority) {
  if (priority === 'P1') return 'danger'
  if (priority === 'P2') return 'warning'
  if (priority === 'P3') return ''
  return 'info'
}

function riskClass(level) {
  if (level === '极高风险') return 'risk-extreme'
  if (level === '高风险') return 'risk-high'
  if (level === '中风险') return 'risk-medium'
  return 'risk-low'
}

function tagType(level) {
  if (level === '极高风险') return 'danger'
  if (level === '高风险') return 'warning'
  if (level === '中风险') return ''
  return 'success'
}

function timelineType(marker) {
  if (String(marker).includes('违约')) return 'danger'
  if (String(marker).includes('逾期')) return 'warning'
  return 'primary'
}

function toNumber(value) {
  const number = Number(value || 0)
  return Number.isFinite(number) ? number : 0
}

function formatInteger(value) {
  return new Intl.NumberFormat('zh-CN').format(toNumber(value))
}

function formatAmount(value) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0
  }).format(toNumber(value))
}

function formatCompactAmount(value) {
  const amount = toNumber(value)
  if (Math.abs(amount) >= 100000000) return `¥${(amount / 100000000).toFixed(2)}亿`
  if (Math.abs(amount) >= 10000) return `¥${(amount / 10000).toFixed(2)}万`
  return formatAmount(amount)
}

function formatPercent(value) {
  return `${(toNumber(value) * 100).toFixed(2)}%`
}
</script>

<style scoped>
.cockpit-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.cockpit-header,
.surface-heading,
.drawer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.cockpit-header {
  padding: 2px 0 1px;
}

.cockpit-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cockpit-header h1,
.drawer-header h2 {
  margin: 0;
  color: #1f2937;
  font-size: 23px;
  line-height: 1.35;
}

.cockpit-header p,
.surface-heading span,
.drawer-header small {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  display: grid;
  gap: 5px;
  min-height: 100px;
  padding: 15px;
  border: 1px solid #dce3ec;
  border-top: 3px solid #94a3b8;
  border-radius: 8px;
  background: #fff;
}

.metric-item span,
.metric-item small {
  color: #64748b;
  font-size: 12px;
}

.metric-item strong {
  display: block;
  min-width: 0;
  color: #1f2937;
  font-size: 25px;
  line-height: 1.1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.metric-item strong.compact { font-size: clamp(19px, 1.55vw, 25px); }

.metric-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.metric-title span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric-info {
  flex: 0 0 auto;
  color: #94a3b8;
}

.metric-popover strong {
  display: block;
  margin-bottom: 6px;
  color: #1f2937;
}

.metric-popover p {
  margin: 0 0 10px;
  color: #475569;
  line-height: 1.6;
}

.metric-popover dl {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 6px 10px;
  margin: 0;
  color: #475569;
  font-size: 12px;
}

.metric-popover dt {
  color: #64748b;
}

.metric-popover dd {
  margin: 0;
}

.metric-item.critical { border-top-color: #dc2626; }
.metric-item.warning { border-top-color: #d97706; }
.metric-item.attention { border-top-color: #2563eb; }
.metric-item.success { border-top-color: #059669; }

.cockpit-grid {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.primary-grid {
  grid-template-columns: minmax(0, 1.65fr) minmax(340px, 0.75fr);
}

.surface {
  min-width: 0;
  border: 1px solid #dce3ec;
  border-radius: 8px;
  background: #fff;
}

.surface-heading {
  min-height: 64px;
  padding: 14px 16px;
  border-bottom: 1px solid #e8edf3;
}

.surface-heading h2,
.drawer-grid h3,
.timeline-section h3 {
  margin: 0;
  color: #1f2937;
  font-size: 16px;
  line-height: 1.4;
}

.surface-heading.compact {
  min-height: 56px;
}

.risk-pill {
  display: inline-block;
  font-size: 13px;
  font-weight: 700;
}

.risk-extreme,
.risk-high { color: #dc2626; }
.risk-medium { color: #c2410c; }
.risk-low { color: #047857; }

.side-stack {
  display: grid;
  grid-template-rows: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  min-width: 0;
}

.industry-list {
  display: grid;
  gap: 14px;
  padding: 17px 16px;
}

.industry-row {
  display: grid;
  grid-template-columns: minmax(96px, 1fr) minmax(84px, 1.2fr) 28px;
  gap: 10px;
  align-items: center;
}

.industry-name {
  overflow: hidden;
  color: #334155;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.industry-track {
  height: 8px;
  overflow: hidden;
  border-radius: 4px;
  background: #e8edf3;
}

.industry-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #d97706;
}

.industry-row strong {
  color: #7c2d12;
  font-size: 14px;
  text-align: right;
}

.mini-chart {
  display: flex;
  min-height: 154px;
  align-items: end;
  gap: 5px;
  padding: 12px 16px 15px;
}

.mini-bar-group {
  display: grid;
  flex: 1 1 0;
  min-width: 0;
  gap: 5px;
}

.mini-bars {
  display: flex;
  height: 108px;
  align-items: end;
  justify-content: center;
  gap: 3px;
  border-bottom: 1px solid #cbd5e1;
}

.mini-bar {
  display: block;
  width: min(11px, 38%);
  min-height: 3px;
  border-radius: 3px 3px 0 0;
}

.mini-bar.overdue,
.legend.overdue { background: #f59e0b; }
.mini-bar.default,
.legend.default { background: #dc2626; }

.mini-bar-group span {
  overflow: hidden;
  color: #64748b;
  font-size: 10px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.legend {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin: 0 4px 0 10px;
  border-radius: 2px;
}

.legend:first-child { margin-left: 0; }

.workflow-surface { padding-bottom: 15px; }

.migration-surface { padding-bottom: 15px; }

.migration-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  padding: 14px 16px 0;
}

.migration-item {
  display: grid;
  gap: 5px;
  min-height: 84px;
  padding: 12px;
  border: 1px solid #dce3ec;
  border-radius: 8px;
  background: #fff;
}

.migration-item span { font-size: 13px; font-weight: 700; }
.migration-item strong { color: #1f2937; font-size: 22px; }
.migration-item small { color: #64748b; font-size: 12px; }

.workflow-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  padding: 14px 16px 0;
}

.workflow-steps div {
  position: relative;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  column-gap: 10px;
  align-items: center;
  min-height: 58px;
}

.workflow-steps b {
  display: grid;
  grid-row: span 2;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  background: #0f766e;
  color: #fff;
  font-size: 13px;
}

.workflow-steps span {
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
}

.workflow-steps small {
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.drawer-header {
  padding: 20px 22px;
  border-bottom: 1px solid #e2e8f0;
}

.drawer-header > div > span {
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
}

.drawer-body {
  min-height: 300px;
  padding: 18px 22px 30px;
}

.drawer-risk-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border: 1px solid #dce3ec;
  border-radius: 8px;
  overflow: hidden;
}

.drawer-risk-summary div {
  display: grid;
  gap: 7px;
  min-height: 86px;
  padding: 15px;
  border-right: 1px solid #dce3ec;
}

.drawer-risk-summary div:last-child { border-right: 0; }
.drawer-risk-summary span { color: #64748b; font-size: 12px; }
.drawer-risk-summary strong { color: #1f2937; font-size: 20px; }
.drawer-risk-summary .source-state { color: #0f766e; font-size: 14px; line-height: 1.35; }

.drawer-conclusion {
  margin: 16px 0 0;
  color: #334155;
  line-height: 1.75;
}

.drawer-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.drawer-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.drawer-grid section,
.timeline-section {
  padding: 14px;
  border: 1px solid #dce3ec;
  border-radius: 8px;
}

.drawer-grid ul {
  display: grid;
  gap: 8px;
  margin: 10px 0 0;
  padding-left: 18px;
  color: #475569;
  font-size: 13px;
  line-height: 1.55;
}

.timeline-section { margin-top: 14px; }
.timeline-section :deep(.el-timeline) { margin: 14px 0 0; }
.timeline-section :deep(.el-timeline-item__content) { color: #475569; font-size: 13px; line-height: 1.55; }
.timeline-section :deep(.el-timeline-item__content strong) { color: #1f2937; }
.timeline-section :deep(.el-timeline-item__content p) { margin: 4px 0 0; }

.scoring-drawer .drawer-risk-summary strong.risk-extreme,
.scoring-drawer .drawer-risk-summary strong.risk-high {
  color: #dc2626;
}

.scoring-drawer .drawer-risk-summary strong.risk-medium {
  color: #d97706;
}

.explain-section {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid #dce3ec;
  border-radius: 8px;
  background: #fff;
}

.explain-section h3 {
  margin: 0 0 12px;
  color: #1f2937;
  font-size: 16px;
}

.forecast-factor-list {
  display: grid;
  gap: 8px;
}

.forecast-factor-list div {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 3px 12px;
  padding: 10px 12px;
  border: 1px solid #e8edf3;
  border-radius: 8px;
  background: #f8fafc;
}

.forecast-factor-list div.hit {
  border-color: #fed7aa;
  background: #fff7ed;
}

.forecast-factor-list span {
  color: #1f2937;
  font-weight: 700;
}

.forecast-factor-list small {
  color: #64748b;
}

.forecast-factor-list b {
  grid-row: span 2;
  align-self: center;
  color: #d97706;
}

.explain-section ul {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.7;
}

@media (max-width: 1280px) {
  .metric-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
  .primary-grid { grid-template-columns: 1fr; }
  .side-stack { grid-template-columns: repeat(2, minmax(0, 1fr)); grid-template-rows: none; }
}

@media (max-width: 820px) {
  .metric-grid,
  .side-stack,
  .workflow-steps,
  .migration-grid,
  .drawer-grid,
  .drawer-risk-summary { grid-template-columns: 1fr; }
  .drawer-risk-summary div { border-right: 0; border-bottom: 1px solid #dce3ec; }
  .drawer-risk-summary div:last-child { border-bottom: 0; }
  .cockpit-header { align-items: flex-start; flex-direction: column; }
  .cockpit-actions { width: 100%; }
  .workflow-steps div { min-height: 46px; }
}
</style>
