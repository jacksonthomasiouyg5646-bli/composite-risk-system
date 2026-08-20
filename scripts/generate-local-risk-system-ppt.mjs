import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';

const projectRoot = path.resolve(process.cwd());
const outDir = path.join(projectRoot, 'docs');
const businessMode = process.argv.includes('--business');
const outFile = process.env.PPT_OUT || path.join(outDir, businessMode ? '组合风险系统业务介绍.pptx' : '本地风险管理系统建设过程与架构说明.pptx');

const EMU = 914400;
const PT = 12700;
const W = 13.333;
const H = 7.5;

const C = {
  bg: 'F7F9FB',
  white: 'FFFFFF',
  ink: '0F172A',
  text: '334155',
  muted: '64748B',
  line: 'CBD5E1',
  softLine: 'E2E8F0',
  teal: '0F766E',
  blue: '1D4ED8',
  amber: 'D97706',
  rose: 'BE123C',
  violet: '6D28D9',
  green: '15803D',
  red: 'DC2626',
  slate100: 'F1F5F9',
  paleTeal: 'CCFBF1',
  paleBlue: 'DBEAFE',
  paleAmber: 'FEF3C7',
  paleRose: 'FFE4E6',
  paleViolet: 'EDE9FE',
  paleGreen: 'DCFCE7'
};

function esc(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

function emu(v) {
  return Math.round(v * EMU);
}

function pt(v) {
  return Math.round(v * PT);
}

function solidFill(color, alpha) {
  if (!color) return '<a:noFill/>';
  const alphaXml = alpha === undefined ? '' : `<a:alpha val="${alpha}"/>`;
  return `<a:solidFill><a:srgbClr val="${color}">${alphaXml}</a:srgbClr></a:solidFill>`;
}

function lineXml(line) {
  if (line === false) return '<a:ln><a:noFill/></a:ln>';
  const opt = line || {};
  const color = opt.color || C.softLine;
  const width = opt.width === undefined ? 1 : opt.width;
  const dash = opt.dash ? '<a:prstDash val="dash"/>' : '<a:prstDash val="solid"/>';
  const arrow = opt.arrow ? '<a:tailEnd type="triangle"/>' : '';
  return `<a:ln w="${pt(width)}">${solidFill(color)}${dash}${arrow}</a:ln>`;
}

function shadowXml(enabled) {
  if (!enabled) return '<a:effectLst/>';
  return '<a:effectLst><a:outerShdw blurRad="76200" dist="25400" dir="5400000" algn="tl" rotWithShape="0"><a:srgbClr val="0F172A"><a:alpha val="9000"/></a:srgbClr></a:outerShdw></a:effectLst>';
}

function runProps(p, base) {
  const size = Math.round((p.size || base.size || 12) * 100);
  const bold = p.bold || base.bold ? ' b="1"' : '';
  const color = p.color || base.color || C.text;
  return `<a:rPr lang="zh-CN" sz="${size}"${bold}>${solidFill(color)}<a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/><a:cs typeface="Microsoft YaHei"/></a:rPr>`;
}

function paragraphsXml(paragraphs, base = {}) {
  const list = Array.isArray(paragraphs) ? paragraphs : [paragraphs];
  return list.map((item) => {
    const p = typeof item === 'string' ? { text: item } : item;
    const align = p.align || base.align || 'l';
    const before = p.before === undefined ? 0 : p.before;
    const after = p.after === undefined ? 180 : p.after;
    const bulletIndent = p.bullet ? ' marL="285750" indent="-171450"' : '';
    const bullet = p.bullet ? '<a:buChar char="•"/>' : '';
    return `<a:p><a:pPr algn="${align}"${bulletIndent}><a:spcBef><a:spcPts val="${before}"/></a:spcBef><a:spcAft><a:spcPts val="${after}"/></a:spcAft>${bullet}</a:pPr><a:r>${runProps(p, base)}<a:t>${esc(p.text)}</a:t></a:r></a:p>`;
  }).join('');
}

function txBody(paragraphs, opts = {}) {
  const anchor = opts.anchor || 't';
  const l = opts.lIns === undefined ? 91440 : opts.lIns;
  const r = opts.rIns === undefined ? 91440 : opts.rIns;
  const t = opts.tIns === undefined ? 45720 : opts.tIns;
  const b = opts.bIns === undefined ? 45720 : opts.bIns;
  return `<p:txBody><a:bodyPr wrap="square" anchor="${anchor}" lIns="${l}" rIns="${r}" tIns="${t}" bIns="${b}"/><a:lstStyle/>${paragraphsXml(paragraphs, opts)}</p:txBody>`;
}

function shape(id, opt) {
  const geom = opt.type || 'rect';
  const fill = opt.fill === undefined ? C.white : opt.fill;
  const text = opt.text || opt.paragraphs;
  const tx = text ? txBody(text, {
    size: opt.fontSize,
    color: opt.fontColor,
    bold: opt.bold,
    align: opt.align,
    anchor: opt.valign || opt.anchor,
    lIns: opt.lIns,
    rIns: opt.rIns,
    tIns: opt.tIns,
    bIns: opt.bIns
  }) : '';
  return `<p:sp><p:nvSpPr><p:cNvPr id="${id}" name="${esc(opt.name || `Shape ${id}`)}"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr><p:spPr><a:xfrm><a:off x="${emu(opt.x)}" y="${emu(opt.y)}"/><a:ext cx="${emu(opt.w)}" cy="${emu(opt.h)}"/></a:xfrm><a:prstGeom prst="${geom}"><a:avLst/></a:prstGeom>${solidFill(fill)}${lineXml(opt.line)}${shadowXml(opt.shadow)}</p:spPr>${tx}</p:sp>`;
}

function connector(id, x1, y1, x2, y2, opt = {}) {
  const flipH = x2 < x1 ? ' flipH="1"' : '';
  const flipV = y2 < y1 ? ' flipV="1"' : '';
  const x = Math.min(x1, x2);
  const y = Math.min(y1, y2);
  const w = Math.abs(x2 - x1);
  const h = Math.abs(y2 - y1);
  const line = { color: opt.color || C.line, width: opt.width || 1.2, arrow: opt.arrow !== false, dash: opt.dash };
  return `<p:cxnSp><p:nvCxnSpPr><p:cNvPr id="${id}" name="Connector ${id}"/><p:cNvCxnSpPr/><p:nvPr/></p:nvCxnSpPr><p:spPr><a:xfrm${flipH}${flipV}><a:off x="${emu(x)}" y="${emu(y)}"/><a:ext cx="${emu(w)}" cy="${emu(h)}"/></a:xfrm><a:prstGeom prst="line"><a:avLst/></a:prstGeom>${lineXml(line)}</p:spPr></p:cxnSp>`;
}

function slideBuilder(bg = C.bg) {
  let id = 2;
  const elements = [];
  return {
    shape(opt) {
      elements.push(shape(id++, opt));
    },
    connector(x1, y1, x2, y2, opt) {
      elements.push(connector(id++, x1, y1, x2, y2, opt));
    },
    xml() {
      return slideXml(elements.join(''), bg);
    }
  };
}

function title(s, no, text, subtitle, accent = C.teal) {
  s.shape({ x: 0, y: 0, w: W, h: 0.09, fill: accent, line: false });
  s.shape({ x: 0.58, y: 0.34, w: 0.1, h: 0.48, fill: accent, line: false });
  s.shape({ x: 0.76, y: 0.25, w: 9.5, h: 0.43, fill: null, line: false, text, fontSize: 23, bold: true, fontColor: C.ink, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  if (subtitle) {
    s.shape({ x: 0.77, y: 0.72, w: 10.8, h: 0.28, fill: null, line: false, text: subtitle, fontSize: 10.5, fontColor: C.muted, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  }
  footer(s, no);
}

function footer(s, no) {
  s.shape({ x: 0.58, y: 7.08, w: 5.8, h: 0.22, fill: null, line: false, text: 'D:\\software\\workspace\\user-management-distributed', fontSize: 8, fontColor: C.muted, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  s.shape({ x: 12.22, y: 7.08, w: 0.54, h: 0.22, fill: null, line: false, text: String(no).padStart(2, '0'), fontSize: 8.5, fontColor: C.muted, align: 'r', valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
}

function card(s, x, y, w, h, heading, lines, color = C.teal, fill = C.white) {
  s.shape({ x, y, w, h, type: 'roundRect', fill, line: { color: C.softLine, width: 1 }, shadow: true });
  s.shape({ x, y, w: 0.08, h, fill: color, line: false });
  s.shape({ x: x + 0.2, y: y + 0.14, w: w - 0.35, h: 0.28, fill: null, line: false, text: heading, fontSize: 13.2, bold: true, fontColor: C.ink, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  s.shape({ x: x + 0.2, y: y + 0.5, w: w - 0.35, h: h - 0.58, fill: null, line: false, paragraphs: lines.map((line) => ({ text: line, size: 8.9, color: C.text, after: 115 })), lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
}

function pill(s, x, y, w, h, text, color, fill = C.white) {
  s.shape({ x, y, w, h, type: 'roundRect', fill, line: { color, width: 1 }, text, fontSize: 9.4, bold: true, fontColor: color, align: 'ctr', valign: 'mid', tIns: 0, bIns: 0 });
}

function mini(s, x, y, w, h, text, color, fill = C.white, size = 9.2) {
  s.shape({ x, y, w, h, type: 'roundRect', fill, line: { color, width: 1 }, text, fontSize: size, bold: true, fontColor: color, align: 'ctr', valign: 'mid', tIns: 0, bIns: 0 });
}

function bulletPanel(s, x, y, w, h, heading, bullets, color = C.teal) {
  s.shape({ x, y, w, h, type: 'roundRect', fill: C.white, line: { color: C.softLine, width: 1 }, shadow: true });
  s.shape({ x: x + 0.25, y: y + 0.22, w: w - 0.5, h: 0.28, fill: null, line: false, text: heading, fontSize: 14, bold: true, fontColor: C.ink, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  s.shape({ x: x + 0.35, y: y + 0.66, w: w - 0.6, h: h - 0.8, fill: null, line: false, paragraphs: bullets.map((b) => ({ text: b, size: 9.8, color: C.text, bullet: true, after: 140 })), lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  s.shape({ x, y, w: 0.08, h, fill: color, line: false });
}

function coverSlide() {
  const s = slideBuilder(C.bg);
  s.shape({ x: 0, y: 0, w: 0.22, h: H, fill: C.teal, line: false });
  s.shape({ x: 0.22, y: 0, w: 0.08, h: H, fill: C.amber, line: false });
  s.shape({ x: 0.88, y: 0.78, w: 9.5, h: 0.75, fill: null, line: false, text: '本地风险管理系统建设过程与架构说明', fontSize: 30, bold: true, fontColor: C.ink, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  s.shape({ x: 0.9, y: 1.63, w: 10.2, h: 0.4, fill: null, line: false, text: '基于 user-management-distributed 本地 Workspace 源码、配置与部署脚本生成', fontSize: 14.5, bold: true, fontColor: C.teal, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  s.shape({ x: 0.92, y: 2.26, w: 7.9, h: 0.66, fill: null, line: false, paragraphs: [
    { text: '覆盖系统生成过程、总体架构、技术部署、业务数据流、核心模块说明、安全合规、实施计划与验收指标。', size: 12.2, color: C.text, after: 120 },
    { text: '本文件面向项目汇报、部署交接和验收评审使用。', size: 12.2, color: C.text, after: 0 }
  ], lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  const tags = [
    ['Vue 3 + Vite', C.blue, C.paleBlue],
    ['Spring Boot 3', C.teal, C.paleTeal],
    ['Spring Cloud', C.violet, C.paleViolet],
    ['Docker Compose', C.amber, C.paleAmber],
    ['MySQL + Redis', C.green, C.paleGreen]
  ];
  tags.forEach((t, i) => pill(s, 0.92 + i * 1.45, 3.32, 1.22, 0.36, t[0], t[1], t[2]));
  s.shape({ x: 9.25, y: 1.0, w: 3.08, h: 4.9, type: 'roundRect', fill: C.white, line: { color: C.softLine, width: 1 }, shadow: true });
  const nodes = [
    ['前端管理台', C.blue, C.paleBlue],
    ['API 网关', C.teal, C.paleTeal],
    ['微服务集群', C.amber, C.paleAmber],
    ['数据与中间件', C.rose, C.paleRose]
  ];
  nodes.forEach((n, i) => {
    mini(s, 9.72, 1.46 + i * 0.88, 2.12, 0.5, n[0], n[1], n[2], 10.4);
    if (i < nodes.length - 1) s.connector(10.78, 1.96 + i * 0.88, 10.78, 2.32 + i * 0.88, { color: C.line, width: 1.1 });
  });
  s.shape({ x: 9.6, y: 5.18, w: 2.36, h: 0.3, fill: null, line: false, text: '本地可运行的分布式风险平台', fontSize: 9.8, fontColor: C.muted, align: 'ctr', valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  s.shape({ x: 0.92, y: 6.85, w: 5.6, h: 0.26, fill: null, line: false, text: '生成日期：2026-07-20', fontSize: 9.5, fontColor: C.muted, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  return s.xml();
}

function overviewSlide() {
  const s = slideBuilder();
  title(s, 2, '项目识别与当前形态', '从本地源码判断：这是一个可容器化部署的分布式风险管理后台系统', C.blue);
  card(s, 0.76, 1.28, 3.75, 1.6, '项目路径', ['D:\\software\\workspace\\user-management-distributed', '包含 backend、frontend、database、docker、docs、scripts 等目录', 'docs 目录承载部署、监控、MQ 与本 PPT'], C.blue, C.white);
  card(s, 4.8, 1.28, 3.75, 1.6, '系统定位', ['以用户管理与权限体系为基础', '扩展风险台账、评估、控制、整改、事件和指标管理', '支持本地演示、容器化部署和运维监控'], C.teal, C.white);
  card(s, 8.84, 1.28, 3.75, 1.6, '交付状态', ['已有数据库初始化脚本和示例数据', '已有一键启动、构建、冒烟测试脚本', 'Docker Compose 定义完整本地运行环境'], C.amber, C.white);
  const stack = [
    ['前端', 'Vue 3、Vite、Element Plus、Pinia、Vue Router、Axios', C.blue, C.paleBlue],
    ['后端', 'Java 17、Spring Boot 3.3.7、Spring Cloud 2023.0.4、Eureka、Gateway', C.teal, C.paleTeal],
    ['数据', 'MySQL、MyBatis、Redis 会话缓存、初始化 SQL 脚本', C.green, C.paleGreen],
    ['治理', 'Apollo 配置中心、JWT/RSA、权限拦截、操作日志、错误日志', C.violet, C.paleViolet],
    ['运维', 'Docker Compose、RocketMQ、Prometheus、Grafana、Dozzle', C.rose, C.paleRose]
  ];
  stack.forEach((row, i) => {
    s.shape({ x: 1.0, y: 3.55 + i * 0.52, w: 1.05, h: 0.32, type: 'roundRect', fill: row[3], line: { color: row[2], width: 1 }, text: row[0], fontSize: 9.5, bold: true, fontColor: row[2], align: 'ctr', valign: 'mid', tIns: 0, bIns: 0 });
    s.shape({ x: 2.28, y: 3.55 + i * 0.52, w: 9.8, h: 0.32, type: 'roundRect', fill: C.white, line: { color: C.softLine, width: 1 }, text: row[1], fontSize: 9.2, fontColor: C.text, valign: 'mid', tIns: 0, bIns: 0 });
  });
  return s.xml();
}

function agendaSlide() {
  const s = slideBuilder();
  title(s, 3, '汇报目录', '按照建设过程、架构、部署、模块、安全与验收展开', C.teal);
  const items = [
    ['01', '生成过程', '项目如何从基础用户系统演进为风险管理平台', C.teal, C.paleTeal],
    ['02', '总体架构', '前端、网关、微服务、数据与中间件的分层关系', C.blue, C.paleBlue],
    ['03', '技术部署', 'Docker Compose 容器、端口、构建和启动顺序', C.amber, C.paleAmber],
    ['04', '业务数据流', '登录认证、CRUD 操作、风险闭环、通知消息流', C.rose, C.paleRose],
    ['05', '核心模块', '风险台账、评估、控制、整改、事件、指标与权限', C.violet, C.paleViolet],
    ['06', '安全合规', 'JWT/RSA、权限点、Redis 会话、审计日志和配置治理', C.green, C.paleGreen],
    ['07', '实施计划', '构建、部署、联调、试运行和推广路径', C.teal, C.paleTeal],
    ['08', '验收指标', '功能、接口、部署、性能、安全和运维验收口径', C.blue, C.paleBlue]
  ];
  items.forEach((item, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.78 + col * 6.05;
    const y = 1.2 + row * 1.32;
    s.shape({ x, y, w: 5.45, h: 0.96, type: 'roundRect', fill: C.white, line: { color: C.softLine, width: 1 }, shadow: true });
    mini(s, x + 0.22, y + 0.22, 0.62, 0.48, item[0], item[3], item[4], 12);
    s.shape({ x: x + 1.02, y: y + 0.16, w: 3.85, h: 0.26, fill: null, line: false, text: item[1], fontSize: 12.5, bold: true, fontColor: C.ink, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
    s.shape({ x: x + 1.02, y: y + 0.5, w: 4.0, h: 0.25, fill: null, line: false, text: item[2], fontSize: 8.4, fontColor: C.muted, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  });
  return s.xml();
}

function generationSlide() {
  const s = slideBuilder();
  title(s, 4, '系统生成过程总览', '以代码工程、数据模型、部署配置和运维能力四条线同步生成', C.teal);
  const steps = [
    ['1', '工程骨架', '后端 Maven 多模块 + 前端 Vue 管理台', C.teal, C.paleTeal],
    ['2', '风险建模', '风险表、系统表、权限点和初始化样例数据', C.blue, C.paleBlue],
    ['3', '服务拆分', '注册中心、网关、认证、用户、系统风险服务', C.amber, C.paleAmber],
    ['4', '安全接入', 'JWT/RSA、Redis 会话、权限注解和审计过滤器', C.rose, C.paleRose],
    ['5', '容器部署', 'Dockerfile、Compose、端口、健康检查、环境变量', C.violet, C.paleViolet],
    ['6', '运维验收', '冒烟测试、日志查看、Prometheus/Grafana 指标', C.green, C.paleGreen]
  ];
  steps.forEach((st, i) => {
    const x = 0.58 + i * 2.08;
    s.shape({ x, y: 1.62, w: 1.72, h: 1.55, type: 'roundRect', fill: C.white, line: { color: C.softLine, width: 1 }, shadow: true });
    s.shape({ x: x + 0.17, y: 1.83, w: 0.42, h: 0.42, type: 'ellipse', fill: st[4], line: { color: st[3], width: 1 }, text: st[0], fontSize: 12, bold: true, fontColor: st[3], align: 'ctr', valign: 'mid', tIns: 0, bIns: 0 });
    s.shape({ x: x + 0.17, y: 2.38, w: 1.36, h: 0.25, fill: null, line: false, text: st[1], fontSize: 10.7, bold: true, fontColor: C.ink, align: 'ctr', valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
    s.shape({ x: x + 0.18, y: 2.72, w: 1.34, h: 0.32, fill: null, line: false, text: st[2], fontSize: 7.4, fontColor: C.muted, align: 'ctr', valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
    if (i < steps.length - 1) s.connector(x + 1.72, 2.38, x + 2.04, 2.38, { color: C.line, width: 1.1 });
  });
  bulletPanel(s, 0.82, 4.15, 5.75, 1.52, '本地生成资产', [
    'backend：6 个 Maven 模块，服务按职责拆分',
    'frontend：通用 CRUD 页面与模块配置驱动渲染',
    'database/init.sql：表结构、权限点、菜单和演示数据',
    'docker-compose.yml：完整本地容器运行拓扑'
  ], C.teal);
  bulletPanel(s, 6.95, 4.15, 5.75, 1.52, '生成后的使用路径', [
    '脚本构建后端 jar 与前端静态资源',
    'Compose 拉起 MySQL、Redis、RocketMQ、监控和业务容器',
    '用户通过 http://localhost:5173 访问前端',
    '网关统一承接 /api/** 请求并做认证会话校验'
  ], C.blue);
  return s.xml();
}

function codeOrganizationSlide() {
  const s = slideBuilder();
  title(s, 5, '代码组织与工程成果', '项目目录反映了前后端分离、微服务拆分和本地部署交付形态', C.blue);
  const dirs = [
    ['backend', 'Maven 父工程与 common、discovery-server、api-gateway、auth-service、user-service、system-service', C.blue, C.paleBlue],
    ['frontend', 'Vue 3 管理后台、模块配置、路由、会话 Store、Axios 拦截器、Docker 前端服务', C.teal, C.paleTeal],
    ['database', 'MySQL 初始化脚本：系统表、风险表、权限点、菜单和演示数据', C.green, C.paleGreen],
    ['docker', 'RocketMQ broker、Prometheus、Grafana provisioning 和仪表盘配置', C.amber, C.paleAmber],
    ['scripts', '构建、启动、停止、数据库初始化、Docker 部署和冒烟测试脚本', C.rose, C.paleRose],
    ['docs', 'Apollo 示例、Docker 手册、监控说明、MQ 邮件说明及本 PPT', C.violet, C.paleViolet]
  ];
  dirs.forEach((d, i) => {
    const y = 1.25 + i * 0.82;
    mini(s, 0.92, y, 1.55, 0.46, d[0], d[2], d[3], 10.2);
    s.shape({ x: 2.75, y, w: 9.5, h: 0.46, type: 'roundRect', fill: C.white, line: { color: C.softLine, width: 1 }, text: d[1], fontSize: 9.6, fontColor: C.text, valign: 'mid', tIns: 0, bIns: 0 });
  });
  return s.xml();
}

function overallArchitectureSlide() {
  const s = slideBuilder();
  title(s, 6, '总体架构', '用户入口、网关路由、微服务、数据资源和运维组件分层解耦', C.teal);
  const layers = [
    ['访问层', C.paleBlue, C.blue, ['浏览器', 'Vue 管理台', 'Element Plus', 'Axios']],
    ['网关层', C.paleTeal, C.teal, ['api-gateway', 'JWT 校验', 'CORS', '路由转发']],
    ['服务层', C.paleAmber, C.amber, ['auth-service', 'user-service', 'system-service', 'discovery-server']],
    ['公共能力', C.paleViolet, C.violet, ['common', '权限拦截', '会话服务', '日志过滤']],
    ['数据与中间件', C.paleGreen, C.green, ['MySQL', 'Redis', 'Apollo', 'RocketMQ']],
    ['观测运维', C.paleRose, C.rose, ['Actuator', 'Prometheus', 'Grafana', 'Dozzle']]
  ];
  layers.forEach((layer, i) => {
    const y = 1.15 + i * 0.82;
    s.shape({ x: 0.82, y, w: 1.85, h: 0.56, type: 'roundRect', fill: layer[1], line: { color: layer[2], width: 1 }, text: layer[0], fontSize: 11.5, bold: true, fontColor: layer[2], align: 'ctr', valign: 'mid', tIns: 0, bIns: 0 });
    s.shape({ x: 3.0, y, w: 9.35, h: 0.56, type: 'roundRect', fill: C.white, line: { color: C.softLine, width: 1 }, shadow: i % 2 === 0 });
    layer[3].forEach((box, j) => {
      mini(s, 3.25 + j * 2.12, y + 0.12, 1.63, 0.32, box, layer[2], j % 2 ? C.slate100 : layer[1], 8.2);
    });
  });
  s.shape({ x: 1.1, y: 6.35, w: 11.2, h: 0.36, fill: C.white, line: { color: C.softLine, width: 1 }, text: '核心思路：前端统一访问 /api，网关做认证与路由，风险业务集中在 system-service，用户权限集中在 user-service，配置和会话外置到 Apollo/Redis。', fontSize: 9.7, fontColor: C.text, align: 'ctr', valign: 'mid', shadow: true, tIns: 0, bIns: 0 });
  return s.xml();
}

function backendSlide() {
  const s = slideBuilder();
  title(s, 7, '后端微服务架构', 'Maven 多模块工程拆分为注册、网关、认证、用户权限、风险系统与公共库', C.violet);
  const services = [
    ['discovery-server', '8761', 'Eureka 注册中心，服务注册发现，关闭自我保护便于本地演示', C.blue, C.paleBlue],
    ['api-gateway', '8088', '统一 API 入口，处理 CORS、JWT/RSA、Redis 会话和路由转发', C.teal, C.paleTeal],
    ['auth-service', '9001', '认证登录、验证码、忘记密码、用户 Profile 与登录日志', C.amber, C.paleAmber],
    ['user-service', '9002', '用户、角色、权限、部门、岗位、菜单和授权关系维护', C.rose, C.paleRose],
    ['system-service', '9003', '风险台账、评估、控制、整改、事件、指标、通知和系统参数', C.green, C.paleGreen],
    ['common', 'library', 'ApiResponse、分页、JWT、权限注解、审计日志、异常处理、SQL 日志', C.violet, C.paleViolet]
  ];
  services.forEach((svc, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.74 + col * 6.02;
    const y = 1.28 + row * 1.58;
    s.shape({ x, y, w: 5.45, h: 1.06, type: 'roundRect', fill: C.white, line: { color: C.softLine, width: 1 }, shadow: true });
    mini(s, x + 0.2, y + 0.2, 1.05, 0.44, svc[1], svc[3], svc[4], 9.4);
    s.shape({ x: x + 1.45, y: y + 0.16, w: 3.55, h: 0.25, fill: null, line: false, text: svc[0], fontSize: 12.2, bold: true, fontColor: C.ink, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
    s.shape({ x: x + 1.45, y: y + 0.48, w: 3.7, h: 0.32, fill: null, line: false, text: svc[2], fontSize: 8.1, fontColor: C.muted, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  });
  return s.xml();
}

function frontendSlide() {
  const s = slideBuilder();
  title(s, 8, '前端架构', 'Vue 3 管理后台通过模块配置驱动页面，统一 API 与会话处理', C.blue);
  bulletPanel(s, 0.78, 1.28, 3.55, 2.2, '技术组成', [
    'Vue 3.5 + Vite 6',
    'Element Plus 组件库与图标',
    'Pinia 管理登录会话和本地风险数据',
    'Vue Router 管理 Login、Dashboard、CRUD 页面'
  ], C.blue);
  bulletPanel(s, 4.88, 1.28, 3.55, 2.2, '页面模式', [
    'LoginPage 负责登录、验证码和密码重置入口',
    'MainLayout 负责菜单、顶部栏和内容区',
    'CrudPage 根据 modules.js 统一渲染列表、表单、分页',
    'DashboardPage 展示风险概览和系统入口'
  ], C.teal);
  bulletPanel(s, 8.98, 1.28, 3.55, 2.2, '接口治理', [
    'Axios 实例统一 baseURL 和 15 秒超时',
    '请求拦截自动注入 Authorization Bearer Token',
    '响应拦截统一处理 code、401、403、503 和网络异常',
    '前端所有 /api/** 请求由容器代理到网关'
  ], C.amber);
  const flow = [
    ['登录页', C.blue, C.paleBlue],
    ['会话 Store', C.teal, C.paleTeal],
    ['模块配置', C.amber, C.paleAmber],
    ['通用 CRUD', C.rose, C.paleRose],
    ['API 网关', C.violet, C.paleViolet]
  ];
  flow.forEach((f, i) => {
    mini(s, 1.28 + i * 2.18, 5.2, 1.42, 0.48, f[0], f[1], f[2], 9.4);
    if (i < flow.length - 1) s.connector(2.7 + i * 2.18, 5.44, 3.28 + i * 2.18, 5.44, { color: C.line, width: 1.1 });
  });
  return s.xml();
}

function deploymentSlide() {
  const s = slideBuilder();
  title(s, 9, '技术部署拓扑', 'Docker Compose 一次性拉起业务服务、基础设施、消息队列和监控组件', C.amber);
  const top = [
    ['risk-frontend', '5173:80', '前端页面', C.blue, C.paleBlue],
    ['risk-api-gateway', '8088:8088', '统一网关', C.teal, C.paleTeal],
    ['risk-discovery-server', '8761:8761', 'Eureka', C.violet, C.paleViolet]
  ];
  top.forEach((b, i) => card(s, 0.8 + i * 4.1, 1.18, 3.35, 1.16, b[0], [`端口：${b[1]}`, b[2]], b[3], b[4]));
  const mid = [
    ['risk-auth-service', '9001', C.amber, C.paleAmber],
    ['risk-user-service', '9002', C.rose, C.paleRose],
    ['risk-system-service', '9003', C.green, C.paleGreen]
  ];
  mid.forEach((b, i) => card(s, 0.8 + i * 4.1, 2.85, 3.35, 1.16, b[0], [`服务端口：${b[1]}`, '注册到 Eureka，连接 MySQL/Redis'], b[2], b[3]));
  const infra = [
    ['MySQL', '3307 -> 3306', C.green, C.paleGreen],
    ['Redis', '6381 -> 6379', C.teal, C.paleTeal],
    ['RocketMQ', '9876 / 10909 / 10911 / 8082', C.amber, C.paleAmber],
    ['Prometheus + Grafana', '9090 / 3000', C.rose, C.paleRose],
    ['Dozzle', '9999 -> 8080', C.violet, C.paleViolet]
  ];
  infra.forEach((b, i) => {
    const x = 0.8 + (i % 5) * 2.48;
    s.shape({ x, y: 5.25, w: 2.08, h: 0.66, type: 'roundRect', fill: b[3], line: { color: b[2], width: 1 }, paragraphs: [
      { text: b[0], size: 9.2, bold: true, color: b[2], align: 'ctr', after: 50 },
      { text: b[1], size: 7.2, color: C.text, align: 'ctr', after: 0 }
    ], valign: 'mid', tIns: 0, bIns: 0 });
  });
  s.shape({ x: 1.2, y: 6.42, w: 10.9, h: 0.34, fill: C.white, line: { color: C.softLine, width: 1 }, text: '启动脚本 docker-up.ps1 会处理端口占用、后端构建、前端构建、前端 Java 服务编译和 Compose 启动。', fontSize: 9.2, fontColor: C.text, align: 'ctr', valign: 'mid', shadow: true, tIns: 0, bIns: 0 });
  return s.xml();
}

function deploymentProcessSlide() {
  const s = slideBuilder();
  title(s, 10, '构建与启动流程', '本地部署可走一键脚本，也可按基础设施、注册中心、业务服务、前端逐步启动', C.amber);
  const steps = [
    ['1', '环境准备', 'Docker Desktop、JDK 17+、Maven、Node.js、PowerShell', C.blue, C.paleBlue],
    ['2', '构建后端', 'mvn -f backend\\pom.xml -DskipTests clean package', C.teal, C.paleTeal],
    ['3', '构建前端', 'npm install、npm run build、编译 DockerFrontendServer.java', C.amber, C.paleAmber],
    ['4', '构建镜像', 'Compose build 生成 risk/* 服务镜像', C.rose, C.paleRose],
    ['5', '启动依赖', 'MySQL、Redis、RocketMQ、Dozzle、Prometheus、Grafana', C.violet, C.paleViolet],
    ['6', '启动业务', 'Eureka -> Gateway/Auth/User/System -> Frontend', C.green, C.paleGreen]
  ];
  steps.forEach((st, i) => {
    const x = 0.64 + i * 2.04;
    s.shape({ x, y: 1.55, w: 1.7, h: 1.48, type: 'roundRect', fill: C.white, line: { color: C.softLine, width: 1 }, shadow: true });
    mini(s, x + 0.15, 1.75, 0.44, 0.4, st[0], st[3], st[4], 11);
    s.shape({ x: x + 0.15, y: 2.28, w: 1.38, h: 0.23, fill: null, line: false, text: st[1], fontSize: 10.3, bold: true, fontColor: C.ink, align: 'ctr', valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
    s.shape({ x: x + 0.15, y: 2.61, w: 1.38, h: 0.28, fill: null, line: false, text: st[2], fontSize: 7.1, fontColor: C.muted, align: 'ctr', valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
    if (i < steps.length - 1) s.connector(x + 1.7, 2.3, x + 2.0, 2.3, { color: C.line, width: 1 });
  });
  bulletPanel(s, 0.82, 4.1, 5.55, 1.55, '关键脚本', [
    'scripts\\docker-up.ps1：完整构建并启动',
    'scripts\\docker-down.ps1：停止容器',
    'scripts\\build-backend.ps1 / build-frontend.ps1：分段构建',
    'scripts\\smoke-test.ps1：登录与用户列表冒烟测试'
  ], C.amber);
  bulletPanel(s, 6.88, 4.1, 5.55, 1.55, '访问入口', [
    '前端：http://localhost:5173',
    '网关：http://localhost:8088',
    'Eureka：http://localhost:8761',
    'Grafana：http://localhost:3000',
    'RocketMQ Dashboard：http://localhost:8082'
  ], C.teal);
  return s.xml();
}

function dataFlowSlide() {
  const s = slideBuilder();
  title(s, 11, '业务数据流', '风险生命周期通过统一 CRUD 接口和关系表沉淀到 MySQL，操作过程留痕', C.rose);
  const steps = [
    ['风险台账', 'risk_register', C.teal, C.paleTeal],
    ['风险评估', 'risk_assessment', C.blue, C.paleBlue],
    ['控制措施', 'risk_control_measure', C.amber, C.paleAmber],
    ['整改计划', 'risk_treatment_plan', C.rose, C.paleRose],
    ['风险事件', 'risk_event', C.violet, C.paleViolet],
    ['风险指标', 'risk_indicator', C.green, C.paleGreen]
  ];
  steps.forEach((st, i) => {
    const x = 0.62 + i * 2.06;
    s.shape({ x, y: 1.7, w: 1.58, h: 1.12, type: 'roundRect', fill: st[3], line: { color: st[2], width: 1 }, shadow: true });
    s.shape({ x: x + 0.16, y: 1.94, w: 1.25, h: 0.22, fill: null, line: false, text: st[0], fontSize: 10.8, bold: true, fontColor: st[2], align: 'ctr', valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
    s.shape({ x: x + 0.16, y: 2.28, w: 1.25, h: 0.22, fill: null, line: false, text: st[1], fontSize: 7.2, fontColor: C.text, align: 'ctr', valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
    if (i < steps.length - 1) s.connector(x + 1.58, 2.26, x + 2.03, 2.26, { color: C.line, width: 1.1 });
  });
  s.shape({ x: 1.3, y: 3.58, w: 10.8, h: 0.42, type: 'roundRect', fill: C.white, line: { color: C.softLine, width: 1 }, text: '风险数据通过台账建立主对象，评估确定等级，控制措施与整改计划承接处置，事件和指标持续回流形成闭环。', fontSize: 9.4, fontColor: C.text, align: 'ctr', valign: 'mid', shadow: true, tIns: 0, bIns: 0 });
  bulletPanel(s, 0.82, 4.55, 3.65, 1.4, '前端提交', ['CrudPage 根据 modules.js 字段配置生成表单', 'Axios 自动携带 token', '接口统一走 /api/risks/**'], C.blue);
  bulletPanel(s, 4.88, 4.55, 3.65, 1.4, '服务处理', ['网关校验会话后转发到 system-service', 'Controller 继承通用 CRUD 能力', 'Service 调用 MyBatis Mapper'], C.teal);
  bulletPanel(s, 8.94, 4.55, 3.65, 1.4, '数据落库', ['业务表保存风险数据', '非 GET 操作写入操作日志', 'SQL 执行日志记录耗时与语句'], C.rose);
  return s.xml();
}

function authFlowSlide() {
  const s = slideBuilder();
  title(s, 12, '认证、授权与请求流', '系统通过网关、JWT/RSA、Redis 会话和权限点共同保护 API', C.violet);
  const flow = [
    ['浏览器', C.blue, C.paleBlue],
    ['登录接口 /api/auth/login', C.teal, C.paleTeal],
    ['签发 JWT + tokenId', C.amber, C.paleAmber],
    ['Redis 保存会话', C.green, C.paleGreen],
    ['访问 /api/**', C.rose, C.paleRose],
    ['网关校验并转发', C.violet, C.paleViolet],
    ['服务端权限注解校验', C.teal, C.paleTeal],
    ['业务数据返回', C.blue, C.paleBlue]
  ];
  flow.forEach((f, i) => {
    const x = 0.62 + (i % 4) * 3.05;
    const y = 1.46 + Math.floor(i / 4) * 1.65;
    mini(s, x, y, 2.25, 0.56, f[0], f[1], f[2], 9.2);
    if (i < 3) s.connector(x + 2.25, y + 0.28, x + 3.02, y + 0.28, { color: C.line, width: 1 });
    if (i === 3) s.connector(x + 1.12, y + 0.56, x + 1.12, y + 1.09, { color: C.line, width: 1 });
    if (i > 4 && i < 7) s.connector(x + 2.25, y + 0.28, x + 3.02, y + 0.28, { color: C.line, width: 1 });
  });
  bulletPanel(s, 0.82, 5.0, 3.65, 1.2, '安全机制', ['JWT 使用 RSA 签名/加密配置', 'Redis validateAndRefresh 控制会话有效性', '公共路径和 OPTIONS 请求放行'], C.violet);
  bulletPanel(s, 4.88, 5.0, 3.65, 1.2, '权限机制', ['RequirePermission 标注接口权限', '用户角色权限来自登录上下文', '无权限返回 403 permission denied'], C.teal);
  bulletPanel(s, 8.94, 5.0, 3.65, 1.2, '验收关注', ['登录返回权限需覆盖风险菜单', '401/403/503 场景前端需友好提示', '登出后 token 应被撤销'], C.rose);
  return s.xml();
}

function moduleMapSlide() {
  const s = slideBuilder();
  title(s, 13, '核心模块全景', '系统模块覆盖风险业务闭环、用户权限、系统治理和运维审计', C.green);
  const modules = [
    ['风险台账', 'risk:manage', C.teal, C.paleTeal],
    ['风险评估', 'risk:assess', C.blue, C.paleBlue],
    ['控制措施', 'risk:control', C.amber, C.paleAmber],
    ['整改任务', 'risk:treat', C.rose, C.paleRose],
    ['风险事件', 'risk:event', C.violet, C.paleViolet],
    ['风险指标', 'risk:indicator', C.green, C.paleGreen],
    ['用户账号', 'user:manage', C.blue, C.paleBlue],
    ['角色权限', 'role:manage', C.teal, C.paleTeal],
    ['审计日志', 'log:view', C.amber, C.paleAmber],
    ['风险通知', 'notification:manage', C.rose, C.paleRose],
    ['风险参数', 'config:manage', C.violet, C.paleViolet],
    ['安全策略/租户', 'security:manage / tenant:manage', C.green, C.paleGreen]
  ];
  modules.forEach((m, i) => {
    const col = i % 4;
    const row = Math.floor(i / 4);
    const x = 0.78 + col * 3.05;
    const y = 1.35 + row * 1.46;
    s.shape({ x, y, w: 2.55, h: 1.0, type: 'roundRect', fill: C.white, line: { color: C.softLine, width: 1 }, shadow: true });
    mini(s, x + 0.22, y + 0.2, 0.52, 0.4, String(i + 1), m[2], m[3], 10);
    s.shape({ x: x + 0.88, y: y + 0.18, w: 1.35, h: 0.22, fill: null, line: false, text: m[0], fontSize: 10.5, bold: true, fontColor: C.ink, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
    s.shape({ x: x + 0.88, y: y + 0.55, w: 1.42, h: 0.2, fill: null, line: false, text: m[1], fontSize: 7.2, fontColor: C.muted, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  });
  s.shape({ x: 1.18, y: 6.28, w: 10.95, h: 0.42, type: 'roundRect', fill: C.white, line: { color: C.softLine, width: 1 }, text: '模块入口由 sys_menu 与前端 modules.js 共同定义，后台接口用权限点保护，风险数据和系统数据分别落在 risk_* 与 sys_* 表。', fontSize: 9.3, fontColor: C.text, align: 'ctr', valign: 'mid', shadow: true, tIns: 0, bIns: 0 });
  return s.xml();
}

function riskModulesSlide() {
  const s = slideBuilder();
  title(s, 14, '核心模块说明：风险业务', '风险业务集中在 system-service，接口前缀为 /risks/**', C.rose);
  card(s, 0.78, 1.28, 3.7, 1.45, '风险台账 /registers', ['维护风险编号、名称、分类、等级、责任部门、责任人、状态、识别日期、整改期限和描述', '作为后续评估、控制、整改、事件和指标的主对象'], C.teal, C.white);
  card(s, 4.82, 1.28, 3.7, 1.45, '风险评估 /assessments', ['记录发生可能性、影响程度、固有风险、剩余风险、评估人、评估日期和结论', '支持按评分矩阵和阈值形成风险等级'], C.blue, C.white);
  card(s, 8.86, 1.28, 3.7, 1.45, '控制措施 /controls', ['维护控制编号、关联风险、控制名称、控制类型、执行频率、负责人、有效性和状态', '承接风险控制设计与执行跟踪'], C.amber, C.white);
  card(s, 0.78, 3.28, 3.7, 1.45, '整改任务 /treatments', ['记录任务编号、风险编号、整改措施、负责人、截止日期、进度和状态', '用于闭环跟踪整改计划和逾期情况'], C.rose, C.white);
  card(s, 4.82, 3.28, 3.7, 1.45, '风险事件 /events', ['记录事件编号、标题、关联风险、严重程度、发生日期、损失金额、负责人和状态', '将真实事件回流到风险画像'], C.violet, C.white);
  card(s, 8.86, 3.28, 3.7, 1.45, '风险指标 /indicators', ['维护 KRI 指标编号、名称、阈值、当前值、趋势、负责人和状态', '支持预警、趋势监控和管理驾驶舱数据来源'], C.green, C.white);
  return s.xml();
}

function systemModulesSlide() {
  const s = slideBuilder();
  title(s, 15, '核心模块说明：平台与系统能力', '用户权限、配置、通知、导入导出和日志为风险业务提供支撑', C.teal);
  const groups = [
    ['用户权限', ['用户、角色、权限、部门、岗位、菜单', '用户-角色、角色-权限关系表支撑授权', '菜单权限点控制前端入口与后端接口'], C.blue, C.paleBlue],
    ['系统治理', ['系统参数 sys_config 管理风险阈值等配置', '安全策略 sys_security_policy 维护密码复杂度、登录锁定、Token 有效期', '租户 sys_tenant 为后续多租户隔离预留'], C.teal, C.paleTeal],
    ['通知协同', ['sys_notification 保存通知内容、渠道、目标和状态', 'EMAIL + PUBLISHED 时发布 RocketMQ 邮件消息', 'MAIL_SEND_ENABLED 控制是否真实连接 SMTP'], C.amber, C.paleAmber],
    ['日志审计', ['登录日志记录登录成功/失败', '操作日志记录非 GET 请求', '错误日志保存服务名、traceId、异常信息和堆栈'], C.rose, C.paleRose],
    ['导入导出', ['提供 /import/users 与 /export/users', '由 import:user 与 export:user 权限控制', '可作为批量初始化和运营交接能力'], C.violet, C.paleViolet],
    ['公共响应', ['ApiResponse 统一 code/message/data/timestamp', 'PageResult 统一分页结构', 'GlobalExceptionHandler 统一异常响应'], C.green, C.paleGreen]
  ];
  groups.forEach((g, i) => {
    const x = 0.72 + (i % 3) * 4.18;
    const y = 1.26 + Math.floor(i / 3) * 2.28;
    card(s, x, y, 3.55, 1.62, g[0], g[1], g[2], g[3]);
  });
  return s.xml();
}

function securitySlide() {
  const s = slideBuilder();
  title(s, 16, '安全合规设计', '围绕身份认证、权限控制、数据安全、审计留痕和配置治理建立控制点', C.violet);
  const quads = [
    ['身份认证', ['登录后签发 JWT，Token 包含 tokenId、用户、角色、权限和租户', '网关和服务端都具备 token 校验能力', '验证码、忘记密码、重置密码入口用于本地演示'], C.blue, C.paleBlue],
    ['会话控制', ['Redis 保存 tokenId 并支持 validateAndRefresh', '登出后 revoke 会话', 'SESSION_REDIS_REQUIRED 可控制 Redis 强依赖策略'], C.teal, C.paleTeal],
    ['权限控制', ['RequirePermission 注解保护 Controller', '权限点覆盖 user、role、risk、log、config、security、tenant 等模块', '前端根据权限过滤菜单与操作入口'], C.amber, C.paleAmber],
    ['审计留痕', ['登录日志、操作日志、错误日志分别落表', 'SQL_EXEC 日志记录 statementId、耗时、SQL 与参数', 'RequestTraceFilter 提供请求链路定位基础'], C.rose, C.paleRose],
    ['配置安全', ['Apollo 管理 application、database、security、gateway、risk、mq namespace', 'Docker 环境变量覆盖数据库、Redis、MQ、JWT 和端口配置', 'APOLLO_REQUIRED=true 时配置缺失不允许启动'], C.violet, C.paleViolet],
    ['数据保护', ['数据库使用 utf8mb4 并独立 Docker volume 持久化', '敏感配置通过 .env 与环境变量注入', '生产化建议补充密钥轮换、备份、脱敏与最小权限账号'], C.green, C.paleGreen]
  ];
  quads.forEach((q, i) => {
    const x = 0.72 + (i % 3) * 4.18;
    const y = 1.2 + Math.floor(i / 3) * 2.28;
    card(s, x, y, 3.55, 1.65, q[0], q[1], q[2], q[3]);
  });
  return s.xml();
}

function observabilitySlide() {
  const s = slideBuilder();
  title(s, 17, '监控、日志与消息能力', '本地环境已经纳入容器日志、服务指标和 RocketMQ 邮件通知链路', C.green);
  bulletPanel(s, 0.78, 1.22, 3.62, 1.68, '监控指标', [
    '各 Spring 服务暴露 /actuator/prometheus',
    'Prometheus 每 15 秒采集服务指标',
    'Grafana 自动加载 Risk Management System Overview',
    '看板覆盖服务可用性、CPU、JVM、HTTP 请求和 5xx'
  ], C.green);
  bulletPanel(s, 4.86, 1.22, 3.62, 1.68, '日志查看', [
    'Log4j2 输出服务日志',
    'Dozzle 通过 9999 查看容器标准输出',
    '应用日志挂载到 risk_app_logs',
    'SQL 日志支持定位慢查询和异常数据'
  ], C.blue);
  bulletPanel(s, 8.94, 1.22, 3.62, 1.68, '消息通知', [
    'RocketMQ Namesrv、Broker、Dashboard 已纳入 Compose',
    'system-service 负责邮件消息生产与消费',
    '默认只消费并记录日志，不真实发送 SMTP',
    'MQ 临时不可用不回滚通知保存'
  ], C.amber);
  const endpoints = [
    ['Prometheus', 'http://localhost:9090', C.green, C.paleGreen],
    ['Grafana', 'http://localhost:3000', C.rose, C.paleRose],
    ['Dozzle', 'http://localhost:9999', C.violet, C.paleViolet],
    ['RocketMQ Console', 'http://localhost:8082', C.amber, C.paleAmber]
  ];
  endpoints.forEach((e, i) => {
    mini(s, 1.05 + i * 3.0, 5.1, 2.35, 0.58, `${e[0]}\n${e[1]}`, e[2], e[3], 8.4);
  });
  return s.xml();
}

function implementationPlanSlide() {
  const s = slideBuilder();
  title(s, 18, '实施计划', '按“本地跑通、功能联调、安全校验、试运行交接”四阶段推进', C.teal);
  const phases = [
    ['阶段 1', '环境与构建', '1-2 天', ['检查 Docker/JDK/Maven/Node', '后端与前端构建成功', '初始化数据库与演示数据'], C.blue, C.paleBlue],
    ['阶段 2', '部署与联调', '2-3 天', ['Compose 启动所有容器', '验证前端、网关、Eureka、数据库、Redis', '跑通登录与主要 CRUD'], C.teal, C.paleTeal],
    ['阶段 3', '安全与运维', '2-3 天', ['验证 JWT、会话、权限、审计日志', '验证 Prometheus、Grafana、Dozzle', '验证 RocketMQ 邮件消息链路'], C.amber, C.paleAmber],
    ['阶段 4', '试运行与验收', '1 周', ['按真实角色录入风险数据', '复盘预警、整改和日志问题', '形成部署手册与验收报告'], C.rose, C.paleRose]
  ];
  phases.forEach((p, i) => {
    const x = 0.72 + i * 3.05;
    card(s, x, 1.35, 2.65, 2.25, `${p[0]}：${p[1]}`, [`周期：${p[2]}`, ...p[3]], p[4], p[5]);
    if (i < phases.length - 1) s.connector(x + 2.65, 2.45, x + 3.0, 2.45, { color: C.line, width: 1.1 });
  });
  bulletPanel(s, 0.88, 4.55, 5.65, 1.35, '交付物', [
    '可运行源码、Docker Compose、数据库脚本、Apollo 配置示例',
    '部署手册、监控说明、MQ 邮件说明、冒烟测试脚本',
    '本 PPT 与验收记录'
  ], C.teal);
  bulletPanel(s, 6.86, 4.55, 5.65, 1.35, '实施重点', [
    '先验证本地闭环，再扩大到真实业务数据',
    '所有服务端口、账号、密钥和中间件配置要文档化',
    '上线前补齐安全配置与生产级备份策略'
  ], C.rose);
  return s.xml();
}

function acceptanceSlide() {
  const s = slideBuilder();
  title(s, 19, '验收指标', '用功能、接口、部署、安全、数据和运维六类指标判断系统是否可交付', C.blue);
  const metrics = [
    ['功能验收', ['登录、菜单、风险台账、评估、控制、整改、事件、指标、通知、日志、参数可正常增删改查或查看', '前端页面无乱码、无明显布局错位'], C.blue, C.paleBlue],
    ['接口验收', ['网关 /api/** 路由正确，401/403/503 响应可识别', 'smoke-test.ps1 登录成功并能获取用户列表', '分页、搜索、创建、更新、删除响应结构一致'], C.teal, C.paleTeal],
    ['部署验收', ['Compose 容器均启动，业务容器能注册到 Eureka', '前端 5173、网关 8088、Eureka 8761、Grafana 3000、Prometheus 9090 可访问', 'MySQL、Redis、RocketMQ 数据与消息链路可用'], C.amber, C.paleAmber],
    ['安全验收', ['JWT/RSA 签发、校验、登出撤销有效', 'Redis 会话过期后访问受保护接口返回 401', '权限点覆盖所有菜单和接口，越权访问返回 403'], C.rose, C.paleRose],
    ['数据验收', ['risk_* 与 sys_* 表结构完整，初始化数据可加载', '风险台账与评估、控制、整改、事件、指标可通过 risk_code 关联', '非 GET 操作写入操作日志，错误日志可检索'], C.violet, C.paleViolet],
    ['运维验收', ['Prometheus 能采集所有服务 /actuator/prometheus', 'Grafana 看板显示服务可用性、JVM、HTTP 和 5xx', 'Dozzle 可查看容器日志，MQ 控制台可查看 Topic 和消费进度'], C.green, C.paleGreen]
  ];
  metrics.forEach((m, i) => {
    const x = 0.72 + (i % 3) * 4.18;
    const y = 1.18 + Math.floor(i / 3) * 2.36;
    card(s, x, y, 3.55, 1.75, m[0], m[1], m[2], m[3]);
  });
  return s.xml();
}

function risksSlide() {
  const s = slideBuilder();
  title(s, 20, '后续演进建议', '当前系统适合本地演示和基础验收，生产化前建议补强以下能力', C.rose);
  const items = [
    ['权限一致性', '核对网关登录与 auth-service 登录返回的权限点，确保风险菜单 risk:* 在实际登录态可用', C.rose, C.paleRose],
    ['中文编码', 'README、docs、modules.js 和初始化数据存在编码显示异常，应统一为 UTF-8 并重新校验页面文案', C.amber, C.paleAmber],
    ['生产密钥', 'JWT RSA 私钥、数据库密码、Grafana 密码应移出默认配置，纳入密钥管理和轮换机制', C.violet, C.paleViolet],
    ['测试覆盖', '补充服务层、权限拦截、网关路由、导入导出、风险 CRUD 和 MQ 消费的自动化测试', C.blue, C.paleBlue],
    ['数据治理', '补充外键/索引、数据校验、软删除、操作人字段、租户隔离和备份恢复策略', C.teal, C.paleTeal],
    ['可观测性', '完善告警规则、日志 traceId 贯通、慢 SQL 指标和服务容量基线', C.green, C.paleGreen]
  ];
  items.forEach((it, i) => {
    const x = 0.78 + (i % 2) * 6.05;
    const y = 1.26 + Math.floor(i / 2) * 1.52;
    s.shape({ x, y, w: 5.45, h: 1.02, type: 'roundRect', fill: C.white, line: { color: C.softLine, width: 1 }, shadow: true });
    mini(s, x + 0.22, y + 0.25, 0.58, 0.42, String(i + 1), it[2], it[3], 10.5);
    s.shape({ x: x + 0.98, y: y + 0.18, w: 1.7, h: 0.22, fill: null, line: false, text: it[0], fontSize: 11.2, bold: true, fontColor: C.ink, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
    s.shape({ x: x + 0.98, y: y + 0.5, w: 4.1, h: 0.28, fill: null, line: false, text: it[1], fontSize: 8.2, fontColor: C.muted, valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  });
  s.shape({ x: 1.15, y: 6.55, w: 10.9, h: 0.3, fill: null, line: false, text: '建议将上述项纳入下一阶段迭代计划，用于从本地演示版走向可长期运行的企业内部系统。', fontSize: 10, fontColor: C.text, align: 'ctr', valign: 'mid', lIns: 0, rIns: 0, tIns: 0, bIns: 0 });
  return s.xml();
}

function slideXml(elements, bg) {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg><p:bgPr>${solidFill(bg)}<a:effectLst/></p:bgPr></p:bg>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
      ${elements}
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>`;
}

function contentTypes(slideCount) {
  const slideOverrides = Array.from({ length: slideCount }, (_, i) => `<Override PartName="/ppt/slides/slide${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>`).join('');
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
  ${slideOverrides}
</Types>`;
}

function rootRels() {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>`;
}

function appProps(slideCount) {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Codex</Application>
  <PresentationFormat>Widescreen</PresentationFormat>
  <Slides>${slideCount}</Slides>
  <Notes>0</Notes>
  <HiddenSlides>0</HiddenSlides>
  <MMClips>0</MMClips>
  <ScaleCrop>false</ScaleCrop>
  <Company></Company>
  <LinksUpToDate>false</LinksUpToDate>
  <SharedDoc>false</SharedDoc>
  <HyperlinksChanged>false</HyperlinksChanged>
  <AppVersion>16.0000</AppVersion>
</Properties>`;
}

function coreProps() {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>本地风险管理系统建设过程与架构说明</dc:title>
  <dc:subject>生成过程、总体架构、技术部署、业务数据流、核心模块说明、安全合规、实施计划与验收指标</dc:subject>
  <dc:creator>Codex</dc:creator>
  <cp:lastModifiedBy>Codex</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">2026-07-20T00:00:00Z</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">2026-07-20T00:00:00Z</dcterms:modified>
</cp:coreProperties>`;
}

function presentationXml(slideCount) {
  const ids = Array.from({ length: slideCount }, (_, i) => `<p:sldId id="${256 + i}" r:id="rId${i + 2}"/>`).join('');
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" saveSubsetFonts="1">
  <p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId1"/></p:sldMasterIdLst>
  <p:sldIdLst>${ids}</p:sldIdLst>
  <p:sldSz cx="12192000" cy="6858000" type="wide"/>
  <p:notesSz cx="6858000" cy="9144000"/>
  <p:defaultTextStyle>
    <a:defPPr><a:defRPr lang="zh-CN"><a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/></a:defRPr></a:defPPr>
  </p:defaultTextStyle>
</p:presentation>`;
}

function presentationRels(slideCount) {
  const slideRels = Array.from({ length: slideCount }, (_, i) => `<Relationship Id="rId${i + 2}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide${i + 1}.xml"/>`).join('');
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>
  ${slideRels}
</Relationships>`;
}

function slideRel() {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
</Relationships>`;
}

function slideMasterXml() {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
    </p:spTree>
  </p:cSld>
  <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>
  <p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst>
  <p:txStyles>
    <p:titleStyle><a:lvl1pPr algn="l"><a:defRPr sz="3200" b="1"><a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/></a:defRPr></a:lvl1pPr></p:titleStyle>
    <p:bodyStyle><a:lvl1pPr algn="l"><a:defRPr sz="1800"><a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/></a:defRPr></a:lvl1pPr></p:bodyStyle>
    <p:otherStyle><a:lvl1pPr algn="l"><a:defRPr sz="1800"><a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/></a:defRPr></a:lvl1pPr></p:otherStyle>
  </p:txStyles>
</p:sldMaster>`;
}

function slideMasterRels() {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
</Relationships>`;
}

function slideLayoutXml() {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1">
  <p:cSld name="Blank">
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sldLayout>`;
}

function slideLayoutRels() {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>`;
}

function themeXml() {
  return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="LocalRiskTheme">
  <a:themeElements>
    <a:clrScheme name="LocalRiskTheme">
      <a:dk1><a:srgbClr val="0F172A"/></a:dk1>
      <a:lt1><a:srgbClr val="FFFFFF"/></a:lt1>
      <a:dk2><a:srgbClr val="334155"/></a:dk2>
      <a:lt2><a:srgbClr val="F7F9FB"/></a:lt2>
      <a:accent1><a:srgbClr val="0F766E"/></a:accent1>
      <a:accent2><a:srgbClr val="1D4ED8"/></a:accent2>
      <a:accent3><a:srgbClr val="D97706"/></a:accent3>
      <a:accent4><a:srgbClr val="BE123C"/></a:accent4>
      <a:accent5><a:srgbClr val="6D28D9"/></a:accent5>
      <a:accent6><a:srgbClr val="15803D"/></a:accent6>
      <a:hlink><a:srgbClr val="1D4ED8"/></a:hlink>
      <a:folHlink><a:srgbClr val="6D28D9"/></a:folHlink>
    </a:clrScheme>
    <a:fontScheme name="Microsoft YaHei">
      <a:majorFont><a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/><a:cs typeface="Microsoft YaHei"/></a:majorFont>
      <a:minorFont><a:latin typeface="Microsoft YaHei"/><a:ea typeface="Microsoft YaHei"/><a:cs typeface="Microsoft YaHei"/></a:minorFont>
    </a:fontScheme>
    <a:fmtScheme name="LocalRiskFormat">
      <a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:fillStyleLst>
      <a:lnStyleLst>
        <a:ln w="6350"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:prstDash val="solid"/></a:ln>
        <a:ln w="12700"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:prstDash val="solid"/></a:ln>
        <a:ln w="19050"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:prstDash val="solid"/></a:ln>
      </a:lnStyleLst>
      <a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst>
      <a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst>
    </a:fmtScheme>
  </a:themeElements>
  <a:objectDefaults/>
  <a:extraClrSchemeLst/>
</a:theme>`;
}

function crc32(buf) {
  const table = crc32.table || (crc32.table = (() => {
    const t = new Uint32Array(256);
    for (let n = 0; n < 256; n++) {
      let c = n;
      for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
      t[n] = c >>> 0;
    }
    return t;
  })());
  let c = 0xFFFFFFFF;
  for (const b of buf) c = table[(c ^ b) & 0xFF] ^ (c >>> 8);
  return (c ^ 0xFFFFFFFF) >>> 0;
}

function dosDateTime(date = new Date()) {
  const year = Math.max(date.getFullYear(), 1980);
  const time = (date.getHours() << 11) | (date.getMinutes() << 5) | Math.floor(date.getSeconds() / 2);
  const day = (year - 1980) << 9 | ((date.getMonth() + 1) << 5) | date.getDate();
  return { time, day };
}

function makeZip(entries, outputPath) {
  const chunks = [];
  const central = [];
  let offset = 0;
  const { time, day } = dosDateTime(new Date(2026, 6, 20, 12, 0, 0));
  for (const entry of entries) {
    const nameBuf = Buffer.from(entry.name, 'utf8');
    const data = Buffer.isBuffer(entry.data) ? entry.data : Buffer.from(entry.data, 'utf8');
    const compressed = zlib.deflateRawSync(data, { level: 9 });
    const crc = crc32(data);
    const local = Buffer.alloc(30);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(20, 4);
    local.writeUInt16LE(0x0800, 6);
    local.writeUInt16LE(8, 8);
    local.writeUInt16LE(time, 10);
    local.writeUInt16LE(day, 12);
    local.writeUInt32LE(crc, 14);
    local.writeUInt32LE(compressed.length, 18);
    local.writeUInt32LE(data.length, 22);
    local.writeUInt16LE(nameBuf.length, 26);
    local.writeUInt16LE(0, 28);
    chunks.push(local, nameBuf, compressed);
    central.push({ nameBuf, crc, compressedSize: compressed.length, size: data.length, offset });
    offset += local.length + nameBuf.length + compressed.length;
  }
  const centralStart = offset;
  for (const entry of central) {
    const h = Buffer.alloc(46);
    h.writeUInt32LE(0x02014b50, 0);
    h.writeUInt16LE(20, 4);
    h.writeUInt16LE(20, 6);
    h.writeUInt16LE(0x0800, 8);
    h.writeUInt16LE(8, 10);
    h.writeUInt16LE(time, 12);
    h.writeUInt16LE(day, 14);
    h.writeUInt32LE(entry.crc, 16);
    h.writeUInt32LE(entry.compressedSize, 20);
    h.writeUInt32LE(entry.size, 24);
    h.writeUInt16LE(entry.nameBuf.length, 28);
    h.writeUInt16LE(0, 30);
    h.writeUInt16LE(0, 32);
    h.writeUInt16LE(0, 34);
    h.writeUInt16LE(0, 36);
    h.writeUInt32LE(0, 38);
    h.writeUInt32LE(entry.offset, 42);
    chunks.push(h, entry.nameBuf);
    offset += h.length + entry.nameBuf.length;
  }
  const centralSize = offset - centralStart;
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(0, 4);
  end.writeUInt16LE(0, 6);
  end.writeUInt16LE(central.length, 8);
  end.writeUInt16LE(central.length, 10);
  end.writeUInt32LE(centralSize, 12);
  end.writeUInt32LE(centralStart, 16);
  end.writeUInt16LE(0, 20);
  chunks.push(end);
  fs.writeFileSync(outputPath, Buffer.concat(chunks));
}

function bizCover() {
  const s=slideBuilder('F3F6FA');
  s.shape({x:.7,y:.65,w:1.05,h:.12,fill:C.teal,line:false});
  s.shape({x:.82,y:1.22,w:10.9,h:.72,fill:null,line:false,text:'组合风险系统',fontSize:32,bold:true,fontColor:C.ink,lIns:0,rIns:0,tIns:0,bIns:0});
  s.shape({x:.84,y:2.08,w:10.5,h:.44,fill:null,line:false,text:'面向业务人员的组合信用风险管理全景介绍',fontSize:18,fontColor:C.teal,lIns:0,rIns:0,tIns:0,bIns:0});
  s.shape({x:.84,y:2.76,w:7.4,h:.78,type:'roundRect',fill:C.white,line:{color:C.softLine,width:1},paragraphs:[{text:'核心主线',size:10,bold:true,color:C.muted,after:80},{text:'月末批量分析  ·  组合风险计量  ·  限额预警  ·  风险处置  ·  管理决策',size:14,bold:true,color:C.ink}],shadow:true});
  const items=[['PD','违约概率',C.amber,C.paleAmber],['LGD','违约损失率',C.rose,C.paleRose],['EAD','风险暴露',C.blue,C.paleBlue],['EL','预期损失',C.teal,C.paleTeal]];
  items.forEach((v,i)=>{mini(s,8.7+(i%2)*1.65,3.0+Math.floor(i/2)*1.0,1.35,.68,v[0],v[2],v[3],16);s.shape({x:8.7+(i%2)*1.65,y:3.7+Math.floor(i/2)*1.0,w:1.35,h:.22,fill:null,line:false,text:v[1],fontSize:8.5,fontColor:C.muted,align:'ctr',tIns:0,bIns:0,lIns:0,rIns:0});});
  s.shape({x:.84,y:6.45,w:5.5,h:.28,fill:null,line:false,text:'适用对象：管理层、风险经理、业务机构、模型与数据人员',fontSize:10,fontColor:C.muted,lIns:0,rIns:0,tIns:0,bIns:0});
  s.shape({x:9.9,y:6.45,w:2.2,h:.28,fill:null,line:false,text:'2026年7月',fontSize:10,fontColor:C.muted,align:'right',lIns:0,rIns:0,tIns:0,bIns:0});
  return s.xml();
}

function bizWhy() {
  const s=slideBuilder(); title(s,2,'为什么建设组合风险系统','把分散的客户和债项风险，转化为可管理、可比较、可处置的组合风险',C.teal);
  const cards=[['看不全','客户、合同、债项、押品、逾期和违约分散在不同业务环节','建立统一风险视图',C.blue,C.paleBlue],['看不清','组合规模变化后，无法快速解释是新增业务、评级、PD还是LGD造成','形成跨月变化归因',C.amber,C.paleAmber],['看不早','超限、集中度和风险恶化往往在结果发生后才发现','建立前瞻限额与预警',C.rose,C.paleRose],['管不住','预警、整改和复核之间缺少状态回写和效果评价','形成处置管理闭环',C.teal,C.paleTeal]];
  cards.forEach((v,i)=>{const x=.78+(i%2)*6.05,y=1.3+Math.floor(i/2)*2.15;card(s,x,y,5.5,1.62,v[0],[v[1],`系统作用：${v[2]}`],v[3],v[4]);});
  s.shape({x:1.1,y:5.85,w:11.1,h:.62,type:'roundRect',fill:C.ink,line:false,text:'业务目标：控制集中度与尾部风险  ·  提前识别组合恶化  ·  防止限额突破  ·  支持投向与资源配置',fontSize:13,bold:true,fontColor:C.white,align:'ctr',valign:'mid',shadow:true,tIns:0,bIns:0});
  return s.xml();
}

function bizPanorama() {
  const s=slideBuilder(); title(s,3,'业务全景','系统围绕“数据—加工—计量—管理—决策”构成完整业务链路',C.blue);
  const cols=[['信贷业务数据',['集团与客户','评级与额度','申请与合同','债项与押品','逾期与违约'],C.blue,C.paleBlue],['双运行主线',['月末批量加工','日常风险预警','质量与勾稽','发布与版本','处置与评价'],C.amber,C.paleAmber],['风险计量分析',['PD / LGD','EAD / EL','集中度','评级迁徙','跨月归因'],C.teal,C.paleTeal],['组合管理',['风险偏好','组合限额','下月预测','压力测试','集团风险'],C.rose,C.paleRose],['管理输出',['风险驾驶舱','月末报告','客户360','集团视图','AI智能分析'],C.violet,C.paleViolet]];
  cols.forEach((c,i)=>{const x=.46+i*2.54;s.shape({x,y:1.25,w:2.22,h:4.85,type:'roundRect',fill:C.white,line:{color:c[2],width:1},shadow:true});s.shape({x:x+.12,y:1.42,w:1.98,h:.52,type:'roundRect',fill:c[3],line:{color:c[2],width:1},text:c[0],fontSize:11.5,bold:true,fontColor:c[2],align:'ctr',valign:'mid',tIns:0,bIns:0});c[1].forEach((n,j)=>mini(s,x+.22,2.18+j*.68,1.78,.4,n,c[2],j%2?C.white:c[3],8.7));if(i<4)s.connector(x+2.22,3.55,x+2.48,3.55,{color:C.line,width:1.2});});
  s.shape({x:.72,y:6.35,w:11.9,h:.38,type:'roundRect',fill:C.slate100,line:{color:C.line,width:1},text:'底层保障：数据质量与业务口径  ·  模型生命周期与验证  ·  权限安全与操作审计',fontSize:10.5,bold:true,fontColor:C.text,align:'ctr',valign:'mid',tIns:0,bIns:0}); return s.xml();
}

function bizDataChain() {
  const s=slideBuilder(); title(s,4,'信贷业务数据链路','系统保留业务对象之间的真实关联关系，风险结果可以追溯到具体债项',C.blue);
  const flow=[['集团与关联方','一个集团包含多个客户',C.violet,C.paleViolet],['客户信息','一个客户一条有效评级和额度',C.blue,C.paleBlue],['评级与额度','一笔额度可发起多笔申请',C.teal,C.paleTeal],['业务申请','一笔申请对应一笔合同',C.amber,C.paleAmber],['信贷合同','一笔合同可产生多笔支用并关联多个押品',C.green,C.paleGreen],['债项支用','债项产生逾期、违约和风险敞口',C.rose,C.paleRose]];
  flow.forEach((v,i)=>{const x=.45+i*2.08;s.shape({x,y:1.48,w:1.72,h:1.25,type:'roundRect',fill:C.white,line:{color:v[2],width:1},shadow:true});mini(s,x+.18,1.68,1.36,.42,v[0],v[2],v[3],9.5);s.shape({x:x+.15,y:2.18,w:1.42,h:.34,fill:null,line:false,text:v[1],fontSize:7.4,fontColor:C.muted,align:'ctr',valign:'mid',tIns:0,bIns:0,lIns:0,rIns:0});if(i<flow.length-1)s.connector(x+1.72,2.1,x+2.04,2.1,{color:C.line,width:1.1});});
  bulletPanel(s,.75,3.45,3.75,2.1,'组合分析维度',['行业、产品、机构、区域','集团、客户、评级、期限','组合可逐级下钻到合同和债项'],C.blue);
  bulletPanel(s,4.8,3.45,3.75,2.1,'核心风险数据',['PD、LGD、EAD、EL','评级迁徙、逾期天数、违约状态','押品价值、回收率和覆盖率'],C.teal);
  bulletPanel(s,8.85,3.45,3.75,2.1,'业务可追溯性',['风险指标关联客户、合同和债项','金额变化可定位到具体业务记录','预警案件自动关联风险台账'],C.rose); return s.xml();
}

function bizMonthEnd() {
  const s=slideBuilder(); title(s,5,'月末批量加工主线','每月月末统一接收、校验、计量和发布，形成可比较的正式组合快照',C.amber);
  const steps=[['1','数据接收','上游批次与数据日期',C.blue,C.paleBlue],['2','批次登记','清单、版本与校验值',C.teal,C.paleTeal],['3','质量检查','完整性、唯一性、关联性',C.amber,C.paleAmber],['4','月末快照','客户、债项与组合汇总',C.green,C.paleGreen],['5','正式发布','锁定口径并支持重算',C.violet,C.paleViolet],['6','跨月比较','变化、迁徙和归因',C.rose,C.paleRose]];
  steps.forEach((v,i)=>{const x=.5+i*2.08;s.shape({x,y:1.45,w:1.75,h:1.35,type:'roundRect',fill:C.white,line:{color:v[3],width:1},shadow:true});mini(s,x+.15,1.65,.45,.45,v[0],v[3],v[4],11);s.shape({x:x+.68,y:1.65,w:.9,h:.25,fill:null,line:false,text:v[1],fontSize:10.5,bold:true,fontColor:C.ink,tIns:0,bIns:0,lIns:0,rIns:0});s.shape({x:x+.16,y:2.2,w:1.42,h:.32,fill:null,line:false,text:v[2],fontSize:7.5,fontColor:C.muted,align:'ctr',tIns:0,bIns:0,lIns:0,rIns:0});if(i<5)s.connector(x+1.75,2.12,x+2.04,2.12,{color:C.line,width:1.1});});
  card(s,.82,3.55,3.75,1.65,'批次管理',['正式、重算、补录等运行模式','发布后锁定，修改保留版本和审计记录','源数据清单与加工结果可勾稽'],C.blue,C.white);
  card(s,4.8,3.55,3.75,1.65,'月末变化',['组合规模、客户数和债项数变化','PD、LGD、EAD、EL变化','新增、退出、还款、支用与违约变化'],C.teal,C.white);
  card(s,8.78,3.55,3.75,1.65,'质量整改',['检查异常形成问题清单','分派责任人、填写整改说明','处理完成后复核关闭'],C.rose,C.white);
  s.shape({x:1.25,y:5.85,w:10.8,h:.52,type:'roundRect',fill:C.paleAmber,line:{color:C.amber,width:1},text:'月末结果是正式分析与管理报告的共同基准，保证不同月份、不同部门使用同一套风险口径。',fontSize:11.5,bold:true,fontColor:C.amber,align:'ctr',valign:'mid',tIns:0,bIns:0}); return s.xml();
}

function bizMeasurement() {
  const s=slideBuilder(); title(s,6,'组合风险计量','以风险敞口为核心，将客户和债项风险统一转换为可比较的组合指标',C.teal);
  mini(s,5.12,2.55,3.05,1.05,'风险敞口',C.blue,C.paleBlue,22);
  const items=[['PD','客户或债项发生违约的可能性',1.0,1.35,C.amber,C.paleAmber],['LGD','发生违约后无法收回的损失比例',9.4,1.35,C.rose,C.paleRose],['EAD','发生违约时面临的风险暴露金额',1.0,4.45,C.blue,C.paleBlue],['EL','PD × LGD × EAD形成的预期损失',9.4,4.45,C.teal,C.paleTeal]];
  items.forEach(v=>card(s,v[2],v[3],2.95,1.35,v[0],[v[1]],v[4],v[5]));
  s.connector(3.95,2.02,5.12,2.78,{color:C.line,width:1.2});s.connector(9.4,2.02,8.17,2.78,{color:C.line,width:1.2});s.connector(3.95,5.12,5.12,3.38,{color:C.line,width:1.2});s.connector(9.4,5.12,8.17,3.38,{color:C.line,width:1.2});
  s.shape({x:3.9,y:5.82,w:5.55,h:.48,type:'roundRect',fill:C.white,line:{color:C.softLine,width:1},text:'输出：组合损失、风险等级、押品覆盖、评级迁徙、逾期违约和跨月变化',fontSize:10,bold:true,fontColor:C.text,align:'ctr',valign:'mid',shadow:true,tIns:0,bIns:0}); return s.xml();
}

function bizPortfolio() {
  const s=slideBuilder(); title(s,7,'集中度、限额与前瞻管理','从“本月是否超限”提升到“下月是否可能超限”',C.rose);
  const dims=['行业','产品','机构','区域','集团','评级','期限'];dims.forEach((d,i)=>mini(s,.72+i*1.78,1.28,1.35,.42,d,C.blue,i%2?C.white:C.paleBlue,9.2));
  const stages=[['风险偏好','管理层确定组合目标和风险边界',C.violet,C.paleViolet],['限额分解','将目标分解到行业、产品、机构和集团',C.blue,C.paleBlue],['当前监测','展示已用、可用、预警和超限',C.teal,C.paleTeal],['下月预测','叠加已审批业务预计支用',C.amber,C.paleAmber],['管理动作','调整投向、压降敞口、冻结或释放额度',C.rose,C.paleRose]];
  stages.forEach((v,i)=>{const x=.58+i*2.52;card(s,x,2.15,2.13,1.52,v[0],[v[1]],v[2],v[3]);if(i<4)s.connector(x+2.13,2.92,x+2.47,2.92,{color:C.line,width:1.1});});
  bulletPanel(s,.82,4.38,3.75,1.65,'正常',['预计使用率低于预警线','保持当前策略并持续监测'],C.green);
  bulletPanel(s,4.8,4.38,3.75,1.65,'预警',['预计使用率进入临界区间','收紧新增业务并制定压降计划'],C.amber);
  bulletPanel(s,8.78,4.38,3.75,1.65,'超限',['当前或预计占用突破限额','执行审批、豁免、整改或退出'],C.rose); return s.xml();
}

function bizStressGroup() {
  const s=slideBuilder(); title(s,8,'压力测试与集团客户风险','识别正常环境下不明显、但在压力和关联关系下可能放大的组合风险',C.amber);
  bulletPanel(s,.75,1.25,5.75,2.35,'压力测试',['内置基准、轻度下行和重度下行情景','同步冲击PD、LGD、EAD和押品价值','测算压力后EL、增量损失和限额突破','按行业输出需要优先关注的风险领域'],C.amber);
  bulletPanel(s,6.82,1.25,5.75,2.35,'集团客户风险',['聚合集团内所有成员企业风险敞口','识别股权控制、担保和共同实际控制关系','展示集团风险偏好使用率和押品覆盖率','任一成员违约可传导至集团风险判断'],C.violet);
  const flow=[['宏观与行业情景',C.blue,C.paleBlue],['参数冲击',C.amber,C.paleAmber],['组合损失测算',C.rose,C.paleRose],['限额与资本影响',C.violet,C.paleViolet],['管理措施',C.teal,C.paleTeal]];
  flow.forEach((v,i)=>{mini(s,1.1+i*2.42,4.6,1.72,.55,v[0],v[1],v[2],9.3);if(i<4)s.connector(2.82+i*2.42,4.88,3.47+i*2.42,4.88,{color:C.line,width:1.1});});
  s.shape({x:1.15,y:5.65,w:11.0,h:.58,type:'roundRect',fill:C.white,line:{color:C.softLine,width:1},text:'业务价值：避免只看单一客户和当前时点，提前发现集团传染、押品折价和行业下行带来的尾部损失。',fontSize:11,bold:true,fontColor:C.text,align:'ctr',valign:'mid',shadow:true,tIns:0,bIns:0}); return s.xml();
}

function bizAlert() {
  const s=slideBuilder(); title(s,9,'预警与处置闭环','风险识别不是终点，系统持续跟踪整改、复核和实际效果',C.rose);
  const steps=[['风险识别','逾期、违约、评级、PD、LGD、限额、押品',C.amber,C.paleAmber],['预警案件','按客户形成优先级和风险证据',C.rose,C.paleRose],['关联台账','自动关联客户、合同、债项和指标',C.blue,C.paleBlue],['整改任务','明确责任人、措施、期限和目标',C.amber,C.paleAmber],['处置复核','记录进度、结果和复核意见',C.green,C.paleGreen],['效果评价','评价有效、部分有效、无效或观察中',C.teal,C.paleTeal],['规则优化','根据效果调整阈值和预警规则',C.violet,C.paleViolet]];
  steps.forEach((v,i)=>{const angle=(Math.PI*2*i/7)-Math.PI/2,cx=6.65+Math.cos(angle)*4.2,cy=3.75+Math.sin(angle)*2.35;card(s,cx-1.05,cy-.48,2.1,.92,v[0],[v[1]],v[2],v[3]);});
  mini(s,5.15,3.12,3.0,1.2,'风险处置闭环',C.ink,C.white,18);
  s.shape({x:3.2,y:6.35,w:6.9,h:.32,fill:null,line:false,text:'所有状态同步回写风险台账、风险指标和风险事件，避免预警与业务管理脱节。',fontSize:10.5,bold:true,fontColor:C.rose,align:'ctr',tIns:0,bIns:0,lIns:0,rIns:0}); return s.xml();
}

function bizAI() {
  const s=slideBuilder(); title(s,10,'AI智能风险能力','AI贯穿查询、分析、预警、归因、处置和报告，但不替代正式风险决策',C.violet);
  const items=[['自然语言查询','用业务语言查询客户、集团和组合风险',C.blue,C.paleBlue],['智能风险画像','自动汇总客户业务、评级、逾期、违约和敞口',C.teal,C.paleTeal],['异常识别预警','从组合变化和外部信号中发现异常',C.amber,C.paleAmber],['变化智能归因','解释EAD、PD、LGD和EL为何变化',C.rose,C.paleRose],['处置与报告','生成处置建议、管理摘要和分析报告',C.green,C.paleGreen],['外部数据辅助','结合工商、司法、舆情等外部数据分析',C.violet,C.paleViolet]];
  items.forEach((v,i)=>{const x=.75+(i%3)*4.12,y=1.28+Math.floor(i/3)*2.0;card(s,x,y,3.65,1.45,v[0],[v[1]],v[2],v[3]);});
  s.shape({x:.9,y:5.65,w:11.55,h:.72,type:'roundRect',fill:C.slate100,line:{color:C.line,width:1},paragraphs:[{text:'AI治理边界',size:11,bold:true,color:C.ink,align:'ctr',after:70},{text:'辅助分析  ·  结论可解释  ·  来源可追溯  ·  敏感数据受控  ·  正式处置与模型发布必须人工复核',size:10,color:C.text,align:'ctr'}],valign:'mid',tIns:0,bIns:0}); return s.xml();
}

function bizRoles() {
  const s=slideBuilder(); title(s,11,'谁在使用系统','不同角色关注不同问题，系统支持个性化默认工作台',C.blue);
  const roles=[['管理层',['组合总体风险是否上升','哪些行业或集团需要调整投向','压力损失和限额突破情况'],C.violet,C.paleViolet],['风险经理',['预警客户和风险证据','跨月变化原因和重点债项','整改任务和处置效果'],C.rose,C.paleRose],['业务机构',['本机构客户风险情况','额度空间和业务准入影响','需要完成的整改任务'],C.teal,C.paleTeal],['模型人员',['PD/LGD/EAD偏差','模型版本、回溯与校准','预警规则效果'],C.amber,C.paleAmber],['数据人员',['月末批次和上游清单','数据质量、勾稽和问题整改','业务口径与数据血缘'],C.blue,C.paleBlue],['审计合规',['权限与敏感数据访问','审批、发布和操作留痕','风险报告口径一致性'],C.green,C.paleGreen]];
  roles.forEach((r,i)=>{const x=.72+(i%3)*4.18,y=1.22+Math.floor(i/3)*2.55;card(s,x,y,3.72,2.0,r[0],r[1],r[2],r[3]);}); return s.xml();
}

function bizOutputs() {
  const s=slideBuilder(); title(s,12,'业务输出与管理动作','系统的最终价值不是展示指标，而是支持管理决策和业务行动',C.teal);
  const out=[['组合风险驾驶舱','当前风险、预警客户、集中度和待办处置',C.blue,C.paleBlue],['月末组合变动报告','本月变化、风险迁徙、归因和重点明细',C.teal,C.paleTeal],['客户360风险画像','客户业务链、风险证据、时间轴和AI建议',C.green,C.paleGreen],['集团客户风险视图','集团成员、担保关系、集团敞口和传染风险',C.violet,C.paleViolet],['压力测试报告','压力后损失、限额突破和重点行业',C.amber,C.paleAmber],['模型与预警评价','模型偏差、校准建议和处置有效率',C.rose,C.paleRose]];
  out.forEach((v,i)=>{const x=.72+(i%3)*4.18,y=1.2+Math.floor(i/3)*1.75;card(s,x,y,3.72,1.3,v[0],[v[1]],v[2],v[3]);});
  s.shape({x:.9,y:5.0,w:11.5,h:1.15,type:'roundRect',fill:C.ink,line:false,paragraphs:[{text:'可执行管理动作',size:12,bold:true,color:C.white,align:'ctr',after:90},{text:'调整行业和区域投向  ·  压降高风险敞口  ·  收紧或释放额度  ·  增补押品担保  ·  调整定价期限  ·  限制或退出客户',size:11,color:C.white,align:'ctr'}],valign:'mid',shadow:true,tIns:0,bIns:0}); return s.xml();
}

function bizValue() {
  const s=slideBuilder(); title(s,13,'系统带来的业务价值','从信息汇总平台升级为组合风险管理与决策平台',C.green);
  const values=[['统一口径','月末锁定数据和指标口径，减少部门之间结果差异','一个数字'],['穿透分析','从组合逐级下钻到行业、集团、客户、合同和债项','找到原因'],['风险前瞻','通过限额预测、评级迁徙和压力测试提前识别风险','提前行动'],['管理闭环','预警、整改、复核、回写和效果评价全部在线完成','形成闭环'],['模型持续优化','使用实际违约和回收表现校准PD、LGD和EAD','越用越准'],['决策效率','自动生成风险画像、分析摘要和管理报告','提高效率']];
  values.forEach((v,i)=>{const x=.72+(i%3)*4.18,y=1.25+Math.floor(i/3)*2.15;s.shape({x,y,w:3.72,h:1.65,type:'roundRect',fill:C.white,line:{color:C.softLine,width:1},shadow:true});mini(s,x+.2,y+.22,1.05,.42,v[2],i%2?C.teal:C.blue,i%2?C.paleTeal:C.paleBlue,9);s.shape({x:x+1.45,y:y+.2,w:1.8,h:.26,fill:null,line:false,text:v[0],fontSize:12,bold:true,fontColor:C.ink,tIns:0,bIns:0,lIns:0,rIns:0});s.shape({x:x+.22,y:y+.82,w:3.1,h:.38,fill:null,line:false,text:v[1],fontSize:9,fontColor:C.muted,align:'ctr',valign:'mid',tIns:0,bIns:0,lIns:0,rIns:0});});
  s.shape({x:1.15,y:5.9,w:11.05,h:.54,type:'roundRect',fill:C.paleGreen,line:{color:C.green,width:1},text:'核心判断：系统已经具备组合信用风险管理的主要业务闭环，可支撑内部管理、月末分析和业务试运行。',fontSize:11.5,bold:true,fontColor:C.green,align:'ctr',valign:'mid',tIns:0,bIns:0}); return s.xml();
}

function bizRoadmap() {
  const s=slideBuilder(); title(s,14,'后续建设建议','从功能完善进一步迈向生产级银行组合风险管理平台',C.amber);
  const phases=[['近期：真实数据与批量生产化',['对接授信、核心、评级、押品和客户主数据','完善任务依赖、补数、重跑、回退和批次验收','固化月报、限额报告和压力测试报告'],C.blue,C.paleBlue],['中期：计量与模型验证深化',['完善PD区分度、校准度和稳定性检验','补充LGD回收现金流和EAD/CCF验证','完善集团关系、担保圈和风险传染分析'],C.teal,C.paleTeal],['远期：资本与会计应用',['建设IFRS 9/ECL与宏观情景权重','建设RWA、经济资本和RAROC','完善监管报送、高可用、灾备和安全审计'],C.violet,C.paleViolet]];
  phases.forEach((p,i)=>bulletPanel(s,.78+i*4.15,1.35,3.7,3.75,p[0],p[1],p[2]));
  s.shape({x:1.0,y:5.65,w:11.3,h:.72,type:'roundRect',fill:C.white,line:{color:C.softLine,width:1},paragraphs:[{text:'建设原则',size:11,bold:true,color:C.ink,align:'ctr',after:70},{text:'先保证月末数据可信和风险口径一致，再扩展高级模型、资本计量与监管应用。',size:10.5,color:C.text,align:'ctr'}],shadow:true,valign:'mid',tIns:0,bIns:0}); return s.xml();
}

function bizEnd() {
  const s=slideBuilder('F3F6FA');s.shape({x:.82,y:.92,w:1.0,h:.1,fill:C.teal,line:false});s.shape({x:.82,y:1.45,w:11,h:.6,fill:null,line:false,text:'组合风险系统的核心价值',fontSize:28,bold:true,fontColor:C.ink,lIns:0,rIns:0,tIns:0,bIns:0});
  s.shape({x:.85,y:2.35,w:10.9,h:1.15,type:'roundRect',fill:C.white,line:{color:C.softLine,width:1},paragraphs:[{text:'看清组合风险',size:20,bold:true,color:C.blue,align:'ctr',after:120},{text:'解释风险为什么变化，判断风险将向哪里发展，并推动管理措施真正落地。',size:14,color:C.text,align:'ctr'}],shadow:true,valign:'mid',tIns:0,bIns:0});
  const p=[['统一数据','可信'],['统一计量','可比'],['穿透分析','可查'],['限额预警','可控'],['处置闭环','可管'],['AI辅助','高效']];p.forEach((v,i)=>{mini(s,1.02+i*2.02,4.35,1.48,.52,v[0],i%2?C.teal:C.blue,i%2?C.paleTeal:C.paleBlue,10);s.shape({x:1.02+i*2.02,y:4.98,w:1.48,h:.26,fill:null,line:false,text:v[1],fontSize:9,bold:true,fontColor:C.muted,align:'ctr',tIns:0,bIns:0,lIns:0,rIns:0});});
  s.shape({x:4.75,y:6.05,w:3.8,h:.42,fill:null,line:false,text:'谢谢',fontSize:22,bold:true,fontColor:C.teal,align:'ctr',tIns:0,bIns:0,lIns:0,rIns:0});return s.xml();
}

const businessSlides=[bizCover(),bizWhy(),bizPanorama(),bizDataChain(),bizMonthEnd(),bizMeasurement(),bizPortfolio(),bizStressGroup(),bizAlert(),bizAI(),bizRoles(),bizOutputs(),bizValue(),bizRoadmap(),bizEnd()];

const technicalSlides = [
  coverSlide(),
  overviewSlide(),
  agendaSlide(),
  generationSlide(),
  codeOrganizationSlide(),
  overallArchitectureSlide(),
  backendSlide(),
  frontendSlide(),
  deploymentSlide(),
  deploymentProcessSlide(),
  dataFlowSlide(),
  authFlowSlide(),
  moduleMapSlide(),
  riskModulesSlide(),
  systemModulesSlide(),
  securitySlide(),
  observabilitySlide(),
  implementationPlanSlide(),
  acceptanceSlide(),
  risksSlide()
];
const slides = businessMode ? businessSlides : technicalSlides;

const files = [];
const add = (name, data) => files.push({ name, data: Buffer.from(data, 'utf8') });

add('[Content_Types].xml', contentTypes(slides.length));
add('_rels/.rels', rootRels());
add('docProps/app.xml', appProps(slides.length));
add('docProps/core.xml', coreProps());
add('ppt/presentation.xml', presentationXml(slides.length));
add('ppt/_rels/presentation.xml.rels', presentationRels(slides.length));
add('ppt/slideMasters/slideMaster1.xml', slideMasterXml());
add('ppt/slideMasters/_rels/slideMaster1.xml.rels', slideMasterRels());
add('ppt/slideLayouts/slideLayout1.xml', slideLayoutXml());
add('ppt/slideLayouts/_rels/slideLayout1.xml.rels', slideLayoutRels());
add('ppt/theme/theme1.xml', themeXml());
slides.forEach((xml, i) => {
  add(`ppt/slides/slide${i + 1}.xml`, xml);
  add(`ppt/slides/_rels/slide${i + 1}.xml.rels`, slideRel());
});

fs.mkdirSync(outDir, { recursive: true });
makeZip(files, outFile);
console.log(outFile);
