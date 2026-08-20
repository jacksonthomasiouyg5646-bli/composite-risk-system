import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(scriptDir, '..')
const docsDir = path.join(rootDir, 'docs')
const documentName = process.argv[2] || '组合风险系统详细需求说明书'
const documentTitle = process.argv[3] || documentName
const sourcePath = path.join(docsDir, documentName + '.md')
const htmlPath = path.join(docsDir, documentName + '.html')
const packageDir = path.join(docsDir, '.openxml-docx')

const source = fs.readFileSync(sourcePath, 'utf8').replace(/\r\n/g, '\n')

function xmlEscape(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

function htmlEscape(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function isTableSeparator(line) {
  const cells = line.split('|').slice(1, -1)
  return cells.length > 0 && cells.every((cell) => /^\s*:?-{3,}:?\s*$/.test(cell))
}

function parseTableRow(line) {
  return line.split('|').slice(1, -1).map((cell) => cell.trim())
}

function isSpecial(line, nextLine) {
  return /^#{1,6}\s+/.test(line)
    || /^<!--\s*PAGEBREAK\s*-->$/.test(line)
    || /^!\[[^\]]*\]\([^)]+\)$/.test(line)
    || /^-\s+/.test(line)
    || /^\d+\.\s+/.test(line)
    || (line.startsWith('|') && nextLine && isTableSeparator(nextLine))
}

function parseMarkdown(markdown) {
  const lines = markdown.split('\n')
  const elements = []
  let index = 0

  while (index < lines.length) {
    const line = lines[index].trimEnd()
    const trimmed = line.trim()

    if (!trimmed) {
      index += 1
      continue
    }

    if (/^<!--\s*PAGEBREAK\s*-->$/.test(trimmed)) {
      elements.push({ type: 'pagebreak' })
      index += 1
      continue
    }

    const heading = trimmed.match(/^(#{1,6})\s+(.+)$/)
    if (heading) {
      elements.push({ type: 'heading', level: heading[1].length, text: heading[2].trim() })
      index += 1
      continue
    }

    const image = trimmed.match(/^!\[([^\]]*)\]\(([^)]+)\)$/)
    if (image) {
      elements.push({ type: 'image', alt: image[1].trim(), src: image[2].trim() })
      index += 1
      continue
    }

    if (trimmed.startsWith('|') && index + 1 < lines.length && isTableSeparator(lines[index + 1])) {
      const rows = [parseTableRow(trimmed)]
      index += 2
      while (index < lines.length && lines[index].trim().startsWith('|')) {
        rows.push(parseTableRow(lines[index].trim()))
        index += 1
      }
      elements.push({ type: 'table', rows })
      continue
    }

    const unordered = trimmed.match(/^-\s+(.+)$/)
    if (unordered) {
      elements.push({ type: 'list', ordered: false, text: unordered[1] })
      index += 1
      continue
    }

    const ordered = trimmed.match(/^(\d+)\.\s+(.+)$/)
    if (ordered) {
      elements.push({ type: 'list', ordered: true, number: ordered[1], text: ordered[2] })
      index += 1
      continue
    }

    const paragraph = [trimmed]
    index += 1
    while (index < lines.length) {
      const candidate = lines[index].trim()
      const next = index + 1 < lines.length ? lines[index + 1] : ''
      if (!candidate || isSpecial(candidate, next)) break
      paragraph.push(candidate)
      index += 1
    }
    elements.push({ type: 'paragraph', text: paragraph.join(' ') })
  }

  return elements
}

function htmlFor(elements) {
  const toc = elements
    .filter((item) => item.type === 'heading' && item.level >= 2 && item.level <= 3)
    .map((item, index) => ({ ...item, id: 'section-' + (index + 1) }))
  let tocIndex = 0
  let afterCover = false
  let tocInserted = false
  const body = []

  for (const element of elements) {
    if (element.type === 'pagebreak') {
      body.push('<div class="page-break"></div>')
      afterCover = true
      if (!tocInserted) {
        body.push('<section class="toc"><h2>目录</h2>')
        for (const item of toc) {
          body.push('<a class="toc-' + item.level + '" href="#' + item.id + '">' + htmlEscape(item.text) + '</a>')
        }
        body.push('</section><div class="page-break"></div>')
        tocInserted = true
      }
      continue
    }

    if (element.type === 'heading') {
      let id = ''
      if (element.level >= 2 && element.level <= 3) {
        id = ' id="' + toc[tocIndex].id + '"'
        tocIndex += 1
      }
      const className = !afterCover && element.level === 1 ? ' class="document-title"' : ''
      body.push('<h' + element.level + id + className + '>' + htmlEscape(element.text) + '</h' + element.level + '>')
      continue
    }

    if (element.type === 'paragraph') {
      body.push('<p>' + htmlEscape(element.text).replace(/  $/, '') + '</p>')
      continue
    }

    if (element.type === 'list') {
      body.push('<p class="list">' + (element.ordered ? element.number + '. ' : '• ') + htmlEscape(element.text) + '</p>')
      continue
    }

    if (element.type === 'image') {
      body.push('<figure><img src="' + htmlEscape(element.src) + '" alt="' + htmlEscape(element.alt) + '"><figcaption>' + htmlEscape(element.alt) + '</figcaption></figure>')
      continue
    }

    if (element.type === 'table') {
      body.push('<table><thead><tr>')
      for (const cell of element.rows[0]) body.push('<th>' + htmlEscape(cell) + '</th>')
      body.push('</tr></thead><tbody>')
      for (const row of element.rows.slice(1)) {
        body.push('<tr>')
        for (const cell of row) body.push('<td>' + htmlEscape(cell) + '</td>')
        body.push('</tr>')
      }
      body.push('</tbody></table>')
    }
  }

  return '<!doctype html><html lang="zh-CN"><head><meta charset="utf-8">'
    + '<meta name="viewport" content="width=device-width,initial-scale=1">'
    + '<title>' + htmlEscape(documentTitle) + '</title>'
    + '<style>'
    + '@page{size:A4;margin:20mm 18mm 20mm 22mm;}'
    + '*{box-sizing:border-box;}body{margin:0 auto;max-width:210mm;padding:18mm 18mm 24mm 22mm;color:#1f2937;background:#fff;font-family:"Microsoft YaHei","PingFang SC",sans-serif;font-size:11pt;line-height:1.65;}'
    + 'h1,h2,h3,h4{color:#163a5f;page-break-after:avoid;}h1{font-size:24pt;text-align:center;margin:52mm 0 18mm;}h2{font-size:17pt;border-bottom:1.5px solid #2b6f9f;padding-bottom:5px;margin-top:24px;}h3{font-size:13.5pt;margin-top:20px;}h4{font-size:12pt;}'
    + 'p{margin:7px 0;text-align:justify;}.document-title{font-size:28pt;letter-spacing:0;margin-top:58mm;}'
    + '.list{padding-left:18px;margin:4px 0;}table{width:100%;border-collapse:collapse;margin:10px 0 16px;font-size:9.3pt;page-break-inside:auto;}tr{page-break-inside:avoid;}th,td{border:1px solid #9ca8b5;padding:6px 7px;vertical-align:top;}th{background:#dbeaf4;color:#15354f;font-weight:700;text-align:left;}tbody tr:nth-child(even){background:#f7fafc;}'
    + 'figure{margin:14px 0 20px;page-break-inside:avoid;}figure img{display:block;width:100%;height:auto;border:1px solid #cbd5e1;}figcaption{text-align:center;color:#52606d;font-size:9.5pt;margin-top:6px;}'
    + '.page-break{page-break-before:always;height:0;}.toc{padding-top:5mm;}.toc a{display:block;color:#243b53;text-decoration:none;border-bottom:1px dotted #cbd5e1;padding:3px 0;}.toc-3{padding-left:18px;font-size:10pt;}'
    + '@media print{body{max-width:none;padding:0;}.toc a{color:#000;}}'
    + '</style></head><body>' + body.join('') + '</body></html>'
}

function wordRun(text, options = {}) {
  const bold = options.bold ? '<w:b/>' : ''
  const size = options.size ? '<w:sz w:val="' + options.size + '"/><w:szCs w:val="' + options.size + '"/>' : ''
  const color = options.color ? '<w:color w:val="' + options.color + '"/>' : ''
  return '<w:r><w:rPr>' + bold + size + color
    + '<w:rFonts w:ascii="Microsoft YaHei" w:hAnsi="Microsoft YaHei" w:eastAsia="Microsoft YaHei"/>'
    + '</w:rPr><w:t xml:space="preserve">' + xmlEscape(text) + '</w:t></w:r>'
}

function wordParagraph(text, style = 'Normal', options = {}) {
  const alignment = options.center ? '<w:jc w:val="center"/>' : ''
  const pageBreak = options.pageBreak ? '<w:pageBreakBefore/>' : ''
  const indent = options.indent ? '<w:ind w:left="' + options.indent + '"/>' : ''
  const spacing = options.spacing || '<w:spacing w:after="120" w:line="330" w:lineRule="auto"/>'
  return '<w:p><w:pPr><w:pStyle w:val="' + style + '"/>' + alignment + pageBreak + indent + spacing + '</w:pPr>'
    + wordRun(text, options) + '</w:p>'
}

function wordTable(rows) {
  const maxColumns = Math.max(...rows.map((row) => row.length))
  const width = Math.floor(10000 / maxColumns)
  const grid = new Array(maxColumns).fill('<w:gridCol w:w="' + width + '"/>').join('')
  const body = rows.map((row, rowIndex) => {
    const cells = row.map((cell) => {
      const shading = rowIndex === 0 ? '<w:shd w:fill="D9EAF5"/>' : ''
      return '<w:tc><w:tcPr><w:tcW w:w="' + width + '" w:type="dxa"/>' + shading + '<w:vAlign w:val="top"/></w:tcPr>'
        + wordParagraph(cell, 'TableText', { bold: rowIndex === 0, size: 18, spacing: '<w:spacing w:after="30" w:line="260" w:lineRule="auto"/>' })
        + '</w:tc>'
    }).join('')
    return '<w:tr><w:trPr><w:cantSplit/></w:trPr>' + cells + '</w:tr>'
  }).join('')
  return '<w:tbl><w:tblPr><w:tblW w:w="10000" w:type="dxa"/><w:tblLayout w:type="fixed"/>'
    + '<w:tblBorders><w:top w:val="single" w:sz="4" w:color="8A9AAA"/><w:left w:val="single" w:sz="4" w:color="8A9AAA"/>'
    + '<w:bottom w:val="single" w:sz="4" w:color="8A9AAA"/><w:right w:val="single" w:sz="4" w:color="8A9AAA"/>'
    + '<w:insideH w:val="single" w:sz="4" w:color="B4BEC8"/><w:insideV w:val="single" w:sz="4" w:color="B4BEC8"/></w:tblBorders>'
    + '</w:tblPr><w:tblGrid>' + grid + '</w:tblGrid>' + body + '</w:tbl>'
}

function pngSize(filePath) {
  const buffer = fs.readFileSync(filePath)
  if (buffer.length < 24 || buffer.toString('ascii', 1, 4) !== 'PNG') {
    return { width: 1440, height: 1000 }
  }
  return { width: buffer.readUInt32BE(16), height: buffer.readUInt32BE(20) }
}

function wordImage(element, index) {
  const imagePath = path.resolve(path.dirname(sourcePath), element.src)
  const size = pngSize(imagePath)
  const maxWidth = 5500000
  const maxHeight = 6000000
  let width = maxWidth
  let height = Math.round(width * size.height / size.width)
  if (height > maxHeight) {
    height = maxHeight
    width = Math.round(height * size.width / size.height)
  }
  const relationshipId = 'rId' + (index + 2)
  const drawing = '<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:before="120" w:after="60"/></w:pPr><w:r><w:drawing>'
    + '<wp:inline xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" distT="0" distB="0" distL="0" distR="0">'
    + '<wp:extent cx="' + width + '" cy="' + height + '"/><wp:effectExtent l="0" t="0" r="0" b="0"/>'
    + '<wp:docPr id="' + (index + 1) + '" name="Screenshot ' + (index + 1) + '" descr="' + xmlEscape(element.alt) + '"/>'
    + '<wp:cNvGraphicFramePr><a:graphicFrameLocks xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" noChangeAspect="1"/></wp:cNvGraphicFramePr>'
    + '<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">'
    + '<pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="' + (index + 1) + '" name="image' + (index + 1) + '.png"/><pic:cNvPicPr/></pic:nvPicPr>'
    + '<pic:blipFill><a:blip xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" r:embed="' + relationshipId + '"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
    + '<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="' + width + '" cy="' + height + '"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom><a:ln><a:solidFill><a:srgbClr val="CBD5E1"/></a:solidFill></a:ln></pic:spPr>'
    + '</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>'
  return drawing + wordParagraph(element.alt, 'CoverText', { center: true, size: 18, color: '52606D', spacing: '<w:spacing w:after="180"/>' })
}

function documentXml(elements) {
  const parts = []
  let beforeFirstBreak = true
  let tocInserted = false
  let imageIndex = 0

  for (const element of elements) {
    if (element.type === 'pagebreak') {
      parts.push('<w:p><w:r><w:br w:type="page"/></w:r></w:p>')
      beforeFirstBreak = false
      if (!tocInserted) {
        parts.push(wordParagraph('目录', 'Heading1'))
        parts.push('<w:p><w:fldSimple w:instr="TOC \\o &quot;1-3&quot; \\h \\z \\u"><w:r><w:t>打开文档后更新目录字段</w:t></w:r></w:fldSimple></w:p>')
        parts.push('<w:p><w:r><w:br w:type="page"/></w:r></w:p>')
        tocInserted = true
      }
      continue
    }

    if (element.type === 'heading') {
      if (element.level === 1) {
        parts.push(wordParagraph(element.text, 'Title', { center: true, bold: true, size: 56, color: '163A5F', spacing: '<w:spacing w:before="2400" w:after="720"/>' }))
      } else {
        parts.push(wordParagraph(element.text, 'Heading' + Math.min(element.level - 1, 3)))
      }
      continue
    }

    if (element.type === 'paragraph') {
      parts.push(wordParagraph(element.text, beforeFirstBreak ? 'CoverText' : 'Normal', { center: beforeFirstBreak, size: beforeFirstBreak ? 22 : 21 }))
      continue
    }

    if (element.type === 'list') {
      const prefix = element.ordered ? element.number + '. ' : '• '
      parts.push(wordParagraph(prefix + element.text, 'ListParagraph', { indent: 420, size: 21 }))
      continue
    }

    if (element.type === 'image') {
      parts.push(wordImage(element, imageIndex))
      imageIndex += 1
      continue
    }

    if (element.type === 'table') parts.push(wordTable(element.rows))
  }

  return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
    + '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">'
    + '<w:body>' + parts.join('')
    + '<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1134" w:right="1021" w:bottom="1134" w:left="1247" w:header="567" w:footer="567" w:gutter="0"/>'
    + '<w:cols w:space="425"/><w:docGrid w:linePitch="312"/></w:sectPr></w:body></w:document>'
}

const stylesXml = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
  + '<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">'
  + '<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Microsoft YaHei" w:hAnsi="Microsoft YaHei" w:eastAsia="Microsoft YaHei"/><w:sz w:val="21"/><w:szCs w:val="21"/></w:rPr></w:rPrDefault>'
  + '<w:pPrDefault><w:pPr><w:spacing w:after="120" w:line="330" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>'
  + '<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:qFormat/><w:pPr><w:jc w:val="both"/></w:pPr></w:style>'
  + '<w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/><w:next w:val="CoverText"/><w:qFormat/><w:pPr><w:jc w:val="center"/></w:pPr></w:style>'
  + '<w:style w:type="paragraph" w:styleId="CoverText"><w:name w:val="Cover Text"/><w:basedOn w:val="Normal"/><w:pPr><w:jc w:val="center"/></w:pPr></w:style>'
  + '<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/><w:spacing w:before="360" w:after="180"/><w:outlineLvl w:val="0"/></w:pPr><w:rPr><w:b/><w:color w:val="163A5F"/><w:sz w:val="34"/></w:rPr></w:style>'
  + '<w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/><w:spacing w:before="280" w:after="120"/><w:outlineLvl w:val="1"/></w:pPr><w:rPr><w:b/><w:color w:val="1F5A82"/><w:sz w:val="28"/></w:rPr></w:style>'
  + '<w:style w:type="paragraph" w:styleId="Heading3"><w:name w:val="heading 3"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/><w:pPr><w:keepNext/><w:spacing w:before="220" w:after="100"/><w:outlineLvl w:val="2"/></w:pPr><w:rPr><w:b/><w:color w:val="2B6F9F"/><w:sz w:val="24"/></w:rPr></w:style>'
  + '<w:style w:type="paragraph" w:styleId="ListParagraph"><w:name w:val="List Paragraph"/><w:basedOn w:val="Normal"/></w:style>'
  + '<w:style w:type="paragraph" w:styleId="TableText"><w:name w:val="Table Text"/><w:basedOn w:val="Normal"/><w:pPr><w:jc w:val="left"/></w:pPr></w:style>'
  + '</w:styles>'

const contentTypes = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
  + '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
  + '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
  + '<Default Extension="xml" ContentType="application/xml"/>'
  + '<Default Extension="png" ContentType="image/png"/>'
  + '<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>'
  + '<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>'
  + '<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>'
  + '<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>'
  + '</Types>'

const rootRels = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
  + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
  + '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>'
  + '<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>'
  + '<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>'
  + '</Relationships>'

function documentRelationships(elements) {
  const relationships = ['<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>']
  elements.filter((item) => item.type === 'image').forEach((item, index) => {
    relationships.push('<Relationship Id="rId' + (index + 2) + '" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image' + (index + 1) + '.png"/>')
  })
  return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
    + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
    + relationships.join('') + '</Relationships>'
}

const coreXml = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
  + '<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">'
  + '<dc:title>' + xmlEscape(documentTitle) + '</dc:title><dc:subject>组合风险系统项目文档</dc:subject><dc:creator>项目组</dc:creator>'
  + '<cp:lastModifiedBy>项目组</cp:lastModifiedBy><dcterms:created xsi:type="dcterms:W3CDTF">2026-07-23T00:00:00Z</dcterms:created>'
  + '<dcterms:modified xsi:type="dcterms:W3CDTF">2026-07-23T00:00:00Z</dcterms:modified></cp:coreProperties>'

const appXml = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
  + '<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">'
  + '<Application>Microsoft Office Word</Application><DocSecurity>0</DocSecurity><ScaleCrop>false</ScaleCrop><Company>项目组</Company>'
  + '<AppVersion>16.0000</AppVersion></Properties>'

const elements = parseMarkdown(source)
fs.writeFileSync(htmlPath, htmlFor(elements), 'utf8')

fs.rmSync(packageDir, { recursive: true, force: true })
fs.mkdirSync(path.join(packageDir, '_rels'), { recursive: true })
fs.mkdirSync(path.join(packageDir, 'word', '_rels'), { recursive: true })
fs.mkdirSync(path.join(packageDir, 'word', 'media'), { recursive: true })
fs.mkdirSync(path.join(packageDir, 'docProps'), { recursive: true })

fs.writeFileSync(path.join(packageDir, '[Content_Types].xml'), contentTypes, 'utf8')
fs.writeFileSync(path.join(packageDir, '_rels', '.rels'), rootRels, 'utf8')
fs.writeFileSync(path.join(packageDir, 'word', 'document.xml'), documentXml(elements), 'utf8')
fs.writeFileSync(path.join(packageDir, 'word', 'styles.xml'), stylesXml, 'utf8')
fs.writeFileSync(path.join(packageDir, 'word', '_rels', 'document.xml.rels'), documentRelationships(elements), 'utf8')
fs.writeFileSync(path.join(packageDir, 'docProps', 'core.xml'), coreXml, 'utf8')
fs.writeFileSync(path.join(packageDir, 'docProps', 'app.xml'), appXml, 'utf8')
elements.filter((item) => item.type === 'image').forEach((item, index) => {
  const imagePath = path.resolve(path.dirname(sourcePath), item.src)
  fs.copyFileSync(imagePath, path.join(packageDir, 'word', 'media', 'image' + (index + 1) + '.png'))
})

console.log(JSON.stringify({
  source: sourcePath,
  html: htmlPath,
  package: packageDir,
  sections: elements.filter((item) => item.type === 'heading').length,
  tables: elements.filter((item) => item.type === 'table').length,
  images: elements.filter((item) => item.type === 'image').length
}))
