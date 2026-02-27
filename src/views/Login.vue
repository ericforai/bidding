<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1 class="login-title">西域MRO投标管理平台</h1>
        <p class="login-subtitle">专业的招投标全流程管理系统</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="loginForm.remember" class="login-checkbox">
            记住我
          </el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-tips">
        <p>测试账号：小王 / 张经理 / 李总 / 李工</p>
        <p>密码：任意输入即可</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
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

const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
    loading.value = true

    await userStore.login(loginForm.username, loginForm.password)

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
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #409EFF 100%);
}

.login-box {
  width: 420px;
  padding: 48px 40px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.login-form {
  margin-top: 32px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 8px 16px;
}

.login-checkbox {
  color: #595959;
}

.login-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
}

.login-tips {
  margin-top: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  text-align: center;
}

.login-tips p {
  margin: 4px 0;
  font-size: 12px;
  color: #8c8c8c;
}

@media (max-width: 480px) {
  .login-box {
    width: 90%;
    padding: 32px 24px;
  }

  .login-title {
    font-size: 24px;
  }

  .login-subtitle {
    font-size: 12px;
  }

  .login-form {
    margin-top: 24px;
  }

  .login-form :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  .login-tips {
    margin-top: 20px;
    padding: 12px;
  }

  .login-tips p {
    font-size: 11px;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .login-button {
    min-height: 48px;
  }

  .login-form :deep(.el-input__wrapper) {
    min-height: 48px;
  }
}
</style>
