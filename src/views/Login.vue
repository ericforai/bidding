<template>
  <div class="login-page">
    <!-- 左侧品牌区域 -->
    <LoginBrandSection />

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

          <div class="social-login">
            <div class="divider">
              <span>其他登录方式</span>
            </div>
            <el-button
              class="wecom-button"
              @click="handleWeComLogin"
            >
              <template #icon>
                <img src="/wecom-icon.png" alt="WeCom" class="wecom-icon-img">
              </template>
              企业微信登录
            </el-button>
          </div>
        </el-form>

        <LoginDevAccountsHint v-if="LoginDevAccountsHint" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineAsyncComponent, ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import LoginBrandSection from '@/components/login/LoginBrandSection.vue'

const LoginDevAccountsHint = import.meta.env.DEV
  ? defineAsyncComponent(() => import('@/components/common/LoginDevAccountsHint.vue'))
  : null

import { onMounted } from 'vue'
import { authApi } from '@/api/modules/auth'

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
      ElMessage.error(error?.message || '登录失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

const handleWeComLogin = async () => {
  try {
    const { data } = await authApi.getWeComAuthorizeParams()
    const { state, appid, agentid } = data
    
    if (!appid || !agentid) {
      ElMessage.error('企业微信集成配置不完整，请联系管理员')
      return
    }

    const redirectUri = encodeURIComponent(window.location.origin + '/login')
    const wecomUrl = `https://open.work.weixin.qq.com/wwopen/sso/qrConnect?appid=${appid}&agentid=${agentid}&redirect_uri=${redirectUri}&state=${state}`
    
    window.location.href = wecomUrl
  } catch (error) {
    ElMessage.error('无法启动企业微信登录，请联系管理员')
  }
}

onMounted(async () => {
  const urlParams = new URLSearchParams(window.location.search)
  const code = urlParams.get('code')
  const state = urlParams.get('state')

  if (code && state) {
    loading.value = true
    try {
      await userStore.loginByWeCom(code, state)
      ElMessage.success('企业微信登录成功')
      router.push('/dashboard')
    } catch (error) {
      if (error?.response?.data?.code === 40101) {
        ElMessage.warning('您的企业微信账号尚未绑定系统账号，请先手动登录一次进行绑定')
      } else {
        const msg = error?.response?.data?.message || error?.message || '企业微信登录失败'
        ElMessage.error(msg)
      }
      // Clean URL params
      router.replace('/login')
    } finally {
      loading.value = false
    }
  }
})
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #F8FAFC;
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

/* ==================== 第三方登录 ==================== */
.social-login {
  margin-top: 32px;
}

.divider {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e2e8f0;
}

.divider span {
  padding: 0 16px;
  font-size: 13px;
  color: #94a3b8;
}

.wecom-button {
  width: 100%;
  height: 44px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  transition: all 0.2s ease;
}

.wecom-button:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #0F172A;
}

.wecom-icon-img {
  width: 20px;
  height: 20px;
}

@media (max-width: 640px) {
  .login-section {
    padding: 24px;
  }
}
</style>
