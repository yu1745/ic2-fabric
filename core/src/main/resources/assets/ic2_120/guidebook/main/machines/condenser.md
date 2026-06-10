---
navigation:
  title: Condenser
  parent: index.md
  position: 43
  icon: ic2_120:condenser
item_ids:
  - ic2_120:condenser
---

# Condenser

<BlockImage id="ic2_120:condenser" p:facing="north" p:active="true" scale="4" />

The Condenser cools steam back into distilled water. It is the recovery machine for steam loops: after a Steam Kinetic Generator uses regular steam, it sends the remaining exhaust steam to adjacent Condensers, where it can be condensed and returned to a Steam Generator as distilled water.

## Operation

The machine accepts regular steam and superheated steam into its steam tank. Every tick it consumes steam up to its current cooling rate and adds that amount to an internal progress buffer. Each 10,000 mB of steam processed produces 100 mB of distilled water, so the practical recovery ratio is **100 mB steam → 1 mB distilled water**.

Without heat vents the Condenser still works passively at 100 mB/t and does not consume EU. Each installed heat vent adds another 100 mB/t of cooling and costs 2 EU/t while steam is being processed.

## Tanks and Power

- **Steam tank:** 8,000 mB, accepts steam and superheated steam
- **Distilled water tank:** 8,000 mB, outputs distilled water
- **Base cooling:** 100 mB/t, no EU cost
- **Heat vents:** up to 4, +100 mB/t each
- **EU use:** 2 EU/t per installed heat vent
- **EU storage:** 100,000 EU
- **Energy tier:** 3
- **Max EU input:** 512 EU/t

## Slots

- Four heat vent slots increase cooling speed. Each installed heat vent adds 100 mB/t and raises the running cost by 2 EU/t.
- The discharge slot accepts batteries and fills the internal EU buffer.
- The tier slot accepts a battery-tier item to raise the accepted battery tier, starting from tier 3.
- The water input slot accepts empty cells or empty buckets and fills them from the distilled-water tank.
- The water output slot receives filled containers. Empty cells become distilled water cells; empty buckets become water buckets because distilled water has no bucket item.

## Automation

Fluid pipes can insert steam or superheated steam and extract distilled water from any side. The Condenser also actively pulls steam from neighboring fluid storages and pushes distilled water to neighboring storages at its built-in fluid transfer rate. Its item automation routes heat vents, batteries, and empty fluid containers into their matching slots, and extracts output containers plus removable vent and battery items.

## Steam Loop Role

Place Condensers next to a Steam Kinetic Generator when running regular steam. The turbine keeps 10% of consumed steam as distilled water in its own tank and tries to send the other 90% only to adjacent Condensers; if that exhaust has nowhere to go, it is vented and can cause small explosions. The Condenser turns that exhaust into distilled water that can be piped back into the Steam Generator, reducing calcification and closing the water loop.

## Recipe

<Recipe id="ic2_120:condenser" />
