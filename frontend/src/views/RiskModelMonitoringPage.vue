<template>
  <section class="monitor-page">
    <div class="page-toolbar">
      <div>
        <h2>模型监控</h2>
        <p>监控组合评分模型运行状态、效果指标、稳定性和外部数据接入情况。</p>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Camera" :loading="capturing" @click="capture">生成快照</el-button>
      </div>
    </div>

    <section v-loading="loading" class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-card" :class="item.tone">
        <span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.note }}</small>
      </div>
    </section>

    <section class="effect-grid">
      <article class="panel">
        <div class="panel-title"><h3>模型效果</h3><el-tag :type="stabilityType(effect.stability_status)" effect="plain">{{ stabilityLabel(effect.stability_status) }}</el-tag></div>
        <div class="effect-metrics">
          <div><span>AUC</span><strong>{{ decimal(effect.auc_value) }}</strong><small>排序区分能力</small></div>
          <div><span>KS</span><strong>{{ decimal(effect.ks_value) }}</strong><small>高低风险分离度</small></div>
          <div><span>PSI</span><strong>{{ decimal(effect.psi_value) }}</strong><small>评分分布稳定性</small></div>
          <div><span>召回率</span><strong>{{ percent(effect.recall_rate) }}</strong><small>风险客户覆盖</small></div>
          <div><span>误报率</span><strong>{{ percent(effect.false_alarm_rate) }}</strong><small>处置资源占用</small></div>
        </div>
      </article>
      <article class="panel">
        <div class="panel-title"><h3>混淆矩阵</h3><span>基于当前预警和逾期/违约样本的模拟回看</span></div>
        <div class="confusion">
          <div><span>样本总量</span><strong>{{ number(effect.confusion_matrix?.sample_total) }}</strong></div>
          <div><span>预测风险</span><strong>{{ number(effect.confusion_matrix?.predicted_positive) }}</strong></div>
          <div><span>实际风险</span><strong>{{ number(effect.confusion_matrix?.observed_positive) }}</strong></div>
          <div><span>命中风险</span><strong>{{ number(effect.confusion_matrix?.true_positive) }}</strong></div>
        </div>
        <ul class="interpretation">
          <li v-for="item in effect.business_interpretation || []" :key="item">{{ item }}</li>
        </ul>
      </article>
    </section>

    <section class="monitor-grid">
      <article class="panel">
        <div class="panel-title"><h3>外部数据提供方</h3><el-tag :type="statusType(external.connection_status)" effect="plain">{{ statusLabel(external.connection_status) }}</el-tag></div>
        <dl>
          <div><dt>提供方</dt><dd>{{ external.provider_name || '-' }}</dd></div>
          <div><dt>接入开关</dt><dd>{{ external.enabled ? '已启用' : '未启用' }}</dd></div>
          <div><dt>今日请求</dt><dd>{{ number(external.today?.query_count_today) }}</dd></div>
          <div><dt>可用响应</dt><dd>{{ number(external.today?.available_count_today) }}</dd></div>
          <div><dt>不可用响应</dt><dd>{{ number(external.today?.unavailable_count_today) }}</dd></div>
          <div><dt>最近访问</dt><dd>{{ formatDate(external.latest?.requested_at) }}</dd></div>
        </dl>
      </article>
      <article class="panel">
        <div class="panel-title"><h3>运行分布</h3><span>{{ current.model_name || '-' }}</span></div>
        <div class="risk-bars">
          <div v-for="item in distribution" :key="item.label"><span>{{ item.label }}</span><el-progress :percentage="item.percent" :stroke-width="10" :color="item.color" :show-text="false" /><strong>{{ item.value }}</strong></div>
        </div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-title"><h3>每日监控快照</h3><span>保留最近 30 条</span></div>
      <el-table :data="snapshots" size="small" border max-height="360">
        <el-table-column prop="snapshot_date" label="日期" min-width="110" />
        <el-table-column prop="model_name" label="模型" min-width="210" show-overflow-tooltip />
        <el-table-column prop="customer_total" label="客户" width="80" align="right" />
        <el-table-column label="平均评分" width="100" align="right"><template #default="{ row }">{{ decimal(row.average_risk_score) }}</template></el-table-column>
        <el-table-column prop="auc_value" label="AUC" width="82" align="right" />
        <el-table-column prop="ks_value" label="KS" width="82" align="right" />
        <el-table-column prop="psi_value" label="PSI" width="82" align="right" />
        <el-table-column prop="stability_status" label="稳定性" width="100" />
        <el-table-column prop="external_query_count" label="外部查询" width="100" align="right" />
      </el-table>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, Refresh } from '@element-plus/icons-vue'
import http from '../api/http'

const loading = ref(false)
const capturing = ref(false)
const current = ref({})
const external = ref({ today: {} })
const effect = ref({ confusion_matrix: {}, business_interpretation: [] })
const snapshots = ref([])
const metrics = computed(() => [
  { label: '模型规则', value: number(current.value.rule_count), note: current.value.model_name || '-', tone: 'blue' },
  { label: '纳入客户', value: number(current.value.customer_total), note: '动态组合评分', tone: 'slate' },
  { label: '平均评分', value: decimal(current.value.average_risk_score), note: '满分 100', tone: 'blue' },
  { label: '高风险客户', value: number(current.value.high_risk_count), note: `极高 ${number(current.value.extreme_risk_count)}`, tone: 'danger' },
  { label: '预警客户', value: number(current.value.warning_customer_count), note: '评分不低于 45', tone: 'warning' },
  { label: '预测上迁', value: number(current.value.forecast_upgrade_count), note: '未来 30 天', tone: 'warning' }
])
const distribution = computed(() => {
  const total = Math.max(number(current.value.customer_total), 1)
  return [
    { label: '极高风险', value: number(current.value.extreme_risk_count), percent: Math.round(number(current.value.extreme_risk_count) / total * 100), color: '#c2410c' },
    { label: '高风险', value: number(current.value.high_risk_count), percent: Math.round(number(current.value.high_risk_count) / total * 100), color: '#dc2626' },
    { label: '预警客户', value: number(current.value.warning_customer_count), percent: Math.round(number(current.value.warning_customer_count) / total * 100), color: '#d97706' },
    { label: '预测上迁', value: number(current.value.forecast_upgrade_count), percent: Math.round(number(current.value.forecast_upgrade_count) / total * 100), color: '#7c3aed' }
  ]
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const data = await http.get('/api/risks/model-monitoring')
    current.value = data.current || {}
    external.value = data.external_data || { today: {} }
    effect.value = data.effect_metrics || { confusion_matrix: {}, business_interpretation: [] }
    snapshots.value = data.snapshots || []
  } finally { loading.value = false }
}

async function capture() {
  capturing.value = true
  try {
    await http.post('/api/risks/model-monitoring/snapshot')
    ElMessage.success('监控快照已生成')
    await load()
  } finally { capturing.value = false }
}

function number(value) { return Number(value || 0) }
function decimal(value) { return Number(value || 0).toFixed(2) }
function percent(value) { return `${(Number(value || 0) * 100).toFixed(1)}%` }
function statusLabel(value) { return ({ AVAILABLE: '可用', UNAVAILABLE: '不可用', NOT_ENABLED: '未启用', NO_REQUEST: '待请求', LOCAL_ONLY: '内部数据' })[value] || value || '未知' }
function statusType(value) { return ({ AVAILABLE: 'success', NOT_ENABLED: 'info', NO_REQUEST: 'info', LOCAL_ONLY: 'warning', UNAVAILABLE: 'danger' })[value] || 'info' }
function stabilityLabel(value) { return ({ STABLE: '稳定', WATCH: '关注', DRIFT: '漂移', UNKNOWN: '未知' })[value] || value || '未知' }
function stabilityType(value) { return ({ STABLE: 'success', WATCH: 'warning', DRIFT: 'danger' })[value] || 'info' }
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 19) : '-' }
</script>

<style scoped>
.monitor-page { display: grid; gap: 16px; }
.page-toolbar, .toolbar-actions, .panel-title { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
h2, h3, p { margin: 0; }
h2 { color: #1f2937; font-size: 18px; font-weight: 650; }
.page-toolbar p { color: #64748b; font-size: 13px; margin-top: 5px; }
.metric-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 10px; }
.metric-card, .panel { border: 1px solid #dce3ec; border-radius: 8px; background: #fff; }
.metric-card { display: grid; gap: 5px; padding: 13px; min-width: 0; }
.metric-card span, .metric-card small, .panel-title > span { color: #64748b; font-size: 12px; }
.metric-card strong { color: #1f2937; font-size: 22px; }
.metric-card.danger strong { color: #b91c1c; }
.metric-card.warning strong { color: #b45309; }
.metric-card.blue strong { color: #1d4f7e; }
.panel { padding: 16px; }
.panel-title { margin-bottom: 13px; }
.panel-title h3 { color: #334155; font-size: 15px; }
.effect-grid, .monitor-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.effect-metrics, .confusion { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 8px; }
.confusion { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.effect-metrics div, .confusion div { display: grid; gap: 4px; padding: 10px; border-radius: 7px; background: #f8fafc; }
.effect-metrics span, .effect-metrics small, .confusion span { color: #64748b; font-size: 12px; }
.effect-metrics strong, .confusion strong { color: #1f2937; font-size: 20px; }
.interpretation { margin: 12px 0 0; padding-left: 18px; color: #64748b; font-size: 13px; line-height: 1.7; }
dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); margin: 14px 0 0; gap: 10px; }
dl > div { display: grid; gap: 3px; padding-bottom: 8px; border-bottom: 1px solid #edf1f5; }
dt { color: #64748b; font-size: 12px; }
dd { margin: 0; color: #1f2937; font-size: 14px; overflow-wrap: anywhere; }
.risk-bars { display: grid; gap: 16px; margin-top: 18px; }
.risk-bars > div { display: grid; grid-template-columns: 74px minmax(80px, 1fr) 34px; gap: 10px; align-items: center; }
.risk-bars span, .risk-bars strong { color: #475569; font-size: 13px; }
.risk-bars strong { text-align: right; }
@media (max-width: 1100px) { .metric-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } .effect-metrics { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 760px) { .page-toolbar { align-items: flex-start; flex-direction: column; } .monitor-grid, .effect-grid { grid-template-columns: 1fr; } .metric-grid, .effect-metrics, .confusion { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
