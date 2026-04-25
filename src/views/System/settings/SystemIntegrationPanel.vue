<!-- Input: useWeComSettings composable wired to WeComIntegrationCard + coming-soon cards -->
<!-- Output: System Integration panel with 4 integration cards in a vertical stack -->
<!-- Pos: src/views/System/settings/ -->
<!-- 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。 -->

<template>
  <div class="system-integration-panel">
    <div class="panel-toolbar">
      <div>
        <p class="panel-kicker">System Integration</p>
        <h2>系统集成</h2>
      </div>
    </div>

    <div class="integration-stack">
      <WeComIntegrationCard
        :form="form"
        :secret-configured="secretConfigured"
        :test-result="testResult"
        :loading="loading"
        :saving="saving"
        :testing="testing"
        @save="save"
        @test-conn="testConn"
      />

      <IntegrationComingSoonCard title="CRM 系统" />
      <IntegrationComingSoonCard title="OA / 审批流" />
      <IntegrationComingSoonCard title="组织架构系统" />
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useWeComSettings } from './useWeComSettings.js'
import WeComIntegrationCard from './integration/WeComIntegrationCard.vue'
import IntegrationComingSoonCard from './integration/IntegrationComingSoonCard.vue'

const {
  loading,
  saving,
  testing,
  form,
  secretConfigured,
  testResult,
  load,
  save,
  testConn,
} = useWeComSettings()

onMounted(load)
</script>

<style scoped>
.system-integration-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel-toolbar {
  padding: 6px 2px 10px;
  border-bottom: 1px solid rgba(67, 89, 55, 0.1);
}

.panel-kicker {
  margin: 0 0 6px;
  color: #6d7d5d;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.panel-toolbar h2 {
  margin: 0;
  color: #1f2d1d;
}

.integration-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
