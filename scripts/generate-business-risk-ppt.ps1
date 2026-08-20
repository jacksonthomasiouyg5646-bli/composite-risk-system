param([string]$OutputPath = 'D:\software\workspace\组合风险系统正式业务汇报.pptx')

$ErrorActionPreference = 'Stop'
$ppt = New-Object -ComObject PowerPoint.Application
$ppt.Visible = -1
$presentation = $ppt.Presentations.Add()
$presentation.PageSetup.SlideWidth = 960
$presentation.PageSetup.SlideHeight = 540

$C = @{ Navy=0x472817; Blue=0xAD8143; Teal=0x6E920F; Orange=0x2D86DA; Red=0x4B52C7; Green=0x749232; Purple=0xA86977; White=0xFFFFFF; Bg=0xFAF8F5; Text=0x453220; Muted=0x8B7560; Line=0xE5D7C9; PaleBlue=0xFBF3EA; PaleTeal=0xF2F7EA; PaleOrange=0xE5F3FF; PaleRed=0xEBECFC; PaleGreen=0xF2F7EA; PalePurple=0xFAEFF2; Gray=0xF7F4F1 }
$W=960; $H=540

function Add-Text($slide,$x,$y,$w,$h,$text,$size=18,$color=$C.Text,$bold=$false,$align=1) {
  $shape=$slide.Shapes.AddTextbox(1,$x,$y,$w,$h); $shape.TextFrame.TextRange.Text=$text
  $shape.TextFrame.TextRange.Font.Name='Microsoft YaHei'; $shape.TextFrame.TextRange.Font.Size=$size; $shape.TextFrame.TextRange.Font.Color.RGB=$color
  $shape.TextFrame.TextRange.Font.Bold=if($bold){-1}else{0}; $shape.TextFrame.TextRange.ParagraphFormat.Alignment=$align
  $shape.TextFrame.MarginLeft=4; $shape.TextFrame.MarginRight=4; $shape.TextFrame.MarginTop=2; $shape.TextFrame.MarginBottom=2
  return $shape
}
function Add-Box($slide,$x,$y,$w,$h,$title,$body,$accent=$C.Blue,$fill=$C.White) {
  $box=$slide.Shapes.AddShape(5,$x,$y,$w,$h); $box.Fill.ForeColor.RGB=$fill; $box.Line.ForeColor.RGB=$accent; $box.Line.Weight=1
  Add-Text $slide ($x+12) ($y+9) ($w-24) 24 $title 14 $accent $true 1 | Out-Null
  Add-Text $slide ($x+12) ($y+39) ($w-24) ($h-47) $body 10 $C.Text $false 1 | Out-Null
}
function Add-Title($slide,$no,$title,$subtitle,$accent=$C.Teal) {
  $slide.Background.Fill.ForeColor.RGB=$C.Bg
  $bar=$slide.Shapes.AddShape(1,30,23,7,40); $bar.Fill.ForeColor.RGB=$accent; $bar.Line.Visible=0
  Add-Text $slide 48 20 850 34 "$no  $title" 22 $C.Navy $true 1 | Out-Null
  Add-Text $slide 49 55 850 20 $subtitle 10 $C.Muted $false 1 | Out-Null
  $line=$slide.Shapes.AddLine(30,82,930,82); $line.Line.ForeColor.RGB=$C.Line
}
function Add-Bullets($slide,$x,$y,$w,$h,$items,$size=12,$color=$C.Text) {
  $shape=Add-Text $slide $x $y $w $h ($items -join "`r") $size $color $false 1
  for($i=1;$i -le $items.Count;$i++){ $p=$shape.TextFrame.TextRange.Paragraphs($i); $p.ParagraphFormat.Bullet.Visible=-1; $p.ParagraphFormat.SpaceAfter=8 }
}
function Add-Footer($slide,$page) { Add-Text $slide 30 510 880 15 '组合风险系统业务介绍' 8 $C.Muted $false 1 | Out-Null; Add-Text $slide 900 510 30 15 "$page" 8 $C.Muted $false 3 | Out-Null }
function New-Slide { return $presentation.Slides.Add($presentation.Slides.Count+1,12) }
function New-SlideAt($index) { return $presentation.Slides.Add($index,12) }

# 1 封面
$s=New-Slide; $s.Background.Fill.ForeColor.RGB=$C.Bg
$bar=$s.Shapes.AddShape(1,55,62,75,7);$bar.Fill.ForeColor.RGB=$C.Teal;$bar.Line.Visible=0
Add-Text $s 55 105 810 70 '组合风险系统' 34 $C.Navy $true 1|Out-Null
Add-Text $s 58 178 800 35 '面向业务人员的组合信用风险管理全景介绍' 20 $C.Teal $false 1|Out-Null
Add-Box $s 58 255 610 105 '核心主线' "月末批量分析  ·  组合风险计量  ·  限额预警`r风险处置闭环  ·  管理决策  ·  AI智能分析" $C.Blue $C.White
@(@('PD','违约概率',$C.Orange,$C.PaleOrange),@('LGD','违约损失率',$C.Red,$C.PaleRed),@('EAD','风险暴露',$C.Blue,$C.PaleBlue),@('EL','预期损失',$C.Teal,$C.PaleTeal))|ForEach-Object -Begin{$i=0}-Process{$x=710+($i%2)*100;$y=245+[math]::Floor($i/2)*105;Add-Box $s $x $y 86 78 $_[0] $_[1] $_[2] $_[3];$i++}
Add-Text $s 58 465 770 25 '适用对象：管理层、风险经理、业务机构、模型人员、数据管理人员' 11 $C.Muted $false 1|Out-Null
Add-Text $s 830 465 75 25 '2026年7月' 10 $C.Muted $false 3|Out-Null

# 2 背景与目标
$s=New-Slide;Add-Title $s '01' '为什么建设组合风险系统' '把分散的客户和债项风险，转化为可管理、可比较、可处置的组合风险' $C.Teal
$cards=@(@('看不全','客户、合同、债项、押品、逾期和违约信息分散。','建立统一风险视图',$C.Blue,$C.PaleBlue),@('看不清','组合规模变化后，难以解释是业务还是风险参数造成。','形成跨月变化归因',$C.Orange,$C.PaleOrange),@('看不早','超限和风险恶化经常在结果发生后才被发现。','建立前瞻限额和预警',$C.Red,$C.PaleRed),@('管不住','预警、整改、复核和效果评价之间缺少闭环。','形成处置管理闭环',$C.Teal,$C.PaleTeal));for($i=0;$i -lt 4;$i++){$x=55+($i%2)*445;$y=115+[math]::Floor($i/2)*145;Add-Box $s $x $y 410 120 $cards[$i][0] ($cards[$i][1]+"`r`r系统作用："+$cards[$i][2]) $cards[$i][3] $cards[$i][4]}
Add-Box $s 55 420 855 58 '业务目标' '控制集中度与尾部风险  ·  提前识别组合恶化  ·  防止限额突破  ·  支持投向与资源配置' $C.Navy $C.Gray;Add-Footer $s 2

# 3 全景图
$s=New-Slide;Add-Title $s '02' '组合风险系统业务全景' '数据、加工、计量、管理、决策和AI能力贯通' $C.Blue
$img='D:\software\workspace\user-management-distributed\docs\组合风险系统业务全景图-优化版.png';$s.Shapes.AddPicture($img,0,-1,32,93,896,390)|Out-Null;Add-Footer $s 3

# 4 数据链路
$s=New-Slide;Add-Title $s '03' '信贷业务数据链路' '系统保留真实业务关系，风险结果可以追溯到具体债项' $C.Blue
$flow=@('集团与关联方','客户信息','评级与额度','业务申请','信贷合同','债项支用','逾期与违约');for($i=0;$i -lt $flow.Count;$i++){$x=34+$i*130;Add-Box $s $x 135 108 74 $flow[$i] '' $C.Blue $(if($i -ge 5){$C.PaleOrange}else{$C.PaleBlue});if($i -lt $flow.Count-1){$ln=$s.Shapes.AddLine($x+108,172,$x+128,172);$ln.Line.EndArrowheadStyle=3;$ln.Line.ForeColor.RGB=$C.Muted}}
Add-Box $s 55 265 265 150 '组合分析维度' "行业、产品、机构、区域`r集团、客户、评级、期限`r可逐级下钻到合同和债项" $C.Blue $C.White
Add-Box $s 347 265 265 150 '核心风险数据' "PD、LGD、EAD、EL`r评级迁徙、逾期天数、违约状态`r押品价值、回收率和覆盖率" $C.Teal $C.White
Add-Box $s 639 265 265 150 '业务可追溯性' "指标关联客户、合同和债项`r金额变化定位到具体业务记录`r预警案件自动关联风险台账" $C.Red $C.White;Add-Footer $s 4

# 5 月末加工
$s=New-Slide;Add-Title $s '04' '月末批量加工主线' '每月月末统一接收、校验、计量和发布，形成正式组合快照' $C.Orange
$steps=@(@('1','数据接收','批次与日期'),@('2','批次登记','清单与版本'),@('3','质量检查','完整性与勾稽'),@('4','月末快照','客户与债项'),@('5','发布锁定','正式口径'),@('6','跨月比较','变化与归因'));for($i=0;$i -lt 6;$i++){$x=35+$i*150;Add-Box $s $x 125 125 92 ($steps[$i][0]+'  '+$steps[$i][1]) $steps[$i][2] $(if($i -eq 2){$C.Orange}elseif($i -ge 3){$C.Teal}else{$C.Blue}) $(if($i -eq 2){$C.PaleOrange}elseif($i -ge 3){$C.PaleTeal}else{$C.PaleBlue})}
Add-Box $s 55 285 265 150 '批次管理' "正式、重算、补录等运行模式`r发布后锁定并保留审计记录`r源数据清单与结果可勾稽" $C.Blue $C.White
Add-Box $s 347 285 265 150 '月末变化' "组合规模和客户债项变化`rPD、LGD、EAD、EL变化`r新增、退出、还款、支用与违约" $C.Teal $C.White
Add-Box $s 639 285 265 150 '质量整改' "异常自动形成问题清单`r分派责任人和整改期限`r处理完成后复核关闭" $C.Red $C.White;Add-Footer $s 5

# 6 计量
$s=New-Slide;Add-Title $s '05' '组合风险计量' '以风险敞口为核心，将客户和债项风险转化为可比较的组合指标' $C.Teal
Add-Box $s 370 205 220 100 '风险敞口' '统一计量和组合分析基础' $C.Blue $C.PaleBlue
Add-Box $s 80 120 230 105 'PD 违约概率' '客户或债项发生违约的可能性' $C.Orange $C.PaleOrange
Add-Box $s 650 120 230 105 'LGD 违约损失率' '发生违约后无法收回的损失比例' $C.Red $C.PaleRed
Add-Box $s 80 335 230 105 'EAD 风险暴露' '发生违约时面临的风险暴露金额' $C.Blue $C.PaleBlue
Add-Box $s 650 335 230 105 'EL 预期损失' 'PD × LGD × EAD形成的预期损失' $C.Teal $C.PaleTeal
Add-Box $s 300 395 360 58 '分析输出' '组合损失、风险等级、评级迁徙、押品覆盖和跨月变化' $C.Navy $C.Gray;Add-Footer $s 6

# 7 限额
$s=New-Slide;Add-Title $s '06' '集中度、限额与前瞻管理' '从“本月是否超限”提升到“下月是否可能超限”' $C.Red
$stages=@(@('风险偏好','确定组合目标'),@('限额分解','分解到行业和集团'),@('当前监测','已用、可用与超限'),@('下月预测','叠加预计支用'),@('管理动作','压降、冻结或退出'));for($i=0;$i -lt 5;$i++){Add-Box $s (40+$i*182) 125 160 90 $stages[$i][0] $stages[$i][1] $(if($i -eq 4){$C.Red}elseif($i -eq 3){$C.Orange}else{$C.Blue}) $C.White}
Add-Box $s 55 285 265 130 '正常' "预计使用率低于预警线`r保持当前策略并持续监测" $C.Green $C.PaleGreen
Add-Box $s 347 285 265 130 '预警' "预计使用率进入临界区间`r收紧新增业务并制定压降计划" $C.Orange $C.PaleOrange
Add-Box $s 639 285 265 130 '超限' "当前或预计占用突破限额`r执行审批、豁免、整改或退出" $C.Red $C.PaleRed;Add-Footer $s 7

# 8 压力与集团
$s=New-Slide;Add-Title $s '07' '压力测试与集团客户风险' '发现正常环境和单一客户视角下不明显的尾部风险' $C.Orange
Add-Box $s 55 120 405 245 '压力测试' "基准、轻度下行和重度下行情景`r`r同步冲击PD、LGD、EAD和押品价值`r`r测算增量损失、压力后EL和限额突破" $C.Orange $C.PaleOrange
Add-Box $s 500 120 405 245 '集团客户风险' "聚合集团全部成员企业敞口`r`r识别股权控制、担保和关联关系`r`r展示风险偏好使用率、押品覆盖和风险传染" $C.Purple $C.PalePurple
Add-Box $s 80 405 800 62 '业务价值' '避免只看单一客户和当前时点，提前发现行业下行、押品折价和集团风险传染。' $C.Navy $C.Gray;Add-Footer $s 8

# 9 预警闭环
$s=New-Slide;Add-Title $s '08' '预警与处置闭环' '风险识别不是终点，系统持续跟踪整改、复核和实际效果' $C.Red
$steps=@('风险识别','预警案件','关联风险台账','整改任务','处置与复核','状态回写','效果评价','规则优化');for($i=0;$i -lt 8;$i++){$x=55+($i%4)*220;$y=125+[math]::Floor($i/4)*145;Add-Box $s $x $y 185 95 $steps[$i] $(switch($i){0{'逾期、评级、限额等信号'}1{'优先级与风险证据'}2{'客户、合同和债项'}3{'责任人、措施和期限'}4{'记录进度与复核意见'}5{'同步指标和风险事件'}6{'有效、部分有效或无效'}7{'调整阈值和规则'}}) $(if($i -in 0,3){$C.Orange}elseif($i -eq 1){$C.Red}elseif($i -in 4,6){$C.Teal}else{$C.Blue}) $C.White}
Add-Box $s 190 430 580 55 '闭环结果' '预警、任务、台账、指标和风险事件状态保持一致，形成责任可追踪的管理闭环。' $C.Teal $C.PaleTeal;Add-Footer $s 9

# 10 AI
$s=New-Slide;Add-Title $s '09' 'AI智能风险能力' 'AI贯穿查询、分析、预警、归因、处置和报告，但不替代正式风险决策' $C.Purple
$items=@(@('自然语言查询','用业务语言查询客户、集团和组合风险'),@('智能风险画像','自动汇总业务和风险证据'),@('异常识别预警','发现组合变化和外部异常信号'),@('变化智能归因','解释EAD、PD、LGD和EL变化'),@('处置与报告','生成处置建议和管理摘要'),@('外部数据辅助','结合工商、司法和舆情信息'));for($i=0;$i -lt 6;$i++){Add-Box $s (55+($i%3)*300) (115+[math]::Floor($i/3)*145) 270 115 $items[$i][0] $items[$i][1] $(if($i -in 2,3){$C.Orange}elseif($i -in 4,5){$C.Teal}else{$C.Blue}) $C.White}
Add-Box $s 80 420 800 58 'AI治理边界' '辅助分析、结论可解释、来源可追溯、敏感数据受控，正式处置和模型发布必须人工复核。' $C.Purple $C.PalePurple;Add-Footer $s 10

# 11 角色
$s=New-Slide;Add-Title $s '10' '谁在使用系统' '不同角色关注不同问题，系统支持个性化默认工作台' $C.Blue
$roles=@(@('管理层','组合趋势、限额突破、压力损失和投向调整'),@('风险经理','重点客户、变化原因、预警处置和整改效果'),@('业务机构','本机构风险、额度空间和整改任务'),@('模型人员','PD/LGD/EAD偏差、版本发布和模型校准'),@('数据人员','月末批次、勾稽、质量问题和业务口径'),@('审计合规','权限、安全、审批流程和操作留痕'));for($i=0;$i -lt 6;$i++){Add-Box $s (55+($i%3)*300) (115+[math]::Floor($i/3)*155) 270 125 $roles[$i][0] $roles[$i][1] $(if($i -eq 0){$C.Purple}elseif($i -eq 1){$C.Red}elseif($i -eq 2){$C.Teal}elseif($i -eq 3){$C.Orange}else{$C.Blue}) $C.White};Add-Footer $s 11

# 12 输出
$s=New-Slide;Add-Title $s '11' '业务输出与管理动作' '系统的最终价值不是展示指标，而是支持决策和行动' $C.Teal
$outputs=@(@('组合风险驾驶舱','当前风险、集中度和待办处置'),@('月末组合变动报告','本月变化、迁徙、归因和重点明细'),@('客户360风险画像','业务链、风险证据、时间轴和AI建议'),@('集团客户风险视图','成员关系、集团敞口和风险传染'),@('压力测试报告','压力损失、限额突破和重点行业'),@('模型与预警评价','模型偏差、校准建议和处置有效率'));for($i=0;$i -lt 6;$i++){Add-Box $s (55+($i%3)*300) (112+[math]::Floor($i/3)*135) 270 105 $outputs[$i][0] $outputs[$i][1] $C.Blue $C.White}
Add-Box $s 65 405 830 67 '可执行管理动作' '调整投向 · 压降敞口 · 收紧或释放额度 · 增补押品 · 调整定价期限 · 限制或退出客户' $C.Navy $C.Gray;Add-Footer $s 12

# 13 价值
$s=New-Slide;Add-Title $s '12' '系统带来的业务价值' '从信息汇总平台升级为组合风险管理与决策平台' $C.Green
$vals=@(@('统一口径','月末锁定数据和指标，减少部门结果差异'),@('穿透分析','从组合逐级下钻并找到风险变化原因'),@('风险前瞻','通过限额预测和压力测试提前行动'),@('管理闭环','预警、整改、复核、回写和评价在线完成'),@('模型优化','使用实际违约和回收表现持续校准'),@('决策效率','自动生成画像、摘要和管理报告'));for($i=0;$i -lt 6;$i++){Add-Box $s (55+($i%3)*300) (112+[math]::Floor($i/3)*145) 270 115 $vals[$i][0] $vals[$i][1] $(if($i%2){$C.Teal}else{$C.Blue}) $C.White}
Add-Box $s 85 420 790 58 '核心判断' '系统已经具备组合信用风险管理的主要业务闭环，可支撑内部管理、月末分析和业务试运行。' $C.Green $C.PaleGreen;Add-Footer $s 13

# 14 路线
$s=New-Slide;Add-Title $s '13' '后续建设建议' '从功能完善进一步迈向生产级银行组合风险管理平台' $C.Orange
Add-Box $s 55 115 270 300 '近期：真实数据与批量生产化' "对接授信、核心、评级、押品和客户主数据`r`r完善任务依赖、补数、重跑和批次验收`r`r固化月报、限额和压力测试报告" $C.Blue $C.PaleBlue
Add-Box $s 345 115 270 300 '中期：计量与验证深化' "完善PD区分度、校准度和稳定性检验`r`r补充LGD回收现金流和EAD/CCF验证`r`r完善担保圈和风险传染分析" $C.Teal $C.PaleTeal
Add-Box $s 635 115 270 300 '远期：资本与会计应用' "建设IFRS 9/ECL和宏观情景权重`r`r建设RWA、经济资本和RAROC`r`r完善监管报送、高可用和灾备" $C.Purple $C.PalePurple
Add-Box $s 95 440 770 55 '建设原则' '先保证月末数据可信和风险口径一致，再扩展高级模型、资本计量与监管应用。' $C.Orange $C.PaleOrange;Add-Footer $s 14

# 15 结束
$s=New-Slide;$s.Background.Fill.ForeColor.RGB=$C.Bg;Add-Text $s 65 85 820 55 '组合风险系统的核心价值' 28 $C.Navy $true 1|Out-Null
Add-Box $s 65 180 830 120 '看清组合风险，推动管理措施落地' '解释风险为什么变化，判断风险将向哪里发展，并将分析结果转化为额度、投向、押品和客户管理动作。' $C.Teal $C.White
$end=@('统一数据','统一计量','穿透分析','限额预警','处置闭环','AI辅助');for($i=0;$i -lt 6;$i++){Add-Box $s (70+$i*145) 355 125 65 $end[$i] '' $(if($i%2){$C.Teal}else{$C.Blue}) $(if($i%2){$C.PaleTeal}else{$C.PaleBlue})}
Add-Text $s 360 470 240 40 '谢谢' 24 $C.Teal $true 2|Out-Null

# 正式汇报补充页：按业务汇报逻辑插入
$s=New-SlideAt 2;Add-Title $s '汇报摘要' '当前建设成果与本次汇报结论' '系统已经形成主要业务闭环，但生产化仍需统一数据口径并接入真实上游' $C.Navy
Add-Box $s 45 110 205 125 '当前监测规模' "驾驶舱客户：200户`r预警客户：54户`r极高风险：30户" $C.Blue $C.PaleBlue
Add-Box $s 270 110 205 125 '月末正式快照' "批次：ME-20260731-V2`r客户：100户`r债项：400笔" $C.Teal $C.PaleTeal
Add-Box $s 495 110 205 125 '组合计量结果' "组合EAD：1.05亿元`r衰退EL：301.81万元`r质量得分：100分" $C.Orange $C.PaleOrange
Add-Box $s 720 110 195 125 '管理能力' "组合限额：24条`r集团客户：40组`r已发布模型：1个" $C.Purple $C.PalePurple
Add-Box $s 45 275 420 145 '已形成能力' "信贷数据链路、月末批次、PD/LGD/EAD/EL、集中度与限额、压力测试、集团风险、预警处置、模型治理、AI分析。" $C.Green $C.PaleGreen
Add-Box $s 495 275 420 145 '本次核心判断' "系统适合内部管理、业务演示和试运行。进入生产前，应优先统一驾驶舱与月末快照的数据范围，并完成真实上游接入、调度、权限和监管口径建设。" $C.Red $C.PaleRed
Add-Text $s 50 455 860 25 '注：本页数据来自2026-07-24本地系统实际查询结果，属于演示环境数据，不代表真实业务规模。' 9 $C.Muted $false 1|Out-Null;Add-Footer $s 2

$s=New-SlideAt 4;Add-Title $s '汇报边界' '本次汇报范围与业务口径' '区分已实现功能、演示数据和后续生产化能力' $C.Blue
Add-Box $s 50 110 270 300 '本次汇报覆盖' "对公信用风险组合管理`r`r客户、集团、合同和债项风险`r`r月末批量加工与跨月变化`r`r组合限额、压力测试和处置闭环" $C.Blue $C.PaleBlue
Add-Box $s 345 110 270 300 '当前系统定位' "组合信用风险分析平台`r`r支持内部管理和月末分析`r`r支持业务试运行和规则验证`r`r当前数据主要为本地模拟数据" $C.Teal $C.PaleTeal
Add-Box $s 640 110 270 300 '暂不作为正式口径' "IFRS 9/ECL减值结果`r`rRWA与监管资本`r`r经济资本与RAROC`r`r正式监管报送和会计入账" $C.Red $C.PaleRed
Add-Text $s 60 445 840 32 '汇报原则：不将模拟数据包装为生产结果，不将AI建议替代正式风险决策。' 12 $C.Navy $true 2|Out-Null;Add-Footer $s 4

$s=New-SlideAt 6;Add-Title $s '现状数据' '组合风险驾驶舱实际运行结果' '快速回答“当前风险有多大、集中在哪里、需要处理什么”' $C.Blue
$s.Shapes.AddPicture('D:\software\workspace\user-management-distributed\docs\images\operation-manual\02-dashboard.png',0,-1,45,105,610,370)|Out-Null
Add-Box $s 680 105 230 85 '风险规模' "监测客户200户`r预警客户54户" $C.Blue $C.PaleBlue
Add-Box $s 680 205 230 85 '重点风险' "极高风险30户`r待办处置55项" $C.Red $C.PaleRed
Add-Box $s 680 305 230 85 '集中度' "第一大行业：制造业`r集中度：33.55%" $C.Orange $C.PaleOrange
Add-Box $s 680 405 230 62 '预警敞口' '5,672.75万元' $C.Teal $C.PaleTeal;Add-Footer $s 6

$s=New-SlideAt 9;Add-Title $s '月末分析实证' '最新正式月末批次结果' '批次、组合指标、跨月变化和管理结论使用同一锁定快照' $C.Teal
$s.Shapes.AddPicture('D:\software\workspace\user-management-distributed\docs\images\operation-manual\24-month-end-overview.png',0,-1,42,105,620,365)|Out-Null
Add-Box $s 685 105 225 78 '批次信息' "ME-20260731-V2`r重算版本 / 已锁定" $C.Blue $C.PaleBlue
Add-Box $s 685 195 225 78 '业务范围' "客户100户`r债项400笔" $C.Teal $C.PaleTeal
Add-Box $s 685 285 225 78 '风险规模' "EAD 1.05亿元`r衰退EL 301.81万元" $C.Orange $C.PaleOrange
Add-Box $s 685 375 225 78 '数据质量' "质量得分100分`r6项勾稽通过" $C.Green $C.PaleGreen;Add-Footer $s 9

$s=New-SlideAt 10;Add-Title $s '月末控制' '批次生产化与数据质量控制点' '月末结果必须满足可验证、可复现、可追溯和可回退' $C.Red
Add-Box $s 45 110 205 125 '输入控制' "上游批次号`r数据日期与清单`r接收数量与校验值" $C.Blue $C.PaleBlue
Add-Box $s 270 110 205 125 '关联控制' "客户编号唯一`r合同与债项关联`r押品与合同关联" $C.Orange $C.PaleOrange
Add-Box $s 495 110 205 125 '参数控制' "PD、LGD范围`rEAD非负`r评级和违约规则" $C.Teal $C.PaleTeal
Add-Box $s 720 110 195 125 '发布控制' "质量评分`r勾稽通过`r发布锁定与版本" $C.Green $C.PaleGreen
$s.Shapes.AddPicture('D:\software\workspace\user-management-distributed\docs\images\operation-manual\27-month-end-quality.png',0,-1,70,270,545,190)|Out-Null
Add-Box $s 640 270 270 190 '生产要求' "失败批次不得发布`r异常形成质量整改任务`r重算必须生成新版本`r正式结果保留操作审计`r生产环境需增加作业依赖和断点续跑" $C.Red $C.PaleRed;Add-Footer $s 10

$s=New-SlideAt 12;Add-Title $s '计量口径' '核心风险指标定义与业务用途' '保证业务、风险、模型和管理层对指标含义理解一致' $C.Teal
$defs=@(@('PD','违约概率','未来一定期限内发生违约的可能性','评级迁徙、客户筛选、预警'),@('LGD','违约损失率','违约后无法回收的损失占风险暴露比例','押品评价、损失测算、定价'),@('EAD','违约风险暴露','违约发生时预计面临的风险暴露金额','组合规模、限额和资本占用'),@('EL','预期损失','PD × LGD × EAD','损失预算、组合比较、变化归因'));for($i=0;$i -lt 4;$i++){$y=112+$i*92;Add-Box $s 55 $y 115 70 $defs[$i][0] $defs[$i][1] $(if($i -eq 0){$C.Orange}elseif($i -eq 1){$C.Red}elseif($i -eq 2){$C.Blue}else{$C.Teal}) $C.White;Add-Box $s 185 $y 340 70 '业务定义' $defs[$i][2] $C.Blue $C.Gray;Add-Box $s 540 $y 365 70 '主要用途' $defs[$i][3] $C.Teal $C.Gray}
Add-Text $s 65 480 830 22 '口径要求：模型版本、数据日期、计算层级和情景类型必须随结果一并展示。' 10 $C.Red $true 2|Out-Null;Add-Footer $s 12

$s=New-SlideAt 14;Add-Title $s '限额实证' '当前限额监测与下月占用预测' '将已审批业务的预计支用纳入前瞻占用，提前识别潜在超限' $C.Orange
$s.Shapes.AddPicture('D:\software\workspace\user-management-distributed\docs\images\operation-manual\20-portfolio-forecast.png',0,-1,42,105,620,365)|Out-Null
Add-Box $s 685 105 225 78 '限额体系' "当前限额24条`r覆盖行业、产品、机构" $C.Blue $C.PaleBlue
Add-Box $s 685 195 225 78 '当前状态' "预警0条`r超限0条" $C.Green $C.PaleGreen
Add-Box $s 685 285 225 100 '前瞻方法' "当前占用 + 已审批申请预计支用`r计算预计使用率和剩余空间" $C.Orange $C.PaleOrange
Add-Box $s 685 400 225 68 '业务动作' '预警后收紧新增、压降或申请豁免' $C.Red $C.PaleRed;Add-Footer $s 14

$s=New-SlideAt 16;Add-Title $s '压力测试实证' '重度下行情景结果与集团风险' '识别正常环境下不明显、但在压力或关联关系下被放大的风险' $C.Red
$s.Shapes.AddPicture('D:\software\workspace\user-management-distributed\docs\images\operation-manual\21-portfolio-stress.png',0,-1,42,105,575,350)|Out-Null
Add-Box $s 640 105 270 88 '重度情景参数' "PD上升80%`rLGD增加12个百分点`rEAD扩张40%，押品折价30%" $C.Red $C.PaleRed
Add-Box $s 640 208 270 88 '测试结果' "5个行业参与`r增量损失510.81万元`r5个行业触发压力后超限" $C.Orange $C.PaleOrange
Add-Box $s 640 311 270 88 '集团风险' "已建立40个集团`r每组聚合成员、敞口、押品、逾期和违约" $C.Purple $C.PalePurple
Add-Box $s 640 414 270 58 '管理建议' '压力结果应进入年度风险偏好和限额调整流程' $C.Navy $C.Gray;Add-Footer $s 16

$s=New-SlideAt 19;Add-Title $s '治理机制' '模型、数据与AI治理' '确保风险结果不仅能算出来，还能解释、审批、监控和审计' $C.Purple
$s.Shapes.AddPicture('D:\software\workspace\user-management-distributed\docs\images\operation-manual\23-model-lifecycle.png',0,-1,45,110,420,270)|Out-Null
$s.Shapes.AddPicture('D:\software\workspace\user-management-distributed\docs\images\operation-manual\13-data-governance.png',0,-1,495,110,420,270)|Out-Null
Add-Box $s 45 400 270 70 '模型治理' '创建、模拟、审批、发布、回滚、监控和回溯校准' $C.Purple $C.PalePurple
Add-Box $s 345 400 270 70 '数据治理' '业务口径、质量检查、血缘、责任人和整改闭环' $C.Blue $C.PaleBlue
Add-Box $s 645 400 270 70 'AI治理' '辅助分析、可解释、可追溯、敏感数据受控、人工复核' $C.Teal $C.PaleTeal;Add-Footer $s 19

$s=New-SlideAt 23;Add-Title $s '待决策事项' '从试运行进入生产化需要业务与管理层确认的关键事项' '建议在项目立项或试点启动会上形成明确决议' $C.Red
$asks=@(@('1','统一风险数据范围','确认驾驶舱、月末快照和管理报告的客户与债项范围'),@('2','确定风险偏好与限额','明确行业、集团、产品和机构限额及预警线'),@('3','确认指标与违约口径','明确PD、LGD、EAD、EL、逾期和违约的正式定义'),@('4','确定处置责任机制','明确风险经理、业务机构、模型和数据人员职责'),@('5','确定生产化路线','选择试点机构、数据日期、运行周期和验收标准'));for($i=0;$i -lt 5;$i++){$y=105+$i*77;Add-Box $s 55 $y 85 58 $asks[$i][0] '' $C.Red $C.PaleRed;Add-Box $s 155 $y 210 58 $asks[$i][1] '' $C.Blue $C.PaleBlue;Add-Box $s 380 $y 525 58 '需确认内容' $asks[$i][2] $C.Teal $C.Gray};Add-Footer $s 23

# 统一刷新页码，避免插页后旧页码失真
for($si=1;$si -le $presentation.Slides.Count;$si++){
  $sl=$presentation.Slides.Item($si)
  for($j=$sl.Shapes.Count;$j -ge 1;$j--){
    $sh=$sl.Shapes.Item($j)
    if($sh.HasTextFrame -and $sh.TextFrame.HasText -and $sh.Top -gt 495 -and $sh.TextFrame.TextRange.Text.Trim() -match '^\d+$'){$sh.Delete()}
  }
  Add-Text $sl 900 510 30 15 "$si" 8 $C.Muted $false 3|Out-Null
}

try {
  if(Test-Path $OutputPath){Remove-Item -LiteralPath $OutputPath -Force}
  $presentation.SaveAs($OutputPath,24)
} finally {
  $presentation.Close(); $ppt.Quit()
}
Write-Output $OutputPath
