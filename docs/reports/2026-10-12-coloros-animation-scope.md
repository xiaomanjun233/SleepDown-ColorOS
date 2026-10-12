# 原生控件与全屏弹层动画范围隔离

用户反馈中心弹窗文字变化、加号菜单形变和课程快捷操作触发全屏模糊时，Dock 一起响应，影响性能。本轮针对能从代码确认的范围及重复更新修复；没有连接设备，尚无帧时间或 GPU 性能提升数据。

## 结构依据与修复

原生 Dock 曾位于 `HomeBackgroundZoomLayer` → `HomeBackgroundBlurLayer` 的后代中。加号与 `courseShortcuts.progress` 共用这两个全屏背景容器；背景缩放、全屏 RenderEffect 或冻结快照因此也作用于原生材质 View。现在原生 Dock 在两层外作为同级消费者绘制，仍复用同一业务回调、入口权限和底部安全区。非原生路径保留原位置，原生 3+1 的 show/hide 弹簧只响应标签状态变化。

原生 FAB 的返回端点此前在每次 draw 时重新 `GraphicsLayer.record()`。其尺寸与外观在加号形变过程中固定，现在按上下文、明暗、壁纸色和密度记录一次，外观改变再记录；不在每个菜单/背景动画帧重复录制静止端点。

`Source.themedContext()` 原来每创建一个弹窗、按钮或卡片 Context，就对进程级 `COUIMaterialStateManager` 调用 `onConfigurationChange()`。本机反汇编确认此方法替换 `globalContext` 并执行全部已注册 flags 的 `updateState()`。现在状态管理器使用应用 Context、真实配置；注册一次，只有真实 Configuration 改变才触发全局更新。控件自己的壁纸明暗仍在其 NativeThemeContext / View 中局部刷新。省电和系统材质设置变化仍由原生 observer 接收。

原生弹窗 SideEffect 曾每次重组都 `setTitle` / `setMessage` / `setEnabled` 并重绑按钮监听。现在仅在相应值变化时更新；点击监听绑定一次，执行时读取最新业务 request。正文内部动画不应反复触发窗口重新测量或按钮材质重新应用。

## 验收边界

待实机核对加号展开/收回、中心表单切换正文、课程快捷操作、连续打开不同弹窗、弹层前后 Dock 明暗和触摸状态。构建与定向路由测试不能替代这些场景的实际性能测量；不声称消除全部重组或所有系统渲染开销。
