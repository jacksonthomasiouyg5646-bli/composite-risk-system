<template>
  <section class="credit-query-page">
    <header class="query-page-header">
      <div>
        <h2>信贷域综合查询</h2>
        <p>客户、额度、合同、债项、逾期与违约关联数据</p>
      </div>
      <div class="query-header-meta">
        <el-tag effect="plain">{{ activeQuery.label }}</el-tag>
        <span>已命中 {{ formatInteger(total) }} 条记录</span>
      </div>
    </header>

    <el-tabs v-model="queryType" class="query-tabs">
      <el-tab-pane v-for="item in queryTypes" :key="item.key" :label="item.label" :name="item.key" />
    </el-tabs>

    <section class="query-command-panel">
    <div class="credit-query-toolbar">
      <el-input
        v-model="filters.keyword"
        clearable
        :prefix-icon="Search"
        placeholder="综合关键字"
        class="keyword-input"
        @keyup.enter="loadData"
      />
      <el-input v-model="filters.customerNo" clearable placeholder="客户编号" class="small-input" @keyup.enter="loadData" />
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        class="date-range"
      />
      <el-input v-model="filters.ratingLevel" clearable placeholder="评级" class="mini-input" @keyup.enter="loadData" />
      <el-select v-model="filters.riskLevel" clearable placeholder="风险等级" class="small-input">
        <el-option label="LOW" value="LOW" />
        <el-option label="MEDIUM" value="MEDIUM" />
        <el-option label="HIGH" value="HIGH" />
      </el-select>
      <el-input v-model="filters.productType" clearable placeholder="产品类型" class="small-input" @keyup.enter="loadData" />
      <el-select v-model="filters.status" clearable placeholder="状态" class="small-input">
        <el-option v-for="option in activeStatusOptions" :key="option" :label="option" :value="option" />
      </el-select>
      <el-select v-model="filters.defaultLevel" clearable placeholder="违约等级" class="small-input">
        <el-option label="A" value="A" />
        <el-option label="B" value="B" />
        <el-option label="C" value="C" />
      </el-select>
      <el-input v-model="filters.ownerOrgName" clearable placeholder="所属机构" class="small-input" @keyup.enter="loadData" />
      <el-button type="primary" :icon="Search" :loading="loading" @click="loadData">查询</el-button>
      <el-button type="warning" plain :icon="MagicStick" @click="openAiDialog">AI智能分析</el-button>
      <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
    </div>
    </section>

    <div class="query-summary-row">
      <div class="query-summary-item">
        <span>查询视角</span>
        <strong>{{ activeQuery.label }}</strong>
      </div>
      <div class="query-summary-item">
        <span>总记录</span>
        <strong>{{ formatInteger(total) }}</strong>
      </div>
      <div class="query-summary-item">
        <span>当前页</span>
        <strong>{{ page }} / {{ pageCount }}</strong>
      </div>
      <div class="query-summary-item">
        <span>筛选项</span>
        <strong>{{ activeFilterCount }}</strong>
      </div>
    </div>

    <section class="panel query-panel">
      <el-table v-loading="loading" :data="rows" border highlight-current-row height="calc(100vh - 396px)" @row-click="openAiFromRow">
        <el-table-column type="index" label="#" width="56" fixed="left" />
        <el-table-column
          v-for="column in activeColumns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :width="column.width"
          :min-width="column.minWidth || 140"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ formatCell(row[column.prop], column) }}</template>
        </el-table-column>
      </el-table>
    </section>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="loadData"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="aiVisible" title="AI智能分析" width="920px" class="ai-dialog">
      <div class="ai-search-row">
        <el-input
          v-model="aiCustomer"
          clearable
          :prefix-icon="Search"
          placeholder="输入客户编号或客户名称"
          @keyup.enter="runAiAnalysis"
        />
        <el-button type="primary" :icon="MagicStick" :loading="aiLoading" @click="runAiAnalysis">开始分析</el-button>
      </div>
      <div class="ai-search-options">
        <el-switch
          v-model="includeExternalData"
          active-text="融合外部大数据"
          inactive-text="仅本地数据"
        />
      </div>

      <div v-if="aiResult" class="ai-result">
        <div class="ai-risk-head">
          <div>
            <div class="ai-caption">当前风险等级</div>
            <div class="ai-risk-level" :class="riskLevelClass(aiResult.risk_level)">{{ aiResult.risk_level }}</div>
          </div>
          <div class="ai-score-block">
            <div class="ai-score">{{ aiResult.risk_score }}</div>
            <el-progress :percentage="aiResult.risk_score" :status="progressStatus" :stroke-width="10" />
          </div>
        </div>

        <p class="ai-conclusion">{{ aiResult.risk_conclusion }}</p>

        <div class="ai-tags">
          <el-tag v-for="tag in aiResult.risk_tags" :key="tag" effect="plain" :type="tagType(aiResult.risk_level)">
            {{ tag }}
          </el-tag>
        </div>

        <section v-if="aiResult.external_data" class="ai-external-section">
          <div class="ai-external-head">
            <span>分析模型：{{ aiResult.analysis_model }}</span>
            <el-tag effect="plain" :type="externalStatusTagType(externalData.status)">
              {{ externalData.status_label }}
            </el-tag>
            <span>数据提供方：{{ externalData.provider_name || '-' }}</span>
          </div>
          <div v-if="externalData.available" class="ai-external-grid">
            <div>
              <span>外部风险评分</span>
              <strong>{{ displayValue(externalData.risk_score) }}</strong>
            </div>
            <div>
              <span>外部信用评分</span>
              <strong>{{ displayValue(externalData.credit_score) }}</strong>
            </div>
            <div>
              <span>司法执行记录</span>
              <strong>{{ displayValue(externalData.enforcement_count) }}</strong>
            </div>
            <div>
              <span>涉诉案件记录</span>
              <strong>{{ displayValue(externalData.court_case_count) }}</strong>
            </div>
            <div>
              <span>负面舆情信号</span>
              <strong>{{ displayValue(externalData.negative_news_count) }}</strong>
            </div>
            <div>
              <span>外部分数调整</span>
              <strong>+{{ displayValue(externalData.score_adjustment) }}</strong>
            </div>
          </div>
          <div v-if="externalData.risk_signals?.length" class="ai-external-signals">
            <span>外部风险信号</span>
            <el-tag v-for="signal in externalData.risk_signals" :key="signal" type="warning" effect="plain">
              {{ signal }}
            </el-tag>
          </div>
        </section>

        <div class="ai-metric-grid">
          <div v-for="item in aiMetricItems" :key="item.label" class="ai-metric">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>

        <div class="ai-two-column">
          <section class="ai-section">
            <h3>风险原因</h3>
            <ul>
              <li v-for="item in aiResult.risk_reasons" :key="item">{{ item }}</li>
            </ul>
          </section>
          <section class="ai-section">
            <h3>处置建议</h3>
            <ul>
              <li v-for="item in aiResult.recommendations" :key="item">{{ item }}</li>
            </ul>
          </section>
        </div>

        <el-tabs class="ai-detail-tabs">
          <el-tab-pane label="最近违约">
            <el-table :data="aiResult.recent_defaults || []" border height="220">
              <el-table-column prop="debt_default_no" label="违约编号" min-width="150" show-overflow-tooltip />
              <el-table-column prop="debt_no" label="债项编号" min-width="140" show-overflow-tooltip />
              <el-table-column prop="default_level" label="等级" width="80" />
              <el-table-column prop="default_date" label="违约日期" width="112" />
              <el-table-column prop="overdue_days" label="逾期天数" width="100" />
              <el-table-column label="违约敞口" width="130" align="right">
                <template #default="{ row }">{{ formatAmount(row.default_exposure_amount) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="最近逾期">
            <el-table :data="aiResult.recent_overdues || []" border height="220">
              <el-table-column prop="overdue_no" label="逾期编号" min-width="150" show-overflow-tooltip />
              <el-table-column prop="debt_no" label="债项编号" min-width="140" show-overflow-tooltip />
              <el-table-column prop="overdue_date" label="逾期日期" width="112" />
              <el-table-column prop="overdue_days" label="逾期天数" width="100" />
              <el-table-column label="逾期总额" width="130" align="right">
                <template #default="{ row }">{{ formatAmount(row.total_overdue_amount) }}</template>
              </el-table-column>
              <el-table-column prop="repayment_status" label="还款状态" width="110" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="合同与押品">
            <div class="ai-side-tables">
              <el-table :data="aiResult.recent_contracts || []" border height="220">
                <el-table-column prop="contract_no" label="合同编号" min-width="145" show-overflow-tooltip />
                <el-table-column prop="product_type" label="产品" width="110" />
                <el-table-column label="合同金额" width="125" align="right">
                  <template #default="{ row }">{{ formatAmount(row.contract_amount) }}</template>
                </el-table-column>
                <el-table-column prop="contract_status" label="状态" width="100" />
              </el-table>
              <el-table :data="aiResult.collateral_summary || []" border height="220">
                <el-table-column prop="collateral_no" label="押品编号" min-width="145" show-overflow-tooltip />
                <el-table-column prop="collateral_type" label="类型" width="110" />
                <el-table-column label="确认价值" width="125" align="right">
                  <template #default="{ row }">{{ formatAmount(row.confirmed_value) }}</template>
                </el-table-column>
                <el-table-column prop="collateral_status" label="状态" width="100" />
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { MagicStick, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import http from '../api/http'

const route = useRoute()
const queryTypes = [
  {
    key: 'customer',
    label: '客户视角',
    statusOptions: ['ACTIVE', 'INACTIVE'],
    columns: [
      col('customer_no', '客户编号', 150),
      col('customer_name', '客户名称', 190),
      col('industry_name', '行业', 150),
      col('rating_level', '评级', 90),
      col('customer_risk_level', '客户风险', 110),
      col('risk_state', '风险状态', 100),
      col('limit_no', '额度编号', 150),
      amountCol('total_limit_amount', '额度总额'),
      amountCol('available_limit_amount', '可用额度'),
      intCol('application_count', '申请数'),
      intCol('contract_count', '合同数'),
      intCol('drawdown_count', '支用数'),
      amountCol('outstanding_principal_amount_total', '未结本金'),
      amountCol('ead_amount_total', 'EAD'),
      intCol('overdue_count', '逾期数'),
      intCol('max_overdue_days', '最大逾期天数'),
      intCol('debt_default_count', '违约债项数'),
      col('highest_default_level', '最高违约等级', 120)
    ]
  },
  {
    key: 'limit',
    label: '额度视角',
    statusOptions: ['ACTIVE', 'EXPIRED', 'FROZEN', 'CLOSED'],
    columns: [
      col('limit_no', '额度编号', 150),
      col('customer_no', '客户编号', 150),
      col('customer_name', '客户名称', 190),
      col('rating_level', '评级', 90),
      col('limit_type', '额度类型', 120),
      amountCol('total_limit_amount', '额度总额'),
      amountCol('used_limit_amount', '已用额度'),
      amountCol('available_limit_amount', '可用额度'),
      col('effective_date', '生效日期', 110),
      col('expiry_date', '到期日期', 110),
      col('limit_status', '状态', 110),
      intCol('application_count', '申请数'),
      intCol('contract_count', '合同数'),
      intCol('drawdown_count', '支用数'),
      amountCol('outstanding_principal_amount_total', '未结本金'),
      amountCol('ead_amount_total', 'EAD'),
      intCol('overdue_count', '逾期数'),
      intCol('debt_default_count', '违约数')
    ]
  },
  {
    key: 'applicationContract',
    label: '申请合同视角',
    statusOptions: ['DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'ACTIVE', 'SIGNED', 'CLOSED'],
    columns: [
      col('application_no', '申请编号', 150),
      col('contract_no', '合同编号', 150),
      col('customer_no', '客户编号', 150),
      col('customer_name', '客户名称', 190),
      col('rating_level', '评级', 90),
      col('limit_no', '额度编号', 150),
      col('product_type', '产品类型', 120),
      amountCol('apply_amount', '申请金额'),
      amountCol('contract_amount', '合同金额'),
      amountCol('used_draw_amount', '已支用'),
      amountCol('available_draw_amount', '可支用'),
      col('application_status', '申请状态', 110),
      col('contract_status', '合同状态', 110),
      col('submitted_at', '提交时间', 170),
      col('sign_date', '签约日期', 110),
      col('expiry_date', '合同到期', 110),
      intCol('drawdown_count', '支用数'),
      intCol('collateral_relation_count', '押品数'),
      amountCol('total_secured_amount', '担保金额'),
      intCol('overdue_count', '逾期数'),
      intCol('debt_default_count', '违约数')
    ]
  },
  {
    key: 'debtExposure',
    label: '债项敞口视角',
    statusOptions: ['DRAFT', 'ACTIVE', 'CLOSED'],
    columns: [
      col('debt_no', '债项编号', 150),
      col('drawdown_no', '支用编号', 150),
      col('contract_no', '合同编号', 150),
      col('customer_no', '客户编号', 150),
      col('customer_name', '客户名称', 190),
      col('product_type', '产品类型', 120),
      amountCol('actual_draw_amount', '支用金额'),
      amountCol('outstanding_principal_amount', '未结本金'),
      col('drawdown_date', '支用日期', 110),
      col('maturity_date', '到期日期', 110),
      col('five_category', '五级分类', 120),
      col('drawdown_risk_level', '债项风险', 110),
      col('exposure_no', '敞口编号', 150),
      amountCol('ead_amount', 'EAD'),
      ratioCol('pd', 'PD'),
      ratioCol('lgd_avg', 'LGD平均'),
      ratioCol('lgd_downturn', 'LGD衰退'),
      amountCol('el_avg', 'EL平均'),
      intCol('overdue_count', '逾期数'),
      col('default_level', '违约等级', 100)
    ]
  },
  {
    key: 'defaultOverdue',
    label: '逾期违约视角',
    statusOptions: ['OVERDUE', 'PENDING', 'ACTIVE', 'RESOLVED', 'SETTLED'],
    columns: [
      col('overdue_no', '逾期编号', 150),
      col('debt_default_no', '违约编号', 150),
      col('customer_no', '客户编号', 150),
      col('customer_name', '客户名称', 190),
      col('contract_no', '合同编号', 150),
      col('debt_no', '债项编号', 150),
      col('product_type', '产品类型', 120),
      col('overdue_date', '逾期日期', 110),
      intCol('overdue_days', '逾期天数'),
      col('grace_due_date', '宽限日期', 110),
      flagCol('legal_holiday_flag', '法定假期'),
      amountCol('total_overdue_amount', '逾期总额'),
      amountCol('remaining_overdue_amount', '剩余逾期'),
      col('repayment_status', '还款状态', 110),
      col('collection_status', '催收状态', 110),
      col('default_level', '违约等级', 100),
      col('default_date', '违约日期', 110),
      amountCol('default_exposure_amount', '违约敞口'),
      amountCol('loss_amount', '损失金额')
    ]
  }
]

const queryType = ref('customer')
const dateRange = ref([])
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const aiVisible = ref(false)
const aiCustomer = ref('')
const aiLoading = ref(false)
const aiResult = ref(null)
const includeExternalData = ref(true)
const filters = reactive({
  keyword: '',
  customerNo: '',
  ratingLevel: '',
  riskLevel: '',
  productType: '',
  status: '',
  defaultLevel: '',
  ownerOrgName: ''
})

const activeQuery = computed(() => queryTypes.find((item) => item.key === queryType.value) || queryTypes[0])
const activeColumns = computed(() => activeQuery.value.columns)
const activeStatusOptions = computed(() => activeQuery.value.statusOptions)
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / size.value)))
const activeFilterCount = computed(() => {
  const filterValues = Object.values(filters).filter((value) => String(value || '').trim())
  return filterValues.length + (dateRange.value?.length === 2 ? 1 : 0)
})
const progressStatus = computed(() => {
  const score = Number(aiResult.value?.risk_score || 0)
  if (score >= 85) return 'exception'
  if (score >= 70) return 'warning'
  if (score >= 45) return ''
  return 'success'
})
const externalData = computed(() => aiResult.value?.external_data || {})
const aiMetricItems = computed(() => {
  const credit = aiResult.value?.credit_summary || {}
  const risk = aiResult.value?.risk_metrics || {}
  return [
    { label: '额度总额', value: formatAmount(credit.total_limit_amount) },
    { label: '已用额度', value: formatAmount(credit.used_limit_amount) },
    { label: '未结本金', value: formatAmount(credit.outstanding_principal_amount_total) },
    { label: 'EAD', value: formatAmount(risk.ead_amount_total) },
    { label: '最大PD', value: formatPercent(risk.max_pd) },
    { label: '最大逾期天数', value: `${formatInteger(risk.max_overdue_days)} 天` },
    { label: '逾期笔数', value: formatInteger(risk.overdue_count) },
    { label: '违约债项数', value: formatInteger(risk.debt_default_count) }
  ]
})

watch(queryType, () => {
  filters.status = ''
  page.value = 1
  loadData()
})

watch(() => route.query.keyword, (value) => {
  const keyword = String(value || '').trim()
  if (keyword && keyword !== filters.keyword) {
    filters.keyword = keyword
    page.value = 1
    loadData()
  }
})

onMounted(() => {
  const keyword = String(route.query.keyword || '').trim()
  if (keyword) filters.keyword = keyword
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const params = {
      queryType: queryType.value,
      page: page.value,
      size: size.value
    }

    Object.entries(filters).forEach(([key, value]) => {
      if (String(value || '').trim()) {
        params[key] = String(value).trim()
      }
    })

    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const data = await http.get('/api/risks/credit-domain/query', { params })
    rows.value = data.items || []
    total.value = data.total || 0
    page.value = data.page || page.value
    size.value = data.size || size.value
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.keys(filters).forEach((key) => {
    filters[key] = ''
  })
  dateRange.value = []
  page.value = 1
  loadData()
}

function handleSizeChange() {
  page.value = 1
  loadData()
}

function openAiDialog() {
  aiCustomer.value = filters.customerNo || filters.keyword || ''
  aiResult.value = null
  aiVisible.value = true
}

function openAiFromRow(row) {
  aiCustomer.value = row.customer_no || filters.customerNo || ''
  aiResult.value = null
  aiVisible.value = true
  if (aiCustomer.value) runAiAnalysis()
}

async function runAiAnalysis() {
  const customer = aiCustomer.value.trim()
  if (!customer) {
    ElMessage.warning('请输入客户编号或客户名称')
    return
  }
  aiLoading.value = true
  try {
    aiResult.value = await http.get('/api/risks/ai-analysis/customer', {
      params: {
        customer,
        includeExternal: includeExternalData.value
      }
    })
  } finally {
    aiLoading.value = false
  }
}

function col(prop, label, width) {
  return { prop, label, width }
}

function amountCol(prop, label) {
  return { prop, label, minWidth: 130, type: 'amount' }
}

function intCol(prop, label) {
  return { prop, label, minWidth: 110, type: 'integer' }
}

function ratioCol(prop, label) {
  return { prop, label, minWidth: 110, type: 'ratio' }
}

function flagCol(prop, label) {
  return { prop, label, minWidth: 110, type: 'flag' }
}

function formatCell(value, column) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (column.type === 'amount') {
    return formatAmount(value)
  }
  if (column.type === 'integer') {
    return formatInteger(value)
  }
  if (column.type === 'ratio') {
    return `${(toNumber(value) * 100).toFixed(2)}%`
  }
  if (column.type === 'flag') {
    return Number(value) === 1 ? '是' : '否'
  }
  return value
}

function formatAmount(value) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0
  }).format(toNumber(value))
}

function formatInteger(value) {
  return new Intl.NumberFormat('zh-CN').format(toNumber(value))
}

function formatPercent(value) {
  return `${(toNumber(value) * 100).toFixed(2)}%`
}

function riskLevelClass(level) {
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

function externalStatusTagType(status) {
  if (status === 'AVAILABLE') return 'success'
  if (status === 'UNAVAILABLE') return 'warning'
  if (status === 'NOT_ENABLED') return 'info'
  return ''
}

function displayValue(value) {
  if (value === null || value === undefined || value === '') return '-'
  return formatInteger(value)
}

function toNumber(value) {
  const number = Number(value || 0)
  return Number.isFinite(number) ? number : 0
}
</script>

<style scoped>
.credit-query-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.query-page-header,
.query-header-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.query-page-header h2 {
  margin: 0;
  color: #1f2937;
  font-size: 19px;
  font-weight: 650;
}

.query-page-header p,
.query-header-meta span {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
}

.query-header-meta {
  justify-content: flex-end;
  flex-wrap: wrap;
}

.query-tabs {
  padding: 0 4px;
  border-bottom: 1px solid #dce4ee;
}

.query-command-panel {
  padding: 14px;
  border: 1px solid #dce4ee;
  border-radius: 8px;
  background: #fff;
}

.credit-query-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.keyword-input {
  width: min(300px, 100%);
}

.date-range {
  width: 300px;
}

.small-input {
  width: 150px;
}

.mini-input {
  width: 100px;
}

.query-summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.query-summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 56px;
  padding: 14px 16px;
  border: 1px solid #dce4ee;
  border-top: 3px solid #9ab7cf;
  border-radius: 8px;
  background: #fff;
}

.query-summary-item span {
  color: #64748b;
  font-size: 13px;
}

.query-summary-item strong {
  color: #1f2937;
  font-size: 18px;
}

.query-summary-item:nth-child(2) { border-top-color: #1f5e93; }
.query-summary-item:nth-child(3) { border-top-color: #087a65; }
.query-summary-item:nth-child(4) { border-top-color: #b45309; }

.query-panel {
  min-width: 0;
  padding: 0;
  overflow: hidden;
}

.query-panel :deep(.el-table__body tr) { cursor: pointer; }
.query-panel :deep(.el-table__body tr:hover > td) { background: #f2f7fb !important; }

.ai-search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.ai-search-options {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.ai-result {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 16px;
}

.ai-risk-head {
  display: grid;
  grid-template-columns: minmax(180px, 0.38fr) minmax(0, 0.62fr);
  gap: 18px;
  align-items: center;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.ai-caption {
  color: #64748b;
  font-size: 13px;
}

.ai-risk-level {
  margin-top: 6px;
  font-size: 28px;
  font-weight: 800;
}

.risk-extreme,
.risk-high {
  color: #dc2626;
}

.risk-medium {
  color: #d97706;
}

.risk-low {
  color: #0f766e;
}

.ai-score-block {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.ai-score {
  color: #111827;
  font-size: 36px;
  font-weight: 800;
  text-align: right;
}

.ai-conclusion {
  margin: 0;
  color: #334155;
  line-height: 1.7;
}

.ai-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ai-external-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border: 1px solid #c7d2fe;
  border-radius: 8px;
  background: #f8faff;
}

.ai-external-head,
.ai-external-signals {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  color: #475569;
  font-size: 13px;
}

.ai-external-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.ai-external-grid div {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-height: 62px;
  padding: 10px 12px;
  border: 1px solid #dbe4ff;
  border-radius: 6px;
  background: #fff;
}

.ai-external-grid span {
  color: #64748b;
  font-size: 12px;
}

.ai-external-grid strong {
  color: #1e3a8a;
  font-size: 18px;
}

.ai-metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.ai-metric {
  display: grid;
  gap: 6px;
  min-height: 70px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.ai-metric span {
  color: #64748b;
  font-size: 12px;
}

.ai-metric strong {
  color: #111827;
  font-size: 17px;
}

.ai-two-column {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.ai-section {
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.ai-section h3 {
  margin: 0 0 10px;
  font-size: 16px;
}

.ai-section ul {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
  color: #334155;
  line-height: 1.6;
}

.ai-detail-tabs {
  margin-top: 2px;
}

.ai-side-tables {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 1100px) {
  .query-summary-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .keyword-input,
  .date-range,
  .small-input,
  .mini-input {
    width: 100%;
  }

  .ai-metric-grid,
  .ai-two-column,
  .ai-side-tables {
    grid-template-columns: 1fr;
  }

  .ai-external-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .query-page-header { align-items: flex-start; flex-direction: column; }
  .query-header-meta { justify-content: flex-start; }
  .query-summary-row {
    grid-template-columns: 1fr;
  }

  .ai-search-row,
  .ai-risk-head,
  .ai-score-block,
  .ai-external-grid {
    grid-template-columns: 1fr;
  }

  .ai-score {
    text-align: left;
  }
}
</style>
