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

The Induction Furnace is the advanced form of the Electric Furnace. It uses the same vanilla `smelting` recipes, but has two independent input lanes and can run both at once when each lane has room for its own output.

Its speed depends on stored heat. A redstone signal heats the machine while it has EU available; without redstone power, the furnace cools down again.

## Energy

- **Tier:** 2
- **Internal storage:** 1,600 EU before Energy Storage upgrades
- **Maximum input:** 128 EU/t before Transformer upgrades
- **EU output:** none
- **Heating cost:** 1 EU/t while redstone-powered and below maximum heat
- **Processing cost:** 150 EU per item at full heat; each active lane spends 15 EU/t for a 10-tick smelt at maximum heat

Energy may come from the IC2 energy network or from a tier 2 or higher battery in the discharging slot.

## Heat and Redstone

Heat ranges from 0 to 10,000. While the block receives redstone power and can spend EU, heat rises by 5 per tick until it reaches 10,000. When it is not redstone-powered, heat falls by 5 per tick.

The furnace can process as soon as heat is above 0. Processing time is `100000 / heat` ticks, with a minimum of 10 ticks at full heat. At lower heat, each active lane spends at least 1 EU/t while its progress advances, so the exact total EU per item can differ from the full-heat value. If heat reaches 0, progress in both lanes is reset.

## Smelting

Each input slot is paired with its own output slot. The left input smelts into the left output, and the right input smelts into the right output. If an output is blocked, full, or contains a different item, only that lane stops; the other lane can keep working.

Compared with the Electric Furnace, the Induction Furnace trades instant startup for redstone-controlled warm-up, much faster full-heat smelting, and two parallel smelting lanes. It is also crafted from an Electric Furnace and an Advanced Machine Casing.

## Slots

- **Inputs:** two valid vanilla smelting inputs
- **Outputs:** two matching smelting outputs; automation can extract from these slots
- **Battery:** one tier 2 or higher chargeable IC2 battery item for discharging into the furnace
- **Upgrades:** two upgrade slots

## Upgrades and Automation

Energy Storage upgrades add 10,000 EU of buffer each when installed in the upgrade slots. Transformer upgrades raise the accepted input tier. Ejector upgrades push both output slots into adjacent inventories, using their filter and side settings.

External item transfer can insert valid smelting inputs, one battery, and Ejector or Pulling upgrade items from any side, and can extract only the two output slots. In the current implementation, Pulling upgrade items are accepted by the routed upgrade slots, but the Induction Furnace does not actively pull inputs with them.

## Recipe

<Recipe id="ic2_120:induction_furnace" />
