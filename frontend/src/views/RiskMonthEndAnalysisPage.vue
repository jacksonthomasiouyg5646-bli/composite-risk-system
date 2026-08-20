<template>
  <section class="month-page">
    <header class="page-toolbar">
      <div>
        <h2>月末组合变动分析</h2>
        <p>基于已发布月末快照，解释组合规模、风险参数、迁徙、逾期与违约变化。</p>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Calendar" :loading="capturing" @click="batchDialog = true">生产批次加工</el-button>
      </div>
    </header>

    <section class="compare-bar">
      <div>
        <span>当前月末</span>
        <el-select v-model="currentMonth" @change="changeCurrent">
          <el-option v-for="item in months" :key="'c'+item.id" :label="monthLabel(item.month_end_date)" :value="item.month_end_date" />
        </el-select>
      </div>
      <div class="compare-arrow">对比</div>
      <div>
        <span>基准月末</span>
        <el-select v-model="baseMonth" @change="load">
          <el-option v-for="item in baseMonthOptions" :key="'b'+item.id" :label="monthLabel(item.month_end_date)" :value="item.month_end_date" />
        </el-select>
      </div>
      <div class="batch-meta">
        <el-tag type="success" effect="plain">已发布并锁定</el-tag>
        <span>批次 {{ data.current_batch?.batch_no || '-' }}</span>
        <span>源数据 {{ date(data.current_batch?.source_data_date) }}</span>
        <span>质量 {{ number(data.current_batch?.quality_score) }} 分</span>
      </div>
    </section>

    <section class="metric-strip" v-loading="loading">
      <div><span>组合 EAD</span><strong>{{ amount(overview.current_ead) }}</strong><small :class="deltaClass(overview.ead_delta)">{{ signedAmount(overview.ead_delta) }} · {{ signedPercent(overview.ead_change_rate) }}</small></div>
      <div><span>衰退 EL</span><strong>{{ amount(overview.current_el_downturn) }}</strong><small :class="deltaClass(overview.el_downturn_delta)">{{ signedAmount(overview.el_downturn_delta) }}</small></div>
      <div><span>加权 PD</span><strong>{{ percent(overview.current_pd) }}</strong><small :class="deltaClass(overview.pd_delta)">{{ signedPoints(overview.pd_delta) }}</small></div>
      <div><span>衰退 LGD</span><strong>{{ percent(overview.current_lgd_downturn) }}</strong><small :class="deltaClass(overview.lgd_downturn_delta)">{{ signedPoints(overview.lgd_downturn_delta) }}</small></div>
      <div><span>高风险客户</span><strong>{{ integer(overview.current_high_risk_count) }}</strong><small :class="deltaClass(overview.high_risk_delta)">{{ signedInteger(overview.high_risk_delta) }} 户</small></div>
      <div><span>逾期 / 违约债项</span><strong>{{ integer(overview.current_overdue_count) }} / {{ integer(overview.current_default_count) }}</strong><small :class="deltaClass(Number(overview.overdue_delta || 0)+Number(overview.default_delta || 0))">{{ signedInteger(overview.overdue_delta) }} / {{ signedInteger(overview.default_delta) }}</small></div>
    </section>

    <el-tabs v-model="activeTab" class="analysis-tabs">
      <el-tab-pane label="月末总览" name="overview">
        <section class="grid-two">
          <article class="panel">
            <div class="panel-title"><div><h3>近 12 个月组合趋势</h3><span>EAD 与衰退 EL 均来自锁定快照</span></div></div>
            <div class="trend-list">
              <div v-for="item in data.monthly_trend || []" :key="item.month_end_date" class="trend-row">
                <span>{{ shortMonth(item.month_end_date) }}</span>
                <div class="bar-track"><div class="bar-fill" :style="{ width: trendWidth(item.ead_amount) }"></div></div>
                <strong>{{ compactAmount(item.ead_amount) }}</strong>
                <small>EL {{ compactAmount(item.el_downturn_amount) }}</small>
              </div>
            </div>
          </article>
          <article class="panel">
            <div class="panel-title">
              <div><h3>维度变化</h3><span>按 EAD 变化绝对值排序</span></div>
              <el-segmented v-model="dimension" :options="dimensionOptions" @change="load" />
            </div>
            <el-table :data="data.dimension_changes || []" size="small" max-height="330">
              <el-table-column prop="dimension_name" label="范围" min-width="145" show-overflow-tooltip />
              <el-table-column label="本月 EAD" min-width="105" align="right"><template #default="{ row }">{{ compactAmount(row.current_ead) }}</template></el-table-column>
              <el-table-column label="变化" min-width="105" align="right"><template #default="{ row }"><span :class="deltaClass(row.ead_delta)">{{ signedCompact(row.ead_delta) }}</span></template></el-table-column>
              <el-table-column label="集中度" width="86" align="right"><template #default="{ row }">{{ percent(row.current_concentration) }}</template></el-table-column>
            </el-table>
          </article>
        </section>
      </el-tab-pane>

      <el-tab-pane label="组合变动归因" name="attribution">
        <section class="grid-two">
          <article class="panel">
            <div class="panel-title"><div><h3>衰退 EL 变化归因</h3><span>规模、PD、LGD 与结构交叉效应</span></div></div>
            <div class="effect-list">
              <div v-for="item in data.el_attribution || []" :key="item.effect_code" class="effect-row">
                <strong>{{ item.effect_name }}</strong>
                <div class="effect-track"><div :class="['effect-fill', Number(item.amount) >= 0 ? 'increase' : 'decrease']" :style="{ width: effectWidth(item.amount) }"></div></div>
                <span :class="deltaClass(item.amount)">{{ signedAmount(item.amount) }}</span>
              </div>
            </div>
          </article>
          <article class="panel">
            <div class="panel-title"><div><h3>债项变化原因</h3><span>新增、退出、参数和风险状态变化</span></div></div>
            <el-table :data="data.change_reasons || []" size="small" max-height="330">
              <el-table-column label="变化类型" min-width="135"><template #default="{ row }"><el-tag size="small" :type="reasonType(row.change_type)" effect="plain">{{ reasonLabel(row.change_type) }}</el-tag></template></el-table-column>
              <el-table-column prop="item_count" label="债项" width="70" align="right" />
              <el-table-column prop="customer_count" label="客户" width="70" align="right" />
              <el-table-column label="EAD 影响" min-width="110" align="right"><template #default="{ row }"><span :class="deltaClass(row.ead_delta)">{{ signedCompact(row.ead_delta) }}</span></template></el-table-column>
              <el-table-column label="EL 影响" min-width="105" align="right"><template #default="{ row }"><span :class="deltaClass(row.el_delta)">{{ signedCompact(row.el_delta) }}</span></template></el-table-column>
            </el-table>
          </article>
        </section>
      </el-tab-pane>

      <el-tab-pane label="风险迁徙" name="migration">
        <section class="grid-two">
          <article class="panel">
            <div class="panel-title"><div><h3>风险等级迁徙矩阵</h3><span>行表示基准月，列表示当前月</span></div></div>
            <el-table :data="riskMatrixRows" size="small" border>
              <el-table-column prop="previous" label="基准 \ 当前" min-width="105" />
              <el-table-column v-for="level in riskLevels" :key="level" :label="riskLabel(level)" min-width="84" align="right">
                <template #default="{ row }"><strong :class="{ hot: Number(row[level] || 0) > 0 && row.previous !== level }">{{ integer(row[level]) }}</strong></template>
              </el-table-column>
            </el-table>
          </article>
          <article class="panel">
            <div class="panel-title"><div><h3>评级迁徙明细</h3><span>连续客户的前后评级变化</span></div></div>
            <el-table :data="data.rating_migration || []" size="small" max-height="330">
              <el-table-column prop="previous_level" label="基准评级" width="95" />
              <el-table-column prop="current_level" label="当前评级" width="95" />
              <el-table-column prop="customer_count" label="客户数" width="80" align="right" />
              <el-table-column label="当前 EAD" min-width="115" align="right"><template #default="{ row }">{{ compactAmount(row.current_ead) }}</template></el-table-column>
            </el-table>
          </article>
        </section>
      </el-tab-pane>

      <el-tab-pane label="逾期违约变化" name="default">
        <section class="default-strip">
          <div><span>逾期客户</span><strong>{{ integer(overdue.current_overdue_customer_count) }}</strong><small :class="deltaClass(overdue.overdue_customer_delta)">{{ signedInteger(overdue.overdue_customer_delta) }}</small></div>
          <div><span>逾期债项</span><strong>{{ integer(overdue.current_overdue_debt_count) }}</strong><small :class="deltaClass(overdue.overdue_debt_delta)">{{ signedInteger(overdue.overdue_debt_delta) }}</small></div>
          <div><span>违约客户</span><strong>{{ integer(overdue.current_default_customer_count) }}</strong><small :class="deltaClass(overdue.default_customer_delta)">{{ signedInteger(overdue.default_customer_delta) }}</small></div>
          <div><span>违约债项</span><strong>{{ integer(overdue.current_default_debt_count) }}</strong><small :class="deltaClass(overdue.default_debt_delta)">{{ signedInteger(overdue.default_debt_delta) }}</small></div>
          <div><span>最大逾期天数</span><strong>{{ integer(overdue.current_max_overdue_days) }}</strong><small>基准 {{ integer(overdue.base_max_overdue_days) }} 天</small></div>
        </section>
        <article class="panel">
          <div class="panel-title"><div><h3>新增违约、治愈与风险变化</h3><span>从变化明细中筛选风险状态变化</span></div></div>
          <el-table :data="defaultChanges" size="small" border max-height="390">
            <el-table-column prop="customer_no" label="客户编号" min-width="145" />
            <el-table-column prop="debt_no" label="债项编号" min-width="145" />
            <el-table-column label="变化" width="105"><template #default="{ row }"><el-tag size="small" :type="reasonType(row.change_type)" effect="plain">{{ reasonLabel(row.change_type) }}</el-tag></template></el-table-column>
            <el-table-column label="逾期天数" width="110" align="right"><template #default="{ row }">{{ integer(row.previous_overdue_days) }} → {{ integer(row.current_overdue_days) }}</template></el-table-column>
            <el-table-column label="风险等级" width="125"><template #default="{ row }">{{ riskLabel(row.previous_risk_level) }} → {{ riskLabel(row.current_risk_level) }}</template></el-table-column>
            <el-table-column label="EAD 变化" min-width="110" align="right"><template #default="{ row }"><span :class="deltaClass(row.ead_delta)">{{ signedCompact(row.ead_delta) }}</span></template></el-table-column>
          </el-table>
        </article>
      </el-tab-pane>

      <el-tab-pane label="集中度与限额变化" name="concentration">
        <article class="panel">
          <div class="panel-title"><div><h3>{{ dimensionLabel(dimension) }}集中度变化</h3><span>对比集中度、EAD、PD、LGD 与衰退 EL</span></div><el-segmented v-model="dimension" :options="dimensionOptions" @change="load" /></div>
          <el-table :data="data.dimension_changes || []" size="small" border max-height="470">
            <el-table-column prop="dimension_name" label="范围" min-width="160" fixed="left" />
            <el-table-column label="本月 EAD" min-width="115" align="right"><template #default="{ row }">{{ amount(row.current_ead) }}</template></el-table-column>
            <el-table-column label="EAD 变化" min-width="110" align="right"><template #default="{ row }"><span :class="deltaClass(row.ead_delta)">{{ signedCompact(row.ead_delta) }}</span></template></el-table-column>
            <el-table-column label="本月集中度" width="105" align="right"><template #default="{ row }">{{ percent(row.current_concentration) }}</template></el-table-column>
            <el-table-column label="集中度变化" width="110" align="right"><template #default="{ row }"><span :class="deltaClass(row.concentration_delta)">{{ signedPoints(row.concentration_delta) }}</span></template></el-table-column>
            <el-table-column label="PD 变化" width="95" align="right"><template #default="{ row }"><span :class="deltaClass(row.pd_delta)">{{ signedPoints(row.pd_delta) }}</span></template></el-table-column>
            <el-table-column label="LGD 变化" width="95" align="right"><template #default="{ row }"><span :class="deltaClass(row.lgd_delta)">{{ signedPoints(row.lgd_delta) }}</span></template></el-table-column>
            <el-table-column label="EL 变化" min-width="105" align="right"><template #default="{ row }"><span :class="deltaClass(row.el_delta)">{{ signedCompact(row.el_delta) }}</span></template></el-table-column>
          </el-table>
        </article>
      </el-tab-pane>

      <el-tab-pane label="变化逐级下钻" name="drilldown">
        <article class="panel">
          <div class="panel-title">
            <div><h3>组合 → 行业 → 客户 → 合同 → 债项</h3><span>点击名称进入下一级，所有金额变化均可追溯至具体债项</span></div>
            <div class="drill-actions"><el-button v-if="drillPath.length" size="small" @click="drillBack">返回上一级</el-button><el-tag effect="plain">{{ drillLevelLabel }}</el-tag></div>
          </div>
          <div class="drill-path"><span>组合</span><span v-for="item in drillPath" :key="item.value">/ {{ item.label }}</span></div>
          <el-table v-loading="drillLoading" :data="drillRows" size="small" border max-height="500" @row-click="drillNext">
            <el-table-column prop="node_name" label="范围" min-width="210" fixed="left"><template #default="{ row }"><el-link type="primary" :underline="false">{{ row.node_name || '-' }}</el-link></template></el-table-column>
            <el-table-column prop="item_count" label="债项数" width="90" align="right" />
            <el-table-column prop="customer_count" label="客户数" width="90" align="right" />
            <el-table-column label="基准 EAD" min-width="125" align="right"><template #default="{ row }">{{ compactAmount(row.previous_ead) }}</template></el-table-column>
            <el-table-column label="本月 EAD" min-width="125" align="right"><template #default="{ row }">{{ compactAmount(row.current_ead) }}</template></el-table-column>
            <el-table-column label="EAD 变化" min-width="120" align="right"><template #default="{ row }"><span :class="deltaClass(row.ead_delta)">{{ signedCompact(row.ead_delta) }}</span></template></el-table-column>
            <el-table-column label="EL 变化" min-width="110" align="right"><template #default="{ row }"><span :class="deltaClass(row.el_delta)">{{ signedCompact(row.el_delta) }}</span></template></el-table-column>
            <el-table-column prop="default_count" label="违约债项" width="95" align="right" />
          </el-table>
        </article>
      </el-tab-pane>

      <el-tab-pane label="明细核对" name="details">
        <article class="panel">
          <div class="panel-title"><div><h3>债项级月末变化</h3><span>按 EAD 与 EL 变化绝对值展示前 200 项</span></div></div>
          <el-table :data="data.change_details || []" size="small" border max-height="500">
            <el-table-column prop="customer_no" label="客户编号" min-width="145" fixed="left" />
            <el-table-column prop="debt_no" label="债项编号" min-width="145" />
            <el-table-column prop="industry_name" label="行业" min-width="125" show-overflow-tooltip />
            <el-table-column label="类型" width="118"><template #default="{ row }"><el-tag size="small" :type="reasonType(row.change_type)" effect="plain">{{ reasonLabel(row.change_type) }}</el-tag></template></el-table-column>
            <el-table-column label="评级" width="92"><template #default="{ row }">{{ row.previous_rating || '-' }} → {{ row.current_rating || '-' }}</template></el-table-column>
            <el-table-column label="EAD 前值" min-width="105" align="right"><template #default="{ row }">{{ compactAmount(row.previous_ead) }}</template></el-table-column>
            <el-table-column label="EAD 本月" min-width="105" align="right"><template #default="{ row }">{{ compactAmount(row.current_ead) }}</template></el-table-column>
            <el-table-column label="EAD 变化" min-width="105" align="right"><template #default="{ row }"><span :class="deltaClass(row.ead_delta)">{{ signedCompact(row.ead_delta) }}</span></template></el-table-column>
            <el-table-column label="PD" width="100" align="right"><template #default="{ row }">{{ percent(row.previous_pd) }} → {{ percent(row.current_pd) }}</template></el-table-column>
            <el-table-column label="LGD" width="110" align="right"><template #default="{ row }">{{ percent(row.previous_lgd) }} → {{ percent(row.current_lgd) }}</template></el-table-column>
            <el-table-column label="EL 变化" min-width="100" align="right"><template #default="{ row }"><span :class="deltaClass(row.el_delta)">{{ signedCompact(row.el_delta) }}</span></template></el-table-column>
          </el-table>
        </article>
      </el-tab-pane>

      <el-tab-pane label="批次与数据质量" name="batch">
        <section class="grid-two">
          <article class="panel">
            <div class="panel-title"><div><h3>已发布月末批次</h3><span>每个月仅展示最新锁定版本</span></div></div>
            <el-table :data="months" size="small" max-height="390">
              <el-table-column label="月末" width="105"><template #default="{ row }">{{ date(row.month_end_date) }}</template></el-table-column>
              <el-table-column prop="batch_no" label="批次" min-width="155" />
              <el-table-column prop="batch_type" label="类型" width="92" />
              <el-table-column prop="record_count" label="债项" width="72" align="right" />
              <el-table-column prop="quality_score" label="质量分" width="82" align="right" />
              <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag size="small" type="success" effect="plain">{{ row.status }}</el-tag></template></el-table-column>
            </el-table>
          </article>
          <article class="panel">
            <div class="panel-title"><div><h3>当前批次勾稽</h3><span>源数据与锁定快照的一致性检查</span></div></div>
            <el-table :data="data.reconciliation || []" size="small" max-height="390">
              <el-table-column prop="check_name" label="检查项" min-width="145" />
              <el-table-column label="源数据" min-width="100" align="right"><template #default="{ row }">{{ number(row.source_value) }}</template></el-table-column>
              <el-table-column label="快照" min-width="100" align="right"><template #default="{ row }">{{ number(row.snapshot_value) }}</template></el-table-column>
              <el-table-column label="差异" min-width="90" align="right"><template #default="{ row }">{{ number(row.difference_value) }}</template></el-table-column>
              <el-table-column label="结果" width="85"><template #default="{ row }"><el-tag size="small" :type="row.check_status === 'PASSED' ? 'success' : 'danger'" effect="plain">{{ row.check_status === 'PASSED' ? '通过' : '失败' }}</el-tag></template></el-table-column>
            </el-table>
          </article>
        </section>
        <section class="grid-two batch-detail-grid">
          <article class="panel">
            <div class="panel-title"><div><h3>上游数据清单</h3><span>核验数据日期、记录数和输入校验值</span></div></div>
            <el-table :data="data.source_manifest || []" size="small">
              <el-table-column prop="source_name" label="数据源" min-width="180" />
              <el-table-column prop="source_batch_no" label="上游批次" min-width="150" />
              <el-table-column prop="data_date" label="数据日期" width="110" />
              <el-table-column prop="received_count" label="接收数" width="90" align="right" />
              <el-table-column prop="receive_status" label="状态" width="95"><template #default="{ row }"><el-tag size="small" type="success" effect="plain">{{ row.receive_status }}</el-tag></template></el-table-column>
            </el-table>
          </article>
          <article class="panel">
            <div class="panel-title"><div><h3>数据质量整改</h3><span>异常问题分派、处理和关闭</span></div><el-tag :type="(data.quality_overview?.open_count || 0) ? 'danger' : 'success'" effect="plain">待整改 {{ integer(data.quality_overview?.open_count) }}</el-tag></div>
            <el-table :data="data.quality_issues || []" size="small" empty-text="当前批次无质量问题">
              <el-table-column prop="issue_description" label="问题" min-width="210" show-overflow-tooltip />
              <el-table-column prop="issue_level" label="等级" width="75" />
              <el-table-column prop="issue_count" label="数量" width="72" align="right" />
              <el-table-column prop="status" label="状态" width="95" />
              <el-table-column label="操作" width="80"><template #default="{ row }"><el-button link type="primary" @click="resolveIssue(row)">处理</el-button></template></el-table-column>
            </el-table>
          </article>
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="batchDialog" title="月末生产批次加工" width="520px">
      <el-form label-width="100px">
        <el-form-item label="月末日期"><el-date-picker v-model="batchForm.month_end_date" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="上游批次号"><el-input v-model="batchForm.source_batch_no" placeholder="例如 CREDIT-20260731-01" /></el-form-item>
        <el-form-item label="运行模式"><el-segmented v-model="batchForm.run_mode" :options="[{label:'正式加工',value:'FORMAL'},{label:'重算版本',value:'REPROCESS'}]" /></el-form-item>
        <el-form-item label="发布说明"><el-input v-model="batchForm.publish_comment" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="batchDialog=false">取消</el-button><el-button type="primary" :loading="capturing" @click="capture">执行加工</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { Calendar, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'

const data = ref({})
const loading = ref(false)
const capturing = ref(false)
const batchDialog = ref(false)
const batchForm = ref({ month_end_date: '', source_batch_no: '', run_mode: 'FORMAL', publish_comment: '月末组合风险正式加工' })
const drillLoading = ref(false)
const drillRows = ref([])
const drillPath = ref([])
const drillLevels = ['INDUSTRY', 'CUSTOMER', 'CONTRACT', 'DEBT']
const activeTab = ref('overview')
const currentMonth = ref('')
const baseMonth = ref('')
const dimension = ref('INDUSTRY')
const dimensionOptions = [
  { label: '行业', value: 'INDUSTRY' },
  { label: '产品', value: 'PRODUCT' },
  { label: '机构', value: 'ORGANIZATION' }
]
const overview = computed(() => data.value.overview || {})
const overdue = computed(() => data.value.overdue_default || {})
const months = computed(() => data.value.available_months || [])
const drillLevel = computed(() => drillLevels[Math.min(drillPath.value.length, drillLevels.length - 1)])
const drillLevelLabel = computed(() => ({ INDUSTRY: '行业层', CUSTOMER: '客户层', CONTRACT: '合同层', DEBT: '债项层' })[drillLevel.value])
const baseMonthOptions = computed(() => months.value.filter((item) => item.month_end_date !== currentMonth.value))
const defaultChanges = computed(() => (data.value.change_details || []).filter((row) => ['NEW_DEFAULT', 'CURED', 'DOWNGRADE', 'UPGRADE'].includes(row.change_type)))
const riskLevels = computed(() => {
  const values = new Set(['LOW', 'MEDIUM', 'HIGH'])
  ;(data.value.risk_migration || []).forEach((row) => { values.add(row.previous_level); values.add(row.current_level) })
  return [...values].filter(Boolean)
})
const riskMatrixRows = computed(() => riskLevels.value.map((previous) => {
  const row = { previous }
  riskLevels.value.forEach((current) => {
    row[current] = (data.value.risk_migration || []).find((item) => item.previous_level === previous && item.current_level === current)?.customer_count || 0
  })
  return row
}))

onMounted(load)
watch(activeTab, (value) => { if (value === 'drilldown' && !drillRows.value.length) loadDrilldown() })

async function load() {
  loading.value = true
  try {
    const params = { dimension: dimension.value }
    if (currentMonth.value) params.currentMonth = currentMonth.value
    if (baseMonth.value) params.baseMonth = baseMonth.value
    data.value = await http.get('/api/risks/month-end-analysis', { params })
    currentMonth.value = String(data.value.current_batch?.month_end_date || '').slice(0, 10)
    baseMonth.value = String(data.value.base_batch?.month_end_date || '').slice(0, 10)
  } finally {
    loading.value = false
  }
}

async function changeCurrent() {
  const options = months.value.filter((item) => item.month_end_date < currentMonth.value)
  baseMonth.value = options[0]?.month_end_date || baseMonthOptions.value[0]?.month_end_date || ''
  await load()
}

async function capture() {
  capturing.value = true
  try {
    const payload = { ...batchForm.value, force_new_version: batchForm.value.run_mode === 'REPROCESS' }
    data.value = await http.post('/api/risks/month-end-analysis/batches', payload)
    currentMonth.value = String(data.value.current_batch?.month_end_date || '').slice(0, 10)
    baseMonth.value = String(data.value.base_batch?.month_end_date || '').slice(0, 10)
    batchDialog.value = false
    ElMessage.success('月末批次加工、质量检查与发布已完成')
  } finally {
    capturing.value = false
  }
}

async function loadDrilldown() {
  drillLoading.value = true
  try {
    const params = { currentMonth: currentMonth.value, baseMonth: baseMonth.value, level: drillLevel.value }
    drillPath.value.forEach((item) => { params[item.param] = item.value })
    const result = await http.get('/api/risks/month-end-analysis/changes/drilldown', { params })
    drillRows.value = result.rows || []
  } finally { drillLoading.value = false }
}

async function drillNext(row) {
  if (drillLevel.value === 'DEBT') return
  const mapping = {
    INDUSTRY: { param: 'industry', label: row.node_name },
    CUSTOMER: { param: 'customerNo', label: `${row.node_name} (${row.node_key})` },
    CONTRACT: { param: 'contractNo', label: row.node_key }
  }
  drillPath.value.push({ ...mapping[drillLevel.value], value: row.node_key })
  await loadDrilldown()
}

async function drillBack() { drillPath.value.pop(); await loadDrilldown() }

async function resolveIssue(row) {
  const { value } = await ElMessageBox.prompt('填写整改说明', '处理数据质量问题', { inputValue: row.resolution_note || '', confirmButtonText: '完成整改', cancelButtonText: '取消' })
  await http.patch(`/api/risks/month-end-analysis/quality-issues/${row.id}`, { status: 'RESOLVED', resolution_note: value })
  ElMessage.success('质量问题已完成整改')
  await load()
}

function date(value) { return value ? String(value).slice(0, 10) : '-' }
function monthLabel(value) { return date(value).slice(0, 7) + ' 月末' }
function shortMonth(value) { return date(value).slice(2, 7) }
function number(value) { return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 }) }
function integer(value) { return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 }) }
function amount(value) { return '¥' + Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
function compactAmount(value) { const n = Number(value || 0); return n >= 100000000 ? '¥' + (n / 100000000).toFixed(2) + '亿' : n >= 10000 ? '¥' + (n / 10000).toFixed(2) + '万' : amount(n) }
function signedAmount(value) { const n = Number(value || 0); return (n >= 0 ? '+' : '-') + amount(Math.abs(n)) }
function signedCompact(value) { const n = Number(value || 0); return (n >= 0 ? '+' : '-') + compactAmount(Math.abs(n)) }
function percent(value) { return (Number(value || 0) * 100).toFixed(2) + '%' }
function signedPercent(value) { const n = Number(value || 0); return (n >= 0 ? '+' : '') + (n * 100).toFixed(2) + '%' }
function signedPoints(value) { const n = Number(value || 0); return (n >= 0 ? '+' : '') + (n * 100).toFixed(2) + 'pct' }
function signedInteger(value) { const n = Number(value || 0); return (n >= 0 ? '+' : '') + n.toLocaleString('zh-CN', { maximumFractionDigits: 0 }) }
function deltaClass(value) { const n = Number(value || 0); return n > 0 ? 'up' : n < 0 ? 'down' : 'flat' }
function trendWidth(value) { const max = Math.max(...(data.value.monthly_trend || []).map((item) => Number(item.ead_amount || 0)), 1); return Math.max(5, Number(value || 0) / max * 100) + '%' }
function effectWidth(value) { const max = Math.max(...(data.value.el_attribution || []).map((item) => Math.abs(Number(item.amount || 0))), 1); return Math.max(4, Math.abs(Number(value || 0)) / max * 100) + '%' }
function dimensionLabel(value) { return ({ INDUSTRY: '行业', PRODUCT: '产品', ORGANIZATION: '机构' })[value] || value }
function riskLabel(value) { return ({ LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' })[value] || value || '-' }
function reasonLabel(value) { return ({ NEW: '新增', EXIT: '退出/结清', NEW_DEFAULT: '新增违约', CURED: '违约治愈', DOWNGRADE: '风险下迁', UPGRADE: '风险改善', EXPOSURE_INCREASE: '敞口增加', EXPOSURE_DECREASE: '敞口减少', PARAM_CHANGE: '参数变化', STABLE: '保持稳定' })[value] || value }
function reasonType(value) { return ({ NEW: 'primary', EXIT: 'info', NEW_DEFAULT: 'danger', CURED: 'success', DOWNGRADE: 'danger', UPGRADE: 'success', EXPOSURE_INCREASE: 'warning', EXPOSURE_DECREASE: 'success', PARAM_CHANGE: 'warning', STABLE: 'info' })[value] || 'info' }
</script>

<style scoped>
.month-page{display:grid;gap:15px}.page-toolbar,.toolbar-actions,.panel-title,.compare-bar{display:flex;align-items:center;justify-content:space-between;gap:12px}h2,h3,p{margin:0}h2{font-size:18px;color:#1f2937}.page-toolbar p,.panel-title span{margin-top:5px;color:#64748b;font-size:13px}.compare-bar{justify-content:flex-start;padding:12px 14px;background:#fff;border:1px solid #dce4ee;border-radius:8px}.compare-bar>div:not(.batch-meta){display:flex;align-items:center;gap:8px}.compare-bar span{font-size:13px;color:#52606d}.compare-bar .el-select{width:150px}.compare-arrow{padding:4px 10px;background:#edf4f8;color:#245b82;border-radius:4px}.batch-meta{margin-left:auto;display:flex;align-items:center;gap:10px;flex-wrap:wrap}.metric-strip,.default-strip{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));background:#fff;border:1px solid #dce4ee;border-radius:8px;overflow:hidden}.metric-strip>div,.default-strip>div{display:grid;gap:5px;padding:14px;border-right:1px solid #e8eef4}.metric-strip>div:last-child,.default-strip>div:last-child{border-right:0}.metric-strip span,.metric-strip small,.default-strip span,.default-strip small{font-size:12px;color:#64748b}.metric-strip strong,.default-strip strong{font-size:19px;color:#1f2937}.analysis-tabs :deep(.el-tabs__header){margin-bottom:12px}.grid-two{display:grid;grid-template-columns:minmax(0,1fr) minmax(390px,.95fr);gap:15px}.panel{padding:16px}.panel-title{margin-bottom:14px}.panel-title h3{font-size:15px;color:#334155}.trend-list,.effect-list{display:grid;gap:12px}.trend-row{display:grid;grid-template-columns:58px minmax(120px,1fr) 88px 98px;align-items:center;gap:10px}.trend-row span,.trend-row small{font-size:12px;color:#64748b}.trend-row strong{font-size:13px;text-align:right}.bar-track,.effect-track{height:12px;background:#edf2f6;overflow:hidden}.bar-fill{height:100%;background:#39769f}.effect-row{display:grid;grid-template-columns:120px minmax(120px,1fr) 118px;align-items:center;gap:10px}.effect-row strong{font-size:13px}.effect-row span{text-align:right;font-size:13px}.effect-fill{height:100%}.effect-fill.increase{background:#c65f35}.effect-fill.decrease{background:#16826d}.up{color:#c2413a!important}.down{color:#087a65!important}.flat{color:#64748b!important}.hot{color:#c2413a}.default-strip{grid-template-columns:repeat(5,minmax(0,1fr));margin-bottom:15px}.drill-actions{display:flex;align-items:center;gap:8px}.drill-path{display:flex;gap:7px;min-height:34px;margin:-3px 0 10px;padding:8px 10px;background:#f5f8fb;color:#52606d;font-size:12px;overflow-x:auto}.batch-detail-grid{margin-top:15px}@media(max-width:1100px){.metric-strip{grid-template-columns:repeat(3,minmax(0,1fr))}.grid-two{grid-template-columns:1fr}.compare-bar{flex-wrap:wrap}.batch-meta{width:100%;margin-left:0}.default-strip{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:680px){.page-toolbar,.panel-title{align-items:flex-start;flex-direction:column}.toolbar-actions{width:100%}.toolbar-actions .el-button{flex:1}.compare-bar>div:not(.batch-meta){width:100%;justify-content:space-between}.compare-bar .el-select{width:min(240px,65%)}.compare-arrow{display:none}.metric-strip,.default-strip{grid-template-columns:1fr}.metric-strip>div,.default-strip>div{border-right:0;border-bottom:1px solid #e8eef4}.trend-row,.effect-row{grid-template-columns:1fr}.trend-row strong,.effect-row span{text-align:left}}
</style>
