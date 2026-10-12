# 原生按钮内容形变、课程边缘与顶部渐变模糊

后续实机反馈按钮文字消失、顶部出现白色遮罩；下文记录当时方案与交付，按钮及渐变模糊实现已由[后续修订](2026-10-12-coloros-shortcut-native-blur.md)替代。课程边缘预设保持。

## 原生证据与修复

本轮读取本机已有的 ColorOS 17 Settings APK 对应类，并对照 Weather 的 COUIButton 实现；未重新提取手机数据。

- COUIButton 的 `dispatchTouchEvent` 将事件送入 `COUITouchMotionAnimator`。动画回调将 ContributeSink 的 translationX/Y、scaleX/Y 直接设置到整个 View，因此原生文字天然跟随按钮。上一轮 Compose Row 与空文本 COUIButton 并列绘制，导致只有背景参与形变。
- 现将 Compose 文字、图标和加载内容录制为 GraphicsLayer，交由按钮自身的 ViewOverlay 绘制。COUIButton 的 draw 在 super.draw 之后绘制原生跟手光；内容和背景共享同一个原生变换，不再通过 Compose 状态逐帧追踪动画。外层保留测量、无障碍与键盘激活，销毁时释放 overlay 关联。
- `BaseBottomFloatingMenuView.updateStrokeEffect` 在 RenderNode 路径使用 `TYPE_FRAMEWORK_CAPSULE_3`；跟手光使用 `TYPE_OPAQUE_MEDIUM_2`。课程卡改用这两个预设和 Dock 现有的 2× 跟手光强度，保留系统默认光色与课程自己的轮廓。模糊仍使用内容通透预设，课程渐变和 50% 默认着色不变。移除个性化弹窗的凝光说明文字。
- `AppBarBlurHelper.setUpGradientBlurBackground(View)` 使用系统背景渐变及 `OplusRenderEffect.createGradientBlurEffect(FFZFIII)`，并管理系统材质状态监听。首页顶部改为通过框架调用这个入口，保留原页面布局、显示时机与快照归属。原生路径不叠加旧模糊或重复亮度补偿，应用性能模式及缺少能力时继续原有实现。

## 验证与交付

- `:app:assembleGithubRelease` 通过，耗时 4 分 44 秒；包含 Kotlin、R8、资源压缩、lintVital 与 Release 签名。
- `apksigner verify --verbose` 通过，v2 签名、1 位签名者。包名为 `com.xiaomanjun.sleepdownschedule.coloros`，版本仍为 `1.2.7_beta5` / `34`，显示名称沿用 SleepDown课程表。
- `git diff --check` 通过。此轮是绘制宿主和视觉参数调整，未追加机械单元测试；编译不能替代系统私有材质的实机验证。
- 本轮 ADB 5038 未发现设备，没有执行安装。按钮按压/拖动中文字同步、动态加载图标刷新、课程上下边缘、首页渐变模糊，以及 shortcut / 多课表中完整快照仍待手机验收。

安装包：`oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-native-motion-gradient.apk`，7,410,074 字节；SHA-256：`1328f7e76105512d29d7b940789c928b99e26b0fa74b39ef8f678b6fa100dbf9`。仅作为本地修订候选包交付，没有创建标签或 GitHub Release。
