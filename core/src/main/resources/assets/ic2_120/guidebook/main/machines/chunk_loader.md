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

The Chunk Loader keeps a **3x3** chunk area (9 chunks total) loaded and ticking even when no players are nearby, ensuring your machines continue operating and farms keep running.

## Energy and Storage

- **EU Storage**: 10,000 EU
- **Cost**: 1 EU per chunk per tick (9 EU/t total for the full 3x3 area)

## Operation

When powered, the Chunk Loader keeps all chunks in a 3x3 square centered on its own chunk loaded into memory. This allows machines, crops, and other tile entities in the area to continue processing regardless of player proximity.

## Usage

Place the Chunk Loader in the chunk you wish to keep loaded. Supply continuous EU power — if the internal buffer runs out, the chunks will unload. The machine consumes 1 EU/t per chunk, for a total of 9 EU/t when all 9 chunks are active. Use this to keep remote mining operations, crop farms, and processing lines running at all times.
