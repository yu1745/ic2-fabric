---
navigation:
  title: Electric Heat Generator
  parent: index.md
  position: 55
  icon: ic2_120:electric_heat_generator
item_ids:
  - ic2_120:electric_heat_generator
---

# Electric Heat Generator

<BlockImage id="ic2_120:electric_heat_generator" scale="4" />

The Electric Heat Generator converts stored EU directly into Heat Units (HU). It is a controllable heat source for builds that already have an EU supply and need heat for nearby heat machines.

## Output

The conversion rate is **1 EU = 1 HU**. Heat output depends on the installed coils:

- 1 coil: **10 HU/t**
- 10 coils: **100 HU/t**

The machine has **10 coil slots**, and each slot accepts one <ItemLink id="ic2_120:coil" />. It has an internal **10,000 EU** buffer, accepts up to **2,048 EU/t** from the EU network, and does not output EU.

Heat is not buffered inside the Electric Heat Generator. Each tick, it consumes only enough EU for the heat it can generate from the installed coils, then immediately tries to send that HU to the connected heat consumer. Any HU that is generated but not accepted by the consumer is lost.

The generator only runs when it has EU, at least one coil, redstone control allows operation, and a valid heat consumer is attached to its heat-transfer face.

## Slots

- Coil grid: 10 slots, one coil per slot
- Battery slot: accepts one chargeable battery item and discharges it into the internal EU buffer
- Upgrade slots: none

## Usage

Connect an EU source to any side, or place a charged battery in the battery slot. Automation can insert coils into the coil slots and batteries into the battery slot; items can be extracted from any machine slot.

Heat transfer is direct machine-to-machine transfer, not a heat network. The Electric Heat Generator sends HU only through its heat-transfer face, which is the side it faces when placed. Put the consumer directly against that side and rotate the consumer so its heat-transfer face points back at the generator. If the faces are not aligned, the generator stays inactive and does not consume EU.

Useful heat consumers include:

- <ItemLink id="ic2_120:steam_generator" />
- <ItemLink id="ic2_120:stirling_generator" />
- <ItemLink id="ic2_120:blast_furnace" />
- <ItemLink id="ic2_120:fermenter" />

## Recipe

<Recipe id="ic2_120:electric_heat_generator" />
