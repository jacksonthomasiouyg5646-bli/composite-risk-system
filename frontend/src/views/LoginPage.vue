<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="brand-block">
        <h1>组合风险系统</h1>
        <p>企业风险识别、评估、整改与监控平台</p>
      </div>
      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" size="large" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" size="large" type="password" autocomplete="current-password" show-password />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" size="large" />
            <img v-if="captcha.image" :src="captcha.image" alt="captcha" />
          </div>
        </el-form-item>
        <el-button class="login-button" type="primary" size="large" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const captcha = reactive({ captchaId: '', image: '' })
const form = reactive({
  username: '',
  password: '',
  captchaId: '',
  captchaCode: ''
})

onMounted(loadCaptcha)

async function loadCaptcha() {
  const data = await http.get('/api/auth/captcha')
  captcha.captchaId = data.captchaId
  captcha.image = data.image
  form.captchaId = data.captchaId
  form.captchaCode = ''
}

async function handleLogin() {
  loading.value = true
  try {
    await auth.login(form)
    await router.replace('/')
  } catch (error) {
    await loadCaptcha().catch(() => {})
    throw error
  } finally {
    loading.value = false
  }
}
</script>
