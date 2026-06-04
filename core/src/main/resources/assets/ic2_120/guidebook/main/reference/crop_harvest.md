---
navigation:
  title: Crop Harvest Output Reference
  parent: index.md
  position: 209
  icon: minecraft:book
---

# Crop Harvest Output Reference

## Harvest Mechanics

On each harvest, the number of drop rolls follows a Gaussian distribution with `dropChance` as the mean:

```
dropChance = dropGainChance × 1.03^Ga
drop count = round( Normal(mean = dropChance, std = dropChance) )
```

Each roll calls `createGainStacks()` to produce a base output stack, then has a `Ga%` chance to grant that stack `+1` extra item.

---

## 1. Food Crops

| Crop | Name | maxAge | Tier | Harvest Age | Age After Harvest | Per-Roll Output | dropGainChance |
|------|------|---:|---:|---:|------|------|:---:|
| WHEAT | Wheat | 7 | 1 | =7 | → 2 | Wheat ×1 | 0.950 |
| CARROTS | Carrot | 3 | 1 | =3 | reset (empty) | Carrot ×1 | 0.950 |
| POTATO | Potato | 3 | 1 | ≥2 | reset (empty) | Age=3: 5% poisonous potato; else Potato ×1 | 0.950 |
| BEETROOTS | Beetroot | 3 | 1 | =3 | reset (empty) | Beetroot ×1 | 0.950 |
| PUMPKIN | Pumpkin | 3 | 2 | =3 | → 2 | Pumpkin ×1 | 0.903 |
| MELON | Melon | 3 | 2 | =3 | → 2 | 1/3 chance Melon ×1, 2/3 chance Melon Slice ×(2~5) | 0.903 |

## 2. Flower Crops

| Crop | Name | maxAge | Tier | Harvest Age | Age After Harvest | Per-Roll Output | dropGainChance |
|------|------|---:|---:|---:|------|------|:---:|
| DANDELION | Dandelion | 3 | 1 | =3 | → 2 | Yellow Dye ×1 | 0.950 |
| POPPY | Poppy | 3 | 1 | =3 | → 2 | Red Dye ×1 | 0.950 |
| BLACKTHORN | Blackthorn | 3 | 2 | =3 | → 2 | Black Dye ×1 | 0.903 |
| TULIP | Tulip | 3 | 2 | =3 | → 2 | Purple Dye ×1 | 0.903 |
| CYAZINT | Cyazint | 3 | 3 | =3 | → 2 | Blue Dye ×1 | 0.857 |
| VENOMILIA | Venomilia | 5 | 4 | ≥3 | → 2 | Age=4: Toad Powder ×1; Age=3/5: Purple Dye ×1 | 0.815 |

## 3. Reed and Fibre Crops

| Crop | Name | maxAge | Tier | Harvest Age | Age After Harvest | Per-Roll Output | dropGainChance |
|------|------|---:|---:|---:|------|------|:---:|
| REED | Sugar Cane | 2 | 1 | ≥1 | → 0 | Sugar Cane ×**age** (max 2) | 0.950 |
| STICKY_REED | Sticky Reed | 3 | 4 | ≥1 | age=3 → 1~2, else → 0 | Age=3: Sticky Resin ×1; age&lt;3: Sugar Cane ×age | 0.815 |
| FLAX | Flax | 3 | 3 | =3 | → 0 | String ×1 | 0.857 |
| COCOA | Cocoa | 3 | 2 | =3 | → 2 | Cocoa Beans ×1 | 0.903 |

## 4. Mushroom and Nether Crops

| Crop | Name | maxAge | Tier | Harvest Age | Age After Harvest | Per-Roll Output | dropGainChance |
|------|------|---:|---:|---:|------|------|:---:|
| RED_MUSHROOM | Red Mushroom | 2 | 2 | =2 | reset (empty) | Red Mushroom ×1 | 0.903 |
| BROWN_MUSHROOM | Brown Mushroom | 2 | 2 | =2 | reset (empty) | Brown Mushroom ×1 | 0.903 |
| NETHER_WART | Nether Wart | 2 | 2 | =2 | reset (empty) | Nether Wart ×1 | **2.0** |
| TERRA_WART | Terra Wart | 2 | 3 | =2 | reset (empty) | Terra Wart ×1 | 0.800 |

## 5. Sapling Crops

| Crop | Name | maxAge | Tier | Harvest Age | Age After Harvest | Per-Roll Output | dropGainChance |
|------|------|---:|---:|---:|------|------|:---:|
| OAK_SAPLING | Oak Sapling | 4 | 2 | =4 | → 3 | Oak Sapling ×1 + 25% second + 25% Apple ×1 | 0.903 |
| SPRUCE_SAPLING | Spruce Sapling | 4 | 2 | =4 | → 3 | Spruce Sapling ×1 + 25% second | 0.903 |
| BIRCH_SAPLING | Birch Sapling | 4 | 2 | =4 | → 3 | Birch Sapling ×1 + 25% second | 0.903 |
| JUNGLE_SAPLING | Jungle Sapling | 4 | 2 | =4 | → 3 | Jungle Sapling ×1 + 25% second | 0.903 |
| ACACIA_SAPLING | Acacia Sapling | 4 | 2 | =4 | → 3 | Acacia Sapling ×1 + 25% second | 0.903 |
| DARK_OAK_SAPLING | Dark Oak Sapling | 4 | 2 | =4 | → 3 | Dark Oak Sapling ×1 + 25% second | 0.903 |

## 6. Metal Crops

Require a matching ore or metal block within 1-5 blocks below the crop.

| Crop | Name | maxAge | Tier | Harvest Age | Age After Harvest | Per-Roll Output | dropGainChance | Required Root |
|------|------|---:|---:|---:|------|------|:---:|------|
| FERRU | Iron Powder Plant | 3 | 4 | =3 | → 1 | Small Pile of Iron Dust ×1 | 0.407 | Iron Ore / Iron Block |
| CYPRIUM | Copper Powder Plant | 3 | 4 | =3 | → 1 | Small Pile of Copper Dust ×1 | 0.407 | Copper Ore / Copper Block |
| STAGNIUM | Tin Powder Plant | 3 | 4 | =3 | → 1 | Small Pile of Tin Dust ×1 | 0.407 | Tin Ore / Tin Block |
| PLUMBISCUS | Lead Powder Plant | 3 | 4 | =3 | → 1 | Small Pile of Lead Dust ×1 | 0.407 | Lead Ore / Lead Block |
| AURELIA | Gold Powder Plant | 4 | 5 | =4 | → 1 | Small Pile of Gold Dust ×1 | 0.774 | Gold Ore / Gold Block |
| SHINING | Silver Powder Plant | 4 | 5 | =4 | → 1 | Small Pile of Silver Dust ×1 | 0.774 | Silver Ore / Silver Block |

## 7. Special Crops

| Crop | Name | maxAge | Tier | Harvest Age | Age After Harvest | Per-Roll Output | dropGainChance | Notes |
|------|------|---:|---:|---:|------|------|:---:|------|
| RED_WHEAT | Red Wheat | 6 | 4 | =6 | → 1 | Sky ≤0 and 50%: Wheat, else Redstone ×1 | 0.500 | |
| COFFEE | Coffee | 4 | 3 | ≥3 | → 2 | Age=3: nothing; Age=4: Coffee Beans ×1 | 0.857 | Best harvest age = 4 |
| HOPS | Hops | 6 | 3 | =6 | → 2 | Hops ×1 | 0.857 | |
| EATING_PLANT | Eating Plant | 5 | 5 | =3~4 | → 0 | Melon ×1 | 0.774 | Attacks nearby entities |

## Drop Count Reference

Theoretical average number of drop rolls per harvest for different `Ga` values:

| Ga | dropGainChance=0.950 | 0.903 | 0.857 | 0.815 | 0.774 | 0.500 | 0.407 |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 | 0.98 | 0.93 | 0.88 | 0.84 | 0.80 | 0.52 | 0.42 |
| 5 | 1.10 | 1.05 | 0.99 | 0.95 | 0.90 | 0.58 | 0.47 |
| 10 | 1.28 | 1.21 | 1.15 | 1.10 | 1.04 | 0.67 | 0.55 |
| 15 | 1.48 | 1.41 | 1.34 | 1.27 | 1.21 | 0.78 | 0.63 |
| 20 | 1.72 | 1.63 | 1.55 | 1.47 | 1.40 | 0.90 | 0.73 |
| 25 | 1.99 | 1.89 | 1.79 | 1.71 | 1.62 | 1.05 | 0.85 |
| 31 | 2.38 | 2.26 | 2.15 | 2.04 | 1.94 | 1.25 | 1.02 |

Actual drop counts per harvest are normally distributed around the mean, usually within ±1.
