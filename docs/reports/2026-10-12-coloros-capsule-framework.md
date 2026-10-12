# ColorOS 框架、胶囊按钮及菜单配色

## 交付范围

- 提取 `coloros/` 专用框架并统一 AppTheme 装配，运行时加载系统 COUI；课程管理 Activity 的 FQCN 保持原值。
- 共用 LiquidButton 的真实按钮路径、DialogLiquidButton、Alert 操作按钮接入原生 COUIButton，覆盖设置、导入、AI 助理、多课表等页面。原生路径在创建 Kyant 交互状态前返回。
- 保留加号主题色底、固定白色图标；形变起点透明度对齐原生 FAB，展开菜单和形变目标共用中性色。
- 右上角菜单在系统列表绑定之后同时修正文字、ImageView tint 与复合图标反色。
- 取消/继续编辑/返回检查使用显式 Cancel 语义；三排弹窗的取消在最下方，确认使用主题色与白字。回调按原 action 索引派发。
- 固定白字的多课表按钮指定深色原生材质，保留可读性。

框架边界及后续接入方式见 [SLEEPDOWN_COLOROS_FRAMEWORK.md](../architecture/SLEEPDOWN_COLOROS_FRAMEWORK.md)。非按钮弹层外壳、搜索输入表面和共享手势宿主保留原有结构；本轮没有改动数据、包名、名称或导航协议。

## 验证

- `:app:assembleGithubRelease`：通过，保留 R8、资源压缩、lintVital 与正式签名。最终构建耗时 5 分钟。
- `:app:testGithubDebugUnitTest --tests com.xiaomanjun.sleepdownschedule.coloros.ColorOsAlertSlotsTest`：3 项通过、0 失败，覆盖三按钮全部排列、取消/删除映射、单操作及空表单。
- 通过本地系统 Settings 类验证 COUI 纵向排序为 neutral → positive → negative；取消映射 negative。
- `apksigner verify --verbose`：通过，v2 签名、1 位签名者。
- ADB 5038 本轮设备列表为空：未安装、未完成手机视觉及触摸验收。安装后重点验收菜单交接配色、菜单图标反色、原生按钮滚动取消、三排弹窗，以及 shortcut / 多课表中的快照表现。

## 安装包

- 文件：`oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-capsule-framework.apk`（仅本地交付，不进入 Git）。
- applicationId：`com.xiaomanjun.sleepdownschedule.coloros`。
- 版本：`1.2.7_beta5` / `34`，名称沿用 SleepDown课程表。
- 大小：7,410,074 字节。
- SHA-256：`768441e7fcad43abc4adda2503bcb47fec6bbac0947bccb83f093a2962c11383`。

此文件是本轮修订候选包，没有创建新标签或发布 GitHub Release。
