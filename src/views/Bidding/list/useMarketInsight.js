// Input: TrendRadar API functions
// Output: market insight dialog state and refresh actions
// Pos: src/views/Bidding/list/ - Market insight composable
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  generateForecastTips,
  generateInsight,
  getBreakoutTopics,
  getStatsSummary,
  transformToIndustryTrends,
  transformToOpportunities,
} from '@/api/trendradar'

const POLITICS_KEYWORDS = ['空袭', '袭击', '战争', '东部战区', '导弹', '俄乌', '巴以', '哈马斯', '以色列', '伊朗']

const DEFAULT_INDUSTRY_TRENDS = [
  { industry: '劳保安全', count: 413, amount: 37600, growth: 42, trend: 'up', hotLevel: 5, color: 'red' },
  { industry: '工控低压', count: 333, amount: 57600, growth: 30, trend: 'up', hotLevel: 5, color: 'blue' },
  { industry: '电工照明', count: 410, amount: 36300, growth: 24, trend: 'up', hotLevel: 4, color: 'yellow' },
  { industry: '制冷暖通', count: 223, amount: 53300, growth: 25, trend: 'up', hotLevel: 4, color: 'cyan' },
]

const DEFAULT_OPPORTUNITIES = [
  { id: 'base-1', title: '制造业工厂劳保用品年度采购', purchaser: '制造业客户', budget: 680, region: '华东', priority: 'high', match: 95, reason: '需求稳定，产品线匹配度高。' },
  { id: 'base-2', title: '变电站检修工具采购', purchaser: '能源客户', budget: 520, region: '华北', priority: 'high', match: 92, reason: '检修场景明确，工具耗材需求集中。' },
]

export function useMarketInsight() {
  const showMarketInsight = ref(false)
  const activeInsightTab = ref('industry')
  const loadingTrendData = ref(false)
  const trendDataLoaded = ref(false)
  const industryTrends = ref([...DEFAULT_INDUSTRY_TRENDS])
  const potentialOpportunities = ref([...DEFAULT_OPPORTUNITIES])
  const industryInsight = ref('劳保安全、工控低压、电工照明保持高热度，建议优先关注华东和华北区域。')
  const forecastTips = ref([
    { text: '劳保安全类产品预计 Q2 需求旺盛，建议提前备货。', color: '#67c23a' },
    { text: '工控低压类产品在新能源行业需求强劲，建议重点跟进。', color: '#409eff' },
  ])

  const loadTrendRadarData = async () => {
    if (trendDataLoaded.value) return
    loadingTrendData.value = true
    try {
      const [topics, stats] = await Promise.all([getBreakoutTopics(50, 2), getStatsSummary()])
      const filteredTopics = (topics || []).filter((topic) => {
        const text = `${topic.normalized_title || ''} ${(topic.sample_titles || []).join(' ')}`.toLowerCase()
        return !POLITICS_KEYWORDS.some((keyword) => text.includes(keyword.toLowerCase()))
      })

      if (filteredTopics.length > 0) {
        const transformed = transformToIndustryTrends(filteredTopics)
        const opportunities = transformToOpportunities(filteredTopics)
        if (transformed.length > 0) industryTrends.value = transformed
        if (opportunities.length > 0) potentialOpportunities.value = opportunities
        industryInsight.value = generateInsight(filteredTopics, stats)
        forecastTips.value = generateForecastTips(filteredTopics)
        ElMessage.success(`已从 TrendRadar 加载 ${filteredTopics.length} 条趋势数据`)
      } else {
        ElMessage.info('当前实时热点暂无工业相关内容，已展示基准市场洞察')
      }
      trendDataLoaded.value = true
    } catch {
      trendDataLoaded.value = true
    } finally {
      loadingTrendData.value = false
    }
  }

  const refreshTrendData = async () => {
    trendDataLoaded.value = false
    await loadTrendRadarData()
  }

  watch(showMarketInsight, (visible) => {
    if (visible) loadTrendRadarData()
  })

  return {
    showMarketInsight,
    activeInsightTab,
    loadingTrendData,
    industryTrends,
    potentialOpportunities,
    industryInsight,
    forecastTips,
    refreshTrendData,
  }
}
