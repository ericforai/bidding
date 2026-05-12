<template>
  <el-card class="tender-evaluation-form" shadow="never">
    <template #header>
      <div class="evaluation-card-header">
        <h3 class="evaluation-title">项目评估</h3>
      </div>
    </template>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="140px"
      :disabled="isReadOnly"
    >
      <el-form-item label="项目背景" prop="projectBackground" required>
        <el-input
          v-model="form.projectBackground"
          type="textarea"
          :rows="4"
          placeholder="请填写项目背景"
          maxlength="5000"
          :readonly="isReadOnly"
        />
      </el-form-item>

      <el-form-item label="竞争对手情况" prop="competitorAnalysis" required>
        <el-input
          v-model="form.competitorAnalysis"
          type="textarea"
          :rows="4"
          placeholder="请填写竞争对手情况"
          maxlength="5000"
          :readonly="isReadOnly"
        />
      </el-form-item>

      <el-form-item label="项目合同周期" required>
        <div class="contract-period-row">
          <el-form-item prop="contractPeriodStart" class="contract-period-item">
            <el-date-picker
              v-model="form.contractPeriodStart"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="开始日期"
              aria-label="开始日期"
              :disabled="isReadOnly"
            />
          </el-form-item>
          <span class="contract-period-sep">至</span>
          <el-form-item prop="contractPeriodEnd" class="contract-period-item">
            <el-date-picker
              v-model="form.contractPeriodEnd"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="结束日期"
              aria-label="结束日期"
              :disabled="isReadOnly"
            />
          </el-form-item>
        </div>
      </el-form-item>

      <el-form-item label="入围家数" prop="shortlistedCount" required>
        <!-- C5: backend policy rejects 0; min must be 1 client-side too. -->
        <el-input-number
          v-model="form.shortlistedCount"
          :min="1"
          :precision="0"
          :disabled="isReadOnly"
        />
      </el-form-item>

      <el-form-item label="平台服务费" prop="platformServiceFee" required>
        <el-input-number
          v-model="form.platformServiceFee"
          :min="0"
          :precision="2"
          :disabled="isReadOnly"
        />
      </el-form-item>

      <el-form-item label="上一次报价情况" prop="previousQuotation">
        <!-- M3: align frontend maxlength with backend @Size(max=5000). -->
        <el-input
          v-model="form.previousQuotation"
          type="textarea"
          :rows="3"
          placeholder="请填写上一次报价情况（如无可留空）"
          maxlength="5000"
          :readonly="isReadOnly"
        />
      </el-form-item>

      <!-- C4: bidRecommendation is optional per backend policy. -->
      <el-form-item label="建议是否投标" prop="bidRecommendation">
        <el-select
          v-model="form.bidRecommendation"
          placeholder="请选择（可选）"
          clearable
          :disabled="isReadOnly"
        >
          <el-option label="建议投标" value="RECOMMEND" />
          <el-option label="不建议投标" value="NOT_RECOMMEND" />
        </el-select>
      </el-form-item>
    </el-form>

    <div class="evaluation-actions">
      <template v-if="showDraftSubmitButtons">
        <el-button @click="handleSaveDraft">保存草稿</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </template>
      <template v-if="showDecisionButtons">
        <el-button type="primary" @click="handleBid">投标</el-button>
        <el-button type="danger" @click="handleAbandon">弃标</el-button>
      </template>
    </div>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { useTenderEvaluationForm } from './useTenderEvaluationForm.js'

const props = defineProps({
  evaluation: { type: Object, default: null },
  // Instance-level permission booleans — backend computes these on the
  // evaluation DTO based on the current user's relationship to the tender:
  //   canFill   ← user is latest assignee
  //   canDecide ← user is latest assigned-by
  canFill: { type: Boolean, default: false },
  canDecide: { type: Boolean, default: false },
  tenderId: { type: Number, required: true },
})

const emit = defineEmits(['submit', 'save-draft', 'bid', 'abandon'])

const formRef = ref(null)

const {
  form,
  rules,
  isReadOnly,
  showDraftSubmitButtons,
  showDecisionButtons,
  handleSubmit,
  handleSaveDraft,
  handleBid,
  handleAbandon,
} = useTenderEvaluationForm(props, emit)
</script>

<style scoped>
.tender-evaluation-form {
  margin-top: 16px;
}

.evaluation-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.evaluation-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.contract-period-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.contract-period-item {
  margin-bottom: 0;
  flex: 1;
}

.contract-period-sep {
  color: var(--el-text-color-secondary);
}

.evaluation-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
</style>
