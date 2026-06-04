---
navigation:
  title: Crop System Guide
  parent: index.md
  position: 200
  icon: minecraft:book
item_ids:
  - ic2_120:crop_stick
  - ic2_120:crop_seed_bag
  - ic2_120:cropnalyzer
  - ic2_120:fertilizer
  - ic2_120:weed_ex
  - ic2_120:weeding_spade
  - minecraft:wheat_seeds
  - minecraft:beetroot_seeds
  - minecraft:melon_seeds
  - minecraft:pumpkin_seeds
  - minecraft:carrot
  - minecraft:potato
  - minecraft:cocoa_beans
  - minecraft:sugar_cane
  - minecraft:cactus
  - minecraft:nether_wart
---

# Crop System Guide

This document covers gameplay, not code. IC2 crops support two play styles:

- **Regular farming**: plant existing seeds on Crop Sticks, maintain water, fertilizer, and Weed-EX, then harvest when mature.
- **Breeding and crossing**: use the double Crop Stick and parent crops to develop new crop types or seeds with higher stats, then move them into a production field.

---

## Meet the 4 Core Items

- `Crop Stick (crop_stick)`: can only be placed on tilled farmland.
- `Crop (planted state)`: a Crop Stick becomes a crop block after you plant a seed on it.
- `Seed Bag (crop_seed_bag)`: stores a crop's type and its `G/Ga/R` stats.
- `Cropnalyzer (cropnalyzer)`: right-click to open a GUI; insert a Seed Bag and click Scan to unlock stats level by level.

Additional automation:

- `Cropmatron`: automatically applies water, fertilizer, and Weed-EX to crops in range.

---

## Regular Farming and Mass Production

If you don't want to breed, simply plant an existing Seed Bag on a regular Crop Stick. The core loop is:

1. Place a Crop Stick on tilled farmland.
2. Plant the crop with a Seed Bag or any plantable item.
3. Keep water, nutrients, and Weed-EX protection applied.
4. Harvest by hand when mature, or use a Crop Harvester for automation.

Keep your production field separate from your breeding field. The production field only grows the seed you have already chosen to mass-produce, which makes the layout cleaner and easier to maintain with a Cropmatron.

## Crossing and Breeding

Crossing is how you obtain new crop types or push the `G/Ga/R` of existing crops higher.

## 1. Build a Standard Crossing Field

A `5×5` field is a good starting point; scale up to `9×9` later.

Key rules:

- A single Crop Stick placed on farmland is a regular stick.
- Placing a Crop Stick **on top of** an existing Crop Stick turns it into a Crossing Base (a taller cross-shaped rod).
- Only the Crossing Base performs crossing and spread checks.

Recommended layout (top-down):

- Parent slot: `P`
- Crossing slot (double Crop Stick): `X`
- Empty tilled farmland: `.`

```text
. P .
P X P
. P .
```

Notes:
- The current implementation only considers the four orthogonal neighbors of `X` (up/down/left/right) as parents; the four diagonals are ignored.
- You can start with 2 parents flanking 1 `X`, then expand to 4 parents around 1 crossing slot once it is stable.

## 2. Get the Parents into a Crossable State

Under the current rules, a parent must:

- not be `weed` or `eating_plant`
- have `age >= 2`

If the parents don't meet this, the central `X` will not produce offspring.

## 3. After Getting the Target Crop, Lock the Type First, Then Push the Stats

Suggested flow:

1. Aim for the target crop type first (don't worry about stats yet).
2. Once the target crop appears, secure it and scan it first.
3. Keep crossing high-stat individuals of the same type to gradually raise `G/Ga/R`.

Why:
- Offspring stats are the parents' average plus random noise.
- Crossing parents of the same type / attributes is much more likely to keep producing the same type, so iteration is more efficient.

## 4. Scanning and Selection (Required)

Right-click the Cropnalyzer in your hand to open the GUI:

1. Place a `Seed Bag` in the scan slot.
2. Click the Scan button (consumes power each time).
3. The scan level goes from `0 -> 4`.

At `Lv4` you can see the stats directly: `G / Ga / R`.

Selection tips:

- **Crossing phase**: prioritize `G` and `Ga`; don't push `R` to extremes yet.
- **Production phase**: pick the highest overall stats to bring into the production field.

## 5. Weeds and the Correct Mental Model for Weed-EX

- Weeds spread and infect neighboring crops.
- Weed-EX's main role is **prevention and suppression** of infection.
- Weed-EX is **not** an instant killer of existing weeds.

So the correct workflow is:

- Apply Weed-EX to key parents and the crossing area in advance.
- For tiles that have already become weeds, remove them by hand and replant.

---

## Phase 2: Mass Production with Your Ideal Seeds

## 1. Keep the Production Field and Breeding Field Separate

- **Breeding field**: dedicated to crossing and selecting seeds.
- **Production field**: grows only the Seed Bag you have finalized.

This way the randomness of crossing does not affect the stability of your output.

## 2. Recommended Production Field Practices

- Till all the farmland so every Crop Stick is legally placed.
- Plant the same high-stat seed across the whole field.
- Harvest in a loop once mature, watching drop rates.
- Replant periodically (using the best Seed Bags you have selected).

## 3. Use a Cropmatron to Keep Things Stable

Current Cropmatron behavior:

- Affects a `9×9` area on the layer below the machine.
- Automatically spends power to scan each tile.
- Can automatically dispense: fertilizer, water, Weed-EX.

You will need to provide:

- **Water slots**: top slot filled with water containers, bottom slot collects empties.
- **Weed-EX slots**: top slot filled with Weed-EX containers, bottom slot collects empties.
- **Fertilizer slot**: holds fertilizer.
- **Battery slot**: can hold a battery to power the machine (or you can connect it to the grid).

## 4. Fertilizer Buffs (Numbers)

In the current implementation, fertilizer doesn't instantly grow the crop a stage. It raises the crop's internal `nutrients` value, which feeds into the growth formula.

- **Manual fertilizing (right-click with fertilizer)**:
  - Can be applied when `nutrients < 100`.
  - Each application adds `+100 nutrients` (can exceed 100).
- **Cropmatron fertilizing**:
  - Each fertilizer consumed triggers one care pass on the target crop.
  - Each pass adds `+90 nutrients` (also can exceed 100).
- **Decay**:
  - Every main crop tick (about every 256 ticks), `nutrients` drops by `1`.

How fertilizer translates into growth:

- The terrain nutrient value adds `(nutrients + 19) / 20` (integer).
- The environment bonus additionally adds `nutrients / 40`.
- Metal crops have an extra check:
  - `nutrients < 50` applies a negative modifier.
  - `nutrients >= 50` applies a positive modifier.

Practical conclusions:

- In the breeding field, just keep nutrients topped up; no need to spam the limit.
- For metal crops, keep `nutrients >= 50` to avoid obvious growth slowdown.

---

## What "Best Seed" Actually Means

In practice, the definition depends on the use case:

- **Speed-first**: look at `G` (faster growth).
- **Yield-first**: look at `Ga` (better drops).
- **Stability-first**: look at `R` (more resistant to weeds and oddities).

General recommendations:

- During breeding, prioritize `G + Ga`.
- Lock the seed in for mass production, then push `R` to balance the build.

## Full Impact of the 3 Stats (G/Ga/Re) (Current Implementation)

Terminology:

- `G` = Growth
- `Ga` = Gain
- `Re` = Resistance

### 1) What `G` (Growth) Affects

- Directly raises growth points per growth cycle: `baseGrowth = 3 + rand(0..6) + G`.
- Affects parent selection chance in crossing (positive):
  - `G >= 16`: +1 selection tier
  - `G >= 30`: +1 more selection tier
- Indirectly boosts breeding efficiency: reaches crossable age (`age >= 2`) faster.

Bottom line: `G` is the **speed** stat and also raises crossing trigger frequency.

### 2) What `Ga` (Gain) Affects

- Raises the base drop-chance multiplier per harvest: `dropChance * 1.03^Ga`.
- Each harvest roll has a chance to produce `+1` extra drop, scaling with `Ga`.
- Does not directly affect crossing triggers or growth speed.

Bottom line: `Ga` is the **yield** stat.

### 3) What `Re` (Resistance) Affects

- Raises resistance to weed infection:
  - In the weed infection check, higher `Re` makes conversion to weed less likely.
- Raises stability in harsh environments:
  - When growth penalties are high, higher `Re` makes it less likely to regress back into a Crop Stick.
- Negatively affects crossing triggers (high `Re`):
  - Parent selection starts losing significant score past `Re >= 28`, slowing crossing down.

Bottom line: `Re` is the **stability** stat, but pushing it too high slows breeding.

### 4) Recommended Stat Ratios

- **Breeding phase**: prioritize `G + Ga`; don't push `Re` too high.
- **Production phase**: keep `G/Ga` strong, then top up `Re` to resist disruption and stabilize the field.

## The Best Crossing Parent

First, the conclusion:

- The fastest crop to reach crossable state (`age >= 2`) is: **Sticky Resin Reed (sticky_reed)**.
- In practice the "best parent" is not a fixed crop; it is a seed with high `G`, high `Ga`, and moderate `Re`.

### Why Sticky Resin Reed Is Fastest

- Sticky Resin Reed's first two growth stages are very short, so it usually reaches `age >= 2` first and becomes a parent earlier.
- Internal name: sticky_reed.

### Why You Can't Judge Parents by Name Alone

- Crossing efficiency is driven mostly by parent stats, especially `G` (growth) and `Re` (resistance).
- `Re` that's too high (especially 28+) drags parent selection down and slows crossing.
- So "how strong a parent is" depends on that seed's stats, not its species.

### Parent Selection Rules You Can Copy

1. Prefer same-type individuals with high `G` and high `Ga`.
2. Keep `Re` moderate; don't push it blindly.
3. Early on, use easy-to-grow crops to bootstrap:
   - Wheat
   - Carrots
   - Beetroot
   - Sugar Cane
4. Later, switch to high-stat parents and iterate long-term.

## Wheat Growth Time at Different G Values (Example)

For a quick reference, raising `G` significantly shortens full growth time. The numbers below are theoretical averages for wheat under "standard environment" assumptions (`Ga=1, R=1`):

| G | Avg. Time to Mature (seconds) | Avg. Time to Mature (minutes) |
|---:|---:|---:|
| 1 | 1333.3 | 22.22 |
| 5 | 866.5 | 14.44 |
| 10 | 612.0 | 10.20 |
| 15 | 479.4 | 7.99 |
| 20 | 398.4 | 6.64 |
| 25 | 344.1 | 5.73 |
| 31 | 299.0 | 4.98 |

Practical read:

- Going from `G=1` to `G=10` roughly halves the time.
- Continued gains exist, but the marginal improvement shrinks.

---

## Quick FAQ

- Why isn't crossing happening even though the Crop Sticks are placed?
  - The center tile must have a Crop Stick placed **on top of an existing Crop Stick** to become a Crossing Base.
- Why is no offspring appearing?
  - The parents may not have reached `age >= 2`, or there aren't enough valid parents nearby.
- Why do I still see weeds despite using Weed-EX?
  - Weed-EX prevents infection; it doesn't clear existing weeds.
- Why won't the Crop Stick place?
  - There is a hard limit: Crop Sticks can only be placed on tilled farmland.

---

## The Whole Loop in One Sentence

Repeatedly cross, scan, and keep high-stat Seed Bags in your breeding field. Once you have a top seed, transplant it to a separate production field and use a Cropmatron to maintain water, fertilizer, and Weed-EX for stable mass production.
