<template>
  <SmartAssistantPanel v-model:visible="ctx.assistantPanelVisible" :project-id="ctx.dialogProjectId" :show-demo-features="ctx.isDemoMode" @open-competition-intel="ctx.handleOpenCompetitionIntel" @open-roi-analysis="ctx.handleOpenRoiAnalysis" @open-score-coverage="ctx.handleOpenScoreCoverage" @open-compliance-check="ctx.handleOpenComplianceCheck" @open-version-control="ctx.handleOpenVersionControl" @open-collaboration="ctx.handleOpenCollaboration" @open-auto-tasks="ctx.handleOpenAutoTasks" @open-mobile-card="ctx.handleOpenMobileCard" />
  <CompetitionIntel v-model="ctx.showCompetitionIntel" :project-id="ctx.route.params.id" />
  <ComplianceCheck v-model="ctx.showComplianceCheck" :project-id="ctx.route.params.id" />
</template>

<script setup>
import { useProjectDetailContext } from '@/composables/projectDetail/context.js'
import CompetitionIntel from '@/components/ai/CompetitionIntel.vue'
import ComplianceCheck from '@/components/ai/ComplianceCheck.vue'
import SmartAssistantPanel from '@/components/ai/SmartAssistantPanel.vue'

const ctx = useProjectDetailContext()
</script>
