---
navigation:
  title: Biofuel Production Chain
  parent: index.md
  position: 55
---

# Biofuel Production Chain

This page derives the machine ratio from **one Fluid/Solid Canning Machine**. The calculation uses basic machines without overclocker upgrades and uses long-term average rates.

## One Fluid/Solid Canning Machine

The fluid-mixing recipe is:

```text
1 Bio Chaff + 1 bucket of Water → 1 bucket of Biomass
```

The base operation takes 200 ticks, or 10 seconds, so one machine produces:

```text
6 buckets of Biomass per minute
```

## Fermenter Ratio

With heat supplied by a Fluid Heat Generator, one Fermenter completes a production cycle every 125 ticks. Each cycle consumes 20 mB of Biomass and produces 400 mB of Biofuel.

```text
1200 ÷ 125 × 20 = 192 mB Biomass per minute per Fermenter
6000 ÷ 192 = 31.25 Fermenters
```

The theoretical ratio is therefore:

```text
1 Fluid/Solid Canning Machine : 31.25 Fermenters
```

## Fluid Heat Generator Ratio

Each Fluid Heat Generator outputs 32 HU/t. A Fermenter needs 4000 HU per cycle:

```text
4000 ÷ 32 = 125 ticks per Fermenter cycle
```

Each Fermenter therefore needs one Fluid Heat Generator:

```text
1 Fluid/Solid Canning Machine
: 31.25 Fermenters
: 31.25 Fluid Heat Generators
```

Biofuel production and fuel consumed by the heat generators are:

```text
31.25 × (1200 ÷ 125) × 400 = 120000 mB Biofuel/minute
31.25 × 600 = 18750 mB/minute consumed by the Fluid Heat Generators
120000 - 18750 = 101250 mB/minute available for power generation
```

## Semifluid Generator Ratio

Biofuel is consumed by a Semifluid Generator at about 592.6 mB/minute, while producing 16 EU/t on average. The theoretical number of generators is:

```text
101250 ÷ 592.5926 = 170.859375 Semifluid Generators
```

The complete theoretical ratio is:

```text
1 Fluid/Solid Canning Machine
: 31.25 Fermenters
: 31.25 Fluid Heat Generators
: 170.859375 Semifluid Generators
```

The corresponding average generation is:

```text
170.859375 × 16 = 2733.75 EU/t
```

The fractional generator count is a long-term average; machines themselves must be whole blocks. This corresponds to 169 generators at full load plus one generator at about 85.94% load.

## Integer Machine Configuration

If all machine counts must be integers, use:

```text
1 Fluid/Solid Canning Machine
31 Fermenters
31 Fluid Heat Generators
169 full-load Semifluid Generators
+ 1 Semifluid Generator at about 49.25% load
```

The 31 Fermenters consume 5952 mB Biomass per minute, just below the Canning Machine's 6000 mB/minute output. The Canning Machine can therefore run at about 99.2% duty cycle while consuming all Bio Chaff and Water. This configuration produces about 2711.88 EU/t on average.

## Crop Sources for Bio Chaff

The following comparison uses directly plantable vanilla crops, without hybridization, with seed stats `G=1 / Ga=1 / R=1`. It assumes standard light, water, nutrition, and air-quality conditions, and that a Macerator processes the harvested crops into Bio Chaff.

A full-speed Fluid/Solid Canning Machine consumes:

```text
1 Bio Chaff every 10 seconds = 6 Bio Chaff per minute
```

| Vanilla crop | Macerator recipe | Bio Chaff per crop stick/minute | Theoretical crop sticks | Recommended crop sticks |
|---|---|---:|---:|---:|
| Sugar Cane | 8 Sugar Cane → 1 Bio Chaff | **0.0384** | **156.25** | **160** |
| Melon | 8 Melon Slices or Melons → 1 Bio Chaff | 0.0294 | 204.1 | 210 |
| Carrot | 8 Carrots → 1 Bio Chaff | 0.0131 | 457.1 | 470 |
| Potato | 8 Potatoes → 1 Bio Chaff | 0.0131 | 457.1 | 470 |
| Pumpkin | 8 Pumpkins → 1 Bio Chaff | 0.0128 | 467.3 | 480 |
| Wheat | 8 Wheat → 1 Bio Chaff | 0.0079 | 759.5 | 770 |

The theoretical count is calculated as `6 ÷ chaff per crop stick`. The practical count rounds upward to account for random growth, random drops, and intermittent item transfer.

Sugar Cane is the best directly planted vanilla crop for non-hybrid Bio Chaff production:

```text
About 157 Sugar Cane crop sticks = theoretical supply for one full-speed Canning Machine
About 160 Sugar Cane crop sticks = two full single-layer farm units
```

## Single-Layer Farm and Crop Harvester

The Crop Harvester scans a `9×3×9` volume. If the farm uses only the layer at the same height as the harvester, the plane is `9×9`. The center position is occupied by the harvester itself:

```text
9 × 9 - 1 = 80 crop sticks per full single-layer farm unit
```

Therefore:

```text
1 full single-layer farm unit = 80 Sugar Cane crop sticks
2 full single-layer farm units = 160 Sugar Cane crop sticks
```

Two full single-layer farm units, each handled by one Crop Harvester, provide the practical 160-crop-stick configuration for one full-speed Fluid/Solid Canning Machine. The harvester can scan one layer above and below as well, but those layers share the same scan cycle; it checks one coordinate every 10 ticks.
