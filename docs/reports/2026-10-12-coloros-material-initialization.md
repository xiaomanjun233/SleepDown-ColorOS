# ColorOS 原生材质初始化修复（2026-10-12）

## 问题与证据

用户要求安装首版后，通过项目 ADB 5038 通道在 OPPO PLJ110 上执行签名 Release 覆盖安装，返回 `Success`，`pm path` 确认独立包名存在。用户反馈首页控件已经是 ColorOS 控件，但材质没有生效。

读取已保存的本机“设置”APK，确认 `COUIMaterialStateManager` 的模糊、描边与材质功能状态初始值均为 `false`。控件的 `onAttachedToWindow` 只添加状态监听器，不负责注册系统配置观察器。首版仅调用控件的 `setUseMaterialEffectAttr(FOLLOW_SYSTEM)`，没有调用 `registerMaterialStateObserver`；因此没有初始化跟随系统所依赖的状态。

设备只读查询确认 `system_material_blur_enable=1`，系统 AppFeatureProvider 的 `com.oplus.appcompat.texture_effect.Enable` 配置为 `boolean:true`。这些结果说明系统侧已开启相应配置，不能替代应用进程实际初始化和渲染的验证。查询只针对材质配置，不读取联系人或课程数据。

## 修复

- 创建原生主题后、创建控件前，注册系统库的 `COUIMaterialStateManager`。使用公开的模糊、描边、聚光与焦散阴影标志；注册由缓存的系统库来源持有，生命周期与进程一致，避免重复注册观察器。
- 后续主题/配置变化调用同一个管理器的 `onConfigurationChange`，由原生库更新已有控件与省电状态监听。
- Application Context 保留 SleepDown 的身份和 ContentResolver，仅替换为系统库的 resources/assets/classLoader。管理器持有应用 Context，不持有 Activity。
- GitHub Manifest 添加原生 AppFeatureProvider 的可见性声明。继续使用 `FOLLOW_SYSTEM`，不写系统开关或反射改写私有材质状态。
- `SleepDownCOUI` 日志输出版本兼容性、功能、模糊、描边、聚光和焦散阴影的布尔状态，供后续实机验收定位。

范围仍为首页底栏、3+1 FAB 与顶栏按钮；菜单内容及其他页面不参与此次替换。

## 验证

材质初始化修正版 `assembleGithubRelease --no-daemon` 成功（4 分 18 秒），包含 Kotlin、lintVital、R8、资源压缩与签名。`apksigner verify --verbose` 通过 v2 校验，名称仍为“SleepDown课程表”，包名仍为 `com.xiaomanjun.sleepdownschedule.coloros`。SHA-256：`65399e6be7718cda5c89b187eeca26d4667362fc444f984817c786b7650bfdfd`。已通过 ADB 5038 执行 `install -r`，返回 `Success`。

用户打开后，应用进程的 `SleepDownCOUI` 日志确认 `materialVersionCheck`、`isFeatureEnabled`、`isBlurEffectEnable`、`isStrokeEffectEnable`、`isSpotLightEffectEnable` 和 `isCausticShadowEffectEnable` 均为 `true`。这验证了漏接的状态初始化已经生效，不等同于全部交互完成验收；用户随后指出图标颜色、顶栏样式和切页融合问题，继续在同一轮修复。

## 同轮首页验收修正

1. 原版首页通过 `homeActionIconPainter` 使用新的 Today/Weeks 图形，首版原生路径直接读取旧 Drawable XML，绕过了这层映射。原生路径现改用当前 Today 图形及 Miuix Regular Weeks 的 VectorDrawable；Weeks 保留 Apache-2.0 作者与许可标识。用户明确要求设置齿轮继续使用原 Compose `Icons.Default.Settings` 的形状，因此保留具有相同路径的 `ic_settings.xml`，撤回 Miuix Settings 图形。图形来自应用自身，不宣称为厂商官方图标。
2. `COUIBottomFloatingToolbarItemView.setIcon` 只接收 Drawable，并不会自动应用标签的颜色。按原生 `couiColorLabelPrimaryVariant`、`couiColorLabelTertiary` 配置未选中及禁用颜色，图标和文字的勾选/选中颜色按用户要求统一为蓝色 `#007AFF`。颜色仍通过 ColorStateList 响应原生状态，不增加宿主的触摸处理。
3. 用户指定相册作右上角按钮参考。只读取本机系统相册 APK，确认 `COUIActionMenuItemView` 创建 `ToolbarMaterialEffectDelegate.forMenuItem`，使用专门的工具栏材质与触摸动画。原 tiny 文本按钮样式不等同于该路径。现用 Android ImageButton 承载居中图标，接入“设置”APK 中同名原生 delegate 与工具栏 `FOLLOW_SYSTEM`，按照相册控件的 draw 路径调用原生聚光绘制。中间版使用 `TYPE_DEFAULT_BLUR`，用户仍反馈背景材质不明显；后续改用相册构造器采用的 `TYPE_DEFAULT`，附着及主题变化时调用原生 `refreshRipple` 重新应用背景材质。按钮销毁时执行原生 `detach`。该调整的视觉结果需实机确认，不能仅凭开关为 true 宣称按钮材质已验收。
4. 用户要求恢复跟随壁纸的变色。保留原有壁纸亮度明暗判断，并将原生主题 Context 从按 dark key 重建改为保留同一实例、更新内部配置与主题。明暗改变后调用 dock 的 `refresh`、FAB 的 `onConfigurationChanged`、顶栏 delegate 的 `refreshRipple`，刷新原生材质和图标颜色；不解绑 expansion view，不新建整组 View。FAB 刷新会通过 `createConfigurationContext` 读取厂商尺寸，因此配置 Context 同样保留宿主身份并转接系统库资源。真实窗口配置变化仍可按原流程重新创建。此处跟随的是现有壁纸亮度策略，未新增壁纸主色提取算法。
5. 原生库未提供两个宿主的聚光 Drawable getter，按已确认的系统 APK 字段读取现有实例，再调用公开 `setLightColor` 将跟手光设为蓝色。只读取 Drawable 引用，不写入私有材质开关，不替换原生光效、触摸轨迹或动画。原生主题刷新后重新应用颜色。

中间整合版 `assembleGithubRelease` 成功（6 分 12 秒），包含 lintVital、R8、资源压缩与签名；v2 签名校验通过，应用名称和独立包名均未变化。SHA-256：`b5a8029fb812a2372e298370ed6201af165a1e132333d4c015a6e60483afe510`。确认同一台 OPPO PLJ110 连接后，通过 ADB 5038 执行 `install -r`，返回 `Success`，未自动启动应用。用户打开后的 COUI 日志再次确认全部材质开关为 true；用户随后反馈顶栏按钮材质不明显，并追加恢复壁纸变色、蓝色选中态/跟手光及保留 Compose 设置图标的要求。

包含上述追加修正的最终版 `assembleGithubRelease` 成功（8 分 43 秒），保留 lintVital、R8、资源压缩与签名；v2 签名校验通过。版本为 `1.2.7_beta5` / `34`，名称仍为“SleepDown课程表”，包名为 `com.xiaomanjun.sleepdownschedule.coloros`。SHA-256：`61de5f7843a9f32f6d40db8981162c0d948fe8b6f738a553e58ac32b22624152`。再次确认同一台 OPPO PLJ110 后，通过 ADB 5038 覆盖安装，返回 `Success`，未自动启动应用。

最终版增加顶栏 delegate 自身材质类型、模糊/描边允许及开关状态日志，并在 dock 原地刷新时记录 View 身份与明暗配置。它们用于区分“全局材质开关已启用”和“实际按钮是否应用了材质”，以及核对变色是否保留同一组 View。实际顶栏背景和连续变色切页融合仍待用户打开后实机验收。
