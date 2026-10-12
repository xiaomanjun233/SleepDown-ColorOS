# SleepDown ColorOS 组件框架

本框架服务 SleepDown ColorOS 派生版，入口为 `coloros/ProvideSleepDownColorOs`。组件源文件已从 `feature/experimental/colorosui` 移到独立的 `coloros/` 目录；课程管理 Activity 的旧类名保留，避免改变 Manifest、Intent 和转场路由。

## 装配和边界

- `MainActivity.kt` 的公共 `AppTheme` 安装框架；共用设计系统通过 `NativeAlertRenderer`、`NativeCapsuleButtonRenderer`、`NativeCourseCardRenderer` 接口调用，不依赖具体业务 feature。
- `ColorOsNativeUi` 管理系统 APK、类加载器、资源、材质状态与跟手光；`ColorOsContext` 负责配对的主题 Context。
- `ColorOsCapsuleButton`、`ColorOsDialogs`、`ColorOsSearchField` 是共用组件；Dock、首页按钮、菜单及课程卡适配也集中在框架中。
- 不复制或打包系统 COUI 实现；运行时读取已安装、经过系统应用标志检查的 Settings / Contacts / FileManager。当前能力还受渠道、设备厂商和系统 API 支持约束，缺少组件时返回共用 Compose 实现。
- `SLEEPDOWN_EXPERIMENTAL_FEATURES` 仍是发行渠道开关；框架提取不意味着这些私有厂商接口在所有 Android 上可用。

## 胶囊按钮

既有 `LiquidButton` 是兼容入口：可点击按钮优先交给 `ColorOsCapsuleButton`，成功后直接返回，之前的 Kyant 按压、拖拽形变与高光状态不会创建。`DialogLiquidButton` 和 Alert 操作按钮直接使用同一个渲染接口，包括没有 Backdrop 的场景。

原生 `COUIButton` 负责材质、圆角、触摸、滑动取消、跟手光和触感；Compose 保留图标、文字、加载内容及原有尺寸。外层合并按钮语义并处理键盘激活，原生空文本 View 不重复暴露给无障碍。业务显式禁用状态传递给原生控件。

Compose 内容直接绘制在 COUIButton 上方，保持文字、图标和加载状态的正常更新。`ColorOsCapsuleMotionBinding` 订阅 COUIButton 自身的 `COUITouchMotionAnimator`，从同一个 `ContributeSink` 取得位移与缩放；Row 只在 graphicsLayer 块中读取这些值，不在组合阶段读取，也不创建第二套弹簧。销毁时解除回调。旧的 ViewOverlay 跨 Canvas 录制方案已因实机文字消失撤回。

普通按钮使用系统 secondary 材质，彩色操作使用 primary 材质并沿用操作颜色。primary 仅替换系统预设的主题颜色层，保留系统模糊半径、混合模式和禁用透明度；确认按钮指定应用主题色与白字。跟手光沿用系统默认光色和当前 2× 强度。

`clickTargetEnabled = false` 的弹层外壳、输入区域和共享手势表面不是普通按钮，不会被该入口变成可点击胶囊。它们继续使用各自的几何、采样与手势宿主。

覆盖共用入口后，现有设置、导入、AI 助理、教务浏览、课表管理、多课表、课程编辑及部分快捷操作中的按钮无需分别维护 COUI 生命周期。

## 弹窗操作顺序

`LiquidAlertActionStyle.Cancel` 表达取消/继续编辑语义，不按显示文本判断。COUI 原生纵向按钮顺序是 neutral → positive → negative，因此取消映射到 negative、推荐确认映射到 positive，剩余操作映射到 neutral；业务回调仍按原始 action 索引绑定。Compose 三排后备布局同样将 Cancel 排到最后。

窗口位置、材质、进入退出动画仍由 COUI 默认弹窗负责。关闭后的动作在系统退出完成后派发；表单的脏数据保护、校验与保存语义保持在业务层。

## 首页色彩和采样

- 加号保留主题色底，图标固定白色。形变起点匹配原生 FAB 的不透明主题层，向中性菜单表面连续插值。
- 展开的菜单与形变目标使用同一个中性色函数，避免壁纸亮度判断和菜单主题判断在交接处产生跳色。
- 三点菜单在系统列表完成绑定后，同时设置 TextView、ImageView 与复合图标的前景色，避免原生适配器重新套用旧图标 tint。
- Dock 和加号仍位于完整首页快照内；框架迁移不把它们移到根页面上方。完整采样、临时变换和快照的归属仍由页面宿主管理。
- 壁纸主题色结合 Dock 区域实际可见背景亮度调整明度；深背景使用较亮主题色，浅背景提高对比度。加号底色单独保证白色图标对比度，形变起点与实际加号底色一致。
- 原生 Dock 的后备内容通过稳定 State 传入，避免未使用的 Compose 后备函数捕获背景状态后改变原生宿主参数。shortcut 的整页模糊保留，动画进度只由绘制层读取；这一隔离不等同于实机重组次数已验证。

## 课程 shortcut

课程长按菜单复用右上角菜单的 `NativeOverflowMenu` / `COUIPopupListWindow`，由课程位置对应的透明 View 提供锚点。原生窗口负责面板材质、触摸和进入退出动画；背景仍由首页完整快照执行整页模糊，原生 Dock 处在同一快照内。编辑、复制与删除继续走原业务回调，移除项保留红色。

关闭时等待原生窗口和首页背景退出均完成，再执行后续操作；reset、页面退出和拖拽接管会取消待执行动作。系统原生能力不可用时使用既有 Compose shortcut。

## 课程凝光与渐变模糊

课程凝光保留课程渐变色与默认 50% 着色，模糊沿用内容通透预设。描边改用 Dock 的 RenderNode 预设 `TYPE_FRAMEWORK_CAPSULE_3`；跟手光改用 `TYPE_OPAQUE_MEDIUM_2` 与当前 Dock 的 2× 强度，颜色保持系统默认。课程形状仍由课程自身 outline 提供，不套用 Dock 的胶囊圆角。个性化面板只保留开关与着色滑杆，不显示模式说明文字。

所有公共 `ProgressiveBackdropBlur` / `progressiveBackdropBlur` 入口通过 `LocalNativeProgressiveBlur` 使用系统 `OplusRenderEffect.createGradientBlurEffect`。模糊效果接入现有 glass 背景采样链，沿用页面半径、方向、透明度包络、快照及坐标归属，速度取系统 `AppBarBlurHelper` 默认参数。顶部、底部和侧边入口共用这一路线，侧边字母栏保留顶部/底部消隐。

不再调用 `setUpGradientBlurBackground(View)`：它会同时添加系统页面底色的 `GradientBackgroundDrawable`，造成首页白色遮罩。当前仅调用原生模糊算子，并保留应用原有显式着色；首页原本为零的着色不会被系统底色覆盖。监听系统材质开关，关闭时不继续添加模糊效果；缺少原生能力时使用原有算法。私有原生 API 的方向及观感仍需 ColorOS 真机验收，桌面测试不能证明系统 GPU 输出。

## 验证入口

UI 改动使用 `assembleGithubRelease` 验证编译、R8、资源和签名；`ColorOsAlertSlotsTest` 保护三操作排列、取消/删除回调映射及空表单动作。`ColorOsDockCompositionTest` 检查背景依赖隔离，`ColorOsCapsuleMotionTest` 检查内容运动及更新，`CourseShortcutTest` 保护退出交接。桌面测试不加载系统 COUI，实机仍需检查：加号展开收回、菜单反色、滚动过程中取消按钮按压、明暗主题、原生渐变模糊、三操作弹窗及 shortcut / 多课表快照。本轮证据见[shortcut 与渐变模糊修订](../reports/2026-10-12-coloros-shortcut-native-blur.md)。
