<template>
  <section class="relationship-page">
    <div class="page-toolbar">
      <div><h2>风险关系图谱</h2><p>识别客户、合同、债项、押品及客户经理关联客户之间的风险关系。</p></div>
      <div class="graph-search"><el-input v-model="customerNo" :prefix-icon="Search" placeholder="输入客户编号" @keyup.enter="load" /><el-button type="primary" :loading="loading" @click="load">查询图谱</el-button></div>
    </div>
    <section v-if="graph.customer" class="graph-summary">
      <div><span>客户</span><strong>{{ graph.customer.customer_name || graph.customer.customer_no }}</strong><small>{{ graph.customer.customer_no }}</small></div>
      <div><span>组合风险</span><strong :class="riskClass(graph.customer.risk_score)">{{ graph.customer.risk_level }}</strong><small>评分 {{ graph.customer.risk_score }}</small></div>
      <div><span>关系节点</span><strong>{{ (graph.nodes || []).length }}</strong><small>连接 {{ (graph.edges || []).length }} 条</small></div>
      <div><span>债项违约</span><strong>{{ number(graph.customer.debt_default_count) }}</strong><small>逾期 {{ number(graph.customer.overdue_count) }} 笔</small></div>
    </section>
    <section v-if="graph.customer" class="panel graph-panel">
      <div class="panel-title"><div><h3>关系视图</h3><span>从客户向业务、担保与关联客群展开</span></div><el-tag effect="plain">{{ graph.customer.owner_org_name || '未分配机构' }}</el-tag></div>
      <div class="relationship-stage">
        <div class="relation-lane center"><span class="lane-title">客户</span><div v-for="node in groups.CUSTOMER" :key="node.id" class="relation-node customer"><strong>{{ node.label }}</strong><small>{{ node.meta?.industry_name || '客户主体' }}</small></div></div>
        <div class="relation-lane"><span class="lane-title">合同</span><div v-for="node in groups.CONTRACT" :key="node.id" class="relation-node contract"><strong>{{ node.label }}</strong><small>{{ node.meta?.product_type || '-' }}</small></div><el-empty v-if="!groups.CONTRACT.length" :image-size="38" description="暂无合同" /></div>
        <div class="relation-lane"><span class="lane-title">债项与押品</span><div v-for="node in businessNodes" :key="node.id" class="relation-node" :class="node.type === 'COLLATERAL' ? 'collateral' : 'drawdown'"><strong>{{ node.label }}</strong><small>{{ node.type === 'COLLATERAL' ? '押品担保' : '债项支用' }}</small></div><el-empty v-if="!businessNodes.length" :image-size="38" description="暂无业务节点" /></div>
        <div class="relation-lane"><span class="lane-title">关联客群</span><div v-for="node in groups.PEER" :key="node.id" class="relation-node peer"><strong>{{ node.label }}</strong><small>{{ node.meta?.relationship_manager_name || '同客户经理' }}</small></div><el-empty v-if="!groups.PEER.length" :image-size="38" description="暂无关联客户" /></div>
      </div>
    </section>
    <section v-if="graph.customer" class="panel edge-panel"><div class="panel-title"><div><h3>关联明细</h3><span>可作为尽调和风险传导核验依据</span></div></div><el-table :data="graph.edges || []" border max-height="300"><el-table-column prop="source" label="来源节点" min-width="190" show-overflow-tooltip /><el-table-column prop="type" label="关联关系" min-width="130" /><el-table-column prop="target" label="目标节点" min-width="190" show-overflow-tooltip /></el-table></section>
    <el-empty v-else-if="!loading" description="输入客户编号后查看关联关系" :image-size="88" />
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import http from '../api/http'

const customerNo = ref('')
const loading = ref(false)
const graph = ref({})
const groups = computed(() => {
  const result = { CUSTOMER: [], CONTRACT: [], DRAWDOWN: [], COLLATERAL: [], PEER: [] }
  for (const node of graph.value.nodes || []) if (result[node.type]) result[node.type].push(node)
  return result
})
const businessNodes = computed(() => [...groups.value.DRAWDOWN, ...groups.value.COLLATERAL])
async function load() { if (!customerNo.value.trim()) { ElMessage.warning('请输入客户编号'); return }; loading.value = true; try { graph.value = await http.get('/api/risks/relationship-graph', { params: { customerNo: customerNo.value.trim() } }) } finally { loading.value = false } }
function number(value) { return Number(value || 0) }
function riskClass(score) { return Number(score || 0) >= 85 ? 'danger' : Number(score || 0) >= 65 ? 'warning' : Number(score || 0) >= 45 ? 'attention' : 'success' }
</script>

<style scoped>
.relationship-page { display: grid; gap: 16px; }
.page-toolbar, .graph-search, .panel-title { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
h2, h3, p { margin: 0; }
h2 { color: #1f2937; font-size: 18px; font-weight: 650; }
.page-toolbar p, .panel-title span { color: #64748b; font-size: 13px; margin-top: 5px; }
.graph-search { width: min(430px, 100%); }
.graph-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); border: 1px solid #dce4ee; border-radius: 8px; overflow: hidden; background: #fff; }
.graph-summary > div { display: grid; gap: 5px; min-height: 92px; padding: 14px; border-right: 1px solid #e9eef4; }
.graph-summary > div:last-child { border-right: 0; }
.graph-summary span, .graph-summary small { color: #64748b; font-size: 12px; }
.graph-summary strong { color: #1f2937; font-size: 19px; overflow-wrap: anywhere; }
.graph-summary strong.danger { color: #c2413a; }
.graph-summary strong.warning { color: #b45309; }
.graph-summary strong.attention { color: #1f5e93; }
.graph-summary strong.success { color: #087a65; }
.panel { padding: 16px; }
.panel-title { margin-bottom: 14px; }
.panel-title h3 { color: #334155; font-size: 15px; }
.relationship-stage { display: grid; grid-template-columns: 1.15fr 1fr 1.25fr 1fr; min-height: 300px; border: 1px solid #e1e8ef; background: #f8fafc; }
.relation-lane { display: flex; flex-direction: column; gap: 9px; min-width: 0; padding: 13px; border-right: 1px solid #e1e8ef; }
.relation-lane:last-child { border-right: 0; }
.relation-lane.center { justify-content: center; background: #f0f6fb; }
.lane-title { color: #64748b; font-size: 12px; font-weight: 650; }
.relation-node { display: grid; gap: 4px; padding: 10px; border: 1px solid #d7e2ec; border-left: 3px solid #5a8ab1; background: #fff; }
.relation-node strong { color: #334155; font-size: 13px; overflow-wrap: anywhere; }
.relation-node small { color: #718096; font-size: 11px; }
.relation-node.customer { border-left-color: #1f5e93; }
.relation-node.contract { border-left-color: #087a65; }
.relation-node.drawdown { border-left-color: #b45309; }
.relation-node.collateral { border-left-color: #7b6d3e; }
.relation-node.peer { border-left-color: #7c6fa8; }
@media (max-width: 980px) { .relationship-stage { grid-template-columns: repeat(2, minmax(0, 1fr)); } .relation-lane:nth-child(2) { border-right: 0; } .relation-lane:nth-child(-n+2) { border-bottom: 1px solid #e1e8ef; } }
@media (max-width: 620px) { .page-toolbar { align-items: flex-start; flex-direction: column; } .graph-search { width: 100%; } .graph-summary, .relationship-stage { grid-template-columns: 1fr; } .graph-summary > div, .relation-lane { border-right: 0; border-bottom: 1px solid #e1e8ef; } .relation-lane.center { justify-content: flex-start; } }
</style>
