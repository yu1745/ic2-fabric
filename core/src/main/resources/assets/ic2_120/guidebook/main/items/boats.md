---
navigation:
  title: Boats
  parent: index.md
  position: 340
  icon: ic2_120:rubber_boat
item_ids:
  - ic2_120:rubber_boat
  - ic2_120:broken_rubber_boat
  - ic2_120:carbon_boat
  - ic2_120:electric_boat
---

# Boats

<ItemImage id="ic2_120:rubber_boat" scale="4" />

The Boats family is a four-vessel set of watercraft built from IC2 materials. Three of them — Rubber, Broken Rubber, and Carbon — are vanilla-style rowboats with different hulls and handling, while the Electric Boat is an mEU-powered craft for fast, motor-assisted travel. All four are placeable on water and ridden the same way as a vanilla boat.

## Item View

| Rubber Boat | Broken Rubber Boat | Carbon Boat | Electric Boat |
|:-----------:|:------------------:|:-----------:|:-------------:|
| <ItemImage id="ic2_120:rubber_boat" scale="2" /> | <ItemImage id="ic2_120:broken_rubber_boat" scale="2" /> | <ItemImage id="ic2_120:carbon_boat" scale="2" /> | <ItemImage id="ic2_120:electric_boat" scale="2" /> |

## Stats

| Type | Material | Speed Tier | Notes |
|------|----------|:----------:|-------|
| Rubber Boat | Rubber | Vanilla-style | Basic watercraft, the baseline rowboat |
| Broken Rubber Boat | Rubber (damaged) | Vanilla-style | Repair material for a damaged Rubber Boat |
| Carbon Boat | Carbon Plate | Vanilla-style | Carbon-fiber lightweight hull |
| Electric Boat | Iron Plate + Insulated Copper Cable + Electric Motor + Iron Rotor | Electric (mEU-driven) | Faster than the other three, but more expensive to build |

The first three are pure hulls with no power system on board. The Electric Boat carries an actual electric motor and rotor, and is described as motor-driven and faster than the other three. Specific speed/handling values are defined on the spawned entity (see Related section); if you need exact numbers, refer to the entity behavior in `ModEntities` rather than the item itself.

## How to Use

### Placing

- Hold a boat item in your hand and **right-click on the surface of water** to spawn a rideable boat entity.
- The placement uses the standard raycast used by vanilla boats: it will only commit if the hit is a block, the player has clear line of sight, and there is room for the entity's collision box.
- The entity type is fixed per item: Rubber → `ModEntities.RUBBER_BOAT`, Broken Rubber → `ModEntities.BROKEN_RUBBER_BOAT`, Carbon → `ModEntities.CARBON_BOAT`, Electric → `ModEntities.ELECTRIC_BOAT`.
- In **Survival**, the item is **consumed** on successful placement (1 per boat). In **Creative**, the item is not consumed.
- The item acts only as a spawn medium — once the entity is placed, the item is gone and the boat's behavior is defined entirely by the entity.

### Boarding and Riding

- Right-click the placed boat to enter it, exactly like a vanilla boat.
- Each boat entity has its own speed and handling characteristics defined on the entity side; consult the entity definition if you need the exact numbers.
- Exit the boat with the standard dismount key.

### Repairing a Broken Rubber Boat

A Rubber Boat that has been damaged becomes a **Broken Rubber Boat**. To repair it back to a working Rubber Boat:

- Combine the Broken Rubber Boat with **1 Rubber** in a crafting grid (shapeless).
- This outputs a fresh Rubber Boat and consumes the broken one.

This is the only recipe that uses the Broken Rubber Boat item.

### Charging the Electric Boat

The Electric Boat is an electric device — it contains a real motor and rotor and is designed to be powered by mEU. It is described as motor-driven and faster than the three non-electric hulls, but exact range/speed/drain values are defined on the entity side (see Related). Treat it as a powered vehicle: build a charging setup (charger, charging pad, or any tier-appropriate EU storage) to keep it running.

## Crafting

Three of the boats have a shaped crafting recipe. The Broken Rubber Boat is **not** crafted — it appears as the damaged state of a Rubber Boat and is restored via a shapeless repair recipe.

| Rubber Boat | Carbon Boat |
|:-----------:|:-----------:|
| <Recipe id="ic2_120:rubber_boat" /> | <Recipe id="ic2_120:carbon_boat" /> |
| Electric Boat | Broken Rubber Boat (repair) |
| <Recipe id="ic2_120:electric_boat" /> | <Recipe id="ic2_120:repair_to_rubber_boat" /> |

Recipe notes:

- **Rubber Boat** uses the `Compat.Items.RUBBER` tag, so any mod's rubber item is accepted.
- **Carbon Boat** uses the Carbon Plate item directly.
- **Electric Boat** uses the Insulated Copper Cable item, the `Compat.Items.PLATES_IRON` tag for iron plates, the Electric Motor item, and the Iron Rotor item.
- **Broken Rubber Boat → Rubber Boat** is a shapeless recipe: 1 Broken Rubber Boat + 1 Rubber → 1 Rubber Boat.

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
