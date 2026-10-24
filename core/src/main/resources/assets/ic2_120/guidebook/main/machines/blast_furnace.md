---
navigation:
  title: Blast Furnace
  parent: index.md
  position: 31
  icon: ic2_120:blast_furnace
item_ids:
  - ic2_120:blast_furnace
---

# Blast Furnace

<BlockImage id="ic2_120:blast_furnace" p:facing="north" p:active="true" scale="4" />

The Blast Furnace turns iron materials into steel. It does not take EU directly: it must be fed Heat Units (HU) into its heat face, and it consumes compressed air while the recipe is progressing.

## Heat and Temperature

The heat face is the machine's facing side, so place a heat generator against that side and point the two machines at each other. The Blast Furnace has an internal HU buffer and can continuously receive heat.

- Temperature range: **0-1700 C**
- Work starts at **1401 C**

The furnace dissipates heat continuously, and the dissipation (HU/t needed to hold the current temperature) grows linearly with temperature:

- 0-1401 C: dissipation rises from **0 to 50 HU/t**
- 1402-1700 C: dissipation rises from **50 to 100 HU/t**

Net heat per tick = HU input − dissipation. Positive net heat accumulates and raises the temperature; negative net heat lowers it; exactly zero holds the temperature steady. **The final stable temperature depends entirely on the HU input**:

- **50 HU/t** → stabilizes at **1401 C** (just enough to start working)
- **100 HU/t** → stabilizes at **1700 C**
- Linear in between, e.g. 75 HU/t → about 1550 C

Heating speed is proportional to net HU: more HU input warms faster. The temperature still rises and falls with the HU input while working — it is no longer frozen. If HU runs short and the temperature drops below 1401 C, the smelting progress is lost.

Higher temperature is better: one steel takes about **10000 ticks at 1401 C**, **8400 ticks at 1500 C**, **6000 ticks at 1600 C**, and **4000 ticks at 1700 C**.

## Air and Recipes

Compressed air is stored in an internal **8-bucket / 8000 mB** tank. The tank accepts compressed air through Fabric fluid pipes or from the air input slot. Inserting a Compressed Air Cell, or another full compressed-air fluid cell/container, adds one bucket and returns an Empty Cell to the empty-cell output when applicable.

Air use falls as temperature rises:

- 1401 C: about **6000 mB** per steel
- 1500 C: about **5200 mB** per steel
- 1600 C: about **4700 mB** per steel
- 1700 C: about **4200 mB** per steel

Each valid input produces **1 Steel Ingot** and **1 Slag**:

- Iron Dust
- Crushed Iron
- Purified Iron
- Iron Ingot
- Iron Ore
- Deepslate Iron Ore
- Raw Iron

## Slots

- **Input:** iron material for the blast furnace recipe
- **Air input:** Compressed Air Cells or compatible compressed-air containers
- **Steel output:** Steel Ingots
- **Slag output:** Slag
- **Empty-cell output:** Empty Cells returned from air cells
- **Upgrade slots:** 2 slots for item Pulling and Ejector Upgrades

## Automation

Item automation accepts recipe inputs into the input slot and Compressed Air Cells into the air slot. Outputs can be extracted from the steel, slag, and empty-cell slots. Pulling Upgrades pull recipe inputs or air cells; Ejector Upgrades eject the three output slots.

Fluid automation can insert compressed air into the internal tank from any side except the heat face. The tank is input-only, so it will not output compressed air back into pipes.

For a reliable setup, heat the furnace first, keep the heat source running while recipes process, keep compressed air supplied, and leave space for both steel and slag outputs. A full slag output blocks further steel production.

## Recipe

<Recipe id="ic2_120:blast_furnace" />
