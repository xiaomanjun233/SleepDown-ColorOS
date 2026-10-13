# 原生选择器、半屏宿主与滑条背景联动

## 本轮范围与证据

用户反馈上一版半屏菜单在第二次打开时仍整块缩小；滑条形变时背景 Dock 也会变化。本轮没有实机连接，不能将代码检查、桌面测试等同于 ColorOS GPU 验收。

发现一条明确的页面缩放联动：`personalizationPreviewProgress` 同时插值了 `homeOverlayBackgroundZoom` 和弹层采样比例。滑条开始/结束预览时，整个首页（包括 Dock）会在弹层深度比例与 1 倍之间往返，并不是 LiquidSlider 与 COUI 共用一个 Kotlin 弹簧。现已去掉预览进度对页面及采样比例的控制，保持弹窗自身的深度；预览进度仍控制背景模糊消退，让课程实时调参可见。没有改变滑块自身反馈或 Dock 的 2× 跟手光。

半屏菜单原来通过全局 Miuix 弹层注册表转移显示。上一版只重建表单与动画状态，没有独立弹层宿主。本轮将整个 `QuickScheduleSettingsSheets` 移到根 Scaffold 的后绘制 popup sibling，使用每次打开独立的透明 Scaffold、本地 DialogState 列表和 `renderInRootScaffold = false`。关闭完成后一起销毁；日期子菜单使用同一个本地宿主。仍处于原窗口中，保留详细设置的窗口快照和跨 Activity 交接，不增加窗口坐标转换。未更改面板边距、宽度或以放大抵消缩小。这是宿主隔离修订，实机缩小现象仍待用户确认。

## 原生选择器

- 共用 `SleepDownPickerDialog` 优先走 `NativeAlertRenderer`，涵盖使用该入口的日期、时间、周次、节次、课程颜色等选择器。
- 使用原生 COUIAlertDialogBuilder 的窗口、默认位置、材质和进入退出动画；不在 COUI 窗口外再套原来的中心玻璃/背景采样层。
- 业务选择内容、标题操作、内容切换及底部操作保留；滚轮与色板本体仍是项目现有实现。没有改变选择、确认、取消或保存逻辑。
- `NativeAlertRequest` 增加退出完成回调及内容内边距。关闭请求先由业务确认，`visible = false` 再通知窗口关闭，原生退出完成后才卸载内容和派发完成回调。下一次打开创建新窗口。
- `QuickSheetLiquidAction` 直接尝试共用 COUI 胶囊按钮接口，包括没有 Backdrop 的选择器；主按钮使用主题色白字，破坏性操作使用红字。
- 缺少原生能力时仍使用原 Compose 选择器。

## 验证

- `QuickScheduleSheetLifecycleTest` 两项通过，普通/玻璃路径各连续打开三次；第二次将底层页面缩到 0.8 倍，验证面板左右及底边仍与根页面一致，标题和底部操作位置不变，退出中字段保留，保存/退出各派发一次。
- `NativePickerLifecycleTest` 一项通过，连续三次检查原生窗口接口的创建/销毁、可见性、标题操作与底部操作保留、业务关闭请求及退出完成回调；退出完成前内容保持挂载。
- 上述桌面测试均无失败或错误。测试使用原生 renderer 的替身验证接口，不加载手机 COUI，不验证 ColorOS GPU 材质输出。
- `:app:testGithubDebugUnitTest`（只运行上述两类）与 `:app:assembleGithubRelease` 成功，耗时 8 分 28 秒，包含 R8、资源压缩、lintVital 和签名打包。APK v2 签名校验通过。
- 候选包：`oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-native-pickers.apk`。包名 `com.xiaomanjun.sleepdownschedule.coloros`，名称 `SleepDown课程表`，版本 `1.2.7_beta5` / `34`。本轮未安装或做实机验收。
- 包大小 7,426,458 字节；SHA-256：`39f378974276f60318de61349d8ec216bbab65ffeba8facb7c4dcb9247c2dfe8`。
