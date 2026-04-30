<!-- Input: WeCom + OA 配置卡片面板，统一挂在系统集成 Tab -->
<!-- Output: 系统设置中企业微信与泛微 OA 配置入口（OA 使用系统设置 integrationConfig 持久化） -->
<!-- Pos: src/views/System/settings/ -->

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

      <WeaverIntegrationCard
        :form="oaForm"
        :secret-configured="oaSecretConfigured"
        :loading="oaLoading"
        :saving="oaSaving"
        @save="saveOaConfig"
      />

      <IntegrationComingSoonCard title="CRM 系统" />
      <IntegrationComingSoonCard title="组织架构系统" />
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useWeComSettings } from './useWeComSettings.js'
import WeComIntegrationCard from './integration/WeComIntegrationCard.vue'
import WeaverIntegrationCard from './integration/WeaverIntegrationCard.vue'
import IntegrationComingSoonCard from './integration/IntegrationComingSoonCard.vue'
import { useSystemIntegrationSettings } from './useSystemIntegrationSettings.js'

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

const {
  loading: oaLoading,
  saving: oaSaving,
  form: oaForm,
  secretConfigured: oaSecretConfigured,
  load: loadSystemIntegration,
  save: saveOaConfig,
} = useSystemIntegrationSettings()

onMounted(() => {
  load()
  loadSystemIntegration()
})
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
