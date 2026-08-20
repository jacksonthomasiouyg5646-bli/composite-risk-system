<template>
  <section class="default-trend-page">
    <div class="trend-toolbar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        class="date-range"
      />
      <el-input
        v-model="keyword"
        clearable
        :prefix-icon="Search"
        placeholder="客户号、客户名、合同号、债项号"
        class="keyword-input"
        @keyup.enter="loadTrend"
      />
      <el-select v-model="defaultLevel" clearable placeholder="违约等级" class="level-select">
        <el-option label="A：逾期90天以上" value="A" />
        <el-option label="B：五级分类后三类" value="B" />
        <el-option label="C：评级21级" value="C" />
      </el-select>
      <el-button type="primary" :icon="Search" :loading="loading" @click="loadTrend">查询</el-button>
      <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
    </div>

    <div class="metric-row trend-metrics">
      <div v-for="item in metrics" :key="item.label" class="metric-card">
        <div class="metric-label">{{ item.label }}</div>
        <div class="metric-value">{{ item.value }}</div>
        <div class="metric-sub">{{ item.sub }}</div>
      </div>
    </div>

    <div class="trend-grid">
      <section class="panel chart-panel">
        <div class="panel-title-row">
          <h2>每日违约笔数</h2>
          <span class="legend-item"><i class="legend-bar"></i>债项违约</span>
        </div>
        <svg class="trend-chart" :viewBox="`0 0 ${chart.width} ${chart.height}`" role="img" aria-label="每日违约笔数柱状图">
          <g v-for="tick in countTicks" :key="`bar-${tick.value}`">
            <line :x1="chart.left" :x2="chart.width - chart.right" :y1="tick.y" :y2="tick.y" class="grid-line" />
            <text :x="chart.left - 10" :y="tick.y + 4" class="axis-label" text-anchor="end">{{ tick.value }}</text>
          </g>
          <line :x1="chart.left" :x2="chart.width - chart.right" :y1="chartBaseY" :y2="chartBaseY" class="axis-line" />
          <rect
            v-for="bar in debtDefaultBars"
            :key="bar.date"
            :x="bar.x"
            :y="bar.y"
            :width="bar.width"
            :height="bar.height"
            rx="3"
            class="debt-bar"
          >
            <title>{{ bar.date }}：{{ bar.value }} 笔</title>
          </rect>
          <text v-for="label in xLabels" :key="`bar-label-${label.date}`" :x="label.x" :y="chart.height - 14" class="axis-label" text-anchor="middle">
            {{ label.text }}
          </text>
        </svg>
      </section>

      <section class="panel chart-panel">
        <div class="panel-title-row">
          <h2>违约与逾期走势</h2>
          <div class="chart-legend">
            <span class="legend-item"><i class="legend-line debt"></i>债项违约</span>
            <span class="legend-item"><i class="legend-line customer"></i>客户违约</span>
            <span class="legend-item"><i class="legend-line overdue"></i>逾期</span>
          </div>
        </div>
        <svg class="trend-chart" :viewBox="`0 0 ${chart.width} ${chart.height}`" role="img" aria-label="违约与逾期走势折线图">
          <g v-for="tick in countTicks" :key="`line-${tick.value}`">
            <line :x1="chart.left" :x2="chart.width - chart.right" :y1="tick.y" :y2="tick.y" class="grid-line" />
            <text :x="chart.left - 10" :y="tick.y + 4" class="axis-label" text-anchor="end">{{ tick.value }}</text>
          </g>
          <line :x1="chart.left" :x2="chart.width - chart.right" :y1="chartBaseY" :y2="chartBaseY" class="axis-line" />
          <polyline :points="linePoints('debt_default_count')" class="trend-line debt" />
          <polyline :points="linePoints('customer_default_count')" class="trend-line customer" />
          <polyline :points="linePoints('overdue_count')" class="trend-line overdue" />
          <g v-for="point in linePointMarkers('debt_default_count')" :key="`debt-${point.date}`">
            <circle :cx="point.x" :cy="point.y" r="3.8" class="point debt" />
            <title>{{ point.date }}：债项违约 {{ point.value }} 笔</title>
          </g>
          <g v-for="point in linePointMarkers('customer_default_count')" :key="`customer-${point.date}`">
            <circle :cx="point.x" :cy="point.y" r="3.8" class="point customer" />
            <title>{{ point.date }}：客户违约 {{ point.value }} 户</title>
          </g>
          <g v-for="point in linePointMarkers('overdue_count')" :key="`overdue-${point.date}`">
            <circle :cx="point.x" :cy="point.y" r="3.8" class="point overdue" />
            <title>{{ point.date }}：逾期 {{ point.value }} 笔</title>
          </g>
          <text v-for="label in xLabels" :key="`line-label-${label.date}`" :x="label.x" :y="chart.height - 14" class="axis-label" text-anchor="middle">
            {{ label.text }}
          </text>
        </svg>
      </section>
    </div>

    <div class="trend-grid lower-grid">
      <section class="panel">
        <h2>违约等级分布</h2>
        <div class="level-bars">
          <div v-for="item in levelDistribution" :key="item.default_level" class="level-row">
            <div class="level-name">{{ item.default_level }}</div>
            <div class="level-track">
              <div class="level-fill" :class="`level-${item.default_level}`" :style="{ width: `${item.percent}%` }"></div>
            </div>
            <div class="level-count">{{ item.debt_default_count }} 笔</div>
          </div>
        </div>
      </section>

      <section class="panel">
        <h2>最近违约明细</h2>
        <el-table v-loading="loading" :data="recentDefaults" height="300" border>
          <el-table-column prop="default_date" label="违约日期" width="112" />
          <el-table-column prop="customer_no" label="客户编号" min-width="140" show-overflow-tooltip />
          <el-table-column prop="customer_name_snapshot" label="客户名称" min-width="170" show-overflow-tooltip />
          <el-table-column prop="contract_no" label="合同编号" min-width="140" show-overflow-tooltip />
          <el-table-column prop="debt_no" label="债项编号" min-width="140" show-overflow-tooltip />
          <el-table-column prop="default_level" label="等级" width="76" />
          <el-table-column prop="overdue_days" label="逾期天数" width="96" />
          <el-table-column label="违约敞口" width="128" align="right">
            <template #default="{ row }">{{ formatAmount(row.default_exposure_amount) }}</template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import http from '../api/http'

const chart = {
  width: 920,
  height: 320,
  left: 52,
  right: 26,
  top: 24,
  bottom: 42
}

const dateRange = ref([])
const keyword = ref('')
const defaultLevel = ref('')
const loading = ref(false)
const trend = ref({
  filters: {},
  summary: {},
  daily: [],
  level_distribution: [],
  recent_defaults: []
})

const chartBaseY = computed(() => chart.height - chart.bottom)
const plotWidth = computed(() => chart.width - chart.left - chart.right)
const plotHeight = computed(() => chart.height - chart.top - chart.bottom)
const dailyRows = computed(() => trend.value.daily || [])
const recentDefaults = computed(() => trend.value.recent_defaults || [])
const maxCount = computed(() => {
  const values = dailyRows.value.flatMap((row) => [
    toNumber(row.debt_default_count),
    toNumber(row.customer_default_count),
    toNumber(row.overdue_count)
  ])
  return Math.max(1, ...values)
})
const countMax = computed(() => Math.max(1, Math.ceil(maxCount.value / 5) * 5))
const countTicks = computed(() => [0, Math.ceil(countMax.value / 2), countMax.value].map((value) => ({
  value,
  y: yForValue(value)
})))
const debtDefaultBars = computed(() => {
  const rows = dailyRows.value
  const slot = plotWidth.value / Math.max(rows.length, 1)
  const width = Math.min(18, Math.max(4, slot * 0.5))
  return rows.map((row, index) => {
    const value = toNumber(row.debt_default_count)
    const y = yForValue(value)
    return {
      date: row.stat_date,
      value,
      x: chart.left + index * slot + slot / 2 - width / 2,
      y,
      width,
      height: Math.max(0, chartBaseY.value - y)
    }
  })
})
const xLabels = computed(() => {
  const rows = dailyRows.value
  const every = Math.max(1, Math.ceil(rows.length / 8))
  return rows
    .map((row, index) => {
      if (index % every !== 0 && index !== rows.length - 1) {
        return null
      }
      return {
        date: row.stat_date,
        text: String(row.stat_date).slice(5),
        x: xForIndex(index, rows.length)
      }
    })
    .filter(Boolean)
})
const metrics = computed(() => {
  const summary = trend.value.summary || {}
  return [
    { label: '债项违约笔数', value: formatInteger(summary.debt_default_total), sub: `${filterRangeText.value}` },
    { label: '违约客户数', value: formatInteger(summary.customer_default_total), sub: `最高等级 ${summary.highest_default_level || '-'}` },
    { label: '逾期笔数', value: formatInteger(summary.overdue_total), sub: `最大逾期 ${formatInteger(summary.max_overdue_days)} 天` },
    { label: '违约敞口金额', value: formatAmount(summary.default_exposure_total), sub: `单日峰值 ${formatInteger(summary.max_daily_debt_default_count)} 笔` }
  ]
})
const filterRangeText = computed(() => {
  const filters = trend.value.filters || {}
  if (!filters.start_date || !filters.end_date) {
    return '当前范围'
  }
  return `${filters.start_date} 至 ${filters.end_date}`
})
const levelDistribution = computed(() => {
  const rows = trend.value.level_distribution || []
  const max = Math.max(1, ...rows.map((row) => toNumber(row.debt_default_count)))
  return rows.map((row) => ({
    ...row,
    debt_default_count: toNumber(row.debt_default_count),
    percent: Math.max(4, Math.round((toNumber(row.debt_default_count) / max) * 100))
  }))
})

onMounted(loadTrend)

async function loadTrend() {
  loading.value = true
  try {
    const params = {}
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    if (keyword.value.trim()) {
      params.keyword = keyword.value.trim()
    }
    if (defaultLevel.value) {
      params.defaultLevel = defaultLevel.value
    }

    const data = await http.get('/api/risks/default-trends', { params })
    trend.value = {
      filters: data.filters || {},
      summary: data.summary || {},
      daily: data.daily || [],
      level_distribution: data.level_distribution || [],
      recent_defaults: data.recent_defaults || []
    }
    if (!dateRange.value?.length && trend.value.filters.start_date && trend.value.filters.end_date) {
      dateRange.value = [trend.value.filters.start_date, trend.value.filters.end_date]
    }
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  dateRange.value = []
  keyword.value = ''
  defaultLevel.value = ''
  loadTrend()
}

function linePoints(key) {
  return linePointMarkers(key).map((point) => `${point.x},${point.y}`).join(' ')
}

function linePointMarkers(key) {
  const rows = dailyRows.value
  return rows.map((row, index) => {
    const value = toNumber(row[key])
    return {
      date: row.stat_date,
      value,
      x: xForIndex(index, rows.length),
      y: yForValue(value)
    }
  })
}

function xForIndex(index, total) {
  if (total <= 1) {
    return chart.left + plotWidth.value / 2
  }
  return chart.left + (plotWidth.value * index) / (total - 1)
}

function yForValue(value) {
  return chart.top + ((countMax.value - toNumber(value)) / countMax.value) * plotHeight.value
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
</script>

<style scoped>
.default-trend-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.trend-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 0;
}

.date-range {
  width: 300px;
}

.keyword-input {
  width: min(360px, 100%);
}

.level-select {
  width: 172px;
}

.trend-metrics {
  margin-bottom: 0;
}

.metric-sub {
  margin-top: 8px;
  min-height: 18px;
  color: #64748b;
  font-size: 12px;
}

.trend-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.lower-grid {
  grid-template-columns: minmax(320px, 0.52fr) minmax(0, 1.48fr);
}

.chart-panel {
  min-width: 0;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.panel-title-row h2,
.panel h2 {
  margin: 0 0 12px;
  font-size: 18px;
}

.panel-title-row h2 {
  margin-bottom: 0;
}

.chart-legend {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-size: 12px;
  white-space: nowrap;
}

.legend-bar {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  background: #0f766e;
}

.legend-line {
  width: 18px;
  height: 3px;
  border-radius: 999px;
}

.legend-line.debt {
  background: #0f766e;
}

.trend-line.debt {
  stroke: #0f766e;
}

.point.debt {
  fill: #0f766e;
}

.legend-line.customer {
  background: #2563eb;
}

.trend-line.customer {
  stroke: #2563eb;
}

.point.customer {
  fill: #2563eb;
}

.legend-line.overdue {
  background: #dc2626;
}

.trend-line.overdue {
  stroke: #dc2626;
}

.point.overdue {
  fill: #dc2626;
}

.trend-chart {
  display: block;
  width: 100%;
  height: 320px;
}

.grid-line {
  stroke: #e5e7eb;
  stroke-width: 1;
}

.axis-line {
  stroke: #94a3b8;
  stroke-width: 1.2;
}

.axis-label {
  fill: #64748b;
  font-size: 12px;
}

.debt-bar {
  fill: #0f766e;
}

.trend-line {
  fill: none;
  stroke-width: 2.6;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.point {
  stroke: #fff;
  stroke-width: 1.5;
}

.level-bars {
  display: grid;
  gap: 18px;
  padding: 12px 0 6px;
}

.level-row {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 60px;
  gap: 10px;
  align-items: center;
}

.level-name {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #0f172a;
  font-weight: 700;
}

.level-track {
  height: 14px;
  overflow: hidden;
  border-radius: 999px;
  background: #e5e7eb;
}

.level-fill {
  height: 100%;
  border-radius: inherit;
}

.level-A {
  background: #dc2626;
}

.level-B {
  background: #f59e0b;
}

.level-C {
  background: #2563eb;
}

.level-count {
  color: #475569;
  font-size: 13px;
  text-align: right;
}

@media (max-width: 1100px) {
  .trend-grid,
  .lower-grid,
  .trend-metrics {
    grid-template-columns: 1fr;
  }

  .date-range,
  .keyword-input,
  .level-select {
    width: 100%;
  }
}
</style>
