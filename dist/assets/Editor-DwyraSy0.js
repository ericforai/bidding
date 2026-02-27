import{_ as Me,m as Xe,o as p,c as y,b as o,d as n,w as t,f as m,ag as Ee,t as v,ak as Oe,$ as ze,Y as Le,k as u,r as c,u as Re,N as Be,i,an as Ue,P as $e,ar as Fe,K as Ye,aV as Ke,a0 as R,F as B,G as U,z as $,R as He,aW as Qe,aX as je,p as F,ad as G,au as W,O as Ge,E as k,H as We,I as q,aC as qe}from"./index-LwbFnuqN.js";const Ze={class:"document-editor-page"},Je={class:"editor-header"},en={class:"header-left"},nn={class:"title-section"},tn={class:"document-title"},ln={class:"header-actions"},on={class:"editor-container"},sn={class:"left-panel"},an={class:"card-header"},dn={class:"tree-node-content"},cn={class:"node-icon"},un={class:"node-label"},rn={class:"center-panel"},mn={class:"editor-header-bar"},pn={class:"section-title"},vn={class:"editor-tools"},_n={key:0,class:"editor-content"},fn={key:0,class:"knowledge-float-panel"},yn={class:"panel-header"},gn={class:"knowledge-list"},hn=["onClick"],kn={class:"knowledge-type"},bn={class:"relevance"},In={class:"knowledge-title"},Cn={class:"knowledge-summary"},wn={key:1,class:"empty-state"},Sn={class:"right-panel"},Tn={class:"card-header with-ai"},Vn={class:"assembly-content"},xn={class:"form-section"},Nn={class:"form-section"},Pn={key:0,class:"history-section"},An={class:"history-list"},Dn={class:"history-content"},Mn={class:"history-title"},Xn={class:"history-time"},En={class:"assembly-progress"},On={class:"progress-icon"},zn={class:"progress-steps"},Ln={class:"step-icon"},Rn={key:2},Bn={class:"step-text"},Un=14,$n={__name:"Editor",setup(Fn){const Z=Re();Be();const J=u({id:"P001",name:"智慧城市IOC项目"}),ee=u({templateId:"TPL_SMARTCITY",templateName:"智慧城市标书模板"}),w=u({sections:[{id:"cover",name:"封面",type:"section",content:`# 智慧城市IOC项目

投标文件

投标单位：西域科技股份有限公司
投标日期：2025年2月`},{id:"1",name:"技术方案",type:"folder",children:[{id:"1.1",name:"项目背景",type:"section",content:`## 1.1 项目背景

本项目旨在构建一套智慧城市IOC（智能运营中心）系统，实现对城市运行状态的全面感知、实时监测和智能分析。

### 建设目标
- 实现城市数据统一汇聚
- 构建可视化指挥调度平台
- 建立智能决策支持体系`},{id:"1.2",name:"需求分析",type:"section",content:`## 1.2 需求分析

### 功能需求

1. 数据采集与汇聚
2. 可视化展示
3. 智能预警
4. 应急指挥

### 非功能需求

- 系统响应时间 < 2秒
- 支持1000+并发用户
- 系统可用性 99.9%`},{id:"1.3",name:"技术架构",type:"section",content:`## 1.3 技术架构

### 总体架构

本系统采用微服务架构，分为以下几层：

1. **感知层**：IoT设备、传感器、摄像头
2. **网络层**：5G、NB-IoT、LoRa
3. **数据层**：数据湖、数据仓库
4. **平台层**：微服务、中间件
5. **应用层**：各业务应用

### 技术选型

- 前端：Vue3 + Element Plus
- 后端：Spring Cloud Alibaba
- 数据库：MySQL + MongoDB + Redis
- 大数据：Hadoop + Spark
- 可视化：ECharts + DataV`}]},{id:"2",name:"商务文件",type:"folder",children:[{id:"2.1",name:"投标函",type:"section",content:`## 投标函

致：[招标单位名称]

根据贵方[项目名称]招标文件（编号：[招标编号]），我方经过认真研究，决定参加投标。

### 投标报价

人民币：[金额]万元

### 投标承诺

1. 我方承诺投标文件真实有效
2. 我方承诺按要求完成项目
3. 我方承诺提供优质服务`},{id:"2.2",name:"报价清单",type:"section",content:`## 报价清单

| 序号 | 项目名称 | 数量 | 单价 | 金额 |
|------|---------|------|------|------|
| 1 | IOC平台软件 | 1套 | | |
| 2 | 大屏展示系统 | 1套 | | |
| 3 | 数据采集服务 | 1项 | | |

**合计：[金额]万元**`}]},{id:"3",name:"案例展示",type:"folder",children:[{id:"3.1",name:"智慧城市案例",type:"section",content:`## 成功案例

### 上海XX区智慧城市IOC项目

**项目规模**：500万元
**完成时间**：2024年

**项目亮点**：
- 接入20+委办局数据
- 实现300+指标实时监测
- 建成指挥调度大厅

### 深圳XX园区智慧管理平台

**项目规模**：300万元
**完成时间**：2023年

**项目亮点**：
- 园区资产数字化管理
- 能耗智能分析
- 安防联动预警`}]}]}),ne={children:"children",label:"name"},E=u(null),te=We(()=>w.value.sections),_=u(null),O=u([]),S=u(100),g=u({templateId:"TPL_SMARTCITY",sections:[]}),T=u(!1),P=u(!1),b=u(0),Y=u(["分析评分标准...","匹配技术方案模板...","检索相关案例...","组装资质文件...","生成服务承诺...","整合交付计划...","合规性检查..."]),z=u([]),I=u(!1),A=u("添加章节"),r=u({id:"",name:"",type:"section",parentId:""}),V=u(""),le=l=>l==="folder"?"📁":"📄",oe=l=>!0,se=(l,e,a)=>a==="inner"?e.data.type==="folder":!0,ae=l=>{l.type==="section"&&(_.value=l,D(l.id))},D=l=>{const e={"1.1":[{id:"k1",type:"case",title:"上海XX智慧城市IOC项目",summary:"项目背景与建设目标描述，可直接参考使用",relevance:95,content:`### 项目背景

上海XX区作为国家新型智慧城市试点区，亟需建设一套综合性的智能运营中心系统。

### 建设目标

1. 实现"一屏观全城"
2. 建立"一网管全城"
3. 打造"一脑慧全城"`},{id:"k2",type:"template",title:"智慧城市建设方案模板",summary:"标准的项目背景描述模板，含建设目标",relevance:88,content:`## 项目背景

随着智慧城市建设的深入推进，...

## 建设目标

本项目旨在构建...`}],"1.2":[{id:"k3",type:"case",title:"深圳XX项目需求分析",summary:"详细的功能需求和非功能需求描述",relevance:92,content:`## 功能需求

### 数据采集需求

支持多种数据源接入...

### 可视化需求

支持多种图表类型...`},{id:"k4",type:"template",title:"需求分析模板",summary:"标准需求分析文档结构模板",relevance:85,content:`## 需求分析

### 功能需求

#### 1. 用户管理

#### 2. 数据管理...`}],"1.3":[{id:"k5",type:"template",title:"微服务架构方案模板",summary:"完整的微服务架构设计说明模板",relevance:92,content:`## 技术架构

### 总体架构

采用微服务架构...

### 技术选型

- 前端框架
- 后端框架
- 数据库...`}],"3.1":[{id:"k6",type:"case",title:"北京XX区智慧城市项目",summary:"完整的项目案例描述，含项目亮点",relevance:90,content:`## 北京XX区智慧城市项目

**项目规模**：800万元
**完成时间**：2024年

### 项目亮点

1. 实现跨部门数据共享
2. 建成城市运行指标体系
3. 支持7×24小时运行监控`}]};O.value=e[l]||[]},ie=(l,e,a)=>{k.success("章节顺序已更新")},de=()=>{S.value<150&&(S.value+=10)},ce=()=>{S.value>70&&(S.value-=10)},ue=()=>{},re=l=>{if(_.value){_.value.content.length;const e=`

> 来自知识库：[${l.title}]

${l.content}

`;_.value.content+=e,k.success("已插入知识库内容")}},me=()=>{if(g.value.sections.length===0){k.warning("请至少选择一个章节");return}q.confirm(`确定要使用"${K(g.value.templateId)}"模板生成${g.value.sections.length}个章节的内容吗？`,"确认装配",{confirmButtonText:"开始装配",cancelButtonText:"取消",type:"warning"}).then(()=>{pe()}).catch(()=>{})},K=l=>({TPL_SMARTCITY:"智慧城市",TPL_SOFTWARE:"软件开发",TPL_EQUIPMENT:"设备采购"})[l]||"未知模板",pe=()=>{T.value=!0,P.value=!0,b.value=0;const l=Y.value;let e=0;const a=()=>{e<l.length?(b.value=e,setTimeout(()=>{e++,a()},800)):ve()};a()},ve=()=>{setTimeout(()=>{P.value=!1,T.value=!1;const l=_e();if(l.length>0){const e=l[0],a=M(e);a&&(_.value=a,D(e),qe(()=>{E.value&&E.value.setCurrentKey(e)}))}z.value.unshift({id:Date.now(),templateName:K(g.value.templateId),time:new Date().toLocaleString()}),k.success(`智能装配完成！已填充 ${l.length} 个章节`)},500)},_e=()=>{const l=[],a={TPL_SMARTCITY:{technical:{"1.1":`## 1.1 项目背景

本项目立足于智慧城市建设的实际需求，旨在打造一个集数据汇聚、智能分析、联动指挥于一体的智能运营中心(IOC)。

### 建设背景

随着城市化进程加快，城市管理面临诸多挑战：数据分散、管理滞后、决策缺乏支撑。亟需建设统一的城市智能运营平台。

### 建设目标

1. **数据融合**：整合各部门数据资源，实现"一湖汇全城"
2. **智能感知**：建立城市运行指标体系，实现"一屏观全城"
3. **联动指挥**：构建应急指挥体系，实现"一网管全城"`,"1.2":`## 1.2 需求分析

### 业务需求

**数据汇聚需求**
- 接入不少于20个委办局数据
- 支持结构化、非结构化数据
- 实时数据接入能力

**可视化需求**
- 支持大屏、PC、移动端多终端展示
- 提供2D/3D可视化能力
- 自定义仪表盘

### 技术需求

- 系统响应时间<2秒
- 支持1000+并发用户
- 系统可用性≥99.9%
- 数据安全等保三级`,"1.3":`## 1.3 技术架构

### 总体架构

\`\`\`
┌─────────────────────────────────────┐
│          应用层 (SaaS)              │
├─────────────────────────────────────┤
│          平台层 (PaaS)              │
│  微服务 | 中间件 | API网关           │
├─────────────────────────────────────┤
│          数据层 (DaaS)              │
│  数据湖 | 数据仓库 | 数据治理         │
├─────────────────────────────────────┤
│          感知层 (IoT)               │
│  传感器 | 摄像头 | 智能设备          │
└─────────────────────────────────────┘
\`\`\`

### 技术选型

| 类别 | 技术选型 | 说明 |
|------|---------|------|
| 前端 | Vue3 + Element Plus | 响应式UI框架 |
| 后端 | Spring Cloud Alibaba | 微服务框架 |
| 数据库 | MySQL + MongoDB | 关系型+文档型 |
| 缓存 | Redis Cluster | 分布式缓存 |
| 大数据 | Hadoop + Spark | 数据处理 |`},cases:{"3.1":`## 成功案例

### 案例1：上海XX区智慧城市IOC

**项目概况**
- 项目规模：500万元
- 完成时间：2024年6月
- 服务周期：3年

**建设内容**
1. 建设800平指挥大厅
2. 接入23个委办局数据
3. 实现300+城市指标监测

**项目成效**
- 城市事件发现效率提升60%
- 跨部门协同效率提升50%
- 领导决策支持满意度95%

### 案例2：深圳XX园区智慧管理

**项目概况**
- 项目规模：300万元
- 完成时间：2023年12月

**建设内容**
- 园区资产数字化管理
- 能耗智能分析与优化
- 安防联动预警系统

**项目成效**
- 园区能耗降低15%
- 安防事件响应时间缩短70%`},qualification:{"2.1":`## 公司资质

### 基础资质

- 营业执照（注册资本5000万元）
- ISO9001质量管理体系认证
- ISO27001信息安全管理体系认证
- CMMI5级认证

### 行业资质

- 电子与智能化工程专业承包一级
- 信息系统集成及服务资质一级
- 安全技术防范工程设计施工一级

### 软件著作权

- 智慧城市综合管理平台V1.0
- IOC智能运营中心系统V2.0
- 城市数据中台系统V1.0
- 可视化大屏展示系统V3.0`},service:{"2.2":`## 服务承诺

### 质量承诺

1. **系统质量**：符合国家及行业标准，通过第三方测评
2. **数据质量**：数据准确率≥99.5%
3. **服务响应**：7×24小时技术支持热线

### 培训承诺

- 现场培训：不少于10个工作日
- 培训人数：不少于20人
- 培训内容：系统操作、维护、管理

 ### 售后服务

**质保期**：3年免费质保

**响应时间**：
- 严重故障：2小时内响应，24小时内解决
- 一般故障：4小时内响应，48小时内解决

**定期巡检**：每季度一次现场巡检服务`},delivery:{"2.3":`## 交付计划

### 项目周期

总工期：6个月

### 里程碑计划

| 阶段 | 工作内容 | 周期 | 交付物 |
|------|---------|------|--------|
| 需求调研 | 需求分析、方案设计 | 1个月 | 需求规格说明书 |
| 系统开发 | 平台开发、功能实现 | 3个月 | 系统源码 |
| 测试验收 | 系统测试、用户验收 | 1个月 | 测试报告 |
| 上线运行 | 部署上线、培训移交 | 1个月 | 操作手册 |

### 交付标准

1. 完整的系统源代码
2. 系统设计文档、技术文档
3. 用户操作手册、维护手册
4. 测试报告、验收报告`}}}[g.value.templateId];return a&&Object.keys(a).forEach(f=>{if(g.value.sections.includes(f)){const d=a[f];Object.keys(d).forEach(C=>{const X=M(C);X&&(X.content=d[C],l.push(C))})}}),l},M=l=>{const e=a=>{for(const f of a){if(f.id===l)return f;if(f.children){const d=e(f.children);if(d)return d}}return null};return e(w.value.sections)},fe=()=>{A.value="添加章节",r.value={id:"",name:"",type:"section",parentId:""},V.value="",I.value=!0},ye=(l,e)=>{switch(l){case"add":A.value="添加子章节",r.value={id:"",name:"",type:"section",parentId:e.id},V.value="",I.value=!0;break;case"rename":A.value="重命名章节",r.value={id:e.id,name:e.name,type:e.type,parentId:""},V.value=e.id,I.value=!0;break;case"delete":ge(e);break}},ge=l=>{q.confirm("确定要删除该章节吗？","确认删除",{type:"warning"}).then(()=>{var e;he(l.id),k.success("章节已删除"),((e=_.value)==null?void 0:e.id)===l.id&&(_.value=null)}).catch(()=>{})},he=l=>{const e=a=>{const f=a.findIndex(d=>d.id===l);if(f>-1)return a.splice(f,1),!0;for(const d of a)if(d.children&&e(d.children))return!0;return!1};e(w.value.sections)},ke=()=>{if(!r.value.name){k.warning("请输入章节名称");return}if(V.value){const l=M(V.value);l&&(l.name=r.value.name,k.success("章节已重命名"))}else{const l={id:Date.now().toString(),name:r.value.name,type:r.value.type,content:r.value.type==="section"?"## "+r.value.name+`

在此处添加内容...`:""};if(r.value.parentId){const e=M(r.value.parentId);e&&(e.children||(e.children=[]),e.children.push(l))}else w.value.sections.push(l);k.success("章节已添加")}I.value=!1},be=()=>{Z.back()},Ie=()=>{k.info("预览功能开发中...")},Ce=()=>{k.info("导出功能开发中...")},we=()=>{k.success("保存成功")};return Xe(()=>{if(w.value.sections.length>0){const l=w.value.sections[0];l.type==="section"?(_.value=l,D(l.id)):l.children&&l.children.length>0&&(_.value=l.children[0],D(l.children[0].id))}}),(l,e)=>{const a=c("el-button"),f=c("el-tag"),d=c("el-icon"),C=c("el-dropdown-item"),X=c("el-dropdown-menu"),Se=c("el-dropdown"),Te=c("el-tree"),L=c("el-card"),Ve=c("el-button-group"),x=c("el-radio"),H=c("el-radio-group"),N=c("el-checkbox"),xe=c("el-checkbox-group"),Ne=c("el-divider"),Q=c("el-dialog"),Pe=c("el-input"),j=c("el-form-item"),Ae=c("el-form");return p(),y("div",Ze,[o("div",Je,[o("div",en,[n(a,{icon:m(Ee),onClick:be},{default:t(()=>[...e[8]||(e[8]=[i("返回",-1)])]),_:1},8,["icon"]),o("div",nn,[o("h2",tn,v(J.value.name)+" - 标书编辑器",1),n(f,{size:"small",type:"info"},{default:t(()=>[i(v(ee.value.templateName),1)]),_:1})])]),o("div",ln,[n(a,{icon:m(Oe),onClick:Ie},{default:t(()=>[...e[9]||(e[9]=[i("预览",-1)])]),_:1},8,["icon"]),n(a,{icon:m(ze),onClick:Ce},{default:t(()=>[...e[10]||(e[10]=[i("导出",-1)])]),_:1},8,["icon"]),n(a,{type:"primary",icon:m(Le),onClick:we},{default:t(()=>[...e[11]||(e[11]=[i("保存",-1)])]),_:1},8,["icon"])])]),o("div",on,[o("div",sn,[n(L,{shadow:"never",class:"section-tree-card"},{header:t(()=>[o("div",an,[e[13]||(e[13]=o("span",null,"章节目录",-1)),n(a,{icon:m($e),size:"small",text:"",onClick:fe},{default:t(()=>[...e[12]||(e[12]=[i("添加章节",-1)])]),_:1},8,["icon"])])]),default:t(()=>[n(Te,{ref_key:"sectionTreeRef",ref:E,data:te.value,props:ne,"highlight-current":!0,"allow-drag":oe,"allow-drop":se,"node-key":"id",draggable:"",onNodeClick:ae,onNodeDrop:ie},{default:t(({node:s,data:h})=>[o("div",dn,[o("span",cn,v(le(h.type)),1),o("span",un,v(s.label),1),n(Se,{trigger:"click",onCommand:De=>ye(De,h)},{dropdown:t(()=>[n(X,null,{default:t(()=>[n(C,{command:"add"},{default:t(()=>[...e[14]||(e[14]=[i("添加子章节",-1)])]),_:1}),n(C,{command:"rename"},{default:t(()=>[...e[15]||(e[15]=[i("重命名",-1)])]),_:1}),n(C,{command:"delete",divided:""},{default:t(()=>[...e[16]||(e[16]=[i("删除",-1)])]),_:1})]),_:1})]),default:t(()=>[n(d,{size:14,class:"node-more-icon"},{default:t(()=>[n(m(Ue))]),_:1})]),_:1},8,["onCommand"])])]),_:1},8,["data"])]),_:1})]),o("div",rn,[n(L,{shadow:"never",class:"editor-card"},{header:t(()=>{var s;return[o("div",mn,[o("span",pn,v(((s=_.value)==null?void 0:s.name)||"请选择章节"),1),o("div",vn,[n(Ve,{size:"small"},{default:t(()=>[n(a,{icon:m(Qe),onClick:ce},null,8,["icon"]),n(a,null,{default:t(()=>[i(v(S.value)+"%",1)]),_:1}),n(a,{icon:m(je),onClick:de},null,8,["icon"])]),_:1})])])]}),default:t(()=>[_.value?(p(),y("div",_n,[Fe(o("textarea",{"onUpdate:modelValue":e[0]||(e[0]=s=>_.value.content=s),class:"content-textarea",style:Ye({fontSize:Un*S.value/100+"px"}),placeholder:"在此处编辑内容...",onInput:ue},null,36),[[Ke,_.value.content]]),O.value.length>0?(p(),y("div",fn,[o("div",yn,[n(d,null,{default:t(()=>[n(m(R))]),_:1}),e[17]||(e[17]=o("span",null,"知识库推荐",-1))]),o("div",gn,[(p(!0),y(B,null,U(O.value,s=>(p(),y("div",{key:s.id,class:"knowledge-item",onClick:h=>re(s)},[o("div",kn,[n(f,{type:s.type==="case"?"success":"primary",size:"small"},{default:t(()=>[i(v(s.type==="case"?"案例":"模板"),1)]),_:2},1032,["type"]),o("span",bn,"匹配度: "+v(s.relevance)+"%",1)]),o("div",In,v(s.title),1),o("div",Cn,v(s.summary),1),e[18]||(e[18]=o("div",{class:"insert-hint"},"点击插入",-1))],8,hn))),128))])])):$("",!0)])):(p(),y("div",wn,[n(d,{size:48,color:"#c0c4cc"},{default:t(()=>[n(m(He))]),_:1}),e[19]||(e[19]=o("p",null,"请从左侧选择章节进行编辑",-1))]))]),_:1})]),o("div",Sn,[n(L,{shadow:"never",class:"assembly-card"},{header:t(()=>[o("div",Tn,[n(d,{class:"ai-icon"},{default:t(()=>[n(m(R))]),_:1}),e[20]||(e[20]=o("span",null,"智能装配",-1))])]),default:t(()=>[o("div",Vn,[o("div",xn,[e[24]||(e[24]=o("h4",{class:"section-label"},"选择模板",-1)),n(H,{modelValue:g.value.templateId,"onUpdate:modelValue":e[1]||(e[1]=s=>g.value.templateId=s),class:"template-options"},{default:t(()=>[n(x,{label:"TPL_SMARTCITY",border:""},{default:t(()=>[...e[21]||(e[21]=[i("智慧城市",-1)])]),_:1}),n(x,{label:"TPL_SOFTWARE",border:""},{default:t(()=>[...e[22]||(e[22]=[i("软件开发",-1)])]),_:1}),n(x,{label:"TPL_EQUIPMENT",border:""},{default:t(()=>[...e[23]||(e[23]=[i("设备采购",-1)])]),_:1})]),_:1},8,["modelValue"])]),o("div",Nn,[e[30]||(e[30]=o("h4",{class:"section-label"},"包含章节",-1)),n(xe,{modelValue:g.value.sections,"onUpdate:modelValue":e[2]||(e[2]=s=>g.value.sections=s),class:"section-checkboxes"},{default:t(()=>[n(N,{label:"technical"},{default:t(()=>[...e[25]||(e[25]=[i("技术方案",-1)])]),_:1}),n(N,{label:"cases"},{default:t(()=>[...e[26]||(e[26]=[i("案例展示",-1)])]),_:1}),n(N,{label:"qualification"},{default:t(()=>[...e[27]||(e[27]=[i("资质文件",-1)])]),_:1}),n(N,{label:"service"},{default:t(()=>[...e[28]||(e[28]=[i("服务承诺",-1)])]),_:1}),n(N,{label:"delivery"},{default:t(()=>[...e[29]||(e[29]=[i("交付计划",-1)])]),_:1})]),_:1},8,["modelValue"])]),n(a,{type:"primary",size:"large",loading:T.value,disabled:g.value.sections.length===0,class:"assembly-btn",onClick:me},{default:t(()=>[T.value?$("",!0):(p(),F(d,{key:0},{default:t(()=>[n(m(R))]),_:1})),i(" "+v(T.value?"装配中...":"开始装配"),1)]),_:1},8,["loading","disabled"]),z.value.length>0?(p(),y("div",Pn,[n(Ne,null,{default:t(()=>[...e[31]||(e[31]=[i("装配历史",-1)])]),_:1}),o("div",An,[(p(!0),y(B,null,U(z.value,s=>(p(),y("div",{key:s.id,class:"history-item"},[n(d,{color:"#67c23a"},{default:t(()=>[n(m(G))]),_:1}),o("div",Dn,[o("div",Mn,v(s.templateName),1),o("div",Xn,v(s.time),1)])]))),128))])])):$("",!0)])]),_:1})])]),n(Q,{modelValue:P.value,"onUpdate:modelValue":e[3]||(e[3]=s=>P.value=s),title:"智能装配中",width:"500px","close-on-click-modal":!1,"close-on-press-escape":!1,"show-close":!1},{footer:t(()=>[...e[32]||(e[32]=[o("span",null,null,-1)])]),default:t(()=>[o("div",En,[o("div",On,[n(d,{size:48,class:"rotating"},{default:t(()=>[n(m(W))]),_:1})]),o("div",zn,[(p(!0),y(B,null,U(Y.value,(s,h)=>(p(),y("div",{key:h,class:Ge(["step-item",{"step-active":h===b.value,"step-done":h<b.value,"step-pending":h>b.value}])},[o("div",Ln,[h<b.value?(p(),F(d,{key:0},{default:t(()=>[n(m(G))]),_:1})):h===b.value?(p(),F(d,{key:1},{default:t(()=>[n(m(W))]),_:1})):(p(),y("span",Rn,v(h+1),1))]),o("div",Bn,v(s),1)],2))),128))])])]),_:1},8,["modelValue"]),n(Q,{modelValue:I.value,"onUpdate:modelValue":e[7]||(e[7]=s=>I.value=s),title:A.value,width:"500px"},{footer:t(()=>[n(a,{onClick:e[6]||(e[6]=s=>I.value=!1)},{default:t(()=>[...e[35]||(e[35]=[i("取消",-1)])]),_:1}),n(a,{type:"primary",onClick:ke},{default:t(()=>[...e[36]||(e[36]=[i("确定",-1)])]),_:1})]),default:t(()=>[n(Ae,{model:r.value,"label-width":"80px"},{default:t(()=>[n(j,{label:"章节名称"},{default:t(()=>[n(Pe,{modelValue:r.value.name,"onUpdate:modelValue":e[4]||(e[4]=s=>r.value.name=s),placeholder:"请输入章节名称"},null,8,["modelValue"])]),_:1}),n(j,{label:"章节类型"},{default:t(()=>[n(H,{modelValue:r.value.type,"onUpdate:modelValue":e[5]||(e[5]=s=>r.value.type=s)},{default:t(()=>[n(x,{label:"section"},{default:t(()=>[...e[33]||(e[33]=[i("章节",-1)])]),_:1}),n(x,{label:"folder"},{default:t(()=>[...e[34]||(e[34]=[i("文件夹",-1)])]),_:1})]),_:1},8,["modelValue"])]),_:1})]),_:1},8,["model"])]),_:1},8,["modelValue","title"])])}}},Kn=Me($n,[["__scopeId","data-v-36a958b1"]]);export{Kn as default};
