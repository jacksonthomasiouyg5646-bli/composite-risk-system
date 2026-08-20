<template>
  <section class="report-page">
    <div class="page-toolbar">
      <div><h2>管理报表</h2><p>从机构、行业、产品和风险迁移四个视角汇总组合风险。</p></div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <section class="summary-grid">
      <div v-for="item in summaries" :key="item.label" class="summary-card" :class="item.tone"><span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.note }}</small></div>
    </section>

    <section class="report-panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="机构" name="organization">
          <el-table v-loading="loading" :data="report.organization || []" border max-height="430">
            <el-table-column prop="owner_org_name" label="机构" min-width="180" show-overflow-tooltip />
            <el-table-column prop="customer_count" label="客户数" width="100" align="right" />
            <el-table-column prop="warning_customer_count" label="预警客户" width="110" align="right" />
            <el-table-column prop="high_risk_count" label="高风险" width="100" align="right" />
            <el-table-column label="预警敞口" min-width="140" align="right"><template #default="{ row }">{{ money(row.warning_ead_amount) }}</template></el-table-column>
            <el-table-column prop="forecast_upgrade_count" label="预测上迁" width="110" align="right" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="行业" name="industry">
          <el-table v-loading="loading" :data="report.industry || []" border max-height="430">
            <el-table-column prop="industry_name" label="行业" min-width="170" show-overflow-tooltip />
            <el-table-column prop="customer_count" label="客户数" width="100" align="right" />
            <el-table-column prop="warning_customer_count" label="预警客户" width="110" align="right" />
            <el-table-column prop="high_risk_count" label="高风险" width="100" align="right" />
            <el-table-column label="风险敞口" min-width="140" align="right"><template #default="{ row }">{{ money(row.ead_amount_total) }}</template></el-table-column>
            <el-table-column label="行业集中度" width="120" align="right"><template #default="{ row }">{{ percent(row.concentration_rate) }}</template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="产品" name="product">
          <el-table v-loading="loading" :data="report.product || []" border max-height="430">
            <el-table-column prop="product_type" label="产品" min-width="160" show-overflow-tooltip />
            <el-table-column prop="drawdown_count" label="支用笔数" width="110" align="right" />
            <el-table-column label="未结本金" min-width="145" align="right"><template #default="{ row }">{{ money(row.outstanding_amount) }}</template></el-table-column>
            <el-table-column prop="overdue_count" label="逾期笔数" width="110" align="right" />
            <el-table-column prop="debt_default_count" label="债项违约" width="110" align="right" />
            <el-table-column label="违约敞口" min-width="145" align="right"><template #default="{ row }">{{ money(row.default_exposure_amount) }}</template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="风险迁移" name="migration">
          <div class="migration-grid">
            <div v-for="item in migrations" :key="item.label" class="migration-item" :class="item.tone"><span>{{ item.label }}</span><strong>{{ item.value }}</strong></div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import http from '../api/http'

const loading = ref(false)
const activeTab = ref('organization')
const report = ref({ summary: {} })
const summaries = computed(() => {
  const summary = report.value.summary || {}
  return [
    { label: '客户总数', value: number(summary.customer_total), note: '纳入组合评分', tone: 'blue' },
    { label: '预警客户', value: number(summary.warning_customer_count), note: '评分不低于 45', tone: 'warning' },
    { label: '高风险客户', value: number(summary.high_risk_count), note: `极高 ${number(summary.extreme_risk_count)}`, tone: 'danger' },
    { label: '预测上迁', value: number(summary.forecast_upgrade_count), note: '未来 30 天', tone: 'warning' },
    { label: '逾期笔数', value: number(summary.overdue_count), note: '当前有效逾期', tone: 'slate' },
    { label: '债项违约', value: number(summary.debt_default_count), note: '当前有效违约', tone: 'danger' }
  ]
})
const migrations = computed(() => {
  const source = report.value.risk_migration || {}
  return [
    { label: '风险上迁', value: number(source.upgrade_count), tone: 'danger' },
    { label: '风险稳定', value: number(source.stable_count), tone: 'blue' },
    { label: '风险下迁', value: number(source.downgrade_count), tone: 'green' }
  ]
})

onMounted(load)
async function load() {
  loading.value = true
  try { report.value = await http.get('/api/risks/management-reports') } finally { loading.value = false }
}
function number(value) { return Number(value || 0) }
function money(value) { return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
function percent(value) { return `${(Number(value || 0) * 100).toFixed(2)}%` }
</script>

<style scoped>
.report-page { display: grid; gap: 16px; }.page-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }h2, p { margin: 0; }h2 { color: #1f2937; font-size: 18px; font-weight: 650; }.page-toolbar p { color: #64748b; font-size: 13px; margin-top: 5px; }
.summary-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 10px; }.summary-card, .report-panel { border: 1px solid #dce3ec; border-radius: 8px; background: #fff; }.summary-card { display: grid; gap: 5px; padding: 13px; min-width: 0; }.summary-card span, .summary-card small { color: #64748b; font-size: 12px; }.summary-card strong { color: #1f2937; font-size: 22px; }.summary-card.danger strong { color: #b91c1c; }.summary-card.warning strong { color: #b45309; }.summary-card.blue strong { color: #1d4f7e; }
.report-panel { padding: 0 16px 14px; }.migration-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; padding: 14px 0; }.migration-item { display: grid; gap: 6px; padding: 18px; border-left: 4px solid #6b8fb2; background: #f8fafc; }.migration-item span { color: #64748b; font-size: 13px; }.migration-item strong { color: #1f2937; font-size: 28px; }.migration-item.danger { border-color: #dc2626; }.migration-item.danger strong { color: #b91c1c; }.migration-item.green { border-color: #059669; }.migration-item.green strong { color: #047857; }
@media (max-width: 1100px) { .summary-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }@media (max-width: 680px) { .page-toolbar { align-items: flex-start; flex-direction: column; }.summary-grid, .migration-grid { grid-template-columns: 1fr; }.report-panel { padding: 0 10px 10px; } }
</style>
