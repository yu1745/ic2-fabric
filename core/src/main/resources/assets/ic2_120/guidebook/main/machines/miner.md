---
navigation:
  title: Miner
  parent: index.md
  position: 73
  icon: ic2_120:miner
item_ids:
  - ic2_120:miner
---

# Miner

<BlockImage id="ic2_120:miner" p:facing="north" scale="4" />

The Miner is an automated digging machine that excavates ores and blocks in a radius below its position. It requires mining pipes to reach downward and EU to operate.

## Operation

The Miner drills downward, consuming **mining pipes** from its inventory to extend its reach. It will mine any breakable blocks within its mining radius, collecting all drops into its output inventory. When a pipe reaches the bottom, the Miner begins mining horizontally within its configured radius.

## Scanners

The Miner supports OD (Orientation Detector) and OV (Orientation Viewer) scanners to control its mining area:

- **OD Scanner**: Limits mining to a specific radius directly below the miner.
- **OV Scanner**: Expands the scanning range, allowing the miner to cover a larger area.

## Requirements

- **EU**: Power to operate the mining mechanism.
- **Mining Pipes**: Consumed as the miner drills deeper.
- **Scanners** (optional): For area control.

## Usage

Place the Miner above the area you wish to mine. Supply mining pipes and EU power. The miner will automatically excavate ores and stone in its range, providing a steady stream of resources without manual digging.
