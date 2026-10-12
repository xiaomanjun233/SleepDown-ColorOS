# 课程与表头光感、弹窗操作顺序修订

## 材质

- 课程凝光、普通周表头和日期胶囊沿用各自形状，将 Dock 的 `TYPE_FRAMEWORK_CAPSULE_3` 描边宽度放大到 1.3×。本机提取的预设宽度为 2.5dp，对应本轮 3.25dp；其余亮度、方向、阴影衰减参数直接读取系统预设。
- 不添加新的圆角参数，不把课程形状改成胶囊，不叠加旧白色轮廓绘制。课程配色渐变及默认 50% 着色继续保留。
- 表头模糊半径改为系统通透按钮预设的 60%。日期胶囊恢复配色渐变：上部 30% 着色，向底部过渡到 50%；Brush 随高度缓存，文字仍由 Compose 绘制。
- Dock 自身材质与 2× 跟手光强度不变；AI 助手也不使用本轮课程/表头的宽度、半径覆盖值。

## 弹窗和操作语义

- 三排弹窗主按钮在顶部，普通操作居中，取消在底部。通过语义优先级映射到 COUI 的 neutral / positive / negative 槽位，回调仍使用原 action 索引。
- COUIButtonBarLayout 的推荐按钮 ID 同步到实际主按钮，避免位置变更后原生材质刷新又套用其他按钮类型。Compose 三排后备布局采用同一顺序。
- 两处分享入口的 ICS 操作本来就是 Secondary。错误来自原生位置默认色未被覆盖，现在明确恢复普通文字颜色；删除类 Destructive 仍用系统错误色。
- 课程 shortcut 的删除垃圾桶与文字保留 `#FF453A`，继续经 `NativeOverflowMenu` 的逐项图标/文字 tint 覆盖原生适配器默认色。

## 验证

- `ColorOsAlertSlotsTest` 的 4 项定向测试通过，覆盖三排排列、分享操作回调、删除确认与单操作映射。
- `:app:testGithubDebugUnitTest --tests '*ColorOsAlertSlotsTest' :app:assembleGithubRelease` 成功，耗时 7 分 54 秒，包含 R8、资源压缩、lintVital 与签名打包。
- APK 的 v2 签名校验通过。包名 `com.xiaomanjun.sleepdownschedule.coloros`，显示名 `SleepDown课程表`，版本 `1.2.7_beta5` / `34`。
- 本地候选包：`oplusbox-analysis/releases/SleepDown-ColorOS-1.2.7_beta5-rim-header-alert.apk`，7,410,074 字节。
- SHA-256：`170ae5281d71ff4e0084c442c7e41f2a6d91687d52a703c534eb2fbcc6653d87`。
- 用户当前不便连接手机，本轮未覆盖安装，也未验证实机圆角光感、模糊程度和原生弹窗显示效果。
