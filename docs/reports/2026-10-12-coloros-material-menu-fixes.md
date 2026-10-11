# ColorOS 材质、搜索键盘及菜单修复

用户反馈顶栏、返回按钮和搜索框无法采样下层模糊，搜索点击不弹键盘，并列模式弹出菜单放大不连贯且有闪退。原生 Dock 和菜单模糊能够工作，因此调查集中于各控件预设与生命周期，没有全局强制开启系统私有材质开关。

## 根因与改动

- 本机系统 COUI 的 `TYPE_DEFAULT = 0` 映射到 `FRAMEWORK_TOP_BAR`，模糊半径实际为 0；`TYPE_DEFAULT_BLUR = 2` 映射到半径 20 的 `FRAMEWORK_TOP_BAR_BLUR`。材质管理器的 blur-on 状态只表示允许使用模糊，不能证明当前预设半径非零。顶栏、公共返回按钮和搜索框改用原生 type 2，继续遵守 FOLLOW_SYSTEM。删除顶栏采样开孔 helper，恢复完整 `HomeTopGradientBlur`；没有新增 Compose/View 包装来掩盖错误预设。
- `COUISearchBar.setInputMethodAnimationEnabled(false)` 同时阻止 `openSoftInput()`，不是单独的位移动画开关。恢复 true，由原生搜索框打开键盘。宿主教务页面继续负责避让。搜索提交和有查询时的功能按钮只隐藏键盘并清除焦点；不调用会清空查询的 `changeState(0, true)`。空查询取消和返回行为仍使用原生关闭。
- 实机应用专属日志中的崩溃为 `IllegalArgumentException: Native-capable route HomeToCourseManagement must use suspend open()`。原生菜单课程管理动作改用 `ActivityTransitionCoordinator.open()`，保留完整初始状态和原有能力开关，没有伪造锚点或转场快照。
- COUI Popup 的 `OnDismiss` 在退出开始时回调，而非动画结束。业务动作移到 `onMainMenuExited` 后的主线程 post，并以真正的内容 detach 补充完成通知；pending 动作取出后立即清空，避免执行两次。`isShowing` 在退出开始已经为 false，因此打开检查还包含内容是否 attached 和是否有待执行动作，防止退出期间重入。
- Popup 构造器的 window-animation 资源 ID 来自系统应用，WindowManager 按本应用身份解析可能无法匹配，也会和菜单根的原生弹簧叠加。window animation 设为 0，只保留 `COUIPopupMenuRootView` 的原生进入/退出控制器，不改系统弹簧参数。
- 本机相册与 Settings 的带图标菜单资源一致：宽 196dp、最大宽 256dp、条目最小高 40dp、纵向内边距 8dp、圆角 28dp。代码从正在加载的系统库读取宽度，行高、内边距和圆角继续由原生菜单管理，未写手机像素坐标。

普通模式右上角菜单继续使用原实现。Dock 的取色、原 Compose 设置齿轮及 3+1 绑定保持现有接口；非 Dock 跟手光没有添加壁纸色覆盖。

## 验证边界

已核对本机系统 APK 的公开 API、预设映射及菜单回调实现；反编译资料仅留在忽略目录，没有加入仓库或安装包。完整签名候选的构建、安装与哈希见[中心弹窗接入](2026-10-12-coloros-centered-dialogs.md)。

需要用户实机验收：渐变层连续且按钮自身模糊生效、搜索中文输入与提交保留查询、菜单连续放大/退出、快速重复点击与三项动作。构建及覆盖安装不等于这些视觉和输入项目已通过。
