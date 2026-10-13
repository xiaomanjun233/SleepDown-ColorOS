# 系统 App 的欧加无缝动画调用：离线分析

本轮按用户要求只分析本地已保存 APK，不连接 ADB、不启用项目中的 ViewSeamless 实验。下面是静态调用证据，不能证明第三方包在当前手机上一定能播放成功。

## 样本与入口

| 本地样本 | 版本 | 主要调用点 |
| --- | --- | --- |
| `ColorOS17-Weather.apk`，`com.coloros.weather2` | 17.7.4 / 17007004 | `com.oplus.weather.utils.ViewSeamlessUtils`；`CityManagerActivity` |
| `ColorOS17-FileManager.apk`，`com.coloros.filemanager` | 17.9.16 / 17009016 | 混淆类 `d9.z.C`、`d9.z.F`，日志语义分别是 prepareActivitySeamless、putListCoverIfNeeded |

样本 SHA-256：天气 `C4DC9B91FBBBBA1B0B3E3BF49FD062FB3ED301233A94A2485DA7ED9541DD9EB0`；文件管理 `C55B185E1A02B22EAACCC31EAEDD3DCAA2DEEC35C7F74682BFBE64823314A859`。本地用 DEX 调用索引定位，再通过 baksmali 核对完整方法，未将厂商 APK/代码打包进应用或提交仓库。

## 天气的打开链路

1. `ViewSeamlessUtils.isSupport` 检查设备能力以及是否属于它自己的 `BaseWeatherMainActivity`。此 Context 限制是天气封装的业务策略，不代表系统 API 只允许天气使用。
2. `setSeamlessView` 保留真实被点击 View 的弱引用，计算卡片在列表上/下边界的遮挡，建立 Bundle：`view_seamless_open=true`、可选浮点圆角、`view_seamless_view_with_alpha=true`、`view_seamless_view_visible=false`。
3. 调用 `OplusViewSeamless.setSeamlessView(view, context, bundle, callback)`，保存 Boolean 返回值；注册失败立即清理本次准备状态。
4. `startActivityWithSeamless` 再检查源 View、Bundle 和注册成功标记，使用同一 Bundle 调用 `startActivity(intent, bundle)` 或 `startActivityForResult`。它本身不是普通 View 的缩放动画。
5. 回调处理应用配合工作：打开开始时清理临时状态、必要时滚动被遮挡的源卡片；返回结束后通过主线程回调恢复页面业务状态。

天气此条通用打开路线没有设置 `view_seamless_bitmap`。它交出真实 View，让系统接管；这与项目为了 Compose 锚点建立的位图桥接方式不同。

## 返回与中断

`CityManagerActivity.handlerClickElementAnimWithSeamless` 取得真实城市卡片 `clMain`，建立 `view_seamless_close=true`，附加背景色和卡片圆角，重新调用 `setSeamlessView`，随后 `finish()`。另一个城市管理进入准备分支还显式传递 `view_seamless_rect`。

城市列表滚动、部分点击打断场景会调用 `finishCurrentAnimation()` 并恢复 View 状态。`ViewSeamlessUtils.skipViewSeamless(activity)` 封装 `setSkipViewSeamless`。因此要对齐系统体验，还要处理返回目标、列表滚动、中断和恢复，不是只加一次注册调用。

文件管理先检查是否已有其他来源的关闭动画正在运行，避免来源竞争；它注册源 View、`open`、半径、背景色，必要时为列表裁切加入 `view_seamless_list_cover` 两个比例值及可见性标志。说明裁切处理和状态互斥是系统 App 的实际工作内容。

## 能力检查与 SleepDown 的差别

天气 `checkVersion` 检查 Android API 至少 33，以及 Oplus SDK 大版本大于 38，或等于 38 且子版本至少 45；另经 `isSupportWindowSeamlessAnimation` 检查 OS 版本、`persist.sys.oplus.anim_level` 对应能力、Light OS、柔性窗口场景。以上是该样本自身策略，不能当成跨版本通用保证。

SleepDown 当前 `transition/OplusSeamlessBackend.kt` 已有相同系统入口及开/关 Bundle，但源是为 Compose 锚点准备的注册 View，加上软件 bitmap、rect 和快照交接。系统 App 的真实 View 路线更直接，值得后续先用原生 COUI 按钮作锚点对照；不能据此断言移除位图就能修复已有所有问题。

COUI 材质/控件与 `OplusViewSeamless` 是不同接口。Dock 内部名为 Seamless 的融合实现，也不能等同于这个跨 Activity 的系统窗口协议。

## 暂缓的实机部分

需要手机后再核对：本机系统框架 API/能力返回、第三方包是否被拒绝、源/目标窗口条件、注册成功后是否真正收到开始回调、Compose 与原生 View 两种锚点的双影/透明度、返回中断与连续往返。当前不修改全局开关、allowlist、现有转场或系统属性。
