<template>
  <div class="collaboration-container">
    <div class="page-header">
      <h2>协作讨论</h2>
    </div>

    <el-select v-model="projectId" placeholder="选择项目" @change="loadThreads" style="margin-bottom: 20px; width: 300px;">
      <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
    </el-select>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="讨论列表" name="threads">
        <div v-if="threads.length === 0 && projectId" class="empty-tip">暂无讨论</div>
        <div v-else-if="!projectId" class="empty-tip">请先选择项目</div>
        <div v-else>
          <el-card v-for="thread in threads" :key="thread.id" class="thread-card" @click="selectThread(thread)">
            <div class="thread-header">
              <span class="thread-title">{{ thread.title }}</span>
              <el-tag :type="statusType(thread.status)" size="small">{{ thread.status }}</el-tag>
            </div>
            <div class="thread-meta">
              <span>{{ thread.commentCount || 0 }} 条评论</span>
              <span>{{ formatTime(thread.createdAt) }}</span>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="讨论详情" name="detail" v-if="selectedThread">
        <el-card>
          <div class="thread-detail">
            <h3>{{ selectedThread.title }}</h3>
            <p class="thread-content">{{ selectedThread.content }}</p>
            <div class="thread-info">
              <span>创建人: {{ selectedThread.createdByName }}</span>
              <span>{{ formatTime(selectedThread.createdAt) }}</span>
            </div>
          </div>
        </el-card>

        <div class="comments-section">
          <h4>评论</h4>
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-header">
              <strong>{{ comment.createdByName }}</strong>
              <span>{{ formatTime(comment.createdAt) }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
          </div>

          <el-input v-model="newComment" type="textarea" :rows="3" placeholder="添加评论..." style="margin-top: 16px;" />
          <el-button type="primary" @click="addComment" style="margin-top: 8px;">发送评论</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { collaborationApi } from '@/api/modules/collaboration.js'
import { useProjectStore } from '@/stores/project.js'

const projectId = ref(null)
const projects = ref([])
const threads = ref([])
const comments = ref([])
const selectedThread = ref(null)
const activeTab = ref('threads')
const newComment = ref('')

onMounted(() => {
  const projectStore = useProjectStore()
  projects.value = projectStore.projects || []
  if (projects.value.length > 0) {
    projectId.value = projects.value[0].id
    loadThreads()
  }
})

async function loadThreads() {
  if (!projectId.value) return
  try {
    const res = await collaborationApi.getThreads(projectId.value)
    threads.value = res.data || []
  } catch (e) {
    ElMessage.error('加载讨论失败')
  }
}

async function selectThread(thread) {
  selectedThread.value = thread
  try {
    const res = await collaborationApi.getThreadDetail(thread.id)
    comments.value = res.data?.comments || []
  } catch (e) {
    comments.value = []
  }
  activeTab.value = 'detail'
}

async function addComment() {
  if (!newComment.value.trim()) return
  try {
    await collaborationApi.createComment(selectedThread.value.id, { content: newComment.value })
    ElMessage.success('评论已发送')
    newComment.value = ''
    selectThread(selectedThread.value)
  } catch (e) {
    ElMessage.error('发送失败')
  }
}

function statusType(s) {
  const map = { OPEN: 'primary', RESOLVED: 'success', CLOSED: 'info' }
  return map[s] || 'info'
}

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}
</script>

<style scoped>
.collaboration-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.empty-tip { color: #909399; text-align: center; padding: 40px; }
.thread-card { margin-bottom: 12px; cursor: pointer; }
.thread-card:hover { border-color: #409eff; }
.thread-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.thread-title { font-weight: bold; }
.thread-meta { font-size: 12px; color: #909399; display: flex; gap: 16px; }
.thread-detail h3 { margin: 0 0 12px 0; }
.thread-content { color: #606266; margin-bottom: 12px; }
.thread-info { font-size: 12px; color: #909399; display: flex; gap: 16px; }
.comments-section { margin-top: 20px; }
.comment-item { padding: 12px; border-bottom: 1px solid #eee; }
.comment-header { display: flex; gap: 12px; margin-bottom: 8px; font-size: 14px; }
.comment-content { color: #606266; }
</style>
