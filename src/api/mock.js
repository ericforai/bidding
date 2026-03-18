// Mock数据 - 西域投标管理平台POC

export const mockData = {
  // 用户信息
  users: [
    { id: 'U001', name: '小王', role: 'manager', dept: '华南销售部', avatar: '' },
    { id: 'U002', name: '张经理', role: 'manager', dept: '投标管理部', avatar: '' },
    { id: 'U003', name: '李总', role: 'admin', dept: '管理层', avatar: '' },
    { id: 'U004', name: '李工', role: 'staff', dept: '技术部', avatar: '' }
  ],

  // 待办任务
  todos: [
    { id: 'T001', type: 'urgent', title: '某央企智慧办公项目 - 保证金缴纳截止',
      deadline: '2025-02-26 18:00', project: '某央企项目', status: 'pending', priority: 'high' },
    { id: 'T002', type: 'review', title: '华南电力集采 - 技术方案评审',
      deadline: '2025-02-27', project: '华南电力', status: 'pending', priority: 'medium' },
    { id: 'T003', type: 'upload', title: '深圳地铁项目 - 中标通知书上传',
      deadline: '2025-02-28', project: '深圳地铁', status: 'pending', priority: 'low' },
    { id: 'T004', type: 'approve', title: '西部云数据中心 - 立项审批',
      deadline: '2025-02-26', project: '西部云', status: 'pending', priority: 'high' }
  ],

  // 标讯列表
  tenders: [
    {
      id: 'B001',
      title: '某央企智慧办公平台采购项目',
      budget: 500,
      region: '北京',
      date: '2025-02-26',
      aiScore: 92,
      probability: 'high',
      aiReason: '该客户历史采购3次，我司优势在信创能力',
      status: 'new',
      industry: '政府',
      deadline: '2025-03-15',
      tags: ['智慧办公', '信创', '高优先级']
    },
    {
      id: 'B002',
      title: '华南电力集团集采项目',
      budget: 1200,
      region: '广州',
      date: '2025-02-26',
      aiScore: 78,
      probability: 'medium',
      aiReason: '传统优势领域，但竞争激烈',
      status: 'new',
      industry: '能源',
      deadline: '2025-03-20',
      tags: ['电力', '集采']
    },
    {
      id: 'B003',
      title: '深圳地铁自动化系统建设',
      budget: 800,
      region: '深圳',
      date: '2025-02-25',
      aiScore: 85,
      probability: 'high',
      aiReason: '前期已建立良好关系',
      status: 'following',
      industry: '交通',
      deadline: '2025-03-10',
      tags: ['地铁', '自动化']
    },
    {
      id: 'B004',
      title: '西部云数据中心建设项目',
      budget: 2000,
      region: '成都',
      date: '2025-02-25',
      aiScore: 70,
      probability: 'medium',
      aiReason: '预算充足但技术要求高',
      status: 'bidding',
      industry: '数据中心',
      deadline: '2025-03-25',
      tags: ['数据中心', '云计算']
    }
  ],

  // 投标项目
  projects: [
    {
      id: 'P001',
      name: '某央企智慧办公平台采购',
      customer: '某央企集团',
      budget: 500,
      deadline: '2025-03-15',
      status: 'bidding',
      progress: 90,
      manager: '小王',
      createTime: '2025-02-20',
      description: '智慧办公平台建设，包括OA系统、会议系统、文档管理等',
      industry: '政府',
      region: '北京',
      sourceModule: 'customer-opportunity-center',
      sourceCustomer: '某央企集团',
      sourceOpportunityId: 'CO_OPP_001',
      sourceReasoningSummary: '基于2024-2025年3月/9月劳保类集中采购、预算稳定在500-550万',
      tasks: [
        { id: 'TK001', name: '技术方案编制', owner: '李工', status: 'doing', deadline: '2025-02-27', priority: 'high' },
        { id: 'TK002', name: '商务应答文件', owner: '王经理', status: 'doing', deadline: '2025-02-28', priority: 'high' },
        { id: 'TK003', name: '资质文件准备', owner: '小王', status: 'done', deadline: '2025-02-25', priority: 'medium' },
        { id: 'TK004', name: '报价单制作', owner: '财务部', status: 'todo', deadline: '2025-03-01', priority: 'high' },
        { id: 'TK005', name: '标书装订封装', owner: '行政部', status: 'todo', deadline: '2025-03-10', priority: 'medium' }
      ],
      documents: [
        { id: 'D001', name: '技术方案v2.0.docx', uploader: '李工', time: '2025-02-25 14:30', size: '2.3MB' },
        { id: 'D002', name: '商务应答v1.5.docx', uploader: '王经理', time: '2025-02-25 16:00', size: '1.8MB' }
      ],
      aiCheck: {
        compliance: { score: 95, issues: [], passed: true },
        quality: { score: 88, issues: ['发现1处可能错别字: "信产" → "信创"?'], passed: true }
      }
    },
    {
      id: 'P002',
      name: '华南电力集团集采项目',
      customer: '华南电力集团',
      budget: 1200,
      deadline: '2025-03-20',
      status: 'reviewing',
      progress: 40,
      manager: '张经理',
      createTime: '2025-02-18',
      description: '电力集团信息化系统集中采购',
      industry: '能源',
      region: '广州',
      sourceModule: 'manual',
      sourceCustomer: '华南电力集团',
      sourceOpportunityId: '',
      sourceReasoningSummary: '从投标线索人工上报，未关联智能预测',
      tasks: [
        { id: 'TK006', name: '资质审核', owner: '小王', status: 'doing', deadline: '2025-02-27', priority: 'high' },
        { id: 'TK007', name: '技术方案', owner: '李工', status: 'todo', deadline: '2025-03-05', priority: 'high' }
      ],
      documents: [],
      aiCheck: null
    },
    {
      id: 'P003',
      name: '深圳地铁自动化系统',
      customer: '深圳地铁集团',
      budget: 800,
      deadline: '2025-03-10',
      status: 'won',
      progress: 100,
      manager: '小王',
      createTime: '2025-02-10',
      description: '地铁自动化控制系统建设',
      industry: '交通',
      region: '深圳',
      sourceModule: 'crowd-sourced',
      sourceCustomer: '深圳地铁集团',
      sourceOpportunityId: '',
      sourceReasoningSummary: '历史项目沉淀，在系统中视为标配来源',
      tasks: [],
      documents: [{ id: 'D003', name: '中标通知书.pdf', uploader: '小王', time: '2025-02-20', size: '0.5MB' }],
      aiCheck: null
    }
  ],

  projectScoreDrafts: {},

  // 资质库
  qualifications: [
    { id: 'Q001', name: 'ISO9001质量管理体系认证', type: '企业资质', expiry: '2026-12-31', status: 'valid', warning: false },
    { id: 'Q002', name: 'ISO27001信息安全管理体系', type: '企业资质', expiry: '2026-08-30', status: 'valid', warning: false },
    { id: 'Q003', name: '信息系统建设和服务能力CS4级', type: '企业资质', expiry: '2025-08-30', status: 'valid', warning: true },
    { id: 'Q004', name: 'CMMI5级认证', type: '软件能力', expiry: '2027-03-15', status: 'valid', warning: false },
    { id: 'Q005', name: '高新技术企业证书', type: '企业资质', expiry: '2026-11-20', status: 'valid', warning: false },
    { id: 'Q006', name: '涉密信息系统集成资质', type: '安全资质', expiry: '2025-06-30', status: 'expiring', warning: true }
  ],

  // 案例库
  cases: [
    {
      id: 'C001',
      title: '某省政府OA办公系统',
      customer: '某省政府',
      industry: '政府',
      amount: 300,
      year: 2024,
      status: 'won',
      tags: ['OA', '政府', '信创'],
      summary: '为省政府打造一体化办公平台，包括公文管理、会议管理、日程管理等核心功能',
      highlights: ['信创适配', '高并发处理', '移动端支持']
    },
    {
      id: 'C002',
      title: '华东电网信息化项目',
      customer: '华东电网',
      industry: '能源',
      amount: 800,
      year: 2024,
      status: 'won',
      tags: ['电力', 'ERP', '数据中台'],
      summary: '电网企业ERP系统升级及数据中台建设',
      highlights: ['微服务架构', '数据治理', '智能报表']
    },
    {
      id: 'C003',
      title: '西部智慧园区项目',
      customer: '西部某园区',
      industry: '园区',
      amount: 500,
      year: 2023,
      status: 'won',
      tags: ['智慧园区', '物联网', '大数据'],
      summary: '智慧园区综合管理平台',
      highlights: ['IoT集成', '3D可视化', '能耗分析']
    }
  ],

  // 模板库
  templates: [
    {
      id: 'TP001',
      name: '智慧办公平台技术方案模板',
      category: '技术方案',
      tags: ['智慧办公', '信创', 'OA'],
      downloads: 156,
      updateTime: '2025-01-15',
      description: '适用于政府、央企智慧办公项目技术方案编制'
    },
    {
      id: 'TP002',
      name: '央企项目商务应答模板',
      category: '商务文件',
      tags: ['央企', '标准', '应答'],
      downloads: 89,
      updateTime: '2025-02-01',
      description: '央企招投标项目商务应答文件标准模板'
    },
    {
      id: 'TP003',
      name: '电力行业解决方案模板',
      category: '行业方案',
      tags: ['电力', '能源', '解决方案'],
      downloads: 67,
      updateTime: '2024-12-20',
      description: '电力行业信息化项目解决方案模板'
    },
    {
      id: 'TP004',
      name: '项目组织实施方案模板',
      category: '实施方案',
      tags: ['实施', '项目管理', '标准'],
      downloads: 234,
      updateTime: '2025-01-30',
      description: '通用项目组织实施方案模板'
    }
  ],

  // 费用台账
  fees: [
    { id: 'F001', project: '某央企项目', type: '保证金', amount: 10, status: 'paid', date: '2025-02-20', returnDate: '2025-04-20', approvalStatus: 'approved' },
    { id: 'F002', project: '华南电力', type: '标书费', amount: 0.05, status: 'pending', date: '2025-02-26', returnDate: null, approvalStatus: 'pending' },
    { id: 'F003', project: '深圳地铁', type: '保证金', amount: 16, status: 'returned', date: '2025-02-10', returnDate: '2025-02-22', approvalStatus: 'approved' },
    { id: 'F004', project: '西部云', type: '差旅费', amount: 0.8, status: 'pending', date: '2025-02-25', returnDate: null, approvalStatus: 'approved' },
    { id: 'F005', project: '某央企项目', type: '标书费', amount: 0.05, status: 'paid', date: '2025-02-18', returnDate: null, approvalStatus: 'approved' }
  ],

  // 平台账户
  accounts: [
    { id: 'A001', platform: '政府采购网', username: 'xiyu001', password: '***', status: 'available', lastUsed: '2025-02-20' },
    { id: 'A002', platform: '央企招标采购平台', username: 'xy_bid002', password: '***', status: 'in_use', lastUsed: '2025-02-25', borrower: '小王' },
    { id: 'A003', platform: '广东省招标监管网', username: 'xy_gd003', password: '***', status: 'available', lastUsed: '2025-02-15' }
  ],

  // 数据看板统计
  dashboard: {
    totalBids: 156,
    totalBidsChange: '+8%',
    inProgress: 23,
    wonThisYear: 45,
    winRate: 35,
    winRateChange: '+5%',
    totalAmount: 28000,
    totalAmountChange: '+12%',
    totalCost: 320,
    totalCostChange: '-3%',
    trendData: [
      { month: '7月', bids: 12, wins: 4, rate: 33, amount: 1800 },
      { month: '8月', bids: 15, wins: 5, rate: 33, amount: 2200 },
      { month: '9月', bids: 18, wins: 6, rate: 33, amount: 3100 },
      { month: '10月', bids: 14, wins: 5, rate: 36, amount: 2500 },
      { month: '11月', bids: 16, wins: 6, rate: 38, amount: 2800 },
      { month: '12月', bids: 20, wins: 8, rate: 40, amount: 4200 },
      { month: '1月', bids: 22, wins: 8, rate: 36, amount: 3800 }
    ],
    competitors: [
      { name: '甲公司', share: 35, amount: 9800, bids: 45 },
      { name: '乙公司', share: 25, amount: 7000, bids: 38 },
      { name: '我司', share: 20, amount: 5600, bids: 32 },
      { name: '丙公司', share: 12, amount: 3360, bids: 25 },
      { name: '其他', share: 8, amount: 2240, bids: 16 }
    ],
    productLines: [
      { name: '智慧办公', rate: 42, cost: 120, revenue: 5600, bids: 28 },
      { name: '工业软件', rate: 28, cost: 80, revenue: 2100, bids: 18 },
      { name: '云服务', rate: 51, cost: 60, revenue: 8900, bids: 22 },
      { name: '数据中心', rate: 35, cost: 60, revenue: 3500, bids: 20 }
    ],
    regionData: [
      { name: '华南', amount: 8500, bids: 42, rate: 38 },
      { name: '华东', amount: 7200, bids: 35, rate: 34 },
      { name: '华北', amount: 6800, bids: 32, rate: 36 },
      { name: '西南', amount: 3500, bids: 25, rate: 32 },
      { name: '西北', amount: 2000, bids: 22, rate: 30 }
    ]
  },

  // 投标日历 - 使用当前年月
  calendar: [
    // 3月的事件
    { id: 'CAL001', date: '2026-03-05', type: 'deadline', title: '某央企项目 - 保证金缴纳', shortTitle: '保证金缴纳', project: '某央企智慧办公平台采购', urgent: true },
    { id: 'CAL002', date: '2026-03-08', type: 'review', title: '华南电力 - 方案评审', shortTitle: '方案评审', project: '华南电力集团集采项目', urgent: false },
    { id: 'CAL003', date: '2026-03-10', type: 'deadline', title: '深圳地铁 - 中标通知书上传', shortTitle: '中标书上传', project: '深圳地铁自动化系统', urgent: false },
    { id: 'CAL004', date: '2026-03-12', type: 'bid', title: '西部云 - 投标截止', shortTitle: '投标截止', project: '西部云数据中心建设', urgent: false },
    { id: 'CAL005', date: '2026-03-15', type: 'opening', title: '某央企项目 - 开标', shortTitle: '开标', project: '某央企智慧办公平台采购', urgent: false },
    { id: 'CAL006', date: '2026-03-18', type: 'deadline', title: '深圳地铁 - 保证金退还', shortTitle: '保证金退还', project: '深圳地铁自动化系统', urgent: false },
    { id: 'CAL007', date: '2026-03-20', type: 'bid', title: '华东电网 - 投标截止', shortTitle: '投标截止', project: '华东电网信息化项目', urgent: false },
    { id: 'CAL008', date: '2026-03-22', type: 'review', title: '某政府项目 - 预审', shortTitle: '资格预审', project: 'XX区政府数字平台', urgent: false },
    { id: 'CAL009', date: '2026-03-25', type: 'opening', title: 'XX区数字政府 - 开标', shortTitle: '开标', project: 'XX区数字政府平台', urgent: false },
    { id: 'CAL010', date: '2026-03-28', type: 'deadline', title: '资质文件更新', shortTitle: '资质更新', project: 'ISO9001认证续期', urgent: true },
    { id: 'CAL011', date: '2026-03-30', type: 'bid', title: '某央企项目 - 投标截止', shortTitle: '投标截止', project: '某央企集团OA系统', urgent: false },
    // 4月的事件
    { id: 'CAL012', date: '2026-04-02', type: 'opening', title: '华南电力 - 开标', shortTitle: '开标', project: '华南电力集团集采项目', urgent: false },
    { id: 'CAL013', date: '2026-04-08', type: 'deadline', title: '西安智慧园区 - 保证金缴纳', shortTitle: '保证金缴纳', project: '西安智慧园区项目', urgent: false },
    { id: 'CAL014', date: '2026-04-15', type: 'bid', title: '某制造企业 - 投标截止', shortTitle: '投标截止', project: '制造执行系统(MES)', urgent: false }
  ],

  // AI分析数据
  aiAnalysis: {
    T001: {
      winScore: 75,
      suggestion: '有把握投，建议补齐智慧城市类案例',
      dimensionScores: [
        { name: '客户关系', score: 80 },
        { name: '需求匹配', score: 70 },
        { name: '资质满足', score: 60 },
        { name: '交付能力', score: 85 },
        { name: '竞争态势', score: 50 }
      ],
      risks: [
        { level: 'high', desc: '缺智慧城市类案例(权重15分)', action: '推荐使用上海XX项目案例' },
        { level: 'medium', desc: 'CMMI 5级资质即将过期(30天)', action: '需更新资质文件' },
        { level: 'medium', desc: '客户关系强度: 中', action: '建议安排高层拜访' }
      ],
      autoTasks: [
        { id: 1, title: '准备资质文件', owner: '张三', dueDate: '2024-03-15', priority: 'high' },
        { id: 2, title: '编写技术方案', owner: '李四', dueDate: '2024-03-20', priority: 'high' },
        { id: 3, title: '商务报价测算', owner: '王五', dueDate: '2024-03-25', priority: 'medium' },
        { id: 4, title: '案例素材整理', owner: '赵六', dueDate: '2024-03-18', priority: 'high' },
        { id: 5, title: '投标保证金办理', owner: '钱七', dueDate: '2024-03-28', priority: 'medium' }
      ]
    },
    B001: {
      winScore: 85,
      suggestion: '高度匹配，建议积极参与',
      dimensionScores: [
        { name: '客户关系', score: 90 },
        { name: '需求匹配', score: 85 },
        { name: '资质满足', score: 80 },
        { name: '交付能力', score: 88 },
        { name: '竞争态势', score: 75 }
      ],
      risks: [
        { level: 'medium', desc: '竞争对手可能以低价策略', action: '建议优化成本结构' }
      ],
      autoTasks: [
        { id: 1, title: '联系客户确认需求', owner: '小王', dueDate: '2024-02-28', priority: 'high' },
        { id: 2, title: '技术方案编写', owner: '李工', dueDate: '2024-03-05', priority: 'high' }
      ]
    },
    B002: {
      winScore: 60,
      suggestion: '竞争激烈，需谨慎评估',
      dimensionScores: [
        { name: '客户关系', score: 50 },
        { name: '需求匹配', score: 70 },
        { name: '资质满足', score: 65 },
        { name: '交付能力', score: 60 },
        { name: '竞争态势', score: 55 }
      ],
      risks: [
        { level: 'high', desc: '主要竞争对手已建立深度合作', action: '需评估差异化竞争策略' },
        { level: 'medium', desc: '预算充足但利润空间可能受限', action: '建议仔细核算成本' }
      ],
      autoTasks: [
        { id: 1, title: '竞争对手调研', owner: '张经理', dueDate: '2024-02-28', priority: 'high' }
      ]
    },
    B003: {
      winScore: 80,
      suggestion: '前期关系良好，建议积极跟进',
      dimensionScores: [
        { name: '客户关系', score: 90 },
        { name: '需求匹配', score: 75 },
        { name: '资质满足', score: 70 },
        { name: '交付能力', score: 85 },
        { name: '竞争态势', score: 70 }
      ],
      risks: [
        { level: 'medium', desc: '技术方案需要进一步细化', action: '安排技术团队进行需求对接' }
      ],
      autoTasks: [
        { id: 1, title: '技术方案细化', owner: '李工', dueDate: '2024-03-01', priority: 'high' }
      ]
    },
    B004: {
      winScore: 68,
      suggestion: '技术要求较高，需评估团队能力',
      dimensionScores: [
        { name: '客户关系', score: 55 },
        { name: '需求匹配', score: 65 },
        { name: '资质满足', score: 70 },
        { name: '交付能力', score: 75 },
        { name: '竞争态势', score: 60 }
      ],
      risks: [
        { level: 'high', desc: '数据中心建设经验相对不足', action: '建议寻找合作伙伴联合投标' },
        { level: 'medium', desc: '项目规模大，资源投入风险高', action: '需进行详细的成本收益分析' }
      ],
      autoTasks: [
        { id: 1, title: '合作伙伴寻找', owner: '张经理', dueDate: '2024-02-28', priority: 'high' },
        { id: 2, title: '成本收益分析', owner: '财务部', dueDate: '2024-03-02', priority: 'high' }
      ]
    }
  }
}

// 模拟API函数
export const api = {
  // 登录
  login: (username, password) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const user = mockData.users.find(u => u.name === username) || mockData.users[0]
        resolve({ success: true, data: user })
      }, 300)
    })
  },

  // 获取待办
  getTodos: () => Promise.resolve(mockData.todos),

  // 获取标讯
  getTenders: () => Promise.resolve(mockData.tenders),

  // 获取项目
  getProjects: () => Promise.resolve(mockData.projects),

  // 获取资质
  getQualifications: () => Promise.resolve(mockData.qualifications),

  // 获取案例
  getCases: () => Promise.resolve(mockData.cases),

  // 获取模板
  getTemplates: () => Promise.resolve(mockData.templates),

  // 获取费用
  getFees: () => Promise.resolve(mockData.fees),

  // 获取账户
  getAccounts: () => Promise.resolve(mockData.accounts),

  // 获取看板数据
  getDashboard: () => Promise.resolve(mockData.dashboard),

  // 获取日历
  getCalendar: () => Promise.resolve(mockData.calendar),

  // 获取评分分析
  getScoreAnalysis: (projectId) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({ success: true, data: mockData.scoreAnalysis[projectId] || mockData.scoreAnalysis.P001 })
      }, 300)
    })
  }
}

// 评分分析数据
mockData.scoreAnalysis = {
  P001: {
    scoreCategories: [
      { name: '技术', weight: 40, covered: 28, total: 40, percentage: 70, gaps: ['物联网架构方案', '大数据平台'] },
      { name: '商务', weight: 30, covered: 25, total: 30, percentage: 83, gaps: [] },
      { name: '案例', weight: 20, covered: 8, total: 20, percentage: 40, gaps: ['智慧城市案例'] },
      { name: '服务', weight: 10, covered: 7, total: 10, percentage: 70, gaps: ['运维承诺'] }
    ],
    gapItems: [
      { category: '技术', scorePoint: '物联网架构', required: '架构图+技术说明', status: 'missing' },
      { category: '技术', scorePoint: '大数据平台', required: '平台架构+性能指标', status: 'missing' },
      { category: '案例', scorePoint: '智慧城市案例', required: '至少1个同类案例', status: 'missing' },
      { category: '服务', scorePoint: '运维承诺', required: '3年免费运维承诺', status: 'missing' }
    ],
    aiSummary: {
      winScore: 72,
      winLevel: 'medium',
      risks: [
        { level: 'high', content: '技术方案中物联网架构缺失，可能扣15分' },
        { level: 'medium', content: '智慧城市同类案例储备不足' }
      ],
      suggestions: [
        '优先补充物联网架构方案，建议参考某省政府IoT项目',
        '尽快完善智慧城市案例材料，可使用西部智慧园区项目作为类似案例',
        '商务条件较好，建议继续保持'
      ]
    }
  }
}

// ========== AI功能相关Mock数据 ==========

// 竞争情报
mockData.competitionIntel = {
  P001: {
    updateTime: '2025-02-26 14:30',
    marketOverview: {
      totalBidders: 8,
      knownCompetitors: 5,
      newCompetitors: 2,
      withdrawCompetitors: 1
    },
    competitors: [
      {
        id: 'C001',
        name: '华东科技',
        threatLevel: 'high',
        similarBids: 12,
        priceRange: '800-1200万',
        winRate: 65,
        tactics: '低价策略，常以低于市场价10%-15%投标，适合价格敏感型客户',
        strongPoints: ['资质齐全', '案例丰富', '价格优势'],
        weakPoints: ['技术方案一般', '交付周期较长']
      },
      {
        id: 'C002',
        name: '智慧云通',
        threatLevel: 'medium',
        similarBids: 8,
        priceRange: '1000-1500万',
        winRate: 45,
        tactics: '技术领先策略，强调创新能力和AI能力，适合技术型客户',
        strongPoints: ['技术方案先进', 'AI能力突出'],
        weakPoints: ['案例较少', '价格偏高']
      },
      {
        id: 'C003',
        name: '城投建设',
        threatLevel: 'medium',
        similarBids: 15,
        priceRange: '900-1300万',
        winRate: 55,
        tactics: '本地化优势，强调本地服务能力，适合政府类项目',
        strongPoints: ['本地企业', '政府资源', '服务响应快'],
        weakPoints: ['技术实力一般', '跨区域能力弱']
      }
    ],
    strategies: [
      { priority: 'high', text: '突出技术方案创新性，避免与华东科技拼价格' },
      { priority: 'high', text: '尽快补齐智慧城市类案例，可借用关联公司案例' },
      { priority: 'medium', text: '加强客户关系维护，安排高层拜访' },
      { priority: 'medium', text: '报价建议控制在1000-1200万区间' }
    ],
    marketDynamics: [
      { date: '2025-02-25', event: '甲公司高层拜访客户', impact: 'negative', source: '内部消息' },
      { date: '2025-02-24', event: '乙公司近期项目被投诉', impact: 'positive', source: '行业新闻' },
      { date: '2025-02-23', event: '客户追加信创要求', impact: 'positive', source: '招标文件变更' }
    ],
    winningStrategy: {
      focusPoints: ['突出信创能力', '强调同类案例', '合理定价'],
      avoidPoints: ['不要过度承诺', '避免陷入价格战'],
      keyActions: [
        { action: '邀请客户参观我司信创实验室', priority: 'high', deadline: '2025-03-01' },
        { action: '准备3个以上央企同级别案例', priority: 'high', deadline: '2025-02-28' },
        { action: '与关键决策人建立联系', priority: 'medium', deadline: '2025-03-05' }
      ]
    }
  }
}

// 合规检查
mockData.complianceCheck = {
  P001: {
    checkDate: '2025-02-26',
    overallStatus: 'passed',
    overallScore: 95,
    highRiskCount: 1,
    mediumRiskCount: 2,
    checks: {
      mandatory: [
        { item: '营业执照', requirement: '在有效期内', status: 'pass', location: '资质文件夹' },
        { item: 'ISO9001认证', requirement: '具备质量管理体系认证', status: 'pass', location: '资质文件夹' },
        { item: 'ISO27001认证', requirement: '具备信息安全认证', status: 'pass', location: '资质文件夹' },
        { item: '涉密资质', requirement: '具备涉密资质', status: 'pass', location: '资质文件夹' },
        { item: 'CMMI5级认证', requirement: '具备CMMI5级或以上', status: 'pass', location: '资质文件夹' }
      ],
      format: [
        { item: '页码格式', result: '连续页码', status: 'pass' },
        { item: '字体要求', result: '宋体/标题黑体', status: 'pass' },
        { item: '装订要求', result: '胶装/左侧装订', status: 'pass' },
        { item: '签章要求', result: '公章+法人章', status: 'fail', action: '首页缺少法人章' }
      ],
      qualification: [
        { item: 'CMMI证书', status: 'valid', expireDate: '2027-03-15' },
        { item: 'ISO9001', status: 'valid', expireDate: '2026-12-31' },
        { item: 'ISO27001', status: 'expiring', expireDate: '2025-06-30' },
        { item: '安防资质', status: 'valid', expireDate: '2025-12-31' }
      ]
    },
    categories: [
      {
        id: 'CAT001',
        name: '资质文件',
        status: 'passed',
        score: 100,
        items: [
          { name: '营业执照', status: 'valid', expiry: '2030-05-20', file: '营业执照.pdf', checked: true },
          { name: 'ISO9001认证', status: 'valid', expiry: '2026-12-31', file: 'ISO9001.pdf', checked: true },
          { name: 'ISO27001认证', status: 'valid', expiry: '2026-08-30', file: 'ISO27001.pdf', checked: true },
          { name: '涉密资质', status: 'warning', expiry: '2025-06-30', file: '涉密资质.pdf', checked: true, warning: '即将到期' },
          { name: 'CMMI5级认证', status: 'valid', expiry: '2027-03-15', file: 'CMMI5.pdf', checked: true }
        ]
      },
      {
        id: 'CAT002',
        name: '人员要求',
        status: 'warning',
        score: 85,
        items: [
          { name: '项目经理（PMP）', status: 'valid', person: '张经理', certificate: 'PMP123456', checked: true },
          { name: '技术负责人（高级）', status: 'valid', person: '李工', certificate: '高级工程师', checked: true },
          { name: '安全工程师', status: 'missing', person: null, certificate: null, checked: false, warning: '需配备CISP认证人员' }
        ]
      },
      {
        id: 'CAT003',
        name: '技术应答',
        status: 'passed',
        score: 98,
        items: [
          { name: '技术偏离表', status: 'valid', deviation: '无偏离', checked: true },
          { name: '功能响应', status: 'valid', coverage: '100%', checked: true },
          { name: '性能指标', status: 'valid', met: '全部满足', checked: true }
        ]
      },
      {
        id: 'CAT004',
        name: '商务条款',
        status: 'passed',
        score: 92,
        items: [
          { name: '付款方式', status: 'valid', response: '响应', checked: true },
          { name: '交付周期', status: 'valid', response: '响应', checked: true },
          { name: '质保期', status: 'warning', response: '部分响应', checked: true, warning: '建议延长至3年' }
        ]
      }
    ],
    issues: [
      { id: 'ISS001', level: 'high', category: '人员', description: '缺少安全工程师（CISP）', suggestion: '外聘或内部培养', resolved: false },
      { id: 'ISS002', level: 'medium', category: '资质', description: '涉密资质即将到期', suggestion: '尽快启动续期', resolved: false },
      { id: 'ISS003', level: 'low', category: '商务', description: '质保期承诺偏短', suggestion: '考虑延长至3年', resolved: false }
    ],
    passedItems: 17,
    warningItems: 3,
    failedItems: 0
  }
}

// 版本历史
mockData.versionHistory = {
  P001: {
    versions: [
      {
        id: 'v3',
        version: '3.0',
        isCurrent: true,
        timestamp: '2024-03-10 14:30',
        author: '李四',
        avatar: '李',
        changes: ['更新技术架构章节', '补充AI能力描述', '修改报价为1180万']
      },
      {
        id: 'v2',
        version: '2.0',
        isCurrent: false,
        timestamp: '2024-03-08 10:15',
        author: '张三',
        avatar: '张',
        changes: ['补充案例章节', '更新服务承诺', '调整项目团队']
      },
      {
        id: 'v1',
        version: '1.0',
        isCurrent: false,
        timestamp: '2024-03-05 16:00',
        author: '王五',
        avatar: '王',
        changes: ['初始版本创建', '完成基础框架']
      }
    ]
  }
}

// 协作记录
mockData.collaboration = [
  {
    id: 'COLL001',
    type: 'comment',
    author: '张经理',
    avatar: '',
    content: '技术方案的移动端部分需要再详细一些，建议单独作为一个章节',
    position: { section: '技术方案', page: 15 },
    time: '2025-02-25 14:20',
    resolved: false,
    replies: [
      { author: '李工', content: '好的，我会在下一版中补充', time: '2025-02-25 15:00' }
    ]
  },
  {
    id: 'COLL002',
    type: 'review',
    author: '李总',
    avatar: '',
    content: '整体方案不错，报价策略需要再评估一下',
    position: { section: '商务报价', page: 32 },
    time: '2025-02-25 16:30',
    resolved: false,
    replies: []
  },
  {
    id: 'COLL003',
    type: 'task',
    author: '系统',
    avatar: '',
    content: 'AI检测到1处可能的错别字："信产" → "信创"？',
    position: { section: '技术方案', page: 8, line: 12 },
    time: '2025-02-26 10:00',
    resolved: true,
    replies: [
      { author: '李工', content: '已修正', time: '2025-02-26 10:30' }
    ]
  },
  {
    id: 'COLL004',
    type: 'approval',
    author: '张经理',
    avatar: '',
    content: '技术方案已通过评审',
    position: null,
    time: '2025-02-26 11:00',
    resolved: true,
    replies: []
  }
]

// ROI分析
mockData.roiAnalysis = {
  P001: {
    projectBudget: 500,
    estimatedRevenue: 500,
    estimatedCost: {
      bidPreparation: 2,
      deposit: 10,
      travel: 1.5,
      personnel: 8,
      other: 0.5
    },
    totalCost: 22,
    netProfit: 478,
    profitMargin: 95.6,
    paybackPeriod: '项目结束后',
    riskAdjustedProfit: 410,
    assumptions: [
      '中标概率：78%',
      '项目周期：6个月',
      '回款周期：3-6-1（30%验收款）'
    ],
    costBreakdown: [
      { category: '人力成本', amount: 8, percentage: 36.4, description: '技术+商务团队' },
      { category: '保证金', amount: 10, percentage: 45.5, description: '投标保证金（可退）' },
      { category: '差旅费', amount: 1.5, percentage: 6.8, description: '现场踏勘+述标' },
      { category: '标书编制', amount: 2, percentage: 9.1, description: '打印装订等' },
      { category: '其他', amount: 0.5, percentage: 2.2, description: '杂项支出' }
    ],
    comparison: [
      { name: '本次项目', budget: 500, cost: 22, roi: 2173 },
      { name: '行业平均', budget: 500, cost: 25, roi: 1900 },
      { name: '最佳实践', budget: 500, cost: 18, roi: 2678 }
    ]
  }
}

// 自动化任务
mockData.autoTasks = [
  {
    id: 'AT001',
    name: '资质到期提醒',
    type: 'alert',
    trigger: 'date',
    triggerValue: '2025-06-30',
    status: 'scheduled',
    description: '涉密资质将于2025-06-30到期，请提前3个月启动续期',
    action: '发送通知给行政部'
  },
  {
    id: 'AT002',
    name: '招标文件变更监控',
    type: 'monitor',
    trigger: 'external',
    triggerValue: '某央企项目',
    status: 'active',
    description: '监控招标文件变更，如有更新立即通知',
    action: '检查频率：每日2次'
  },
  {
    id: 'AT003',
    name: '竞争对手动态跟踪',
    type: 'monitor',
    trigger: 'external',
    triggerValue: '甲公司,乙公司',
    status: 'active',
    description: '跟踪竞争对手在同类项目中的动态',
    action: '每周汇总报告'
  },
  {
    id: 'AT004',
    name: '标书质量检查',
    type: 'check',
    trigger: 'manual',
    triggerValue: '文档上传后',
    status: 'ready',
    description: '自动检查标书完整性和规范性',
    action: 'AI扫描检测'
  },
  {
    id: 'AT005',
    name: '报价优化建议',
    type: 'recommend',
    trigger: 'manual',
    triggerValue: '报价确定前',
    status: 'ready',
    description: '基于历史数据和竞争情况，提供报价建议',
    action: '生成报价建议报告'
  },
  {
    id: 'AT006',
    name: '截止日期提醒',
    type: 'alert',
    trigger: 'date',
    triggerValue: '2025-03-15',
    status: 'scheduled',
    description: '投标截止前3天、1天、当天提醒',
    action: '多级提醒通知'
  }
]

// 移动端卡片数据
mockData.mobileCard = {
  summary: {
    totalProjects: 3,
    urgentTasks: 2,
    todayDeadlines: 1,
    unreadMessages: 5
  },
  quickActions: [
    { id: 'QA001', name: '新建项目', icon: 'plus', route: '/project/create' },
    { id: 'QA002', name: '标讯搜索', icon: 'search', route: '/bidding' },
    { id: 'QA003', name: 'AI分析', icon: 'magic', route: '/bidding/ai-analysis/P001' },
    { id: 'QA004', name: '数据看板', icon: 'chart', route: '/analytics' }
  ],
  urgentTasks: [
    { id: 'UT001', title: '某央企项目 - 保证金缴纳', deadline: '2025-02-26 18:00', type: 'payment', urgent: true },
    { id: 'UT002', title: '华南电力 - 方案评审', deadline: '2025-02-27 10:00', type: 'review', urgent: true }
  ],
  recentActivities: [
    { id: 'RA001', action: '李工上传了', target: '技术方案v2.0.docx', time: '10分钟前' },
    { id: 'RA002', action: '张经理审批通过', target: '商务应答v1.5', time: '1小时前' },
    { id: 'RA003', action: 'AI检测完成', target: '技术方案检查', time: '2小时前' },
    { id: 'RA004', action: '系统提醒', target: '资质即将到期', time: '昨天' }
  ],
  notifications: [
    { id: 'N001', type: 'warning', title: '涉密资质即将到期', content: '将于2025-06-30到期，请提前续期', time: '2025-02-26 09:00', read: false },
    { id: 'N002', type: 'info', title: '招标文件更新', content: '某央企项目招标文件有变更', time: '2025-02-25 15:30', read: false },
    { id: 'N003', type: 'success', title: 'AI检测通过', content: '技术方案v2.0检测通过', time: '2025-02-25 14:00', read: true },
    { id: 'N004', type: 'warning', title: '截止提醒', content: '保证金缴纳截止今天18:00', time: '2025-02-26 08:00', read: false },
    { id: 'N005', type: 'info', title: '新标讯推荐', content: '发现3条匹配的标讯', time: '2025-02-25 10:00', read: true }
  ]
}

// 文档编辑器数据
mockData.documentEditor = {
  P001: {
    documentType: 'technical',
    documentName: '技术方案v2.0.docx',
    lastModified: '2025-02-26 14:30',
    lastModifier: '李工',
    sections: [
      { id: 'SEC001', name: '项目概述', level: 1, order: 1, wordCount: 520, status: 'completed' },
      { id: 'SEC002', name: '系统架构设计', level: 1, order: 2, wordCount: 1200, status: 'completed' },
      { id: 'SEC003', name: '功能设计', level: 1, order: 3, wordCount: 2300, status: 'completed' },
      { id: 'SEC004', name: '移动端方案', level: 1, order: 4, wordCount: 800, status: 'in_progress' },
      { id: 'SEC005', name: '安全方案', level: 1, order: 5, wordCount: 950, status: 'completed' },
      { id: 'SEC006', name: '实施方案', level: 1, order: 6, wordCount: 1100, status: 'pending' },
      { id: 'SEC007', name: '运维方案', level: 1, order: 7, wordCount: 0, status: 'pending' }
    ],
    aiSuggestions: [
      { id: 'AI001', type: 'addition', position: 'SEC004', content: '建议增加移动端离线功能说明', priority: 'medium' },
      { id: 'AI002', type: 'completion', position: 'SEC006', content: '实施方案章节尚未开始', priority: 'high' },
      { id: 'AI003', type: 'improvement', position: 'SEC005', content: '可增加零信任安全架构描述', priority: 'low' }
    ],
    statistics: {
      totalWords: 6870,
      totalPages: 24,
      completionRate: 71,
      estimatedReadTime: 28
    }
  }
}

// 文档组装数据
mockData.documentAssembly = {
  P001: {
    name: '某央企智慧办公平台采购-投标文件',
    assemblyDate: '2025-02-26',
    status: 'in_progress',
    components: [
      {
        id: 'COMP001',
        type: 'document',
        name: '技术方案',
        file: '技术方案v2.0.docx',
        status: 'included',
        order: 1,
        pages: 15,
        required: true
      },
      {
        id: 'COMP002',
        type: 'document',
        name: '商务应答',
        file: '商务应答v1.5.docx',
        status: 'included',
        order: 2,
        pages: 8,
        required: true
      },
      {
        id: 'COMP003',
        type: 'qualification',
        name: '资质文件',
        file: '资质文件合集.pdf',
        status: 'included',
        order: 3,
        pages: 12,
        required: true
      },
      {
        id: 'COMP004',
        type: 'case',
        name: '案例展示',
        file: '成功案例集.pdf',
        status: 'included',
        order: 4,
        pages: 10,
        required: true
      },
      {
        id: 'COMP005',
        type: 'certificate',
        name: '项目团队证书',
        file: null,
        status: 'missing',
        order: 5,
        pages: 0,
        required: true
      },
      {
        id: 'COMP006',
        type: 'quote',
        name: '报价单',
        file: '报价单v1.0.xlsx',
        status: 'included',
        order: 6,
        pages: 3,
        required: true
      }
    ],
    checklist: [
      { id: 'CHK001', item: '技术方案', status: 'passed', remark: '' },
      { id: 'CHK002', item: '商务应答', status: 'passed', remark: '' },
      { id: 'CHK003', item: '资质文件', status: 'warning', remark: '涉密资质即将到期' },
      { id: 'CHK004', item: '案例展示', status: 'passed', remark: '' },
      { id: 'CHK005', item: '项目团队证书', status: 'failed', remark: '缺少CISP证书' },
      { id: 'CHK006', item: '报价单', status: 'passed', remark: '' },
      { id: 'CHK007', item: '授权书', status: 'passed', remark: '' },
      { id: 'CHK008', item: '唱标单', status: 'passed', remark: '' }
    ],
    progress: {
      total: 8,
      completed: 6,
      percentage: 75
    },
    preview: {
      totalPages: 48,
      fileSize: '12.5MB',
      format: 'PDF'
    }
  }
}

// 客户商机中心数据
mockData.customerInsights = [
  {
    customerId: 'CUST_001',
    customerName: '宝武集团采购共享中心',
    industry: '钢铁制造',
    region: '华东',
    mainCategories: ['劳保用品', '搬运设备', '工业耗材'],
    purchaseFrequency12m: 26,
    commonPurchaseMonths: ['1月', '4月', '7月', '10月'],
    avgBudget: 660,
    budgetRange: '520-780',
    lastPurchaseDate: '2025-01-15',
    cycleType: '季度',
    opportunityScore: 92,
    predictedNextWindow: '2025-04',
    status: 'recommend',
    predictionSummary: '季度性集采明显，劳保、搬运和工业耗材预算稳定，适合持续经营。'
  },
  {
    customerId: 'CUST_002',
    customerName: '国家电网江苏省电力有限公司',
    industry: '能源电力',
    region: '华东',
    mainCategories: ['工具', '电缆', '测试仪器'],
    purchaseFrequency12m: 18,
    commonPurchaseMonths: ['3月', '9月'],
    avgBudget: 480,
    budgetRange: '380-540',
    lastPurchaseDate: '2025-01-05',
    cycleType: '半年',
    opportunityScore: 88,
    predictedNextWindow: '2025-03',
    status: 'watch',
    predictionSummary: '每年春秋两季有集中集采，预算集中在 380-540 万。'
  },
  {
    customerId: 'CUST_003',
    customerName: '中国石油天然气集团有限公司',
    industry: '能源化工',
    region: '西北',
    mainCategories: ['安全防护', '阀门管件', '检维修工具'],
    purchaseFrequency12m: 20,
    commonPurchaseMonths: ['2月', '6月', '11月'],
    avgBudget: 720,
    budgetRange: '600-900',
    lastPurchaseDate: '2024-11-20',
    cycleType: '半年度',
    opportunityScore: 90,
    predictedNextWindow: '2025-06',
    status: 'recommend',
    predictionSummary: '炼化与检修体系采购规律稳定，安全防护和阀门管件需求持续。'
  },
  {
    customerId: 'CUST_004',
    customerName: '中国中铁股份有限公司',
    industry: '基础建设',
    region: '华北',
    mainCategories: ['焊接设备', '测量仪器', '建工辅材'],
    purchaseFrequency12m: 16,
    commonPurchaseMonths: ['3月', '8月'],
    avgBudget: 580,
    budgetRange: '420-700',
    lastPurchaseDate: '2025-02-18',
    cycleType: '工程节点型',
    opportunityScore: 84,
    predictedNextWindow: '2025-08',
    status: 'watch',
    predictionSummary: '随重点工程节点释放设备采购，测量和施工辅材需求集中。'
  },
  {
    customerId: 'CUST_005',
    customerName: '中国移动通信集团浙江有限公司',
    industry: '通信运营',
    region: '华东',
    mainCategories: ['机房耗材', '低压电气', '网络配套'],
    purchaseFrequency12m: 14,
    commonPurchaseMonths: ['5月', '11月'],
    avgBudget: 430,
    budgetRange: '300-520',
    lastPurchaseDate: '2024-11-08',
    cycleType: '半年',
    opportunityScore: 81,
    predictedNextWindow: '2025-05',
    status: 'watch',
    predictionSummary: '围绕机房改造和运维补货形成稳定半年度采购节奏。'
  },
  {
    customerId: 'CUST_006',
    customerName: '中国华能集团有限公司',
    industry: '发电能源',
    region: '华北',
    mainCategories: ['检修工器具', '劳保用品', '仪器仪表'],
    purchaseFrequency12m: 17,
    commonPurchaseMonths: ['4月', '10月'],
    avgBudget: 510,
    budgetRange: '400-620',
    lastPurchaseDate: '2024-10-16',
    cycleType: '半年',
    opportunityScore: 86,
    predictedNextWindow: '2025-04',
    status: 'recommend',
    predictionSummary: '春秋检修季采购明显，发电侧劳保和检修设备体量稳定。'
  },
  {
    customerId: 'CUST_007',
    customerName: '中国建筑第八工程局有限公司',
    industry: '建筑工程',
    region: '华东',
    mainCategories: ['电动工具', '安全消防', '建工材料'],
    purchaseFrequency12m: 19,
    commonPurchaseMonths: ['3月', '7月', '9月'],
    avgBudget: 760,
    budgetRange: '580-980',
    lastPurchaseDate: '2025-01-26',
    cycleType: '项目制',
    opportunityScore: 89,
    predictedNextWindow: '2025-07',
    status: 'recommend',
    predictionSummary: '大型工程项目带动年中集中补货，电动工具和安全材料需求突出。'
  },
  {
    customerId: 'CUST_008',
    customerName: '中国中车股份有限公司',
    industry: '轨道交通装备',
    region: '华中',
    mainCategories: ['工业检测', '紧固密封', '液压气动'],
    purchaseFrequency12m: 15,
    commonPurchaseMonths: ['2月', '5月', '9月'],
    avgBudget: 540,
    budgetRange: '420-650',
    lastPurchaseDate: '2025-02-10',
    cycleType: '季度滚动',
    opportunityScore: 83,
    predictedNextWindow: '2025-05',
    status: 'watch',
    predictionSummary: '装备制造采购以季度滚动方式释放，检测与液压类采购持续性较强。'
  }
]

mockData.customerPurchases = [
  {
    recordId: 'CPR_001',
    customerId: 'CUST_001',
    source: 'platform',
    publishDate: '2025-01-15',
    title: '宝武集团采购共享中心劳保用品年度集采',
    category: '劳保用品',
    budget: 560,
    contentSummary: '包含防护服、安全帽、消防器材及工业清洁耗材',
    extractedTags: ['劳保', '年度采购', '华东']
  },
  {
    recordId: 'CPR_002',
    customerId: 'CUST_001',
    source: 'platform',
    publishDate: '2024-10-10',
    title: '宝武集团采购共享中心搬运及输送设备集中采购',
    category: '搬运设备',
    budget: 740,
    contentSummary: '叉车、搬运机器人、AGV',
    extractedTags: ['搬运', 'AGV', '华东']
  },
  {
    recordId: 'CPR_003',
    customerId: 'CUST_002',
    source: 'platform',
    publishDate: '2025-01-05',
    title: '国家电网江苏省电力有限公司检修工具及检测仪器采购',
    category: '检测工具',
    budget: 450,
    contentSummary: '电缆检测仪、变电站检修工具、绝缘测试设备',
    extractedTags: ['检测', '工具', '华东']
  },
  {
    recordId: 'CPR_004',
    customerId: 'CUST_003',
    source: 'platform',
    publishDate: '2024-11-20',
    title: '中国石油天然气集团有限公司炼化检维修工器具采购',
    category: '检维修工具',
    budget: 680,
    contentSummary: '防爆工具、检修套装、油田作业安全防护物资',
    extractedTags: ['炼化', '检修', '安全']
  },
  {
    recordId: 'CPR_005',
    customerId: 'CUST_003',
    source: 'platform',
    publishDate: '2024-06-18',
    title: '中国石油天然气集团有限公司阀门管件框架采购',
    category: '阀门管件',
    budget: 860,
    contentSummary: '阀门、法兰、密封件及管线检修配套物资',
    extractedTags: ['阀门', '框架采购', '管件']
  },
  {
    recordId: 'CPR_006',
    customerId: 'CUST_004',
    source: 'platform',
    publishDate: '2025-02-18',
    title: '中国中铁股份有限公司工程测量仪器及焊接设备采购',
    category: '测量仪器',
    budget: 520,
    contentSummary: '全站仪、激光测距仪、焊接设备及建工辅材',
    extractedTags: ['工程', '测量', '焊接']
  },
  {
    recordId: 'CPR_007',
    customerId: 'CUST_005',
    source: 'platform',
    publishDate: '2024-11-08',
    title: '中国移动通信集团浙江有限公司机房低压配电及耗材采购',
    category: '低压电气',
    budget: 410,
    contentSummary: '机房低压配电柜、PDU、线缆辅材及运维耗材',
    extractedTags: ['机房', '低压电气', '通信']
  },
  {
    recordId: 'CPR_008',
    customerId: 'CUST_006',
    source: 'platform',
    publishDate: '2024-10-16',
    title: '中国华能集团有限公司春检工器具及劳保用品采购',
    category: '检修工器具',
    budget: 530,
    contentSummary: '检修工具箱、劳保用品及电厂运维仪表',
    extractedTags: ['检修', '发电', '劳保']
  },
  {
    recordId: 'CPR_009',
    customerId: 'CUST_007',
    source: 'platform',
    publishDate: '2025-01-26',
    title: '中国建筑第八工程局有限公司工程现场电动工具集采',
    category: '电动工具',
    budget: 820,
    contentSummary: '冲击钻、切割机、安全消防器材及辅材',
    extractedTags: ['工程', '电动工具', '安全']
  },
  {
    recordId: 'CPR_010',
    customerId: 'CUST_008',
    source: 'platform',
    publishDate: '2025-02-10',
    title: '中国中车股份有限公司工业检测与液压气动配件采购',
    category: '工业检测',
    budget: 560,
    contentSummary: '检测仪表、液压件、气动元件及密封紧固件',
    extractedTags: ['轨交装备', '检测', '液压']
  }
]

mockData.customerPredictions = [
  {
    opportunityId: 'CO_OPP_001',
    customerId: 'CUST_001',
    suggestedProjectName: '宝武集团采购共享中心劳保及搬运设备年度集采',
    predictedCategory: '劳保用品+搬运设备',
    predictedBudgetMin: 520,
    predictedBudgetMax: 780,
    predictedWindow: '2025-04',
    confidence: 0.92,
    reasoningSummary: '连续两年 4 月启动季度补货，预算稳定在 520-780 万，劳保与搬运类匹配度高。',
    evidenceRecords: ['CPR_001', 'CPR_002'],
    convertedProjectId: 'P001'
  },
  {
    opportunityId: 'CO_OPP_002',
    customerId: 'CUST_002',
    suggestedProjectName: '国家电网江苏省电力有限公司春季检修工具采购',
    predictedCategory: '检测仪器',
    predictedBudgetMin: 380,
    predictedBudgetMax: 540,
    predictedWindow: '2025-03',
    confidence: 0.86,
    reasoningSummary: '近两年 3 月/9 月集采周期稳定，预算靠近 400-520 万，AI 判断工具+测试类需求集中。',
    evidenceRecords: ['CPR_003'],
    convertedProjectId: ''
  },
  {
    opportunityId: 'CO_OPP_003',
    customerId: 'CUST_003',
    suggestedProjectName: '中国石油天然气集团有限公司夏季检维修物资框采',
    predictedCategory: '安全防护+阀门管件',
    predictedBudgetMin: 620,
    predictedBudgetMax: 920,
    predictedWindow: '2025-06',
    confidence: 0.91,
    reasoningSummary: '6 月前后连续出现炼化检修及阀门框采，预算稳定且需求和我司供货能力高度贴合。',
    evidenceRecords: ['CPR_004', 'CPR_005'],
    convertedProjectId: ''
  },
  {
    opportunityId: 'CO_OPP_004',
    customerId: 'CUST_004',
    suggestedProjectName: '中国中铁股份有限公司下半年工程测量设备采购',
    predictedCategory: '测量仪器+焊接设备',
    predictedBudgetMin: 450,
    predictedBudgetMax: 700,
    predictedWindow: '2025-08',
    confidence: 0.82,
    reasoningSummary: '重点工程推进期会集中补充测量与施工设备，8 月窗口较明确。',
    evidenceRecords: ['CPR_006'],
    convertedProjectId: ''
  },
  {
    opportunityId: 'CO_OPP_005',
    customerId: 'CUST_005',
    suggestedProjectName: '中国移动通信集团浙江有限公司机房配套低压物资采购',
    predictedCategory: '低压电气+机房耗材',
    predictedBudgetMin: 320,
    predictedBudgetMax: 520,
    predictedWindow: '2025-05',
    confidence: 0.79,
    reasoningSummary: '机房改造和运维补货以半年度触发，低压配套物资需求稳定。',
    evidenceRecords: ['CPR_007'],
    convertedProjectId: ''
  },
  {
    opportunityId: 'CO_OPP_006',
    customerId: 'CUST_006',
    suggestedProjectName: '中国华能集团有限公司春检工器具与劳保集采',
    predictedCategory: '检修工器具+劳保用品',
    predictedBudgetMin: 420,
    predictedBudgetMax: 620,
    predictedWindow: '2025-04',
    confidence: 0.87,
    reasoningSummary: '发电企业春检窗口稳定，检修类工器具与劳保用品采购体量大。',
    evidenceRecords: ['CPR_008'],
    convertedProjectId: ''
  },
  {
    opportunityId: 'CO_OPP_007',
    customerId: 'CUST_007',
    suggestedProjectName: '中国建筑第八工程局有限公司年中工程物资补货项目',
    predictedCategory: '电动工具+安全消防',
    predictedBudgetMin: 600,
    predictedBudgetMax: 980,
    predictedWindow: '2025-07',
    confidence: 0.89,
    reasoningSummary: '年中工程全面铺开，现场施工和安全配套物资采购将集中释放。',
    evidenceRecords: ['CPR_009'],
    convertedProjectId: ''
  },
  {
    opportunityId: 'CO_OPP_008',
    customerId: 'CUST_008',
    suggestedProjectName: '中国中车股份有限公司季度检测与液压配件补货',
    predictedCategory: '工业检测+液压气动',
    predictedBudgetMin: 420,
    predictedBudgetMax: 650,
    predictedWindow: '2025-05',
    confidence: 0.8,
    reasoningSummary: '轨交装备制造场景下，季度性检测与液压配件采购持续滚动发生。',
    evidenceRecords: ['CPR_010'],
    convertedProjectId: ''
  }
]

// ========== BAR 投标资产台账 Mock数据 ==========

mockData.barSites = [
  {
    id: 'S001',
    name: '中国政府采购网',
    url: 'http://www.ccgp.gov.cn',
    region: '全国',
    industry: '政府',
    siteType: '公共资源交易中心',
    loginType: 'both', // password / ca / both
    status: 'active', // active / inactive / need_verify
    hasRisk: true,
    riskLevel: 'medium', // high / medium / low
    lastVerifyTime: '2025-03-01',
    nextVerifyTime: '2025-03-15',
    remark: '需要IE浏览器，安装CA驱动插件',
    createTime: '2024-01-15',
    accounts: [
      {
        id: 'A001',
        username: 'xy_admin',
        phone: '138****1234',
        email: 't***@example.com',
        role: 'admin', // admin / operator / viewer
        owner: '张三',
        status: 'active',
        recoverMethod: 'sms' // sms / email / manual
      },
      {
        id: 'A002',
        username: 'xy_operator',
        phone: '139****5678',
        email: 'o***@example.com',
        role: 'operator',
        owner: '李四',
        status: 'active',
        recoverMethod: 'sms'
      }
    ],
    uks: [
      {
        id: 'UK001',
        type: '法人CA',
        provider: '北京CA',
        serialNo: 'SN123456789',
        expiryDate: '2025-12-31',
        holder: '王五',
        location: '保险柜A-1',
        status: 'available' // available / borrowed / expired
      },
      {
        id: 'UK002',
        type: '业务CA',
        provider: '北京CA',
        serialNo: 'SN789012345',
        expiryDate: '2025-04-15',
        holder: '赵六',
        location: '保险柜A-2',
        status: 'borrowed',
        borrower: '李四',
        borrowProject: '某央企智慧办公平台采购',
        borrowPurpose: '投标上传',
        borrowTime: '2025-02-28 10:00',
        expectedReturn: '2025-03-05'
      }
    ],
    sop: {
      resetUrl: 'http://www.ccgp.gov.cn/reset',
      unlockUrl: 'http://www.ccgp.gov.cn/unlock',
      contacts: ['400-888-8888 (工作日 9:00-17:00)'],
      requiredDocs: [
        { name: '营业执照副本（加盖公章）', required: true },
        { name: '法人授权委托书', required: true },
        { name: '经办人身份证正反面', required: true },
        { name: 'CA证书申请表（现场填写）', required: true }
      ],
      estimatedTime: '1-3个工作日',
      faqs: [
        {
          q: '验证码收不到怎么办？',
          a: '尝试使用邮箱找回，或联系客服人工处理。'
        },
        {
          q: 'CA密码输错3次被锁？',
          a: '需携带CA证书和授权书到服务网点解锁。'
        }
      ],
      history: [
        { date: '2025-02-15', user: '张三', action: '密码重置成功', duration: '1天' },
        { date: '2025-01-20', user: '李四', action: 'CA补办完成', duration: '5天' }
      ]
    },
    attachments: [
      { id: 'ATT001', name: '营业执照.pdf', url: '#', size: '2.3MB', uploadTime: '2024-01-15' },
      { id: 'ATT002', name: 'CA办理资料.zip', url: '#', size: '5.6MB', uploadTime: '2024-01-15' },
      { id: 'ATT003', name: '注册截图.png', url: '#', size: '1.2MB', uploadTime: '2024-01-15' }
    ],
    auditLog: [
      { time: '2025-03-01 10:30', user: '张三', action: '验证登录成功' },
      { time: '2025-02-28 15:20', user: '李四', action: '借用 业务CA (SN789012345)' },
      { time: '2025-02-15 14:00', user: '张三', action: '密码重置' },
      { time: '2025-01-20 09:30', user: '李四', action: 'CA补办' }
    ]
  },
  {
    id: 'S002',
    name: '广东省公共资源交易中心',
    url: 'http://www.gdggzy.cn',
    region: '广东',
    industry: '政府',
    siteType: '公共资源交易中心',
    loginType: 'ca',
    status: 'active',
    hasRisk: true,
    riskLevel: 'high', // high / medium / low
    lastVerifyTime: '2025-02-20',
    nextVerifyTime: '2025-03-20',
    remark: '仅支持CA登录，需要广东CA',
    createTime: '2024-02-10',
    accounts: [],
    uks: [
      {
        id: 'UK003',
        type: '机构CA',
        provider: '广东CA',
        serialNo: 'GD987654321',
        expiryDate: '2025-06-30',
        holder: '王五',
        location: '保险柜B-1',
        status: 'available'
      }
    ],
    sop: {
      resetUrl: 'http://www.gdggzy.cn/help',
      contacts: ['020-12345678'],
      requiredDocs: [
        { name: '组织机构代码证', required: true },
        { name: '法人授权书', required: true }
      ],
      estimatedTime: '3-5个工作日',
      faqs: [],
      history: []
    },
    attachments: [],
    auditLog: []
  },
  {
    id: 'S003',
    name: '某央企招标采购平台',
    url: 'http://bidding.xxx-corp.com',
    region: '北京',
    industry: '央企',
    siteType: '企业自建平台',
    loginType: 'password',
    status: 'inactive',
    hasRisk: false,
    riskLevel: 'low', // high / medium / low
    lastVerifyTime: '2025-01-15',
    nextVerifyTime: '2025-04-15',
    remark: '账号密码丢失，正在找回中',
    createTime: '2023-06-20',
    accounts: [
      {
        id: 'A003',
        username: 'xiyu_bidding',
        phone: '138****9999',
        email: 'b***@example.com',
        role: 'admin',
        owner: '张三',
        status: 'inactive',
        recoverMethod: 'manual'
      }
    ],
    uks: [],
    sop: {
      resetUrl: 'http://bidding.xxx-corp.com/reset',
      contacts: ['客服工单系统'],
      requiredDocs: [
        { name: '营业执照', required: true },
        { name: '公章扫描件', required: true },
        { name: '法人身份证明', required: true }
      ],
      estimatedTime: '5-7个工作日',
      faqs: [
        {
          q: '账号被锁定怎么办？',
          a: '需要提交工单，上传营业执照和法人授权书，等待客服人工处理。'
        }
      ],
      history: []
    },
    attachments: [
      { id: 'ATT004', name: '工单回执.pdf', url: '#', size: '0.5MB', uploadTime: '2025-01-15' }
    ],
    auditLog: [
      { time: '2025-01-15 09:00', user: '张三', action: '发现账号无法登录' },
      { time: '2025-01-15 10:30', user: '张三', action: '提交找回工单' }
    ]
  },
  {
    id: 'S004',
    name: '深圳市建设工程交易服务中心',
    url: 'http://www.szjsjy.cn',
    region: '深圳',
    industry: '建设',
    siteType: '公共资源交易中心',
    loginType: 'both',
    status: 'active',
    hasRisk: false,
    lastVerifyTime: '2025-02-28',
    nextVerifyTime: '2025-03-28',
    remark: '需要办理深圳数字证书',
    createTime: '2024-03-01',
    accounts: [
      {
        id: 'A004',
        username: 'sz_xy_001',
        phone: '137****1111',
        email: 's***@example.com',
        role: 'admin',
        owner: '王五',
        status: 'active',
        recoverMethod: 'email'
      }
    ],
    uks: [
      {
        id: 'UK004',
        type: '机构数字证书',
        provider: '深圳CA',
        serialNo: 'SZ11223344',
        expiryDate: '2025-10-20',
        holder: '王五',
        location: '保险柜C-1',
        status: 'available'
      }
    ],
    sop: {
      resetUrl: 'http://www.szjsjy.cn/help',
      contacts: ['0755-12345678'],
      requiredDocs: [
        { name: '企业资质证书', required: true },
        { name: '法人授权书', required: true }
      ],
      estimatedTime: '2-3个工作日',
      faqs: [],
      history: []
    },
    attachments: [],
    auditLog: []
  },
  {
    id: 'S005',
    name: '军队采购网',
    url: 'http://www.plap.cn',
    region: '全国',
    industry: '军队',
    siteType: '军队采购平台',
    loginType: 'ca',
    status: 'active',
    hasRisk: true,
    riskLevel: 'high', // high / medium / low
    lastVerifyTime: '2025-02-25',
    nextVerifyTime: '2025-03-25',
    remark: '需要军队专用CA，办理周期较长',
    createTime: '2023-11-10',
    accounts: [],
    uks: [
      {
        id: 'UK005',
        type: '军队采购CA',
        provider: '军队CA中心',
        serialNo: 'PLA55667788',
        expiryDate: '2025-08-15',
        holder: '李四',
        location: '保险柜A-3',
        status: 'available'
      }
    ],
    sop: {
      resetUrl: 'http://www.plap.cn/help',
      contacts: ['010-66666666 (军队专线)'],
      requiredDocs: [
        { name: '军工保密资格证', required: true },
        { name: '法人授权书（加盖公章）', required: true },
        { name: '经办人军官证/身份证', required: true }
      ],
      estimatedTime: '7-15个工作日',
      faqs: [
        {
          q: 'CA过期怎么办？',
          a: '需提前30个工作日申请续期，携带原有CA证书和授权书到指定地点办理。'
        }
      ],
      history: []
    },
    attachments: [],
    auditLog: []
  }
]
