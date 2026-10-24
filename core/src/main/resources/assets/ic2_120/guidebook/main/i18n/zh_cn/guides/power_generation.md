---
navigation:
  title: 发电与储电
  parent: index.md
  position: 202
  icon: minecraft:book
item_ids:
  - minecraft:coal
  - minecraft:charcoal
  - minecraft:lava_bucket
  - ic2_120:coal_fuel_dust
  - ic2_120:plant_ball
  - ic2_120:bio_chaff
  - ic2_120:bio_cell
---

# 发电与储电

<BlockImage id="ic2_120:generator" scale="4" />

## 发电机

### 火力发电机 (Generator)

- **缓冲**：4,000 EU
- **发电速率**：10 EU/t
- **燃料换算**：1 煤 = 4,000 EU
- **接线**：正面不可输出，其余面可接导线

### 斯特林发电机 (Stirling Generator)

- **发电速率**：10 EU/t
- **燃料**：煤、木炭、岩浆桶（1 桶 = 20,000 EU）、等

### 地热发电机 (Geothermal Generator)

- **缓冲**：10,000 EU
- **发电速率**：20 EU/t
- **流体消耗**：岩浆 1 桶（1000 mB）= 24,000 EU
- **输出时间**：60 秒（1200 tick at 20 EU/t）

### 太阳能发电机 (Solar Generator)

- **发电速率**：晴天 1 EU/t（白天）
- **不发电**：夜晚、阴天、雨天
- **需要天空视野**：上方不能有遮挡

### 水力发电机 (Water Kinetic Generator)

- 将水的动能转化为 KU，再由动能发电机转化为 EU
- 需要水流驱动
- 详见 [水力发电机](../machines/water_generator.md)

### 风力发电机 (Wind Kinetic Generator)

详见 [动能系统](../systems/kinetic_transmission.md)

- **动能输出**：根据高度和天气变化
- **碳转子标定输出**：y=150 + 晴天 + gustFactor=1.0 → 512 KU/t = 128 EU/t
- **最大 KU/t**：2048 KU/t = 512 EU/t
- 详见 [风力动能发生机](../machines/wind_kinetic_generator.md)

### 流体燃料发电机 (Semifluid Generator)

- **燃料**：两类，精炼燃油系和原油/杂酚油系
- **每 mB 发电量**：精炼燃油系更高，原油/杂酚油系较低
- 详见 [流体燃料发电机](../machines/semifluid_generator.md)

### 蒸汽动能发电机 (Steam Kinetic Generator)

- 将蒸汽转化为动能（KU），再由动能发电机转化为 EU
- 详见 [蒸汽动能发电机](../machines/steam_kinetic_generator.md)

### RT 发电机 (RT Generator / Radioisotope Thermoelectric Generator)

- **热量驱动**：接收 HU 并转换为 EU
- **1 HU = 1 EU**
- 详见 [RT 发电机](../machines/rt_generator.md)

### 创意发电机 (Creative Generator)

- **无限 EU/t**：仅创意模式使用

---

## 储电设备

| 方块 | 电压等级 | 容量 | 每面最大吞吐 |
|---|---:|---:|---:|
| BatBox | Tier 1 (LV) | 40,000 EU | 32 EU/t |
| CESU | Tier 2 (MV) | 300,000 EU | 128 EU/t |
| MFE | Tier 3 (HV) | 4,000,000 EU | 512 EU/t |
| MFSU | Tier 4 (EV) | 40,000,000 EU | 2048 EU/t |

### 储电箱接线规则

- **BatBox**：仅正面输入，其余面输出
- **CESU**：仅正面输入，其余面输出
- **MFE**：仅正面输入，其余面输出
- **MFSU**：仅正面输入，其余面输出

### 充电台

- **同级容量/耐压**：与储电箱相同
- **特点**：玩家站在台面上可给可充电物品补能

### 变压器 (Transformer)

- **功能**：接收电网能量并提升输出电压等级
- **升级后**：可向上游高压电网输出更多能量

---

## 发电与储电计算

### 燃料能量表

| 燃料 | 产出 EU |
|------|---------|
| 1 煤 | 4,000 EU |
| 1 木炭 | 4,000 EU |
| 1 岩浆桶 | 24,000 EU |
| 1 沼气 mB | 约 60 EU |

### 风力发电机高度模型

- **Y > 74**：固定 3 EU/t
- **Y 为 74 或更低**：不发电
- 天气、风力强度和附近障碍物都不会影响直接输出 EU 的风力发电机。

### 转子寿命（晴天基准）

| 转子材质 | 寿命 |
|---------|------|
| 木 | 3 小时 |
| 铁 | 24 小时 |
| 钢 | 48 小时 |
| 碳 | 168 小时 |
