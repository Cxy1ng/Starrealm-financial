<template>
  <div class="home-dashboard">
    <!-- 顶部流光欢迎横幅 Banner -->
    <el-card class="hero-banner stagger-1" shadow="never">
      <div class="hero-content">
        <div class="hero-left">
          <div class="greeting-chip">
            <span class="live-dot"></span>
            <span>智能财务工作台 · 实时同步</span>
          </div>
          <h2 class="hero-title">
            {{ greetingWord }}，{{ data.user.nickname || data.user.username }}
          </h2>
          <div class="hero-calendar-line">
            {{ todayCalendarText }}
          </div>
          <p class="hero-subtitle">
            欢迎回到星界。清晰的收支洞察与科学的预算规划，助您稳步积累每一份财富。
          </p>
        </div>
        <div class="hero-actions" v-if="data.user.role === 'USER'">
          <el-button type="primary" size="large" @click="router.push('/main/bill')">
            <el-icon style="margin-right: 6px"><Plus /></el-icon>记一笔账单
          </el-button>
          <el-button size="large" plain @click="router.push('/main/AIAnalysis')">
            <el-icon style="margin-right: 6px"><MagicStick /></el-icon>AI 账单诊断
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 核心指标 Bento 卡片组 -->
    <el-row :gutter="16" class="stagger-2" v-if="data.user.role === 'USER'" style="margin-bottom: 16px">
      <el-col :xs="24" :sm="8">
        <el-card class="metric-bento balance-bento" shadow="never">
          <div class="metric-head">
            <span class="metric-label">
              <el-icon class="metric-icon primary"><Wallet /></el-icon>账户净结余
            </span>
            <span class="metric-badge primary">NET BALANCE</span>
          </div>
          <div class="metric-value num-font">
            <el-statistic :value="balance" :precision="2" prefix="¥" />
          </div>
          <div class="metric-foot">总收入减去总支出后的净资产沉淀</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="8">
        <el-card class="metric-bento income-bento" shadow="never">
          <div class="metric-head">
            <span class="metric-label">
              <el-icon class="metric-icon success"><TrendCharts /></el-icon>累计总收入
            </span>
            <span class="metric-badge success">TOTAL INCOME</span>
          </div>
          <div class="metric-value num-font">
            <el-statistic :value="income" :precision="2" prefix="¥" />
          </div>
          <div class="metric-foot">包含薪资、理财收益及各类入账流水</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="8">
        <el-card class="metric-bento pay-bento" shadow="never">
          <div class="metric-head">
            <span class="metric-label">
              <el-icon class="metric-icon danger"><Histogram /></el-icon>累计总支出
            </span>
            <span class="metric-badge danger">TOTAL EXPENSE</span>
          </div>
          <div class="metric-value num-font">
            <el-statistic :value="pay" :precision="2" prefix="¥" />
          </div>
          <div class="metric-foot">日常消费、餐饮购物及固定账单支出</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分类进度条对比 -->
    <el-row :gutter="16" class="stagger-3" v-if="data.user.role === 'USER'" style="margin-bottom: 16px">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="section-header">
              <span class="section-title">收入构成占比</span>
              <el-tag size="small" type="success" effect="light">{{ data.incomeList.length }} 个分类</el-tag>
            </div>
          </template>
          <el-empty v-if="!data.incomeList.length" description="暂无收入记录" :image-size="72" />
          <div v-for="item in data.incomeList" :key="item.category" class="progress-row">
            <div class="progress-meta">
              <span class="cat-name">{{ item.category }}</span>
              <span class="cat-amount num-font">¥ {{ item.amount }}</span>
            </div>
            <el-progress :stroke-width="10" :percentage="item.percent" :color="data.colorList" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="section-header">
              <span class="section-title">支出构成占比（单位：元）</span>
              <el-tag size="small" type="danger" effect="light">{{ data.payList.length }} 个分类</el-tag>
            </div>
          </template>
          <el-empty v-if="!data.payList.length" description="暂无支出记录" :image-size="72" />
          <div v-for="item in data.payList" :key="item.category" class="progress-row">
            <div class="progress-meta">
              <span class="cat-name">{{ item.category }}</span>
              <span class="cat-amount num-font">¥ {{ item.amount }}</span>
            </div>
            <el-progress :stroke-width="10" :percentage="item.percent" :color="data.colorList" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ECharts 可视化环形图 -->
    <el-row :gutter="16" class="stagger-4" v-if="data.user.role === 'USER'" style="margin-bottom: 16px">
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <div style="height: 380px" id="pie1"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never">
          <div style="height: 380px" id="pie2"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统公告时间轴 -->
    <el-card class="stagger-4" shadow="never">
      <template #header>
        <div class="section-header">
          <span class="section-title">
            <el-icon class="section-head-icon"><Bell /></el-icon>平台最新公告
          </span>
          <el-tag size="small" effect="plain">System Announcements</el-tag>
        </div>
      </template>
      <el-timeline style="max-width: 720px; padding-left: 8px">
        <el-timeline-item
            v-for="item in data.noticeData"
            :key="item.id"
            :timestamp="item.time"
            type="primary"
            placement="top"
        >
          <div class="notice-item-box">
            <div class="notice-title">{{ item.title }}</div>
            <div class="notice-body">{{ item.content }}</div>
            <div class="notice-publisher">发布者：{{ item.user }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 初始密码修改弹窗（禁止遮罩点击关闭、禁止ESC关闭） -->
    <el-dialog
        v-model="forcePwdVisible"
        title="首次登录修改密码"
        width="460px"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :show-close="false"
        align-center
        destroy-on-close
    >
      <div style="margin-bottom: 14px;">
        <el-alert
            title="检测到当前使用的是初始密码。为保障账户安全，请先设置新密码后再继续使用。"
            type="warning"
            :closable="false"
            show-icon
        />
      </div>
      <el-form
          ref="forcePwdFormRef"
          :model="forcePwdForm"
          :rules="forcePwdRules"
          label-position="top"
      >
        <el-form-item label="当前密码" prop="pwd">
          <el-input
              v-model="forcePwdForm.pwd"
              type="password"
              placeholder="请输入当前登录使用的初始密码"
              show-password
              autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="设置新密码" prop="newPwd">
          <el-input
              v-model="forcePwdForm.newPwd"
              type="password"
              placeholder="8-20位，含大小写字母、数字及特殊符号"
              show-password
              autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPwd">
          <el-input
              v-model="forcePwdForm.confirmPwd"
              type="password"
              placeholder="请再次输入新密码"
              show-password
              autocomplete="new-password"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 10px;">
          <el-button @click="handleForcePwdLogout" style="flex: 1;">
            退出登录
          </el-button>
          <el-button type="primary" :loading="submittingForcePwd" @click="handleForcePwdSubmit" style="flex: 2;">
            提交并重新登录
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { request } from "@/utils/request.js";
import { reactive, ref, computed, watch, onMounted, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Plus, MagicStick, Bell, Wallet, TrendCharts, Histogram } from "@element-plus/icons-vue";
import { useDark, useTransition } from '@vueuse/core';
import * as echarts from "echarts";
import { getStoredLoginUser, updateStoredLoginUser, clearLoginSession } from "@/utils/auth.js";
import { getChartFontScale } from "@/utils/theme.js";
// Validator rules for password validation
import { validatePasswordRule } from "@/utils/validator.js";

const router = useRouter();
const isDark = useDark();

// 时段问候语与公历+星期+农历日期生成
const greetingWord = computed(() => {
  const h = new Date().getHours();
  if (h < 6) return "夜深了";
  if (h < 11) return "早安";
  if (h < 14) return "午安";
  if (h < 18) return "下午好";
  return "晚上好";
});

const todayCalendarText = computed(() => {
  const now = new Date();
  const solarStr = `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`;
  const weekNames = ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"];
  const weekStr = weekNames[now.getDay()];

  let lunarStr = "";
  try {
    const fmt = new Intl.DateTimeFormat("zh-CN-u-ca-chinese", {
      year: "numeric",
      month: "long",
      day: "numeric"
    });
    const parts = fmt.formatToParts(now);
    const yName = parts.find((p) => p.type === "yearName")?.value || "";
    const mVal = parts.find((p) => p.type === "month")?.value || "";
    const dVal = parts.find((p) => p.type === "day")?.value || "";
    const zodiacMap = {
      子: "鼠", 丑: "牛", 寅: "虎", 卯: "兔", 辰: "龙", 巳: "蛇",
      午: "马", 未: "羊", 申: "猴", 酉: "鸡", 戌: "狗", 亥: "猪"
    };
    const branch = yName.length >= 2 ? yName[1] : "";
    const animal = zodiacMap[branch] || "";
    const dayCn = {
      "1": "初一", "2": "初二", "3": "初三", "4": "初四", "5": "初五",
      "6": "初六", "7": "初七", "8": "初八", "9": "初九", "10": "初十",
      "11": "十一", "12": "十二", "13": "十三", "14": "十四", "15": "十五",
      "16": "十六", "17": "十七", "18": "十八", "19": "十九", "20": "二十",
      "21": "廿一", "22": "廿二", "23": "廿三", "24": "廿四", "25": "廿五",
      "26": "廿六", "27": "廿七", "28": "廿八", "29": "廿九", "30": "三十"
    }[dVal] || dVal;
    lunarStr = `农历${yName}${animal ? animal + "年" : ""}${mVal}${dayCn}`;
  } catch {
    lunarStr = "";
  }
  return `今天是 ${solarStr}，${weekStr}${lunarStr ? "，" + lunarStr : ""}`;
});

const data = reactive({
  user: getStoredLoginUser(),
  noticeData: [],
  acfun: {},
  payList: [],
  incomeList: [],
  colorList: ['#1677ff', '#06b6d4', '#10b981', '#f59e0b', '#8b5cf6']
});

// ==================== 初始临时密码强制修改逻辑 ====================
const forcePwdVisible = ref(Boolean(data.user?.needChangePwd === 1));
const submittingForcePwd = ref(false);
const forcePwdFormRef = ref();
const forcePwdForm = reactive({
  pwd: "",
  newPwd: "",
  confirmPwd: ""
});

const validateForceConfirmPwd = (rule, value, callback) => {
  if (!value) {
    callback(new Error("请再次输入新密码确认"));
  } else if (value !== forcePwdForm.newPwd) {
    callback(new Error("两次输入的新密码不一致"));
  } else {
    callback();
  }
};

const forcePwdRules = {
  pwd: [{ required: true, message: "请输入当前密码", trigger: "blur" }],
  newPwd: [
    { required: true, message: "请输入设置的新密码", trigger: "blur" },
    { validator: validatePasswordRule, trigger: "blur" }
  ],
  confirmPwd: [{ validator: validateForceConfirmPwd, trigger: "blur" }]
};

const handleForcePwdLogout = () => {
  clearLoginSession();
  request.post("/logout").catch(() => {});
  router.replace("/login");
};

const checkNeedChangePwd = () => {
  if (!data.user?.id) return;
  const api = data.user.perm >= 10 ? ('/admins/' + data.user.id) : ('/users/' + data.user.id);
  request.get(api).then(res => {
    if (res.code === 200 && res.data) {
      const needChange = res.data.needChangePwd === 1 ? 1 : 0;
      forcePwdVisible.value = needChange === 1;
      updateStoredLoginUser({ needChangePwd: needChange });
    }
  });
};
checkNeedChangePwd();

const handleForcePwdSubmit = () => {
  forcePwdFormRef.value?.validate((valid) => {
    if (!valid) return;
    submittingForcePwd.value = true;
    const payload = {
      id: data.user.id,
      perm: data.user.perm,
      role: data.user.role,
      username: data.user.username,
      pwd: forcePwdForm.pwd,
      newPwd: forcePwdForm.newPwd
    };
    request.put('/password', payload).then(res => {
      submittingForcePwd.value = false;
      if (res.code === 200) {
        ElMessage.success("初始密码重置成功，请使用新密码重新登录！");
        forcePwdVisible.value = false;
        // 密码重置后立即强制退出登录并跳转至登录页
        clearLoginSession();
        request.post("/logout").catch(() => {});
        router.replace("/login");
      } else {
        ElMessage.error(res.msg || "修改失败，请检查原密码是否正确");
      }
    }).catch(() => {
      submittingForcePwd.value = false;
    });
  });
};

const loadNotice = () => {
  request.get('/notices/all').then(res => {
    if (res.code === 200) {
      data.noticeData = (res.data || []).slice(0, 4);
    } else if (res.code !== 401) {
      ElMessage.error(res.msg);
    }
  });
};
loadNotice();

const loadAcfun = () => {
  if (data.user.role === 'USER') {
    request.get('/acfun/all', {
      params: { userId: data.user.id }
    }).then(res => {
      if (res.code === 200) {
        data.acfun = res.data?.length ? res.data[0] : {};
      } else if (res.code !== 401) {
        ElMessage.error(res.msg);
      }
    });
  }
};
loadAcfun();

function useAnimatedValue(sourceGetter, duration = 1200) {
  const raw = ref(0);
  const animated = useTransition(raw, { duration });
  watch(sourceGetter, (newVal) => {
    const num = Number(newVal);
    if (!Number.isNaN(num)) {
      raw.value = num;
    }
  }, { immediate: true });
  return animated;
}

const balance = useAnimatedValue(() => data.acfun.balance);
const income = useAnimatedValue(() => data.acfun.income);
const pay = useAnimatedValue(() => data.acfun.pay);

const createPieOption = (titleText, dark, dataList) => {
  const scale = getChartFontScale();
  return {
  title: {
    text: titleText,
    subtext: '实时账单聚合分布',
    left: 'left',
    textStyle: { fontSize: Math.round(16 * scale), fontWeight: 600 },
    subtextStyle: { fontSize: Math.round(12 * scale) }
  },
  backgroundColor: 'transparent',
  tooltip: {
    trigger: 'item',
    textStyle: { fontSize: Math.round(13 * scale) }
  },
  legend: {
    orient: 'horizontal',
    bottom: 0,
    textStyle: { color: dark ? '#cbd5e1' : '#475569', fontSize: Math.round(12 * scale) }
  },
  series: [
    {
      type: 'pie',
      radius: ['44%', '72%'],
      center: ['50%', '48%'],
      avoidLabelOverlap: false,
      showEmptyCircle: true,
      emptyCircleStyle: {
        color: dark ? 'rgba(148, 163, 184, 0.14)' : 'rgba(148, 163, 184, 0.22)',
        borderColor: dark ? 'rgba(255, 255, 255, 0.08)' : 'rgba(148, 163, 184, 0.28)',
        borderWidth: 1
      },
      itemStyle: {
        borderRadius: 10,
        borderColor: dark ? '#1e293b' : '#ffffff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center',
        formatter: (param) => `${param.name}\n${param.percent}%`
      },
      emphasis: {
        label: {
          show: true,
          fontSize: Math.round(17 * scale),
          fontWeight: 'bold',
          color: dark ? '#fff' : '#0f172a'
        }
      },
      labelLine: { show: false },
      data: dataList.map(item => ({
        name: item.category,
        value: item.amount
      }))
    }
  ]
  };
};

const loadPie = () => {
  const chartDom1 = document.getElementById('pie1');
  const chartDom2 = document.getElementById('pie2');
  if (!chartDom1 || !chartDom2) return;
  echarts.getInstanceByDom(chartDom1)?.dispose();
  echarts.getInstanceByDom(chartDom2)?.dispose();

  const myChart1 = echarts.init(chartDom1, isDark.value ? 'dark' : null);
  const myChart2 = echarts.init(chartDom2, isDark.value ? 'dark' : null);

  request.get('/bills/count', { params: { type: '收入' } }).then(res => {
    data.incomeList = res.data || [];
    myChart1.setOption(createPieOption('账户收入结构分布', isDark.value, data.incomeList));
  });
  request.get('/bills/count', { params: { type: '支出' } }).then(res => {
    data.payList = res.data || [];
    myChart2.setOption(createPieOption('账户支出结构分布', isDark.value, data.payList));
  });
};

const handleFontSizeChangeForCharts = () => {
  if (data.user.role === 'USER') {
    loadPie();
  }
};

onMounted(() => {
  if (data.user.role === 'USER') {
    loadPie();
  }
  window.addEventListener('font-size-change', handleFontSizeChangeForCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener('font-size-change', handleFontSizeChangeForCharts);
});

watch(isDark, () => {
  if (data.user.role === 'USER') {
    loadPie();
  }
});
</script>

<style scoped>
.home-dashboard {
  padding-bottom: 36px;
}
.hero-banner {
  margin-bottom: 16px;
  background: linear-gradient(135deg, rgba(var(--theme-rgb), 0.14) 0%, rgba(6, 182, 212, 0.06) 50%, rgba(139, 92, 246, 0.06) 100%) !important;
}

.hero-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
  padding: 6px 4px;
}

.greeting-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(var(--theme-rgb), 0.12);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 10px;
}

.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 8px #10b981;
}

.hero-title {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 700;
}

.hero-calendar-line {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-color-primary);
  letter-spacing: 0.02em;
}

.hero-subtitle {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13.5px;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.metric-bento {
  padding: 4px;
  position: relative;
  overflow: hidden;
}

.metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.metric-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.metric-icon.primary {
  color: var(--el-color-primary);
}
.metric-icon.success {
  color: #10b981;
}
.metric-icon.danger {
  color: #ef4444;
}

.metric-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  letter-spacing: 0.5px;
}

.metric-badge.primary {
  background: rgba(var(--theme-rgb), 0.12);
  color: var(--el-color-primary);
}

.metric-badge.success {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}

.metric-badge.danger {
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
}

.metric-value :deep(.el-statistic__content) {
  font-size: 30px;
  font-weight: 700;
}

.metric-foot {
  margin-top: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
}

.section-head-icon {
  color: var(--el-color-primary);
}

.progress-row {
  margin-bottom: 14px;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 6px;
}

.cat-name {
  font-weight: 500;
}

.cat-amount {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.notice-item-box {
  padding: 14px 18px;
  border-radius: 12px;
  background: rgba(var(--theme-rgb), 0.04);
  border: 1px solid rgba(var(--theme-rgb), 0.12);
}

.notice-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}

.notice-body {
  font-size: 13.5px;
  line-height: 1.65;
  color: var(--el-text-color-regular);
  white-space: pre-line;
}

.notice-publisher {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: right;
}
</style>
