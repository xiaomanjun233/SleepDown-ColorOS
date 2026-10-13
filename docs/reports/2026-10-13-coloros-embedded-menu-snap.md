# 嵌入菜单材质、模式切换与滑条吸附修订

## 本轮反馈与修订

- popup 和加号菜单显示纯黑、缺少材质。系统 `RoundFrameLayout` 的 `COUIBackgroundBlurBuilder.applyBlurBackground` 使用 `ViewRootManager.getBackgroundBlurDrawable`，属于窗口背景采样。本轮嵌入菜单改用 `ColorOsMaterialSurface` 的 View 级 COUI 采样；独立右上角菜单窗口保留原实现。原来的位置、展开收回、级联动画和菜单行不变。
- 课程卡、表头高光不符合预期。对照系统 `COUIFloatingButton.ensureStrokeEffect`，改用同一个 `TYPE_FRAMEWORK_CIRCLE_2` 描边/阴影 shader 预设。`StrokeEffectParams` 不含圆角半径，几何继续由实际 Shape 设置；课程渐变色与模糊调节保留。
- 按节次/自定义时间切换后无法返回。恢复统一分段滑块；同时修复原生按钮实例改变但 `AndroidView` 仍保留旧 View 的身份问题。主次样式变化时旧监听会释放，宿主必须同步换成新实例。材质、菜单行及数值控件同样按实例绑定。
- 滑条原先仅把预览数值改为默认值，拖动中 `sync` 又跳过原生 progress，因此滑块没有吸附。新增 progress adapter 同步原生滑块、轨道与预览，过滤程序更新的回调；取消恢复原值，键盘/无障碍不被吸附区困住。

## 一级半屏面板仍待定位

用户确认二级面板正常、一级课表设置位置异常；本轮暂不方便 ADB，要求先给包。检查一级/二级均使用根 popup host 内的 `renderInPlace`，无外边距，底部动画只有平移。已有截图的内缩比例更像整体缩放，但没有上版新增的布局矩阵日志，尚不能区分图层变换和 OEM 绘制状态。本轮未加入尺寸补偿或替换窗口，不将这项标为已修复；保留 `SleepDownSheet` 可选诊断。

## 验证

- `NativeValueControlsTest` 6 项、`ColorOsCapsuleMotionTest` 2 项、`ColorOsMaterialCornersTest` 5 项全部通过，共 13 项。新增用例覆盖原生 progress 回调回响、吸附后离开、取消、无障碍单步和原生按钮替换后双向点击。
- `assembleGithubRelease` 成功，包含 R8、资源压缩、lintVital 和签名打包。APK 经 `apksigner verify --print-certs` 核对与既有安装签名一致。
- 包身份 `com.xiaomanjun.sleepdownschedule.coloros`，`1.2.7_beta5` / `34`，名称 `SleepDown课程表`。
- 本地交付：`oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-menu-material-snap.apk`，7,442,842 字节，SHA-256 `6BAA51F729C06DCEDE932A99FEB0A69DACE0F817CE7091E874470966EF5744CC`。交付副本与构建输出校验和一致。
- 用户暂不方便连接设备，未安装、未验收 OEM GPU 材质输出；一级半屏面板悬空仍为未解决项。未发布新 Release 或标签。
