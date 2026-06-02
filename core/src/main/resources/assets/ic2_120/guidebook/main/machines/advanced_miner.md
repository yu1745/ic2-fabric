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
- Put **Mining Pipes** in the pipe slot. The slot can hold up to 1024 pipes.
- Supply EU directly or with a battery in the discharge slot.
- Give the machine a redstone signal. Without redstone it pauses; a **Redstone Inverter Upgrade** makes it run when the signal is off instead.
- Place a chest or other inventory next to the miner for item output.

The machine is tier 3, stores 10,000 EU by default, and accepts up to 512 EU/t before transformer upgrades. Skipped scan steps cost 64 EU from the scanner's own charge; the miner can recharge the scanner from its internal EU buffer. Pipe placement costs 500 EU and one pipe, and mining costs 500 EU per block. Silk Touch raises mining cost to 750 EU. Overclocker upgrades make the scan cycle faster and increase energy use; energy storage and transformer upgrades expand the usual limits.

## Scanning Area

The scanner controls the horizontal square that is checked on every layer below the machine:

- **OD Scanner**: radius 6, a 13x13 area.
- **OV Scanner**: radius 12, a 25x25 area.

The cursor starts at the layer directly under the miner, scans across the square, then moves down one layer. The miner stops when it reaches the bottom of the world. The GUI shows the current cursor coordinates, and the reset button restarts the scan from the top.

Only ore-like blocks are mined: blocks whose registry path contains `ore`, ancient debris, or entries added to the miner config. Machine blocks and unbreakable blocks are ignored.

## Filters

The 15 filter slots accept block items and only affect the Advanced Miner.

- **Blacklist mode** is the default. Blocks in the filter are skipped.
- **Whitelist mode** mines only blocks in the filter.
- Empty filters mine every ore-like block.

The mode button switches between blacklist and whitelist. Silk Touch can also be toggled from the GUI; when enabled, drops are generated with a Silk Touch pickaxe.

## Pipes and Fluids

The miner lays a vertical pipe column downward, then builds short pipe paths so a pipe is adjacent to each target ore before mining it. Pipe placement is limited to 4 pipes per second and consumes one Mining Pipe plus pipe energy each time.

When a layer is finished, the Advanced Miner automatically recovers horizontal branch pipes into the pipe slot and keeps the central column. The recover button stops the scan, gathers connected mining pipes, then resumes if possible.

If pipe placement reaches water or lava, the miner removes the fluid into an internal 1 bucket tank. Add a **Fluid Ejector Upgrade** to push stored fluid into neighboring tanks or fluid handlers.

## Output and Automation

Drops go into a small internal cache, not visible output slots. Each tick the miner tries to insert cached items into adjacent inventories on any side. If the cache reaches 64 items, mining stops until automation makes room.

Item automation can insert valid upgrades, a battery, a scanner, mining pipes, and block filters. Drops leave through the automatic adjacent-inventory insertion, so keep storage or item transport directly next to the miner. Accepted Advanced Miner upgrades are Overclocker, Transformer, Redstone Inverter, Ejector, and Fluid Ejector; the fluid ejector is what moves the internal water or lava tank.
