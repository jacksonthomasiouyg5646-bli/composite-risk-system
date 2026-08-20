import { defineStore } from 'pinia'
import http from '../api/http'
import { clearSession, getStoredProfile, getStoredToken, saveProfile, saveSession } from './session'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getStoredToken(),
    profile: getStoredProfile()
  }),
  getters: {
    permissions: (state) => Array.from(new Set(state.profile?.permissions || [])),
    user: (state) => state.profile?.user || null
  },
  actions: {
    async login(payload) {
      const data = await http.post('/api/auth/login', payload)
      if (!data?.token) {
        throw new Error('login response missing token')
      }
      this.token = data.token
      this.profile = {
        user: data.user,
        roles: data.roles,
        permissions: data.permissions
      }
      saveSession(data.token, this.profile)
    },
    async loadProfile() {
      const data = await http.get('/api/auth/profile')
      this.profile = data
      saveProfile(data)
    },
    syncFromStorage() {
      this.token = getStoredToken()
      this.profile = getStoredProfile()
    },
    clearLocal() {
      this.token = ''
      this.profile = null
      clearSession()
    },
    async logout() {
      if (this.token) {
        await http.post('/api/auth/logout').catch(() => {})
      }
      this.clearLocal()
    },
    hasPermission(permission) {
      return !permission || this.permissions.includes(permission)
    }
  }
})
