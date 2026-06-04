---
navigation:
  title: Steam Kinetic Generator
  parent: index.md
  position: 24
  icon: ic2_120:steam_kinetic_generator
item_ids:
  - ic2_120:steam_kinetic_generator
---

# Steam Kinetic Generator

<BlockImage id="ic2_120:steam_kinetic_generator" p:facing="north" p:active="true" scale="4" />

The Steam Kinetic Generator is the KU-producing half of the steam chain. Install a <ItemLink id="ic2_120:steam_turbine" /> in the turbine slot, feed the machine steam or superheated steam, and pull KU from any side.

Regular steam produces 2 KU per mB. Superheated steam produces 4 KU per mB, causes half as much turbine wear, and leaves the machine as regular steam.

## Operation

The internal steam tank holds 21,000 mB. Each server tick, if a turbine is installed and the machine is not water-blocked, the generator consumes all steam currently in that tank and adds KU to its internal KU buffer.

**Raw KU = consumed steam in mB x KU per mB**

The KU buffer holds 4,096 KU, so actual output for that tick is limited by free buffer space. Existing distilled water in the output tank also throttles production: an empty distilled-water tank gives full KU, and a nearly full tank gives only a small fraction.

## Steam Handling

| Input | KU | Fluid after processing | Turbine wear |
|------|----|------------------------|--------------|
| Steam | 2 KU/mB | 90% of the input is pushed as steam to adjacent Condensers only | 2 durability per 20 ticks |
| Superheated Steam | 4 KU/mB | The full input amount is pushed out as regular steam to adjacent fluid storages | 1 durability per 20 ticks |

For regular steam, the internal condensate counter gains 1 progress per 10 mB consumed. Every 100 progress creates 1 mB of distilled water in the machine's distilled-water tank. The tank holds 1,000 mB.

If outgoing steam has nowhere to go, the machine vents the leftover steam. Venting has a 10% chance each tick to cause a small explosion, so give the output steam a receiver.

## Layout for Superheated Steam

The Steam Kinetic Generator pushes its processed steam out through **all six faces** — it does not favor a particular side. It only ever inserts into fluid containers that touch the machine; there is no internal output buffer beyond the 21,000 mB input tank.

- For **regular steam**, only adjacent <ItemLink id="ic2_120:condenser" /> blocks are accepted as receivers.
- For **superheated steam**, any adjacent fluid storage — tank, pipe, condenser — is accepted.

To make the most of superheated steam, place a Condenser or a fluid tank **directly against the Steam Kinetic Generator on at least one face**. Steam is pushed to whichever neighboring container has room first, so giving it any touching face is enough to avoid venting (and the 10%-per-tick explosion chance that comes with it). The generator's own `facing` is for KU output only; it does not affect which side steam leaves from.

## Water Blocking

The distilled-water tank is an output tank. It can be drained from any side as distilled water, and the machine also actively tries to eject it to adjacent fluid storages.

If the distilled-water tank has no room for the next 1 mB of condensate, the turbine becomes water-blocked. While blocked, it produces no KU; incoming steam is dumped as regular steam instead. Drain at least 1 mB of distilled water to unblock it.

## Output

- **Steam Tank:** 21,000 mB
- **Distilled Water Tank:** 1,000 mB
- **KU Buffer:** 4,096 KU
- **KU Output:** any side
- **Fluid I/O:** any side
- **Tier:** 3

## Turbine Wear

The steam turbine has 48 durability. Wear is applied every 20 ticks while the machine is processing steam. Regular steam applies 2 damage each time; superheated steam applies 1. When the turbine reaches its durability limit, it breaks and the slot becomes empty.

## Slots and Automation

- **Turbine Slot:** accepts one <ItemLink id="ic2_120:steam_turbine" />
- **Upgrade Slot:** accepts ejector, pulling, fluid ejector, and fluid pulling upgrades

Fluid automation can insert steam or superheated steam and extract distilled water from any side. Item automation can insert a steam turbine into the turbine slot and valid upgrades into the upgrade slot; both slots are also extractable.

## Recipe

<Recipe id="ic2_120:steam_kinetic_generator" />

## Related

- <ItemLink id="ic2_120:steam_turbine" />
- <ItemLink id="ic2_120:steam_generator" />
