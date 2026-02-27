<template>
  <div class="settings-page">
    <el-card>
      <template #header>
        <span>系统设置</span>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="用户管理" name="user">
          <div class="tab-header">
            <el-button type="primary">
              <el-icon><Plus /></el-icon> 添加用户
            </el-button>
          </div>
          <el-table :data="users" stripe>
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="role" label="角色" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.role === 'admin'" type="danger">管理员</el-tag>
                <el-tag v-else-if="row.role === 'manager'" type="warning">经理</el-tag>
                <el-tag v-else>普通员工</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="dept" label="部门" />
            <el-table-column label="状态" width="100">
              <template #default>
                <el-tag type="success">启用</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" size="small">编辑</el-button>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="角色权限" name="role">
          <div class="tab-header">
            <el-button type="primary">
              <el-icon><Plus /></el-icon> 添加角色
            </el-button>
          </div>
          <el-table :data="roles" stripe>
            <el-table-column prop="name" label="角色名称" width="150" />
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="userCount" label="用户数" width="100" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" size="small">权限配置</el-button>
                <el-button link type="primary" size="small">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="系统配置" name="config">
          <el-form label-width="150px" style="max-width: 600px">
            <el-form-item label="系统名称">
              <el-input v-model="config.sysName" />
            </el-form-item>
            <el-form-item label="投标保证金提醒">
              <el-input-number v-model="config.depositWarnDays" :min="1" :max="30" />
              <span class="unit">天前</span>
            </el-form-item>
            <el-form-item label="资质到期提醒">
              <el-input-number v-model="config.qualWarnDays" :min="1" :max="180" />
              <span class="unit">天前</span>
            </el-form-item>
            <el-form-item label="启用AI功能">
              <el-switch v-model="config.enableAI" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveConfig">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="日志管理" name="log">
          <el-table :data="logs" stripe>
            <el-table-column prop="time" label="时间" width="180" />
            <el-table-column prop="user" label="用户" width="100" />
            <el-table-column prop="action" label="操作" width="150" />
            <el-table-column prop="detail" label="详情" min-width="200" />
            <el-table-column prop="ip" label="IP地址" width="140" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="系统集成" name="integration">
          <div class="integration-container">
            <!-- 组织架构集成 -->
            <el-card class="integration-card">
              <template #header>
                <div class="card-header">
                  <span>组织架构集成</span>
                  <el-switch
                    v-model="integrationConfig.orgEnabled"
                    @change="handleOrgSync"
                    :active-text="integrationConfig.orgEnabled ? '已启用' : ''"
                  />
                </div>
              </template>
              <el-form label-width="120px">
                <el-form-item label="集成系统">
                  <el-select
                    v-model="integrationConfig.orgSystem"
                    placeholder="请选择集成系统"
                    :disabled="!integrationConfig.orgEnabled"
                  >
                    <el-option label="钉钉" value="dingtalk" />
                    <el-option label="企业微信" value="wecom" />
                    <el-option label="飞书" value="feishu" />
                  </el-select>
                </el-form-item>
                <el-form-item label="AppKey">
                  <el-input
                    v-model="integrationConfig.orgAppKey"
                    placeholder="请输入AppKey"
                    :disabled="!integrationConfig.orgEnabled"
                  />
                </el-form-item>
                <el-form-item label="AppSecret">
                  <el-input
                    v-model="integrationConfig.orgAppSecret"
                    type="password"
                    placeholder="请输入AppSecret"
                    show-password
                    :disabled="!integrationConfig.orgEnabled"
                  />
                </el-form-item>
                <el-form-item label="同步状态">
                  <div class="sync-status">
                    <el-tag :type="syncStatus.type">{{ syncStatus.text }}</el-tag>
                    <span v-if="syncStatus.lastSyncTime" class="sync-time">
                      上次同步: {{ syncStatus.lastSyncTime }}
                    </span>
                  </div>
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    :disabled="!integrationConfig.orgEnabled"
                    :loading="testingConnection"
                    @click="testOrgConnection"
                  >
                    测试连接
                  </el-button>
                  <el-button
                    :disabled="!integrationConfig.orgEnabled"
                    :loading="syncingData"
                    @click="syncOrgData"
                  >
                    同步数据
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <!-- OA审批集成 -->
            <el-card class="integration-card">
              <template #header>
                <div class="card-header">
                  <span>OA审批流集成</span>
                  <el-switch
                    v-model="integrationConfig.oaEnabled"
                    :active-text="integrationConfig.oaEnabled ? '已启用' : ''"
                  />
                </div>
              </template>
              <el-form label-width="120px">
                <el-form-item label="OA系统地址">
                  <el-input
                    v-model="integrationConfig.oaUrl"
                    placeholder="http://oa.xxx.com"
                    :disabled="!integrationConfig.oaEnabled"
                  />
                </el-form-item>
                <el-form-item label="单点登录">
                  <el-switch
                    v-model="integrationConfig.ssoEnabled"
                    :disabled="!integrationConfig.oaEnabled"
                    active-text="启用SSO"
                  />
                </el-form-item>
                <el-form-item label="审批回调URL">
                  <el-input
                    v-model="integrationConfig.callbackUrl"
                    placeholder="审批完成后的回调地址"
                    :disabled="!integrationConfig.oaEnabled"
                  />
                </el-form-item>
                <el-form-item label="审批流程映射">
                  <el-table
                    :data="integrationConfig.flowMapping"
                    size="small"
                    :disabled="!integrationConfig.oaEnabled"
                    class="mapping-table"
                  >
                    <el-table-column prop="systemFlow" label="系统流程" width="150" />
                    <el-table-column prop="oaFlow" label="OA流程" width="150" />
                    <el-table-column prop="description" label="说明" />
                    <el-table-column label="操作" width="120" fixed="right">
                      <template #default="{ row }">
                        <el-button
                          link
                          type="primary"
                          size="small"
                          :disabled="!integrationConfig.oaEnabled"
                          @click="editFlowMapping(row)"
                        >
                          配置
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    :disabled="!integrationConfig.oaEnabled"
                    @click="saveOaConfig"
                  >
                    保存OA配置
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <!-- API接口 -->
            <el-card class="integration-card">
              <template #header>
                <div class="card-header">
                  <span>开放API接口</span>
                  <div>
                    <el-button size="small" @click="generateApiKey">
                      <el-icon><Key /></el-icon> 生成API密钥
                    </el-button>
                    <el-button size="small" type="primary" @click="showApiDoc = true">
                      <el-icon><Document /></el-icon> 查看API文档
                    </el-button>
                  </div>
                </div>
              </template>
              <div class="api-key-section">
                <el-form label-width="120px">
                  <el-form-item label="API密钥">
                    <el-input
                      v-model="integrationConfig.apiKey"
                      readonly
                      style="max-width: 500px"
                    >
                      <template #append>
                        <el-button @click="copyApiKey">复制</el-button>
                      </template>
                    </el-input>
                  </el-form-item>
                  <el-form-item label="IP白名单">
                    <el-input
                      v-model="integrationConfig.ipWhitelist"
                      type="textarea"
                      :rows="2"
                      placeholder="每行一个IP地址，如: 192.168.1.100"
                    />
                  </el-form-item>
                </el-form>
              </div>
              <el-table :data="apiList" size="small" class="api-table">
                <el-table-column prop="name" label="接口名称" width="180" />
                <el-table-column prop="path" label="接口路径" min-width="200" />
                <el-table-column prop="method" label="请求方法" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getMethodType(row.method)">{{ row.method }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="说明" min-width="150" />
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 'enabled' ? 'success' : 'info'">
                      {{ row.status === 'enabled' ? '已启用' : '未启用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" fixed="right">
                  <template #default="{ row }">
                    <el-switch
                      v-model="row.enabled"
                      @change="toggleApiStatus(row)"
                      size="small"
                    />
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- API文档对话框 -->
    <el-dialog
      v-model="showApiDoc"
      title="API接口文档"
      width="900px"
      :close-on-click-modal="false"
    >
      <el-tabs v-model="apiDocTab" type="border-card">
        <el-tab-pane label="概述" name="overview">
          <div class="api-doc-content">
            <h3>接口说明</h3>
            <p>本系统提供RESTful API接口，支持第三方系统集成。</p>

            <h3>认证方式</h3>
            <el-alert type="info" :closable="false">
              <p>所有接口需要在HTTP Header中携带API密钥进行认证：</p>
              <code>Authorization: Bearer YOUR_API_KEY</code>
            </el-alert>

            <h3>Base URL</h3>
            <el-input
              :model-value="baseUrl"
              readonly
              class="base-url-input"
            >
              <template #append>
                <el-button @click="copyBaseUrl">复制</el-button>
              </template>
            </el-input>

            <h3>响应格式</h3>
            <pre class="code-block"><code>{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2025-02-26T14:30:00Z"
}</code></pre>
          </div>
        </el-tab-pane>

        <el-tab-pane label="项目接口" name="project">
          <div class="api-doc-content">
            <div v-for="api in projectApis" :key="api.name" class="api-item">
              <div class="api-header">
                <el-tag :type="getMethodType(api.method)">{{ api.method }}</el-tag>
                <code class="api-path">{{ api.path }}</code>
              </div>
              <h4>{{ api.name }}</h4>
              <p>{{ api.description }}</p>
              <div v-if="api.params" class="api-params">
                <h5>请求参数:</h5>
                <el-table :data="api.params" size="small" border>
                  <el-table-column prop="name" label="参数名" width="120" />
                  <el-table-column prop="type" label="类型" width="100" />
                  <el-table-column prop="required" label="必填" width="80">
                    <template #default="{ row }">
                      <el-tag :type="row.required ? 'danger' : 'info'" size="small">
                        {{ row.required ? '是' : '否' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="description" label="说明" />
                </el-table>
              </div>
              <div v-if="api.example" class="api-example">
                <h5>示例:</h5>
                <pre class="code-block"><code>{{ api.example }}</code></pre>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="审批接口" name="approval">
          <div class="api-doc-content">
            <div v-for="api in approvalApis" :key="api.name" class="api-item">
              <div class="api-header">
                <el-tag :type="getMethodType(api.method)">{{ api.method }}</el-tag>
                <code class="api-path">{{ api.path }}</code>
              </div>
              <h4>{{ api.name }}</h4>
              <p>{{ api.description }}</p>
              <div v-if="api.params" class="api-params">
                <h5>请求参数:</h5>
                <el-table :data="api.params" size="small" border>
                  <el-table-column prop="name" label="参数名" width="120" />
                  <el-table-column prop="type" label="类型" width="100" />
                  <el-table-column prop="required" label="必填" width="80">
                    <template #default="{ row }">
                      <el-tag :type="row.required ? 'danger' : 'info'" size="small">
                        {{ row.required ? '是' : '否' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="description" label="说明" />
                </el-table>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="回调接口" name="callback">
          <div class="api-doc-content">
            <el-alert type="warning" :closable="false" style="margin-bottom: 20px">
              <p>回调接口由第三方系统主动调用，需要在系统中配置正确的回调地址。</p>
            </el-alert>
            <div v-for="api in callbackApis" :key="api.name" class="api-item">
              <div class="api-header">
                <el-tag :type="getMethodType(api.method)">{{ api.method }}</el-tag>
                <code class="api-path">{{ api.path }}</code>
              </div>
              <h4>{{ api.name }}</h4>
              <p>{{ api.description }}</p>
              <div v-if="api.params" class="api-params">
                <h5>请求参数:</h5>
                <el-table :data="api.params" size="small" border>
                  <el-table-column prop="name" label="参数名" width="120" />
                  <el-table-column prop="type" label="类型" width="100" />
                  <el-table-column prop="required" label="必填" width="80">
                    <template #default="{ row }">
                      <el-tag :type="row.required ? 'danger' : 'info'" size="small">
                        {{ row.required ? '是' : '否' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="description" label="说明" />
                </el-table>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="showApiDoc = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 流程映射配置对话框 -->
    <el-dialog
      v-model="showFlowMappingDialog"
      title="配置审批流程映射"
      width="600px"
    >
      <el-form label-width="120px">
        <el-form-item label="系统流程">
          <el-input v-model="currentFlowMapping.systemFlow" readonly />
        </el-form-item>
        <el-form-item label="OA流程编码">
          <el-input v-model="currentFlowMapping.oaFlowCode" placeholder="请输入OA系统中的流程编码" />
        </el-form-item>
        <el-form-item label="OA流程名称">
          <el-input v-model="currentFlowMapping.oaFlowName" placeholder="请输入OA系统中的流程名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFlowMappingDialog = false">取消</el-button>
        <el-button type="primary" @click="saveFlowMapping">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Key, Document } from '@element-plus/icons-vue'
import { mockData } from '@/api/mock'

const activeTab = ref('user')
const users = ref(mockData.users)

const roles = ref([
  { name: '管理员', description: '系统管理员，拥有所有权限', userCount: 1 },
  { name: '经理', description: '部门经理，可查看报表和审批', userCount: 1 },
  { name: '销售', description: '销售人员，可创建项目和查看数据', userCount: 5 },
  { name: '技术人员', description: '技术人员，可参与项目任务', userCount: 10 }
])

const config = ref({
  sysName: '西域数智化投标管理平台',
  depositWarnDays: 7,
  qualWarnDays: 30,
  enableAI: true
})

const logs = ref([
  { time: '2025-02-26 14:30:25', user: '小王', action: '登录系统', detail: '用户登录', ip: '192.168.1.100' },
  { time: '2025-02-26 14:28:10', user: '张经理', action: '审批项目', detail: '审批通过项目P001', ip: '192.168.1.101' },
  { time: '2025-02-26 14:25:33', user: '李工', action: '上传文档', detail: '上传技术方案', ip: '192.168.1.102' },
  { time: '2025-02-26 14:20:15', user: '小王', action: '创建项目', detail: '创建投标项目', ip: '192.168.1.100' },
  { time: '2025-02-26 14:15:08', user: '李总', action: '查看报表', detail: '查看数据分析', ip: '192.168.1.103' }
])

// 系统集成配置
const integrationConfig = ref({
  // 组织架构集成
  orgEnabled: false,
  orgSystem: 'dingtalk',
  orgAppKey: '',
  orgAppSecret: '',
  // OA集成
  oaEnabled: false,
  oaUrl: '',
  ssoEnabled: false,
  callbackUrl: '',
  // API配置
  apiKey: 'sk_xiyu_bid_' + Math.random().toString(36).substring(2, 15),
  ipWhitelist: ''
})

// 同步状态
const syncStatus = ref({
  type: 'info',
  text: '未同步',
  lastSyncTime: ''
})

const testingConnection = ref(false)
const syncingData = ref(false)

// 审批流程映射
const flowMappings = ref([
  { systemFlow: '项目立项审批', oaFlow: 'project_start', oaFlowCode: 'FLOW_001', oaFlowName: '项目立项流程', description: '新建项目时的审批流程' },
  { systemFlow: '投标审批', oaFlow: 'bidding_approval', oaFlowCode: 'FLOW_002', oaFlowName: '投标审批流程', description: '投标前的审批流程' },
  { systemFlow: '合同审批', oaFlow: 'contract_approval', oaFlowCode: 'FLOW_003', oaFlowName: '合同审批流程', description: '合同签署的审批流程' },
  { systemFlow: '用印申请', oaFlow: 'seal_application', oaFlowCode: 'FLOW_004', oaFlowName: '用印申请流程', description: '公章使用申请流程' }
])

integrationConfig.value.flowMapping = flowMappings.value

// 流程映射对话框
const showFlowMappingDialog = ref(false)
const currentFlowMapping = ref({
  systemFlow: '',
  oaFlowCode: '',
  oaFlowName: ''
})

// API列表
const apiList = ref([
  { name: '获取项目列表', path: '/api/v1/projects', method: 'GET', description: '查询投标项目列表', status: 'enabled', enabled: true },
  { name: '创建项目', path: '/api/v1/projects', method: 'POST', description: '创建新的投标项目', status: 'enabled', enabled: true },
  { name: '获取项目详情', path: '/api/v1/projects/{id}', method: 'GET', description: '获取指定项目详情', status: 'enabled', enabled: true },
  { name: '更新项目', path: '/api/v1/projects/{id}', method: 'PUT', description: '更新项目信息', status: 'enabled', enabled: true },
  { name: '提交审批', path: '/api/v1/approvals/submit', method: 'POST', description: '提交审批申请', status: 'enabled', enabled: true },
  { name: '查询审批状态', path: '/api/v1/approvals/{id}/status', method: 'GET', description: '查询审批进度', status: 'enabled', enabled: true },
  { name: '同步组织架构', path: '/api/v1/sync/org', method: 'POST', description: '同步组织架构数据', status: 'enabled', enabled: true },
  { name: '获取资质列表', path: '/api/v1/qualifications', method: 'GET', description: '查询企业资质信息', status: 'enabled', enabled: true },
  { name: '上传附件', path: '/api/v1/files/upload', method: 'POST', description: '上传项目附件', status: 'enabled', enabled: true }
])

// API文档
const showApiDoc = ref(false)
const apiDocTab = ref('overview')
const baseUrl = computed(() => {
  return window.location.origin + '/api/v1'
})

// 项目接口文档
const projectApis = [
  {
    name: '获取项目列表',
    path: 'GET /api/v1/projects',
    method: 'GET',
    description: '分页查询投标项目列表，支持按状态、日期等条件筛选',
    params: [
      { name: 'page', type: 'number', required: true, description: '页码，从1开始' },
      { name: 'pageSize', type: 'number', required: true, description: '每页数量，最大100' },
      { name: 'status', type: 'string', required: false, description: '项目状态：draft/submitting/approved/rejected/completed' },
      { name: 'startDate', type: 'string', required: false, description: '开始日期，格式：YYYY-MM-DD' },
      { name: 'endDate', type: 'string', required: false, description: '结束日期，格式：YYYY-MM-DD' }
    ],
    example: `GET /api/v1/projects?page=1&pageSize=20&status=approved`
  },
  {
    name: '创建项目',
    path: 'POST /api/v1/projects',
    method: 'POST',
    description: '创建新的投标项目',
    params: [
      { name: 'name', type: 'string', required: true, description: '项目名称' },
      { name: 'customerId', type: 'string', required: true, description: '客户ID' },
      { name: 'bidAmount', type: 'number', required: true, description: '投标金额（元）' },
      { name: 'bidDeadline', type: 'string', required: true, description: '投标截止时间，格式：YYYY-MM-DD HH:mm:ss' },
      { name: 'description', type: 'string', required: false, description: '项目描述' }
    ],
    example: `POST /api/v1/projects
{
  "name": "某某系统集成项目",
  "customerId": "CUST_001",
  "bidAmount": 500000,
  "bidDeadline": "2025-03-15 17:00:00",
  "description": "企业ERP系统升级改造项目"
}`
  },
  {
    name: '获取项目详情',
    path: 'GET /api/v1/projects/{id}',
    method: 'GET',
    description: '获取指定项目的详细信息',
    params: [
      { name: 'id', type: 'string', required: true, description: '项目ID' }
    ],
    example: `GET /api/v1/projects/P001`
  },
  {
    name: '更新项目',
    path: 'PUT /api/v1/projects/{id}',
    method: 'PUT',
    description: '更新项目信息',
    params: [
      { name: 'id', type: 'string', required: true, description: '项目ID（路径参数）' },
      { name: 'name', type: 'string', required: false, description: '项目名称' },
      { name: 'bidAmount', type: 'number', required: false, description: '投标金额' },
      { name: 'status', type: 'string', required: false, description: '项目状态' }
    ],
    example: `PUT /api/v1/projects/P001
{
  "bidAmount": 550000,
  "status": "submitting"
}`
  }
]

// 审批接口文档
const approvalApis = [
  {
    name: '提交审批',
    path: 'POST /api/v1/approvals/submit',
    method: 'POST',
    description: '提交审批申请，支持项目立项、投标、合同等类型',
    params: [
      { name: 'type', type: 'string', required: true, description: '审批类型：project_start/bidding/contract/seal' },
      { name: 'businessId', type: 'string', required: true, description: '业务ID' },
      { name: 'title', type: 'string', required: true, description: '审批标题' },
      { name: 'content', type: 'string', required: true, description: '审批内容描述' },
      { name: 'approvers', type: 'array', required: true, description: '审批人ID列表' }
    ],
    example: `POST /api/v1/approvals/submit
{
  "type": "project_start",
  "businessId": "P001",
  "title": "某某项目立项审批",
  "content": "项目概况、预算、计划等说明",
  "approvers": ["USER_001", "USER_002"]
}`
  },
  {
    name: '查询审批状态',
    path: 'GET /api/v1/approvals/{id}/status',
    method: 'GET',
    description: '查询审批流程的当前状态和进度',
    params: [
      { name: 'id', type: 'string', required: true, description: '审批ID' }
    ],
    example: `GET /api/v1/approvals/APR_001/status`
  },
  {
    name: '审批操作',
    path: 'POST /api/v1/approvals/{id}/action',
    method: 'POST',
    description: '对审批进行通过、驳回等操作',
    params: [
      { name: 'id', type: 'string', required: true, description: '审批ID（路径参数）' },
      { name: 'action', type: 'string', required: true, description: '操作类型：approve/reject/transfer' },
      { name: 'comment', type: 'string', required: false, description: '审批意见' },
      { name: 'toUser', type: 'string', required: false, description: '转交用户ID（action为transfer时必填）' }
    ],
    example: `POST /api/v1/approvals/APR_001/action
{
  "action": "approve",
  "comment": "同意，请财务复核"
}`
  }
]

// 回调接口文档
const callbackApis = [
  {
    name: 'OA审批回调',
    path: 'POST /api/v1/callback/oa/approval',
    method: 'POST',
    description: 'OA系统审批完成后回调通知本系统',
    params: [
      { name: 'approvalId', type: 'string', required: true, description: '本系统审批ID' },
      { name: 'oaFlowId', type: 'string', required: true, description: 'OA系统流程ID' },
      { name: 'status', type: 'string', required: true, description: '审批结果：approved/rejected' },
      { name: 'approver', type: 'string', required: true, description: '最后审批人' },
      { name: 'comment', type: 'string', required: false, description: '审批意见' },
      { name: 'timestamp', type: 'string', required: true, description: '审批时间戳' }
    ],
    example: `POST /api/v1/callback/oa/approval
{
  "approvalId": "APR_001",
  "oaFlowId": "OA_FLOW_12345",
  "status": "approved",
  "approver": "张经理",
  "comment": "同意该项目立项",
  "timestamp": "2025-02-26T14:30:00Z"
}`
  },
  {
    name: '组织架构同步回调',
    path: 'POST /api/v1/callback/org/sync',
    method: 'POST',
    description: '组织架构系统同步完成后的回调通知',
    params: [
      { name: 'taskId', type: 'string', required: true, description: '同步任务ID' },
      { name: 'status', type: 'string', required: true, description: '同步结果：success/partial/failed' },
      { name: 'syncedCount', type: 'number', required: true, description: '同步成功数量' },
      { name: 'failedCount', type: 'number', required: false, description: '同步失败数量' },
      { name: 'errors', type: 'array', required: false, description: '错误详情列表' }
    ],
    example: `POST /api/v1/callback/org/sync
{
  "taskId": "SYNC_001",
  "status": "success",
  "syncedCount": 150,
  "failedCount": 0
}`
  }
]

// 方法
const handleSaveConfig = () => {
  ElMessage.success('配置已保存')
}

// 获取HTTP方法对应的标签类型
const getMethodType = (method) => {
  const types = {
    'GET': '',
    'POST': 'success',
    'PUT': 'warning',
    'DELETE': 'danger',
    'PATCH': 'info'
  }
  return types[method] || ''
}

// 组织架构集成
const handleOrgSync = (val) => {
  if (val) {
    ElMessage.success('组织架构集成已启用')
  } else {
    ElMessage.info('组织架构集成已禁用')
  }
}

const testOrgConnection = async () => {
  testingConnection.value = true
  // 模拟测试连接
  setTimeout(() => {
    testingConnection.value = false
    ElMessage.success('连接测试成功！')
  }, 1500)
}

const syncOrgData = async () => {
  syncingData.value = true
  // 模拟同步数据
  setTimeout(() => {
    syncingData.value = false
    syncStatus.value = {
      type: 'success',
      text: '同步成功',
      lastSyncTime: new Date().toLocaleString()
    }
    ElMessage.success('组织架构数据同步完成！')
  }, 2000)
}

// OA集成
const saveOaConfig = () => {
  ElMessage.success('OA配置已保存')
}

const editFlowMapping = (row) => {
  currentFlowMapping.value = {
    systemFlow: row.systemFlow,
    oaFlowCode: row.oaFlowCode,
    oaFlowName: row.oaFlowName,
    _index: flowMappings.value.indexOf(row)
  }
  showFlowMappingDialog.value = true
}

const saveFlowMapping = () => {
  const index = currentFlowMapping.value._index
  if (index !== undefined) {
    flowMappings.value[index].oaFlowCode = currentFlowMapping.value.oaFlowCode
    flowMappings.value[index].oaFlowName = currentFlowMapping.value.oaFlowName
    flowMappings.value[index].oaFlow = currentFlowMapping.value.oaFlowCode
  }
  showFlowMappingDialog.value = false
  ElMessage.success('流程映射配置已保存')
}

// API管理
const generateApiKey = () => {
  const newKey = 'sk_xiyu_bid_' + Math.random().toString(36).substring(2, 15) + Date.now().toString(36)
  integrationConfig.value.apiKey = newKey
  ElMessage.success('新API密钥已生成，请妥善保管')
}

const copyApiKey = () => {
  navigator.clipboard.writeText(integrationConfig.value.apiKey)
  ElMessage.success('API密钥已复制到剪贴板')
}

const copyBaseUrl = () => {
  navigator.clipboard.writeText(baseUrl.value)
  ElMessage.success('Base URL已复制到剪贴板')
}

const toggleApiStatus = (row) => {
  row.status = row.enabled ? 'enabled' : 'disabled'
  ElMessage.success(`${row.name} 已${row.enabled ? '启用' : '禁用'}`)
}
</script>

<style scoped lang="scss">
.settings-page {
  padding: 20px;
}

.tab-header {
  margin-bottom: 20px;
}

.unit {
  margin-left: 10px;
  color: #909399;
}

// 系统集成样式
.integration-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.integration-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    > div {
      display: flex;
      gap: 10px;
    }
  }

  .sync-status {
    display: flex;
    align-items: center;
    gap: 15px;

    .sync-time {
      font-size: 12px;
      color: #909399;
    }
  }

  .mapping-table {
    margin-top: 10px;
  }

  .api-key-section {
    margin-bottom: 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid #ebeef5;
  }

  .api-table {
    margin-top: 15px;
  }
}

// API文档对话框样式
.api-doc-content {
  h3, h4, h5 {
    margin-top: 20px;
    margin-bottom: 10px;
    color: #303133;
  }

  h3 {
    font-size: 18px;
    border-bottom: 2px solid #409eff;
    padding-bottom: 8px;
  }

  h4 {
    font-size: 16px;
  }

  h5 {
    font-size: 14px;
    color: #606266;
  }

  p {
    color: #606266;
    line-height: 1.6;
  }

  code {
    background: #f5f7fa;
    padding: 2px 6px;
    border-radius: 3px;
    color: #e74c3c;
    font-family: 'Courier New', monospace;
  }

  .base-url-input {
    margin-top: 10px;
  }

  .code-block {
    background: #282c34;
    color: #abb2bf;
    padding: 15px;
    border-radius: 5px;
    overflow-x: auto;
    font-family: 'Courier New', monospace;
    font-size: 13px;
    line-height: 1.5;
  }

  .api-item {
    padding: 15px 0;
    border-bottom: 1px solid #ebeef5;

    &:last-child {
      border-bottom: none;
    }

    .api-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 10px;

      .api-path {
        background: #f5f7fa;
        padding: 4px 10px;
        border-radius: 4px;
        color: #409eff;
        font-family: 'Courier New', monospace;
      }
    }

    .api-params {
      margin-top: 15px;
      background: #fafafa;
      padding: 10px;
      border-radius: 4px;
    }

    .api-example {
      margin-top: 15px;
    }
  }
}

:deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}

:deep(.el-tabs--border-card) {
  .el-tabs__content {
    padding: 20px;
  }
}

/* 移动端响应式样式 */
@media (max-width: 768px) {
  .settings-page {
    padding: 12px;
  }

  .page-header {
    margin-bottom: 12px;
  }

  .page-title {
    font-size: 20px;
  }

  /* 标签页移动端优化 */
  :deep(.el-tabs__nav-wrap) {
    padding: 0 8px;
  }

  :deep(.el-tabs__item) {
    font-size: 13px;
    padding: 0 10px;
  }

  :deep(.el-tabs__content) {
    padding: 12px;
  }

  /* 表单移动端优化 */
  .settings-form :deep(.el-form-item__label) {
    width: 100% !important;
    text-align: left;
  }

  .settings-form :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }

  .settings-form :deep(.el-input),
  .settings-form :deep(.el-select),
  .settings-form :deep(.el-switch) {
    width: 100% !important;
  }

  /* 设置项移动端优化 */
  .setting-item {
    padding: 12px;
  }

  .setting-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .setting-actions {
    width: 100%;
  }

  .setting-actions .el-button {
    width: 100%;
  }

  /* 对话框移动端优化 */
  :deep(.el-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }

  :deep(.el-dialog__body) {
    padding: 16px;
    max-height: 60vh;
  }

  /* 表格移动端优化 */
  :deep(.el-table) {
    font-size: 12px;
  }

  :deep(.el-table__body-wrapper) {
    overflow-x: auto;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .setting-item {
    min-height: 70px;
  }

  .setting-item:active {
    background: #f5f7fa;
  }

  .el-button {
    min-height: 44px;
  }
}
</style>
