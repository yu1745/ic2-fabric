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

The Miner is an automated ore finder. It scans the area below itself, lays **mining pipes** to any ore it finds, and breaks that ore with the drill installed in its tool slot.

## Operation

Install an **OD Scanner**, a charged **Mining Drill**, **Diamond Drill**, or **Iridium Drill**, and a stack of **Mining Pipes**. The pipe slot can hold up to 1024 pipes. The Miner scans from the layer directly below the machine down to Y -64, moving across each layer in the scanner's square radius.

Only ore-like blocks are mined: blocks whose registry path contains `ore`, ancient debris, and any extra blocks listed in the Miner config. Machine blocks are ignored. Stone, dirt, and other non-ore blocks are scanned through, not mined for output.

When ore is found, the Miner first extends a pipe column downward, then routes mining pipes through passable or breakable blocks until a pipe is adjacent to the target. It does not place a pipe inside the ore block itself. Pipe placement is rate-limited to 4 pipes per second and costs EU per pipe. If the machine runs out of mining pipes, it stops and resumes automatically after more pipes are inserted.

Breaking ore costs EU separately from scanning and pipe placement. A Mining Drill uses the lower break cost; a Diamond Drill or Iridium Drill lets the Miner harvest blocks that require diamond-level tools but costs more EU per mined block. The drill item itself is used as the tool profile for drops; it is not discharged directly by the Miner.

## Scanners

The basic Miner accepts the **OD Scanner** only. The OD Scanner gives a radius of 6 blocks, so the Miner scans a **13x13** column centered below the machine.

The Miner charges an electric scanner in its scanner slot from its own EU buffer, then uses that scanner slot while scanning non-ore blocks. The OV Scanner is for the Advanced Miner and cannot be inserted into the basic Miner.

The restart button starts the scan over from the top of the area. The pipe recovery button pulls connected mining pipes back into the pipe slot when there is space.

## Output and Fluids

Drops first enter the two internal output slots. With an **Ejector Upgrade**, the Miner tries to push those drops into adjacent item inventories using the upgrade's filter and side setting. Any drops left in the output slots after that attempt are spawned at the Miner, so place an inventory next to it and use an ejector upgrade for clean automation.

When a pipe route crosses water or lava, the Miner removes the fluid and stores up to one bucket internally. If that tank is full, pipe placement waits. A **Fluid Ejector Upgrade** can empty the internal tank into adjacent fluid storage.

## Requirements

- **EU**: 10,000 EU internal buffer, tier 2 input by default.
- **OD Scanner**: Required; defines the 13x13 scan area.
- **Drill**: Mining Drill, Diamond Drill, or Iridium Drill.
- **Mining Pipes**: Required; consumed while laying the pipe network and recovered by the recovery action.
- **Upgrades**: The basic Miner uses its single visible upgrade slot for automation upgrades such as Ejector and Fluid Ejector.

## Usage

Place the Miner above the area to search, give it EU, an OD Scanner, a drill, and plenty of mining pipes. Put a chest or other item inventory beside it and configure an Ejector Upgrade if you want the ore drops captured instead of dropped on the ground.
