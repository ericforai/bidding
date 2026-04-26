<template>
  <div class="login-page">
    <!-- 左侧品牌区域 -->
    <div class="brand-section">
      <div class="brand-content">
        <!-- Logo -->
        <div class="brand-logo">
          <div class="logo-icon">
            <img src="/favicon.ico" alt="西域 Logo">
          </div>
          <span class="logo-text">西域数智化投标管理平台</span>
        </div>

        <!-- 标语 -->
        <div class="brand-hero">
          <h1 class="hero-title">专业高效的<br>招投标全流程管理</h1>
          <p class="hero-desc">AI 驱动的智能投标分析，助您提升中标率</p>
        </div>

        <!-- 核心功能 -->
        <div class="features-grid">
          <div class="feature-item">
            <div class="feature-icon feature-blue">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 12l2 2 4-4"/>
                <circle cx="12" cy="12" r="10"/>
              </svg>
            </div>
            <span class="feature-text">智能标书分析</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon feature-green">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"/>
              </svg>
            </div>
            <span class="feature-text">中标率预测</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon feature-orange">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/>
              </svg>
            </div>
            <span class="feature-text">团队协作</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon feature-purple">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
              </svg>
            </div>
            <span class="feature-text">数据报表</span>
          </div>
        </div>

        <!-- 底部版权 -->
        <div class="brand-footer">
          <span>© 西域数智化投标管理平台</span>
        </div>
      </div>
    </div>

    <!-- 右侧登录区域 -->
    <div class="login-section">
      <div class="login-container">
        <div class="login-header">
          <h2 class="login-title">欢迎回来</h2>
          <p class="login-subtitle">登录您的账户继续工作</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <label class="form-label">用户名</label>
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
              class="form-input"
            />
          </el-form-item>

          <el-form-item prop="password">
            <label class="form-label">密码</label>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              class="form-input"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <div class="form-actions">
              <el-checkbox v-model="loginForm.remember" class="login-checkbox">
                记住我
              </el-checkbox>
              <a href="#" class="forgot-link">忘记密码？</a>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-button"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="test-accounts">
          <div class="test-header">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 16v-4"/>
              <path d="M12 8h.01"/>
            </svg>
            <span>登录提示</span>
          </div>
          <div class="account-list">
            <span
              v-for="account in displayAccounts"
              :key="account"
              class="account-tag"
            >
              {{ account }}
            </span>
          </div>
          <p class="test-hint">{{ accountHint }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 3, message: '密码长度不能少于3位', trigger: 'blur' }
  ]
}

const displayAccounts = computed(() => [
  '普通员工: staff / Test@123',
  '管理者: manager / Test@123',
  '管理员: admin / XiyuAdmin2026!'
])

const accountHint = computed(() => '本地测试账号已按员工、经理、管理员权限划分')

const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
    loading.value = true

    await userStore.login(loginForm.username, loginForm.password, loginForm.remember)

    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    if (error !== false) {
      ElMessage.error('登录失败，请检查用户名和密码')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #F8FAFC;
}

/* ==================== 品牌区域（左侧） ==================== */
.brand-section {
  flex: 1;
  background: linear-gradient(135deg, #0F172A 0%, #1e293b 50%, #0F172A 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.brand-section::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 80%;
  height: 200%;
  background: radial-gradient(circle, rgba(3, 105, 161, 0.15) 0%, transparent 70%);
  pointer-events: none;
}

.brand-section::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -10%;
  width: 60%;
  height: 160%;
  background: radial-gradient(circle, rgba(0, 170, 68, 0.08) 0%, transparent 70%);
  pointer-events: none;
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 520px;
  width: 100%;
}

/* Logo */
.brand-logo {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 48px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.18);
}

.logo-icon img {
  width: 30px;
  height: 30px;
  object-fit: contain;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.02em;
}

/* Hero */
.brand-hero {
  margin-bottom: 56px;
}

.hero-title {
  font-size: 48px;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
  margin-bottom: 16px;
  letter-spacing: -0.02em;
}

.hero-desc {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
}

/* 功能网格 */
.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 48px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.15);
  transform: translateX(4px);
}

.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-icon svg {
  width: 20px;
  height: 20px;
}

.feature-blue {
  background: linear-gradient(135deg, rgba(3, 105, 161, 0.2) 0%, rgba(3, 105, 161, 0.1) 100%);
  color: #38bdf8;
}

.feature-green {
  background: linear-gradient(135deg, rgba(0, 170, 68, 0.2) 0%, rgba(0, 170, 68, 0.1) 100%);
  color: #4ade80;
}

.feature-orange {
  background: linear-gradient(135deg, rgba(255, 136, 0, 0.2) 0%, rgba(255, 136, 0, 0.1) 100%);
  color: #fb923c;
}

.feature-purple {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2) 0%, rgba(139, 92, 246, 0.1) 100%);
  color: #a78bfa;
}

.feature-text {
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

/* 底部版权 */
.brand-footer {
  text-align: center;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
}

/* ==================== 登录区域（右侧） ==================== */
.login-section {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: #fff;
}

.login-container {
  width: 100%;
  max-width: 400px;
}

.login-header {
  margin-bottom: 40px;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #0F172A;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 14px;
  color: #64748b;
}

/* 表单样式 */
.login-form {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  margin-bottom: 8px;
}

.form-input :deep(.el-input__wrapper) {
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  box-shadow: none;
  transition: all 0.2s ease;
}

.form-input :deep(.el-input__wrapper:hover) {
  border-color: #cbd5e1;
}

.form-input :deep(.el-input__inner) {
  font-size: 14px;
  color: #0F172A;
  outline: none;
  box-shadow: none;
}

.form-input :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.login-checkbox {
  color: #64748b;
  font-size: 13px;
}

.forgot-link {
  color: #0369A1;
  font-size: 13px;
  text-decoration: none;
  transition: color 0.2s ease;
}

.forgot-link:hover {
  color: #0891b2;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px;
  background: linear-gradient(135deg, #0369A1 0%, #0891b2 100%);
  border: none;
  transition: all 0.3s ease;
}

.login-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(3, 105, 161, 0.25);
}

/* 测试账号 */
.test-accounts {
  padding: 20px;
  background: linear-gradient(135deg, rgba(3, 105, 161, 0.05) 0%, rgba(3, 105, 161, 0.02) 100%);
  border: 1px solid rgba(3, 105, 161, 0.1);
  border-radius: 12px;
}

.test-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #0369A1;
  font-size: 13px;
  font-weight: 500;
}

.test-header svg {
  width: 16px;
  height: 16px;
}

.account-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.account-tag {
  display: inline-block;
  padding: 4px 12px;
  background: #fff;
  border: 1px solid rgba(3, 105, 161, 0.2);
  border-radius: 16px;
  font-size: 12px;
  color: #0369A1;
  font-weight: 500;
}

.test-hint {
  font-size: 12px;
  color: #64748b;
  margin: 0;
}

/* ==================== 响应式设计 ==================== */
@media (max-width: 1024px) {
  .brand-section {
    display: none;
  }

  .login-section {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .login-section {
    padding: 24px;
  }

  .hero-title {
    font-size: 36px;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .stats-row {
    flex-direction: column;
    gap: 16px;
  }

  .stat-divider {
    display: none;
  }
}
</style>
