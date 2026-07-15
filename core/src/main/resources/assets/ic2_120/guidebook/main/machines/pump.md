---
navigation:
  title: Pump
  parent: index.md
  position: 40
  icon: ic2_120:pump
item_ids:
  - ic2_120:pump
---

# Pump

<BlockImage id="ic2_120:pump" p:facing="north" p:active="true" scale="4" />

The Pump can face any of the six directions. It collects one-bucket fluid sources or fluid storage blocks in front of it and stores the fluid in its internal tank. **It checks only one straight line directly in front of the pump, never the sides.** It looks up to three blocks ahead, so the front face matters. This also means a pump facing up extracts above itself, while a pump facing down extracts below itself.

<GameScene zoom="6" interactive={true} fullWidth={true}>
  <IsometricCamera yaw="0" pitch="30" />
  <ImportStructure src="pump_range_demo.snbt" />
</GameScene>

## Operation

- **EU Storage:** 800 EU
- **Tier:** 1
- **Input:** 32 EU/t before transformer upgrades
- **Tank:** 8 buckets
- **Work:** 20 progress at 1 EU/t, then 20 EU again to move 1 bucket
- **Reach:** 1 to 3 blocks in front of the pump, in any of the six directions

The pump accepts any fluid when its tank is empty. Once it contains fluid, it will only continue pumping that same fluid until the tank is emptied. It can drain still world fluid blocks and adjacent fluid storages in its front line. Protected fluid blocks are skipped.

## Slots

- **Input:** empty cells or empty universal fluid cells.
- **Output:** filled cells produced from the tank.
- **Discharge:** battery slot for portable EU supply.
- **Upgrades:** four slots for overclocker, transformer, energy storage, fluid pipe, ejector, and pulling upgrades.

Fluid pipe upgrades can push tank contents to adjacent fluid handlers at up to a quarter bucket per tick. Ejector upgrades move filled cells from the output slot, and pulling upgrades can pull empty cells into the input slot.

## Layout

Aim the pump at the source line: source blocks, tanks, or machines must be in front of it, within three blocks. You can aim that line horizontally or vertically by placing the pump against the desired face. Do not attach pipes to the front face for extraction from the pump itself; the pump exposes its internal tank on the other sides.

For larger fluid networks, use the Pump as a powered world-source collector and use Pump Attachments on pipes for passive tank-to-pipe extraction.

## Recipe

<Recipe id="ic2_120:pump" />
