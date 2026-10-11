# ColorOS 首页控件第一波接入（2026-10-11）

## 范围与来源

- 独立派生仓库：`xiaomanjun233/SleepDown-ColorOS`，保留原项目完整 Git 历史。
- 上游：`xiaomanjun233/SleepDown-Schedule`，起始提交 `9662d1034ed2941f494ed65c785cc1d21ac833c6`。
- 本轮仅：首页常规底栏、并列模式 3+1 底栏，以及顶栏个性化与三点按钮。菜单内容及其他控件等待用户验收。
- Release applicationId：`com.xiaomanjun.sleepdownschedule.coloros`；应用名称仍为“SleepDown课程表”。源码 namespace/组件类名继续沿用上游，Manifest authority 与权限声明使用新的 applicationId。

只通过 ADB 5038 读取已连接 OPPO PLJ110 上的系统应用 APK。设备系统报告 ColorOS `V17.0.0`、Android 17。未读取联系人内容、课表数据库或其他业务数据。系统 APK、反汇编、设备截图与临时构建日志不进入 Git。

“设置”APK 保留本轮所需的公开类名、资源和明暗主题，作为首选运行时来源。联系人和文件管理器也做了静态检查，但未同时满足本轮完整类名/API 与两个主题的要求；不能仅据此断言联系人采用旧版控件库，应用裁剪/混淆也会影响外部加载。

## 加载方法

OPlusBox GitHub 仓库未公开完整应用源码。其 APK 中可读取的 `RootWarningDialog` 能确认一种系统控件加载方法：使用 `createPackageContext` 的 `CONTEXT_INCLUDE_CODE | CONTEXT_IGNORE_SECURITY`，配合同一系统包的 classLoader/resources/assets、应用 Context 包装及 COUI 主题，再反射创建原生组件。这不代表已经确认 OPlusBox 所有首页控件的内部实现。

本轮采用同类加载方式，但未打包或复制厂商 DEX/JAR/资源。图标来自 SleepDown 自有资源；传入 Drawable 对象，避免把宿主资源 ID 当成系统包 ID。只接受系统包或已更新系统包，且仅 GitHub 实验渠道及 OPPO/OnePlus/realme 设备尝试加载。失败会记录 `SleepDownCOUI` 日志并保留原组件。

## 原生组件与交互

| 入口 | 系统组件/API | 处理方式 |
| --- | --- | --- |
| 常规/并列标签 | `COUIBottomFloatingNavigationBar` | 原生条目 Builder、选中监听、状态同步 |
| 顶栏按钮 | `COUIButton` | 原生材质、触摸与按压动画；只转发打开弹层回调 |
| 并列模式第 4 个按钮 | `COUIFloatingButton`、`IMeltBallView` | 原生 FAB；菜单仍由现有 SleepDown 业务入口打开 |
| 3+1 合拢/分离 | `bindExpansionView`、`showExpansionView`、`hideExpansionView` | 原生弹簧、重叠透明度与材质 metaball chain |
| 明暗/材质 | `Theme.COUI.Main` / `.Dark`、`FOLLOW_SYSTEM` | COUI 处理设备的材质策略，不强制开启系统关闭的效果 |

原生路径通过 Compose `AndroidView` 直接发送 MotionEvent，不包裹 Kyant clickable、拖动高光、按压快照缩放或手写融合裁剪。底栏自己的 MOVE/UP/CANCEL 路径处理滑动、松手命中及取消；页面切换监听不改写原生触摸机制。Compose 程序更新选中项时暂时解绑业务监听，避免重复导航。

顶栏移除宿主额外震动及 Kyant 按压比例。并列模式额外震动仅留在原组件回退路径。菜单返回锚点也使用原生按钮/FAB 的静态副本；副本只为现有菜单转场记录视觉，不接管触摸反馈。原生 3+1 分支不创建原来的 `InteractiveHighlight`、split `Animatable` 或裁剪路径。材质融合是否实际启用仍由系统策略及安装库决定。

## 验证与待验收

使用本机 JDK 21、Android SDK、既有本地 Miuix 来源与外部签名配置构建 GitHub Release。首次 Lint 遇到 Kotlin 分析器内部异常，独立 Gradle 进程重试后通过；没有关闭 Lint 或 R8。

最终 `assembleGithubRelease --no-daemon` 成功，包含 Kotlin 编译、lintVital、R8、资源压缩和签名打包。`aapt2 dump badging` 确认包名为 `com.xiaomanjun.sleepdownschedule.coloros`、显示名称为“SleepDown课程表”、版本 `1.2.7_beta5`；`apksigner verify` 确认 v2 签名有效。APK 定义类清单中没有 `com.coui` 类。

候选 APK：`app/build/outputs/apk/github/release/app-github-release.apk`；SHA-256：`56a9551c81610507d7991e85219cac3222e4c599892b213cbe3eb8b51bfd9dc0`。APK 仅保留在本地，不提交到源码仓库。

首次交付时为未安装的候选 APK。静态检查确认上述系统类、方法和资源存在；编译/打包不能证明跨包加载后的实机显示与触摸行为。当时未启动修改版应用，未完成以下实机验收；后续安装和材质初始化问题见[2026-10-12 记录](2026-10-12-coloros-material-initialization.md)：

1. 顶栏按钮按下、移出、取消及打开/关闭弹层，确认没有双重震动或 Kyant 回弹。
2. 常规和并列底栏点击、横向拖动、抬手及取消，确认原生反馈与业务选中一致。
3. “今日/课程表 ↔ 设置”时 3+1 收回/展开、材质融合、连续切换与添加按钮可点击时机。
4. 明暗主题、壁纸、字体比例、横竖屏、安全区及键盘场景；隐藏的菜单来源与返回锚点不重复绘制。

独立包名使用独立数据目录，不覆盖原应用数据；本轮没有改变备份格式与来源校验策略。原有外部课程代理、更新源和其他页面不在第一波实机验收范围内。
