import fs from 'node:fs'
import path from 'node:path'
import { spawn, spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(scriptDir, '..')
const baseProfile = path.join(rootDir, '.operation-manual-chrome-profile')
const outputDir = path.join(rootDir, 'docs', 'images', 'operation-manual')
const browserPath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const baseUrl = 'http://localhost:5173'

const tasks = [
  { path: '/risks/lgd-center', tab: '债项 LGD 台账', file: '07-lgd-ledger.png' },
  { path: '/risks/portfolio-management', tab: 'PD/LGD/EAD 回溯', file: '09-portfolio-backtest.png' },
  { path: '/risks/portfolio-management', tab: '预警处置效果', file: '10-portfolio-effectiveness.png' },
  { path: '/risks/ai-assistant', action: 'ai', file: '12-ai-analysis.png' }
]

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

class Cdp {
  constructor(url) {
    this.url = url
    this.id = 1
    this.pending = new Map()
  }

  async connect() {
    this.ws = new WebSocket(this.url)
    await new Promise((resolve, reject) => {
      this.ws.addEventListener('open', resolve, { once: true })
      this.ws.addEventListener('error', reject, { once: true })
    })
    this.ws.addEventListener('message', (event) => {
      const message = JSON.parse(String(event.data))
      const pending = this.pending.get(message.id)
      if (!pending) return
      this.pending.delete(message.id)
      clearTimeout(pending.timer)
      if (message.error || message.result?.exceptionDetails) pending.reject(new Error(message.error?.message || 'CDP command failed'))
      else pending.resolve(message.result || {})
    })
  }

  send(method, params = {}, timeoutMs = 30000) {
    const id = this.id++
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.pending.delete(id)
        reject(new Error('CDP command timed out: ' + method))
      }, timeoutMs)
      this.pending.set(id, { resolve, reject, timer })
      this.ws.send(JSON.stringify({ id, method, params }))
    })
  }
}

async function pageTarget(port, expectedUrl) {
  for (let attempt = 0; attempt < 80; attempt += 1) {
    try {
      const pages = await fetch('http://127.0.0.1:' + port + '/json/list').then((response) => response.json())
      const page = pages.find((item) => item.type === 'page' && String(item.url || '').startsWith(expectedUrl))
      if (page) return page
    } catch {
      // Browser is starting.
    }
    await sleep(250)
  }
  throw new Error('Chrome page target unavailable: ' + expectedUrl)
}

for (let index = 0; index < tasks.length; index += 1) {
  const task = tasks[index]
  const port = 9240 + index
  const profile = path.join(rootDir, '.operation-manual-tab-profile-' + (index + 1))
  fs.rmSync(profile, { recursive: true, force: true })
  fs.cpSync(baseProfile, profile, { recursive: true })
  const url = baseUrl + task.path
  const browser = spawn(browserPath, [
    '--headless=new',
    '--disable-gpu',
    '--hide-scrollbars',
    '--no-first-run',
    '--no-default-browser-check',
    '--remote-debugging-port=' + port,
    '--remote-allow-origins=*',
    '--user-data-dir=' + profile,
    '--window-size=1440,1000',
    url
  ], { stdio: 'ignore', windowsHide: true })

  let client
  try {
    const page = await pageTarget(port, url)
    client = new Cdp(page.webSocketDebuggerUrl)
    await client.connect()
    await client.send('Page.enable')
    await client.send('Runtime.enable')
    await sleep(5500)
    if (task.action === 'ai') {
      const result = await client.send('Runtime.evaluate', {
        expression: '(() => { const input=document.querySelector("input[placeholder*=CUST]"); const area=document.querySelector("textarea"); if(!input||!area)return false; const inputSetter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,"value").set; const areaSetter=Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype,"value").set; inputSetter.call(input,"CUST202607210001"); input.dispatchEvent(new Event("input",{bubbles:true})); areaSetter.call(area,"该客户当前主要风险、未来30天趋势和建议措施是什么？"); area.dispatchEvent(new Event("input",{bubbles:true})); const button=[...document.querySelectorAll("button")].find((item)=>item.textContent.includes("开始分析")); if(!button)return false; button.click(); return true; })()',
        returnByValue: true,
        userGesture: true
      })
      if (!result.result?.value) throw new Error('AI analysis form was not found')
      await sleep(7500)
    } else {
      const result = await client.send('Runtime.evaluate', {
        expression: '(() => { const target=[...document.querySelectorAll(".el-tabs__item")].find((item) => item.textContent.trim().includes(' + JSON.stringify(task.tab) + ')); if(!target) return false; target.click(); return true; })()',
        returnByValue: true,
        userGesture: true
      })
      if (!result.result?.value) throw new Error('Tab was not found: ' + task.tab)
      await sleep(3000)
    }
    const shot = await client.send('Page.captureScreenshot', {
      format: 'png',
      fromSurface: true,
      captureBeyondViewport: false
    }, 60000)
    const target = path.join(outputDir, task.file)
    fs.writeFileSync(target, Buffer.from(shot.data, 'base64'))
    console.log('Captured ' + task.file + ' ' + fs.statSync(target).size + ' bytes')
    await client.send('Browser.close', {}, 10000).catch(() => {})
    await sleep(1500)
  } finally {
    if (browser.exitCode === null) {
      spawnSync('taskkill', ['/pid', String(browser.pid), '/T', '/F'], { stdio: 'ignore', windowsHide: true })
    }
  }
}
