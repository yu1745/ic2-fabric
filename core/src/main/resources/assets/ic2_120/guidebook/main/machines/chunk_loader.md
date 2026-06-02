---
navigation:
  title: Chunk Loader
  parent: index.md
  position: 65
  icon: ic2_120:chunk_loader
item_ids:
  - ic2_120:chunk_loader
---

# Chunk Loader

<BlockImage id="ic2_120:chunk_loader" p:facing="north" scale="4" />

The Chunk Loader keeps selected chunks loaded and ticking when no players are nearby. It uses a **9x9 selection grid** centered on its own chunk, and can keep up to **25 chunks** active at once.

## Energy and Storage

- **EU Storage**: 10,000 EU
- **Max input**: 32 EU/t
- **Cost**: 1 EU per loaded chunk per tick

## Operation

The center chunk is always selected and cannot be toggled off. Use the GUI to toggle other chunks in the 9x9 grid. The machine refuses to enable more than 25 chunks.

Every tick, it consumes EU equal to the number of selected chunks. If it cannot pay the full cost, or if no chunks are selected, it releases all forced chunk tickets and turns inactive.

## Usage

Place the Chunk Loader in the center of the area you care about, select only the chunks that contain active machines or farms, and provide enough LV power for the selected count. A full 25-chunk setup costs 25 EU/t continuously; the default single center chunk costs 1 EU/t.
