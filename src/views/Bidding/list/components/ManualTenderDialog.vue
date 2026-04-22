<template>
  <el-dialog
    :model-value="modelValue"
    title="人工录入标讯"
    width="720px"
    @update:model-value="$emit('update:modelValue', $event)"
    @close="$emit('reset')"
  >
    <el-form ref="innerFormRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form-item label="标讯标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入招标项目标题" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="预算金额" prop="budget">
            <el-input-number v-model="form.budget" :min="0" :precision="2" class="full-width" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="地区" prop="region">
            <el-select v-model="form.region" placeholder="选择地区" class="full-width">
              <el-option v-for="region in regions" :key="region" :label="region" :value="region" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="行业分类" prop="industry">
            <el-select v-model="form.industry" placeholder="选择行业" class="full-width">
              <el-option v-for="industry in industries" :key="industry" :label="industry" :value="industry" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="截止日期" prop="deadline">
            <el-date-picker v-model="form.deadline" type="date" placeholder="选择截止日期" class="full-width" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="采购单位">
            <el-input v-model="form.purchaser" placeholder="采购单位名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系人">
            <el-input v-model="form.contact" placeholder="联系人姓名" />
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
          <el-form-item label="附件">
            <el-upload :auto-upload="false" :on-change="onFileChange" :file-list="form.attachments" :limit="5" multiple drag>
              <el-icon class="el-icon--upload"><Upload /></el-icon>
              <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            </el-upload>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="$emit('submit')">保存入库</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { INDUSTRY_OPTIONS, MANUAL_FORM_RULES, REGION_OPTIONS } from '../constants.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  form: { type: Object, required: true },
  rules: { type: Object, default: () => MANUAL_FORM_RULES },
  saving: { type: Boolean, default: false },
  regions: { type: Array, default: () => REGION_OPTIONS },
  industries: { type: Array, default: () => INDUSTRY_OPTIONS },
})

const emit = defineEmits(['update:modelValue', 'reset', 'submit', 'file-change'])

const innerFormRef = ref(null)

const onFileChange = (file, fileList) => emit('file-change', file, fileList)

defineExpose({
  validate: () => innerFormRef.value?.validate(),
})
</script>
