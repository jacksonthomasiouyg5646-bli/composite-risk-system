const fs = require('fs')
const path = require('path')

const projectRoot = path.resolve(__dirname, '..')
const outDir = path.join(projectRoot, 'docs')
const pptxPath = path.join(outDir, '风险管理系统介绍.pptx')

function xmlEscape(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

function textShape(id, x, y, w, h, text, size, color, bold = false) {
  const b = bold ? '<a:b/>' : ''
  return `
<p:sp>
  <p:nvSpPr><p:cNvPr id="${id}" name="TextBox ${id}"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
  <p:spPr>
    <a:xfrm><a:off x="${x}" y="${y}"/><a:ext cx="${w}" cy="${h}"/></a:xfrm>
    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
    <a:noFill/><a:ln><a:noFill/></a:ln>
  </p:spPr>
  <p:txBody>
    <a:bodyPr wrap="square" anchor="t"/><a:lstStyle/>
    <a:p><a:pPr algn="l"/><a:r><a:rPr lang="zh-CN" sz="${size * 100}" dirty="0">${b}<a:solidFill><a:srgbClr val="${color}"/></a:solidFill><a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/></a:rPr><a:t>${xmlEscape(text)}</a:t></a:r></a:p>
  </p:txBody>
</p:sp>`
}

function bulletShape(id, x, y, w, h, items, size, color) {
  const paragraphs = items.map((item) => `
<a:p>
  <a:pPr marL="260000" indent="-180000"><a:buChar char="•"/></a:pPr>
  <a:r><a:rPr lang="zh-CN" sz="${size * 100}" dirty="0"><a:solidFill><a:srgbClr val="${color}"/></a:solidFill><a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/></a:rPr><a:t>${xmlEscape(item)}</a:t></a:r>
</a:p>`).join('')
  return `
<p:sp>
  <p:nvSpPr><p:cNvPr id="${id}" name="Bullets ${id}"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
  <p:spPr>
    <a:xfrm><a:off x="${x}" y="${y}"/><a:ext cx="${w}" cy="${h}"/></a:xfrm>
    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
    <a:noFill/><a:ln><a:noFill/></a:ln>
  </p:spPr>
  <p:txBody><a:bodyPr wrap="square" anchor="t"/><a:lstStyle/>${paragraphs}</p:txBody>
</p:sp>`
}

function rectShape(id, x, y, w, h, text, fill = '168477') {
  return `
<p:sp>
  <p:nvSpPr><p:cNvPr id="${id}" name="Box ${id}"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
  <p:spPr>
    <a:xfrm><a:off x="${x}" y="${y}"/><a:ext cx="${w}" cy="${h}"/></a:xfrm>
    <a:prstGeom prst="roundRect"><a:avLst/></a:prstGeom>
    <a:solidFill><a:srgbClr val="${fill}"/></a:solidFill>
    <a:ln w="12000"><a:solidFill><a:srgbClr val="0F766E"/></a:solidFill></a:ln>
  </p:spPr>
  <p:txBody>
    <a:bodyPr anchor="ctr"/><a:lstStyle/>
    <a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="1700" dirty="0"><a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill><a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/></a:rPr><a:t>${xmlEscape(text)}</a:t></a:r></a:p>
  </p:txBody>
</p:sp>`
}

const slides = [
  {
    title: '风险管理系统介绍',
    subtitle: '分布式企业风险管理平台：从风险识别到整改闭环、通知协同、监控运维',
    bullets: [
      '项目目标：构建一套可本地 Docker 部署、可扩展的风险管理系统',
      '面向对象：系统管理员、风险管理人员、审计人员、业务负责人',
      '核心价值：集中管理风险台账、评估、控制措施、整改任务、事件、指标和通知'
    ]
  },
  {
    title: '建设背景与目标',
    subtitle: '从用户管理系统演进为风险管理系统',
    bullets: [
      '原始项目以用户、角色、权限、部门、菜单为基础',
      '演进后扩展风险业务模块，并保留 RBAC、审计日志、配置、通知等后台能力',
      '目标是形成具备登录认证、配置中心、缓存会话、消息通知、日志监控的分布式平台'
    ]
  },
  {
    title: '制作过程',
    subtitle: '从项目生成到容器化部署的主要步骤',
    bullets: [
      '生成 Spring Boot + Spring Cloud 多模块后端和 Vue 3 管理前端',
      '改造为 Spring MVC 分层范式：Controller、Service、Mapper、mapper.xml',
      '接入 MySQL、Redis、Apollo、RocketMQ、Dozzle、Prometheus、Grafana',
      '完成 Docker Compose 部署、Apollo 配置发布和启动强校验',
      '持续排查并修复登录、网关转发、乱码、MQ、日志与监控问题'
    ]
  },
  {
    title: '系统包含的业务内容',
    subtitle: '围绕风险全生命周期的功能模块',
    boxes: ['风险台账', '风险评估', '控制措施', '整改任务', '风险事件', '风险指标', '用户与账号', '角色权限', '审计日志', '风险参数', '风险通知']
  },
  {
    title: '核心功能说明',
    subtitle: '业务侧能力',
    bullets: [
      '风险台账：记录风险编号、名称、分类、等级、责任部门、责任人、状态和描述',
      '风险评估：记录发生可能性、影响程度、固有风险、剩余风险和评估结论',
      '控制措施：维护控制编号、关联风险、控制类型、执行频率、有效性和状态',
      '整改任务：跟踪整改措施、责任人、截止日期、进度和处理状态',
      '风险通知：通过系统通知和邮件通知进行协同提醒'
    ]
  },
  {
    title: '管理与安全能力',
    subtitle: '平台基础能力',
    bullets: [
      '认证登录：JWT + RSA 加密，默认会话 15 分钟',
      '权限控制：基于角色和权限点控制菜单与按钮',
      '用户管理：支持用户、角色、权限等后台基础数据维护',
      '审计日志：记录操作日志、接口请求、SQL 执行和异常信息',
      'Redis 会话：token 校验依赖 Redis，保障会话态可控'
    ]
  },
  {
    title: '后端框架结构',
    subtitle: 'Spring Boot + Spring Cloud 微服务',
    boxes: ['discovery-server\nEureka 注册中心', 'api-gateway\n统一入口与鉴权', 'auth-service\n认证登录与 token', 'user-service\n用户角色权限', 'system-service\n风险与系统模块', 'common\n通用安全与工具']
  },
  {
    title: '前端框架结构',
    subtitle: 'Vue 3 + Vite + Element Plus',
    bullets: [
      '登录页：账号密码验证码登录，保存 token 和用户信息',
      '主布局：左侧菜单、顶部用户信息、刷新和退出',
      '通用 CRUD 页面：通过模块配置渲染表格、表单、分页和操作按钮',
      '权限控制：根据后端返回权限点过滤菜单与操作',
      '接口统一：Axios 统一走 /api/**，由前端容器代理到网关'
    ]
  },
  {
    title: '部署结构',
    subtitle: 'Docker Compose 本地分布式环境',
    bullets: [
      '前端容器：risk-frontend，访问端口 5173',
      '网关容器：risk-api-gateway，访问端口 8088',
      '业务服务：auth-service、user-service、system-service',
      '基础设施：MySQL、Redis、Apollo、RocketMQ、Dozzle、Prometheus、Grafana',
      '服务注册：Eureka 端口 8761，业务服务注册后由网关转发'
    ]
  },
  {
    title: 'Apollo 配置中心',
    subtitle: '启动前强校验，配置未发布不允许启动',
    bullets: [
      'Docker 默认开启 APOLLO_ENABLED=true，并要求 APOLLO_REQUIRED=true',
      '每个服务启动前检查 Apollo meta 地址和必需 namespace',
      'Apollo 中发布 Docker 可用地址：discovery-server、mysql、redis、rocketmq-namesrv',
      '配置内容包括数据库、Redis、RSA、网关路由、MQ、监控端点等',
      'Apollo 停止或 namespace 为空时，服务会在启动前失败'
    ]
  },
  {
    title: '日志与监控',
    subtitle: '面向运行排障的可观测能力',
    bullets: [
      'Log4j2 输出 INFO 日志，包含时间、接口、线程 ID、事务开始/结束 ID',
      'MyBatis SQL 日志输出 SQL 语句，便于排查数据问题',
      'Dozzle 用于查看容器标准输出日志',
      'Prometheus 采集 /actuator/prometheus 指标',
      'Grafana 用于展示服务运行状态和资源趋势'
    ]
  },
  {
    title: '消息通知与邮件',
    subtitle: 'RocketMQ 用于邮件通知分发',
    bullets: [
      '新增通知时可通过 MQ 发布邮件任务',
      'system-service 负责消息生产和消费',
      'RocketMQ 配置放入 Apollo 的 mq namespace',
      '消费者启动失败时只记录错误，系统服务继续运行',
      '邮件真实发送开关通过 app.mail.send-enabled 控制'
    ]
  },
  {
    title: '当前状态与后续规划',
    subtitle: '基础版已具备完整演示能力，后续可继续增强',
    bullets: [
      '已具备：登录认证、风险模块、用户权限、配置中心、缓存会话、MQ、日志监控、Docker 部署',
      '建议补强：前端文件统一 UTF-8 清理、接口自动化测试、业务字段校验、异常统一展示',
      '生产化方向：高可用注册中心、配置灰度、消息重试策略、数据库备份、链路追踪和告警规则',
      '交付物：源代码、Docker Compose、Apollo 配置脚本、数据库脚本、部署手册和本 PPT'
    ]
  }
]

function slideXml(slide, idx) {
  let id = 2
  let shapes = textShape(id++, 520000, 330000, 11600000, 700000, slide.title, 30, '102033', true)
  shapes += textShape(id++, 550000, 970000, 11200000, 520000, slide.subtitle || '', 15, '52657A')
  if (slide.bullets) shapes += bulletShape(id++, 720000, 1680000, 11200000, 4300000, slide.bullets, 17, '263445')
  if (slide.boxes) {
    let x = 750000
    let y = 1850000
    for (const box of slide.boxes) {
      shapes += rectShape(id++, x, y, 2500000, 620000, box)
      x += 2850000
      if (x > 9000000) {
        x = 750000
        y += 900000
      }
    }
  }
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg><p:bgPr><a:solidFill><a:srgbClr val="F7FAFC"/></a:solidFill><a:effectLst/></p:bgPr></p:bg>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
      ${shapes}
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>`
}

const files = {}
files['[Content_Types].xml'] = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  ${slides.map((_, i) => `<Override PartName="/ppt/slides/slide${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>`).join('\n  ')}
</Types>`
files['_rels/.rels'] = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>`
files['docProps/core.xml'] = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>风险管理系统介绍</dc:title><dc:creator>Codex</dc:creator><cp:lastModifiedBy>Codex</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">2026-07-15T00:00:00Z</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">2026-07-15T00:00:00Z</dcterms:modified>
</cp:coreProperties>`
files['docProps/app.xml'] = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>Microsoft PowerPoint</Application><PresentationFormat>Widescreen</PresentationFormat><Slides>${slides.length}</Slides></Properties>`
files['ppt/presentation.xml'] = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldIdLst>${slides.map((_, i) => `<p:sldId id="${256 + i}" r:id="rId${i + 1}"/>`).join('')}</p:sldIdLst>
  <p:sldSz cx="12192000" cy="6858000" type="wide"/><p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>`
files['ppt/_rels/presentation.xml.rels'] = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  ${slides.map((_, i) => `<Relationship Id="rId${i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide${i + 1}.xml"/>`).join('\n  ')}
</Relationships>`
slides.forEach((slide, i) => {
  files[`ppt/slides/slide${i + 1}.xml`] = slideXml(slide, i + 1)
})

const crcTable = new Uint32Array(256)
for (let n = 0; n < 256; n++) {
  let c = n
  for (let k = 0; k < 8; k++) c = ((c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1))
  crcTable[n] = c >>> 0
}

function crc32(buf) {
  let c = 0xFFFFFFFF
  for (const b of buf) c = crcTable[(c ^ b) & 0xFF] ^ (c >>> 8)
  return (c ^ 0xFFFFFFFF) >>> 0
}

function dosTimeDate(date) {
  const time = (date.getHours() << 11) | (date.getMinutes() << 5) | Math.floor(date.getSeconds() / 2)
  const day = date.getDate()
  const month = date.getMonth() + 1
  const year = Math.max(date.getFullYear() - 1980, 0)
  const dosDate = (year << 9) | (month << 5) | day
  return { time, date: dosDate }
}

function makeZip(fileMap) {
  const chunks = []
  const central = []
  let offset = 0
  const now = dosTimeDate(new Date())
  for (const [name, content] of Object.entries(fileMap)) {
    const nameBuf = Buffer.from(name.replace(/\\/g, '/'))
    const data = Buffer.from(content, 'utf8')
    const crc = crc32(data)
    const local = Buffer.alloc(30)
    local.writeUInt32LE(0x04034b50, 0)
    local.writeUInt16LE(20, 4)
    local.writeUInt16LE(0x0800, 6)
    local.writeUInt16LE(0, 8)
    local.writeUInt16LE(now.time, 10)
    local.writeUInt16LE(now.date, 12)
    local.writeUInt32LE(crc, 14)
    local.writeUInt32LE(data.length, 18)
    local.writeUInt32LE(data.length, 22)
    local.writeUInt16LE(nameBuf.length, 26)
    local.writeUInt16LE(0, 28)
    chunks.push(local, nameBuf, data)

    const cent = Buffer.alloc(46)
    cent.writeUInt32LE(0x02014b50, 0)
    cent.writeUInt16LE(20, 4)
    cent.writeUInt16LE(20, 6)
    cent.writeUInt16LE(0x0800, 8)
    cent.writeUInt16LE(0, 10)
    cent.writeUInt16LE(now.time, 12)
    cent.writeUInt16LE(now.date, 14)
    cent.writeUInt32LE(crc, 16)
    cent.writeUInt32LE(data.length, 20)
    cent.writeUInt32LE(data.length, 24)
    cent.writeUInt16LE(nameBuf.length, 28)
    cent.writeUInt16LE(0, 30)
    cent.writeUInt16LE(0, 32)
    cent.writeUInt16LE(0, 34)
    cent.writeUInt16LE(0, 36)
    cent.writeUInt32LE(0, 38)
    cent.writeUInt32LE(offset, 42)
    central.push(cent, nameBuf)
    offset += local.length + nameBuf.length + data.length
  }
  const centralStart = offset
  const centralBuf = Buffer.concat(central)
  const end = Buffer.alloc(22)
  end.writeUInt32LE(0x06054b50, 0)
  end.writeUInt16LE(0, 4)
  end.writeUInt16LE(0, 6)
  end.writeUInt16LE(Object.keys(fileMap).length, 8)
  end.writeUInt16LE(Object.keys(fileMap).length, 10)
  end.writeUInt32LE(centralBuf.length, 12)
  end.writeUInt32LE(centralStart, 16)
  end.writeUInt16LE(0, 20)
  return Buffer.concat([...chunks, centralBuf, end])
}

fs.mkdirSync(outDir, { recursive: true })
fs.writeFileSync(pptxPath, makeZip(files))
console.log(pptxPath)
