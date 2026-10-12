# ColorOS 按钮、表头与助手材质修订

## 用户可见变化

- 着色胶囊按钮与加号共用 FAB 的颜色层参数，取消 primary 预设对操作色的额外透明度衰减。禁用态仍按系统预设，危险动作保留红色。
- 按钮恢复原来的壁纸取色；Dock 的选中态和跟手光单独提高背景对比度。深色壁纸恢复原生深色 Dock、按钮和菜单材质。
- Dock 跟手光维持系统原始强度范围的 2×，不随颜色对比度调整而提高，也不在材质刷新时累乘。
- 表头、日期状态胶囊、AI 卡片、会话外壳、提醒和输入框接入原生背景材质。文字、编辑框、输入法和业务动作继续由 Compose 管理。

## 长按课程的显示流程

用户确认是在长按课程后立即闪退，尚未点击菜单项。本轮没有连接设备，未获得此次实际崩溃堆栈。

代码与已提取系统类显示：`COUIPopupListWindow.show(View)` 的定位 helper 使用 `anchor.context` 构造 `ResponsiveUIModel`，其 `LayoutGridSystem` 会读取系统 `com.support.responsiveui.R.dimen`。课程 shortcut 的透明锚点原先使用应用 Context，与原生菜单资源不匹配；正常工作的顶栏按钮则使用系统主题 Context。

修订让锚点与菜单共用系统主题 Context，并将异步首次显示放进框架错误边界。若厂商调用失败，记录堆栈并释放原生窗口，展示既有 Compose shortcut。关闭、复制、编辑、移除及取消交接的控制器语义不变。

## 个性化滑杆与 Dock

课程模糊半径在预览时会替换 `SharedBlurBackdrop`。原来的 `staticCompositionLocalOf` 覆盖完整首页，替换后会使整个 provider 子树失效，即便 Dock 不读取该值。现改为 `compositionLocalOf`，只通知实际课程消费者。

新增定向回归场景连续替换 12 次课程采样器，检查消费者收到最终半径，并检查真实 `NativeHomeDockChrome` 的提交次数。此测试检查 Compose 状态隔离，不模拟手机的 COUI RenderNode 或 GPU 行为。Dock 的页面层级、完整快照及 shortcut 整页模糊保持原归属。

## 共用材质入口

`NativeMaterialSurfaceRequest` 供课程、表头和助手共用。课程凝光仍由原设置开关控制，其余表面独立接入。原生 View 不截获输入；形变路径在绘制阶段更新原生 outline，背景透明度不作用到上方文字。课程配色和原有 AI 内容布局保留。

## 验证与交付

- `ColorOsDockCompositionTest` 2 项、`ColorOsDockAccentTest` 1 项、`CourseShortcutTest` 10 项全部通过。
- `assembleGithubRelease` 成功，包含 Release Kotlin、R8、资源压缩、lintVital 和签名，耗时 8 分 6 秒。
- APK v2 签名校验通过；包名 `com.xiaomanjun.sleepdownschedule.coloros`，显示名 `SleepDown课程表`，版本 `1.2.7_beta5` / `34`。
- 本地交付：`oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-button-assistant-material.apk`，7,410,074 字节。
- SHA-256：`ef9c2febd3fa98d549b2bc471a6df44c0c6ee129c558369be155cf0ba8661850`。
- ADB 5038 本轮无设备，未安装。长按原生菜单、AI 输入法、动态轮廓和材质观感仍待实机验收。
