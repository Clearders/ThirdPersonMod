# Cinematic Shoulder Camera

面向 Minecraft Java Edition 26.2、Fabric Loader 和 Java 25 的纯客户端第三人称越肩相机 Mod。

它只在第三人称后视中改写 `Camera` 的渲染位置、为最终视野增加可关闭的轻微动态偏移，并把原版玩家眼睛射线的目标投影成 HUD 准星。不修改玩家坐标、速度、yaw、pitch、头部或身体旋转，也不修改移动输入、鼠标输入、`hitResult`、攻击、挖掘、放置、实体交互或服务器逻辑。项目没有服务端入口点、网络数据包或 OpenGL 调用，可连接未安装本 Mod 的原版/Fabric 服务器，并适用于 Minecraft 的 OpenGL 与实验性 Vulkan 后端。

“科幻电影式越肩视角”只描述相机构图与运动手感。本项目不包含或添加任何《星球大战》角色、名称、武器、Logo、音乐、音效、UI 或其他素材。

## 行为

- 仅在第三人称后视、本地玩家拥有 camera entity 且 `Camera#entity()` 同样是本地玩家时工作。
- Tweakeroo 26.2 Free Camera、旁观其他实体、Replay/freecam 类相机接管时立即让出控制权，并清空平滑状态。
- Free Camera 关闭后，从当前原版玩家相机重新初始化；不会从旧的 Free Camera 世界坐标飞回。
- 五射线（中心与四角）近似相机体积碰撞。进墙方向使用硬安全上限，离墙方向指数平滑恢复。
- 碰撞缩短距离时按距离比例衰减肩偏移，使构图从完整越肩逐步退化为安全的近距离背后视角。
- 左右肩、垂直偏移和距离恢复使用基于实际帧间隔的指数平滑；yaw/pitch 不做额外平滑。
- 默认启用轻微的电影式运动：速度提高时相机平滑拉远并小幅增加视野；使用弓、弩、三叉戟、长矛、盾牌或望远镜时收紧构图。
- 校正准星只显示原版玩家眼睛射线的实际目标；目标位于屏幕后方或画面外时隐藏，不会改变瞄准与交互结果。
- 第一人称、第三人称正面、睡觉（默认）、骑乘（默认）、爬行（默认）和鞘翅飞行（默认）保持原版行为。

禁用 Mod 总开关后，从下一次相机更新起不再写入相机位置，原版相机立即恢复。

## 按键

按键可在 Minecraft 的“控制”界面重新绑定：

- `O`：启用／禁用越肩相机
- `P`：切换左肩／右肩
- `I`：打开游戏内相机设置

GUI 或覆盖层打开时不会触发这些动作。

## 配置

首次启动会在 Fabric 配置目录创建 `thirdpersonmod.json`。JSON 损坏、根节点为空或枚举非法时会记录清晰错误并使用安全默认值，不会阻止游戏启动。

游戏中按 `I` 可直接修改全部配置。界面分为“相机”“碰撞”“行为”和“运动与 HUD”四页，所有修改都会实时预览；“完成”会校验、应用并写入 JSON，“取消”或 `Esc` 会恢复已保存的设置。“重置”只会预览默认值，仍需点击“完成”才会保存。所有选项都提供悬停说明。

```json
{
  "enabled": true,
  "distance": 3.6,
  "shoulderOffset": 0.75,
  "verticalOffset": 0.35,
  "minimumDistance": 0.35,
  "collisionRadius": 0.14,
  "collisionSafetyMargin": 0.1,
  "verticalSmoothingSpeed": 14.0,
  "collisionInSpeed": 32.0,
  "collisionOutSpeed": 9.0,
  "shoulderTransitionSpeed": 11.0,
  "compositionPreset": "CINEMATIC_RIGHT_SHOULDER",
  "defaultShoulder": "RIGHT",
  "disableWhileRiding": true,
  "disableWhileSleeping": true,
  "disableWhileSwimming": false,
  "disableWhileCrawling": true,
  "disableWhileFallFlying": true,
  "debugCameraOwnership": false,
  "cinematicMotionEnabled": true,
  "motionStrength": 0.35,
  "dynamicFovEnabled": true,
  "focusWhileAiming": true,
  "correctedCrosshairEnabled": true
}
```

所有数值在加载时会限制到安全范围。`CompositionPreset` 实现了：

- `CINEMATIC_RIGHT_SHOULDER`
- `CINEMATIC_LEFT_SHOULDER`
- `COMPACT_RIGHT_SHOULDER`
- `COMPACT_LEFT_SHOULDER`
- `VANILLA_SAFE`
- `CUSTOM`

预设提供一组可应用的基准值；JSON 中的 `distance`、`shoulderOffset`、`verticalOffset` 和 `defaultShoulder` 是最终值。手动修改后会自动显示 `CUSTOM`，参数重新与某个预设完全一致时也会自动识别该预设。左右肩运行时切换会保存 `defaultShoulder`。旧配置中的 `positionSmoothingSpeed` 会继续读取，并在下次保存时迁移为 `verticalSmoothingSpeed`。

所有平滑速度都以“数值越大、响应越快”为准；`0` 表示不使用平滑并立即应用目标值，而不是冻结相机状态。

`debugCameraOwnership` 启用后，只在所有权状态或实体类型变化时记录 camera entity、focused entity、Tweakeroo 是否加载和外部相机检测结果，不会逐帧刷屏。

## 视差说明

这是视觉相机 Mod。越肩偏移会产生近距离视差，玩家眼睛射线仍是攻击与交互的唯一依据。启用校正准星后，HUD 会把 `hitResult` 的世界位置投影到最终相机画面中，使显示位置与实际目标一致；它不会替代玩家视线、重算 `hitResult`、辅助瞄准或修正攻击方向。

## 构建

Gradle 本身需要使用 Java 17 或更高版本启动，项目会明确选择 Java 25 工具链来编译和运行 Minecraft。若系统默认 `java` 仍指向 Java 8，请先设置 `JAVA_HOME`；例如本机可使用 JDK 21 启动 Gradle：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
./gradlew.bat test
./gradlew.bat build
```

输出 JAR 位于 `build/libs/thirdpersonmod-1.1.0.jar`。

## 26.2 API 核对

本项目按 26.2 实际官方 Mojang 类名和字节码实现：

- `Camera#update(DeltaTracker)` 取代旧版本常见的带 world/entity/third-person 参数签名。
- 当前 focused entity 通过 `Camera#entity()` 读取。
- forward/up/left 通过 `Camera#forwardVector()`、`upVector()`、`leftVector()` 读取。
- `Camera#setPosition(Vec3)` 仍为 protected，仅通过最小 Mixin Invoker 调用；位置在 `alignWithEntity` 之后、FOV 与视锥计算之前写入。
- GUI 状态位于 `Minecraft#gui.screen()` / `overlay()`，不再是旧版公开 `Minecraft#screen` 字段。

核心注入使用精确描述符 `update(Lnet/minecraft/client/DeltaTracker;)V`、默认 Mixin 优先级和强制注入要求；没有 `require = 0`。

## Tweakeroo 26.2 兼容结论

已核对 `tweakeroo-fabric-26.2-0.29.2.jar`。其 Free Camera 创建独立的 `fi.dy.masa.tweakeroo.util.CameraEntity`，并通过 `Minecraft#setCameraEntity` 接管/恢复相机。即使开启“玩家输入”，它也只在原版 `pick` 调用内部临时选择玩家射线，不会把实际渲染 camera entity 伪装成本地玩家。

因此本 Mod 只需要稳定的通用 camera entity 仲裁，不编译链接 Tweakeroo、不反射其内部配置类，也不把它声明为依赖。

## 手动验收清单

### 基础与碰撞

- 第三人称后视有明显电影式越肩构图；第一人称和第三人称正面完全原版。
- 左右肩切换无瞬间横跳；30、60、144 FPS 下过渡时间接近。
- 快速转动 yaw/pitch 无额外拖手。
- 平墙、内外墙角、楼梯、栅栏、玻璃和半砖附近逐项检查五射线碰撞。
- 靠墙立即受安全上限约束，离墙缓慢恢复；肩位切换过程中仍不穿墙。
- 洞穴和窄走廊中肩偏移随有效距离缩小并逐步居中。
- 站立、步行和疾跑之间距离/FOV 平滑变化；使用瞄准类物品时构图平滑收紧。
- 在多个 GUI 缩放与窗口宽高比下检查近距离和远距离方块/实体目标；画面外目标不显示准星。
- 在视锥边缘转动相机，确认最终越肩位置不会造成物体过早剔除。

### Tweakeroo 0.29.2

- 未安装 Tweakeroo 时正常启动。
- 已安装且 Free Camera 关闭时越肩相机正常。
- 越肩开启时启用 Free Camera，当帧停止写相机；可远离玩家且不会被拉回。
- Free Camera 中切换本 Mod 肩位，不影响自由相机。
- 关闭 Free Camera 后从玩家当前相机重新初始化，不从远处插值飞回。
- 玩家背靠墙退出 Free Camera，碰撞上限立即生效。
- 分别切换 Tweakeroo 的 player inputs / player movement 选项，所有权判断不受影响。

### 生命周期与后端

- 死亡/重生、切换维度、退出服务器、进入另一个世界。
- 暂停/恢复、切出窗口/恢复、第一/第三人称快速切换、运行时总开关。
- 分别在默认 OpenGL 后端和实验性 Vulkan 后端进入世界并完成上述相机测试。
