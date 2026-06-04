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

核电由反应堆方块、外部接口和内部组件组成。先读 [核反应堆系统](../systems/nuclear_reactor.md)，再用本页核对组件职责。

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

## 安全检查

- 新堆型先空载检查冷却布局，再逐步加燃料。
- 流体冷却堆必须保证冷却液供应与热冷却液输出畅通。
- 堆温超过安全线后优先停堆，不要边运行边改内部布局。
