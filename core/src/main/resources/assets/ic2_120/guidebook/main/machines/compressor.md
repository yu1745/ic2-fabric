---
navigation:
  title: Compressor
  parent: index.md
  position: 34
  icon: ic2_120:compressor
item_ids:
  - ic2_120:compressor
---

# Compressor

<BlockImage id="ic2_120:compressor" p:facing="north" p:active="true" scale="4" />

The Compressor turns loose materials into dense blocks, plates, crystals, and other pressure-made parts. It is a basic tier 1 processing machine, but it sits on several important progression paths: carbon mesh becomes carbon plate, mixed metal ingots become advanced alloy, coal chunks become diamonds, empty cells become compressed air cells, and energium dust becomes an energy crystal.

## Operation

- **Tier / max input:** tier 1, up to 32 EU/t by default.
- **Energy buffer:** 600 EU before upgrades.
- **Processing cost:** 2 EU/t for 300 ticks, for 600 EU total per operation.
- **Upgrades:** overclocker upgrades increase speed and power use, transformer upgrades raise the accepted voltage tier, energy storage upgrades add 10,000 EU each, and item ejector/pulling upgrades can move items to or from neighboring inventories.

## Slots

- Input slot: accepts valid compressor recipe ingredients.
- Output slot: receives the compressed result and any returned empty container.
- Battery slot: accepts one battery item and discharges it into the machine buffer.
- Four upgrade slots: accept machine upgrades.

## Recipes

Notable recipe groups include:

- Ingots and loose resources into storage blocks, such as iron, gold, copper, tin, bronze, steel, lead, silver, lapis, and redstone.
- Small dusts and small nuclear fuel piles into their full-size dust or fuel item.
- Plates into dense plates, including bronze, copper, gold, iron, lapis, lead, obsidian, steel, and tin.
- Material progression recipes such as carbon mesh to carbon plate, coal ball to compressed coal ball, coal chunk to diamond, mixed metal ingot to advanced alloy, and iridium shards to iridium ore.
- Utility compression such as sand to sandstone, red sand to red sandstone, clay balls to clay, bricks to bricks, nether bricks to nether bricks, snow and ice compression, glowstone dust to glowstone, blaze powder to blaze rod, and bronze block to bronze tool handle.
- Cells and buckets: an empty cell becomes a compressed air cell. A water cell or water bucket becomes a snow block and returns an empty cell or bucket.

## Automation

The machine exposes routed item storage. Automation can insert recipe inputs into the input slot, batteries into the battery slot, and upgrades into the upgrade slots. Only the output slot can be extracted directly. Ejector and pulling upgrades use their own filter and direction settings when moving items between adjacent inventories.

## Recipe

<Recipe id="ic2_120:compressor" />
