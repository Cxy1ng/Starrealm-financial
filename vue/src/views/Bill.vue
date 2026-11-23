<template>
  <div class="bill-page-wrap">
    <!-- 1. 顶部检索与过滤卡片 -->
    <div class="surface-card filter-panel">
      <div class="filter-group">
        <el-input
            v-model="data.type"
            class="search-input"
            placeholder="账单类型（支出/收入）"
            clearable
            style="width: 170px;"
            :prefix-icon="Search"
        />
        <el-input
            v-model="data.category"
            class="search-input"
            placeholder="输入账单分类查询"
            clearable
            style="width: 170px;"
            :prefix-icon="Search"
        />
        <el-date-picker
            class="search-date"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            v-model="data.start"
            placeholder="查询开始日期"
            style="width: 150px;"/>
        <el-date-picker
            class="search-date"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            v-model="data.end"
            placeholder="查询结束日期"
            :disabled-date="disabledDate"
            :shortcuts="shortcuts"
            style="width: 150px;"/>
        <el-button class="btn-action btn-toolbar-search" :icon="Search" @click="load">查询</el-button>
        <el-button class="btn-action btn-toolbar-reset" :icon="Refresh" @click="reset">重置</el-button>
      </div>

      <div class="kpi-strip" v-if="data.total !== undefined">
        <div class="kpi-item">
          <span>共计流水</span>
          <span class="kpi-val num-font" style="color: var(--text-main);">{{ data.total }} 笔</span>
        </div>
      </div>
    </div>

    <!-- 2. 操作工具栏与单双行排版切换卡片 -->
    <div class="surface-card toolbar-panel">
      <div class="toolbar-left">
        <el-button
            class="btn-action btn-toolbar-create"
            :icon="Plus"
            @click="handleAdd"
            v-if="data.user.role === 'USER'"
        >
          新建账单
        </el-button>
        <el-button
            class="btn-action btn-toolbar-delete"
            :icon="Delete"
            @click="delBatch"
        >
          批量删除
        </el-button>
        <el-button
            v-if="data.user.role === 'USER'"
            class="btn-action btn-toolbar-import"
            :icon="Upload"
            @click="importModalVisible = true"
        >
          导入
        </el-button>
        <el-button
            class="btn-action btn-toolbar-export"
            :icon="Download"
            @click="exportData"
            v-if="data.user.role === 'USER'"
        >
          导出
        </el-button>
      </div>

      <div class="toolbar-right">
        <div class="segmented-switch" role="group" aria-label="表格行密度切换">
          <button
              type="button"
              class="segmented-btn"
              :class="{ 'is-active': layoutMode === 'single' }"
              @click="layoutMode = 'single'"
          >
            单行紧凑
          </button>
          <button
              type="button"
              class="segmented-btn"
              :class="{ 'is-active': layoutMode === 'double' }"
              @click="layoutMode = 'double'"
          >
            双行聚合
          </button>
        </div>
      </div>
    </div>

    <!-- 3. 无竖线金融账单大表卡片 -->
    <div class="surface-card bill-table-card">
      <el-table
          :data="data.tableData"
          class="custom-bill-table main-bill-table"
          :class="{ 'is-double-table': layoutMode === 'double' }"
          :default-sort="{ prop: 'id', order: 'descending'}"
          style="width: 100%;"
          @selection-change="handleSelectionChange"
      >
        <template #empty>
          <div class="empty-table-placeholder">
            <div class="empty-table-title">暂无收支账单数据</div>
            <div class="empty-table-sub">当前没有任何账单记录，您可以点击上方「新建账单」或「导入」添加账单</div>
          </div>
        </template>

        <el-table-column type="selection" width="38" align="center"/>
        <el-table-column label="ID" prop="id" width="60" align="center" sortable class-name="num-font"/>

        <!-- 交易时间（支持单行/双行响应式排版） -->
        <el-table-column
            label="交易时间"
            prop="time"
            :min-width="layoutMode === 'double' ? 95 : 150"
            class-name="num-font"
            sortable
        >
          <template #default="{ row }">
            <div class="cell-time-box" :class="{ 'is-double-line': layoutMode === 'double' }">
              <span class="time-date">{{ splitDateTime(row.time).date }}</span>
              <span class="time-clock">{{ splitDateTime(row.time).clock }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 柔和色点分类徽标 -->
        <el-table-column label="交易分类" prop="category" min-width="95">
          <template #default="{ row }">
            <span class="cat-pill" :class="getCategoryClass(row.category)">{{ row.category || '未分类' }}</span>
          </template>
        </el-table-column>

        <!-- 商户 / 商品说明（双行/单行商户商品与分割线） -->
        <el-table-column label="商户 / 商品说明" :min-width="layoutMode === 'double' ? 200 : 250">
          <template #default="{ row }">
            <div class="cell-merchant-product" :class="{ 'is-double-line': layoutMode === 'double' }">
              <el-tooltip v-if="row.rawPayee && row.rawPayee !== row.payee" :content="'原始商户: ' + row.rawPayee" placement="top" :show-after="180">
                <span class="cell-title">
                  <span>{{ getMerchantDisplay(row).title }}</span>
                  <span class="cleaned-tag">转译</span>
                </span>
              </el-tooltip>
              <el-tooltip v-else :content="getMerchantDisplay(row).title" placement="top" :show-after="250">
                <span class="cell-title">{{ getMerchantDisplay(row).title }}</span>
              </el-tooltip>
              <template v-if="getMerchantDisplay(row).hasSub">
                <span class="cell-divider">·</span>
                <el-tooltip :content="getMerchantDisplay(row).sub" placement="top" :show-after="220">
                  <span class="cell-sub">{{ getMerchantDisplay(row).sub }}</span>
                </el-tooltip>
              </template>
            </div>
          </template>
        </el-table-column>

        <!-- 账单类型徽标 -->
        <el-table-column label="类型" prop="type" width="65" align="center">
          <template #default="{ row }">
            <span class="type-badge" :class="row.type === '支出' ? 'is-expense' : 'is-income'">{{ row.type || '支出' }}</span>
          </template>
        </el-table-column>

        <!-- 核心金额排版 -->
        <el-table-column label="金额（元）" prop="amount" min-width="110" align="right">
          <template #default="{ row }">
            <div class="num-font amount-main" :class="row.type === '支出' ? 'amount-expense' : 'amount-income'">
              {{ row.type === '支出' ? '-' : '+' }}¥{{ Number(row.amount || 0).toFixed(2) }}
            </div>
            <div v-if="row.rawAmount" class="num-font amount-sub">{{ row.rawAmount }}</div>
          </template>
        </el-table-column>

        <!-- 收付款方式 -->
        <el-table-column label="收付款方式" prop="payment" min-width="125">
          <template #default="{ row }">
            <el-tooltip :content="row.payment || '-'" placement="top" :show-after="200">
              <div class="payment-cell-badge">
                <span class="payment-mini-icon" v-html="getPaymentMiniSvg(row.payment)"></span>
                <span style="overflow: hidden; text-overflow: ellipsis;">{{ row.payment || '-' }}</span>
              </div>
            </el-tooltip>
          </template>
        </el-table-column>

        <!-- 交易状态（带 status-dot 呼吸小圆点动效） -->
        <el-table-column label="交易状态" prop="status" min-width="95" align="center">
          <template #default="{ row }">
            <el-tooltip :content="row.status || '支付成功'" placement="top" :show-after="150">
              <span class="status-pill" :class="'status-' + getStatusMeta(row.status).type">
                <span class="status-dot"></span>
                <span>{{ getStatusMeta(row.status).label }}</span>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>

        <el-table-column label="备注" prop="comment" show-overflow-tooltip min-width="90"/>
        <el-table-column v-if="data.user.perm >= 10" label="用户ID" prop="userId" width="80" align="center" show-overflow-tooltip/>

        <!-- 操作链接按钮组 -->
        <el-table-column label="操作" align="center" width="130" fixed="right">
          <template #default="scope">
            <div class="table-action-group">
              <button type="button" class="action-link-btn is-edit" @click="handleUpdate(scope.row)">
                <el-icon><Edit /></el-icon> 编辑
              </button>
              <button type="button" class="action-link-btn is-del" @click="handleDelete(scope.row.id)">
                <el-icon><Delete /></el-icon> 删除
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 底部统计与分页 -->
      <div class="table-pagination-row">
        <div class="pagination-info">
          共 <strong class="num-font">{{ data.total }}</strong> 条 · 当前显示第
          <strong class="num-font">{{ data.total ? (data.pageNum - 1) * data.pageSize + 1 : 0 }}</strong> -
          <strong class="num-font">{{ Math.min(data.pageNum * data.pageSize, data.total) }}</strong> 条
        </div>
        <el-pagination
            @current-change="load"
            @size-change="load"
            v-model:current-page="data.pageNum"
            v-model:page-size="data.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            background
            layout="sizes, prev, pager, next, jumper"
            :total="data.total"
            size="small"
        />
      </div>
    </div>

    <!-- 账单编辑与新建弹窗 -->
    <el-dialog v-model="data.formVisible" title="账单信息" width="40vw" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :rules="data.rules" :model="data.form" style="padding: 10px 50px 0 20px;" label-position="right" label-width="auto">
        <el-form-item label="账单类型" prop="type">
          <el-select style="width: 100%;" v-model="data.form.type" @change="getCategoryList">
            <el-option value="收入" />
            <el-option value="支出" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易分类" prop="category">
          <el-select style="width: 100%;" v-model="data.form.category" placeholder="请选择账单分类">
            <el-option v-for="item in data.categoryList" :key="item.id" :value="item.name"/>
          </el-select>
        </el-form-item>
        <el-form-item label="收款方" prop="payee">
          <el-input v-model="data.form.payee" placeholder="请输入收款方"/>
        </el-form-item>
        <el-form-item label="交易说明" prop="detail">
          <el-input v-model="data.form.detail" placeholder="请输入交易说明"/>
        </el-form-item>
        <el-form-item label="收付款方式" prop="payment">
          <el-select style="width: 100%;" v-model="data.form.payment" placeholder="请选择交易方式">
            <el-option value="支付宝"/>
            <el-option value="微信支付"/>
            <el-option value="银行卡支付"/>
            <el-option value="现金支付"/>
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input v-model="data.form.amount" placeholder="请输入金额" :min="0.1"/>
        </el-form-item>
        <el-form-item label="交易状态" prop="status">
          <el-select style="width: 100%;" v-model="data.form.status" placeholder="请选择交易状态">
            <el-option value="交易成功"/>
            <el-option value="已全额退款"/>
            <el-option value="存入成功"/>
            <el-option value="已到账"/>
            <el-option value="等待确认收货"/>
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="comment">
          <el-input v-model="data.form.comment" placeholder="请输入备注" type="textarea"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="data.formVisible = false">取消</el-button>
          <el-button type="primary" @click="save">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 批量账单智能导入向导模态框 -->
    <BillImportModal v-model:visible="importModalVisible" @success="load" />
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { Delete, Edit, Search, Refresh, Plus, Download, Upload } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { request } from "@/utils/request.js";
import dayjs from "dayjs";
import BillImportModal from "@/components/BillImportModal.vue";

const importModalVisible = ref(false);
const layoutMode = ref('single'); // 'single' | 'double'

const getMerchantDisplay = (row) => {
  if (!row) return { title: '-', sub: '', hasSub: false };
  let payee = (row.payee || '').trim();
  let detail = (row.detail || '').trim();

  // 过滤无效纯符号的商品说明
  if (detail === '/' || detail === '-' || detail === '.' || detail === '无' || detail === '无说明' || detail === '无商品说明') {
    detail = '';
  }

  const isInvalidPayee = !payee || payee === '/' || payee === '-' || payee === '.' || payee === '未知商户' || payee === '未知' || payee === '其他';

  // 1. 当收款方为空或无效符号时，智能从商品说明/交易摘要中提取商户名与业务子项
  if (isInvalidPayee) {
    if (!detail) {
      return { title: '其他收支', sub: '', hasSub: false };
    }

    // 匹配如 "网易BUFF出售-印花|TYLOO (全息) | 2020 RMR" -> brand: "网易BUFF", sub: "出售 印花|TYLOO..."
    const match = detail.match(/^([a-zA-Z0-9\u4e00-\u9fa5]+?)(出售|代发|充值|退款|快捷支付|支付|App|商城|平台)?\s*[-_—:|/]\s*(.*)$/);
    if (match && match[1].trim().length >= 2) {
      const brand = match[1].trim();
      const action = match[2] ? match[2].trim() : '';
      const rest = match[3] ? match[3].trim() : '';
      const sub = action ? `${action} ${rest}`.trim() : rest;
      return { title: brand, sub, hasSub: Boolean(sub) };
    }

    // 常见分隔符分割商户与子项
    const delIdx = detail.search(/[-_—:|/]/);
    if (delIdx > 1 && delIdx < detail.length - 1) {
      const title = detail.substring(0, delIdx).trim();
      const sub = detail.substring(delIdx + 1).trim();
      return { title, sub, hasSub: Boolean(sub) };
    }

    // 空格分割商户与子项（如 "海底捞火锅 3号桌消费"）
    const spaceIdx = detail.indexOf(' ');
    if (spaceIdx >= 2 && spaceIdx <= 12) {
      const title = detail.substring(0, spaceIdx).trim();
      const sub = detail.substring(spaceIdx + 1).trim();
      return { title, sub, hasSub: Boolean(sub) };
    }

    return { title: detail, sub: '', hasSub: false };
  }

  // 2. 当商户与说明完全一致或说明为空时
  if (!detail || detail === payee) {
    // 智能分解类似 "团app短视频-现金激励-微信零钱" 中的品牌与细项
    const match = payee.match(/^([^\-_—:|/]+?)[\-_—:|/]\s*(.+)$/);
    if (match && match[1].trim().length >= 2 && !match[1].startsWith('商户')) {
      return { title: match[1].trim(), sub: match[2].trim(), hasSub: true };
    }
    return { title: payee, sub: '', hasSub: false };
  }

  return { title: payee, sub: detail, hasSub: true };
};

const data = reactive({
  user: JSON.parse(localStorage.getItem('loginToken') || '{}'),
  form: {},
  name: null,
  tableData: [],
  type: null,
  category: null,
  start: null,
  end: null,
  pageNum: 1,
  pageSize: 10,
  total: 0,
  formVisible: false,
  ids: [],
  rows: [],
  rules: {
    category: [
      { required: true, message: '请选择账单分类', trigger: 'blur' },
    ],
    type: [
      { required: true, message: '请选择收支类型', trigger: 'blur' },
    ],
    payment: [
      { required: true, message: '请选择收付款方式', trigger: 'blur' },
    ],
    amount: [
      { required: true, message: '请输入有效的金额', trigger: 'blur' },
      { pattern: /^(([1-9]{1}\d*)|([0]{1}))(\.(\d){1,2})?$/, message: '金额只能以数字形式输入，最多保留2位小数', trigger: 'blur' }
    ]
  },
  categoryList: []
});

const formRef = ref();

const load = () => {
  request.get('/bills', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      type: data.type,
      category: data.category,
      start: data.start,
      end: data.end,
    }
  }).then(res => {
    data.tableData = res.data?.list || [];
    data.total = res.data?.total || 0;
  });
};
load();

const reset = () => {
  data.type = null;
  data.category = null;
  data.start = null;
  data.end = null;
  load();
};

const handleAdd = () => {
  data.formVisible = true;
  data.form = {};
};

const handleUpdate = (row) => {
  data.form = JSON.parse(JSON.stringify(row));
  request.get('/categories/all', {
    params: {
      type: data.form.type
    }
  }).then(res => {
    data.categoryList = res.data || [];
  });
  data.formVisible = true;
};

const handleDelete = (id) => {
  ElMessageBox.confirm('删除账单是永久且不可逆的。您确定要删除吗？', '删除确认', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    request.delete('/bills/' + id).then(res => {
      if (res.code === 200) {
        ElMessage.success('操作成功');
        load();
      } else {
        ElMessage.error(res.msg);
      }
    });
  }).catch(() => {});
};

const handleSelectionChange = (rows) => {
  data.rows = rows;
  data.ids = rows.map(v => v.id);
};

const save = () => {
  formRef.value.validate((valid) => {
    if (valid) {
      data.form.id ? update() : add();
    }
  });
};

const add = () => {
  request.post('/bills', data.form).then(res => {
    if (res.code === 200) {
      data.formVisible = false;
      ElMessage.success('操作成功');
      load();
    } else {
      ElMessage.error(res.msg);
    }
  });
};

const update = () => {
  request.put('/bills/' + data.form.id, data.form).then(res => {
    if (res.code === 200) {
      data.formVisible = false;
      ElMessage.success('操作成功');
      load();
    } else {
      ElMessage.error(res.msg);
    }
  });
};

const delBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请至少选择一行账单记录');
  } else {
    ElMessageBox.confirm('删除账单是永久且不可逆的。您确定要删除吗？', '删除确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      request.delete('/bills/batch', { data: data.ids }).then(res => {
        if (res.code === 200) {
          ElMessage.success('操作成功');
          load();
        } else {
          ElMessage.error(res.msg);
        }
      });
    }).catch(() => {});
  }
};

const exportData = () => {
  let idsStr = data.ids.join(",");
  let url = `http://localhost:8080/bills/export?ids=${idsStr}`;
  let token = data.user.token;
  if (token) {
    url += `&token=${token}`;
  }
  window.open(url);
};

const getCategoryList = () => {
  if (data.form.category) {
    data.form.category = '';
  }
  request.get('/categories/all', {
    params: {
      type: data.form.type
    }
  }).then(res => {
    data.categoryList = res.data || [];
  });
};

const shortcuts = [
  { text: '今天', value: new Date() },
  {
    text: '昨天',
    value: () => {
      const date = new Date();
      date.setTime(date.getTime() - 3600 * 1000 * 24);
      return date;
    },
  },
  {
    text: '一周前',
    value: () => {
      const date = new Date();
      date.setTime(date.getTime() - 3600 * 1000 * 24 * 7);
      return date;
    },
  },
];

const disabledDate = (time) => {
  return time.getTime() > Date.now();
};

const splitDateTime = (timeStr) => {
  if (!timeStr) return { date: '-', clock: '' };
  const formatted = dayjs(timeStr).format('YYYY-MM-DD HH:mm:ss');
  const parts = formatted.split(' ');
  return { date: parts[0] || '', clock: parts[1] || '' };
};

const getCategoryClass = (catName) => {
  const name = String(catName || '');
  if (/餐饮|美食|小吃|外卖|咖啡|食品|果蔬/.test(name)) return 'cat-food';
  if (/交通|出行|打车|地铁|公交|加油|通行费|停车/.test(name)) return 'cat-transport';
  if (/购物|百货|超市|服饰|数码/.test(name)) return 'cat-shopping';
  if (/理财|投资|薪资|报酬|奖金|收益|利息|工资|副业|外快|补贴|奖励/.test(name)) return 'cat-finance';
  if (/医疗|健康|保险|药/.test(name)) return 'cat-health';
  if (/生活|缴费|电费|水费|燃气|物业/.test(name)) return 'cat-utility';
  if (/技术|云端|服务|订阅|娱乐|游戏|科技/.test(name)) return 'cat-tech';
  if (/红包|转账/.test(name)) return 'cat-transfer';
  if (/退款|退还|待确认/.test(name)) return 'cat-refund';
  return 'tag-cat';
};

const STATUS_TAXONOMY = [
  { type: 'warning', label: '部分退款', regex: /已退款\s*\([¥￥]?([0-9.]+)\)|部分退款|退差价/ },
  { type: 'warning', label: '全额退款', regex: /已全额退款|全额退款|对方已退还|已退还|退还成功/ },
  { type: 'danger', label: '交易关闭', regex: /交易关闭|已关闭|超时未支付|超时关闭|订单取消|用户取消/ },
  { type: 'danger', label: '支付失败', regex: /支付失败|扣款失败|对方已拒收|交易失败|扣款不足/ },
  { type: 'success', label: '转账成功', regex: /对方已收钱|已转账|转账成功|转账到/ },
  { type: 'success', label: '支付成功', regex: /支付成功|交易成功|扣款成功|消费成功|存入成功|已到账/ }
];

const getStatusMeta = (statusStr) => {
  const raw = String(statusStr || '支付成功').trim();
  for (const item of STATUS_TAXONOMY) {
    if (item.regex.test(raw)) {
      return { label: item.label, type: item.type };
    }
  }
  return { label: raw || '支付成功', type: 'success' };
};

const miniSvgMap = {
  wechat: `<svg viewBox="0 0 1024 1024" width="18" height="18" fill="none"><rect width="1024" height="1024" rx="240" fill="#22b331"/><path d="M743.8,284.1C724.6,270.2 692.5,246.3 663.6,233.4C634.7,220.4 600.8,213.0 573.7,207.8C549.1,203.2 529.3,202.1 504.3,203.2C477.2,204.5 439.9,209.2 414.4,214.9C391.9,220.0 374.0,227.8 354.1,237.0C334.2,246.1 316.1,254.9 297.3,268.8C277.3,283.5 253.3,304.3 236.7,323.4C220.6,341.9 210.1,360.8 199.7,380.8C189.3,400.8 180.0,418.7 175.7,441.2C171.3,463.7 172.2,490.7 174.3,513.4C176.5,535.9 181.3,554.7 188.4,574.6C195.4,594.6 204.0,612.8 215.8,630.9C227.6,649.0 243.0,665.4 258.0,681.0C273.0,696.5 296.1,710.1 304.1,722.5C310.2,731.8 306.7,742.5 304.9,753.5C302.7,767.3 292.9,793.4 290.8,804.0C289.9,808.2 292.6,815.7 292.6,815.7C292.6,815.7 300.4,817.4 304.1,815.4C317.5,808.0 357.3,780.7 371.7,772.4C377.4,769.1 382.4,766.1 389.0,766.4C406.6,767.2 449.5,775.0 475.2,776.7C498.5,778.3 518.8,778.1 540.1,776.8C561.5,775.4 583.1,772.5 601.0,768.9C617.4,765.5 630.3,762.4 645.4,755.3C667.3,745.0 703.6,727.7 729.4,708.6C755.1,689.5 779.2,665.2 796.7,643.0C813.2,622.2 823.4,601.0 832.3,578.3C841.3,555.5 846.5,526.5 849.4,509.3C851.3,497.8 850.6,488.6 849.2,477.0C847.1,460.9 843.7,432.9 837.4,414.4C831.2,396.5 811.9,368.5 811.9,368.5C811.9,368.5 802.9,368.4 798.9,370.9C733.2,410.1 493.3,560.4 425.8,599.0C418.2,603.3 409.3,601.1 401.6,597.6C393.9,594.0 385.0,587.2 380.5,577.9C367.7,551.1 334.5,465.9 326.3,439.8C324.5,434.1 328.6,427.5 332.5,424.4C336.4,421.2 343.9,418.3 349.0,421.4C364.2,430.5 406.6,467.3 421.7,477.7C426.6,481.2 432.5,482.4 437.6,482.8C442.5,483.2 446.6,482.1 451.2,480.0C507.6,454.2 713.9,357.6 769.4,330.8C773.1,329.0 776.1,325.3 777.4,322.6C778.6,320.2 778.6,317.2 776.8,315.2C771.1,308.6 757.0,293.7 743.8,284.1Z" fill="#ffffff"/></svg>`,
  alipay: `<svg viewBox="0 0 1024 1024" width="18" height="18" fill="none"><rect width="1024" height="1024" rx="240" fill="#f0f7ff" stroke="#1677ff" stroke-opacity="0.28" stroke-width="48"/><path d="M247.3,296.2C244.4,299.9 241.3,307.2 239.4,313.9C237.4,320.8 236.1,332.0 235.8,336.9C235.6,339.1 236.3,341.0 237.6,342.7C239.0,344.4 241.0,346.8 243.6,346.8C278.5,347.6 408.2,346.7 442.7,347.4C444.5,347.4 446.3,348.8 446.4,350.6C446.8,361.5 444.9,411.6 444.9,411.6C444.9,411.6 333.8,411.5 309.6,413.0C305.9,413.2 303.9,416.7 302.3,420.0C299.9,424.9 296.6,435.4 295.3,442.0C294.2,447.9 294.5,454.9 295.0,458.7C295.3,461.0 296.1,464.1 298.4,464.1C344.6,465.2 520.4,464.4 566.8,464.8C568.5,464.8 570.4,465.5 571.3,466.6C572.3,467.8 573.0,469.8 572.6,471.7C569.7,485.6 560.7,528.3 554.3,548.5C549.3,564.3 535.1,590.4 535.1,590.4C535.1,590.4 489.8,570.6 471.9,563.6C457.1,557.7 444.3,553.4 430.0,549.1C415.7,544.8 402.5,541.3 387.7,538.4C373.0,535.4 359.3,532.7 343.1,531.6C325.8,530.4 300.6,530.5 285.6,531.6C274.4,532.4 265.7,534.8 254.9,538.0C244.1,541.2 233.3,544.6 222.1,550.4C210.7,556.3 197.7,564.8 188.1,572.5C179.1,579.9 172.2,587.3 165.6,595.8C159.1,604.4 154.1,612.6 149.5,622.9C144.9,633.2 140.7,645.8 138.4,656.4C136.3,666.4 136.2,676.0 136.2,684.8C136.3,693.1 136.7,699.7 138.7,707.8C141.0,717.6 145.2,732.0 150.1,742.6C155.0,753.2 161.3,761.8 167.7,769.9C174.0,778.1 179.5,784.2 187.6,790.5C197.2,798.1 210.8,807.9 224.4,814.4C238.1,820.9 255.8,825.8 267.7,829.0C277.2,831.5 284.9,832.4 294.8,832.9C306.3,833.6 323.8,833.5 335.3,832.9C345.3,832.4 353.0,831.4 362.7,829.5C376.1,827.0 396.0,823.7 414.1,818.2C432.5,812.6 451.7,805.6 470.9,796.4C490.1,787.1 509.1,776.0 527.0,763.8C545.0,751.7 576.7,724.7 576.7,724.7C576.7,724.7 659.0,776.4 681.2,789.8C690.3,795.2 697.6,799.1 707.3,803.4C719.8,808.9 741.8,817.9 755.3,822.4C766.3,826.1 775.2,828.4 786.7,829.9C798.5,831.4 815.4,831.8 824.9,831.5C831.4,831.2 836.8,830.7 842.5,827.6C848.6,824.4 855.6,818.6 861.0,812.2C866.4,805.8 871.1,798.7 874.5,789.9C878.7,779.1 883.3,761.0 885.5,748.3C887.6,736.6 888.4,723.0 887.5,715.2C886.9,709.9 883.1,705.3 880.4,702.4C877.9,699.7 874.8,698.5 871.2,698.1C862.4,697.1 843.8,698.6 828.6,696.6C808.6,693.8 775.5,687.5 753.2,682.1C732.9,677.2 716.5,671.9 697.8,664.9C679.0,657.8 642.9,640.8 642.9,640.8C642.9,640.8 666.6,594.5 675.3,573.9C683.4,554.8 689.2,537.6 694.1,519.4C699.1,501.3 702.6,483.4 704.6,467.0C706.6,451.3 706.2,431.6 706.1,423.1C706.1,420.7 705.7,418.5 704.0,416.8C702.2,415.0 698.9,412.5 695.4,412.4C671.4,411.7 586.2,413.6 562.4,412.7C559.4,412.6 556.1,410.4 555.8,407.4C554.6,397.2 555.2,363.0 555.6,352.9C555.7,350.9 558.2,347.9 558.2,347.9C558.2,347.9 713.9,348.2 747.8,346.6C752.4,346.4 755.3,342.5 757.9,338.6C760.5,334.6 762.2,328.8 763.4,323.0C764.7,316.7 765.7,306.6 765.5,301.7C765.4,298.8 764.0,295.9 762.3,294.3C760.6,292.7 757.9,292.2 755.3,292.1C720.6,291.7 558.4,291.8 558.4,291.8C558.4,291.8 555.7,288.8 555.6,286.7C555.1,270.4 556.7,212.7 555.6,196.4C555.4,193.5 552.4,191.6 549.5,191.0C544.9,190.0 536.1,189.8 528.7,190.7C516.6,192.3 491.0,195.9 478.4,200.1C468.7,203.2 459.8,210.3 454.6,215.2C450.7,219.0 448.7,223.6 448.1,229.0C446.8,241.2 447.5,276.3 446.8,287.0C446.6,289.1 445.7,291.9 443.6,292.0C411.3,292.8 289.6,291.4 256.2,292.1C252.7,292.2 249.5,293.4 247.3,296.2Z M183.8,688.1C183.0,684.0 183.1,680.5 184.0,676.3C185.1,670.9 188.0,661.9 190.3,656.4C192.4,651.5 194.8,648.0 198.0,643.8C201.3,639.5 204.5,634.9 209.4,631.3C216.9,626.0 232.1,616.8 242.0,612.2C250.8,608.2 261.0,606.1 267.7,604.5C272.6,603.4 276.6,602.9 281.6,602.9C293.0,602.9 318.1,602.3 334.7,604.3C351.0,606.3 363.4,609.7 379.0,615.0C397.1,621.1 423.2,631.9 441.2,640.5C457.5,648.4 484.7,665.9 484.7,665.9C484.7,665.9 476.9,676.9 471.7,682.4C465.8,688.5 458.4,695.6 450.0,702.0C439.2,710.2 422.2,722.5 408.6,730.4C395.1,738.2 380.0,744.4 369.5,748.7C361.5,752.0 355.0,753.9 346.5,755.8C335.8,758.2 319.8,762.2 306.3,762.9C292.8,763.6 277.1,761.3 267.0,759.9C259.7,758.9 254.1,757.3 247.1,754.9C239.6,752.2 230.3,748.6 223.1,744.3C215.9,740.0 209.0,733.8 204.5,729.8C201.2,726.9 198.9,724.0 196.5,720.6C194.1,717.2 192.0,714.0 190.4,709.8C188.2,704.3 184.9,693.8 183.8,688.1Z" fill="#0068ff" fill-rule="evenodd" clip-rule="evenodd"/><path d="M684.8,543.7C684.8,543.7 671.0,578.1 663.3,594.2C655.7,610.2 644.8,630.2 639.9,638.0C638.7,639.8 636.3,640.4 634.3,639.8C627.8,637.9 602.0,626.7 602.0,626.7C602.0,626.7 599.5,629.1 599.3,630.9C598.7,635.3 597.7,647.4 598.3,652.6C598.7,656.3 600.5,658.8 602.6,661.8C608.8,670.7 622.9,689.8 635.0,705.1C648.2,721.8 668.1,746.3 680.4,760.2C689.3,770.4 696.7,778.0 707.0,787.0C717.4,796.0 741.6,813.6 741.6,813.6C741.6,813.6 721.2,806.8 710.3,801.8C697.7,795.9 682.3,788.2 667.4,779.2C644.8,765.5 593.6,731.2 577.4,721.7C575.7,720.7 573.6,721.9 572.0,723.2C565.7,728.5 540.2,752.5 540.2,752.5C540.2,752.5 565.9,731.0 572.3,726.3C574.0,725.0 576.0,723.6 577.8,724.6C596.3,735.4 654.3,774.3 681.2,789.7C700.2,800.6 718.0,808.6 736.3,815.5C754.6,822.4 773.7,827.4 788.7,830.1C801.4,832.5 814.9,832.3 824.6,831.5C832.5,830.9 839.2,829.7 845.9,825.7C852.6,821.8 858.8,815.6 864.1,808.2C869.5,800.6 874.4,791.5 877.6,781.1C881.5,768.6 885.8,746.9 887.1,734.5C888.1,725.0 888.0,714.2 885.5,708.0C883.2,702.6 878.1,699.4 872.2,698.3C862.5,696.3 844.0,698.7 828.3,696.6C808.2,693.9 775.9,687.7 753.9,682.4C733.8,677.5 717.8,672.1 699.1,665.4C680.5,658.7 653.7,647.6 644.2,642.8C642.3,641.8 642.1,639.0 642.9,637.1C647.1,628.0 661.8,604.6 668.9,588.7C676.0,573.0 684.8,543.7 684.8,543.7Z" fill="#78bcff"/></svg>`,
  bank: `<svg viewBox="0 0 24 24" width="18" height="18" fill="none"><rect width="24" height="24" rx="6" fill="#6366f1"/><rect x="4" y="7" width="16" height="10.5" rx="2" fill="#fff"/><rect x="4" y="8.8" width="16" height="2.2" fill="#c7d2fe"/><rect x="6.2" y="12.5" width="3.8" height="2.8" rx="0.6" fill="#f59e0b"/></svg>`,
  paypal: `<svg viewBox="0 0 24 24" width="18" height="18" fill="none"><rect width="24" height="24" rx="6" fill="#003087"/><path d="M11.2 8.2H15.2C17.2 8.2 18.4 9.3 18 11.4C17.6 13.7 16 14.9 13.8 14.9H12.1L11.4 18.8H8.9L10.7 8.6C10.7 8.4 10.9 8.2 11.2 8.2Z" fill="#38bdf8"/><path d="M8.6 5.8H13.4C15.6 5.8 16.9 7 16.5 9.2C16 11.7 14.3 13 11.8 13H9.8L8.9 17.8H6.1L8.1 6.2C8.1 6 8.4 5.8 8.6 5.8Z" fill="#fff"/></svg>`
};

const getPaymentMiniSvg = (paymentText) => {
  const text = String(paymentText || '').toLowerCase();
  if (text.includes('微信') || text.includes('零钱')) return miniSvgMap.wechat;
  if (text.includes('支付宝') || text.includes('花呗') || text.includes('余额宝')) return miniSvgMap.alipay;
  if (text.includes('paypal')) return miniSvgMap.paypal;
  return miniSvgMap.bank;
};
</script>

<style scoped>
.bill-page-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.num-font {
  font-family: var(--font-mono, monospace);
  font-variant-numeric: tabular-nums;
}

/* 1. 表面卡片统一容器 */
.surface-card {
  background: var(--bg-surface, var(--table-row-bg, #ffffff));
  border: 1px solid var(--border-subtle, var(--table-border-color, rgba(15, 23, 42, 0.08)));
  border-radius: 12px;
  box-shadow: var(--card-shadow, 0 4px 20px -4px rgba(15, 23, 42, 0.06));
}

/* 2. 顶部检索面板 */
.filter-panel {
  padding: 13px 16px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.filter-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.filter-group :deep(.el-input__wrapper),
.filter-group :deep(.el-select__wrapper),
.filter-group :deep(.el-date-editor.el-input__wrapper) {
  background-color: var(--bg-input, var(--el-fill-color-blank, #f8fafc)) !important;
  box-shadow: 0 0 0 1px var(--border-subtle, var(--table-border-color, rgba(15, 23, 42, 0.08))) inset !important;
  border-radius: 8px !important;
  transition: box-shadow 0.18s ease;
}

.filter-group :deep(.el-input__wrapper:hover),
.filter-group :deep(.el-select__wrapper:hover),
.filter-group :deep(.el-date-editor.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--border-strong, rgba(15, 23, 42, 0.14)) inset !important;
}

.filter-group :deep(.el-input__wrapper.is-focus),
.filter-group :deep(.el-select__wrapper.is-focused),
.filter-group :deep(.el-date-editor.is-active) {
  box-shadow: 0 0 0 1px var(--theme-primary, #1677ff) inset !important;
}

.kpi-strip {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 5px 14px;
  border-radius: 8px;
  background: var(--bg-elevated, var(--table-row-alt-bg, #f8fafc));
  border: 1px solid var(--border-subtle, var(--table-border-color, rgba(15, 23, 42, 0.08)));
  font-size: 12px;
  color: var(--text-sub, #64748b);
}

.kpi-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.kpi-val {
  font-size: 13px;
  font-weight: 700;
}

/* 3. 操作工具栏与按钮体系 */
.toolbar-panel {
  padding: 11px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 动态随主题色联动的按钮渐变体系 */
.btn-action {
  border-radius: 8px !important;
  font-weight: 600 !important;
  font-size: calc(13.5px * var(--app-font-scale, 1)) !important;
  padding: calc(8px * var(--app-font-scale, 1)) calc(14px * var(--app-font-scale, 1)) !important;
  height: calc(36px * var(--app-font-scale, 1)) !important;
  border: 1px solid transparent !important;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1) !important;
  margin: 0 !important;
  display: inline-flex;
  align-items: center;
  gap: calc(5px * var(--app-font-scale, 1));
}

.btn-action:hover {
  transform: translateY(-1px);
}

.btn-toolbar-search {
  background: linear-gradient(135deg, var(--theme-grad-start, #3b82f6) 0%, var(--theme-grad-end, #1d4ed8) 100%) !important;
  border-color: rgba(255, 255, 255, 0.14) !important;
  color: #ffffff !important;
  box-shadow: 0 3px 12px -2px rgba(var(--theme-rgb, 37, 99, 235), 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.22);
}

.btn-toolbar-search:hover {
  filter: brightness(1.08);
  box-shadow: 0 6px 18px -2px rgba(var(--theme-rgb, 37, 99, 235), 0.58), inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.btn-toolbar-reset {
  background: var(--bg-elevated, var(--table-row-alt-bg, #f8fafc)) !important;
  border-color: var(--border-strong, rgba(15, 23, 42, 0.14)) !important;
  color: var(--text-regular, #334155) !important;
}

.btn-toolbar-reset:hover {
  color: var(--text-main, #0f172a) !important;
  border-color: rgba(var(--theme-rgb, 37, 99, 235), 0.4) !important;
}

.btn-toolbar-create {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 3px 10px -2px rgba(16, 185, 129, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.btn-toolbar-create:hover {
  filter: brightness(1.07);
}

.btn-toolbar-delete {
  background: linear-gradient(135deg, #ef4444 0%, #b91c1c 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 3px 10px -2px rgba(239, 68, 68, 0.34), inset 0 1px 0 rgba(255, 255, 255, 0.16);
}

.btn-toolbar-delete:hover {
  filter: brightness(1.07);
}

.btn-toolbar-import {
  background: linear-gradient(135deg, #0ea5e9 0%, #0369a1 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 3px 10px -2px rgba(14, 165, 233, 0.36), inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.btn-toolbar-import:hover {
  filter: brightness(1.07);
}

.btn-toolbar-export {
  background: linear-gradient(135deg, #6366f1 0%, #4338ca 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 3px 10px -2px rgba(99, 102, 241, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.btn-toolbar-export:hover {
  filter: brightness(1.07);
}

/* 分段切换器（单行紧凑 / 双行聚合） */
.segmented-switch {
  display: inline-flex;
  padding: 3px;
  border-radius: 8px;
  background: var(--bg-elevated, var(--table-row-alt-bg, #f8fafc));
  border: 1px solid var(--border-subtle, var(--table-border-color, rgba(15, 23, 42, 0.08)));
  gap: 2px;
}

.segmented-btn {
  border: none;
  background: transparent;
  color: var(--text-sub, #64748b);
  font-size: calc(13px * var(--app-font-scale, 1));
  font-weight: 500;
  padding: calc(4px * var(--app-font-scale, 1)) calc(10px * var(--app-font-scale, 1));
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.16s ease;
  white-space: nowrap;
}

.segmented-btn:hover {
  color: var(--text-main, #0f172a);
}

.segmented-btn.is-active {
  background: var(--bg-surface, var(--table-row-bg, #ffffff));
  color: var(--text-main, #0f172a);
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.18);
}

html.dark .segmented-btn.is-active {
  background: #1e293b;
  color: #f8fafc;
}

/* 4. 无竖线金融账单大表排版 */
.bill-table-card {
  overflow: hidden;
}

.custom-bill-table {
  width: 100%;
  --el-table-border-color: var(--table-divider, rgba(15, 23, 42, 0.06));
  --el-table-bg-color: var(--table-row-bg, #ffffff);
  --el-table-tr-bg-color: var(--table-row-bg, #ffffff);
  --el-table-header-bg-color: var(--table-head-bg, #f8fafc);
  --el-table-row-hover-bg-color: var(--table-row-hover, #f1f6ff);
  background-color: transparent !important;
}

.custom-bill-table::before,
.custom-bill-table :deep(.el-table__inner-wrapper::before) {
  display: none !important;
}

.custom-bill-table :deep(.cell) {
  padding-left: 6px !important;
  padding-right: 6px !important;
}

.main-bill-table :deep(.cell) {
  padding-left: 5px !important;
  padding-right: 5px !important;
}

.custom-bill-table :deep(th.el-table__cell) {
  background: var(--table-head-bg, #f8fafc) !important;
  color: var(--text-sub, #64748b) !important;
  font-weight: 600 !important;
  font-size: calc(13.5px * var(--app-font-scale, 1)) !important;
  padding: calc(11px * var(--app-font-scale, 1)) 0 !important;
  border-bottom: 1px solid var(--border-subtle, rgba(15, 23, 42, 0.08)) !important;
  letter-spacing: 0.2px;
  white-space: nowrap;
}

.custom-bill-table :deep(td.el-table__cell) {
  background: transparent !important;
  color: var(--text-regular, #334155);
  font-size: calc(13.5px * var(--app-font-scale, 1)) !important;
  padding: calc(10px * var(--app-font-scale, 1)) 0 !important;
  border-bottom: 1px solid var(--table-divider, rgba(15, 23, 42, 0.06)) !important;
  transition: background-color 0.15s ease;
}

.custom-bill-table.is-double-table :deep(td.el-table__cell) {
  padding: calc(9.5px * var(--app-font-scale, 1)) 0 !important;
}

.custom-bill-table :deep(.el-table__row:nth-child(even)) {
  background-color: var(--table-row-stripe, #fafcff) !important;
}

.custom-bill-table :deep(.el-table__row:hover td.el-table__cell) {
  background-color: var(--table-row-hover, #f1f6ff) !important;
}

.custom-bill-table :deep(.el-table-fixed-column--right) {
  background-color: var(--table-row-bg, #ffffff) !important;
}

.custom-bill-table :deep(.el-table__row:nth-child(even) .el-table-fixed-column--right) {
  background-color: var(--table-row-stripe, #fafcff) !important;
}

.custom-bill-table :deep(.el-table__row:hover .el-table-fixed-column--right) {
  background-color: var(--table-row-hover, #f1f6ff) !important;
}

.custom-bill-table :deep(th.el-table-fixed-column--right) {
  background-color: var(--table-head-bg, #f8fafc) !important;
}

/* 时间容器与响应式排版 */
.cell-time-box {
  display: flex;
  align-items: center;
  gap: calc(6px * var(--app-font-scale, 1));
  font-size: calc(13px * var(--app-font-scale, 1));
  color: var(--text-sub, #64748b);
  white-space: nowrap;
  line-height: 1.35;
}

.cell-time-box .time-date {
  color: var(--text-regular, #334155);
  font-weight: 500;
}

.cell-time-box .time-clock {
  color: var(--text-sub, #64748b);
}

.cell-time-box.is-double-line {
  flex-direction: column;
  align-items: flex-start;
  gap: 1px;
  line-height: 1.28;
}

.cell-time-box.is-double-line .time-date {
  font-size: calc(13px * var(--app-font-scale, 1));
}

.cell-time-box.is-double-line .time-clock {
  font-size: calc(11.5px * var(--app-font-scale, 1));
  color: var(--text-muted, #94a3b8);
}

/* 柔和色点分类徽标 */
.cat-pill {
  display: inline-flex;
  align-items: center;
  padding: calc(2.5px * var(--app-font-scale, 1)) calc(8px * var(--app-font-scale, 1));
  border-radius: 6px;
  font-size: calc(12.5px * var(--app-font-scale, 1));
  font-weight: 500;
  line-height: 1.45;
  white-space: nowrap;
  border: 1px solid transparent;
}

.cat-pill.cat-food { background: rgba(249, 115, 22, 0.11); color: #fb923c; border-color: rgba(249, 115, 22, 0.2); }
.cat-pill.cat-transport { background: rgba(14, 165, 233, 0.11); color: #38bdf8; border-color: rgba(14, 165, 233, 0.2); }
.cat-pill.cat-shopping { background: rgba(236, 72, 153, 0.11); color: #f472b6; border-color: rgba(236, 72, 153, 0.2); }
.cat-pill.cat-finance { background: rgba(16, 185, 129, 0.11); color: #34d399; border-color: rgba(16, 185, 129, 0.2); }
.cat-pill.cat-health { background: rgba(244, 63, 94, 0.11); color: #fb7185; border-color: rgba(244, 63, 94, 0.2); }
.cat-pill.cat-utility { background: rgba(99, 102, 241, 0.11); color: #818cf8; border-color: rgba(99, 102, 241, 0.2); }
.cat-pill.cat-tech { background: rgba(6, 182, 212, 0.11); color: #22d3ee; border-color: rgba(6, 182, 212, 0.2); }
.cat-pill.cat-transfer { background: rgba(168, 85, 247, 0.12); color: #c084fc; border-color: rgba(168, 85, 247, 0.24); }
.cat-pill.cat-refund { background: rgba(245, 158, 11, 0.12); color: #fbbf24; border-color: rgba(245, 158, 11, 0.24); }
.cat-pill.tag-cat { background: rgba(148, 163, 184, 0.12); color: var(--text-regular, #334155); border-color: rgba(148, 163, 184, 0.2); }

html:not(.dark) .cat-pill.cat-food { color: #ea580c; }
html:not(.dark) .cat-pill.cat-transport { color: #0284c7; }
html:not(.dark) .cat-pill.cat-shopping { color: #db2777; }
html:not(.dark) .cat-pill.cat-finance { color: #059669; }
html:not(.dark) .cat-pill.cat-health { color: #e11d48; }
html:not(.dark) .cat-pill.cat-utility { color: #4f46e5; }
html:not(.dark) .cat-pill.cat-tech { color: #0891b2; }
html:not(.dark) .cat-pill.cat-transfer { color: #9333ea; }
html:not(.dark) .cat-pill.cat-refund { color: #d97706; }

/* 商户与商品说明容器 */
.cell-merchant-product {
  display: flex;
  align-items: center;
  gap: calc(7px * var(--app-font-scale, 1));
  line-height: 1.4;
  overflow: hidden;
  white-space: nowrap;
}

.cell-merchant-product .cell-title {
  font-weight: 600;
  color: var(--text-main, #0f172a);
  font-size: calc(14px * var(--app-font-scale, 1));
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
}

.cell-merchant-product .cell-divider {
  color: var(--text-muted, #94a3b8);
  opacity: 0.45;
  font-size: calc(12px * var(--app-font-scale, 1));
  user-select: none;
  flex-shrink: 0;
}

.cell-merchant-product .cell-sub {
  font-size: calc(12.8px * var(--app-font-scale, 1));
  color: var(--text-sub, #64748b);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.cell-merchant-product.is-double-line {
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  white-space: normal;
}

.cell-merchant-product.is-double-line .cell-divider {
  display: none;
}

.cell-merchant-product.is-double-line .cell-sub {
  font-size: calc(12px * var(--app-font-scale, 1));
  line-height: 1.35;
  white-space: nowrap;
  max-width: 100%;
}

.cleaned-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px calc(5px * var(--app-font-scale, 1));
  font-size: calc(11px * var(--app-font-scale, 1));
  font-weight: 600;
  border-radius: 4px;
  color: var(--theme-light, #60a5fa);
  background: rgba(var(--theme-rgb, 37, 99, 235), 0.14);
  border: 1px solid rgba(var(--theme-rgb, 37, 99, 235), 0.28);
  margin-left: 6px;
  cursor: help;
  line-height: 1.25;
  white-space: nowrap !important;
  flex-shrink: 0 !important;
}

html:not(.dark) .cleaned-tag {
  color: var(--theme-primary, #2563eb);
  background: rgba(var(--theme-rgb, 37, 99, 235), 0.09);
}

/* 收支类型徽标 */
.type-badge {
  display: inline-block;
  font-size: calc(12.5px * var(--app-font-scale, 1));
  font-weight: 600;
  padding: 1px calc(6px * var(--app-font-scale, 1));
  border-radius: 4px;
}

.type-badge.is-expense {
  color: #f87171;
  background: rgba(239, 68, 68, 0.1);
}

.type-badge.is-income {
  color: #34d399;
  background: rgba(16, 185, 129, 0.1);
}

html:not(.dark) .type-badge.is-expense {
  color: #dc2626;
}

html:not(.dark) .type-badge.is-income {
  color: #059669;
}

/* 核心金额 */
.amount-main {
  font-weight: 700;
  font-size: calc(14.5px * var(--app-font-scale, 1));
  letter-spacing: -0.3px;
  white-space: nowrap;
}

.amount-expense {
  color: #f87171;
}

.amount-income {
  color: #34d399;
}

html:not(.dark) .amount-expense {
  color: #e11d48;
}

html:not(.dark) .amount-income {
  color: #059669;
}

.amount-sub {
  font-size: calc(11.5px * var(--app-font-scale, 1));
  color: var(--text-muted, #94a3b8);
  margin-top: 1px;
}

/* 状态徽标与呼吸动效 */
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: calc(5px * var(--app-font-scale, 1));
  padding: calc(2.5px * var(--app-font-scale, 1)) calc(8px * var(--app-font-scale, 1));
  border-radius: 999px;
  font-size: calc(12px * var(--app-font-scale, 1));
  font-weight: 600;
  line-height: 1.3;
  white-space: nowrap;
  cursor: default;
}

.status-pill .status-dot {
  width: calc(6.5px * var(--app-font-scale, 1));
  height: calc(6.5px * var(--app-font-scale, 1));
  border-radius: 50%;
  flex-shrink: 0;
}

@keyframes status-pulse-green {
  0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.65); }
  70% { box-shadow: 0 0 0 4.5px rgba(16, 185, 129, 0); }
  100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

@keyframes status-pulse-amber {
  0% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.7); }
  70% { box-shadow: 0 0 0 4.5px rgba(245, 158, 11, 0); }
  100% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0); }
}

@keyframes status-pulse-red {
  0% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7); }
  70% { box-shadow: 0 0 0 4.5px rgba(239, 68, 68, 0); }
  100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
}

.status-pill.status-success {
  background: rgba(16, 185, 129, 0.12);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.24);
}

.status-pill.status-success .status-dot {
  background: #10b981;
  animation: status-pulse-green 2.2s ease-out infinite;
}

.status-pill.status-warning {
  background: rgba(245, 158, 11, 0.12);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.26);
}

.status-pill.status-warning .status-dot {
  background: #f59e0b;
  animation: status-pulse-amber 2.2s ease-out infinite;
}

.status-pill.status-danger {
  background: rgba(239, 68, 68, 0.12);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.24);
}

.status-pill.status-danger .status-dot {
  background: #ef4444;
  animation: status-pulse-red 2.2s ease-out infinite;
}

html:not(.dark) .status-pill.status-success { color: #059669; }
html:not(.dark) .status-pill.status-warning { color: #d97706; }
html:not(.dark) .status-pill.status-danger { color: #dc2626; }

/* 支付方式徽标与 Mini SVG 图标 */
.payment-cell-badge {
  display: inline-flex;
  align-items: center;
  gap: calc(5px * var(--app-font-scale, 1));
  font-size: calc(13px * var(--app-font-scale, 1));
  color: var(--text-regular, #334155);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.payment-mini-icon {
  width: calc(20px * var(--app-font-scale, 1));
  height: calc(20px * var(--app-font-scale, 1));
  border-radius: 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 操作链接按钮 */
.table-action-group {
  display: inline-flex;
  align-items: center;
  gap: calc(6px * var(--app-font-scale, 1));
  white-space: nowrap;
}

.action-link-btn {
  display: inline-flex;
  align-items: center;
  gap: calc(3px * var(--app-font-scale, 1));
  font-size: calc(13px * var(--app-font-scale, 1));
  font-weight: 500;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: calc(3px * var(--app-font-scale, 1)) calc(6px * var(--app-font-scale, 1));
  border-radius: 5px;
  transition: all 0.15s ease;
}

.action-link-btn.is-edit {
  color: var(--theme-light, #2563eb);
}

html:not(.dark) .action-link-btn.is-edit {
  color: var(--theme-primary, #2563eb);
}

.action-link-btn.is-edit:hover {
  background: rgba(var(--theme-rgb, 37, 99, 235), 0.14);
}

.action-link-btn.is-del {
  color: #ef4444;
}

.action-link-btn.is-del:hover {
  color: #f87171;
  background: rgba(239, 68, 68, 0.12);
}

/* 分页容器与暗色模式适配 */
.table-pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: calc(12px * var(--app-font-scale, 1)) calc(18px * var(--app-font-scale, 1));
  background: var(--bg-surface, var(--table-row-bg, #ffffff));
  border-top: 1px solid var(--border-subtle, var(--table-divider, rgba(15, 23, 42, 0.06)));
  flex-wrap: wrap;
  gap: calc(12px * var(--app-font-scale, 1));
}

.pagination-info {
  font-size: calc(13px * var(--app-font-scale, 1));
  color: var(--text-sub, #64748b);
}

.pagination-info strong {
  color: var(--text-main, #0f172a);
  font-weight: 600;
}

html.dark :deep(.el-pagination.is-background .el-pager li),
html.dark :deep(.el-pagination.is-background .btn-prev),
html.dark :deep(.el-pagination.is-background .btn-next) {
  background-color: #141d31 !important;
  color: #94a3b8 !important;
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 6px !important;
}

html.dark :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: linear-gradient(135deg, var(--theme-grad-start, #3b82f6), var(--theme-grad-end, #1d4ed8)) !important;
  color: #ffffff !important;
  border-color: transparent !important;
}

.empty-table-placeholder {
  padding: calc(36px * var(--app-font-scale, 1)) calc(16px * var(--app-font-scale, 1));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: calc(6px * var(--app-font-scale, 1));
}

.empty-table-title {
  font-size: calc(15px * var(--app-font-scale, 1));
  font-weight: 600;
  color: var(--text-regular, #334155);
}

.empty-table-sub {
  font-size: calc(13px * var(--app-font-scale, 1));
  color: var(--text-sub, #64748b);
}
</style>
