# 原生选择器输入、按钮白框与预览恢复

## 用户验收纠正

用户报告前一包选择器/按钮不能操作，按钮内部有白框；Dock 仍受滑条影响，且预览效果丢失。用户曾误选“文字与按钮形变不同步”，随后明确纠正为“压根没反应”。本轮没有按文字动画问题修改按钮。

上轮将首页预览缩放视为 Dock 异常根因的判断不足，相关去缩放修改已撤回：恢复页面回到原尺寸、背景模糊释放及弹层采样比例同步的完整预览。上一轮桌面测试只覆盖生命周期与布局，不能验证真机上的这些问题。

## 修订

- 原生表单关闭拦截器将 `getGlobalVisibleRect` 的 decor/root 坐标与 `MotionEvent.rawX/rawY` 屏幕坐标混用。原生窗口偏移时，内部点击会被误判为外部点击并吞掉。现在使用同窗口的 `event.x/y`；内部起始的滚动即使移出面板也继续交给内容，外部完整点击才请求关闭。
- 系统 COUIButton 的 `initStatefulBackground` 会将构造时的旧背景保存在 `COUIStateEffectDrawable` 内，`setButtonType` 不清理它。对框架自行创建的胶囊调用 COUIButton 自己的 `setBackground(null)`，只清除这层旧背景；保留原生状态 drawable、跟手光、材质、描边和运动绑定。白框消失与否仍需实机核验。
- 用户随后明确要求统一原生 Slider、开关与分段控件。最终接入公共 `NativeValueControls`，ColorOS 可用时在旧控件入口提前返回，不再建立 Kyant 滑块/开关形变采样图；试做的壁纸专用采样补丁已撤掉。完整页面及全屏模糊路径仍包含 Dock。尚无 ColorOS GPU 实机复现证据，不将代码路径隔离说成已证明 Dock 异常消失。
- 课程卡与表头遗漏了材质专用圆角。补齐真实 Shape → CornerParams → 系统描边的顺序，保留课程渐变，撤掉 Dock 描边预设及 1.3 倍宽度覆盖。实际课程圆角不会被设为半高胶囊。
- 教务灵动岛此前被 LiquidButton 通用替换套成 COUIButton；改为原生面板材质，继续使用岛自身动态圆角，消除重复胶囊外壳。
- 凝光模糊显示 0%–100%，0% 映射系统半径的 20%，50% 对应默认、100% 对应两倍。旧保存比例低于最低值时钳制，其余旧值保留效果。
- 加号菜单的动态/静止材质，以及普通/级联菜单的背景插槽改为原生材质，保留原有菜单状态机和交互。

## 验证

- 定向测试 12 项通过：窗口坐标与触摸序列 3 项、原生选择器宿主生命周期 1 项、材质圆角与尺寸变化 4 项、Slider 预览/提交/取消及模糊映射 4 项。取消事件的延后处理另加状态检查，避免影响已开始的下一次拖动；数值控件的 4 项测试已重新通过。
- `assembleGithubRelease` 完整构建通过，包含 R8、资源压缩、lintVital 和签名打包。最终安装包包含默认值标记垂直居中的修复，以适配原生滑条的高度。
- 首次构建被另一个进程占用的 Miuix 产物阻塞，后续通过本地 init script 将依赖构建输出放到本项目 `tmp/miuix-build`，未结束其他项目进程。新增菜单材质补丁的逆向检查通过，README 与 CI 均按同一补丁顺序应用。
- 本轮没有连接 ADB，未覆盖安装。测试不加载厂商 GPU 材质；按钮白框、课程/表头高光、菜单实际采样、控件手感及 Dock 是否仍异常形变，均留待用户实机验收。

## 安装包

- 本地交付：`oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-native-controls-material.apk`，7,426,458 字节，不入 Git。
- 包名 `com.xiaomanjun.sleepdownschedule.coloros`；名称 `SleepDown课程表`；版本 `1.2.7_beta5` / `34`。
- `apksigner verify` 通过，V2 签名有效，证书与上一份 `native-pickers.apk` 一致。
- SHA-256：`36112BC80394A62F6745D5E2B96AFAE88D82CB779581F09AF9F377F4ABA2B2C6`。
