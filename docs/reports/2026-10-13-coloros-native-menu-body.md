# ColorOS 菜单本体、添加课程材质与半屏面板

## 本轮要求

加号弹窗与普通/二级 popup 改用右上角同款 COUI 菜单本体，保留原有动画；补齐“添加单节课”材质。用户截图确认课表设置半屏面板仍有左右边距、底部悬空，此前反馈第二次打开时出现。

## 实现

- 增加 `NativeMenuRenderer`，ColorOS 使用系统 `RoundFrameLayout` 和 `DefaultAdapter` 创建外框与菜单行。独立右上角原生窗口共用 `PopupListItem` 的着色映射，原项目图标、前景反色与删除红色均保留。
- 加号继续使用既有形变宿主；普通与级联 popup 继续使用原 Miuix 定位、进退场、父级冻结、点击和滑选状态机。原生 View 只替换菜单本体，不开启第二个 PopupWindow 或窗口动画。原生行包在可回答 intrinsic 测量的 Layout 中，背景从共同宿主接收触摸。
- “添加单节课”横竖屏外壳补上 `NativeMaterialSurfaceRenderer`，原表单及其数据保存链不变。
- 半屏面板默认外边距已经为零；本轮移除其重复的 Scaffold / DialogLayout 宿主，直接在现有根部弹层同级绘制，由底部面板本身管理滑入、拖拽和退出。没有通过反向缩放或设备尺寸特例补偿。此项属于针对宿主链路的修订，尚未取得本次截图对应的实机运行堆栈。

本轮未连接 ADB。原生材质采样、COUI 菜单行视觉与半屏面板实际边缘仍需实机验收。

## 验证

- `NativeMenuBodyTest` 通过：嵌入 AndroidView 的菜单行可参与 intrinsic 宽度测量，启用项回调正常，禁用项不执行操作。
- `QuickScheduleSheetLifecycleTest` 两项通过：普通/玻璃背景分别连续打开三次；中间改变 underlay 缩放并打开关闭另一个注册弹窗，面板左右、底边保持对齐根容器，标题与底部动作位置稳定，保存和退出回调次数正确，退出期间保留表单内容。测试装配包含与应用相同的 MiuixPopupHost。
- 两份新增 Miuix 补丁在实际本地依赖上通过逆向 `git apply --check`；README 与 CI 的应用顺序一致。
- `assembleGithubRelease` 通过，包含 R8、资源压缩、lintVital 与签名打包。APK 的 V2 签名有效，证书与上一份安装包一致；本轮没有执行安装。

## 安装包

- `oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-native-menu-body.apk`，7,442,842 字节，不入 Git。
- 包名 `com.xiaomanjun.sleepdownschedule.coloros`；名称 `SleepDown课程表`；版本 `1.2.7_beta5` / `34`。
- SHA-256：`AD5CE656DDABC3E881F86A139F1379144485CD55D9D20EC97FC184FCB477BD03`。

## 后续安装与 ADB 入口调整

用户重新连接 USB 后，已有服务的只读查询显示手机在 5037 在线，5038 的设备列表为空。旧入口强制选择 5038 并按服务归属执行重启，已按用户要求去掉。入口现在保留 SDK 客户端定位、原样传递 ADB 服务参数、遵循环境配置及原有 connect 失败码修正；不覆盖 ADB 环境变量，也不自动停止其他服务。项目 AGENTS.md 同步取消固定端口要求。

PowerShell 语法检查、默认入口设备发现及显式端点调用均通过。复用当前连接对 OPPO PLJ110（Android 17）覆盖安装上述同一 SHA-256 APK，ADB 返回 `Success`；包管理器确认 `1.2.7_beta5` / `34`，更新时间 `2026-10-13 13:02:45`。操作前后两个 ADB 进程 PID 未变，没有重启服务、清理应用数据或启动应用。仅调整安装脚本和文档，复用已签名产物，没有重新构建 Android 包；页面视觉仍待用户验收。
