# ColorOS 悬浮面板、原生 popup 与主题修订

## 需求与边界

用户不再要求一级课表设置面板贴住屏幕四边，改为适配机身圆角的悬浮面板。普通 popup 后续明确改用完整 COUI 窗口及动画；加号菜单保留现有形变宿主，但材质参数与右上角菜单共用。当前不便连接 ADB，本轮只交付安装包，不记录未经验证的 GPU 观感结论。

## 实现依据

- 半屏材质原先作为整棵 Column 的 modifier，Kyant DrawBackdropModifier 的 placement layer 使用 Offscreen，原生按钮因此也在该层内。Miuix 补丁将材质作为背景子节点，与控件同级；保留原宿主的滑入、拖动及退出保留内容。
- 多课表顶栏 alpha 和长按空白入口 fade 原先触发分组离屏；改为 ModulateAlpha，入口保留原缩放和 320/120 ms 时长。
- Alert 先前以 dark、按钮标签为 remember key，切换外观会销毁窗口。现在只按操作槽位等结构创建，外观更新到同一个 Context/窗口。
- COUI DefaultAdapter 的逐项反馈依赖 StatefulDrawable touch-select 状态，普通 View.pressed 不足以驱动。嵌入行增加进入/退出调用；普通 popup 直接由 COUIPopupListWindow 自己接管触摸。
- 原生 popup 的子菜单回调已去掉返回标题偏移，调用方只按数据索引取子项。主项有 children 时由系统展开，叶子操作等待关闭完成后执行。
- RoundFrameLayout 标准菜单半径为 160 px。所有菜单共用标准菜单颜色或首页中性色配置，嵌入菜单继续用 View 采样，并使用 RoundFrameLayout 初始化的原生 edge drawable，不复用窗口后的背景采样。
- COUISpotLightEffectDrawable.onBoundsChange 只更新 resolution，不更新固定的 radius range。课程卡现在按当前尺寸更新范围，独立 1.2× 跟手光；菜单 1.7×，Dock 2×。课程静态高光只修改原生 stroke alpha/width，不额外绘制白边。
- 深色壁纸切浅色设置页时，Dock 在原地刷新材质前后清除 native spotlight 状态，避免旧的按压光残留；刷新后重新校准 2× 强度。
- 设置页 Dock 的强调色按设置页实际背景计算对比度，不沿用深色壁纸上提亮后的强调色。
- 原生菜单不再受旧超级性能模式的简化分支控制，保留 COUI 自身完整动画。背景同级绘制和 ModulateAlpha 只修复采样层级，不缩短动画。
- 发现首页切换状态机对超过 10 张卡启用整页平移。ColorOS 能力可用时取消这一数量降级，日周视图、首页与设置切换保留分组追随和回弹；原切周甩尾及系统动画时长设置仍生效。
- 浮动课表设置和日期面板通过背景 Composable 插槽接入 Dialog 材质角色，读取 `coui_dialog_blur_radius` 与 `coui_dialog_list_*_blur_*` 原生颜色资源，通过窗口内 View 材质采样；不使用只采样窗口后方的 BlurBuilder，也不叠加课程卡的高光描边。
- 日视图 AI 助理卡片及其展开会话单独采用 1.2 倍模糊半径和 1.2 倍原生颜色层 alpha，保留混合模式，避免额外平面遮罩。

## 主题与胶囊

公共主题从壁纸小图提取颜色并缓存，所有页面的开关、数值滑条和强调按钮跟随同一来源。没有有效色相时默认蓝色。Dock 的强调色仍结合背景提高对比度，删除等语义颜色保留。普通胶囊使用返回按钮的透明颜色层，着色主按钮保留 FAB 同源主题色层。

## 验证

- 最终 `assembleGithubRelease` 任务完成，包含 R8、资源压缩、lintVital 和签名；未使用跳过资源压缩参数。
- `QuickScheduleSheetLifecycleTest` 2 项、`NativePopupPreferenceTest` 1 项通过。`HomeSwitchMotionTest` 5 项通过，包含 30 张课程卡仍有分组追随与回弹。新增测试首次只推进动画帧，未推进协程延迟，结束判定失败；改为统一虚拟时钟后复跑该类全部通过，未修改生产动画参数。
- Miuix 补丁反向检查通过，与本次构建使用的本地源码一致。构建期间释放空闲编译进程的 JAR 文件锁后，使用一次性 Gradle 进程完成构建。
- APK 签名校验通过，签名 SHA-256 为 `0f2b50dfb7e10c6f5981ab966f1f4ebd76af81a7ad9ba31663284eaa88c0181a`。包名 `com.xiaomanjun.sleepdownschedule.coloros`、名称 `SleepDown课程表`、版本 `1.2.7_beta5` / `34` 保持一致。
- 交付文件：`oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-native-popup-full-motion.apk`，7,442,842 bytes，SHA-256 `38D212695305100750EE02488597415926EF78DE13D1F6CA80ECDADC5695E707`。

未连接设备，未执行覆盖安装；原生窗口采样、逐项触感及高光观感仍需手机验收。
