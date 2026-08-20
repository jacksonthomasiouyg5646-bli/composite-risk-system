<template>
  <section class="assistant-page">
    <section class="query-panel">
      <div class="panel-heading">
        <div>
          <h2>客户风险智能问答</h2>
          <p>基于信贷域关联数据、组合评分和可选外部数据生成结论。</p>
        </div>
        <el-tooltip content="清空当前问答"><el-button :icon="Refresh" circle @click="reset" /></el-tooltip>
      </div>
      <el-form class="query-form" label-position="top" @submit.prevent="ask">
        <el-form-item label="客户编号或客户名称" required>
          <el-input v-model="form.customer" clearable placeholder="例如：CUST202607210100" :prefix-icon="Search" @keyup.enter="ask" />
        </el-form-item>
        <el-form-item label="问题">
          <el-input v-model="form.question" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="例如：该客户未来30天风险会如何变化？" @keyup.ctrl.enter="ask" />
        </el-form-item>
        <div class="question-presets" aria-label="常用问题">
          <el-button v-for="item in presets" :key="item" text @click="form.question = item">{{ item }}</el-button>
        </div>
        <div class="query-actions">
          <el-switch v-model="form.includeExternal" active-text="查询外部数据" inactive-text="仅使用内部数据" />
          <el-button type="primary" :icon="MagicStick" :loading="loading" native-type="submit">开始分析</el-button>
        </div>
      </el-form>
    </section>

    <section v-if="result" class="answer-layout">
      <article class="answer-panel">
        <div class="answer-header">
          <div>
            <span class="eyebrow">{{ result.assistant_model }}</span>
            <h3>{{ result.customer?.customer_name || result.customer?.customer_no }}</h3>
            <p>{{ result.question }}</p>
          </div>
          <div class="risk-tags">
            <el-tag :type="riskTagType(result.risk_level)" effect="plain">当前 {{ result.risk_level }}</el-tag>
            <el-tag :type="riskTagType(result.forecast_level)" effect="plain">30天 {{ result.forecast_level }}</el-tag>
          </div>
        </div>
        <p class="answer-text">{{ result.answer }}</p>
        <div class="score-strip">
          <div><span>当前评分</span><strong>{{ number(result.risk_score) }}</strong></div>
          <div><span>预测评分</span><strong>{{ number(result.forecast_score) }}</strong></div>
          <div><span>变化判断</span><strong>{{ result.forecast_change || '-' }}</strong></div>
        </div>
        <div class="recommendations">
          <h4>建议动作</h4>
          <ul><li v-for="item in result.recommendations || []" :key="item">{{ item }}</li></ul>
        </div>
      </article>

      <aside class="evidence-panel">
        <div class="side-heading"><h3>证据引用</h3><span>{{ result.evidence?.length || 0 }} 项</span></div>
        <div class="evidence-list">
          <div v-for="item in result.evidence || []" :key="`${item.label}-${item.source}`" class="evidence-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.source }}</small>
          </div>
        </div>
        <div class="external-status">
          <span>外部数据</span>
          <strong>{{ result.external_data?.status_label || '未查询' }}</strong>
          <small>{{ result.external_data?.provider_name || '-' }}</small>
        </div>
      </aside>
    </section>

    <el-empty v-else-if="!loading" description="输入客户信息后开始风险分析" :image-size="88" />
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Refresh, Search } from '@element-plus/icons-vue'
import http from '../api/http'

const presets = ['该客户当前风险如何？', '是否存在逾期或违约？', '额度和风险敞口情况如何？', '押品担保是否充足？', '该客户未来30天风险会如何变化？', '外部数据有什么提示？', '下一步处置建议是什么？']
presets.push('该客户 LGD、回收率和衰退损失如何？')
const loading = ref(false)
const result = ref(null)
const form = reactive({ customer: '', question: '该客户当前风险如何？', includeExternal: true })

async function ask() {
  if (!form.customer.trim()) {
    ElMessage.warning('请输入客户编号或客户名称')
    return
  }
  loading.value = true
  try {
    result.value = await http.post('/api/risks/ai-chat/customer', {
      customer: form.customer.trim(),
      question: form.question.trim(),
      includeExternal: form.includeExternal
    })
  } finally {
    loading.value = false
  }
}

function reset() {
  form.customer = ''
  form.question = '该客户当前风险如何？'
  form.includeExternal = true
  result.value = null
}

function number(value) { return Number(value || 0).toFixed(0) }
function riskTagType(level) {
  if (['极高', '高'].includes(level)) return 'danger'
  if (['较高', '中'].includes(level)) return 'warning'
  return 'success'
}
</script>

<style scoped>
.assistant-page { display: grid; gap: 16px; }
.query-panel, .answer-panel, .evidence-panel { border: 1px solid #dce3ec; border-radius: 8px; background: #fff; }
.query-panel { padding: 18px 20px; }
.panel-heading, .answer-header, .side-heading, .query-actions { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
h2, h3, h4, p { margin: 0; }
h2 { color: #1f2937; font-size: 18px; font-weight: 650; }
.panel-heading p, .answer-header p { color: #64748b; font-size: 13px; margin-top: 5px; }
.query-form { margin-top: 14px; }
.query-form :deep(.el-form-item) { margin-bottom: 12px; }
.question-presets { display: flex; flex-wrap: wrap; gap: 4px; margin: -3px 0 12px; }
.question-presets .el-button { color: #335d8f; padding: 4px 7px; height: auto; }
.answer-layout { display: grid; grid-template-columns: minmax(0, 1.65fr) minmax(270px, .75fr); gap: 16px; align-items: start; }
.answer-panel { padding: 20px; }
.eyebrow { color: #527ca9; font-size: 11px; font-weight: 600; }
.answer-header h3 { color: #1f2937; font-size: 17px; margin-top: 5px; }
.risk-tags { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 6px; }
.answer-text { color: #334155; line-height: 1.8; margin: 20px 0; white-space: pre-wrap; }
.score-strip { display: grid; grid-template-columns: repeat(3, 1fr); border-top: 1px solid #edf1f5; border-bottom: 1px solid #edf1f5; }
.score-strip > div { display: grid; gap: 5px; padding: 13px 14px; border-right: 1px solid #edf1f5; }
.score-strip > div:last-child { border-right: 0; }
.score-strip span, .external-status span { color: #64748b; font-size: 12px; }
.score-strip strong { color: #1f2937; font-size: 18px; }
.recommendations { margin-top: 18px; }
.recommendations h4 { color: #334155; font-size: 14px; }
.recommendations ul { margin: 10px 0 0; padding-left: 20px; color: #475569; line-height: 1.8; }
.evidence-panel { padding: 16px; }
.side-heading h3 { color: #334155; font-size: 15px; }
.side-heading span { color: #64748b; font-size: 12px; }
.evidence-list { display: grid; gap: 8px; margin-top: 13px; }
.evidence-item { display: grid; gap: 3px; padding: 10px; background: #f8fafc; border-left: 3px solid #81a6cb; }
.evidence-item span, .evidence-item small, .external-status small { color: #64748b; font-size: 12px; }
.evidence-item strong, .external-status strong { color: #1f2937; font-size: 14px; }
.external-status { display: grid; gap: 4px; margin-top: 13px; padding-top: 13px; border-top: 1px solid #edf1f5; }
@media (max-width: 850px) { .answer-layout { grid-template-columns: 1fr; } }
@media (max-width: 600px) { .query-panel, .answer-panel, .evidence-panel { padding: 14px; } .panel-heading, .answer-header, .query-actions { align-items: flex-start; flex-direction: column; } .risk-tags { justify-content: flex-start; } .score-strip { grid-template-columns: 1fr; } .score-strip > div { border-right: 0; border-bottom: 1px solid #edf1f5; } .score-strip > div:last-child { border-bottom: 0; } }
</style>
