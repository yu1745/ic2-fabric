---
navigation:
  title: 放射性同位素温差发电机
  parent: index.md
  position: 16
  icon: ic2_120:rt_generator
item_ids:
  - ic2_120:rt_generator
---

# 放射性同位素温差发电机

<BlockImage id="ic2_120:rt_generator" p:facing="north" p:active="true" scale="4" />

放射性同位素温差发电机（RTG）利用放射性同位素燃料靶丸产生 EU。它有 6 个燃料槽，每个槽可放入一枚 RTG 靶丸，靶丸无限耐久永不消耗。增加靶丸数量可指数级提升发电功率。

该发电机一旦装入靶丸便无需再添加燃料，非常适合远程或长期无人值守的设施。输出随靶丸数量递增：1 枚靶丸产生 1 EU/t，6 枚靶丸产生 32 EU/t。

## 输出

- **EU 输出**：1-32 EU/t（取决于靶丸数量）
- **能量缓存**：20,000 EU
- **等级**：1

### 靶丸数量与功率

| 靶丸数 | EU/t |
|--------|------|
| 1 | 1 |
| 2 | 2 |
| 3 | 4 |
| 4 | 8 |
| 5 | 16 |
| 6 | 32 |

## 槽位

- 6 个燃料槽：RTG 燃料靶丸（无限耐久）

RTG 不接受 EU 输入。它会从除正面以外的所有方向输出 EU。

## 配方

<Recipe id="ic2_120:rt_generator" />
