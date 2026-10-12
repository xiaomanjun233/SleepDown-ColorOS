# 可选课程卡片凝光视效

用户要求调查可直接调用的 ColorOS 17 材质，并增加课程卡片模式；确认保留课程配色、叠加原生材质与光感。入口位于个性化设置，默认关闭，仅支持系统库能力完整且实验渠道开启的设备。

## 已确认的系统接口

本机已提取 Settings APK 中存在 `COUIMaterialEffect.setViewBlurEffect(View, BaseBlurEffectParams)`、`setViewStrokeEffect(View, StrokeEffectType)`、`setCornerParams()` 以及焦散阴影接口；模糊最终使用系统的背景 RenderEffect 挂载入口。`BlurEffectParams` 可接受半径、两层颜色字符串及 BlendMode，适合保留业务配色。COUI 的主题预设枚举只见主按钮 / FAB，通用预设主要为顶底栏、搜索与内容按钮，没有确认可直接套用的课程卡片预设。

`COUISpotLightEffectDrawable` 可以独立创建、读取系统配置、接收触摸并绘制；本轮使用实际存在的 `TYPE_TRANSLUCENT_LARGE_1`，保持默认光色。系统光感描边使用 `TYPE_CONTENT_CAPSULE_1` 通用内容轮廓配置。此处是基于系统通用接口构建的课程材质模式，不能称作系统应用的完整原样卡片组件。

官方将凝光视效列为 ColorOS 17 流体设计的一部分，见 [OPPO 官方介绍](https://www.oppo.com/cn/newsroom/press/549/)。本次检索未找到将上述 COUI / Oplus 接口作为通用 Android 公共 SDK 发布的官方文档；接口兼容性以实际安装的系统库为准。Android 标准 [RenderEffect](https://developer.android.com/reference/android/graphics/RenderEffect) 与这里使用的厂商背景采样入口不能混为同一能力。

## 接入边界

`ColorOsCourseCard.kt` 负责原生背景 View、主题、系统材质开关监听与生命周期释放。公共 glass 通过应用组装层提供的 `LocalNativeCourseCardRenderer` 调用，不依赖实验 feature。课程内容仍由 Compose 绘制，背景 View 不拦截点击、周视图拖动或缩放；MotionEvent 只观察并转发给原生光感 Drawable，不叠加 Kyant 按压反馈。

颜色来自原课程配色，着色强度与模糊半径沿用现有设置和实时预览。形状使用已有 Shape 生成原生 View outline；文字继续使用原壁纸对比度采样。超级性能模式沿用原低成本卡片入口。动画遮挡或不可见阶段按现有 `mountMaterial` / viewport 状态停用材质，detach / dispose 清除背景效果、描边与光感动画。

模式开关使用派生版本独立 SharedPreferences，跨 Activity 同步监听；没有增加 Room 列或改写上游导出格式。该设备外观开关不会包含在 `.sleepdown` 备份中，恢复课程与配置不受影响。无法加载组件时回到原卡片入口。

## 验证范围

本机接口名称、构造签名和原生调用路径已对照系统 APK 核实。构建及签名核验结果完成后补充。本轮没有连接手机，背景采样、圆角描边、长卡片光感、滚动/拖动与编辑转场均待实机验收，不将编译成功当作效果验证。
