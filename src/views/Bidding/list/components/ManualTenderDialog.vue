<template>
  <el-dialog
    v-model="modelValue"
    title="人工录入标讯"
    width="860px"
    @close="handleClose"
  >
    <!-- 步骤指示器 -->
    <el-steps :active="currentStep" align-center finish-status="success" class="manual-steps">
      <el-step title="基本信息" description="填写标讯基础信息" />
      <el-step title="项目评估表" description="填写项目评估信息" />
    </el-steps>

    <div class="step-content">
      <!-- 第一步：基本信息 -->
      <div v-show="currentStep === 0">
        <el-form ref="innerFormRef" :model="form" :rules="rules" label-width="100px">
          <el-row :gutter="16">
            <el-col :span="24">
              <el-form-item label="标讯标题" prop="title">
                <el-input v-model="form.title" placeholder="请输入招标项目标题" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预算金额（元）" prop="budget">
                <el-input-number
                  v-model="form.budget"
                  :min="0"
                  :precision="2"
                  placeholder="采购预算/最高限价，单位元；框架协议可留空"
                  class="full-width"
                />
                <div class="field-tip">采购预算/最高限价，单位元；框架协议可留空</div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="总部所在地" prop="region">
                <el-select v-model="form.region" placeholder="选择总部所在地" class="full-width">
                  <el-option v-for="region in regions" :key="region" :label="region" :value="region" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="招标机构" prop="tenderAgency">
                <el-input v-model="form.tenderAgency" placeholder="招标代理/招标机构名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="业主单位" prop="purchaser">
                <el-input v-model="form.purchaser" placeholder="招标人/采购人名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="报名截止时间" prop="deadline">
                <el-date-picker v-model="form.deadline" type="datetime" placeholder="选择报名截止时间" class="full-width" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="开标时间" prop="bidOpeningTime">
                <el-date-picker v-model="form.bidOpeningTime" type="datetime" placeholder="选择开标时间" class="full-width" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="客户类型" prop="customerType">
                <el-select v-model="form.customerType" placeholder="选择客户类型" class="full-width">
                  <el-option v-for="type in customerTypes" :key="type" :label="type" :value="type" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="优先级" prop="priority">
                <el-select v-model="form.priority" placeholder="选择优先级" class="full-width">
                  <el-option v-for="item in priorities" :key="item.value" :label="item.label" :value="item.value">
                    <div class="priority-option">
                      <span>{{ item.label }} · {{ item.desc }}</span>
                      <small>{{ item.standard }}</small>
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系人" prop="contact">
                <el-input v-model="form.contact" placeholder="联系人姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系方式" prop="phone">
                <el-input v-model="form.phone" placeholder="手机号/座机/邮箱" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="项目描述">
                <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入项目详细描述" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="标签">
                <el-select v-model="form.tags" multiple allow-create filterable placeholder="添加标签" class="full-width">
                  <el-option label="公开招标" value="公开招标" />
                  <el-option label="邀请招标" value="邀请招标" />
                  <el-option label="竞争性谈判" value="竞争性谈判" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="粘贴识别">
                <div class="paste-recognition-hint">粘贴或输入标讯正文，系统将智能拆分回填字段</div>
                <el-input
                  v-model="form.pastedText"
                  type="textarea"
                  :rows="4"
                  maxlength="500000"
                  show-word-limit
                  placeholder="直接粘贴招标公告正文，系统将自动识别并回填字段"
                  :disabled="parsingDocument"
                />
                <div class="paste-actions">
                  <el-button
                    type="primary"
                    :icon="DocumentCopy"
                    :loading="parsingDocument"
                    @click="$emit('parse-pasted-text')"
                  >
                    识别粘贴文字
                  </el-button>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="附件上传">
                <div class="upload-hint">
                  支持 PDF/Word 文件上传（≤50MB），上传后自动 AI 解析并回填表单字段
                </div>
                <el-upload
                  class="manual-tender-upload"
                  :auto-upload="false"
                  :on-change="onFileChange"
                  :file-list="form.attachments"
                  :limit="5"
                  :accept="acceptFileTypes"
                  :on-exceed="onFileExceed"
                  multiple
                  drag
                >
                  <el-icon class="el-icon--upload"><Upload /></el-icon>
                  <div class="el-upload__text">
                    {{ parsingDocument ? 'DeepSeek/AI 解析中...' : '将文件拖到此处，或点击选择附件（PDF/Word ≤50MB）' }}
                  </div>
                </el-upload>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- 第二步：项目评估表 -->
      <div v-show="currentStep === 1">
        <EvaluationFormFields v-model="form" />
      </div>
    </div>

    <template #footer>
      <el-button @click="handleCancel">{{ currentStep === 0 ? '取消' : '取消' }}</el-button>
      <el-button v-if="currentStep === 1" @click="prevStep">上一步</el-button>
      <el-button v-if="currentStep === 0" type="primary" @click="nextStep">
        下一步
      </el-button>
      <el-button v-if="currentStep === 1" type="primary" :loading="saving" @click="$emit('submit')">
        保存入库
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import EvaluationFormFields from './EvaluationFormFields.vue'
import { DocumentCopy, Upload } from '@element-plus/icons-vue'
import './manual-tender-step.css'
import {
  CUSTOMER_TYPE_OPTIONS,
  MANUAL_FORM_RULES,
  PRIORITY_OPTIONS,
  REGION_OPTIONS,
} from '../constants.js'

const ACCEPT_FILE_TYPES = '.pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document'

const modelValue = defineModel({ type: Boolean, default: false })
const form = defineModel('form', { type: Object, required: true })
const props = defineProps({
  rules: { type: Object, default: () => MANUAL_FORM_RULES },
  saving: { type: Boolean, default: false },
  parsingDocument: { type: Boolean, default: false },
  regions: { type: Array, default: () => REGION_OPTIONS },
  customerTypes: { type: Array, default: () => CUSTOMER_TYPE_OPTIONS },
  priorities: { type: Array, default: () => PRIORITY_OPTIONS },
})

const emit = defineEmits(['reset', 'submit', 'file-change', 'parse-pasted-text'])

const currentStep = ref(0)
const innerFormRef = ref(null)
const acceptFileTypes = ACCEPT_FILE_TYPES

const onFileChange = (file, fileList) => emit('file-change', file, fileList)
const onFileExceed = () => {}

async function nextStep() {
  if (currentStep.value === 0) {
    try {
      await innerFormRef.value?.validate()
      currentStep.value = 1
    } catch {
      // 校验不通过，保持当前步骤
    }
  }
}

function prevStep() {
  if (currentStep.value > 0) {
    currentStep.value = 0
  }
}

function handleCancel() {
  currentStep.value = 0
  modelValue.value = false
  emit('reset')
}

function handleClose() {
  currentStep.value = 0
  emit('reset')
}

defineExpose({
  validate: () => innerFormRef.value?.validate(),
  currentStep,
})
</script>

<style scoped>
@import url(./manual-tender-step.css);
</style>
