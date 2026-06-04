---
navigation:
  title: 储电设备
  parent: index.md
  position: 46
  icon: ic2_120:batbox
item_ids:
  - ic2_120:batbox
  - ic2_120:cesu
  - ic2_120:mfe
  - ic2_120:mfsu
---

# 储电设备

<BlockImage id="ic2_120:batbox" scale="4" />

储电设备用于缓冲 EU、给电动物品充电，并从正面输出电力。它们也是把发电端和用电端隔开的可靠缓冲，适合处理机器耗电波动。

## 方块视图

| 储电箱 | CESU | MFE | MFSU |
|:----:|:----:|:---:|:----:|
| <BlockImage id="ic2_120:batbox" scale="2" /> | <BlockImage id="ic2_120:cesu" scale="2" /> | <BlockImage id="ic2_120:mfe" scale="2" /> | <BlockImage id="ic2_120:mfsu" scale="2" /> |
 
## 等级对比

| 等级 | 名称 | 容量 | 输出 | 电压 |
|------|------|------|------|------|
| 1 | 储电箱 (BatBox) | 40,000 EU | 32 EU/t | 低压 |
| 2 | CESU 储电箱 | 300,000 EU | 128 EU/t | 中压 |
| 3 | MFE 储电箱 | 4,000,000 EU | 512 EU/t | 高压 |
| 4 | MFSU 储电箱 | 40,000,000 EU | 2,048 EU/t | 超高压 |

## 使用方式

所有储电设备只从五个非正面接收 EU，只从正面输出 EU。放置时要旋转方块，让输出面朝向要供电的电缆或机器。

## 槽位

- **充电槽：** 可放入等级不高于储电设备等级的电池和电动工具。
- **燃料槽：** 红石提供 800 EU，能量粉提供 16,000 EU；只有剩余容量足够完整接收该数值时才会消耗。
- **MFE/MFSU 装备视图：** 高等级界面还会显示玩家装备充电相关控制。

储电设备本身没有升级槽。需要跨电压等级供电时，请使用变压器。

## 电网提示

储电箱、CESU、MFE、MFSU 分别按自身等级输出 32、128、512、2,048 EU/t。低于该等级的机器或电缆需要在输出侧接变压器或低等级缓冲。MFE 和 MFSU 也可作为传送机的相邻供能方块，传送费用会直接从附近储能中扣除。

## 配方

| BatBox | CESU |
|:------:|:----:|
| <Recipe id="ic2_120:batbox" /> | <Recipe id="ic2_120:cesu" /> |
| MFE | MFSU |
| <Recipe id="ic2_120:mfe" /> | <Recipe id="ic2_120:mfsu" /> |
