# 管理端（Vue 3 + Element Plus）

校园二手书系统中供管理员使用的 Web 后台，对应选题报告里的「管理端」部分。

## 启动

```powershell
cd admin-web
npm install
npm run dev      # 开发模式，http://127.0.0.1:5173
npm run build    # 打包到 dist/
```

依赖后端服务运行：
```powershell
& 'E:\dev\_scripts\backend-start.ps1'    # Spring Boot :8080
& 'E:\dev\_scripts\algo-start.ps1'       # Flask      :5001
```

## 账号

| 账号 | 密码 |
|---|---|
| admin | admin123 |

## 页面

| 路由 | 说明 |
|---|---|
| /login | 管理员登录 |
| /dashboard | 数据大屏（深色）：4 张 KPI 卡 + 分类环形图（可点击剔除）+ 14 天趋势面积图 + 双仪表盘 + 价位/专业分布 + 关键指标 |
| /books | 书籍审核：按状态筛选、关键词搜索、通过 / 驳回（常用原因下拉）/ 下架 |
| /users | 用户管理：搜索、封禁 / 临时封禁 / 禁言 / 解除，状态列显示到期时间 |
| /orders | 订单管理：按状态筛选、查看成交明细 |
| /banners | 广告管理：向小程序投放首页轮播，支持位置/排序/投放时间/启停 |
| /ai | 运营助手：基于实时统计的问答，可配置大模型 |

## 新增能力说明

### 封禁与禁言

- **封禁**：立即生效，小程序登录会被拒绝并提示到期时间；支持 1/3/7/30 天或永久。
- **禁言**：为即将上线的小程序聊天功能预留，管理端先落库（`mute_status` / `mute_until`）。
- 到期自动解除：登录时由 `UserService.decorate()` 顺带复位，校园量级不需要定时任务。

### 广告位

- 数据表 `banner`，字段包含位置、跳转类型与目标、排序、起止时间、曝光/点击统计。
- 管理端 CRUD，学生端通过 `GET /api/public/banners?position=home_banner` 拉取，
  后端已过滤「未启用」和「不在投放时间窗内」的记录。

### 运营助手

- 默认走**本地规则分析**：把数据库统计口径翻译成自然语言，并把推荐算法的
  Recall@10 指标一起带出来，断网也能用。
- 在 `application.yml` 的 `app.ai` 下填好 `base-url` 与 `api-key` 后自动切换为
  大模型回答（OpenAI 兼容协议），调用失败会回退本地分析，页面不会报错。

## 结构

```
src/
├─ api/          axios 封装与接口定义
├─ components/   EChart 图表封装
├─ layout/       侧边栏 + 顶栏布局
├─ router/       路由与登录守卫
├─ store/        登录态（localStorage 持久化）
├─ styles/       全局样式
└─ views/        各页面
```

## 设计说明

- **数据大屏单独走深色**：`/dashboard` 路由下主区切换为深色背景并去掉内边距，
  由 `AdminLayout` 根据 `route.path` 判断；其余页面保持浅色工作区，两者共用同一套侧栏与顶栏。
- **图表配色**：青色 #22d3ee / 紫色 #a78bfa / 翠绿 #34d399 / 琥珀 #fbbf24，
  在深底上对比度足够，投影到答辩教室的投影仪也不会糊成一团。
- **ECharts 渐变**直接用 `{ type: 'linear', colorStops: [...] }` 对象，
  不依赖 `echarts.graphic`，避免额外引入。

## 两个实现上的选择

1. **路由用 hash 模式**：打包后直接双击 dist/index.html 也能跑，答辩现场不用额外起静态服务器。
2. **开发期用 Vite proxy 转发 /api**：避免跨域配置，打包部署时改由 Nginx 或后端托管。
