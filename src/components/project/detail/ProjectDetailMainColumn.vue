<template>
  <el-main class="main-content">
    <ProjectBasicInfoCard :project="ctx.project" />
    <ProjectApprovalStatusCard :approval-history="ctx.approvalHistory" :project-status="ctx.project?.status" :can-approve-current="ctx.canApproveCurrent" @quick-approve="ctx.handleQuickApprove" @quick-reject="ctx.handleQuickReject" />
    <ProjectExpenseSummaryCard :expenses="ctx.projectExpenses" :summary="ctx.expenseSummary" :loading="ctx.expenseLoading" :error="ctx.expenseError" @manage="ctx.goToExpensePage" />
    <ProjectTaskBoardCard :tasks="ctx.project?.tasks || []" :project-id="ctx.project?.id" :can-manage-project-tasks="ctx.canManageProjectTasks" :is-demo-mode="ctx.isDemoMode" @add-task="ctx.handleAddTask" @reset-tasks="ctx.handleResetTasks" @task-click="ctx.handleTaskClick" @generate-tasks="ctx.handleGenerateTasks" @add-deliverable="ctx.handleAddDeliverable" @remove-deliverable="ctx.handleRemoveDeliverable" @submit-to-document="ctx.handleSubmitToDocument" />
    <ProjectDetailWorkflowCard />
    <el-card class="document-card"><template #header><div class="card-title"><el-icon><Folder /></el-icon><span>项目文档</span><el-button v-if="ctx.canManageProjectDocuments" link type="success" :icon="DocumentChecked" @click="ctx.handleArchiveDocuments">归档资料</el-button><el-upload v-if="ctx.canManageProjectDocuments" :show-file-list="false" :before-upload="ctx.handleUpload" accept=".doc,.docx,.pdf,.xls,.xlsx"><el-button link type="primary" :icon="Upload">上传文档</el-button></el-upload></div></template><el-table :data="ctx.project?.documents || []" style="width: 100%"><el-table-column prop="name" label="文档名称" min-width="200"><template #default="{ row }"><div class="file-name"><el-icon><Document /></el-icon><span>{{ row.name }}</span></div></template></el-table-column><el-table-column prop="uploader" label="上传者" width="120" /><el-table-column prop="time" label="上传时间" width="160" /><el-table-column prop="size" label="文件大小" width="100" /><el-table-column label="操作" width="120"><template #default="{ row }"><el-button link type="primary" @click="ctx.handleDownload(row)">下载</el-button><el-button link type="danger" @click="ctx.handleDeleteDoc(row)">删除</el-button></template></el-table-column></el-table><el-empty v-if="!ctx.project?.documents?.length" description="暂无文档" /></el-card>
  </el-main>
</template>

<script setup>
import { Document, DocumentChecked, Folder, Upload } from '@element-plus/icons-vue'
import { useProjectDetailContext } from '@/composables/projectDetail/context.js'
import ProjectApprovalStatusCard from '@/components/project/ProjectApprovalStatusCard.vue'
import ProjectBasicInfoCard from '@/components/project/ProjectBasicInfoCard.vue'
import ProjectExpenseSummaryCard from '@/components/project/ProjectExpenseSummaryCard.vue'
import ProjectTaskBoardCard from '@/components/project/ProjectTaskBoardCard.vue'
import ProjectDetailWorkflowCard from '@/components/project/detail/ProjectDetailWorkflowCard.vue'

const ctx = useProjectDetailContext()
</script>
