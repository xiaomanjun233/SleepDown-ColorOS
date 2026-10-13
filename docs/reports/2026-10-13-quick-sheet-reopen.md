# 课表设置半屏菜单重复打开

用户随后反馈整块缩小问题仍存在。本页记录的是前一轮字段与会话生命周期修复，后续宿主隔离见 [原生选择器、半屏宿主与滑条背景联动](2026-10-13-coloros-picker-host.md)。

## 问题与证据

用户补充：首次正常，第二次打开时整个面板缩小、底部悬空。本轮沿关闭与重新挂载路径排查，未调整边距或用放大比例抵消问题。

当前半屏菜单在根 Miuix Scaffold 的后绘制宿主中显示，宿主不属于首页背景缩放层。外边距为 0，最大宽度 640dp，业务高度上限 590dp。桌面 Compose 测试中，原代码连续打开三次的标题与底部操作坐标一致，尚未复现用户实机的整块缩小。

已复现另一处相关生命周期错误：字段使用 `remember(draft?.scheduleId)`，关闭时父页面将 `draft` 设为 null，导致退场尚未结束就将总周数、当前周输入重置为空。新增测试在关闭两帧后检查总周数，普通与玻璃两条路径均在原代码上失败。原代码还让 BottomSheet 的动画控制器、日期选择状态长期留在首页组合中。

## 修改

- `QuickScheduleSettingsSheets` 只保留本次打开的初始草稿；表单、日期子菜单及 Miuix BottomSheet 归属同一个打开周期。
- 第一次挂载即提供完整草稿，不再等 `LaunchedEffect` 补入正文。
- 关闭时保留字段和正文直至 `onDismissFinished`，之后卸载本次菜单状态。下一次打开创建新的动画、手势、日期和按钮状态。
- 保持完成、取消、返回的既有保存语义，以及详细设置的跨 Activity 交接、半屏尺寸和材质参数。

这是针对重复打开的生命周期修订，不能以桌面测试代替 ColorOS 实机缩小问题的验收。没有实机连接，也没有修改外部 Miuix 工作区。

## 验证

- `QuickScheduleSheetLifecycleTest` 两项通过：普通与玻璃路径各重复三次，检查退场中保留字段、再次打开后的标题和底部按钮坐标、保存与关闭回调次数。原代码的两项退场字段断言失败，修改后通过。
- 测试使用 Robolectric / Compose，未加载手机内 COUI，不证明 OEM GPU 材质和用户实机缩小问题已经修复。早期尝试的软件 Canvas 像素比对因不支持 RuntimeShader 而停止，未用它作视觉通过的依据。
- `:app:testGithubDebugUnitTest --tests '*QuickScheduleSheetLifecycleTest' :app:assembleGithubRelease` 成功，耗时 8 分 4 秒，包含 R8、资源压缩、lintVital、签名打包；APK v2 签名验证通过。
- 候选包：`oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-sheet-reopen.apk`，7,410,074 字节。
- SHA-256：`fd084d2b32182cdb9b436d43301a50e4180b97177bc4c4e987d764a81a18a64a`。
- 包名 `com.xiaomanjun.sleepdownschedule.coloros`，名称 `SleepDown课程表`，版本 `1.2.7_beta5` / `34`。未安装到手机。
