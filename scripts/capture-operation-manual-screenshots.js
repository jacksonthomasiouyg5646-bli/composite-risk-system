import fs from 'node:fs'
import path from 'node:path'
import { spawn, spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(scriptDir, '..')
const outputDir = path.join(rootDir, 'docs', 'images', 'operation-manual')
const profileDir = path.join(rootDir, '.edge-operation-manual-profile')
const browserPath = process.env.BROWSER_PATH || 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe'
const debugPort = Number(process.env.CDP_PORT || 9231)
const baseUrl = process.env.RISK_FRONTEND_URL || 'http://localhost:5173'
const screenshotCredentials = {
  username: process.env.SCREENSHOT_USERNAME,
  password: process.env.SCREENSHOT_PASSWORD,
  captchaId: process.env.SCREENSHOT_CAPTCHA_ID,
  captchaCode: process.env.SCREENSHOT_CAPTCHA_CODE
}

if (Object.values(screenshotCredentials).some((value) => !value)) {
  throw new Error('SCREENSHOT_USERNAME, SCREENSHOT_PASSWORD, SCREENSHOT_CAPTCHA_ID and SCREENSHOT_CAPTCHA_CODE are required')
}

fs.mkdirSync(outputDir, { recursive: true })
fs.rmSync(profileDir, { recursive: true, force: true })
fs.mkdirSync(profileDir, { recursive: true })

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

class CdpClient {
  constructor(url) {
    this.url = url
    this.nextId = 1
    this.pending = new Map()
  }

  async connect() {
    this.socket = new WebSocket(this.url)
    await new Promise((resolve, reject) => {
      this.socket.addEventListener('open', resolve, { once: true })
      this.socket.addEventListener('error', reject, { once: true })
    })
    this.socket.addEventListener('message', (event) => {
      const message = JSON.parse(String(event.data))
      if (!message.id) return
      const pending = this.pending.get(message.id)
      if (!pending) return
      this.pending.delete(message.id)
      clearTimeout(pending.timer)
      if (message.error) pending.reject(new Error(message.error.message))
      else pending.resolve(message.result || {})
    })
  }

  send(method, params = {}, timeoutMs = 15000) {
    const id = this.nextId++
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.pending.delete(id)
        reject(new Error('CDP command timed out: ' + method))
      }, timeoutMs)
      this.pending.set(id, { resolve, reject, timer })
      this.socket.send(JSON.stringify({ id, method, params }))
    })
  }

  close() {
    if (this.socket) this.socket.close()
  }
}

async function waitForTarget() {
  for (let attempt = 0; attempt < 60; attempt += 1) {
    try {
      const targets = await fetch('http://127.0.0.1:' + debugPort + '/json/list').then((response) => response.json())
      const page = targets.find((target) => target.type === 'page')
      if (page?.webSocketDebuggerUrl) return page
    } catch {
      // Browser is still starting.
    }
    await sleep(250)
  }
  throw new Error('Edge remote debugging target did not start')
}

async function evaluate(client, expression) {
  const result = await client.send('Runtime.evaluate', {
    expression,
    awaitPromise: true,
    returnByValue: true,
    userGesture: true
  })
  if (result.exceptionDetails) {
    throw new Error(result.exceptionDetails.text || 'browser evaluation failed')
  }
  return result.result?.value
}

async function navigate(client, relativeUrl, waitMs = 2600) {
  const pathOnly = relativeUrl.split('?')[0]
  let usedSpaNavigation = false
  try {
    usedSpaNavigation = await evaluate(client, '(() => { const expected=' + JSON.stringify(pathOnly) + '; const link=[...document.querySelectorAll("a[href]")].find((item) => { try { return new URL(item.href).pathname === expected; } catch { return false; } }); if(!link) return false; link.click(); return true; })()')
  } catch {
    usedSpaNavigation = false
  }
  if (!usedSpaNavigation) {
    await client.send('Page.navigate', { url: baseUrl + relativeUrl })
    await sleep(waitMs)
    return
  }
  await sleep(waitMs)
  try {
    await evaluate(client, 'document.readyState')
    await waitForLoading(client)
  } catch {
    await sleep(500)
  }
}

async function waitForLoading(client) {
  for (let attempt = 0; attempt < 30; attempt += 1) {
    const ready = await evaluate(client, '(() => { const masks=[...document.querySelectorAll(".el-loading-mask")]; return masks.every((item) => getComputedStyle(item).display === "none" || getComputedStyle(item).visibility === "hidden" || Number(getComputedStyle(item).opacity) === 0); })()')
    if (ready) return
    await sleep(250)
  }
}

async function clickText(client, selector, text) {
  return evaluate(client, '(() => { const target=[...document.querySelectorAll(' + JSON.stringify(selector) + ')].find((item) => item.textContent.trim().includes(' + JSON.stringify(text) + ')); if(!target) return false; target.click(); return true; })()')
}

async function setInput(client, selector, value) {
  return evaluate(client, '(() => { const input=document.querySelector(' + JSON.stringify(selector) + '); if(!input) return false; const setter=Object.getOwnPropertyDescriptor(input instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype,"value").set; setter.call(input,' + JSON.stringify(value) + '); input.dispatchEvent(new Event("input",{bubbles:true})); input.dispatchEvent(new Event("change",{bubbles:true})); return true; })()')
}

async function screenshot(client, fileName) {
  await evaluate(client, 'window.scrollTo(0,0); true')
  await sleep(250)
  const width = 1440
  const height = 1000
  const result = await client.send('Page.captureScreenshot', {
    format: 'png',
    fromSurface: true,
    captureBeyondViewport: false
  }, 60000)
  const filePath = path.join(outputDir, fileName)
  fs.writeFileSync(filePath, Buffer.from(result.data, 'base64'))
  const captured = { file: filePath, width, height, bytes: fs.statSync(filePath).size }
  console.log('captured ' + fileName + ' ' + captured.bytes + ' bytes')
  return captured
}

async function login(client) {
  const expression = '(async()=>{'
    + 'const response=await fetch("/api/auth/login",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(' + JSON.stringify(screenshotCredentials) + ')});'
    + 'const payload=await response.json(); if(payload.code!==0) throw new Error(payload.message||"login failed");'
    + 'const data=payload.data; localStorage.setItem("token",data.token); localStorage.setItem("profile",JSON.stringify({user:data.user,roles:data.roles,permissions:data.permissions}));'
    + 'return true;})()'
  await evaluate(client, expression)
}

const browser = spawn(browserPath, [
  '--headless=new',
  '--disable-gpu',
  '--hide-scrollbars',
  '--no-first-run',
  '--no-default-browser-check',
  '--disable-default-apps',
  '--disable-extensions',
  '--disable-features=Translate,msEdgeSidebarV2',
  '--remote-debugging-port=' + debugPort,
  '--remote-allow-origins=*',
  '--user-data-dir=' + profileDir,
  '--window-size=1440,1000',
  '--force-device-scale-factor=1',
  baseUrl + '/login'
], { stdio: 'ignore', windowsHide: true })

const captured = []

try {
  const target = await waitForTarget()
  const client = new CdpClient(target.webSocketDebuggerUrl)
  await client.connect()
  await client.send('Page.enable')
  await client.send('Runtime.enable')
  await client.send('Network.enable')
  await client.send('Emulation.setDeviceMetricsOverride', {
    width: 1440,
    height: 1000,
    deviceScaleFactor: 1,
    mobile: false
  })

  await sleep(1800)
  await waitForLoading(client)
  captured.push(await screenshot(client, '01-login.png'))

  await login(client)
  await client.send('Page.navigate', { url: baseUrl + '/' })
  await sleep(4200)
  await waitForLoading(client)
  captured.push(await screenshot(client, '02-dashboard.png'))

  await navigate(client, '/risks/credit-domain-query', 2600)
  await setInput(client, 'input[placeholder="综合关键字"]', 'CUST202607210001')
  await clickText(client, 'button', '查询')
  await sleep(2200)
  await waitForLoading(client)
  captured.push(await screenshot(client, '03-credit-query.png'))

  await navigate(client, '/risks/ledgers', 3000)
  captured.push(await screenshot(client, '04-risk-ledger.png'))

  await navigate(client, '/risks/default-trends', 3200)
  captured.push(await screenshot(client, '05-default-trends.png'))

  await navigate(client, '/risks/lgd-center', 3600)
  captured.push(await screenshot(client, '06-lgd-overview.png'))
  await clickText(client, '.el-tabs__item', '债项 LGD 台账')
  await sleep(2400)
  await waitForLoading(client)
  captured.push(await screenshot(client, '07-lgd-ledger.png'))

  await navigate(client, '/risks/portfolio-management', 3400)
  captured.push(await screenshot(client, '08-portfolio-limits.png'))
  await clickText(client, '.el-tabs__item', 'PD/LGD/EAD 回溯')
  await sleep(1000)
  captured.push(await screenshot(client, '09-portfolio-backtest.png'))
  await clickText(client, '.el-tabs__item', '预警处置效果')
  await sleep(1000)
  captured.push(await screenshot(client, '10-portfolio-effectiveness.png'))

  await clickText(client, '.el-tabs__item', '限额前瞻')
  await sleep(1200)
  captured.push(await screenshot(client, '20-portfolio-forecast.png'))
  await clickText(client, '.el-tabs__item', '压力测试')
  await sleep(1200)
  captured.push(await screenshot(client, '21-portfolio-stress.png'))
  await clickText(client, '.el-tabs__item', '集团客户风险')
  await sleep(1200)
  captured.push(await screenshot(client, '22-portfolio-groups.png'))
  await clickText(client, '.el-tabs__item', '模型生命周期')
  await sleep(1200)
  captured.push(await screenshot(client, '23-model-lifecycle.png'))

  await navigate(client, '/risks/month-end-analysis', 4000)
  captured.push(await screenshot(client, '24-month-end-overview.png'))
  await clickText(client, '.el-tabs__item', '组合变动归因')
  await sleep(1200)
  captured.push(await screenshot(client, '25-month-end-attribution.png'))
  await clickText(client, '.el-tabs__item', '变化逐级下钻')
  await sleep(1200)
  captured.push(await screenshot(client, '26-month-end-drilldown.png'))
  await clickText(client, '.el-tabs__item', '批次与数据质量')
  await sleep(1200)
  captured.push(await screenshot(client, '27-month-end-quality.png'))

  await navigate(client, '/risks/alert-cases', 3600)
  captured.push(await screenshot(client, '11-alert-cases.png'))

  await navigate(client, '/risks/ai-assistant', 2200)
  await setInput(client, 'input[placeholder*="CUST"]', 'CUST202607210001')
  await setInput(client, 'textarea', '该客户当前主要风险、未来30天趋势和建议措施是什么？')
  await clickText(client, 'button', '开始分析')
  await sleep(5200)
  await waitForLoading(client)
  captured.push(await screenshot(client, '12-ai-analysis.png'))

  await navigate(client, '/risks/data-governance', 3200)
  captured.push(await screenshot(client, '13-data-governance.png'))

  await navigate(client, '/risks/model-governance', 3400)
  captured.push(await screenshot(client, '14-model-governance.png'))

  await navigate(client, '/risks/relationship-graph', 3400)
  captured.push(await screenshot(client, '15-relationship-graph.png'))

  await navigate(client, '/risks/management-reports', 3000)
  captured.push(await screenshot(client, '16-management-reports.png'))

  client.close()
} finally {
  spawnSync('taskkill', ['/pid', String(browser.pid), '/T', '/F'], { stdio: 'ignore', windowsHide: true })
}

console.log(JSON.stringify({ outputDir, count: captured.length, screenshots: captured }))
