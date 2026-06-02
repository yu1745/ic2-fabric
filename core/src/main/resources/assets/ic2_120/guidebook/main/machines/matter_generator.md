---
navigation:
  title: Matter Generator
  parent: index.md
  position: 69
  icon: ic2_120:matter_generator
item_ids:
  - ic2_120:matter_generator
---

# Matter Generator

<BlockImage id="ic2_120:matter_generator" p:facing="north" scale="4" />

The Matter Generator consumes huge amounts of EU to produce **UU-Matter** for the Replicator. It only runs while powered by redstone and while its internal tank has room.

## Operation

- **Energy tier:** 3
- **EU storage:** 4,000,000 EU
- **UU-Matter tank:** 10,000 mB
- **Without Scrap:** 1,000,000 EU per mB
- **With Scrap:** 166,667 EU and 34 Scrap per mB

Scrap is consumed as progress advances, so even a partial stack helps, but a continuous Recycler feed keeps the machine in its boosted rate.

## Slots

- **Scrap slot:** accepts Scrap only
- **Container input:** Empty Cell, empty Fluid Cell, or bucket
- **Container output:** UU-Matter Cell, filled Fluid Cell, or UU-Matter Bucket
- **Upgrade slots:** 4 slots

The tank is output-only to pipes. Empty containers in the input slot are filled from the tank when at least one bucket of UU-Matter is available.

## Automation and Upgrades

Item automation can insert Scrap, empty containers, and supported upgrades. The Scrap slot, container input, and container output are extractable.

Supported upgrades are Transformer, Energy Storage, Ejector, Pulling, Fluid Ejector, and Fluid Pulling upgrades. Overclockers are not accepted. Fluid Ejector upgrades can push UU-Matter to tanks or a Replicator; Fluid Pulling upgrades usually have no useful input because the internal tank rejects external insertion.

## Replication Chain

The usual chain is Recycler -> Matter Generator -> Replicator. Keep Scrap flowing from the Recycler, power the Matter Generator with redstone, and pipe UU-Matter into a Replicator or storage tank.
