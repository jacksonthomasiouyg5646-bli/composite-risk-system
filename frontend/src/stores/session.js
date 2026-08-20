export function getStoredToken() {
  return localStorage.getItem('token') || ''
}

export function getStoredProfile() {
  const raw = localStorage.getItem('profile')
  if (!raw) return null

  try {
    return JSON.parse(raw)
  } catch {
    localStorage.removeItem('profile')
    return null
  }
}

export function saveSession(token, profile) {
  localStorage.setItem('token', token)
  localStorage.setItem('profile', JSON.stringify(profile))
}

export function saveProfile(profile) {
  localStorage.setItem('profile', JSON.stringify(profile))
}

export function clearSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('profile')
}
