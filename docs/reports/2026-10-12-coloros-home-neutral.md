# 首页中性色、菜单材质与页面归属

用户要求首页 Dock、按钮和右上角菜单参考系统天气中性色，不再随壁纸切换，设置页保持原样；同时修复课程凝光渐变重复绘制、Dock 清晰浮在弹层背景上及快捷操作带动 Dock 更新。追加要求菜单材质与按钮统一，课程管理采用设置进入二级页的转场。

## 系统天气依据

USB 设备 PLJ110（ColorOS 17）经项目 SDK ADB 5038 连接。只读提取本机 `com.coloros.weather2`，分析文件放在忽略的 `oplusbox-analysis/` 中，不进入源码或安装包。

天气首页圆形按钮为 `COUIButton`，`buttonType=3`（transparent）、`drawableMaterialEffect=true`，普通背景透明；主题继承 `Theme.COUI.Main`，没有天气天空颜色覆盖。城市管理更多菜单从按钮 Context 创建 `COUIPopupListWindow`，启用原生背景模糊。此次参考其 COUI 中性角色与透明按钮预设，保留现有独立背景采样路径。

## 当前实现

- `LocalColorOsHomeChromeStyle` 只在首页提供。选中图标与文字使用 `couiColorPrimaryNeutral`，加号使用中性图标与透明背景材质，关闭模糊时回到中性容器色；设置页移除该覆盖并恢复原生 FAB 预设。
- 顶栏按钮使用 `TYPE_CONTENT_TRANSPARENT_BUTTON` 的两层着色及模糊；Dock 将该染色直接挂载到原生菜单背景 View。加号保留天气透明按钮的中性颜色，模糊以 `TYPE_FRAMEWORK_FAB` 原生半径的 85% 为基准，并将同一组 ThemeBlurEffectParams 传给静止 View 和原生融合实现。挂载、真实主题或系统材质状态变化后应用，不在动画帧重复设置。2× 跟手光基准保持。
- 三点菜单仍使用原生跨窗口 `COUIBackgroundBlurBuilder`。在原生挂载初始化后同步按钮中性着色，按 builder 的 blend/mix 顺序传入颜色；两种主题分支使用同一组颜色。保留 COUI 窗口半径 160、原生窗口采样、圆角随缩放更新、触摸光及退出动画。窗口模糊与 View 背景 RenderEffect 是不同接口，不能直接把 View 的 sigma 25 当作同一个窗口参数。普通模式保留现有菜单组件，仅取消它对壁纸明暗的依赖。
- Dock 回到页面 blur/depth 下，作为缓存课程树的同级内容。原生组合入口接收导航、外观及启动阶段输入，快捷操作请求或动画进度不作为输入；聚合 `Native dock commit` 日志供场景计数。
- 凝光卡片移除旧边缘光、白色描边和白/黑明暗叠绘，只保留课程颜色渐变；原生轮廓改为固定增强配置，补齐实际圆角与布局后的材质更新，不再读取旧模式轮廓开关或轮廓强度滑杆。原卡片模式保持。
- 课程管理菜单回调继续等原生菜单退出。`HomeMenuToCourseManagement` 使用与 `SettingsToSettingsDetail` 相同的 `PlatformDefault` 路线和 `AppTheme` 窗口，目的页不额外挂载无锚点 Morph，返回保留普通页面转场。通用转场 host 对这类路线仅处理关闭及 session 生命周期。

## 验证

`assembleGithubRelease` 已完成，保留 R8、资源压缩、lintVital 与外部 Release 签名；v2 校验通过，单签名者。候选 `app/build/outputs/apk/github/release/SleepDown-ColorOS-1.2.7_beta5-home-neutral.apk`，7,393,690 字节，SHA-256 `28b673d02bbd0da6fa68d34a78e105fcbf1f40f5233bc7056c352304953669e1`。包名 `com.xiaomanjun.sleepdownschedule.coloros`、版本 `1.2.7_beta5` / `34`、显示名称保持。已通过 SDK ADB 5038 向 PLJ110 覆盖安装，输出 `Success`，没有自动启动。

Release Compose 报告确认 `NativeHomeDockChrome` 为 `restartable skippable`，全部参数（包括兼容渲染槽与回调）为 stable；背景、配置及 shortcut 进度不作为原生入口参数。

该候选的定向 `TransitionRouteCatalogTest` 6 项通过。用户实机操作后反馈 Dock 未套天气染色、加号模糊过低、菜单材质丢失以及课程轮廓消失，因此该候选不能称为视觉验收通过。日志确认菜单 callback 与参数调用运行，没有证明效果正确；随后按上述链路修订。聚合 Dock 日志改为 Info，以便设备默认日志级别记录。

后续修订再次完整 `assembleGithubRelease` 通过，耗时 6 分 13 秒；v2 签名有效，单签名者。候选 `app/build/outputs/apk/github/release/SleepDown-ColorOS-1.2.7_beta5-system-material-repair.apk`，7,393,690 字节，SHA-256 `776e618dcf1d8260b6b830dd0ae53e2b751a9eea7bc6ca77af53b4c29fef1728`。此前 6 项路由测试之后未再改路由，不重复执行。安装前 ADB devices 与 mDNS 均为空，覆盖安装返回 device not found；该修订当时尚未装到手机，等待重新连接。

用户随后重连 USB，SDK ADB 5038 确认 PLJ110 在线（transport 3）。同一修订包以 `install -r` 覆盖安装成功，输出 `Success`，保留应用数据，没有自动启动。课程轮廓、菜单材质和 Dock 的画面及触摸表现仍待用户实机验收。

重点实机验收：菜单与按钮材质颜色；shortcut/弹窗展开时 Dock 的页面模糊归属；一次 shortcut 展开/收回期间原生 Dock commit 数量；加号融合；课程凝光渐变；课程管理进入与返回。编译器可跳过组合不代表已经测得帧时间提升。
