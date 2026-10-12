# 原样 COUI 弹窗、菜单收回后页面转场及项目新版图标

用户澄清三个目标：原生菜单先收回再播放普通二级页面转场；右上角两个按钮和菜单使用项目上一轮的新版图标；此前非选择器中心弹窗直接替换成原样 COUI 弹窗，包括材质和动画，不再强行放到屏幕中央。三项改动分别提交。

后续实机反馈弹窗材质及搜索展开仍有缺失；本报告中的窗口默认样式改动不代表材质验证通过。后续发现遗漏 Builder 的显示后初始化，同时用户要求课程管理使用项目 Compose 页面动效而非 Depth 窗口动画，见[生命周期及页面修复](2026-10-12-coloros-dialog-search-lifecycle.md)。下文保留当时候选状态。

## 菜单到页面

旧原生入口使用 `HomeToCourseManagement` 且不传锚点。Legacy 后端将该 Anchored 路线的窗口动画关闭，而 `AnchoredDetailActivityMorph` 在 source bounds 为 null 时从完成态开始，所以页面直接出现。

新增隔离的 `HomeMenuToCourseManagement` / `ColorOsCourseManagementActivity`，复用完整课程管理业务和初始状态交接，Manifest 声明为普通不透明窗口。原生菜单仍在 `onMainMenuExited` / 实际 detach 后 post 动作，不增加猜测延时；目的页使用现有 `schedule_depth_enter/exit/pop_enter/pop_exit` 页面动画。协调器按 Depth 路线在 API 34+ 的目的页登记 OPEN/CLOSE，旧系统沿用 pending transition；目的容器不再进入锚点 Morph，关闭不覆盖为零动画。原始首页 Morph、课程卡片到详情和关闭的 Oplus 能力策略保留。

## 项目新版图标

原 Compose 路径经 `HomeActionIcons` 使用 Miuix Regular 的 Edit、More、Notes、Weeks；原生路径直接加载旧 drawable，因此图形退回旧版。本轮将同一项目依赖中的 Edit/More/Notes 原始路径转为 Android VectorDrawable，保留 viewport、Y 翻转和填充规则；Weeks 复用之前已转换的资源，Settings 沿用原 Compose 齿轮的等价 drawable。资源保留 Apache-2.0 来源说明，没有借用系统应用图标。

按钮和菜单共用 `colorOsHomeIconResource()` 映射，业务资源 ID、描述和回调身份保留。菜单每次打开读取当前原生主题前景色，明暗下均正确着色；Dock 的原 Compose 设置齿轮不改。

## 原样系统弹窗

移除显式 `COUIAlertDialog.Center`、`setWindowGravity(CENTER)` 和 `setWindowAnimStyle(Animation_Dialog)`，直接使用 `COUIAlertDialogBuilder(Context)`。本机构造器默认使用 `COUIAlertDialog.BottomWarning`，由库资源决定手机/大屏的位置与布局，并保留其原始窗口动画、圆角、消息、按钮和模糊处理。业务正文、输入长度、禁用、校验与草稿退出确认继续复用原入口。

动画 style ID 和类来自同一系统 APK。窗口的 `LayoutParams.packageName` 指向该资源包，让 WindowManager 解析原始 COUI 动画资源；Activity token、进程 UID 和应用 Context 仍来自 SleepDown，没有复制系统 APK 或手写动画时长。依据为本机 COUI 构造器、资源表与 [Android Window 源码](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/master/core/java/android/view/Window.java) 中保留显式 packageName 的行为；公开字段定义见 [WindowManager.LayoutParams](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#windowAnimations)。最终实际窗口动画仍需要实机核对。

原生弹窗日志记录实际 gravity、window animation ID 和资源包，便于后续确认。选择器、普通模式三点菜单及非中心 QuickSheet 保持原路径。

## 验证及交付

最终 `assembleGithubRelease` 在 4 分 11 秒通过，包含 lintVital、R8、资源压缩和签名。最初将 Release 与 Debug 单测放在同一任务图，2 GB 的 in-process Kotlin 堆在两个变体编译时 GC thrashing；随后拆为顺序独立构建，临时使用 3 GB 堆、一个 worker 完成，项目全局配置没有改动。

`testGithubDebugUnitTest` 仅运行 `TransitionRouteCatalogTest` 和 `TransitionFeaturePolicyTest`，在 2 分 48 秒完成，共 8 项、0 失败、0 错误、0 跳过。测试保护路由登记、原有 Morph 归属和 Oplus 能力策略，不代替窗口动效实机验收。

`apksigner verify --verbose` 确认 v2 签名有效、一个签名者。身份仍为 `com.xiaomanjun.sleepdownschedule.coloros` / `1.2.7_beta5` / versionCode `34`，显示名 `SleepDown课程表`。文件 7,377,306 字节，SHA-256：`522a4a13d586cf6575583253713559fe0cad275e4b534bd5223c2ae52bdf770f`。

交付文件 `app/build/outputs/apk/github/release/SleepDown-ColorOS-1.2.7_beta5-native-defaults.apk` 与 Gradle 原输出哈希一致。ADB 5038 未找到连接设备，最新候选没有安装，也没有启动应用；按用户要求在对话中提供文件。

待实机验收：菜单收回后打开课程管理、返回首页、项目图标与暗色菜单、COUI 默认弹窗位置/模糊/进入退出，以及输入与草稿取消的业务行为。
