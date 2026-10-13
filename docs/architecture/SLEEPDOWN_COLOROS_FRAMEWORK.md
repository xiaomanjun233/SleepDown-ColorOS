# SleepDown ColorOS 组件框架

本框架服务 SleepDown ColorOS 派生版，入口为 `coloros/ProvideSleepDownColorOs`。组件源文件已从 `feature/experimental/colorosui` 移到独立的 `coloros/` 目录；课程管理 Activity 的旧类名保留，避免改变 Manifest、Intent 和转场路由。

## 装配和边界

- `MainActivity.kt` 的公共 `AppTheme` 安装框架；共用设计系统通过 `NativeAlertRenderer`、`NativeCapsuleButtonRenderer`、`NativeCourseCardRenderer`、`NativeMaterialSurfaceRenderer` 接口调用，不依赖具体业务 feature。
- `ColorOsNativeUi` 管理系统 APK、类加载器、资源、材质状态与跟手光；`ColorOsContext` 负责配对的主题 Context。
- `ColorOsCapsuleButton`、`ColorOsDialogs`、`ColorOsSearchField` 是共用组件；Dock、首页按钮、菜单及课程卡适配也集中在框架中。
- 不复制或打包系统 COUI 实现；运行时读取已安装、经过系统应用标志检查的 Settings / Contacts / FileManager。当前能力还受渠道、设备厂商和系统 API 支持约束，缺少组件时返回共用 Compose 实现。
- `SLEEPDOWN_EXPERIMENTAL_FEATURES` 仍是发行渠道开关；框架提取不意味着这些私有厂商接口在所有 Android 上可用。

## 胶囊按钮

既有 `LiquidButton` 是兼容入口：可点击按钮优先交给 `ColorOsCapsuleButton`，成功后直接返回，之前的 Kyant 按压、拖拽形变与高光状态不会创建。`DialogLiquidButton` 和 Alert 操作按钮直接使用同一个渲染接口，包括没有 Backdrop 的场景。

原生 `COUIButton` 负责材质、圆角、触摸、滑动取消、跟手光和触感；Compose 保留图标、文字、加载内容及原有尺寸。外层合并按钮语义并处理键盘激活，原生空文本 View 不重复暴露给无障碍。业务显式禁用状态传递给原生控件。

Compose 内容直接绘制在 COUIButton 上方，保持文字、图标和加载状态的正常更新。`ColorOsCapsuleMotionBinding` 订阅 COUIButton 自身的 `COUITouchMotionAnimator`，从同一个 `ContributeSink` 取得位移与缩放；Row 只在 graphicsLayer 块中读取这些值，不在组合阶段读取，也不创建第二套弹簧。销毁时解除回调。旧的 ViewOverlay 跨 Canvas 录制方案已因实机文字消失撤回。

普通按钮使用系统 secondary 材质。彩色操作沿用操作颜色，通过 `accentButtonBlurParams` 与加号共用颜色层写法：基于 `TYPE_FRAMEWORK_FAB`，第一层直接使用操作颜色，保留系统第二层和混合模式，颜色不再经 primary 预设的透明度衰减；总透明度 255，模糊半径为 FAB 预设的 0.85 倍。禁用按钮继续使用系统 disabled-primary 预设；确认按钮保持主题色与白字，删除等动作保留语义色。跟手光沿用系统默认光色和当前 2× 强度。

`clickTargetEnabled = false` 的弹层外壳、输入区域和共享手势表面不是普通按钮，不会被该入口变成可点击胶囊。它们继续使用各自的几何、采样与手势宿主。

覆盖共用入口后，现有设置、导入、AI 助理、教务浏览、课表管理、多课表、课程编辑及部分快捷操作中的按钮无需分别维护 COUI 生命周期。

## 弹窗操作顺序

`LiquidAlertActionStyle.Cancel` 表达取消、稍后、返回修改和继续编辑语义，不按显示文本判断。纵向操作按 Primary → Secondary → Destructive → Cancel 排列，同类动作保留声明顺序。COUI 三排的物理顺序为 neutral → positive → negative，因此 Primary 映射到 neutral、取消映射到 negative；两操作使用 positive → negative，横向时确认在右、取消在左。Compose 后备布局使用相同语义顺序。回调始终按原始 action 索引绑定。

原生按钮栏的 recommend ID 同步到实际 Primary 按钮；没有 Primary 时明确清除推荐 ID，防止破坏性动作在材质刷新后变为主题色主按钮。新弹窗按下面规则声明，不依赖声明顺序或原生槽位默认色：

| 语义 | 样式 | 例子 |
| --- | --- | --- |
| 推荐的非破坏性操作 | Primary，主题色白字 | 保存、完成、创建新课表、分享口令、授权 |
| 其他可选操作 | Secondary，普通前景 | 分享 ICS、备用下载、切换识别方式 |
| 删除或丢弃已有内容 | Destructive，红字 | 仅删除本次、删除全部、覆盖当前课表、不保存、替换恢复 |
| 关闭/返回而不执行当前决策 | Cancel，普通前景 | 取消、稍后、继续编辑、返回检查 |

恢复到空应用没有覆盖已有数据，使用 Primary；恢复报告确认存在旧数据时使用 Destructive。选择器的选中项不是确认按钮，不按此规则重排。表单标题栏仍为取消在左、完成在右。

按钮类型和文字颜色明确按业务语义设置，不继承位置默认色。分享 ICS 是 Secondary，使用普通字色；只有 Destructive 使用错误色。课程 shortcut 的删除图标和文字继续使用红色，不受弹窗排序影响。

窗口位置、材质、进入退出动画仍由 COUI 默认弹窗负责。关闭后的动作在系统退出完成后派发；表单的脏数据保护、校验与保存语义保持在业务层。

共用 `SleepDownPickerDialog` 也接入同一原生窗口入口，保留已有滚轮、色板、内容切换和确认取消逻辑；标题操作及底部操作仍可由业务提供。选择器先发送关闭请求，业务将 visible 设为 false 后由窗口退出，退出完成再卸载本次宿主。原生路径不叠加 Compose 中心玻璃层。`QuickSheetLiquidAction` 在没有 Backdrop 时也可使用原生胶囊按钮。

## 首页色彩和采样

- 加号保留主题色底，图标固定白色。形变起点匹配原生 FAB 的不透明主题层，向中性菜单表面连续插值。
- 展开的菜单与形变目标使用同一个中性色函数，避免壁纸亮度判断和菜单主题判断在交接处产生跳色。
- 三点菜单和课程 shortcut 通过原生 `PopupListItem.setForceTint(0)` 保留逐项图标色，通过 `setTitleColorList` 指定逐项字色。ListView 与 RecyclerView 两条适配器路线都会在重绑时读取这些属性；不再靠遍历 AdapterView 子树找行号补色。垃圾桶图标和删除文字保留 `#FF453A`。
- 加号展开菜单的行显式接收外层算出的前景色，避免行内按应用主题重新取色，覆盖壁纸下的反色结果。
- Dock 和加号仍位于完整首页快照内；框架迁移不把它们移到根页面上方。完整采样、临时变换和快照的归属仍由页面宿主管理。
- 壁纸主题色分为按钮原始色与 Dock 强调色：按钮保留原 HSV 饱和度/亮度范围，只有 Dock 结合底部实际可见背景提高对比度。颜色在后台从壁纸源图独立提取，允许暗色 swatch；不复用会过滤暗色的课程色板。黑白等没有有效色相的壁纸仍回退默认蓝色。形变起点与实际加号底色一致。
- Dock 跟手光保持 2× 原始强度，颜色与 Dock 强调色一致；重复更新材质不叠加强度。
- 首页判为深色壁纸时，Dock、按钮及菜单恢复原生深色材质；浅色壁纸使用中性天气材质，无壁纸时使用应用明暗主题。前景继续与首页文字反色保持一致。
- 原生 Dock 的后备内容通过稳定 State 传入，避免未使用的 Compose 后备函数捕获背景状态后改变原生宿主参数。shortcut 的整页模糊保留，动画进度只由绘制层读取；这一隔离不等同于实机重组次数已验证。
- 课程共享模糊采样通过可跟踪的 `compositionLocalOf` 提供。个性化滑杆替换采样器时只更新课程消费者，不再让 static provider 刷新整页和原生 Dock。
- 凝光模式通过稳定的 State 持有开关、着色和模糊值，只有读取对应值的消费者订阅变化，框架 provider 不随拖动重新创建模式。表面着色变化仅 invalidate；只有模糊、描边、明暗或可见性变化才重新安装原生效果。
- 个性化实时整页模糊与冻结路径一样，先录制完整页面，再模糊录制层；不把变化的 RenderEffect 直接挂到原生 Dock 的祖先层。Dock 仍在页面快照及整页模糊范围内。桌面重组测试不等同于实机 GPU 材质已验收。
- 个性化预览保留原来的缩放与模糊释放。公共 Slider/开关在 ColorOS 可用时直接使用原生控件，不再创建 Kyant 控件的轨道/滑块采样器或形变节点；未额外叠加滑块专用 Backdrop 补丁。半屏课表设置仍在根页面后绘制区域拥有每次打开独立的本地弹层宿主。见 [输入与预览修订](../reports/2026-10-13-coloros-picker-input.md)。

## 数值与选项控件

`NativeValueControls` 是业务协议，`ColorOsValueControls` 负责加载系统 COUI：数值使用 `COUISeekBar`，开关使用 `COUISwitch`，导入类型等 `LiquidOptionTabs` 使用 `COUISegmentButtonLayout`。旧入口保留给调用方，优先交给原生 renderer；能力不可用时才进入原实现。原生控件负责滑块、文字、拖动、弹簧、触觉与无障碍，不再将 Compose 文本独立贴在原生分段滑块上。

Slider 的值范围与预览/提交协议独立于厂商：拖动开始打开预览，值变化只预览，正常结束先提交再结束预览；取消恢复开始值而不提交；键盘/无障碍变化无需制造触摸预览。禁用状态传入原生 View。课程模糊使用上文百分比映射。

## 菜单与教务面板

加号菜单的本体使用与右上角菜单相同的 `RoundFrameLayout` 和 `DefaultAdapter` 原生菜单项。`NativeMenuRenderer` 只定义菜单数据、行和表面的接口，原宿主仍维护位置、尺寸、源按钮交接和开合动画。图标、文字及原生选中标记属于同一行；菜单面板继续按首页规则取中性或深色材质，FAB 本体仍由自己的着色控制。

公共 Miuix 普通菜单与级联主次菜单通过 `miuix-popup-material-content.patch` 和 `miuix-native-menu-body.patch` 挂载同款 COUI 外框、菜单行及返回标题。原菜单继续负责进退场、父级冻结、点击和滑动选择，不额外启动 `COUIPopupListWindow` 的窗口动画。原生行提供 View 测量桥接，支持弹层的 intrinsic 宽度查询；按压状态交给原生行，公共宿主将触摸转发给背景跟手光，坐标按原生 View 的屏幕位置换算。右上角独立原生弹窗与嵌入菜单共用条目着色规则，保留项目图标和删除红色。

“添加单节课”横竖屏表单外壳接入公共原生材质，保留原来的表单、输入、源菜单交接与进入退出动画。课表快速设置及其日期面板已位于根部弹层同级，使用 `miuix-inline-bottom-sheet.patch` 原地渲染，不再额外创建 Scaffold 和 DialogLayout 注册；保存语义与 BottomSheetContentLayout 的滑入、拖动、退出状态机继续沿用。重复打开后的实机边界仍需验收，桌面测量不能替代 GPU 行为。

教务灵动岛是可展开面板，使用 `TopAssistantSurface` 的原生材质和自身动态圆角；撤掉误套在外层的 `COUIButton` 胶囊，避免出现半高圆角白边。原有展开、收回与上划关闭逻辑保留。

## 课程 shortcut

课程长按菜单复用右上角菜单的 `NativeOverflowMenu` / `COUIPopupListWindow`，由课程位置对应的透明 View 提供锚点。原生窗口负责面板材质、触摸和进入退出动画；背景仍由首页完整快照执行整页模糊，原生 Dock 处在同一快照内。编辑、复制与删除继续走原业务回调，移除项保留红色。

关闭时等待原生窗口和首页背景退出均完成，再执行后续操作；reset、页面退出和拖拽接管会取消待执行动作。系统原生能力不可用时使用既有 Compose shortcut。

锚点 View 与菜单使用相同的系统主题 Context。`PopupMenuLocateHelper` 会从 `anchor.context` 创建 `ResponsiveUIModel` 并读取系统栅格尺寸，不能传应用资源 Context。首次显示的反射异常记录到 `SleepDownCOUI` 并释放窗口，回到 Compose shortcut。该修订来自调用链检查，尚不等同于已获得用户手机的崩溃堆栈。

## 表头与 AI 助手材质

`NativeMaterialSurfaceRenderer` 复用课程凝光的原生背景 View，但不受课程凝光开关控制。周表头、日期状态胶囊、AI 卡片、会话外壳、提醒和输入框通过 glass / 设计系统入口使用该能力；缺少能力时保留原绘制。

原生 View 负责模糊、描边和跟手光，Compose 负责文字、图标、布局和编辑输入。背景不拦截输入、不创建按钮点击行为；动态形状在 draw/layer 中读取并更新原生 outline，保留既有 AI 形变及快照归属。表头的模糊半径为系统通透预设的 60%；日期状态胶囊叠加原配色的纵向渐变，上部着色 30%、底部 50%，不再使用按钮的统一实色层。

成功使用原生材质的内容通过 `LocalNativeMaterialSurfaceActive` 获知这一状态。今日助手不再叠加旧的按压黑色遮罩，沿用原生跟手光；原生能力不可用时保留旧按压反馈。这个局部标志表示实际渲染结果，不靠厂商名猜测是否成功。

## 课程凝光与渐变模糊

课程凝光保留课程渐变色与默认 50% 着色。模糊滑杆显示 0%–100%：0% 对应系统默认半径的 20%，50% 对应系统默认值，100% 对应两倍半径；前后两段线性映射。内部仍保存半径比例，读取旧值时将低于 0.2 的值限制到新的最低强度，其余旧值保留实际效果并换算显示。偏好仅保存在派生版的独立 SharedPreferences，不改变 Room 和备份格式；拖动预览不写盘，提交后保存，预览取消时恢复已保存值。

课程与表头描边改用 COUIButton 通透内容默认预设 `TYPE_CONTENT_CAPSULE_3`，撤掉 Dock 预设的 1.3 倍宽度覆盖。按 COUIButton 的顺序，先根据实际 Shape、密度、尺寸设置 `OplusMaterialCornerParams`，再安装材质描边；只做 ViewOutline 裁剪无法让原生描边沿圆角走。Kyant 连续圆角从 `RoundedRectangularShape.corners` 读取，课程卡保留实际半径，胶囊才使用半高圆角；未知或不均匀路径不强行改成胶囊。跟手光保持系统默认色与 2× 强度。个性化面板显示开关、着色及模糊滑杆，全部加入行进入动画，不显示说明文字、旧轮廓光开关或折射滑杆。

所有公共 `ProgressiveBackdropBlur` / `progressiveBackdropBlur` 入口通过 `LocalNativeProgressiveBlur` 使用系统 `OplusRenderEffect.createGradientBlurEffect`。模糊效果接入现有 glass 背景采样链，沿用页面半径、方向、透明度包络、快照及坐标归属，速度取系统 `AppBarBlurHelper` 默认参数。顶部、底部和侧边入口共用这一路线，侧边字母栏保留顶部/底部消隐。

不再调用 `setUpGradientBlurBackground(View)`：它会同时添加系统页面底色的 `GradientBackgroundDrawable`，造成首页白色遮罩。当前仅调用原生模糊算子，并保留应用原有显式着色；首页原本为零的着色不会被系统底色覆盖。监听系统材质开关，关闭时不继续添加模糊效果；缺少原生能力时使用原有算法。私有原生 API 的方向及观感仍需 ColorOS 真机验收，桌面测试不能证明系统 GPU 输出。

首页和公共设置/二级页顶栏通过 `LocalToolbarBackdropBlurred` 告知图标按钮底下是否已有渐变模糊。存在渐变层时，按钮自身的第二次背景模糊半径降为对应预设的 0.35 倍；没有渐变层时仍使用完整预设。沿用各自天气/深色顶栏的颜色层和 BlendMode，保留原生材质、描边与跟手光，不开启 drawable-only 模式。此调整减轻两次模糊叠加，并不宣称只剩一次采样。

## 验证入口

UI 改动使用 `assembleGithubRelease` 验证编译、R8、资源和签名；`ColorOsAlertSlotsTest` 保护三操作排列、取消/删除回调映射及空表单动作。`ColorOsDockCompositionTest` 检查背景依赖隔离，`ColorOsCapsuleMotionTest` 检查内容运动及更新，`CourseShortcutTest` 保护退出交接。`NativeDialogTouchGateTest` 检查窗口坐标与触摸序列，`NativeValueControlsTest` 检查预览事务及百分比映射，`ColorOsMaterialCornersTest` 检查真实圆角。桌面测试不加载系统 COUI，实机仍需检查：加号展开收回、菜单反色、滚动过程中取消按钮按压、明暗主题、原生渐变模糊、三操作弹窗及 shortcut / 多课表快照。本轮证据见[输入、控件与材质修订](../reports/2026-10-13-coloros-picker-input.md)，前轮记录见[shortcut 与渐变模糊修订](../reports/2026-10-12-coloros-shortcut-native-blur.md)。
