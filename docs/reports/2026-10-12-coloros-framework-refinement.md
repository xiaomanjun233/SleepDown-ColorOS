# ColorOS 配色、弹窗语义与个性化预览修订

## 菜单和今日助手

- 加号菜单外层已经按首页文字计算前景色，`AddMenuLiquidItem` 却重新调用 `glassForegroundColor(menuConfig)`；显式向行传入外层颜色，保留每个操作自己的语义色覆盖。
- 原生删除红色失效有两层原因：COUI 的适配器默认强制图标染色；旧的后绑定修补只认识 `AdapterView`，但系统菜单也会用 `RecyclerView`。改为原生 `PopupListItem.setForceTint(0)` 和 `setTitleColorList`，图标在入列前设置 tint。两种适配器的 `setIcon` / `setText` 实现均核对过，不再遍历 View 树补色。
- 今日助手卡片及内部可点击表面通过 `LocalNativeMaterialSurfaceActive` 判断实际原生路径，移除这一路径叠加的旧黑色按压遮罩；后备绘制仍有反馈。

## 深色壁纸主题色

旧实现复用课程色板。课程色板过滤 HSV 明度低于 0.22 的颜色，ColorOS 又重复过滤，容易把暗红、暗绿等壁纸变成默认蓝色。

ColorOS 现在在后台从原壁纸独立提取色相，关闭 Palette 的默认颜色过滤，再过滤真正无色相或接近全黑的颜色。按钮保持原来的饱和度 0.50–0.85、明度 0.60–0.80 范围；只有 Dock 继续提高对比度。黑白壁纸保留默认蓝色；不修改课程色板和设置页配色。

## 弹窗审计

本次检查了应用及 ColorOS 入口中 30 个文件的 127 处 `LiquidAlertAction` 声明，并检查共享 Alert / 表单头的排列。选择器条目保持原有选择顺序。

| 场景 | 主操作 | 普通/破坏性操作 | 取消或返回 |
| --- | --- | --- | --- |
| 分享课表 | 分享口令 | ICS 为普通操作 | 取消 |
| 退出编辑、作息方案、调休编辑 | 保存/完成 | 放弃、不保存为破坏性 | 继续编辑 |
| 删除课程或课表 | 无主题色主操作 | 仅本次、全部、删除均为破坏性 | 取消 |
| 导入确认 | 创建新课表 | 覆盖当前课表为破坏性 | 返回检查/取消 |
| 备份恢复 | 空应用恢复 | 覆盖已有数据为破坏性 | 返回检查 |
| WebDAV | 查看备份 | 覆盖远端备份为破坏性 | 稍后/取消 |
| 权限、AI 启用、下载更新 | 启用、授权、安装或重试 | 查看正文、备用下载为普通操作 | 暂不启用/稍后 |
| 教务抓取确认 | 确认或进入识屏 | 另一种识别方式为普通操作 | 返回重抓 |
| 冲突处理 | 暂时保留 | 应用全部为普通操作 | 返回修改 |
| 后台进度提示 | 保留原后台继续入口 | 不标为破坏性 | 不取消后台任务 |

统一顺序为 Primary → Secondary → Destructive → Cancel；同类操作保持原声明顺序。原生纵向按这一顺序映射到实际槽位，Compose 横向按相反顺序排列，确认在右。没有 Primary 的原生弹窗清除推荐按钮 ID，防止后续材质更新把删除套成主题色按钮。仅改语义和呈现，不改变删除、导入、恢复、退出确认的回调或数据流程。

## 个性化和 Dock

- 凝光模式对象稳定持有 State，框架 provider 不读取每个滑杆值。消费者按需订阅，预览不再替换整个模式对象。
- 着色变化只重绘原生表面的 tint，不重新安装原生 blur / stroke / light。模糊变化更新 blur 参数；Dock 的 2× 跟手光保持原设置。
- 个性化从冻结快照转为实时预览时，旧路径把动态 RenderEffect 挂到整个页面祖先，包括原生 Dock。现在先在中性父层录制完整页面，再对录制层模糊。Dock 仍属于同一页面、快照和背景模糊。
- 凝光模糊滑杆范围 0–100，50 为系统默认，100 为两倍半径；0 不安装 blur effect，保留课程渐变和描边。内部保存比例仍为 0–2，旧值仅换算显示。设置沿用派生版独立 SharedPreferences，预览结束恢复已提交值，不改变备份或数据库格式。
- 凝光开关、着色、模糊滑杆加入逐行进入动画，并计入后续行号及动画时长，包括旧质感轮廓光行。

## 顶栏叠加模糊

首页渐变层位于 zIndex 10，按钮位于 zIndex 11；公共二级页 `SettingsGradientTopBar` 先绘制 progressive backdrop，随后绘制原生按钮。按钮的 `TYPE_DEFAULT_BLUR` / 天气预设会继续对 View 背景模糊，存在两层叠加。

新增局部 `LocalToolbarBackdropBlurred`，由首页及公共设置/二级页顶栏提供；原生返回、右侧图标按钮在已有渐变层时将自身模糊半径缩为对应预设的 0.35 倍。无渐变顶栏保留完整半径。`ToolbarMaterialEffectDelegate.setCustomBlurParams` 只替换 blur 参数，继续保留颜色层、混合模式、描边、跟手光和原生动画；没有关闭材质，也没有把渐变样式恢复为旧算法。

## 半屏课表设置待确认

用户确认是整体缩小、底部悬空。实际依赖为 `D:/Android studio/CourseSchedule/miuix-local`：`OverlayBottomSheet` 外边距默认为 0、底部圆角为 0，宿主是首页根 Scaffold 的全屏后绘制节点，最大宽度 640dp。业务仅限制高度为 590dp。尚未确认是首次打开还是从详细设置返回后发生，也未取得故障帧或布局信息；不能将其归因为内容边距。半屏菜单、日期子菜单及详细设置交接尚未修改，这一问题未解决。

## 验证

- 10 项定向测试通过：`ColorOsDockAccentTest` 3 项、`ColorOsDockCompositionTest` 3 项、`ColorOsAlertSlotsTest` 4 项，0 失败。测试证明暗色原图可保留色相、原生模式连续预览只更新订阅者、Dock 宿主不额外提交、已有弹窗回调映射正确。
- 首次 `:app:assembleGithubRelease` 与上述测试合并执行成功，耗时 8 分 10 秒。追加顶栏模糊及 0–100 滑杆修订后，于 10 月 13 日完成最终完整构建，耗时 5 分 10 秒，包含 R8、资源压缩、lintVital 和签名打包；APK v2 签名验证通过。用户澄清关注加号展开菜单，最终版本未包含额外的加号本体材质调整。
- 最终候选：`oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-framework-final.apk`，7,410,074 字节。
- SHA-256：`508260fbacdfe56750f985bb032812cee5cc840c4c538ad4da5f2bee8fd8cc25`。
- 包名 `com.xiaomanjun.sleepdownschedule.coloros`，名称 `SleepDown课程表`，版本 `1.2.7_beta5` / `34`。
- 本轮未连接或安装手机。桌面测试不能证明 COUI 的 GPU 光效、按压、菜单配色和 Dock 视觉稳定性已经验收；需检查加号菜单浅色反色、shortcut 红色删除、今日助手按压、各个性化滑杆和 0 / 50 / 100 模糊。
