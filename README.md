# 星界 (Star Realm) — 智能个人财务管理平台

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](./LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-4FC08D.svg)](https://vuejs.org/)

**星界 (Star Realm)** 是一款融合 **DeepSeek 大语言模型智能账单诊断**、**多维 ECharts 可视化收支看板**、**四层充能目标存钱计划** 与 **实时外汇换算** 的全栈个人财务管理平台。

---

## ✨ 核心特性

- **🎨 五套主题配色「随心配」与明暗聚光灯切换**：内置内联自适应矢量 SVG Logo，支持极客蓝、青翠绿、星云紫、珊瑚橙、琥珀金 5 套主题色及 View Transitions 聚光灯明暗动画。
- **🤖 DeepSeek 智能财务顾问**：
  - 采用 **用户自备专属 API Key 模式**（个人中心 AES 密文存储），无需运营方承担公用 Token 算力成本，隐私安全可控。
  - 支持 SSE 流式平滑打字机输出、Markdown 表格与代码块一键复制、饼图悬浮景深预览。
  - 对话历史与活跃会话支持 **云端数据库 + 本地双轨持久化**。
  - 内置前后端多层反破甲安全护栏（**AES-128 密文封装**规避云端关键词扫描，运行时内存同步解密）与 24 小时违规监测，支持阶梯式 AI 对话限制（24小时 / 3天 / 1个月 / 永久）及管理员端统一审核解除/降级。
- **⚡ 目标存钱计划（四层充能进度条）**：支持 0%~400% 四层动态配色与 Canvas 电弧火花粒子特效，到期自动锁定归档。
- **🔒 全链路隐私与凭证安全**：
  - API Key 采用 AES-128 对称加密落盘，前端全链路脱敏。
  - 内部账号（新管理员/新普通用户）创建生成初始凭证并支持带提示的一键复制，首次登录强制改密闭环，具备**后端拦截器级防 F12 绕过硬防护**与退出登录操作。
  - 支持 7 天免签登录 + 后端 `HttpOnly` Cookie 双轨鉴权、主题偏好 Cookie 跨会话记忆及协议版本号绑定 Cookie。

---

## 🛠️ 技术栈

- **后端 (`springboot/`)**：Spring Boot 3 + MyBatis + PageHelper + MySQL 8 + `java-jwt` + Hutool + Apache POI
- **前端 (`vue/`)**：Vue 3 (Composition API) + Vite + Vue Router 4 + Element Plus + ECharts 5 + `markdown-it`

---

## 🚀 快速启动指南

### 1. 初始化数据库
1. 创建 MySQL 数据库 `personal_fi_plat`（字符集 `utf8mb4`）。
2. 导入初始化脚本：`sql/personal_fi_plat.sql`。

### 2. 配置后端环境变量或私有配置
后端支持通过系统环境变量或 `springboot/src/main/resources/application-local.yml`（已被 `.gitignore` 忽略，不会上传至 Git）配置私有参数：

| 环境变量名 | 说明 | 默认值 |
| :--- | :--- | :--- |
| `SERVER_PORT` | 后端服务端口 | `8080` |
| `MYSQL_URL` | MySQL JDBC 连接串 | `jdbc:mysql://localhost:3306/personal_fi_plat?...` |
| `MYSQL_USERNAME` | 数据库用户名 | `root` |
| `MYSQL_PASSWORD` | 数据库密码 | `root` |
| `STAR_REALM_AES_KEY` | API Key 落盘加密 16 位密钥 | 默认开发密钥 |

启动后端：
```bash
cd springboot
mvn spring-boot:run
```

### 3. 启动前端
```bash
cd vue
npm install
npm run dev
```

---

## 📄 开源协议 (License)

本项目基于 **[Apache License 2.0](./LICENSE)** 开源协议发布。

Copyright © 2026 **Star Realm Team**. All rights reserved.
