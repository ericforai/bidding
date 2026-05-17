// Input: none
// Output: Bidding list page option constants, nationwide region options, and default form factories
// Pos: src/views/Bidding/list/ - Local constants for the bidding list page
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

export const REGION_OPTIONS = [
  '北京',
  '天津',
  '河北',
  '山西',
  '内蒙古',
  '辽宁',
  '吉林',
  '黑龙江',
  '上海',
  '江苏',
  '浙江',
  '安徽',
  '福建',
  '江西',
  '山东',
  '河南',
  '湖北',
  '湖南',
  '广东',
  '广西',
  '海南',
  '重庆',
  '四川',
  '贵州',
  '云南',
  '西藏',
  '陕西',
  '甘肃',
  '青海',
  '宁夏',
  '新疆',
  '台湾',
  '香港',
  '澳门',
]

export const INDUSTRY_OPTIONS = ['政府', '能源', '交通', '数据中心', '金融', '医疗', '教育']

export const CUSTOMER_TYPE_OPTIONS = ['央企集团', '国有集团', 'KA 客户']

export const PRIORITY_OPTIONS = [
  {
    value: 'S',
    label: 'S 级',
    desc: '战略级高价值客户',
    standard: '年度合作额超 5000 万存量客户及超大型央企集团',
  },
  {
    value: 'A',
    label: 'A 级',
    desc: '高价值重点客户',
    standard: '年度合作额超 1000 万存量客户及其他央企集团',
  },
  {
    value: 'B',
    label: 'B 级',
    desc: '重要潜力客户',
    standard: '省属/市属国企，或营收超 100 亿制造业民营/外资企业',
  },
  {
    value: 'C',
    label: 'C 级',
    desc: '潜力客户',
    standard: '营收 50-100 亿制造业民营/外资企业',
  },
]

export const SOURCE_OPTIONS = [
  { label: '外部获取', value: 'EXTERNAL' },
  { label: '人工录入', value: 'MANUAL' },
]

export const SOURCE_TYPE_OPTIONS = [
  { label: '全部来源', value: '' },
  { label: '外部获取', value: 'EXTERNAL' },
  { label: '人工录入', value: 'MANUAL' },
]

export const SOURCE_PLATFORM_OPTIONS = ['中国政府采购网', '各省招标网', '第三方商机服务', '企业招标平台']

export const SOURCE_KEYWORD_OPTIONS = [
  'MRO 工具',
  '工具耗材',
  '焊接',
  '刀具',
  '量具',
  '机床',
  '磨具',
  '润滑',
  '胶粘',
  '车间化学品',
  '劳保',
  '安全消防',
  '搬运存储',
  '工控',
  '低压',
  '电工',
  '照明',
  '轴承',
  '液压',
  '管阀',
  '泵',
]

export const ASSIGN_RULES = [
  { value: 'region', label: '按总部所在地分发', desc: '根据标讯总部所在地自动分配' },
  { value: 'score', label: '按 AI 评分', desc: '高分优先给候选人' },
  { value: 'average', label: '平均分配', desc: '均匀分配给可选人员' },
]

export const DEFAULT_SEARCH_FORM = Object.freeze({
  keyword: '',
  region: '',
  status: '',
  source: '',
  sourceType: '',
  customerType: '',
  priority: '',
  registrationDeadlineFrom: null,
  registrationDeadlineTo: null,
  bidOpeningTimeFrom: null,
  bidOpeningTimeTo: null,
})

export const DEFAULT_SOURCE_CONFIG = Object.freeze({
  platforms: ['中国政府采购网'],
  apiEndpoint: '',
  apiKey: '',
  keywords: [],
  regions: [],
  minBudget: 0,
  maxBudget: 1000,
  autoSync: false,
  syncInterval: 6,
  autoSave: true,
  enableDedupe: true,
  businessUnit: '',
})

export const DEFAULT_FETCH_RESULT = Object.freeze({
  visible: false,
  saved: 0,
  skipped: 0,
  message: '',
})

export function createManualTenderForm() {
  return {
    title: '',
    budget: null,
    region: '',
    tenderAgency: '',
    bidOpeningTime: null,
    customerType: '',
    priority: '',
    deadline: null,
    purchaser: '',
    contact: '',
    phone: '',
    description: '',
    tags: [],
    attachments: [],
    sourceDocumentName: '',
    sourceDocumentFileType: '',
    sourceDocumentFileUrl: '',
    pastedText: '',
    // 项目评估表字段（第二步）
    projectBackground: '',
    competitorAnalysis: '',
    contractPeriodStart: null,
    contractPeriodEnd: null,
    shortlistedCount: null,
    platformServiceFee: null,
    previousQuotation: null,
    bidRecommendation: '',
  }
}

export const MANUAL_FORM_RULES = {
  title: [{ required: true, message: '请输入标讯标题', trigger: 'blur' }],
  tenderAgency: [{ required: true, message: '请输入招标机构', trigger: 'blur' }],
  purchaser: [{ required: true, message: '请输入业主单位', trigger: 'blur' }],
  region: [{ required: true, message: '请选择总部所在地', trigger: 'change' }],
  deadline: [{ required: true, message: '请选择报名截止时间', trigger: 'change' }],
  bidOpeningTime: [{ required: true, message: '请选择开标时间', trigger: 'change' }],
  contact: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系方式', trigger: 'blur' }],
  customerType: [{ required: true, message: '请选择客户类型', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
}
