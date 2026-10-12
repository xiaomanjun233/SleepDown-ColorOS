# COUI 弹窗材质生命周期、搜索展开及页面转场修复

用户在上一份 `native-defaults` 候选中反馈弹窗及按钮无材质、搜索框不能展开且尺寸不正确，并要求课程管理使用项目自身二级页面动效。后续要求菜单跟随入口按钮颜色、Dock 跟手光与选中态同色。本报告记录本轮代码修复依据；构建结果不等同于材质实机验收。

## 弹窗

本机系统 APK 的 `COUIAlertDialogBuilder.show()` 在 `Dialog.show()` 后调用 `disabledTitleScroll()` 和 `updateViewAfterShown()`。此前只调用 `create()` / `Dialog.show()`，遗漏后两步。`updateViewAfterShown()` 负责初始化标题、正文间距、内容限制和按钮区域，并安装 `initBlurListener()`；附着回调中的 `operateBlur()` 才会将 `COUIBackgroundBlurBuilder` 连接到实际窗口与 rootView、通知 parentPanel / buttonPanel 的 blur 状态。

本轮保留显示前 Activity 归属、动画资源包及草稿关闭拦截配置，显示后按原库顺序执行上述初始化。自定义正文内的 COUIButton 使用 `setUseMaterialEffectAttr(FOLLOW_SYSTEM, ButtonType)`，保留类型与独立材质；不能调用会将类型重置为 UNKNOWN 的单参数版本。原生窗口按钮区域继续由 COUI 配置。附着后日志增加实际 `isBlurryState()` 与硬件加速状态，便于实机核对。

## 搜索框

`COUISearchBar` 的构造函数只创建背景，不主动绑定展开点击；公开 `setOnClickListener()` 将宿主监听器转发到搜索胶囊。本轮将点击接到 `changeState(STATE_EDIT, true)`，由原库负责展开、焦点与 IME。如果已处于编辑态但键盘关闭，再次点击直接调用 `openSoftInput(true)`。

紧凑窗口的外层不再扣除 32dp：COUI 已自带左右各 16dp 的 responsive padding。保持原生 52dp 布局高度和 40dp 可见胶囊高度，不缩放文本或绘制区域；大屏宽度保留自适应上限。取消、搜索提交及查询同步沿用现有业务规则，搜索提交仅收起键盘。

## 页面与菜单

`HomeMenuToCourseManagement` 从 XML 窗口 Depth 动画切换到项目 Compose 的 Page 路线。窗口使用已有透明 Morph 主题，Legacy 后端关闭平台动画；目的页复用 `AnchoredSettingsStyleMorph`、详情页 520ms / 370ms 曲线及运动模糊，从居中 0.92 倍页面进入、返回，不生成虚假的菜单锚点或已关闭菜单快照。原生菜单实际退出后的 post 回调仍是唯一启动入口。

COUI Popup 在构造时解析主题颜色。菜单以入口按钮的 adaptive `dark` 值创建独立的 NativeThemeContext，并在该值变化时重建窗口，避免仅修改共享 Context、却保留旧 popup 颜色。普通模式菜单仍保留原路径。

Dock 跟手光直接使用与选中态同一壁纸取色值，移除此前 55% 白色混合。用户随后要求调亮：读取原始强度范围，将 `setIntensityRange(min, max)` 的峰值设为原值的 1.5 倍，最低值不变；WeakHashMap 保存每个 Drawable 原值，避免刷新时逐次相乘，也不强引用已销毁控件。其余控件保持系统默认光色与强度，原生触摸弹簧及光晕半径不变。

## 验证

同轮后续动画范围与全局材质更新隔离见[动画范围隔离](2026-10-12-coloros-animation-scope.md)。

最终 `assembleGithubRelease` 在 4 分 32 秒通过，包含 Kotlin、lintVital、R8、资源压缩及签名。动画范围提升时曾出现局部变量作用域错误，编译检查发现后将声明提升到外层并重新完成完整构建。代码审查与 `git diff --check` 通过。

`testGithubDebugUnitTest` 定向运行 `TransitionRouteCatalogTest` / `TransitionFeaturePolicyTest`，3 分 34 秒完成：5 + 3 项、0 失败、0 错误、0 跳过。未运行无关 UI 套件，没有增加样式实现镜像测试。

`apksigner verify --verbose` 确认 v2 签名有效、一个签名者。`aapt2 dump badging` 确认包名 `com.xiaomanjun.sleepdownschedule.coloros`、显示名 `SleepDown课程表`、版本 `1.2.7_beta5` / code `34`。APK 包含 6,542 个定义类，COUI / Oplus 材质定义类数量为零，也没有分析用系统 APK；运行时仍从手机系统加载组件。

交付文件 `app/build/outputs/apk/github/release/SleepDown-ColorOS-1.2.7_beta5-material-scope.apk` 与 Gradle 原输出一致，7,393,690 字节，SHA-256：`0641221026bcf40b6e166a564824331c6512ddc9444310ed9378598dc016f410`。

ADB 5038 的 devices / mDNS 未发现连接设备，未安装或启动最新候选，未声称完成窗口材质、搜索键盘、凝光卡片或动画性能的实机验收。
