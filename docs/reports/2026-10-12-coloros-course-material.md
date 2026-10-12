# 可选课程卡片凝光视效

用户要求调查可直接调用的 ColorOS 17 材质，并增加课程卡片模式；确认保留课程配色、叠加原生材质与光感。入口位于个性化设置，默认关闭，仅支持系统库能力完整且实验渠道开启的设备。

## 已确认的系统接口

本机已提取 Settings APK 中存在 `COUIMaterialEffect.setViewBlurEffect(View, BaseBlurEffectParams)`、`setViewStrokeEffect(View, StrokeEffectType)`、`setCornerParams()` 以及焦散阴影接口；模糊最终使用系统的背景 RenderEffect 挂载入口。`BlurEffectParams` 可接受半径、两层颜色字符串及 BlendMode，适合保留业务配色。COUI 的主题预设枚举只见主按钮 / FAB，通用预设主要为顶底栏、搜索与内容按钮，没有确认可直接套用的课程卡片预设。

`COUISpotLightEffectDrawable` 可以独立创建、读取系统配置、接收触摸并绘制；本轮使用实际存在的 `TYPE_TRANSLUCENT_LARGE_1`，保持默认光色。系统光感描边使用 `TYPE_CONTENT_CAPSULE_1` 通用内容轮廓配置。此处是基于系统通用接口构建的课程材质模式，不能称作系统应用的完整原样卡片组件。

官方将凝光视效列为 ColorOS 17 流体设计的一部分，见 [OPPO 官方介绍](https://www.oppo.com/cn/newsroom/press/549/)。本次检索未找到将上述 COUI / Oplus 接口作为通用 Android 公共 SDK 发布的官方文档；接口兼容性以实际安装的系统库为准。Android 标准 [RenderEffect](https://developer.android.com/reference/android/graphics/RenderEffect) 与这里使用的厂商背景采样入口不能混为同一能力。

## 接入边界

`ColorOsCourseCard.kt` 负责原生背景 View、主题、系统材质开关监听与生命周期释放。公共 glass 通过应用组装层提供的 `LocalNativeCourseCardRenderer` 调用，不依赖实验 feature。课程内容仍由 Compose 绘制，背景 View 不拦截点击、周视图拖动或缩放；MotionEvent 只观察并转发给原生光感 Drawable，不叠加 Kyant 按压反馈。

颜色来自原课程配色，模糊半径沿用现有设置和实时预览；凝光着色与轮廓光使用下述独立参数。形状使用已有 Shape 生成原生 View outline；文字继续使用原壁纸对比度采样。超级性能模式沿用原低成本卡片入口。动画遮挡或不可见阶段按现有 `mountMaterial` / viewport 状态停用材质，detach / dispose 清除背景效果、描边与光感动画。

### 凝光模式参数与渐变修订

用户要求凝光材质保留原质感轮廓光的颜色渐变，并重做有效参数。初版提供课程着色、轮廓光强度和模糊滑杆；用户实机反馈原生轮廓消失后，进一步要求轮廓改为不可调且默认更强。现在只保留课程着色与模糊调节；折射、旧玻璃开关及高斯模糊开关只在原模式显示。关闭凝光可以恢复原模式及其既有参数。

`glass/` 只提供课程颜色的纵向渐变着色，原生背景 View 在系统采样之后、原生触摸光之前调用该绘制；渐变 Brush 按卡片高度缓存，没有额外背景采样或 RenderEffect。上一版错误地一起叠加了旧轮廓边缘光、白色描边及白/黑明暗绘制，用户反馈观感偏离系统；此次从凝光路径移除这些绘制，原模式仍保留。文字对比度仅估计实际着色，不随系统轮廓强度变化。

固定轮廓直接调用原生 `setViewStrokeEffect(View, StrokeEffectParams)`，从 `TYPE_CONTENT_CAPSULE_1` 取得配置，仅将 edgeAlpha 提到 2×（上限 1），保留原生边宽、角度与阴影衰减。凝光轮廓不再被旧 `courseCardOutlineLightEnabled` 或保存过的轮廓滑杆值关掉；仍服从系统轮廓开关与页面材质可见性。移除对应滑杆、预览回调和旧偏好读取，不清除用户数据。

卡片尺寸确定及变化后重新挂载原生材质，使用实际 Shape 圆角生成 `OplusMaterialCornerParams`；原生 View 与 COUI 一样禁用自身 clipToOutline，外部内容仍按课程 Shape 裁剪。仅设置 Android View outline 不等于已经给厂商材质传圆角，挂载时零尺寸也不能代替布局完成后的更新。

所有 COUI 跟手光按用户确认固定为系统基准的 2×，课程卡片也不提供跟手光滑杆。首页 Dock 后续按天气参考改用中性光色，设置页保留先前光色，其余控件保持系统默认光色。控件挂载完成或系统材质状态变化后执行一次强度更新，detach 注销监听；每个原生 effect 保存基准与上次应用值，主题重复更新不会把 2× 累乘成 4×。这不是动画帧轮询。

凝光着色参数使用已有派生版 SharedPreferences：滑动即时预览，释放后保存，取消时恢复已保存值；不改 Room、课表导出或备份协议。效果仍受系统模糊、轮廓与跟手光开关控制。

模式开关使用派生版本独立 SharedPreferences，跨 Activity 同步监听；没有增加 Room 列或改写上游导出格式。该设备外观开关不会包含在 `.sleepdown` 备份中，恢复课程与配置不受影响。无法加载组件时回到原卡片入口。

## 验证范围

初版接口名称、构造签名和原生调用路径已对照系统 APK 核实。初版完整签名 Release 构建与 v2 签名核验通过，现有定向路由 / 能力策略测试 8 项通过；初版文件身份与哈希见[交付记录](2026-10-12-coloros-dialog-search-lifecycle.md)。APK 内没有 COUI / Oplus 材质定义类或分析用系统 APK。

此次 Dock 与参数修订完成完整 `assembleGithubRelease`，保留 R8、资源压缩、lintVital 与外部签名，耗时 4 分 17 秒；v2 签名通过，单签名者。候选为 `app/build/outputs/apk/github/release/SleepDown-ColorOS-1.2.7_beta5-dock-material-controls.apk`，7,393,690 字节，SHA-256 `e7ee6c6cc263649289f8aed57083d98cf8650867fa3588dfe62f14fbe7ed9e1f`，包名 `com.xiaomanjun.sleepdownschedule.coloros`，版本 `1.2.7_beta5` / `34`。Compose 报告仍确认独立 Dock 入口可跳过无关输入更新。本次不重复原有路由测试。

ADB 5038 设备列表为空，没有安装或启动。需实机验收 Dock 遮挡层级和快捷操作重组次数、渐变叠加、轮廓光 0/中间值/100%、参数保存及取消预览、2× 跟手光与明暗切换。构建及参数接口核实不能替代这些效果与性能验证。
