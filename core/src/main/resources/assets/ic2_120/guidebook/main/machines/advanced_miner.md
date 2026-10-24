---
navigation:
  title: Advanced Miner
  parent: index.md
  position: 74
  icon: ic2_120:advanced_miner
item_ids:
  - ic2_120:advanced_miner
---

# Advanced Miner

<BlockImage id="ic2_120:advanced_miner" p:facing="north" scale="4" />

The Advanced Miner is a redstone-controlled ore miner. It does not use a drill item: its internal drill mines ore-like blocks directly, then sends the drops to adjacent inventories.

## Setup

- Put an **OD Scanner** or **OV Scanner** in the scanner slot. A scanner is required.
- Supply EU directly or with a battery in the discharge slot.
- Give the machine a redstone signal. Without redstone it pauses; a **Redstone Inverter Upgrade** makes it run when the signal is off instead.
- Place a chest or other inventory next to the miner for item output.

The machine is tier 3, stores 4,000,000 EU by default, and accepts up to 512 EU/t before transformer upgrades. Scanned positions cost 64 EU from the scanner's own charge; the miner can recharge the scanner from its internal EU buffer. Mining costs 512 EU per block. Overclocker upgrades increase the number of positions checked per work cycle; energy storage and transformer upgrades expand the usual limits.

## Scanning Area

The scanner controls the horizontal square that is checked on every layer below the machine:

- **OD Scanner**: radius 6, a 13x13 area.
- **OV Scanner**: radius 12, a 25x25 area.

The cursor starts at the layer directly under the miner, scans across the square, then moves down one layer. The miner stops when it reaches the bottom of the world. The GUI shows the current cursor coordinates, and the reset button restarts the scan from the top.

In blacklist mode, every breakable non-fluid block with drops is eligible unless it matches a filter. In whitelist mode, only blocks whose drops match a filter are mined. Machine blocks, block entities, fluids, and unbreakable blocks are ignored.

## Filters

The 15 filter slots accept block items and only affect the Advanced Miner.

- **Blacklist mode** is the default. Blocks whose drops match a filter are skipped.
- **Whitelist mode** mines only blocks whose drops match a filter.
- Empty blacklist filters allow all eligible blocks; an empty whitelist allows none.

The filters match the actual drops, not the target block itself. For example, to whitelist copper ore, put **raw copper** in the filter rather than the copper ore block.

The mode button switches between blacklist and whitelist. Silk Touch can also be toggled from the GUI; when enabled, drops are generated with a Silk Touch pickaxe.

## Fluids

The Advanced Miner mines remotely and does not place or consume Mining Pipes. Add a **Fluid Ejector Upgrade** only if the machine has a fluid tank integration in the current build.

## Output and Automation

Drops go into a small internal cache, not visible output slots. Each tick the miner tries to insert cached items into adjacent inventories on any side. If the cache reaches 64 items, mining stops until automation makes room.

Item automation can insert valid upgrades, a battery, a scanner, mining pipes, and block filters. Drops leave through the automatic adjacent-inventory insertion, so keep storage or item transport directly next to the miner. Accepted Advanced Miner upgrades are Overclocker, Transformer, Redstone Inverter, Ejector, and Fluid Ejector; the fluid ejector is what moves the internal water or lava tank.

## Related

- [OD Scanner / OV Scanner](../items/scanners.md) — electric scanners for Miner and Advanced Miner
