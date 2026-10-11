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
