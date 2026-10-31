# 微信小程序 · 学生端

选题报告里要求的原生小程序实现，技术栈 WXML / WXSS / JS，不依赖第三方框架。

## 打开方式

1. 打开 **微信开发者工具**，选择「导入项目」，目录选 `miniprogram/`
2. AppID 选「测试号」或直接使用 `project.config.json` 里的 `touristappid`（游客模式）
3. 详情 → 本地设置 → 勾选 **不校验合法域名**（后端是 http://127.0.0.1:8080）
4. 编译后即可预览；真机调试时把 `app.js` 里的 `baseUrl` 改成电脑的局域网 IP

## 目录

```
miniprogram/
├─ app.js / app.json / app.wxss     全局配置与样式
├─ utils/
│  ├─ request.js                    wx.request 封装（带 token、统一错误）
│  └─ api.js                        接口集中管理
├─ components/book-card/            书籍卡片组件（首页/检索/我的共用）
└─ pages/
   ├─ home/      首页：轮播、分类、推荐、最新
   ├─ search/    检索：关键词 + 排序 + 筛选 + 匹配度
   ├─ detail/    详情：书况四维、卖家、相似书、预约自提
   ├─ publish/   发布：书况表单 + 智能定价
   ├─ orders/    订单：买入/卖出、状态流转、评价
   ├─ me/        我的：资料、统计、发布与收藏
   └─ login/     演示登录
```

## 演示账号

| code | 说明 |
|---|---|
| demo-002 | 买家，有浏览记录，推荐更准 |
| demo-010 | 卖家，已发布多本闲置 |
| demo-030 | 新用户，走冷启动 |

真实环境应改为 `wx.login` 取 code 再换 openid；演示模式直接映射到种子账号，
这样不需要 AppID 和 AppSecret 也能完整走通流程。

## 验证方式

本机没有可用的微信开发者工具登录态，因此用 Node 模拟 `wx` 运行时，
把页面逻辑真实执行一遍并打真实接口（见 `E:\dev\_scripts\mp_harness.js`），
覆盖登录 / 首页 / 检索 / 详情 / 定价 / 我的 / 订单 七个页面的数据流。
视觉呈现需要在开发者工具里再确认一次。
