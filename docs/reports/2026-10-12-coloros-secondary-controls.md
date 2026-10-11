# ColorOS 壁纸色、二级控件及并列模式菜单

此文记录首个安装候选及当时的实现。后续实机反馈确认其顶栏/搜索材质、键盘开关及菜单路由存在问题；顶栏采样开孔方案已经撤回，修复依据见[材质与菜单修复](2026-10-12-coloros-material-menu-fixes.md)。下方首个候选的构建和安装事实不代表这些交互已验收通过。

本轮承接首页实机反馈。用户指定：Dock 选中态、跟手光、加号从壁纸取色，失败时回到原生加号蓝色；左侧选中态蓝色对齐原生加号；Dock 以外的跟手光保留系统默认。新增替换二级页面搜索框和返回按钮，以及并列模式三点菜单。普通模式三点菜单暂不替换，但修正其两项 Dock 尺寸。应用名和独立包身份不变。

## 实现及依据

- 复用 `rememberHomeWallpaperImages` 的已缓存代表色，没有新增滑动时的 Bitmap 分析。保留壁纸色相，约束饱和度和明度以支持小图标及白色加号；没有壁纸、加载失败、代表色回到默认课程色板或没有可用彩色色相时，读取 COUI `couiColorContainerTheme`，与原生 FAB 的默认底色来源一致。这个入口只为 Dock 提供颜色，未修改课程配色数据。
- Dock 图标、文字和 `COUIFloatingButton.setMainFloatingButtonBackgroundColor` 共用同一颜色。跟手光仍由原生 drawable 和触摸动画处理，颜色向白色提升 55%，避免原先饱和蓝 RGB 直接替换白光导致明显变暗；未改弹簧、范围或系统材质开关。顶栏原先的蓝色光覆盖已移除，搜索和返回按钮也不覆盖原生光色。
- 菜单到加号的源色与返回按钮同步取色，避免从原生加号切回硬编码 `#007AFF`。原生展开/回收弹簧或布局仍在进行时不接受菜单锚点交接，以免菜单冻结一个还在移动的按钮。只读取三个原生展开动画的运行状态，不写内部状态，也不把普通触摸缩放当成展开动画。
- 普通模式原先将原生 bar 放进 `140dp × 54dp` 的外壳。系统两项布局需要完整单元、单元间距、菜单内边距及 bar 外边距。本机 Settings 库资源分别为单元最大宽 70dp、间距 8dp、菜单单侧 8dp、bar 单侧 16dp，外壳宽度因此为 196dp，bar 高度读系统资源 56dp；原生控件自己测量实际胶囊。不新增设备分辨率特例。
- 顶栏从宿主 ImageButton 手动附加 delegate 改为完整 `COUIActionMenuItemView`，与系统相册使用同类控件。MenuBuilder、MenuItemImpl、View 和资源来自同一系统 APK。初始化原生菜单项并显式 `setItemIsDrawableMaterialEffect(false)`，保持独立 View 材质和 FOLLOW_SYSTEM；保留 20dp 图标，使用原生测量、配置刷新和光效绘制。核对反编译实现发现旧的 `setIfInBlurWindow(false)` 会开启 drawable-only 模式并 FORCE_DISABLE，不能凭方法名用它开启独立模糊，最终实现不调用它。
- 首个候选曾按两个按钮 bounds 在 `HomeTopGradientBlur` 留出采样区域。这一假设没有解决实际问题，后续确认原生 `TYPE_DEFAULT` 的 blur radius 为零，开孔反而破坏渐变层，已经删除。日志中材质开关为 true 不代表具体预设启用了模糊。
- `TopBackButton` 的公共入口使用工具栏导航按钮的原生 material delegate，所有通过公共详情页/设置页顶栏调用它的页面统一接入，业务返回/保存回调不变。箭头形状的列表展开提示、周数切换及网页历史前进/后退不属于页面返回按钮，未整体替换。
- `SchoolSearchField` 改用完整 `COUISearchBar`，复用学校/工具过滤回调和外部 query 同步。首个候选将 `setInputMethodAnimationEnabled` 关闭以避免重复 IME 位移，但实机反馈后确认该开关同时禁止 `openSoftInput`，必须恢复为 true。搜索提交也不能调用会清空查询的 normal-state 切换。后续修复保留原生触摸与键盘开启，宿主继续处理页面避让。
- 仅并列模式为三点按钮提供 `COUIPopupListWindow` / `PopupListItem`，保留课程管理、跳转周数、课表设置三项；普通模式仍走已有菜单。首个候选误将 `OnDismiss` 当作退出动画完成通知，并用同步路由打开课程管理；实机日志确认该 native-capable 路由要求 suspend `open()`，导致闪退。后续改为原生动画结束/窗口 detach 后执行动作，再通过 suspending coordinator 携带完整初始状态打开页面。

## 验证

首次 Kotlin 编译发现新调用缺少 `SideEffect` 导入，已改为全限定调用。最终 `assembleGithubRelease` 在 4 分 43 秒完成，包含 Kotlin 编译、lintVital、R8、资源压缩和签名。`apksigner verify --verbose` 确认 v2 签名有效、一个签名者。

最终候选为 `1.2.7_beta5` / versionCode `34`，包名 `com.xiaomanjun.sleepdownschedule.coloros`，显示名仍为 `SleepDown课程表`。APK SHA-256：`3b276ede43cd14daf28b1f236388886de27b18e7eb0a2bb0d0b7e665c6d3edcc`。

安装前重新确认唯一连接为 OPPO PLJ110，通过 SDK ADB 5038 执行 `install -r` 返回 `Success`，完成保留数据的覆盖安装；未自动启动应用。

本轮读回 OPPO PLJ110（串号末尾 `00000`）在 ADB 5038 的连接，来源为 SDK ADB 和当前用户配对密钥。检查时前台不在 SleepDown，应用专属日志没有新按钮状态，因此未自动启动或操作其他前台应用。

待实机核对：顶栏按钮是否独立采样、图标居中；壁纸更换及明暗切页时 Dock 光色/加号/选中态一致、3+1 融合不断；原生菜单按压/滑动、取消及三项操作；教务搜索聚焦、中文输入、清除、查询更新、搜索/返回收键盘，以及二级返回的保存拦截。实际安装成功不等于这些视觉/交互项目已通过。
