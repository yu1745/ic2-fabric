---
navigation:
  title: Replicator
  parent: index.md
  position: 72
  icon: ic2_120:replicator
item_ids:
  - ic2_120:replicator
---

# Replicator

<BlockImage id="ic2_120:replicator" p:facing="north" scale="4" />

The Replicator duplicates items from UU-Matter using stored patterns. It requires both UU-Matter fluid and EU to operate, and must have access to a Pattern Storage unit with the desired pattern.

## Energy and Storage

- **Energy tier:** 3
- **Max EU input:** 512 EU/t
- **EU storage:** 400,000 EU
- **UU-Matter tank:** 16,000 mB
- **Base speed:** 5 uB of template cost per tick
- **Base draw:** 512 EU/t

## Requirements

To replicate an item, the Replicator needs:

1. **Redstone power:** no redstone means no work.
2. **UU-Matter:** inserted by pipe from any side except the front face, or drained from UU-Matter buckets/cells in the container input slot.
3. **EU:** continuous tier 3 power.
4. **Pattern:** a selected template in a unique adjacent Pattern Storage.
5. **Output space:** the output slot must be empty or stack with the result.

## Operation

Select a template from the adjacent Pattern Storage, set single or continuous mode, and power the Replicator with redstone. The machine consumes UU-Matter gradually according to the template's UU cost and adds progress each tick while spending EU. When progress reaches the template cost, one output item is created.

Single mode makes one item and then waits. Continuous mode keeps running while redstone, EU, UU-Matter, a selected template, and output space remain available. The cancel button clears current progress and fluid-consumption remainder.

## Slots and Automation

- **Output slot:** replicated items
- **Container input:** UU-Matter Bucket, UU-Matter Cell, or filled Fluid Cell containing UU-Matter
- **Container output:** empty bucket or empty cell after draining
- **Battery slot:** tier 3 battery discharge
- **Upgrade slots:** 4 slots

Supported upgrades are Overclocker, Transformer, Energy Storage, Ejector, Pulling, Fluid Ejector, and Fluid Pulling upgrades. Pulling upgrades can pull UU-Matter containers; Fluid Pulling upgrades can pull UU-Matter fluid into the tank; Ejector upgrades can push output items.

## Full Chain

The Replicator is the final step in the UU-Matter production chain: **Matter Generator** produces UU-Matter, **Pattern Scanner** creates patterns, **Pattern Storage** stores them, and the **Replicator** creates the final items. This system allows you to duplicate any scannable item, from basic resources to rare materials.
