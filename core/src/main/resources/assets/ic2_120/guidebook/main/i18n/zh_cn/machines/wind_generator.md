---
navigation:
  title: 风力发电机
  parent: index.md
  position: 15
  icon: ic2_120:wind_generator
item_ids:
  - ic2_120:wind_generator
---

# 风力发电机

<BlockImage id="ic2_120:wind_generator" p:facing="north" p:active="true" scale="4" />

风力发电机利用风能产生 EU，输出功率取决于高度和天气。功率计算公式为 `p = w * s * (h - 64) / 750`，其中 `w` 为天气系数，`s` 为风力强度（0-30），`h` 为有效高度（Y 坐标减去障碍物数）。

为获得最佳效果，请将风力发电机放置在高处并确保周围空旷。机器周围 9x9x7 范围内的障碍物会降低有效高度和发电功率。

## 输出

- **EU 输出**：0-10+ EU/t（可变，理论极限约 11.46 EU/t）
- **能量缓存**：400 EU
- **等级**：1

### 天气系数

| 天气 | 系数 |
|------|------|
| 晴天 | 1.0x |
| 雨天 | 1.2x |
| 雷雨 | 1.5x |

## 槽位

- 电池槽：可充电电池或电动工具

风力发电机不接受 EU 输入。它会从除正面以外的所有方向输出 EU。

## 配方

<Recipe id="ic2_120:wind_generator" />
