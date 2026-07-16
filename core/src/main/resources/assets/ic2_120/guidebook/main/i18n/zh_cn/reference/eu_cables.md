---
navigation:
  title: EU 电缆与变压
  parent: index.md
  position: 213
  icon: ic2_120:copper_cable
item_ids:
  - ic2_120:tin_cable
  - ic2_120:insulated_tin_cable
  - ic2_120:copper_cable
  - ic2_120:insulated_copper_cable
  - ic2_120:gold_cable
  - ic2_120:insulated_gold_cable
  - ic2_120:double_insulated_gold_cable
  - ic2_120:iron_cable
  - ic2_120:insulated_iron_cable
  - ic2_120:double_insulated_iron_cable
  - ic2_120:triple_insulated_iron_cable
  - ic2_120:glass_fibre_cable
  - ic2_120:splitter_cable
  - ic2_120:limiter_cable
---

# EU 电缆与变压

电缆负责把 EU 从发电机、储电设备送到机器。机器有输入等级，电缆有传输等级，接线时要同时考虑最大传输、损耗和触电风险。

## 电缆规格

| 电缆 | 等级 | 额定传输 | 损耗 |
|------|------|----------|------|
| 锡质导线 / 绝缘锡质导线 | 1 | 32 EU/t | 0.2 EU/格 |
| 铜质导线 / 绝缘铜质导线 | 2 | 128 EU/t | 0.2 EU/格 |
| 金质导线 / 绝缘金质导线 / 2x 绝缘金质导线 | 3 | 512 EU/t | 0.4 EU/格 |
| 高压导线 / 绝缘高压导线 / 2x/3x 绝缘高压导线 | 4 | 2048 EU/t | 0.8 EU/格 |
| 玻璃纤维导线 | 5 | 8192 EU/t | 0.025 EU/格 |
| EU 分流导线 | 5 | 8192 EU/t | 0.5 EU/格 |
| EU 限流导线 | 5 | 8192 EU/t | 0.5 EU/格 |

## 绝缘

- 裸线可能电击玩家，防化服套装和橡胶靴可用于防护。
- 橡胶可给锡、铜、金、高压导线添加绝缘层。
- 板材切割剪刀可剥离绝缘层，也用于把金属板切成导线。
- 玻璃纤维导线自带高绝缘，适合长距离和高压主干。

## 分流与限流

- **EU 分流导线**：右键可设置 1–15 的红石触发阈值，并可反相条件。默认在信号强度达到 1 时断开连接。
- **EU 限流导线**：右键打开界面设置最大通过速率，适合保护低等级支路。

## 变压器

变压器用于在不同电压等级之间转换网络。低压、中压、高压、超高压变压器分别对应 LV、MV、HV、EV 网络。

- 面向低等级机器的一侧应连接低压输出。
- 面向高等级主干的一侧应连接更高等级电缆。
- 机器频繁爆线或断供时，先检查机器输入等级、电缆等级和限流设置。

相关系统：[EU 能量系统](../systems/eu_energy.md)
