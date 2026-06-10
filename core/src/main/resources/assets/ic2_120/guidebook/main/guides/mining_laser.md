---
navigation:
  title: Mining Laser
  parent: index.md
  position: 201
  icon: minecraft:book
---

# Mining Laser

<ItemImage id="ic2_120:mining_laser" scale="4" />

A long-range mining weapon that fires a visible projectile to break blocks or attack entities. Runs on EU, with a full charge of 200,000 EU.

## Basic Controls

| Action | Effect |
|------|------|
| Right-click | Fire a laser projectile |
| Hold the **Mode Switch** key + right-click | Cycle through modes (find the Mod's "Mode Switch" binding under **Options → Controls**; the default key varies by keybinding pack) |

When you cycle modes, the same action won't accidentally fire. When the laser first moves into your main hand, the action bar briefly shows the current mode.

## Mode Overview

**Scatter** spends EU once for 25 shots, **3×3** spends EU once for 9 shots; every other mode counts one right-click as one shot (or one explosion).

| Mode | EU/Use | Approx. Uses per Full Charge¹ | Range | Damage to Entities |
|------|---:|---:|---:|---|
| Mining | 2,000 | 100 | 64 blocks | 2 hearts |
| Low Focus | 500 | 400 | 4 blocks | 1 heart |
| Long Range | 5,000 | 40 | 64 blocks | 3 hearts |
| Super Heat | 5,000 | 40 | 8 blocks | 4 hearts |
| Scatter | 12,500 | 16 | 10 blocks | 1 heart per shot |
| Explosive | 10,000 | 20 | 10 blocks | 100 true-damage hit, then a TNT-like explosion at the same point |
| 3×3 | 7,200 | 27 | 20 blocks | 1 heart per shot |

¹ Calculated from a full charge of **200,000 EU** and the EU/use in the table. The count is the number of times you can **fully** fire; any leftover EU that doesn't cover a full shot is not counted. For Scatter and 3×3, "one use" still means one right-click that triggers many laser beams.

**Note:** In-game, **1 heart = 2 HP**. Aside from Explosive, damage to players is reduced by armor, Protection enchantments, and Quantum/Nano Armor energy absorption. The 100 damage from Explosive **bypasses armor**; the explosion portion still follows vanilla explosion rules.

## Projectiles and Entities

- If a laser hits an **entity** first, the entity takes damage, the projectile disappears, and the laser **does not** punch through to hit blocks behind it.
- **Mining** mode can chew through multiple blocks in a single shot until the shot's "remaining reach" is used up. Each block destroyed can independently trigger the random ignite chance below.

## Random Ignite After Destruction (All Modes)

In every mode, after a target block is **successfully destroyed** (or after an Explosive shot detonates), the original position has a **30%** chance of spawning fire. The fire still has to obey the game's "can fire be placed here" rules (it needs a flammable support block below, etc.). This way the block is cleared first and the fire isn't immediately broken.

- **Low Focus** on leaves, planks, wool: destroys the block first, then rolls the 30% ignite above.
- **Explosive**: rolls ignite at the impact point after the explosion finishes. If the explosion hits an entity, the ignite is rolled at the block under the explosion center.

If the shot fails to destroy a block (e.g. it hits bedrock), the random ignite **does not** trigger.

## General Limits

- Extra-hard blocks like Obsidian, Reinforced Stone, and Reactor Pressure Vessels cannot be broken by the laser.
- Many flammable blocks are broken normally; lit TNT explodes quickly.
- **Super Heat** does not smelt **logs**; smeltable ores drop their **smelted** result (as if passed through a furnace). Blocks that cannot be smelted are simply broken like normal mining.

## Mode Details

### Mining Mode

The standard mining mode. The laser fires toward the crosshair. Each block it breaks reduces the shot's remaining reach by an amount based on the block's **hardness**; harder blocks mean the same shot covers fewer blocks. Each shot costs 2,000 EU.

### Low Focus Mode

Best at very close range (about 4 blocks), only 500 EU per shot. On leaves, planks, and wool it **destroys the block first**, then rolls the 30% ignite from the global rules; other blocks are broken as a normal single-block hit.

### Long Range Mode

Long range, fast projectile, 3 hearts of damage to entities. Great for sniping or hit-and-run.

### Super Heat Mode

When the laser hits a block that can be smelted in a furnace, it drops the **finished product** directly (e.g. iron ore → iron ingot). It does not work on logs; blocks that cannot be smelted are simply broken. 5,000 EU per shot.

### Scatter Mode

12,500 EU per use. Fires **25** laser beams in a fan, covering a wide area in front of you. Each beam independently rolls damage and destruction.

### Explosive Mode

Whether you hit a block or an entity, the impact point creates a **TNT-class** explosion. On entities: 100 true-damage hit, then the explosion. The blast can also hurt you if you're too close, so mind the distance.

### 3×3 Mode

7,200 EU per use. Fires **9** beams arranged in a forward-facing 3×3 wall, perfect for rapidly carving out a tunnel.
