<template>
  <section class="subscription-page">
    <div class="page-toolbar">
      <el-input v-model="keyword" clearable :prefix-icon="Search" placeholder="订阅编码、名称或收件人" @keyup.enter="loadSubscriptions" />
      <el-button type="primary" :icon="Plus" @click="openCreate">新增订阅</el-button>
      <el-tooltip content="刷新订阅"><el-button :icon="Refresh" circle @click="loadSubscriptions" /></el-tooltip>
    </div>

    <div class="subscription-stats">
      <div><span>订阅总数</span><strong>{{ total }}</strong></div>
      <div><span>已启用</span><strong>{{ enabledCount }}</strong></div>
      <div><span>通知通道</span><strong>系统 / 邮件</strong></div>
    </div>

    <el-table v-loading="loading" :data="rows" border height="calc(100vh - 270px)">
      <el-table-column prop="subscription_code" label="订阅编码" min-width="166" show-overflow-tooltip />
      <el-table-column prop="subscription_name" label="订阅名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="frequency" label="频率" width="84"><template #default>每日</template></el-table-column>
      <el-table-column label="通道" width="96"><template #default="{ row }"><el-tag effect="plain" :type="row.channel === 'EMAIL' ? 'warning' : 'success'">{{ row.channel === 'EMAIL' ? '邮件' : '系统' }}</el-tag></template></el-table-column>
      <el-table-column prop="target_type" label="目标" width="88"><template #default="{ row }">{{ row.target_type === 'USER' ? '指定用户' : '全部用户' }}</template></el-table-column>
      <el-table-column prop="recipients" label="收件人" min-width="170" show-overflow-tooltip><template #default="{ row }">{{ row.recipients || '-' }}</template></el-table-column>
      <el-table-column prop="last_dispatch_at" label="最近发送" width="164"><template #default="{ row }">{{ formatDateTime(row.last_dispatch_at) }}</template></el-table-column>
      <el-table-column label="状态" width="84"><template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">{{ row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :loading="dispatchingId === row.id" @click="dispatch(row)">发送</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="removeSubscription(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="loadSubscriptions" @size-change="changeSize" /></div>

    <el-dialog v-model="visible" :title="editingId ? '编辑预警订阅' : '新增预警订阅'" width="620px" destroy-on-close>
      <el-form :model="form" label-width="96px">
        <el-form-item label="订阅编码" required><el-input v-model="form.subscription_code" :disabled="Boolean(editingId)" /></el-form-item>
        <el-form-item label="订阅名称" required><el-input v-model="form.subscription_name" /></el-form-item>
        <el-form-item label="通知通道"><el-radio-group v-model="form.channel"><el-radio-button label="SYSTEM">系统通知</el-radio-button><el-radio-button label="EMAIL">邮件</el-radio-button></el-radio-group></el-form-item>
        <el-form-item label="通知目标"><el-radio-group v-model="form.target_type"><el-radio-button label="ALL">全部用户</el-radio-button><el-radio-button label="USER">指定用户</el-radio-button></el-radio-group></el-form-item>
        <el-form-item v-if="form.target_type === 'USER' || form.channel === 'EMAIL'" label="收件人"><el-input v-model="form.recipients" placeholder="多个邮箱以逗号分隔" /></el-form-item>
        <el-form-item label="启用订阅"><el-switch v-model="form.enabled" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveSubscription">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import http from '../api/http'

const keyword = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const rows = ref([])
const loading = ref(false)
const saving = ref(false)
const dispatchingId = ref(null)
const visible = ref(false)
const editingId = ref(null)
const form = reactive(defaultForm())
const enabledCount = computed(() => rows.value.filter((row) => Number(row.enabled) === 1).length)

watch(keyword, (value) => { if (!value) { page.value = 1; loadSubscriptions() } })
onMounted(loadSubscriptions)

async function loadSubscriptions() {
  loading.value = true
  try {
    const data = await http.get('/api/risks/alert-subscriptions', { params: { page: page.value, size: size.value, keyword: keyword.value.trim() || undefined } })
    rows.value = data.items || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

function changeSize() { page.value = 1; loadSubscriptions() }
function openCreate() { editingId.value = null; Object.assign(form, defaultForm()); visible.value = true }
function openEdit(row) { editingId.value = row.id; Object.assign(form, { ...defaultForm(), ...row, enabled: Number(row.enabled) === 1 }) ; visible.value = true }

async function saveSubscription() {
  if (!form.subscription_code.trim() || !form.subscription_name.trim()) { ElMessage.warning('请填写订阅编码和订阅名称'); return }
  if ((form.target_type === 'USER' || form.channel === 'EMAIL') && !form.recipients.trim()) { ElMessage.warning('请填写收件人'); return }
  saving.value = true
  try {
    if (editingId.value) await http.put(`/api/risks/alert-subscriptions/${editingId.value}`, { ...form })
    else await http.post('/api/risks/alert-subscriptions', { ...form })
    ElMessage.success('预警订阅已保存')
    visible.value = false
    await loadSubscriptions()
  } finally { saving.value = false }
}

async function dispatch(row) {
  dispatchingId.value = row.id
  try {
    await http.post(`/api/risks/alert-subscriptions/${row.id}/dispatch`)
    ElMessage.success('组合风险日报已生成')
    await loadSubscriptions()
  } finally { dispatchingId.value = null }
}

async function removeSubscription(row) {
  await ElMessageBox.confirm(`删除订阅“${row.subscription_name}”后不再自动发送日报。`, '删除预警订阅', { type: 'warning' })
  await http.delete(`/api/risks/alert-subscriptions/${row.id}`)
  ElMessage.success('预警订阅已删除')
  await loadSubscriptions()
}

function formatDateTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
function defaultForm() { return { subscription_code: '', subscription_name: '', channel: 'SYSTEM', target_type: 'ALL', recipients: '', enabled: false } }
</script>

<style scoped>
.subscription-page { display: flex; flex-direction: column; gap: 14px; }
.page-toolbar { display: flex; align-items: center; gap: 10px; }
.page-toolbar .el-input { width: min(340px, 100%); }
.subscription-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 190px)); gap: 10px; }
.subscription-stats > div { display: grid; gap: 6px; padding: 12px 14px; border: 1px solid #dce3ec; border-radius: 8px; background: #fff; }
.subscription-stats span { color: #64748b; font-size: 12px; }
.subscription-stats strong { color: #1f2937; font-size: 20px; }
.pager { display: flex; justify-content: flex-end; }
@media (max-width: 640px) { .page-toolbar { flex-wrap: wrap; } .subscription-stats { grid-template-columns: 1fr; } }
</style>
