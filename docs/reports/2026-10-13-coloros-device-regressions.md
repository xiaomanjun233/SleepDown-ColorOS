# 2026-10-13 COUI 控件与课表设置实机问题

## 证据与修订

- PLJ110 / Android 17 的应用崩溃堆栈指向 `RoundFrameLayout.updateBlurCornerRadius` 的 `NoSuchMethodException`。Settings APK 的方法是 private。菜单创建阶段改为解析 declared method，后续圆角更新复用已解析方法。
- 实机层级中的 COUISwitch 未启用 clickable / focusable；该控件的 preference 默认样式依赖宿主行。独立绑定启用自身输入，外部状态同步期间移除监听，用户切换使用最新回调。开关和滑条接入页面按钮主题色及禁用色。
- 用户明确滑条问题是“100% 刻度或滑块位置不齐”，不是终点弹性拖动。移除原生控件外的 Kyant 刻度叠加，标记使用 COUI 实际轨道起点、长度与中线；保留系统形变。
- AI 原生材质原先铺满 Morph 固定分配容器，inset path 仅裁剪，并不能改变原生描边的几何。改为按当前面板大小和位置布局原生背景，传递当前圆角；全屏半径 0 不再落入胶囊类别。

## 半屏面板尚待确认

实机截图确认课表设置左右和底部有空隙。系统窗口与 Compose 根节点仍为全屏，现有 OverlayBottomSheet 无外边距，只有垂直位移。现有桌面重开测试无法解释真机整体缩小的画面。

在 `QuickScheduleSettingsSheetSession` 加入由 `log.tag.SleepDownSheet=DEBUG` 开启的布局坐标和绘制矩阵日志，默认关闭，不记录课程内容或用户数据。需安装诊断包后重开两次读取现场。当前不能把加入诊断认定为修复。

## 验证记录

- 第一轮定向测试 10 项通过：NativeSwitchInputTest 1 项、NativeValueControlsTest 4 项、ColorOsMaterialCornersTest 5 项。开关测试验证独立输入桥接，未替代 COUI 真机触摸验收。
- 第一轮 `assembleGithubRelease` 成功，保留 R8、资源压缩、lintVital 与签名；包名 `com.xiaomanjun.sleepdownschedule.coloros`、版本 `1.2.7_beta5 (34)`、应用名称均未变。
- 构建期间 USB 从两个活动 ADB 服务断开，第一轮诊断包尚未安装。ADB 选择仍由当前连接决定，项目不固定端口。
- 滑条刻度最终修订的 `assembleGithubRelease` 成功，APK 签名与既有版本一致。交付文件 `oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-native-controls-fix.apk`，SHA-256：`115C2A2DE4F908FB498883C1D96ABCB4504404FD315453B5DE4C9DA18670720B`。
- 最终核验时两个活动 ADB 服务仍无设备，Windows 也未列出手机的 USB/ADB 设备；未覆盖安装，实机交互和半屏绘制诊断尚待连接。
