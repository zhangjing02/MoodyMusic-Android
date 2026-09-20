# 🎵 音信 (MoodyMusic / TunePost) — Android 客户端

> 一个基于情绪共鸣、原声手札与同学互动场景的现代原生 Android 音乐流媒体应用。
> 采用 **Jetpack Compose + 服务端驱动 UI (SDUI) + 现代响应式架构 (MVI/MVVM) + Haze GPU 实时硬件渲染**。

---

## 🌟 核心架构与技术亮点

### 1. 现代化 Jetpack Compose 全量重构
- **淘汰传统 XML**：从旧 DataBinding 布局全面重构为现代声明式 UI，设计风格采用 **Stitch 极简留白与精细线框（Hairline Border）** 质感。
- **动态呼吸动效**：基于 `animateFloatAsState` 与自定义贝塞尔插值器实现黑胶旋转、微弱光影呼吸与丝滑状态切换。

### 2. 服务端驱动 UI (Block-Based SDUI)
- 首页采用完全动态切片流渲染，由边缘 Worker API (`/api/home/feed`) 动态下发模块配置：
  - `hero_banner`：大画幅专题海报与黑胶唱片联动
  - `editor_picks`：2×2 留声精选专辑网格
  - `artist_row`：圆形关注歌手横向滚动条
  - `track_list`：今日单曲试听与试听播放控制器
  - `image_feature`：全屏杂志摄影视觉大图
  - `album_cards`：典藏专辑卡片
- 客户端基于多态密封模型与 `LazyColumn` 动态构建组件树，服务端可实时下发排版布局，无需发版。

### 3. 极致高刷性能调优 (60/120Hz 极速体验)
- **消除监控工具观察者效应**：将帧率监控从主线程 `Looper` 剥离至专属 `HandlerThread`，彻底消除了每帧 5~10ms 的排版卡顿假象。
- **细粒度 SDUI 分帧解构**：将首页单体大块 SDUI 模块平铺解构为独立 LazyColumn 条目，单帧 Composition 耗时由 **45.4ms 直降至 0.6ms~1.2ms（提升 97%）**。
- **GPU 实时毛玻璃与硬件加速**：引入 Compose 原生生态标杆 `dev.chrisbanes.haze:haze` 替代引起 17ms 布局死锁的传统 `BlurView`，实现 GPU Shader 硬件级 120fps 晶莹磨砂效果。
- **动态 Dock 悬浮底栏**：基于 `NestedScrollConnection` + `graphicsLayer { translationY }` 实现上滑下潜、下滑浮现动效，0 布局测量与重排损耗。

### 4. 全局单一数据源 (Single Source of Truth) 与一键换域
- **彻底根除硬编码 URL**：所有 UI 组件、ViewModel 和 Mock 兜底数据均规范为纯相对路径（如 `/storage/...`），不再散落任何具体域名。
- **编译时环境变量驱动**：
  - 在 `gradle.properties` 中统一定义 `MOODY_API_BASE_URL`；
  - 由 AGP `buildConfigField` 自动注入到 `BuildConfig.API_BASE_URL`；
  - 由全局配置中心 `AppConfig.kt` 统一下发基准地址与相对资源解析器；
  - `SongbookImage` 遇相对路径自动委托 `AppConfig.resolveUrl()` 动态拼接；
- **换域成本**：更换域名仅需修改 `gradle.properties` 的 1 行，或通过根目录脚本一键全自动切换。

---

## 📱 核心业务功能

1. **音乐内容与深度专题**
   - 动态首页（Home Feed / SDUI 智能分发）
   - 发现音乐（Discover / 字母索引目录与流派筛选）
   - 专辑与艺术家详情（Album & Artist Detail）
2. **教室座位认领与登录系统 (Classroom Claim)**
   - 64 座位矩阵展示与状态感知（已认领 / 待认领）
   - 三道安全问题防伪验证与身份绑定
   - 登录凭据本地加密留存与自动恢复
3. **专辑社交与班级隔离 (Album Social)**
   - 班级级别数据隔离，防止未经授权的跨班级访问
   - 主贴发布与平铺评论互动
   - JPush 极光透传驱动脏标记（Dirty Flag）静默/前台局部刷新

---

## 🛠 技术栈

| 维度 | 选用技术 / 库 |
| :--- | :--- |
| **语言与核心** | Kotlin `2.1.0` + Kotlin Coroutines & Flow `1.9.0` |
| **构建系统** | Gradle 8.10.x + Android Gradle Plugin `8.10.1` |
| **UI 框架** | Jetpack Compose (BOM `2024.09.00`) + Material 3 |
| **实时渲染** | Chris Banes `Haze` (GPU Shader 实时毛玻璃) |
| **网络层** | Retrofit `2.11.0` + OkHttp `4.12.0` + Gson |
| **图片加载** | Coil Compose `2.7.0` (硬件位图 + 内存直通缓存) |
| **消息推送** | JPush SDK `5.9.2` (Tag 细粒度分发 + 离线脏标记) |
| **架构规范** | 单一配置源 (Single Source of Truth) + 响应式 MVI/MVVM |

---

## 🚀 构建与运行

### 环境要求
- **JDK**：17 或更高版本
- **Android SDK**：Compile SDK 36 / Min SDK 24 / Target SDK 36
- **NDK**：建议安装 `27.0.12077973`（用于 Release 混淆构建）

### 常用 Gradle 命令

```bash
# 仅编译 Kotlin 模块（快速静态语法校验）
./gradlew :app:compileDebugKotlin

# 构建 Debug APK
./gradlew :app:assembleDebug

# 运行基础库构建
./gradlew :commonbase:assembleDebug

# 构建 Release 正式安装包
./gradlew :app:assembleRelease
```

---

## 📂 项目模块结构

```text
MoodyMusicForAndroid/
├── app/
│   ├── src/main/java/com/example/moodymusicforandroid/
│   │   ├── ui/
│   │   │   ├── home/           # 首页 Compose、SDUI 组件流与发现页
│   │   │   ├── album/          # 专辑详情页与黑胶唱片动效
│   │   │   ├── artist/         # 艺术家档案与作品集
│   │   │   ├── classroom/      # 教室座位认领与互动页面
│   │   │   ├── components/     # SongbookImage 等全局 Compose 组件
│   │   │   └── theme/          # Compose 颜色、字体排印与 Material3 主题
│   │   ├── receiver/           # JPush 极光广播接收器（防下线风暴与离线冲刷）
│   │   └── MoodyMusicApplication.kt
│   └── build.gradle.kts
├── commonbase/                 # 公共基础库
│   ├── src/main/java/com/example/moodymusicforandroid/
│   │   ├── common/
│   │   │   ├── config/         # AppConfig 全局配置中心 (Single Source of Truth)
│   │   │   ├── network/        # RetrofitClient、统一脱敏与拦截器
│   │   │   └── utils/          # PinyinUtils (生僻字字典)、ToastUtils (防排队)
│   │   └── data/model/         # 数据模型与 HomeBlock SDUI 多态解析
│   └── build.gradle.kts
└── gradle.properties           # 全局配置中心 (MOODY_API_BASE_URL)
```

---

## 🛡️ 客户端数据访问、错误脱敏与体验优化铁律 (Client Architecture & Guidelines)

### 1. 严格禁止发起无参全量曲库扫描请求
- **按需分层拉取**：客户端只能通过骨架接口 `GET /api/skeleton` 获取轻量艺人名单；点击具体艺人时，必须携带具体 ID 调用 `GET /api/songs?artistId=db_xxx` 按需加载对应作品。
- **杜绝全量拉取**：严禁客户端任何界面直接请求无参的 `/api/songs`，避免造成 D1 数据库 15,000+ 行全表扫描消耗每日配额。

### 2. 底层基础设施异常全局脱敏净化 (Error Sanitization)
- **拦截技术堆栈穿透**：当云端由于配额耗尽、锁表或网络波动抛出 `D1_ERROR`、`sqlite`、`row read limit`、`HTTP 5xx` 等技术错误时，严禁将长串英文原始报错展示给用户；
- **全链路收敛**：在 `BaseViewModel` 的 `handleResponse` / `handleError` 以及 `ToastUtils.showShort` 中均内置安全净化引擎，遇技术异常统一展示友好中文：
  > **「服务器异常，请稍后重试」**

### 3. 单点登录互踢风暴防护与通知清理机制
- **登录即清空历史残留**：新设备登录成功时，调用 `JPushInterface.clearAllNotifications()` 清理系统通知栏历史残留；
- **毫秒级时间戳比对**：端侧记录精确登录毫秒时间戳 `loginTimestamp`，拦截丢弃任何早于当前登录时刻的离线积压互踢消息；
- **系统 Toast 防排队与防抖**：`ToastUtils` 在展示前主动 `cancel()` 上一个系统排队 Toast，互踢相关提示实施 6 秒严格防抖，彻底告别疯狂弹窗排队的吓人现象。

### 4. 汉字拼音分组生僻字与多音字映射规范
- **GB2312 编码陷阱**：GB2312 编码仅一级常用字（3755 个）按拼音排序；如“庾”（`0xE2D7`）与“窦”（`0xF1BC`）等二级汉字属于偏僻/生僻字区，编码大于 `0xD7F9`，无法通过编码区间推导拼音。
- **扩展字典维护**：所有音乐人特殊姓氏与二级字必须在 `PinyinUtils.SPECIAL_SURNAMES` 中集中维护映射（如庾->Y、窦->D、臧->Z、邰->T、岑->C、裘->Q、郁->Y、邝->K、那->N），确保 A-Z 字母索引导航 100% 准确归位。
