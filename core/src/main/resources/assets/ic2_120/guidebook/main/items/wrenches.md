---
navigation:
  title: Wrenches
  parent: index.md
  position: 333
  icon: ic2_120:wrench
item_ids:
  - ic2_120:wrench
  - ic2_120:electric_wrench
---

# Wrenches

<ItemImage id="ic2_120:wrench" scale="4" />

The Wrench family is IC2's two-tier line of machine-configuration tools. Every IC2 machine with multiple facings — cable connectors, machine input/output sides, kinetic-shaft directions — uses a wrench to rotate it, and machines that allow wrenching drop their full contents (with the right sneak-click) instead of just the machine block. The hand Wrench does this with bronze durability; the Electric Wrench does it forever, powered by EU.

## Item View

| Wrench | Electric Wrench |
|:------:|:---------------:|
| <ItemImage id="ic2_120:wrench" scale="2" /> | <ItemImage id="ic2_120:electric_wrench" scale="2" /> |

## Stats

| Type | Durability or Capacity | Use | Repair | Crafting |
|------|------------------------|-----|--------|----------|
| <ItemLink id="ic2_120:wrench" /> (hand tool) | 120 durability | Rotate machine facings, strip cable insulation, disassemble machines | None — replace when worn out | Bronze ingots |
| <ItemLink id="ic2_120:electric_wrench" /> (electric tool) | 10,000 EU internal buffer; **no vanilla durability** | Same as hand Wrench; functionally unlimited uses | Recharge with any tier-1 EU source | Wrench + Small Power Unit |

The two wrenches share the same function list. The electric version trades the 120-use wear budget for an EU buffer, so you never have to carry spares — just keep it topped up.

## How to Use

### Rotating Machines

- **Right-click** a machine with a wrench to rotate its facing **clockwise**.
- **Shift + Right-click** to rotate **counter-clockwise**.

Most IC2 machines expose 4 to 6 valid facings, so a full rotation takes 4 to 6 clicks. Some machines also use the wrench to cycle through secondary configuration states (for example, toggling a machine between input-only and input/output modes) — when that is the case, the right-click advances to the next valid state rather than a fixed facing.

### Disassembling Machines

Most IC2 machines can be picked up cleanly with a wrench, returning the machine block itself and (where applicable) any installed upgrades or internal inventory. The exact pickup behavior depends on the machine; if a particular block does not respond to a wrench, fall back to a pickaxe.

### Cable Insulation

Both wrenches strip insulation from insulated cables, recovering the bare cable item. Right-click an insulated run to remove the insulation; the cable type underneath determines what you get back.

### Hand Wrench Wear

The hand Wrench has 120 durability and behaves like a vanilla tool — every machine rotation or insulation strip ticks the durability down. The wrench does not take damage from being wielded, only from use. It is not repairable; once it breaks, craft a new one from bronze ingots.

### Electric Wrench Power

The Electric Wrench is an `IElectricTool` at tier 1 with a 10,000 EU buffer and **no vanilla durability**. It is treated as an electric tool rather than a wearable battery, so it shows the energy bar above the durability bar. Charge it from a charger, a charging pad, a BatBox/CESU, or an Energy Crystal's right-click. The electric version is functionally unlimited — keep it powered and it never wears out.

## Crafting

Both wrenches are 3x3 shaped recipes.

| Wrench | Electric Wrench |
|:------:|:---------------:|
| <Recipe id="ic2_120:wrench" /> | <Recipe id="ic2_120:electric_wrench" /> |

## Related

- [Tools and Armor reference](../reference/tools_armor.md)
- [Batteries and Mobile Power](../reference/energy_items.md)
- [Energy Crystal and Lapotron Crystal](energy_crystal.md) — the most convenient way to top up the Electric Wrench in the field
- [Drills](drills.md) — the other half of the electric tool line
