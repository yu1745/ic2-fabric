# 传动轴与伞齿轮实现总结

## 已实现功能

1. 添加了四种独立的传动轴方块：
   - `wood_transmission_shaft` - 木制传动轴
   - `iron_transmission_shaft` - 铁制传动轴
   - `steel_transmission_shaft` - 钢制传动轴
   - `carbon_transmission_shaft` - 碳纤维传动轴
2. 传动轴视觉厚度为方块的 `1/6`。
3. 传动轴渲染器使用八边形横截面（8棱柱）。
4. 传动轴纹理映射自：
   - `assets/ic2/textures/item/rotor/wood_rotor_model.png`
   - `assets/ic2/textures/item/rotor/iron_rotor_model.png`
   - `assets/ic2/textures/item/rotor/steel_rotor_model.png`
   - `assets/ic2/textures/item/rotor/carbon_rotor_model.png`
5. 添加了单方块 `bevel_gear`（伞齿轮），视觉上包含两个啮合的伞齿轮，用于90度方向转换。
6. 伞齿轮现在渲染为8齿视觉几何（4个齿条 × 每个齿条2齿）。
7. 修复了传动轴旋转轴问题，使旋转不再在错误的法线轴上自旋。
8. 为 `bevel_gear` 添加了右键调节功能：
   - 循环切换距离值（最大值 → 最小值循环）。
   - 在玩家聊天框显示当前距离值。
   - 当前最大范围接近方块边界（`0.46` 上限值）。
9. 伞齿轮外半径现在从几何计算（偏移驱动）而非固定硬编码大小。
10. 添加了共享的传动方块实体 + 客户端 BER 连线和注册。
11. 为新方块添加了 blockstate/model/item model/lang 资源。
12. 系统保持与现有电力机器逻辑和能量网络的解耦。

## 关键类和枚举

### ShaftMaterial 枚举
定义四种传动轴材质类型：
- `WOOD` - 基础木制轴
- `IRON` - 铁制轴（更好的耐久度）
- `STEEL` - 钢制轴（高耐久度）
- `CARBON` - 碳纤维轴（最佳耐久度）

### BevelPlane 枚举
定义伞齿轮的三个90度方向平面：
- `XY` - 水平面（Z轴旋转）
- `XZ` - 垂直面（Y轴旋转）
- `YZ` - 侧面（X轴旋转）

### BaseTransmissionBlock
所有传动方块的抽象基类。处理：
- 方向属性管理
- 共享方块行为
- 基于材质的属性

### 传动方块类
使用 `@ModBlock` 注解的四种传动轴方块类：
- `WoodTransmissionShaftBlock` - 木制传动轴
- `IronTransmissionShaftBlock` - 铁制传动轴
- `SteelTransmissionShaftBlock` - 钢制传动轴
- `CarbonTransmissionShaftBlock` - 碳纤维传动轴
- `BevelGearBlock` - 伞齿轮

## 关键方法

### distanceFromStep()
将距离步长（0-15）转换为实际几何距离，用于伞齿轮偏移。

### preferredNeighborAxis()
确定传动轴对齐的首选连接轴。

### connectedAxes()
返回通过伞齿轮连接的轴列表，用于旋转传播。

## 主要文件

1. `src/main/kotlin/ic2_120/content/block/transmission/TransmissionBlocks.kt`
2. `src/main/kotlin/ic2_120/content/block/transmission/TransmissionBlockEntity.kt`
3. `src/client/kotlin/ic2_120/client/renderers/TransmissionBlockEntityRenderer.kt`
4. `src/client/kotlin/ic2_120/client/ClientBlockEntityRenderers.kt`
5. `src/main/kotlin/ic2_120/Ic2_120.kt`

## 尚未实现的计划项目

1. 尚无真正的机械传播图（当前动画仍是恒定时间视觉旋转）。
2. 无服务端动能传输状态（速度/符号通过连接的传动轴和伞齿轮传播）。
3. 未添加自动化测试（验证仅限于编译/运行时验证）。
4. 未为新传动方块添加合成配方。
