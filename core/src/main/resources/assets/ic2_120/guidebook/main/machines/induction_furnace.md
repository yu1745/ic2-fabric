---
navigation:
  title: Induction Furnace
  parent: index.md
  position: 28
  icon: ic2_120:induction_furnace
item_ids:
  - ic2_120:induction_furnace
---

# Induction Furnace

<BlockImage id="ic2_120:induction_furnace" p:facing="north" p:active="true" scale="4" />

<<<<<<< HEAD
The Induction Furnace is the advanced form of the Electric Furnace. It uses the same vanilla `smelting` recipes and has two input slots. Both slots share a single heat-driven progress bar: when progress reaches the threshold, both slots are processed simultaneously, so filling both slots is more energy-efficient than filling one.
=======
The Induction Furnace is the advanced form of the Electric Furnace. It uses the same vanilla `smelting` recipes, but has two independent input lanes and can run both at once when each lane has room for its own output.
>>>>>>> backup/1.21.1

Its speed depends on stored heat. A redstone signal heats the machine while it has EU available; without redstone power, the furnace cools down again.

## Energy

- **Tier:** 2
<<<<<<< HEAD
- **Internal storage:** 10,000 EU before Energy Storage upgrades
- **Maximum input:** 128 EU/t before Transformer upgrades
- **EU output:** none
- **Heating cost:** 1 EU/t while redstone-powered and below maximum heat
- **Processing cost:** 15 EU/t while smelting; at full heat a smelt cycle takes 13 ticks (about 195 EU). When both slots are filled, the cycle produces two items for the same EU cost, halving the energy per item.
=======
- **Internal storage:** 1,600 EU before Energy Storage upgrades
- **Maximum input:** 128 EU/t before Transformer upgrades
- **EU output:** none
- **Heating cost:** 1 EU/t while redstone-powered and below maximum heat
- **Processing cost:** 150 EU per item at full heat; each active lane spends 15 EU/t for a 10-tick smelt at maximum heat
>>>>>>> backup/1.21.1

Energy may come from the IC2 energy network or from a tier 2 or higher battery in the discharging slot.

## Heat and Redstone

<<<<<<< HEAD
Heat ranges from 0 to 10,000. While the block can operate or receives redstone power, and can spend 1 EU/t, heat rises by 1 per tick. When these conditions are not met, heat falls by 4 per tick.

Progress accumulates at `floor(heat / 30)` per tick. A smelt cycle completes when progress reaches 4,000. At full heat (10,000) this takes about 13 ticks; at lower heat it takes proportionally longer. Both input slots share the same progress bar: when a cycle completes, both slots that have valid ingredients and output room are processed at the same time. If neither slot can operate, progress is reset to 0.

## Smelting

The left input slot is paired with the left output slot, and the right input with the right output. Because both slots share a single progress bar, filling both slots gives twice the output per cycle for the same energy cost. If an output is blocked, full, or contains a different item, only that slot is skipped during processing; the other slot still works normally.

Compared with the Electric Furnace, the Induction Furnace trades instant startup for redstone-controlled warm-up, much faster full-heat smelting, and a shared-progress dual-slot design that rewards keeping both slots loaded. It is also crafted from an Electric Furnace and an Advanced Machine Casing.
=======
Heat ranges from 0 to 10,000. While the block receives redstone power and can spend EU, heat rises by 5 per tick until it reaches 10,000. When it is not redstone-powered, heat falls by 5 per tick.

The furnace can process as soon as heat is above 0. Processing time is `100000 / heat` ticks, with a minimum of 10 ticks at full heat. At lower heat, each active lane spends at least 1 EU/t while its progress advances, so the exact total EU per item can differ from the full-heat value. If heat reaches 0, progress in both lanes is reset.

## Smelting

Each input slot is paired with its own output slot. The left input smelts into the left output, and the right input smelts into the right output. If an output is blocked, full, or contains a different item, only that lane stops; the other lane can keep working.

Compared with the Electric Furnace, the Induction Furnace trades instant startup for redstone-controlled warm-up, much faster full-heat smelting, and two parallel smelting lanes. It is also crafted from an Electric Furnace and an Advanced Machine Casing.
>>>>>>> backup/1.21.1

## Slots

- **Inputs:** two valid vanilla smelting inputs
- **Outputs:** two matching smelting outputs; automation can extract from these slots
- **Battery:** one tier 2 or higher chargeable IC2 battery item for discharging into the furnace
- **Upgrades:** two upgrade slots

## Upgrades and Automation

<<<<<<< HEAD
Energy Storage upgrades add 10,000 EU of buffer each when installed in the upgrade slots. Transformer upgrades raise the accepted input tier. Ejector upgrades push both output slots into adjacent inventories, using their filter and side settings. In vanilla IC2 the Induction Furnace ignores overclocking and processing upgrades entirely; this implementation follows that behavior.

External item transfer can insert valid smelting inputs, one battery, and Ejector or Pulling upgrade items from any side, and can extract only the two output slots. Pulling upgrades actively pull valid smelting inputs into the two input slots, while Ejector upgrades push the two output slots. The two upgrade slots themselves are never treated as machine input or output targets.
=======
Energy Storage upgrades add 10,000 EU of buffer each when installed in the upgrade slots. Transformer upgrades raise the accepted input tier. Ejector upgrades push both output slots into adjacent inventories, using their filter and side settings.

External item transfer can insert valid smelting inputs, one battery, and Ejector or Pulling upgrade items from any side, and can extract only the two output slots. In the current implementation, Pulling upgrade items are accepted by the routed upgrade slots, but the Induction Furnace does not actively pull inputs with them.
>>>>>>> backup/1.21.1

## Recipe

<Recipe id="ic2_120:induction_furnace" />
