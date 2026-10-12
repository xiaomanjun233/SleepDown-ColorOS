# ColorOS 教务顶栏及非选择器中心弹窗

此文记录首个弹窗候选。用户随后澄清需要原样系统弹窗，包括默认位置、材质和动画；本候选的 `COUIAlertDialog.Center`、强制 CENTER 及 framework `Animation_Dialog` 已撤回，当前实现见[原样弹窗及菜单转场](2026-10-12-coloros-default-dialog-transitions.md)。下方构建哈希保留为历史证据。

用户授权继续实现：教务页右上角按钮改为 ColorOS 样式，非选择器中心弹窗统一接入系统效果。此次与材质、搜索及菜单修复分别提交，便于独立回退。

## 接入范围

- 公共 `TopGlassIconButton` 在支持的 ColorOS 系统优先使用完整 `COUIActionMenuItemView`。教务学校/工具页的刷新操作及复用同一公共入口的详情按钮沿用原回调、尺寸和禁用条件；普通模式首页的三点菜单不改。
- `LiquidAlertDialog` 通过应用组装层提供的 `NativeAlertRenderer` 使用 `COUIAlertDialogBuilder` 和 `COUIAlertDialog.Center`。确认、错误、权限说明、删除、保存及更新提示统一使用原生中心窗口、模糊表面、消息和 COUI 按钮。核心设计系统只持有渲染请求和 CompositionLocal，未增加对实验 feature 的依赖。
- 课程编辑、手动导入和导入确认等原中心表单的业务正文嵌入原生窗口；`CenterLiquidDialog` 在此窗口内只做正文布局，不再套 Kyant 玻璃壳。公共标题按钮使用原生圆形图标按钮或 `COUIButton`，默认光色不取壁纸色。表单字段和课程业务仍复用现有实现。
- 新建/重命名课表使用原生 `COUIEditText`。管理页重命名保留 24 字限制及空名转为“未命名课表”；原没有长度限制的名称入口没有增加限制。主题切换重建编辑器时保留当前草稿。
- 日期、时间、数字、周数、颜色以及其他 `SleepDownPickerDialog` 选择器保留现状。教务适配器和作息对齐单选明确标记 `isSelector = true`，继续使用原来的单选卡片弹窗。横屏表单的专用飞入宿主、侧栏和非中心 QuickSheet 不在本次中心 Dialog 接入范围。

## 业务与窗口生命周期

原生按钮按 Primary/Secondary/Destructive 动作语义分配推荐操作和按钮位置，危险操作保持系统错误色。按钮 `enabled` 及 `dismissOnClick` 沿用原请求；校验失败需要保留弹窗的动作直接执行回调，不能触发原生按钮自动 dismiss。普通关闭和点击操作只分派一次业务完成。

原生窗口内的表单前景和标题按钮跟随窗口的应用明暗主题，避免沿用首页壁纸明暗后在浅色原生表面显示白字；原玻璃表单保留原来的采样对比度。圆形按钮同时同步 native enabled 状态，禁用时保留 COUI 的禁用外观与触摸判断。

多字段表单的外部点击和返回先交由原业务请求关闭，窗口等待宿主 visible 变为 false 再消失，避免原生自动 cancel 绕过“未保存修改”确认。拦截只作用于表单关闭入口，内部触摸仍交给原生 Window callback；系统返回 dispatcher 和旧版本返回键均有对应入口。native ComposeView 明确继承宿主的 Lifecycle、SavedState 和 ViewModelStore owner；释放窗口时注销回调并销毁 composition。

系统库中的窗口动画 style ID 不能直接交给以本应用身份解析资源的 WindowManager。中心窗口使用 Android framework `Animation_Dialog`；材质、尺寸、圆角、按钮和按压光仍由 COUI 负责。本轮没有宣称实现了系统应用的专有跨窗口弹簧转场。

商店渠道、其他设备或本机库不具备所需 API 时退回现有组件；不把系统 APK、反编译代码或资源全集打包进应用。

## 构建、安装与验收

最终 `assembleGithubRelease` 在 5 分 15 秒通过，包含 Kotlin 编译、lintVital、R8、资源压缩与签名。此前一次构建在源文件继续更新期间遇到 lint FIR 分析异常；停止源码修改后重新完整构建，最终检查通过，没有跳过 lint。

`apksigner verify --verbose` 确认 v2 签名有效、一个签名者。候选为 `1.2.7_beta5` / versionCode `34`，包名 `com.xiaomanjun.sleepdownschedule.coloros`，显示名称仍为 `SleepDown课程表`。APK 为 7,374,090 字节，SHA-256：`745d5fb58770c2c18060227bf341827f87986660f8c9c968f70c8f0f5f450405`。

交付文件为 `app/build/outputs/apk/github/release/SleepDown-ColorOS-1.2.7_beta5-native-dialogs.apk`，与已验证的 Gradle 原输出哈希完全一致。用户最终选择直接在对话中获取 APK，没有通过 QQ 发送。

安装前及收尾两次 ADB 5038 `devices -l` 均未找到连接设备，因此这份最新候选没有覆盖安装。先前记录的已安装候选属于第二波历史版本，不能当作本轮新包安装证据；没有启动应用、卸载或清理数据。

后续实机验收覆盖教务刷新/禁用状态、非选择器中心窗口的明暗材质与按钮触摸、名称输入、禁用/校验不关闭、课程草稿“继续编辑/保存/放弃”，以及选择器保留原交互。未自动启动应用，视觉与键盘行为需要用户实际验收。
