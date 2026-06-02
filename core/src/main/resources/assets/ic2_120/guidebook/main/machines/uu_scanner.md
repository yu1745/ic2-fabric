---
navigation:
  title: Pattern Scanner
  parent: index.md
  position: 70
  icon: ic2_120:uu_scanner
item_ids:
  - ic2_120:uu_scanner
---

# Pattern Scanner

<BlockImage id="ic2_120:uu_scanner" p:facing="north" scale="4" />

The Pattern Scanner (UU Scanner) analyzes items and creates digital patterns that can be stored for later replication. This is the first step in the UU-Matter replication process.

## Energy and Storage

- **Energy tier:** 3
- **Max EU input:** 512 EU/t
- **EU storage:** 200,000 EU
- **Scan time:** 3,300 ticks / 165 seconds
- **Power draw:** 256 EU/t
- **Total scan cost:** 844,800 EU

## Operation

The scanner only scans items that have a replication template in config. It also needs one clear pattern destination: either a unique adjacent Pattern Storage, or a Crystal Memory in the crystal slot when no Pattern Storage is adjacent.

When the scan completes, one input item is consumed and the template is cached inside the scanner. Use the scanner controls to save that cached template into the Crystal Memory or adjacent Pattern Storage, or delete it. While a cached template is waiting, the scanner will not begin another scan.

## Slots

- **Input slot:** item to scan
- **Battery slot:** tier 3 battery discharge
- **Crystal slot:** Crystal Memory target

The scanner has no upgrade slots. Item automation can insert scan inputs, batteries, and Crystal Memory items; all three slots are extractable.

## Usage

1. Place a Pattern Storage next to the scanner, or insert a Crystal Memory.
2. Insert a whitelisted item and provide continuous EU.
3. Save the completed template to storage or crystal.
4. Select the template in Pattern Storage for use by a Replicator.

Each item type only needs to be scanned once. Replication still needs the UU-Matter cost recorded in that template.
