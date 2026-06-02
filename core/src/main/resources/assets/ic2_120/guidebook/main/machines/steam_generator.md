---
navigation:
  title: Steam Generator
  parent: index.md
  position: 18
  icon: ic2_120:steam_generator
item_ids:
  - ic2_120:steam_generator
---

# Steam Generator

<BlockImage id="ic2_120:steam_generator" p:facing="north" p:active="true" scale="4" />

The Steam Generator is a boiler for the heat system. It accepts up to 1,200 HU/t from adjacent heat generators and combines that heat with water to make either steam or superheated steam. It does not use EU directly.

Water can be normal water or distilled water. Both work as boiler feed, but normal water adds scale as it is consumed. At 100,000 mB of scale the boiler is calcified and stops accepting heat; use distilled water for continuous operation.

## Controls

The GUI has two adjustable settings:

- **Feed rate**: 0-1,000 mB/t, default 100 mB/t. This is the maximum water the boiler will consume each tick.
- **Pressure valve**: 0-300 bar, default 100 bar. Higher pressure raises the effective boiling temperature.

At 0 bar and below 100°C, the machine behaves like a pass-through for water instead of boiling it. With pressure set above 0, or once the boiler is already at least 100°C, it tries to boil the configured feed rate.

## Heat and Steam

Each 1 mB of consumed water becomes 100 mB of steam, so 10 mB/t water feed produces up to 1,000 mB/t steam. The actual rate is limited by available water, available heat, the feed setting, and whether the output can be cleared.

The boiler tracks system temperature. If it has heat but no water, or the feed rate is set to 0, it heats up and only cools slowly. When producing at 374°C or hotter, the output switches from steam to superheated steam. Above 500°C the boiler explodes.

| Output | Requirement | Use |
|------|------|------|
| Steam | Producing below 374°C | Standard input for the Steam Kinetic Generator |
| Superheated Steam | Producing at 374°C or hotter | More KU per mB and lower turbine wear |

## Tanks and Automation

- **Water tank**: 10,000 mB
- **Steam tank**: 100,000 mB
- **Accepted input**: water or distilled water
- **Extractable output**: steam or superheated steam
- **Fluid access**: all sides
- **Item slots**: none

The machine exposes Fabric fluid storage on every side. Pipes and tanks can insert water or distilled water and extract the current steam output. It also actively pulls water and ejects steam to adjacent fluid storages at the normal fluid upgrade rate, so a simple adjacent tank or pipe network is enough for most setups. Keep the output line clear: steam left in the internal output tank is vented or recovered on the next tick, with a 10% chance of a small explosion.

Steam and superheated steam are gases, not bucket fluids. If released into the world they rise, hurt living entities in the steam block, and disappear after a short time; superheated steam cools into regular steam before dissipating.

## Practical Setup

Place a heat generator next to the Steam Generator, feed distilled water into any side, and pipe steam from any side into a Steam Kinetic Generator. Start with a modest feed rate and raise it only when the heat input can keep the system hot enough and the steam line can empty the output tank every tick.

For superheated steam, increase pressure and heat until the system is producing at 374°C or higher, then route the output to equipment that can use superheated steam. Never run the boiler dry under heat, and do not let it climb past 500°C.

## Recipe

<Recipe id="ic2_120:steam_generator" />
