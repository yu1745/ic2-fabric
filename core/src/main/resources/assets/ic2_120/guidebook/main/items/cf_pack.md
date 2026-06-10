---
navigation:
  title: CF Pack
  parent: index.md
  position: 312
  icon: ic2_120:cf_pack
item_ids:
  - ic2_120:cf_pack
---

# CF Pack

<ItemImage id="ic2_120:cf_pack" scale="4" />

The CF Pack (Construction Foam Backpack) is the chestplate-slot fluid reservoir for the <ItemLink id="ic2_120:foam_sprayer" />. Wear it and the sprayer in your main hand stays topped up automatically, so you can keep building long foam walls without going back to a tank or a fluid pipe.

## Item View

| CF Pack |
|:------:|
| <ItemImage id="ic2_120:cf_pack" scale="2" /> |

## Stats

| Property | Value |
|----------|-------|
| Slot | Chestplate |
| Armor | 2 |
| Vanilla durability | None (`isDamageable = false`) |
| Internal capacity | 80 buckets of Construction Foam (`CAPACITY_DROPLETS = 80 * BUCKET`) |
| Accepted fluid | Construction Foam only |

The CF Pack is a chestplate, not a tool, so it sits in the armor slot. It does not take vanilla damage and has no anvil repair recipe — the only "wear" is foam being consumed. It does not provide any electric power or other utility beyond its fluid storage.

## How to Use

### Equipping

- Place the CF Pack in the chestplate slot.
- Fill it from any Construction Foam source: a tank, a fluid pipe, a machine that outputs foam, or a Construction Foam bucket.

### Auto-Refilling the Sprayer

While the CF Pack is equipped, every tick it tries to fill any <ItemLink id="ic2_120:foam_sprayer" /> sitting in the player's inventory. The sprayer has a small internal capacity; the CF Pack keeps it topped up to the sprayer's own maximum, drawing foam from its own internal reservoir.

- The auto-fill only targets the foam sprayer. It does not refill other fluid containers in the inventory.
- The CF Pack itself stays at whatever level you have filled it to. It only loses foam as it tops up the sprayer.
- When the CF Pack is empty, the sprayer will continue to deplete on its own; you will need to refill the CF Pack from a foam source again.

### Fluid Port

The CF Pack exposes a `FluidStorage` interface so external automation can fill or drain it. The interface is filtered to **Construction Foam only** — any other fluid offered is rejected. This makes the CF Pack safe to attach to a generic fluid pipe without worrying about cross-contamination.

## Crafting

<Recipe id="ic2_120:cf_pack" />

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Building and Decoration](../reference/building_decoration.md)
