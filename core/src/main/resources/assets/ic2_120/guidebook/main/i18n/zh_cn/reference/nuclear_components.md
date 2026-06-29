---
navigation:
  title: 核电方块与组件
  parent: index.md
  position: 216
  icon: ic2_120:nuclear_reactor
item_ids:
  - ic2_120:uranium_fuel_rod
  - ic2_120:dual_uranium_fuel_rod
  - ic2_120:quad_uranium_fuel_rod
  - ic2_120:mox_fuel_rod
  - ic2_120:dual_mox_fuel_rod
  - ic2_120:quad_mox_fuel_rod
  - ic2_120:depleted_uranium_fuel_rod
  - ic2_120:depleted_dual_uranium_fuel_rod
  - ic2_120:depleted_quad_uranium_fuel_rod
  - ic2_120:depleted_mox_fuel_rod
  - ic2_120:depleted_dual_mox_fuel_rod
  - ic2_120:depleted_quad_mox_fuel_rod
  - ic2_120:lithium_fuel_rod
  - ic2_120:depleted_isotope_fuel_rod
  - ic2_120:reactor_coolant_cell
  - ic2_120:triple_reactor_coolant_cell
  - ic2_120:sextuple_reactor_coolant_cell
  - ic2_120:heat_vent
  - ic2_120:reactor_heat_vent
  - ic2_120:overclocked_heat_vent
  - ic2_120:component_heat_vent
  - ic2_120:advanced_heat_vent
  - ic2_120:heat_exchanger
  - ic2_120:reactor_heat_exchanger
  - ic2_120:component_heat_exchanger
  - ic2_120:advanced_heat_exchanger
  - ic2_120:reactor_plating
  - ic2_120:reactor_heat_plating
  - ic2_120:containment_reactor_plating
  - ic2_120:neutron_reflector
  - ic2_120:thick_neutron_reflector
  - ic2_120:iridium_neutron_reflector
  - ic2_120:rsh_condensator
  - ic2_120:lzh_condensator
  - ic2_120:fuel_rod
  - ic2_120:uranium
  - ic2_120:uranium_235
  - ic2_120:uranium_238
  - ic2_120:uranium_pellet
  - ic2_120:mox
  - ic2_120:mox_pellet
  - ic2_120:plutonium
  - ic2_120:rtg_pellet
  - ic2_120:containment_box
---

# 核电方块与组件

核电由反应堆方块、外部接口和内部组件组成。先读 [核反应堆](../machines/nuclear_reactor.md)，再用本页核对组件职责。

## 反应堆方块

| 方块 | 用途 |
|------|------|
| 核反应堆 | 主控方块，放置燃料棒与冷却组件 |
| 核反应仓 | 扩展反应堆内部空间 |
| 反应堆访问接口 | 自动化输入输出反应堆物品 |
| 反应堆流体接口 | 流体冷却模式下输入冷却液、输出热冷却液 |
| 反应堆红石接口 | 用红石控制或读取反应堆状态 |
| 核反应堆压力容器 | 流体冷却堆的结构外壳 |

## 燃料棒

- 铀燃料棒、双联铀燃料棒、四联铀燃料棒产生 EU 和热量。
- MOX 燃料棒在高堆温下收益更高，设计风险也更高。
- 枯竭燃料棒是燃料循环产物，用于后续处理。
- 锂燃料棒、近衰变铀棒属于进阶核燃料链。

## 冷却与换热组件

| 组件 | 作用 |
|------|------|
| 10k / 30k / 60k 冷却单元 | 存储热量，本身不主动散热 |
| 散热片 | 自身散热 |
| 反应堆散热片 | 从堆吸热并自身散热 |
| 超频散热片 | 强力吸堆热，热压力大 |
| 元件散热片 | 冷却邻接的可储热组件 |
| 高级散热片 | 更高自身散热 |
| 热交换器系列 | 在堆、组件、邻接组件之间重新分配热量 |

## 隔板、反射板与冷凝器

- 反应堆隔板增加热容量并降低爆炸影响。
- 高热容反应堆隔板偏向提升热容量。
- 密封反应堆隔热板偏向降低事故影响。
- 中子反射板会反射相邻燃料棒脉冲；铱中子反射板不消耗耐久。
- 红石冷凝模块、青金石冷凝模块可吸收热量，但需要用对应材料修复。

## 密封储存箱

密封储存箱（Containment Box）是一种储存方块，用于安全存放枯竭燃料棒和其他放射性物品。除了提供核废料的主题容器外，没有特殊机制。

## 燃料循环

核燃料链遵循一个循环流程：

1. **采矿**：在 Y −64 至 Y 63 之间开采铀矿石，通过打粉机加工获得粉碎铀矿石。
2. **富集**：将粉碎铀矿石放入热力离心机，获得**铀-235**和**铀-238**。
3. **燃料棒合成**：将铀-235 与铀-238 按 1:1 比例合成为燃料棒。燃料棒有三种尺寸：单联、双联（2倍输出）和四联（4倍输出）。
4. **反应堆运行**：将燃料棒放入核反应堆。运行期间产生 EU 和热量，必须使用冷却组件（冷却单元、散热片、热交换器）管理热量。
5. **消耗**：经过一定数量的反应堆周期后，燃料棒变为枯竭燃料棒（尺寸对应）。
6. **后处理**：将枯竭燃料棒放入热力离心机，回收**铀-238**、**小堆钚**和**铁粉**。
7. **MOX 燃料**：将钚与铀-238 合成为**MOX 燃料棒**。MOX 燃料棒在堆温越高时发电量越大——高风险高回报的权衡。

此后循环回到第 4 步，使用新燃料棒继续运行。

### 关键比例

| 流程 | 输入 | 产出 |
|------|------|------|
| 离心（粉碎铀） | 1 粉碎铀矿石 | 1 小堆铀-235 + 3 小堆铀-238 |
| 离心（枯竭铀燃料棒） | 1 枯竭铀燃料棒 | 1 小堆钚 + 4 小堆铀-238 + 铁粉 |
| 离心（枯竭双联/四联） | 1 枯竭双联/四联燃料棒 | 等比缩放产出（×2 或 ×4） |

## 安全检查

- 新堆型先空载检查冷却布局，再逐步加燃料。
- 流体冷却堆必须保证冷却液供应与热冷却液输出畅通。
- 堆温超过安全线后优先停堆，不要边运行边改内部布局。
