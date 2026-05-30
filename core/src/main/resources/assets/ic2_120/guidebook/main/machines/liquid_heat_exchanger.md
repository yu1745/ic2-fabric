---
navigation:
  title: Liquid Heat Exchanger
  parent: index.md
  position: 57
  icon: ic2_120:liquid_heat_exchanger
item_ids:
  - ic2_120:liquid_heat_exchanger
---

# Liquid Heat Exchanger

<BlockImage id="ic2_120:liquid_heat_exchanger" scale="4" />

The Liquid Heat Exchanger transfers heat between fluids. It can either heat up a fluid using heat (HU) from a heat source, or cool down a fluid by extracting heat into a cooling fluid. It is an essential component in steam and thermal management systems.

## Operation

The Liquid Heat Exchanger has two fluid tanks and a heat buffer:

- **Hot Fluid Tank**: receives heat from the input fluid
- **Cold Fluid Tank**: receives the cooled output fluid
- **Heat Buffer**: stores heat transferred between fluids

### Heating Mode

When supplied with external heat (HU), the exchanger heats the input fluid. For example, water can be heated into steam.

### Cooling Mode

When supplied with a cooling fluid, the exchanger cools the hot input fluid by transferring heat into the coolant.

## Slots

- Top-left: input fluid container (filled)
- Bottom-left: output fluid container (empty)
- Top-right: coolant/heat input container
- Bottom-right: coolant/heat output container

## Usage

Connect a heat source (Solid Heat Generator, Fluid Heat Generator, etc.) to the exchanger's heat input. Supply a fluid to be heated via fluid pipes or cells. The heated or cooled fluid can be extracted from the output tank.

## Recipe

<Recipe id="ic2_120:liquid_heat_exchanger" />
