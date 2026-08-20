const http = require('http')
const fs = require('fs')
const path = require('path')

const root = '/app'
const gatewayHost = process.env.GATEWAY_HOST || 'api-gateway'
const gatewayPort = Number(process.env.GATEWAY_PORT || 8088)

const types = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.ico': 'image/x-icon'
}

function proxy(req, res) {
  const options = {
    hostname: gatewayHost,
    port: gatewayPort,
    path: req.url,
    method: req.method,
    headers: { ...req.headers, host: `${gatewayHost}:${gatewayPort}` }
  }
  const upstream = http.request(options, (upstreamRes) => {
    res.writeHead(upstreamRes.statusCode || 502, upstreamRes.headers)
    upstreamRes.pipe(res)
  })
  upstream.on('error', (error) => {
    res.writeHead(502, { 'content-type': 'application/json; charset=utf-8' })
    res.end(JSON.stringify({ code: 502, message: error.message, data: null }))
  })
  req.pipe(upstream)
}

http.createServer((req, res) => {
  if ((req.url || '').startsWith('/api/')) {
    proxy(req, res)
    return
  }

  const urlPath = decodeURIComponent((req.url || '/').split('?')[0])
  const candidate = path.normalize(path.join(root, urlPath))
  let file = candidate.startsWith(root) ? candidate : path.join(root, 'index.html')
  if (!fs.existsSync(file) || fs.statSync(file).isDirectory()) {
    file = path.join(root, 'index.html')
  }

  const ext = path.extname(file)
  res.writeHead(200, { 'content-type': types[ext] || 'application/octet-stream' })
  fs.createReadStream(file).pipe(res)
}).listen(80)
