# Shortcut、Dock 状态范围与原生渐变模糊修订

本轮用户要求保留整页模糊，检查 Dock 随 shortcut 重组的原因；同时继续修复按钮文字消失、顶部白色遮罩，统一渐变模糊并按背景亮度调整主题色。手机暂时不方便连接，本轮没有重新读取设备数据。

## Dock 和 shortcut

- 全局搜索 `courseShortcuts.progress` / `cardScale` 的读取：背景模糊、遮罩、浮起课程卡的连续值只在 draw / graphicsLayer 中消费，没有把动画进度传给 Dock。
- `NativeHomeDockChrome` 的 Compose 后备 lambda 仍捕获页面配置、Backdrop 等宿主状态。原生路径不会使用它，却把这个闭包作为可变化输入。改为稳定的 `State<@Composable () -> Unit>`，只有单独的后备子组合读取内容。原生宿主不因此订阅后备内容变化。
- Dock 继续位于完整首页录制层内，由原有整页模糊处理。没有移动到模糊层外，没有提升全局层级，也没有移除模糊效果。
- 课程 shortcut 复用右上角 `COUIPopupListWindow` 适配器，保留编辑、复制、删除与快速编辑入口。原生窗口退出和背景模糊退出均完成后执行动作；页面重置或拖拽接管取消待执行动作。延迟显示前核对生命周期和关闭状态，避免已关闭后重新弹出。
- 原生 Dock 更新函数原本已有选中态、外观与展开状态去重，本轮没有重复添加另一套缓存。仍保留 `Native dock commit` 日志，方便后续同场景区分宿主重组与系统 View 绘制。

这次隔离能消除明确的闭包输入关联；不能据此断言用户实机中的全部“重组”现象已经消失。实际 View 材质刷新、GPU 重绘和重组计数仍需设备确认。

## 按钮内容与渐变模糊

本机已有 Settings smali 显示：COUIButton 使用 `COUITouchMotionAnimator` 的 ContributeSink 更新整个 View 的平移和缩放；`COUIComposeMotionAnimator` 提供添加及移除更新回调。按钮内容恢复正常 Compose Row 绘制，同时读取同一个原生 sink 的变换，仅更新 graphicsLayer。撤回导致文字消失的 ViewOverlay 跨 Canvas 录制；不再使用另一套按钮动画。

`AppBarBlurHelper.setUpGradientBlurBackground` 不只设置模糊，还添加系统页面底色的 `GradientBackgroundDrawable`。后者按渐变 stop 替换透明度，因此仅把底色设透明也不能可靠移除白色层。新实现通过公共 glass 入口调用它使用的 `OplusRenderEffect.createGradientBlurEffect`，不附加系统页面底色 Drawable。所有公共渐变模糊入口共用此实现，保留原有采样、坐标、着色与快照；原生速度读取 AppBar 默认值，关闭系统材质时停止加模糊，能力不可用时回到现有算法。

系统 AppBar 实现的布尔参数固定为 true，对应顶部纵向路径；侧边按横向调用接入。私有 API 的方向、RenderEffect 与 Compose 采样输出组合仍须真机检查，构建成功不能证明所有厂商 GPU 路径一致。

## 壁纸主题色

从现有可见壁纸采样助手取得 Dock 下方区域的颜色，包含裁剪和壁纸亮度。保留主题色相，暗背景提高明度，亮背景提高对比度，目标文字对比度为 4.5。加号固定白色图标，底色单独保证至少 3.0 对比度，其形变起点使用相同颜色；Dock 选中态和跟手光共用调整后的主题色。没有壁纸时沿用默认主题蓝。

## 验证与交付

`:app:testGithubDebugUnitTest` 的 13 项定向测试通过，0 失败、0 跳过：

- `CourseShortcutTest`：10 项，包括复制数据、菜单定位、关闭后动作、拖拽接管，以及新增的原生退出等待和 reset 取消动作。
- `ColorOsDockCompositionTest`：连续 12 次改变后备内容捕获的背景状态，后备内容收到最新状态，实际 `NativeHomeDockChrome` 的提交日志数未增加。桌面没有 COUI APK，因此只证明宿主组合边界，不能替代原生 View 计数。
- `ColorOsCapsuleMotionTest`：按钮文字的实时坐标与输入位移/缩放一致，图层运动不新增内容组合，文字更新仍生效。这是组合及几何验证，不是 ColorOS GPU 像素截图验收。
- `ColorOsDockAccentTest`：多种色相在亮暗背景满足目标对比度，加号底色满足白色图标对比度。

首次编译修复 shortcut 的主题函数缺失导入；首次按钮测试使用布局回调保存的旧坐标而失败，改为查询图层变换后的实时坐标后通过。未为通过测试降低断言或移除动画。

完整 `:app:assembleGithubRelease` 与以上定向测试同一轮通过，耗时 6 分 9 秒，包含 Kotlin、R8、资源压缩、lintVital 和签名；`apksigner verify --verbose` 确认 v2 签名有效、1 位签名者。`git diff --check` 通过。

安装包：`oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-shortcut-native-blur.apk`，7,410,074 字节。SHA-256：`357490f993fc12beebcb5f42e6271a4e61978f521c0d598130da21f8ced691c0`。

实际包名 `com.xiaomanjun.sleepdownschedule.coloros`，版本 `1.2.7_beta5` / `34`，名称 `SleepDown课程表`。未安装、未进行 ColorOS 真机观感或性能验收；仅交付本地修订候选，没有创建标签或 GitHub Release。
