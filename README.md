# 校园二手书供需匹配与智能定价系统

面向高校的二手教材流转平台。学生拍照发布闲置教材，系统给出**定价参考**、把书**推荐给可能需要的同学**，并支持预约、聊天、评价、举报的完整闭环。

四个客户端 + 一个算法服务：

| 模块 | 技术栈 | 端口 | 说明 |
|---|---|---|---|
| `backend` | Spring Boot 3.3 + MyBatis-Plus + MySQL + Redis | 8080 | 主业务接口 |
| `algorithm` | Python 3.13 + scikit-learn + Flask | 5001 | 定价回归 + 协同过滤推荐 |
| `miniprogram` | 微信原生小程序 | — | 学生端（主演示载体） |
| `student-h5` | Vue 3 + Vite | 5174 | 学生端网页版，与小程序功能同步 |
| `admin-web` | Vue 3 + Element Plus + ECharts | 5173 | 管理后台 |

---

## 一、怎么跑起来

### 1. 准备环境

- JDK 17
- MySQL 8.0（库名 `campus_book`，默认账号 `root` / `123456`）
- Redis 5+（默认 6379，无密码）
- Python 3.11+（算法服务）
- Node.js 18+（两个前端）

### 2. 初始化数据库

按顺序执行 `db/` 目录下的脚本：

```
01_schema.sql       建表
02_seed.sql         基础字典（专业、课程、分类）
03 ~ 10_migrate_v*.sql   按版本号依次执行
```

### 3. 启动

**Windows 一键启动**（推荐，重复执行安全）：

```powershell
.\一键启动.cmd
```

停止服务运行 `停止服务.cmd`。

**手动启动**：

```bash
# 后端
cd backend && mvn spring-boot:run

# 算法服务
cd algorithm && pip install -r requirements.txt && python app.py

# 管理后台
cd admin-web && npm install && npm run dev

# 学生端 H5
cd student-h5 && npm install && npm run dev
```

### 4. 打开

| 入口 | 地址 | 账号 |
|---|---|---|
| 管理后台 | http://127.0.0.1:5173 | `admin` / `admin123` |
| 学生端 H5 | http://127.0.0.1:5174 | 任意学号登录，或演示账号 |
| 接口文档 | http://127.0.0.1:8080/swagger-ui/index.html | — |
| 小程序 | 用微信开发者工具打开 `miniprogram/` | 同上 |

小程序使用的是测试号（`touristappid`），需要在开发者工具里勾选「不校验合法域名」。

---

## 二、核心功能

### 学生端

- **发布**：拍照上传、填写书况，系统给出参考价与建议区间
- **找书**：关键词 / 拼音首字母（`gdsx` → 高等数学）、分类、价格、品相筛选
- **推荐**：ItemCF + 内容特征 + 热度融合，按专业和浏览历史个性化排序
- **交易**：预约自提 → 卖家确认 → 线下交付 → 买家完成 → 互相评价
- **沟通**：站内聊天，带常用语快捷回复
- **信用**：发布免审核、首页优先等权益随信用分解锁

### 管理端

- 数据大屏、书籍审核、用户管理（封禁/禁言/改昵称/调信用分）
- 举报处理（含证据链：是否购买过、是否聊过、历史被举报次数）
- 广告位管理、运营助手（AI 问答）
- Excel 导出（用户、订单）

### 算法

| 模型 | 指标 | 结果 |
|---|---|---|
| 定价回归（RandomForest） | MAE | 3.94 元（原价的 6.83%） |
| | 对比基线线性回归 | 24.06% → 6.83% |
| 推荐融合 | Recall@10 | 0.4322 |
| | 相比纯 ItemCF | +406% |

---

## 三、项目结构

```
.
├── backend/          Spring Boot 服务
│   └── src/main/java/com/campusbook/
│       ├── controller/   接口层
│       ├── service/      业务逻辑
│       ├── mapper/       MyBatis-Plus 数据访问
│       ├── entity/       实体
│       ├── dto/          请求/响应对象
│       ├── config/       拦截器、定时任务、配置
│       └── common/       统一响应、异常、工具类
├── algorithm/        算法服务
│   ├── src/          price_model / cf / features
│   ├── models/       训练产物（joblib）
│   └── app.py        Flask 入口
├── miniprogram/      微信小程序
│   ├── pages/        首页、搜索、详情、发布、订单、我的、聊天…
│   ├── components/   书卡等复用组件
│   └── utils/        请求封装、资源地址处理
├── student-h5/       Vue3 学生端
├── admin-web/        Vue3 管理后台
├── db/               建表与增量迁移脚本
├── docs/             文档
└── scripts/          启动/停止脚本
```

---

## 四、开发约定

- **接口统一返回 HTTP 200 + `{code, message, data}`**，判断成功看 `code === 0`，不要看 HTTP 状态码
- 时间格式统一 `yyyy-MM-dd HH:mm:ss`
- 上传文件放 `backend/uploads/`，返回相对路径，前端自行拼服务地址
- 数据库变更一律新增 `db/NN_migrate_vN.sql`，不改历史脚本
- 前端两套学生端（小程序 / H5）功能要保持同步，改一边必须改另一边

---

## 五、测试

```bash
# 后端单元测试
cd backend && mvn test

# 接口巡检（需服务已启动）
python scripts/qa_api.py

# 小程序静态校验
python scripts/mp_validate.py
```

---

## 六、说明

- 本项目为毕业设计作品，书籍封面图片来自公开网络，仅用于教学演示，不随代码分发
- 演示环境未做部署，所有服务跑在本机
