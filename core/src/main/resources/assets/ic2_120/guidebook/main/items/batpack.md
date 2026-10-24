---
navigation:
  title: BatPack Family
  parent: index.md
  position: 310
  icon: ic2_120:batpack
item_ids:
  - ic2_120:batpack
  - ic2_120:advanced_batpack
  - ic2_120:energy_pack
  - ic2_120:lappack
---

# BatPack Family

<ItemImage id="ic2_120:batpack" scale="4" />

The BatPack Family is a four-tier line of battery backpacks worn in the chestplate slot. Each tier is a mobile EU buffer that automatically tops up the electric tools in your inventory, so you can swap a discharged drill for a fresh one without leaving the field. The capacity, voltage tier, and auto-charge ceiling scale together.

## Item View

| BatPack | Advanced BatPack | Energy Pack | LapPack |
|:------:|:----------------:|:-----------:|:-------:|
| <ItemImage id="ic2_120:batpack" scale="2" /> | <ItemImage id="ic2_120:advanced_batpack" scale="2" /> | <ItemImage id="ic2_120:energy_pack" scale="2" /> | <ItemImage id="ic2_120:lappack" scale="2" /> |

## Stats

| Backpack | Capacity | Tier | Auto-Charge Behavior |
|----------|----------|:----:|----------------------|
| BatPack | 60,000 EU | 1 | Charges inventory tools of tier ≤ 1 |
| Advanced BatPack | 600,000 EU | 2 | Charges inventory tools of tier ≤ 2 |
| Energy Pack | 2,000,000 EU | 3 | Charges inventory tools of tier ≤ 3 |
| LapPack | 60,000,000 EU | 4 | Charges inventory tools of tier ≤ 4 |

The tier of a backpack sets the highest tool tier it will charge. A tier-1 BatPack will not top up a tier-2 Diamond Drill, but a tier-3 Energy Pack can service tier-1, 2, and 3 tools at the same time.

Battery backpacks are **not damageable** — they have no vanilla durability and are fully powered by their internal EU buffer. There is no repair material and no anvil recipe. When the buffer is empty, simply recharge the backpack with any tier-appropriate EU source.

## How to Use

### Equipping

- Place the BatPack in the chestplate slot.
- The backpack exposes a `FluidStorage`-like energy port: any EU source capable of filling a tier-appropriate battery can fill the backpack.
- Use a charger, a charging pad, or a BatBox/CESU/MFE/MFSU to charge it in your inventory.

### Auto-Charge Loop

While the backpack is equipped, every tick it pumps EU into every `IElectricTool` in the player's inventory whose tier is at or below the backpack's tier. The loop continues until the backpack is empty or all eligible tools are full.

Key points of the behavior:

- Only `IElectricTool` items are topped up.
- The backpack's tier is a hard ceiling: a tier-1 BatPack never charges a tier-2 tool even if the tier-2 tool is empty.
- **Other battery backpacks and standalone battery items are not charged.** A BatPack will not refill an Energy Crystal sitting in your inventory, and a LapPack will not refill a BatPack.
- The auto-charge runs from the backpack's own EU buffer, not from the player's hotbar or a connected storage block.

## Crafting

Each backpack is its own recipe, with the cost and complexity scaling with tier.

| BatPack | Advanced BatPack |
|:------:|:----------------:|
| <Recipe id="ic2_120:batpack" /> | <Recipe id="ic2_120:advanced_batpack" /> |
| Energy Pack | LapPack |
| <Recipe id="ic2_120:energy_pack" /> | <Recipe id="ic2_120:lappack" /> |

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Energy Storage](../machines/energy_storage.md)
