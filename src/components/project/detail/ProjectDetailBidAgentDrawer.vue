<template>
  <el-drawer v-model="visible" title="AI 生成初稿" size="560px" append-to-body>
    <div class="bid-agent-drawer">
      <section class="agent-hero">
        <div>
          <p class="agent-eyebrow">Bid Writing Agent</p>
          <h3>从项目资料生成可审阅标书初稿</h3>
          <p>基于真实后端运行结果展示章节、来源、置信度与需人工确认项。</p>
        </div>
        <el-tag :type="statusType" effect="dark">{{ statusText }}</el-tag>
      </section>

      <el-alert v-if="agent.error.value" :title="agent.error.value" type="error" show-icon :closable="false" />

      <div class="agent-actions">
        <el-button type="primary" :loading="agent.creating.value" @click="agent.createRun()">AI 生成初稿</el-button>
        <el-button :disabled="!agent.currentRunId.value" :loading="agent.fetching.value" @click="agent.fetchRun()">刷新状态</el-button>
      </div>

      <el-empty v-if="!run" description="尚未启动 AI 初稿生成" :image-size="96" />

      <template v-else>
        <section class="agent-section">
          <header>运行阶段</header>
          <ol class="stage-list">
            <li v-for="stage in displayStages" :key="stage.key || stage.title" :class="stageClass(stage.status)">
              <span class="stage-dot" />
              <div>
                <strong>{{ stage.title }}</strong>
                <p>{{ stage.message || getStageText(stage.status) }}</p>
              </div>
            </li>
          </ol>
        </section>

        <section v-if="warnings.length" class="agent-section">
          <header>风险与人工确认</header>
          <el-alert v-for="warning in warnings" :key="warning" :title="warning" type="warning" show-icon :closable="false" />
        </section>

        <section class="agent-section">
          <header>初稿章节</header>
          <div v-if="draftSections.length" class="draft-list">
            <article v-for="section in draftSections" :key="section.id || section.title" class="draft-card">
              <div class="draft-title-row">
                <h4>{{ section.title }}</h4>
                <el-tag v-if="section.confidence !== null && section.confidence !== undefined" size="small" type="success">{{ section.confidence }}%</el-tag>
              </div>
              <p>{{ section.content || '后端暂未返回章节正文预览' }}</p>
              <small v-if="section.source">来源：{{ section.source }}</small>
            </article>
          </div>
          <el-empty v-else description="等待后端返回初稿章节" :image-size="72" />
        </section>

        <section v-if="agent.applyResult.value" class="apply-result">
          <strong>写入结果</strong>
          <span>{{ applyResultText }}</span>
        </section>
      </template>

      <div class="drawer-footer">
        <el-button :disabled="!canReview" :loading="agent.reviewing.value" @click="agent.createReview()">发起审查</el-button>
        <el-button type="primary" :disabled="!canApply" :loading="agent.applying.value" @click="agent.applyBidAgentResult()">写入文档编辑器</el-button>
        <el-button v-if="agent.applyResult.value" type="success" plain @click="agent.goToEditor()">打开文档编辑器</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed } from 'vue'
import { useProjectDetailContext } from '@/composables/projectDetail/context.js'

const detail = useProjectDetailContext()
const agent = detail.bidAgent

const visible = computed({
  get: () => agent.drawerVisible.value,
  set: (value) => { agent.drawerVisible.value = value },
})

const run = computed(() => agent.currentRun.value)
const status = computed(() => String(run.value?.status || '').toUpperCase())
const draftSections = computed(() => run.value?.draft?.sections || [])
const isReady = computed(() => ['COMPLETED', 'READY', 'DONE', 'APPLIED'].includes(status.value))
const canApply = computed(() => Boolean(agent.currentRunId.value) && (isReady.value || draftSections.value.length > 0))
const canReview = computed(() => Boolean(agent.currentRunId.value) && !['FAILED', 'ERROR'].includes(status.value))

const displayStages = computed(() => {
  if (run.value?.stages?.length) return run.value.stages
  return status.value ? [{ key: 'current', title: '当前状态', status: status.value }] : []
})

const warnings = computed(() => [
  ...formatWarnings(run.value?.risks),
  ...formatWarnings(run.value?.gaps),
  ...formatWarnings(run.value?.manualConfirmations),
])

const statusText = computed(() => ({
  QUEUED: '排队中',
  PENDING: '待处理',
  RUNNING: '生成中',
  COMPLETED: '已完成',
  READY: '可写入',
  APPLIED: '已写入',
  FAILED: '失败',
  ERROR: '失败',
}[status.value] || '未启动'))

const statusType = computed(() => ({
  COMPLETED: 'success',
  READY: 'success',
  APPLIED: 'success',
  RUNNING: 'primary',
  QUEUED: 'info',
  PENDING: 'info',
  FAILED: 'danger',
  ERROR: 'danger',
}[status.value] || 'info'))

const applyResultText = computed(() => {
  const result = agent.applyResult.value
  if (!result) return ''
  if (result.message) return result.message
  if (result.documentName) return `已写入 ${result.documentName}`
  if (result.documentId) return `已写入文档 #${result.documentId}`
  return '后端已确认写入结果'
})

function formatWarnings(items = []) {
  return items.map((item) => {
    if (typeof item === 'string') return item
    return item.title || item.message || item.description || item.name || ''
  }).filter(Boolean)
}

function getStageText(stageStatus = '') {
  return ({
    QUEUED: '等待后端调度',
    PENDING: '等待处理',
    RUNNING: '正在生成',
    COMPLETED: '已完成',
    FAILED: '处理失败',
  }[String(stageStatus).toUpperCase()] || '等待后端状态')
}

function stageClass(stageStatus = '') {
  return `stage-${String(stageStatus).toLowerCase() || 'pending'}`
}
</script>

<style scoped>
.bid-agent-drawer {
  display: grid;
  gap: 18px;
}

.agent-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border: 1px solid #d8e6df;
  border-radius: 18px;
  background: linear-gradient(135deg, #f5fbf7 0%, #eef6ee 55%, #fff8eb 100%);
}

.agent-eyebrow {
  margin: 0 0 6px;
  color: #5b7f64;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .08em;
  text-transform: uppercase;
}

.agent-hero h3 {
  margin: 0 0 8px;
  color: #173d2a;
}

.agent-hero p,
.stage-list p,
.draft-card p {
  margin: 0;
  color: #5c6f65;
  line-height: 1.6;
}

.agent-actions,
.drawer-footer,
.draft-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.agent-section header {
  margin-bottom: 10px;
  color: #21372b;
  font-weight: 700;
}

.stage-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.stage-list li {
  display: grid;
  grid-template-columns: 14px 1fr;
  gap: 10px;
  padding: 12px;
  border-radius: 14px;
  background: #f7faf8;
}

.stage-dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 50%;
  background: #9ca3af;
}

.stage-completed .stage-dot,
.stage-ready .stage-dot,
.stage-applied .stage-dot {
  background: #13a46b;
}

.stage-running .stage-dot {
  background: #2f80ed;
}

.stage-failed .stage-dot,
.stage-error .stage-dot {
  background: #d64545;
}

.draft-list {
  display: grid;
  gap: 12px;
}

.draft-card {
  padding: 14px;
  border: 1px solid #e5ece8;
  border-radius: 16px;
  background: #fff;
}

.draft-title-row {
  justify-content: space-between;
  margin-bottom: 8px;
}

.draft-title-row h4 {
  margin: 0;
  color: #24382d;
}

.draft-card small {
  display: inline-block;
  margin-top: 10px;
  color: #789083;
}

.apply-result {
  display: grid;
  gap: 4px;
  padding: 12px;
  border-radius: 14px;
  background: #effaf3;
  color: #23613f;
}

.drawer-footer {
  justify-content: flex-end;
}
</style>
