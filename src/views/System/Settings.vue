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
            <el-button type="primary" @click="showAddRoleDialog = true">
              <el-icon><Plus /></el-icon> 添加角色
            </el-button>
          </div>
          <el-table :data="roles" stripe>
            <el-table-column prop="name" label="角色名称" width="150" />
            <el-table-column prop="code" label="角色代码" width="120" />
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="userCount" label="用户数" width="100" />
            <el-table-column prop="dataScope" label="数据范围" width="120">
              <template #default="{ row }">
                <el-tag size="small">{{ getDataScopeText(row.dataScope) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openPermissionConfig(row)">
                  权限配置
                </el-button>
                <el-button link type="primary" size="small">编辑</el-button>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="数据权限" name="dataScope">
          <div class="data-scope-container">
            <el-tabs v-model="dataScopeTab" type="card">
              <!-- 按用户配置 -->
              <el-tab-pane name="user">
                <template #label>
                  <div class="tab-label-with-icon">
                    <el-icon><User /></el-icon>
                    <span>按用户配置</span>
                    <el-badge :value="userDataScope.length" class="tab-badge" />
                  </div>
                </template>
                <el-table :data="userDataScope" size="small" border>
                  <el-table-column prop="userName" label="用户" width="120" />
                  <el-table-column prop="dept" label="部门" width="140" />
                  <el-table-column prop="role" label="角色" width="100">
                    <template #default="{ row }">
                      <el-tag size="small">{{ row.role || '-' }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="dataScope" label="数据权限" width="140">
                    <template #default="{ row }">
                      <el-select v-model="row.dataScope" size="small" @change="handleUserDataScopeChange(row)">
                        <el-option label="全部数据" value="all" />
                        <el-option label="本部门" value="dept" />
                        <el-option label="本部门及下级" value="deptAndSub" />
                        <el-option label="仅本人" value="self" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column prop="allowedProjects" label="可访问项目组" min-width="220">
                    <template #default="{ row }">
                      <el-select v-model="row.allowedProjects" multiple size="small" style="width: 100%" @change="handleUserProjectChange(row)">
                        <el-option label="央企项目组" value="pg1" />
                        <el-option label="政府项目组" value="pg2" />
                        <el-option label="军队项目组" value="pg3" />
                      </el-select>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <!-- 按部门配置 -->
              <el-tab-pane name="dept">
                <template #label>
                  <div class="tab-label-with-icon">
                    <el-icon><OfficeBuilding /></el-icon>
                    <span>按部门配置</span>
                    <el-badge :value="deptDataScope.length" class="tab-badge" />
                  </div>
                </template>
                <el-table :data="deptDataScope" size="small" border>
                  <el-table-column prop="deptName" label="部门" width="150" />
                  <el-table-column prop="dataScope" label="数据权限" width="140">
                    <template #default="{ row }">
                      <el-select v-model="row.dataScope" size="small" @change="handleDeptDataScopeChange(row)">
                        <el-option label="全部数据" value="all" />
                        <el-option label="本部门" value="dept" />
                        <el-option label="本部门及下级" value="deptAndSub" />
                        <el-option label="仅本部门" value="self" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column prop="canViewOtherDepts" label="跨部门访问" width="120">
                    <template #default="{ row }">
                      <el-switch v-model="row.canViewOtherDepts" @change="handleDeptCrossAccessChange(row)" />
                    </template>
                  </el-table-column>
                  <el-table-column prop="allowedDepts" label="可访问部门" min-width="220">
                    <template #default="{ row }">
                      <el-select v-model="row.allowedDepts" multiple size="small" style="width: 100%" :disabled="!row.canViewOtherDepts">
                        <el-option label="华南销售部" value="dept1" />
                        <el-option label="华东销售部" value="dept2" />
                        <el-option label="技术部" value="dept3" />
                        <el-option label="商务部" value="dept4" />
                        <el-option label="投标管理部" value="dept5" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="100">
                    <template #default="{ row }">
                      <el-button type="primary" size="small" @click="persistDeptConfig(row)">保存</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <!-- 按项目组配置 -->
              <el-tab-pane name="project">
                <template #label>
                  <div class="tab-label-with-icon">
                    <el-icon><Folder /></el-icon>
                    <span>按项目组配置</span>
                    <el-badge :value="projectGroupScope.length" class="tab-badge" />
                  </div>
                </template>
                <el-table :data="projectGroupScope" size="small" border>
                  <el-table-column prop="groupName" label="项目组" width="150" />
                  <el-table-column prop="manager" label="负责人" width="100" />
                  <el-table-column prop="memberCount" label="成员数" width="80" align="center" />
                  <el-table-column prop="visibility" label="可见范围" width="140">
                    <template #default="{ row }">
                      <el-select v-model="row.visibility" size="small" @change="handleProjectVisibilityChange(row)">
                        <el-option label="全员可见" value="all" />
                        <el-option label="项目组成员" value="members" />
                        <el-option label="仅负责人" value="manager" />
                        <el-option label="自定义角色" value="custom" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column prop="allowedRoles" label="可访问角色" min-width="240">
                    <template #default="{ row }">
                      <el-select v-model="row.allowedRoles" multiple size="small" style="width: 100%" :disabled="row.visibility === 'manager'">
                        <el-option label="管理员" value="admin" />
                        <el-option label="经理" value="manager" />
                        <el-option label="销售" value="sales" />
                        <el-option label="技术人员" value="tech" />
                        <el-option label="财务" value="finance" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="100">
                    <template #default="{ row }">
                      <el-button type="primary" size="small" @click="persistProjectConfig(row)">保存</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <!-- 权限规则说明 -->
              <el-tab-pane name="rules">
                <template #label>
                  <div class="tab-label-with-icon">
                    <el-icon><InfoFilled /></el-icon>
                    <span>权限规则说明</span>
                  </div>
                </template>
                <el-alert type="info" :closable="false" show-icon>
                  <div class="rules-content">
                    <h4>数据权限优先级（从高到低）</h4>
                    <ol>
                      <li><strong>用户级权限</strong> > 部门级权限 > 角色级权限（默认）</li>
                      <li>当用户同时拥有多种权限配置时，以最高权限为准</li>
                    </ol>
                    <h4 style="margin-top: 16px;">数据范围说明</h4>
                    <ul>
                      <li><strong>全部数据</strong>：可查看系统中所有数据</li>
                      <li><strong>本部门</strong>：仅查看本部门创建的数据</li>
                      <li><strong>本部门及下级</strong>：查看本部门及下级部门的数据</li>
                      <li><strong>仅本人</strong>：只能查看自己创建的数据</li>
                    </ul>
                  </div>
                </el-alert>
              </el-tab-pane>
            </el-tabs>
          </div>
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
              <el-button type="primary" @click="persistBaseConfig">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="审计日志" name="audit">
          <div class="audit-container">
            <!-- 搜索筛选区域 -->
            <el-card class="audit-search-card" shadow="never">
              <el-form :model="auditSearch" inline>
                <el-form-item label="关键字">
                  <el-input
                    v-model="auditSearch.keyword"
                    placeholder="搜索操作内容/对象"
                    clearable
                    :prefix-icon="Search"
                    style="width: 200px"
                  />
                </el-form-item>
                <el-form-item label="操作类型">
                  <el-select v-model="auditSearch.actionType" placeholder="全部" clearable style="width: 130px">
                    <el-option label="登录" value="login" />
                    <el-option label="创建" value="create" />
                    <el-option label="修改" value="update" />
                    <el-option label="删除" value="delete" />
                    <el-option label="审批" value="approve" />
                    <el-option label="导出" value="export" />
                    <el-option label="查看" value="view" />
                  </el-select>
                </el-form-item>
                <el-form-item label="模块">
                  <el-select v-model="auditSearch.module" placeholder="全部" clearable style="width: 130px">
                    <el-option label="项目管理" value="project" />
                    <el-option label="标讯中心" value="bidding" />
                    <el-option label="资质管理" value="qualification" />
                    <el-option label="费用管理" value="expense" />
                    <el-option label="账户管理" value="account" />
                    <el-option label="系统设置" value="system" />
                  </el-select>
                </el-form-item>
                <el-form-item label="操作人">
                  <el-select v-model="auditSearch.operator" placeholder="全部" clearable style="width: 120px">
                    <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.name" />
                  </el-select>
                </el-form-item>
                <el-form-item label="部门">
                  <el-select v-model="auditSearch.department" placeholder="全部" clearable style="width: 130px">
                    <el-option label="销售部" value="销售部" />
                    <el-option label="技术部" value="技术部" />
                    <el-option label="商务部" value="商务部" />
                    <el-option label="管理部" value="管理部" />
                  </el-select>
                </el-form-item>
                <el-form-item label="时间范围">
                  <el-date-picker
                    v-model="auditSearch.dateRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    style="width: 240px"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleAuditSearch">
                    <el-icon><Search /></el-icon> 搜索
                  </el-button>
                  <el-button @click="handleAuditReset">重置</el-button>
                  <el-button type="success" @click="handleExportAudit">
                    <el-icon><Download /></el-icon> 导出
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <!-- 统计概览 -->
            <div class="audit-stats">
              <div class="stat-item">
                <div class="stat-label">今日操作</div>
                <div class="stat-value">{{ todayAuditCount }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">本周操作</div>
                <div class="stat-value">{{ weekAuditCount }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">异常操作</div>
                <div class="stat-value danger">{{ failedAuditCount }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-label">活跃用户</div>
                <div class="stat-value">{{ activeUserCount }}</div>
              </div>
            </div>

            <!-- 审计日志表格 -->
            <el-table :data="filteredAuditLogs" stripe class="audit-table">
              <el-table-column prop="time" label="时间" width="170" sortable />
              <el-table-column prop="operator" label="操作人" width="100">
                <template #default="{ row }">
                  <div class="operator-cell">
                    <span>{{ row.operator }}</span>
                    <el-tag v-if="row.role === 'admin'" size="small" type="warning">管理员</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="department" label="部门" width="100" />
              <el-table-column prop="actionType" label="操作类型" width="90">
                <template #default="{ row }">
                  <el-tag :type="getActionTypeTag(row.actionType)" size="small">
                    {{ getActionTypeLabel(row.actionType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="module" label="模块" width="100">
                <template #default="{ row }">
                  <el-tag size="small" type="info">{{ getModuleLabel(row.module) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="target" label="操作对象" min-width="150">
                <template #default="{ row }">
                  <span class="target-name" @click="handleViewTarget(row)">{{ row.target }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="detail" label="操作详情" min-width="180" />
              <el-table-column prop="ip" label="IP地址" width="160" />
              <el-table-column prop="status" label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">
                    {{ row.status === 'success' ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleViewAuditDetail(row)">
                    详情
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="audit-pagination">
              <el-pagination
                v-model:current-page="auditPagination.page"
                v-model:page-size="auditPagination.pageSize"
                :page-sizes="[20, 50, 100, 200]"
                :total="auditPagination.total"
                layout="total, sizes, prev, pager, next, jumper"
              />
            </div>
          </div>
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
                    @click="persistOaConfig"
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

              <!-- API 层测试 -->
              <div class="api-test-section" style="margin-bottom: 20px; padding: 15px; background: #f5f7fa; border-radius: 8px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                  <strong>🧪 API 层测试</strong>
                  <el-tag size="small">{{ apiMode === 'mock' ? 'Mock 模式' : 'API 模式' }}</el-tag>
                </div>
                <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                  <el-button size="small" type="primary" @click="testApiLayer" :loading="apiTesting">
                    运行测试
                  </el-button>
                  <el-button size="small" @click="clearApiTestResult">
                    清除结果
                  </el-button>
                  <el-select v-model="apiMode" size="small" style="width: 120px">
                    <el-option label="Mock 模式" value="mock" />
                    <el-option label="API 模式" value="api" />
                  </el-select>
                </div>
                <div v-if="apiTestResult.length > 0" style="margin-top: 12px; font-size: 13px;">
                  <div v-for="(item, index) in apiTestResult" :key="index"
                       :style="{ color: item.success ? '#67c23a' : '#f56c6c', marginBottom: '4px' }">
                    {{ item.success ? '✅' : '❌' }} {{ item.name }}: {{ item.message }}
                  </div>
                </div>
              </div>
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
        <el-button type="primary" @click="persistFlowMapping">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置对话框 -->
    <el-dialog v-model="showPermissionDialog" title="权限配置" width="600px" class="permission-dialog">
      <div class="permission-content">
        <div class="permission-header">
          <span>角色：{{ currentRole?.name }}</span>
          <el-tag size="small">{{ currentRole?.code }}</el-tag>
        </div>
        <el-divider />
        <el-tabs v-model="permissionTab" type="border-card">
          <!-- 菜单权限 -->
          <el-tab-pane name="menu">
            <template #label>
              <span><el-icon><Menu /></el-icon> 菜单权限</span>
            </template>
            <el-tree
              ref="menuTreeRef"
              :data="menuPermissions"
              :props="{ children: 'children', label: 'label' }"
              show-checkbox
              node-key="id"
              :default-checked-keys="currentRole?.menuPermissions || []"
            />
          </el-tab-pane>
          <!-- 操作权限 -->
          <el-tab-pane name="action">
            <template #label>
              <span><el-icon><Operation /></el-icon> 操作权限</span>
            </template>
            <el-table :data="actionPermissions" size="small" border>
              <el-table-column type="selection" width="55" />
              <el-table-column prop="module" label="模块" width="120" />
              <el-table-column prop="action" label="操作" />
              <el-table-column prop="description" label="说明" />
            </el-table>
          </el-tab-pane>
          <!-- 数据权限 -->
          <el-tab-pane name="data">
            <template #label>
              <span><el-icon><Lock /></el-icon> 数据权限</span>
            </template>
            <el-form label-width="100px">
              <el-form-item label="数据范围">
                <el-radio-group v-model="dataScopeForm.scope">
                  <el-radio label="all">全部数据</el-radio>
                  <el-radio label="custom">自定义</el-radio>
                  <el-radio label="dept">本部门</el-radio>
                  <el-radio label="deptAndSub">本部门及下级</el-radio>
                  <el-radio label="self">仅本人</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="自定义部门" v-if="dataScopeForm.scope === 'custom'">
                <el-select v-model="dataScopeForm.depts" multiple placeholder="请选择部门" style="width: 100%">
                  <el-option label="华南销售部" value="dept1" />
                  <el-option label="华东销售部" value="dept2" />
                  <el-option label="技术部" value="dept3" />
                  <el-option label="商务部" value="dept4" />
                  <el-option label="投标管理部" value="dept5" />
                </el-select>
              </el-form-item>
              <el-form-item label="项目组权限">
                <el-select v-model="dataScopeForm.projects" multiple placeholder="请选择可访问的项目组" style="width: 100%">
                  <el-option label="央企项目组" value="pg1" />
                  <el-option label="政府项目组" value="pg2" />
                  <el-option label="军队项目组" value="pg3" />
                </el-select>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button @click="showPermissionDialog = false">取消</el-button>
        <el-button type="primary" @click="persistPermissionConfig">保存配置</el-button>
      </template>
    </el-dialog>

    <!-- 添加角色对话框 -->
    <el-dialog v-model="showAddRoleDialog" title="添加角色" width="500px">
      <el-form :model="roleForm" label-width="100px">
        <el-form-item label="角色名称" required>
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色代码" required>
          <el-input v-model="roleForm.code" placeholder="如: sales_manager" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="数据权限">
          <el-select v-model="roleForm.dataScope" placeholder="请选择">
            <el-option label="全部数据" value="all" />
            <el-option label="本部门" value="dept" />
            <el-option label="本人" value="self" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddRoleDialog = false">取消</el-button>
        <el-button type="primary" @click="persistRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Key, Document, Menu, Operation, Lock, User, OfficeBuilding, Folder, InfoFilled, Search, Download } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { API_CONFIG, auditApi, isMockMode, settingsApi } from '@/api'
import { persistRuntimeSettings } from '@/api/modules/settings.js'

const activeTab = ref('user')
const userStore = useUserStore()
const users = computed(() => userStore.users || [])

const roles = ref([
  { code: 'admin', name: '管理员', description: '系统管理员，拥有所有权限', userCount: 1, dataScope: 'all', menuPermissions: ['all'] },
  { code: 'manager', name: '经理', description: '部门经理，可查看报表和审批', userCount: 1, dataScope: 'dept', menuPermissions: ['dashboard', 'project', 'analytics'] },
  { code: 'sales', name: '销售', description: '销售人员，可创建项目和查看数据', userCount: 5, dataScope: 'self', menuPermissions: ['dashboard', 'project', 'bidding'] },
  { code: 'tech', name: '技术人员', description: '技术人员，可参与项目任务', userCount: 10, dataScope: 'self', menuPermissions: ['dashboard', 'project'] }
])

// 权限配置相关
const showPermissionDialog = ref(false)
const showAddRoleDialog = ref(false)
const permissionTab = ref('menu')
const dataScopeTab = ref('user') // 数据权限 Tab 的当前子 tab
const currentRole = ref(null)
const menuTreeRef = ref(null)

// 菜单权限树
const menuPermissions = ref([
  { id: 'dashboard', label: '工作台' },
  { id: 'bidding', label: '标讯中心', children: [
    { id: 'bidding-list', label: '标讯列表' },
    { id: 'bidding-detail', label: '标讯详情' }
  ]},
  { id: 'project', label: '投标项目', children: [
    { id: 'project-list', label: '项目列表' },
    { id: 'project-create', label: '创建项目' },
    { id: 'project-detail', label: '项目详情' }
  ]},
  { id: 'analytics', label: '数据分析', children: [
    { id: 'analytics-dashboard', label: '数据仪表盘' }
  ]},
  { id: 'knowledge', label: '知识库', children: [
    { id: 'knowledge-qualification', label: '资质库' },
    { id: 'knowledge-case', label: '案例库' },
    { id: 'knowledge-template', label: '模板库' }
  ]},
  { id: 'resource', label: '资源管理', children: [
    { id: 'resource-expense', label: '费用管理' },
    { id: 'resource-account', label: '账户管理' },
    { id: 'resource-bar', label: '资产台账' }
  ]}
])

// 操作权限列表
const actionPermissions = ref([
  { module: '项目管理', action: '创建项目', description: '可以创建新的投标项目' },
  { module: '项目管理', action: '编辑项目', description: '可以编辑项目基本信息' },
  { module: '项目管理', action: '删除项目', description: '可以删除投标项目' },
  { module: '项目管理', action: '提交审批', description: '可以提交项目审批' },
  { module: '费用管理', action: '申请费用', description: '可以申请投标费用' },
  { module: '费用管理', action: '审批费用', description: '可以审批费用申请' },
  { module: '费用管理', action: '查看费用', description: '可以查看费用明细' },
  { module: '标讯管理', action: '查看标讯', description: '可以查看标讯信息' },
  { module: '标讯管理', action: '收藏标讯', description: '可以收藏标讯' },
  { module: '数据分析', action: '查看报表', description: '可以查看数据分析报表' },
  { module: '数据分析', action: '导出数据', description: '可以导出分析数据' }
])

// 数据权限表单
const dataScopeForm = ref({
  scope: 'all',
  depts: [],
  projects: []
})

// 用户数据权限 Mock
const userDataScope = ref([
  { userName: '小王', dept: '华南销售部', role: '销售', dataScope: 'dept', allowedProjects: ['pg1', 'pg2'] },
  { userName: '张经理', dept: '投标管理部', role: '经理', dataScope: 'all', allowedProjects: ['pg1', 'pg2', 'pg3'] },
  { userName: '李工', dept: '技术部', role: '技术人员', dataScope: 'self', allowedProjects: ['pg2'] },
  { userName: '王销售', dept: '华东销售部', role: '销售', dataScope: 'dept', allowedProjects: ['pg1'] },
  { userName: '赵财务', dept: '财务部', role: '财务', dataScope: 'dept', allowedProjects: ['pg1', 'pg2', 'pg3'] }
])

// 部门数据权限 Mock
const deptDataScope = ref([
  { deptName: '华南销售部', dataScope: 'dept', canViewOtherDepts: false, allowedDepts: ['dept1'] },
  { deptName: '华东销售部', dataScope: 'dept', canViewOtherDepts: false, allowedDepts: ['dept2'] },
  { deptName: '技术部', dataScope: 'dept', canViewOtherDepts: false, allowedDepts: ['dept3'] },
  { deptName: '投标管理部', dataScope: 'all', canViewOtherDepts: true, allowedDepts: [] }
])

// 项目组数据权限 Mock
const projectGroupScope = ref([
  { groupName: '央企项目组', manager: '张经理', memberCount: 5, visibility: 'members', allowedRoles: ['admin', 'manager', 'sales'] },
  { groupName: '政府项目组', manager: '李经理', memberCount: 3, visibility: 'members', allowedRoles: ['admin', 'manager', 'sales'] },
  { groupName: '军队项目组', manager: '王经理', memberCount: 2, visibility: 'manager', allowedRoles: ['admin', 'manager'] }
])

// 角色表单
const roleForm = ref({
  name: '',
  code: '',
  description: '',
  dataScope: 'all'
})

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

// ========== 审计系统 ==========
// 审计搜索条件
const auditSearch = ref({
  keyword: '',
  actionType: '',
  module: '',
  operator: '',
  department: '',
  dateRange: []
})

// 审计日志数据
const auditLogs = ref([
  // 登录相关
  { id: 1, time: '2026-03-04 09:30:25', operator: '小王', department: '销售部', role: 'sales', actionType: 'login', module: 'system', target: '系统登录', detail: '用户登录系统', ip: '192.168.1.100', status: 'success' },
  { id: 2, time: '2026-03-04 09:28:10', operator: '张经理', department: '管理部', role: 'manager', actionType: 'login', module: 'system', target: '系统登录', detail: '用户登录系统', ip: '192.168.1.101', status: 'success' },
  { id: 3, time: '2026-03-04 09:15:33', operator: '李工', department: '技术部', role: 'staff', actionType: 'login', module: 'system', target: '系统登录', detail: '用户登录系统', ip: '192.168.1.102', status: 'success' },
  // 项目相关
  { id: 4, time: '2026-03-04 10:20:15', operator: '小王', department: '销售部', role: 'sales', actionType: 'create', module: 'project', target: '某央企智慧办公平台采购', detail: '创建投标项目', ip: '192.168.1.100', status: 'success' },
  { id: 5, time: '2026-03-04 11:15:08', operator: '张经理', department: '管理部', role: 'manager', actionType: 'approve', module: 'project', target: '深圳地铁自动化系统', detail: '审批通过投标申请', ip: '192.168.1.101', status: 'success' },
  { id: 6, time: '2026-03-04 14:30:22', operator: '小王', department: '销售部', role: 'sales', actionType: 'update', module: 'project', target: '西部云数据中心建设', detail: '修改项目预算', ip: '192.168.1.100', status: 'success' },
  { id: 7, time: '2026-03-04 15:45:30', operator: '李工', department: '技术部', role: 'staff', actionType: 'delete', module: 'project', target: '测试项目A', detail: '删除项目', ip: '192.168.1.102', status: 'success' },
  // 标讯相关
  { id: 8, time: '2026-03-04 08:50:15', operator: '小王', department: '销售部', role: 'sales', actionType: 'create', module: 'bidding', target: '中国政府采购网-智慧办公项目', detail: '从外部标讯源同步标讯', ip: '192.168.1.100', status: 'success' },
  { id: 9, time: '2026-03-04 09:30:45', operator: '小王', department: '销售部', role: 'sales', actionType: 'update', module: 'bidding', target: '华南电力集团集采项目', detail: '分配标讯给技术部', ip: '192.168.1.100', status: 'success' },
  { id: 10, time: '2026-03-04 10:15:20', operator: '张经理', department: '管理部', role: 'manager', actionType: 'approve', module: 'bidding', target: 'XX区政府数字平台', detail: '审批标讯分发', ip: '192.168.1.101', status: 'success' },
  // 资质相关
  { id: 11, time: '2026-03-04 13:20:10', operator: '李工', department: '技术部', role: 'staff', actionType: 'create', module: 'qualification', target: 'ISO9001质量管理体系认证', detail: '上传资质文件', ip: '192.168.1.102', status: 'success' },
  { id: 12, time: '2026-03-04 14:00:30', operator: '小王', department: '销售部', role: 'sales', actionType: 'view', module: 'qualification', target: '营业执照', detail: '查看资质详情', ip: '192.168.1.100', status: 'success' },
  { id: 13, time: '2026-03-04 16:30:45', operator: '小王', department: '销售部', role: 'sales', actionType: 'update', module: 'qualification', target: 'ISO14001环境管理体系认证', detail: '更新有效期', ip: '192.168.1.100', status: 'success' },
  // 费用相关
  { id: 14, time: '2026-03-04 10:45:15', operator: '小王', department: '销售部', role: 'sales', actionType: 'create', module: 'expense', target: '某央企项目保证金', detail: '申请保证金50000元', ip: '192.168.1.100', status: 'success' },
  { id: 15, time: '2026-03-04 11:30:20', operator: '张经理', department: '管理部', role: 'manager', actionType: 'approve', module: 'expense', target: '深圳地铁项目标书费', detail: '审批标书费申请', ip: '192.168.1.101', status: 'success' },
  { id: 16, time: '2026-03-04 13:15:40', operator: '小王', department: '销售部', role: 'sales', actionType: 'export', module: 'expense', target: '费用明细表', detail: '导出3月份费用报表', ip: '192.168.1.100', status: 'success' },
  // 账户相关
  { id: 17, time: '2026-03-04 08:30:25', operator: '李工', department: '技术部', role: 'staff', actionType: 'update', module: 'account', target: '中国政府采购网账号', detail: '修改账号密码', ip: '192.168.1.102', status: 'success' },
  { id: 18, time: '2026-03-04 09:45:30', operator: '小王', department: '销售部', role: 'sales', actionType: 'create', module: 'account', target: '各省招标网账号', detail: '添加新账号', ip: '192.168.1.100', status: 'success' },
  { id: 19, time: '2026-03-04 15:20:15', operator: '张经理', department: '管理部', role: 'manager', actionType: 'delete', module: 'account', target: '过期测试账号', detail: '删除无效账号', ip: '192.168.1.101', status: 'success' },
  // 系统设置
  { id: 20, time: '2026-03-04 08:00:10', operator: '李总', department: '管理部', role: 'admin', actionType: 'update', module: 'system', target: '系统配置', detail: '修改系统参数', ip: '192.168.1.103', status: 'success' },
  { id: 21, time: '2026-03-04 12:00:25', operator: '李总', department: '管理部', role: 'admin', actionType: 'create', module: 'system', target: '销售部-小王', detail: '添加用户角色', ip: '192.168.1.103', status: 'success' },
  { id: 22, time: '2026-03-04 17:30:40', operator: '李总', department: '管理部', role: 'admin', actionType: 'export', module: 'system', target: '审计日志', detail: '导出系统日志', ip: '192.168.1.103', status: 'success' },
  // 异常操作
  { id: 23, time: '2026-03-04 10:30:15', operator: '未知用户', department: '-', role: 'unknown', actionType: 'login', module: 'system', target: '系统登录', detail: '密码错误3次', ip: '192.168.1.200', status: 'failed' },
  { id: 24, time: '2026-03-04 14:15:30', operator: '小王', department: '销售部', role: 'sales', actionType: 'delete', module: 'project', target: '某央企项目', detail: '无权限删除项目', ip: '192.168.1.100', status: 'failed' },
  // 更多今日操作
  { id: 25, time: '2026-03-04 09:00:00', operator: '李工', department: '技术部', role: 'staff', actionType: 'login', module: 'system', target: '系统登录', detail: '用户登录系统', ip: '192.168.1.102', status: 'success' },
  { id: 26, time: '2026-03-04 10:00:00', operator: '小王', department: '销售部', role: 'sales', actionType: 'view', module: 'project', target: '西部云数据中心建设', detail: '查看项目详情', ip: '192.168.1.100', status: 'success' },
  { id: 27, time: '2026-03-04 11:00:00', operator: '张经理', department: '管理部', role: 'manager', actionType: 'view', module: 'analytics', target: '数据报表', detail: '查看销售统计', ip: '192.168.1.101', status: 'success' },
])

const auditSummary = ref(null)

// 审计分页
const auditPagination = ref({
  page: 1,
  pageSize: 20,
  total: auditLogs.value.length
})

// 过滤后的审计日志
const filteredAuditLogs = computed(() => {
  if (!isMockMode()) {
    return auditLogs.value
  }

  let result = [...auditLogs.value]

  // 关键字搜索
  if (auditSearch.value.keyword) {
    const keyword = auditSearch.value.keyword.toLowerCase()
    result = result.filter(log =>
      log.detail?.toLowerCase().includes(keyword) ||
      log.target?.toLowerCase().includes(keyword) ||
      log.operator?.toLowerCase().includes(keyword)
    )
  }

  // 操作类型筛选
  if (auditSearch.value.actionType) {
    result = result.filter(log => log.actionType === auditSearch.value.actionType)
  }

  // 模块筛选
  if (auditSearch.value.module) {
    result = result.filter(log => log.module === auditSearch.value.module)
  }

  // 操作人筛选
  if (auditSearch.value.operator) {
    result = result.filter(log => log.operator === auditSearch.value.operator)
  }

  // 部门筛选
  if (auditSearch.value.department) {
    result = result.filter(log => log.department === auditSearch.value.department)
  }

  // 时间范围筛选
  if (auditSearch.value.dateRange && auditSearch.value.dateRange.length === 2) {
    const [start, end] = auditSearch.value.dateRange
    result = result.filter(log => {
      const logDate = log.time.split(' ')[0]
      return logDate >= start && logDate <= end
    })
  }

  return result
})

// 统计数据
const todayAuditCount = computed(() => {
  if (!isMockMode() && auditSummary.value) {
    return auditSummary.value.todayCount || 0
  }
  const today = new Date().toISOString().split('T')[0]
  return auditLogs.value.filter(log => log.time.startsWith(today)).length
})

const weekAuditCount = computed(() => {
  if (!isMockMode() && auditSummary.value) {
    return auditSummary.value.weekCount || 0
  }
  return auditLogs.value.length
})

const failedAuditCount = computed(() => {
  if (!isMockMode() && auditSummary.value) {
    return auditSummary.value.failedCount || 0
  }
  return auditLogs.value.filter(log => log.status === 'failed').length
})

const activeUserCount = computed(() => {
  if (!isMockMode() && auditSummary.value) {
    return auditSummary.value.activeUserCount || 0
  }
  const users = new Set(auditLogs.value.map(log => log.operator))
  return users.size
})

// 获取操作类型标签
const getActionTypeLabel = (type) => {
  const map = {
    'login': '登录',
    'create': '创建',
    'update': '修改',
    'delete': '删除',
    'approve': '审批',
    'export': '导出',
    'view': '查看'
  }
  return map[type] || type
}

const getActionTypeTag = (type) => {
  const map = {
    'login': 'info',
    'create': 'success',
    'update': 'warning',
    'delete': 'danger',
    'approve': 'primary',
    'export': '',
    'view': 'info'
  }
  return map[type] || ''
}

// 获取模块标签
const getModuleLabel = (module) => {
  const map = {
    'project': '项目',
    'bidding': '标讯',
    'qualification': '资质',
    'expense': '费用',
    'account': '账户',
    'system': '系统',
    'analytics': '报表'
  }
  return map[module] || module
}

// 审计搜索
const handleAuditSearch = () => {
  auditPagination.value.page = 1
  if (!isMockMode()) {
    loadAuditLogs()
    return
  }
  ElMessage.success('搜索完成')
}

// 审计重置
const handleAuditReset = () => {
  auditSearch.value = {
    keyword: '',
    actionType: '',
    module: '',
    operator: '',
    department: '',
    dateRange: []
  }
  if (!isMockMode()) {
    loadAuditLogs()
    return
  }
  ElMessage.info('搜索条件已重置')
}

// 导出审计日志
const handleExportAudit = () => {
  const payload = JSON.stringify(filteredAuditLogs.value, null, 2)
  const blob = new Blob([payload], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `audit-logs-${new Date().toISOString().slice(0, 10)}.json`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('审计日志导出成功')
}

// 查看操作对象
const handleViewTarget = (row) => {
  ElMessage.info(`跳转到: ${row.target}`)
}

// 查看审计详情
const handleViewAuditDetail = (row) => {
  ElMessage.info(`查看详情: ${row.detail}`)
}

const buildAuditQuery = () => {
  const params = {}
  if (auditSearch.value.keyword) params.keyword = auditSearch.value.keyword
  if (auditSearch.value.actionType) params.action = auditSearch.value.actionType
  if (auditSearch.value.module) params.module = auditSearch.value.module
  if (auditSearch.value.operator) params.operator = auditSearch.value.operator
  if (auditSearch.value.dateRange?.length === 2) {
    params.start = `${auditSearch.value.dateRange[0]}T00:00:00`
    params.end = `${auditSearch.value.dateRange[1]}T23:59:59`
  }
  return params
}

const loadAuditLogs = async () => {
  if (isMockMode()) {
    auditPagination.value.total = auditLogs.value.length
    return
  }

  try {
    const response = await auditApi.getLogs(buildAuditQuery())
    if (!response?.success) {
      throw new Error(response?.message || '加载审计日志失败')
    }

    auditLogs.value = Array.isArray(response?.data?.items) ? response.data.items : []
    auditSummary.value = response?.data?.summary || null
    auditPagination.value.total = auditLogs.value.length
  } catch (error) {
    ElMessage.error(error?.message || '加载审计日志失败')
  }
}

// 获取数据权限文本
const getDataScopeText = (scope) => {
  const map = {
    'all': '全部数据',
    'dept': '本部门',
    'deptAndSub': '本部门及下级',
    'self': '仅本人'
  }
  return map[scope] || scope
}

// 配置权限
const handleConfigPermission = (role) => {
  currentRole.value = role
  permissionTab.value = 'menu'
  dataScopeForm.value = {
    scope: role.dataScope || 'all',
    depts: [],
    projects: []
  }
  showPermissionDialog.value = true
}

// 保存权限配置
const savePermissionConfig = () => {
  if (currentRole.value) {
    currentRole.value.dataScope = dataScopeForm.value.scope
    currentRole.value.menuPermissions = []
  }
  ElMessage.success('权限配置已保存')
  showPermissionDialog.value = false
}

// 数据权限变更处理
const handleUserDataScopeChange = (row) => {
  // 用户数据权限变更事件
}

const handleUserProjectChange = (row) => {
  // 用户项目权限变更事件
}

const handleDeptDataScopeChange = (row) => {
  // 部门数据权限变更事件
}

onMounted(() => {
  loadAuditLogs()
})

const handleDeptCrossAccessChange = (row) => {
  // 部门跨部门访问变更事件
}

const saveDeptConfig = (row) => {
  ElMessage.success(`部门"${row.deptName}"数据权限配置已保存`)
}

const handleProjectVisibilityChange = (row) => {
  // 项目组可见范围变更事件
}

const saveProjectConfig = (row) => {
  ElMessage.success(`项目组"${row.groupName}"权限配置已保存`)
}

// 保存角色
const saveRole = () => {
  if (!roleForm.value.name || !roleForm.value.code) {
    ElMessage.warning('请填写角色名称和代码')
    return
  }
  roles.value.push({
    ...roleForm.value,
    userCount: 0,
    menuPermissions: [],
    dataScope: roleForm.value.dataScope
  })
  ElMessage.success('角色添加成功')
  showAddRoleDialog.value = false
  // 重置表单
  roleForm.value = {
    name: '',
    code: '',
    description: '',
    dataScope: 'all'
  }
}

const openPermissionConfig = (role) => {
  currentRole.value = role
  permissionTab.value = 'menu'
  dataScopeForm.value = {
    scope: role.dataScope || 'all',
    depts: cloneList(role.allowedDepts),
    projects: cloneList(role.allowedProjects)
  }
  showPermissionDialog.value = true
  nextTick(() => {
    menuTreeRef.value?.setCheckedKeys(cloneList(role.menuPermissions))
  })
}

const persistPermissionConfig = async () => {
  if (currentRole.value) {
    currentRole.value.dataScope = dataScopeForm.value.scope
    currentRole.value.menuPermissions = normalizeMenuPermissions(menuTreeRef.value?.getCheckedKeys(false) || [])
    currentRole.value.allowedDepts = cloneList(dataScopeForm.value.depts)
    currentRole.value.allowedProjects = cloneList(dataScopeForm.value.projects)
  }

  const saved = await persistSettings('权限配置已保存')
  if (saved) {
    showPermissionDialog.value = false
  }
}

const persistDeptConfig = async (row) => {
  await persistSettings(`部门\\\"${row.deptName}\\\"数据权限配置已保存`)
}

const persistProjectConfig = async (row) => {
  await persistSettings(`项目组\\\"${row.groupName}\\\"权限配置已保存`)
}

const persistRole = async () => {
  if (!roleForm.value.name || !roleForm.value.code) {
    ElMessage.warning('请填写角色名称和代码')
    return
  }

  roles.value.push({
    ...roleForm.value,
    userCount: 0,
    menuPermissions: [],
    dataScope: roleForm.value.dataScope,
    allowedProjects: [],
    allowedDepts: []
  })

  const saved = await persistSettings('角色添加成功')
  if (saved) {
    showAddRoleDialog.value = false
    roleForm.value = {
      name: '',
      code: '',
      description: '',
      dataScope: 'all'
    }
  }
}

onMounted(async () => {
  await loadSettings()
  if (isMockMode()) {
    persistRuntimeSettings(buildSettingsPayload())
  }
})

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
  { name: '获取项目列表', path: '/api/projects', method: 'GET', description: '查询投标项目列表', status: 'enabled', enabled: true },
  { name: '创建项目', path: '/api/projects', method: 'POST', description: '创建新的投标项目', status: 'enabled', enabled: true },
  { name: '获取项目详情', path: '/api/projects/{id}', method: 'GET', description: '获取指定项目详情', status: 'enabled', enabled: true },
  { name: '更新项目', path: '/api/projects/{id}', method: 'PUT', description: '更新项目信息', status: 'enabled', enabled: true },
  { name: '获取标讯列表', path: '/api/tenders', method: 'GET', description: '查询标讯列表', status: 'enabled', enabled: true },
  { name: '获取资质列表', path: '/api/knowledge/qualifications', method: 'GET', description: '查询企业资质信息', status: 'enabled', enabled: true },
  { name: '获取案例列表', path: '/api/knowledge/cases', method: 'GET', description: '查询案例库', status: 'enabled', enabled: true },
  { name: '获取模板列表', path: '/api/knowledge/templates', method: 'GET', description: '查询模板库', status: 'enabled', enabled: true },
  { name: '数据看板总览', path: '/api/analytics/overview', method: 'GET', description: '获取核心看板指标', status: 'enabled', enabled: true }
])

// API文档
const showApiDoc = ref(false)
const apiDocTab = ref('overview')
const baseUrl = computed(() => {
  return `${API_CONFIG.baseURL}/api`
})

// API 层测试
const apiMode = ref('mock')
const apiTesting = ref(false)
const apiTestResult = ref([])

async function testApiLayer() {
  apiTesting.value = true
  apiTestResult.value = []

  const addResult = (name, success, message) => {
    apiTestResult.value.push({ name, success, message })
  }

  try {
    addResult('配置检查', true, `${isMockMode() ? 'Mock' : 'API'} 模式, 地址: ${API_CONFIG.baseURL}`)
    addResult('正式上线范围', true, '以统一 API 层和真实 /api/* 契约为准，Mock 仅用于演示模式')

    // 测试 API 模块导入
    try {
      await import('@/api/modules/auth.js')
      addResult('authApi 模块', true, '导入成功')
    } catch (e) {
      addResult('authApi 模块', false, e.message)
    }

    try {
      await import('@/api/modules/tenders.js')
      addResult('tendersApi 模块', true, '导入成功')
    } catch (e) {
      addResult('tendersApi 模块', false, e.message)
    }

    try {
      await import('@/api/modules/projects.js')
      addResult('projectsApi 模块', true, '导入成功')
    } catch (e) {
      addResult('projectsApi 模块', false, e.message)
    }

    // 测试 API 调用
    try {
      const { authApi } = await import('@/api/modules/auth.js')
      const result = await authApi.login('小王', '123456')
      if (result.success) {
        addResult('authApi.login()', true, `用户 ${result.data.user.name} 登录成功`)
      } else {
        addResult('authApi.login()', false, '登录失败')
      }
    } catch (e) {
      addResult('authApi.login()', false, e.message)
    }

    try {
      const { tendersApi } = await import('@/api/modules/tenders.js')
      const result = await tendersApi.getList()
      if (result.success && result.data.length > 0) {
        addResult('tendersApi.getList()', true, `返回 ${result.data.length} 条标讯`)
      } else {
        addResult('tendersApi.getList()', false, '数据格式不正确')
      }
    } catch (e) {
      addResult('tendersApi.getList()', false, e.message)
    }

    try {
      const { projectsApi } = await import('@/api/modules/projects.js')
      const result = await projectsApi.getList()
      if (result.success && result.data.length > 0) {
        addResult('projectsApi.getList()', true, `返回 ${result.data.length} 个项目`)
      } else {
        addResult('projectsApi.getList()', false, '数据格式不正确')
      }
    } catch (e) {
      addResult('projectsApi.getList()', false, e.message)
    }

  } catch (e) {
    addResult('测试框架', false, e.message)
  }

  apiTesting.value = false
}

function clearApiTestResult() {
  apiTestResult.value = []
}

// 项目接口文档
const projectApis = [
  {
    name: '获取项目列表',
    path: 'GET /api/projects',
    method: 'GET',
    description: '查询投标项目列表，筛选在统一 API 层完成兼容',
    params: [
      { name: 'status', type: 'string', required: false, description: '项目状态筛选' },
      { name: 'managerId', type: 'string', required: false, description: '项目经理筛选' },
      { name: 'name', type: 'string', required: false, description: '项目名称关键字' }
    ],
    example: `GET /api/projects?status=bidding`
  },
  {
    name: '创建项目',
    path: 'POST /api/projects',
    method: 'POST',
    description: '创建新的投标项目',
    params: [
      { name: 'name', type: 'string', required: true, description: '项目名称' },
      { name: 'customerId', type: 'string', required: true, description: '客户ID' },
      { name: 'bidAmount', type: 'number', required: true, description: '投标金额（元）' },
      { name: 'bidDeadline', type: 'string', required: true, description: '投标截止时间，格式：YYYY-MM-DD HH:mm:ss' },
      { name: 'description', type: 'string', required: false, description: '项目描述' }
    ],
    example: `POST /api/projects
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
    path: 'GET /api/projects/{id}',
    method: 'GET',
    description: '获取指定项目的详细信息',
    params: [
      { name: 'id', type: 'string', required: true, description: '项目ID' }
    ],
    example: `GET /api/projects/1001`
  },
  {
    name: '更新项目',
    path: 'PUT /api/projects/{id}',
    method: 'PUT',
    description: '更新项目信息',
    params: [
      { name: 'id', type: 'number', required: true, description: '项目ID（路径参数）' },
      { name: 'name', type: 'string', required: false, description: '项目名称' },
      { name: 'bidAmount', type: 'number', required: false, description: '投标金额' },
      { name: 'status', type: 'string', required: false, description: '项目状态' }
    ],
    example: `PUT /api/projects/1001
{
  "bidAmount": 550000,
  "status": "submitting"
}`
  }
]

// 审批接口文档
const approvalApis = [
  {
    name: '上线范围说明',
    path: 'N/A',
    method: 'INFO',
    description: '审批流、回调流未纳入当前正式上线范围，不应按演示文档联调。',
    params: [
      { name: 'status', type: 'string', required: false, description: '仅保留说明，不提供联调参数' }
    ],
    example: `正式上线版本不提供审批接口文档，避免传播错误契约`
  }
]

// 回调接口文档
const callbackApis = [
  {
    name: '回调接口说明',
    path: 'N/A',
    method: 'INFO',
    description: '第三方回调链路未纳入当前正式上线范围，需单独立项后提供正式契约。',
    params: [
      { name: 'status', type: 'string', required: false, description: '仅保留说明，不提供联调参数' }
    ],
    example: `正式上线版本不提供回调接口文档，避免传播错误契约`
  }
]

const cloneList = (value) => (Array.isArray(value) ? [...value] : [])
const normalizeMenuPermissions = (permissions) => {
  const selected = cloneList(permissions)
  if (selected.includes('all')) {
    return ['all']
  }

  const parentPermissions = new Set(menuPermissions.value.map((item) => item.id))
  return selected.filter((permission) => {
    const separatorIndex = permission.indexOf('-')
    if (separatorIndex === -1) {
      return true
    }

    const parentPermission = permission.slice(0, separatorIndex)
    return !parentPermissions.has(parentPermission) || selected.includes(parentPermission)
  })
}

const applySettingsPayload = (payload) => {
  if (!payload) return

  persistRuntimeSettings(payload)
  userStore.permissionProfileLoaded = true

  if (payload.systemConfig) {
    config.value = {
      ...config.value,
      ...payload.systemConfig
    }
  }

  if (Array.isArray(payload.roles)) {
    roles.value = payload.roles.map((role) => ({
      ...role,
      menuPermissions: cloneList(role.menuPermissions),
      allowedProjects: cloneList(role.allowedProjects),
      allowedDepts: cloneList(role.allowedDepts)
    }))
  }

  if (Array.isArray(payload.deptDataScope)) {
    deptDataScope.value = payload.deptDataScope.map((item) => ({
      ...item,
      allowedDepts: cloneList(item.allowedDepts)
    }))
  }

  if (Array.isArray(payload.projectGroupScope)) {
    projectGroupScope.value = payload.projectGroupScope.map((item) => ({
      ...item,
      allowedRoles: cloneList(item.allowedRoles)
    }))
  }

  if (payload.integrationConfig) {
    integrationConfig.value = {
      ...integrationConfig.value,
      ...payload.integrationConfig
    }
  }

  if (Array.isArray(payload.flowMappings)) {
    flowMappings.value = payload.flowMappings.map((item) => ({ ...item }))
    integrationConfig.value.flowMapping = flowMappings.value
  }

  if (Array.isArray(payload.apiList)) {
    apiList.value = payload.apiList.map((item) => ({ ...item }))
  }
}

const buildSettingsPayload = () => ({
  systemConfig: {
    ...config.value
  },
  roles: roles.value.map((role) => ({
    code: role.code,
    name: role.name,
    description: role.description,
    userCount: role.userCount,
    dataScope: role.dataScope,
    menuPermissions: cloneList(role.menuPermissions),
    allowedProjects: cloneList(role.allowedProjects),
    allowedDepts: cloneList(role.allowedDepts)
  })),
  deptDataScope: deptDataScope.value.map((item) => ({
    deptName: item.deptName,
    dataScope: item.dataScope,
    canViewOtherDepts: item.canViewOtherDepts,
    allowedDepts: cloneList(item.allowedDepts)
  })),
  projectGroupScope: projectGroupScope.value.map((item) => ({
    groupName: item.groupName,
    manager: item.manager,
    memberCount: item.memberCount,
    visibility: item.visibility,
    allowedRoles: cloneList(item.allowedRoles)
  })),
  integrationConfig: {
    orgEnabled: integrationConfig.value.orgEnabled,
    orgSystem: integrationConfig.value.orgSystem,
    orgAppKey: integrationConfig.value.orgAppKey,
    orgAppSecret: integrationConfig.value.orgAppSecret,
    oaEnabled: integrationConfig.value.oaEnabled,
    oaUrl: integrationConfig.value.oaUrl,
    ssoEnabled: integrationConfig.value.ssoEnabled,
    callbackUrl: integrationConfig.value.callbackUrl,
    apiKey: integrationConfig.value.apiKey,
    ipWhitelist: integrationConfig.value.ipWhitelist
  },
  flowMappings: flowMappings.value.map((item) => ({
    systemFlow: item.systemFlow,
    oaFlow: item.oaFlow,
    oaFlowCode: item.oaFlowCode,
    oaFlowName: item.oaFlowName,
    description: item.description
  })),
  apiList: apiList.value.map((item) => ({
    name: item.name,
    path: item.path,
    method: item.method,
    description: item.description,
    status: item.status,
    enabled: item.enabled
  }))
})

const persistSettings = async (successMessage) => {
  if (isMockMode()) {
    persistRuntimeSettings(buildSettingsPayload())
    userStore.permissionProfileLoaded = true
    ElMessage.success(successMessage)
    return true
  }

  try {
    const response = await settingsApi.updateSettings(buildSettingsPayload())
    if (!response?.success || !response?.data) {
      throw new Error(response?.message || '保存配置失败')
    }
    applySettingsPayload(response.data)
    await userStore.refreshPermissionProfile()
    ElMessage.success(successMessage)
    return true
  } catch (error) {
    await loadSettings()
    ElMessage.error(error?.message || '保存配置失败')
    return false
  }
}

const loadSettings = async () => {
  if (isMockMode()) {
    return
  }

  try {
    const response = await settingsApi.getSettings()
    if (!response?.success || !response?.data) {
      throw new Error(response?.message || '加载系统配置失败')
    }
    applySettingsPayload(response.data)
    await userStore.refreshPermissionProfile()
  } catch (error) {
    ElMessage.error(error?.message || '加载系统配置失败')
  }
}

// 方法
const handleSaveConfig = async () => {
  await persistSettings('配置已保存')
}

const persistBaseConfig = async () => {
  await handleSaveConfig()
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

const persistOaConfig = async () => {
  await persistSettings('OA配置已保存')
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

const persistFlowMapping = async () => {
  const index = currentFlowMapping.value._index
  if (index !== undefined) {
    flowMappings.value[index].oaFlowCode = currentFlowMapping.value.oaFlowCode
    flowMappings.value[index].oaFlowName = currentFlowMapping.value.oaFlowName
    flowMappings.value[index].oaFlow = currentFlowMapping.value.oaFlowCode
  }

  const saved = await persistSettings('流程映射配置已保存')
  if (saved) {
    showFlowMappingDialog.value = false
  }
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

/* ==================== Tab Enhancements ==================== */

:deep(.el-tabs__item) {
  height: 42px;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-tabs__item:hover) {
  color: #0369a1;
}

:deep(.el-tabs__item.is-active) {
  color: #0369a1;
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #0369a1, #0ea5e9);
  height: 3px;
  border-radius: 2px;
}

/* ==================== Button Enhancements ==================== */

.tab-header .el-button,
.card-header .el-button,
.card-header > div .el-button {
  min-width: 110px;
  height: 38px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-header .el-button--primary,
.card-header .el-button--primary {
  background: linear-gradient(135deg, #0369a1, #0284c7);
  border: none;
  box-shadow: 0 2px 8px rgba(3, 105, 161, 0.2);
}

.tab-header .el-button--primary:hover,
.card-header .el-button--primary:hover {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(3, 105, 161, 0.3);
}

.tab-header .el-button--primary:active,
.card-header .el-button--primary:active {
  transform: translateY(0);
}

.tab-header .el-button--default,
.card-header .el-button--default {
  border: 1.5px solid #e5e7eb;
  color: #64748b;
}

.tab-header .el-button--default:hover,
.card-header .el-button--default:hover {
  border-color: #94a3b8;
  color: #1e293b;
  background: #f8fafc;
}

/* ==================== Input Field Enhancements ==================== */

:deep(.el-input__wrapper) {
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: none;
}

:deep(.el-input__wrapper:hover) {
  border-color: #94a3b8;
  box-shadow: 0 0 0 3px rgba(148, 163, 184, 0.1);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #0369a1;
  box-shadow: 0 0 0 3px rgba(3, 105, 161, 0.1);
}

/* Select dropdown */
:deep(.el-select__wrapper) {
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-select__wrapper:hover) {
  border-color: #94a3b8;
}

:deep(.el-select__wrapper.is-focus) {
  border-color: #0369a1;
  box-shadow: 0 0 0 3px rgba(3, 105, 161, 0.1);
}

/* ==================== Table Action Buttons ==================== */

:deep(.el-button--link) {
  font-size: 13px;
  font-weight: 500;
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-button--link:hover) {
  transform: translateX(2px);
}

/* ==================== Tag Enhancements ==================== */

:deep(.el-tag) {
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border: none;
}

:deep(.el-tag--primary) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #ffffff;
}

:deep(.el-tag--success) {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #ffffff;
}

:deep(.el-tag--warning) {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #ffffff;
}

:deep(.el-tag--danger) {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #ffffff;
}

:deep(.el-tag--info) {
  background: linear-gradient(135deg, #64748b, #475569);
  color: #ffffff;
}

/* ==================== Integration Card Enhancements ==================== */

.integration-card {
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
  border: 1.5px solid #e5e7eb;
}

.integration-card:hover {
  border-color: #0369a1;
  box-shadow: 0 4px 12px rgba(3, 105, 161, 0.08);
}

/* ==================== Switch Enhancement ==================== */

:deep(.el-switch__core) {
  transition: all 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

/* ==================== 数据权限 Tab 样式 ==================== */
.data-scope-container {
  padding: 10px 0;
}

.tab-label-with-icon {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-label-with-icon .el-badge {
  margin-left: 4px;
}

.rules-content {
  line-height: 1.8;
}

.rules-content h4 {
  margin: 12px 0 8px 0;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.rules-content ol {
  padding-left: 18px;
  margin: 0;
}

.rules-content ul {
  padding-left: 18px;
  margin: 0;
}

.rules-content li {
  margin: 4px 0;
}

// 审计系统样式
.audit-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.audit-search-card {
  :deep(.el-card__body) {
    padding: 16px;
  }

  :deep(.el-form--inline .el-form-item) {
    margin-right: 12px;
    margin-bottom: 12px;
  }
}

.audit-stats {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;

  .stat-item {
    flex: 1;
    text-align: center;
    padding: 12px;
    background: white;
    border-radius: 4px;

    .stat-label {
      font-size: 13px;
      color: #909399;
      margin-bottom: 8px;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: #409eff;

      &.danger {
        color: #f56c6c;
      }
    }
  }
}

.audit-table {
  .operator-cell {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .target-name {
    color: #409eff;
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }
}

.audit-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
}
</style>
