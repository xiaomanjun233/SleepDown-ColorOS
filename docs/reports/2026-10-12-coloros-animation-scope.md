# 原生控件与全屏弹层动画范围隔离

用户反馈中心弹窗文字变化、加号菜单形变和课程快捷操作触发全屏模糊时，Dock 一起响应，影响性能。本轮针对能从代码确认的范围及重复更新修复；没有连接设备，尚无帧时间或 GPU 性能提升数据。

## 结构依据与修复

原生 Dock 曾位于 `HomeBackgroundZoomLayer` → `HomeBackgroundBlurLayer` 的后代中。加号与 `courseShortcuts.progress` 共用这两个全屏背景容器；背景缩放、全屏 RenderEffect 或冻结快照因此也作用于原生材质 View。现在原生 Dock 在两层外作为同级消费者绘制，仍复用同一业务回调、入口权限和底部安全区。非原生路径保留原位置，原生 3+1 的 show/hide 弹簧只响应标签状态变化。

### 后续层级与快捷操作修正

用户实机反馈上述提升位置后 Dock 错误全局置顶，快捷操作状态变化仍带动 Dock 重组。提升时沿用了旧容器内的 `zIndex(100)`，在根层压过了 `zIndex(90)` 的锚点弹层；此次原生 Dock 改为根层普通绘制顺序，位于背景之后、弹层之前。非原生路径的局部层级保持原位置。

原来的局部 `renderHomeDock` lambda 捕获整个首页状态，且创建随状态变化的点击回调。现在使用独立的 `HomeDockChrome` 组合入口，仅传入可见性、布局模式、选中项、配置、固定 Backdrop、启动阶段和加号可见性。点击回调用 `remember` 保持身份，执行时才通过 `rememberUpdatedState` 获取最新导航与入口保护；快捷操作请求、关闭状态及逐帧动画进度不作为 Dock 输入。普通模式返回首页仍保留原日/周视图选择。

本次 `assembleGithubRelease` 完整签名构建通过。临时 Compose 编译报告确认 `HomeDockChrome` 为 `restartable skippable`，三个回调为 stable，Backdrop 由原有 remember 入口保持同一对象。报告只证明编译器可跳过无关输入更新，不能替代实际重组计数或帧时间测量。ADB 5038 设备列表为空，未安装或启动；层级遮挡、快捷操作展开/收回与加号融合仍需实机验收。

原生 FAB 的返回端点此前在每次 draw 时重新 `GraphicsLayer.record()`。其尺寸与外观在加号形变过程中固定，现在按上下文、明暗、壁纸色和密度记录一次，外观改变再记录；不在每个菜单/背景动画帧重复录制静止端点。

`Source.themedContext()` 原来每创建一个弹窗、按钮或卡片 Context，就对进程级 `COUIMaterialStateManager` 调用 `onConfigurationChange()`。本机反汇编确认此方法替换 `globalContext` 并执行全部已注册 flags 的 `updateState()`。现在状态管理器使用应用 Context、真实配置；注册一次，只有真实 Configuration 改变才触发全局更新。控件自己的壁纸明暗仍在其 NativeThemeContext / View 中局部刷新。省电和系统材质设置变化仍由原生 observer 接收。

原生弹窗 SideEffect 曾每次重组都 `setTitle` / `setMessage` / `setEnabled` 并重绑按钮监听。现在仅在相应值变化时更新；点击监听绑定一次，执行时读取最新业务 request。正文内部动画不应反复触发窗口重新测量或按钮材质重新应用。

### 恢复页面模糊归属

用户进一步明确“全局置顶”是打开 shortcut 或弹窗时 Dock 清晰浮在模糊背景上。将 Dock 提到背景 RenderEffect 外导致这一表现，即使移除根层 zIndex 也无法解决。此次原生 Dock 回到 `HomeBackgroundBlurLayer` 内，作为缓存课程树的同级内容；仍在课程冻结状态与采样局部变量之外。整个页面一起接受弹层深度及模糊，原生按钮不额外连接 shortcut 动画值。

原生入口 `NativeHomeDockChrome` 返回 Unit，仅接收可见性、模式、选中项、主题、对齐、加号可见性、启动阶段及稳定回调。配置对象、Backdrop 与兼容渲染槽保留在外部；`Native dock commit` 聚合日志用于实机计数，逐帧状态不在入口读取。此调整的编译报告与设备结果见[首页中性色及菜单](2026-10-12-coloros-home-neutral.md)。

## 验收边界

待实机核对加号展开/收回、中心表单切换正文、课程快捷操作、连续打开不同弹窗、弹层前后 Dock 明暗和触摸状态。构建与定向路由测试不能替代这些场景的实际性能测量；不声称消除全部重组或所有系统渲染开销。

最终完整签名 Release 构建、v2 签名核验与现有 8 项定向路由测试通过，详细交付身份见[本轮交付记录](2026-10-12-coloros-dialog-search-lifecycle.md)。ADB 5038 未发现手机，没有安装或启动本轮候选。
