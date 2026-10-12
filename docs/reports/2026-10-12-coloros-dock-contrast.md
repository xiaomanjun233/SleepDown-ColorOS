# Dock 可读性、主题色恢复与课程材质回退

用户反馈天气染色后部分 Dock 文字和图标不易阅读，要求增加阴影，恢复加号原来的颜色，并确认 Dock 选中态应保留原主题色；课程卡片的增强轮廓出现胶囊观感，要求恢复初版，只保留课程颜色渐变。

## 实现

- Dock 玻璃仍使用天气中性色；选中图标、文字与加号接回原 `dockAccent` 来源（既有壁纸代表色，无有效颜色时取 COUI 主题容器色），不把它们替换成中性前景色。加号图标恢复 `couiColorOnPrimary`。
- 原生加号的颜色层从 COUI 自身 `getNonThemeFabColor` 取得；保留上轮原生 FAB 半径的 85%，同一组参数用于静止背景与融合材质。真实按钮及菜单返回端点使用同一取色路径。
- 原生 Dock 标题仍是 COUI 的 TextView，使用 1.5dp 轻阴影；图标用 DrawableWrapper 绘制同样的字色对比阴影，按前景亮度选择亮影或暗影。小型 alpha mask 只在图标尺寸、颜色或状态变化后生成，不逐帧增加背景采样、模糊纹理或全屏层；原生触摸、滑动、选中动画保持。
- 阴影仅在首页天气玻璃上启用，进入设置页时清除文字阴影并停用图标阴影。设置齿轮仍使用原 Compose 路径。
- 课程材质恢复首版提交 `14e0f75` 的 enum 预设调用和 `clipToOutline=true`；移除自定义 edgeAlpha 2×、额外 OplusMaterialCornerParams 和自定义描边构造。保留课程颜色渐变、材质生命周期、模糊归零行为及全局 2× 跟手光；不重新加入旧白色描边、边缘光或黑白明暗叠绘，也不恢复轮廓强度滑杆。

## 验证

完整 `assembleGithubRelease` 通过，耗时 4 分 20 秒，包含 R8、资源压缩、lintVital 与 Release 签名；v2 核验有效，单签名者。候选保存在不会被后续 Gradle 打包清空的忽略目录：`oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-dock-contrast-course-defaults.apk`，7,393,690 字节，SHA-256 `3607c56382530e7eb24a92c3e8aa7be0d818c0d01156447eb0cd1c0c9ec3d0db`。包名、版本和显示名称保持此前派生身份。

构建后 SDK ADB 5038 设备列表为空，尚未覆盖安装，已请用户重新连接。此轮为可逆的局部视觉调整，不追加机械 UI 测试。需实机验收明暗壁纸上的文字与图标、主题色选中态与加号、融合返回端点、设置页切换，以及恢复初版的课程轮廓与渐变。
