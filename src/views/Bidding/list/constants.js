// Input: none
// Output: Bidding list page option constants and default form factories
// Pos: src/views/Bidding/list/ - Local constants for the bidding list page
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

export const REGION_OPTIONS = ['北京', '上海', '广州', '深圳', '成都', '其他']

export const INDUSTRY_OPTIONS = ['政府', '能源', '交通', '数据中心', '金融', '医疗', '教育']

export const SOURCE_OPTIONS = [
  { label: '内部', value: 'internal' },
  { label: '外部获取', value: 'external' },
  { label: '人工录入', value: 'manual' },
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
  { value: 'region', label: '按区域分发', desc: '根据标讯地区自动分配' },
  { value: 'industry', label: '按行业分发', desc: '根据行业类型分配' },
  { value: 'score', label: '按 AI 评分', desc: '高分优先给候选人' },
  { value: 'average', label: '平均分配', desc: '均匀分配给可选人员' },
]

export const DEFAULT_SEARCH_FORM = Object.freeze({
  keyword: '',
  region: '',
  industry: '',
  status: '',
  source: '',
})

export const DEFAULT_SOURCE_CONFIG = Object.freeze({
  platforms: ['中国政府采购网'],
  apiEndpoint: '',
  apiKey: '',
  keywords: [],
  regions: ['北京', '上海', '广州', '深圳'],
  minBudget: 0,
  maxBudget: 1000,
  autoSync: false,
  syncInterval: 6,
  autoSave: true,
  enableDedupe: true,
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
    industry: '',
    deadline: null,
    purchaser: '',
    contact: '',
    phone: '',
    description: '',
    tags: [],
    attachments: [],
  }
}

export const MANUAL_FORM_RULES = {
  title: [{ required: true, message: '请输入标讯标题', trigger: 'blur' }],
  budget: [{ required: true, message: '请输入预算金额', trigger: 'blur' }],
  region: [{ required: true, message: '请选择地区', trigger: 'change' }],
  industry: [{ required: true, message: '请选择行业', trigger: 'change' }],
  deadline: [{ required: true, message: '请选择截止日期', trigger: 'change' }],
}
