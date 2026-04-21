<template>
  <section class="customer-list-panel">
    <div class="panel-header search-integrated">
      <div class="panel-title">
        <el-icon class="title-icon"><User /></el-icon>
        <h3>客户池</h3>
      </div>
      <div class="header-filters multi-row">
        <div class="filter-row">
          <el-input
            :model-value="filters.keyword"
            placeholder="搜索名称..."
            clearable
            size="default"
            :disabled="!demoEnabled"
            class="search-input"
            @update:model-value="updateFilter('keyword', $event)"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select
            :model-value="filters.sales"
            placeholder="销售负责人"
            size="default"
            clearable
            :disabled="!demoEnabled"
            class="filter-item"
            @update:model-value="updateFilter('sales', $event)"
          >
            <el-option label="全部销售" value="" />
            <el-option v-for="user in salesUsers" :key="user.id" :label="user.name" :value="user.name" />
          </el-select>
        </div>
        <div class="filter-row">
          <el-select
            :model-value="filters.region"
            placeholder="全部地区"
            size="default"
            clearable
            :disabled="!demoEnabled"
            class="filter-item"
            @update:model-value="updateFilter('region', $event)"
          >
            <el-option v-for="region in regions" :key="region" :label="region" :value="region" />
          </el-select>
          <el-select
            :model-value="filters.industry"
            placeholder="全部行业"
            size="default"
            clearable
            :disabled="!demoEnabled"
            class="filter-item"
            @update:model-value="updateFilter('industry', $event)"
          >
            <el-option v-for="item in industries" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select
            :model-value="filters.status"
            placeholder="全部分类"
            size="default"
            clearable
            :disabled="!demoEnabled"
            class="filter-item"
            @update:model-value="updateFilter('status', $event)"
          >
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
      </div>
    </div>

    <el-skeleton :loading="loading" animated :rows="10">
      <el-table
        :data="filteredCustomers"
        size="default"
        row-key="customerId"
        :row-class-name="rowClass"
        class="premium-table"
        @row-click="$emit('select-customer', $event)"
      >
        <template #empty>
          <el-empty :description="demoEnabled ? '暂无符合条件的客户' : '客户商机数据源未接入'" />
        </template>
        <el-table-column prop="customerName" label="客户名称" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="customer-name-cell">
              <strong>{{ row.customerName }}</strong>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="region" label="地区" width="100" show-overflow-tooltip />
        <el-table-column prop="industry" label="行业" width="100" show-overflow-tooltip />
        <el-table-column prop="salesRep" label="销售负责人" width="140" show-overflow-tooltip />
        <el-table-column prop="opportunityScore" label="机会评分" width="110" align="center">
          <template #default="{ row }">
            <div class="score-container">
              <span class="score-num" :class="getScoreClass(row.opportunityScore)">{{ row.opportunityScore }}</span>
              <el-progress
                :percentage="row.opportunityScore"
                :show-text="false"
                :stroke-width="4"
                :color="getScoreColor(row.opportunityScore)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="predictedNextWindow" label="预测窗口" width="140" align="center">
          <template #default="{ row }">
            <span class="window-tag">{{ row.predictedNextWindow }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-skeleton>
  </section>
</template>

<script setup>
import { Search, User } from '@element-plus/icons-vue'
import { getScoreClass, getScoreColor } from '@/views/Bidding/customerOpportunityView.js'

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  demoEnabled: {
    type: Boolean,
    default: false
  },
  filters: {
    type: Object,
    default: () => ({})
  },
  salesUsers: {
    type: Array,
    default: () => []
  },
  regions: {
    type: Array,
    default: () => []
  },
  industries: {
    type: Array,
    default: () => []
  },
  statusOptions: {
    type: Array,
    default: () => []
  },
  filteredCustomers: {
    type: Array,
    default: () => []
  },
  activeCustomerId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:filters', 'select-customer'])

const updateFilter = (key, value) => {
  emit('update:filters', {
    ...props.filters,
    [key]: value || ''
  })
}

const rowClass = ({ row }) => (row.customerId === props.activeCustomerId ? 'row-active' : '')
</script>
