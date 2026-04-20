<template>
  <div class="project-detail-feature-modals">
    <CompetitionIntel v-model="detail.showCompetitionIntel.value" :project-id="detail.route.params.id" />
    <ComplianceCheck v-model="detail.showComplianceCheck.value" :project-id="detail.route.params.id" />
    <VersionControl v-model="detail.showVersionControl.value" :project-id="detail.dialogProjectId.value" />
    <CollaborationCenter v-model="detail.showCollaboration.value" :project-id="detail.dialogProjectId.value" />
    <ROIAnalysis v-model="detail.showROIAnalysis.value" :project-id="detail.route.params.id" />
    <AutoTasks v-if="detail.isDemoMode" v-model="detail.showAutoTasks.value" :project-id="detail.route.params.id" :data="detail.demoAutoTasks.value" />
    <MobileCard v-if="detail.isDemoMode" v-model="detail.showMobileCard.value" :project-id="detail.route.params.id" :data="detail.demoMobileCard.value" />
    <ScoreDraftDialog v-model:visible="detail.scoreDraftDialogVisible.value" :project-id="detail.route.params.id" @generated="detail.handleScoreDraftGenerated" />
    <ApprovalDialog v-model:visible="detail.approvalDialogVisible.value" :mode="detail.approvalMode.value" :project-id="detail.project.value?.id" :project-name="detail.project.value?.name" :approval-type="detail.approvalType.value" :approval-info="detail.currentApprovalItem.value" @success="detail.handleApprovalSuccess" />
  </div>
</template>

<script setup>
import { inject } from 'vue'
import CollaborationCenter from '@/components/ai/CollaborationCenter.vue'
import CompetitionIntel from '@/components/ai/CompetitionIntel.vue'
import ComplianceCheck from '@/components/ai/ComplianceCheck.vue'
import MobileCard from '@/components/ai/MobileCard.vue'
import ROIAnalysis from '@/components/ai/ROIAnalysis.vue'
import AutoTasks from '@/components/ai/AutoTasks.vue'
import VersionControl from '@/components/ai/VersionControl.vue'
import ApprovalDialog from '@/components/common/ApprovalDialog.vue'
import ScoreDraftDialog from '@/components/project/ScoreDraftDialog.vue'
import { projectDetailKey } from '@/composables/projectDetail/context.js'

const detail = inject(projectDetailKey)
</script>
