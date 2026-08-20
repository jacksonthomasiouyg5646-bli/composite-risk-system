<template>
  <section class="crud-page">
    <div class="toolbar">
      <el-input v-model="keyword" class="search-input" clearable :prefix-icon="Search" placeholder="搜索" @keyup.enter="loadData" />
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      <el-button v-if="!module.readOnly" type="primary" :icon="Plus" @click="openCreate">新增</el-button>
      <el-button v-if="module.key === 'users'" :icon="Download" @click="exportUsers">导出用户</el-button>
      <el-button v-if="module.key === 'users'" :icon="Upload" @click="importUsers">导入用户</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border height="calc(100vh - 252px)">
      <el-table-column v-for="field in module.fields" :key="field.prop" :prop="field.prop" :label="field.label" min-width="140" show-overflow-tooltip />
      <el-table-column v-if="!module.readOnly" label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="module.key === 'users'" size="small" :icon="UserFilled" @click="openAssignment(row)" />
          <el-button v-if="module.key === 'roles'" size="small" :icon="Key" @click="openAssignment(row)" />
          <el-button size="small" :icon="Edit" @click="openEdit(row)" />
          <el-popconfirm title="确认删除？" @confirm="remove(row)">
            <template #reference>
              <el-button size="small" type="danger" :icon="Delete" />
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50]"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑' : '新增'" width="560px">
      <el-form :model="form" label-width="96px">
        <el-form-item v-for="field in module.fields" :key="field.prop" :label="field.label" :required="field.required">
          <el-select v-if="field.type === 'select'" v-model="form[field.prop]" class="full-width">
            <el-option v-for="option in field.options" :key="option" :label="option" :value="option" />
          </el-select>
          <el-input-number v-else-if="field.type === 'number'" v-model="form[field.prop]" class="full-width" />
          <el-input v-else-if="field.type === 'textarea'" v-model="form[field.prop]" type="textarea" :rows="4" />
          <el-input v-else v-model="form[field.prop]" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignmentVisible" :title="assignmentTitle" width="560px">
      <el-checkbox-group v-model="assignmentSelected" class="assignment-list">
        <el-checkbox v-for="option in assignmentOptions" :key="option.id" :label="option.id">
          {{ option.name }}
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="assignmentVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAssignment">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Delete, Download, Edit, Key, Plus, Refresh, Search, Upload, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { findModule } from '../modules'

const route = useRoute()
const module = computed(() => findModule(route.meta.moduleKey))
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const form = reactive({})
const assignmentVisible = ref(false)
const assignmentTitle = ref('')
const assignmentRow = ref(null)
const assignmentOptions = ref([])
const assignmentSelected = ref([])
const assignmentUrl = ref('')

watch(
  () => route.meta.moduleKey,
  () => {
    page.value = 1
    keyword.value = ''
    loadData()
  },
  { immediate: true }
)

async function loadData() {
  if (!module.value) return
  loading.value = true
  try {
    const data = await http.get(module.value.api, {
      params: { page: page.value, size: size.value, keyword: keyword.value }
    })
    rows.value = data.items || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function resetForm(row = {}) {
  Object.keys(form).forEach((key) => delete form[key])
  module.value.fields.forEach((field) => {
    form[field.prop] = row[field.prop] ?? defaultValue(field)
  })
}

function defaultValue(field) {
  if (field.type === 'number') return 0
  if (field.type === 'select') return field.options?.[0] || ''
  return ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  resetForm(row)
  dialogVisible.value = true
}

async function save() {
  if (editingId.value) {
    await http.put(`${module.value.api}/${editingId.value}`, form)
  } else {
    await http.post(module.value.api, form)
  }
  dialogVisible.value = false
  ElMessage.success('保存成功')
  loadData()
}

async function remove(row) {
  await http.delete(`${module.value.api}/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}

async function exportUsers() {
  const response = await http.get('/api/export/users', { responseType: 'blob' })
  const url = URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = 'users.csv'
  anchor.click()
  URL.revokeObjectURL(url)
}

async function importUsers() {
  const data = await http.post('/api/import/users')
  ElMessage.success(data.message)
}

async function openAssignment(row) {
  assignmentRow.value = row
  if (module.value.key === 'users') {
    assignmentTitle.value = `分配角色：${row.username}`
    const roles = await http.get('/api/roles', { params: { page: 1, size: 100 } })
    assignmentOptions.value = (roles.items || []).map((item) => ({ id: item.id, name: item.name }))
    assignmentSelected.value = await http.get(`/api/users/${row.id}/roles`)
    assignmentUrl.value = `/api/users/${row.id}/roles`
  }
  if (module.value.key === 'roles') {
    assignmentTitle.value = `配置权限：${row.name}`
    const permissions = await http.get('/api/permissions', { params: { page: 1, size: 100 } })
    assignmentOptions.value = (permissions.items || []).map((item) => ({ id: item.id, name: `${item.name} (${item.code})` }))
    assignmentSelected.value = await http.get(`/api/roles/${row.id}/permissions`)
    assignmentUrl.value = `/api/roles/${row.id}/permissions`
  }
  assignmentVisible.value = true
}

async function saveAssignment() {
  await http.put(assignmentUrl.value, { ids: assignmentSelected.value })
  assignmentVisible.value = false
  ElMessage.success('配置已保存')
}
</script>
