import fs from 'node:fs'
import path from 'node:path'
import { spawn, spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(scriptDir, '..')
const profileDir = path.join(rootDir, '.operation-manual-chrome-profile')
const browserPath = process.env.BROWSER_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const debugPort = Number(process.env.CDP_PORT || 9233)
const baseUrl = process.env.RISK_FRONTEND_URL || 'http://localhost:5173'

fs.rmSync(profileDir, { recursive: true, force: true })
fs.mkdirSync(profileDir, { recursive: true })

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function target() {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    try {
      const pages = await fetch('http://127.0.0.1:' + debugPort + '/json/list').then((response) => response.json())
      const page = pages.find((item) => item.type === 'page' && String(item.url || '').startsWith(baseUrl))
        || pages.find((item) => item.type === 'page')
      if (page) return page
    } catch {
      // Browser is starting.
    }
    await sleep(250)
  }
  throw new Error('Chrome debugging target unavailable')
}

async function evaluate(ws, expression) {
  const id = Math.floor(Math.random() * 1000000) + 1
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('browser evaluation timed out')), 15000)
    const handler = (event) => {
      const message = JSON.parse(String(event.data))
      if (message.id !== id) return
      clearTimeout(timer)
      ws.removeEventListener('message', handler)
      if (message.error || message.result?.exceptionDetails) reject(new Error(message.error?.message || 'browser evaluation failed'))
      else resolve(message.result?.result?.value)
    }
    ws.addEventListener('message', handler)
    ws.send(JSON.stringify({
      id,
      method: 'Runtime.evaluate',
      params: { expression, awaitPromise: true, returnByValue: true, userGesture: true }
    }))
  })
}

const browser = spawn(browserPath, [
  '--headless=new',
  '--disable-gpu',
  '--no-first-run',
  '--no-default-browser-check',
  '--remote-debugging-port=' + debugPort,
  '--remote-allow-origins=*',
  '--user-data-dir=' + profileDir,
  '--window-size=1440,1000',
  baseUrl + '/login'
], { stdio: 'ignore', windowsHide: true })

let gracefulCloseRequested = false
try {
  const page = await target()
  console.log(JSON.stringify({ targetUrl: page.url, title: page.title }))
  const ws = new WebSocket(page.webSocketDebuggerUrl)
  await new Promise((resolve, reject) => {
    ws.addEventListener('open', resolve, { once: true })
    ws.addEventListener('error', reject, { once: true })
  })
  await sleep(5000)
  console.log(JSON.stringify(await evaluate(ws, '({title:document.title,ready:document.readyState,body:(document.body?.innerText||"").slice(0,300)})')))
  const clicked = await evaluate(ws, '(() => { const button=[...document.querySelectorAll("button")].find((item) => item.textContent.trim()==="登录"); if(!button) return false; button.click(); return true; })()')
  if (!clicked) throw new Error('Login button was not found')
  await sleep(5000)
  const state = await evaluate(ws, '({path:location.pathname,hasToken:Boolean(localStorage.getItem("token")),hasProfile:Boolean(localStorage.getItem("profile"))})')
  console.log(JSON.stringify({ profileDir, state }))
  ws.send(JSON.stringify({ id: 9999999, method: 'Browser.close' }))
  gracefulCloseRequested = true
  await sleep(4000)
} finally {
  if (!gracefulCloseRequested || browser.exitCode === null) {
    spawnSync('taskkill', ['/pid', String(browser.pid), '/T', '/F'], { stdio: 'ignore', windowsHide: true })
  }
}
