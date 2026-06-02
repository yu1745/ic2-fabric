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

The Liquid Heat Exchanger turns stored heat fluids into heat units (HU). It consumes **hot coolant** or **lava**, converts that fluid into its cooled form, and sends the released HU into an adjacent heat machine.

## Operation

The machine has an input tank and an output tank. Each tank holds **8 buckets**.

- **Hot coolant -> coolant**
- **Lava -> pahoehoe lava**

Each bucket of input fluid releases **20,000 HU**. Heat is produced only while the exchanger has a valid heat consumer connected on its heat transfer face and enough room in the output tank for the converted fluid.

## Heat Conductors

The ten center slots accept **Heat Conductors**. Each conductor allows **10 HU/t**, up to **100 HU/t** with all ten slots filled. The conductors are required; without them the machine stores fluid but outputs no heat.

The heat rate controls how fast the input fluid is processed. A full set of conductors consumes one bucket in 200 seconds, because one bucket contains 20,000 HU.

## Slots

- Center grid: up to 10 Heat Conductors, one per slot
- Upgrade row: 3 upgrade slots, including fluid ejector and fluid pulling upgrades
- Bottom left pair: filled input container in, empty container out
- Bottom right pair: empty output container in, filled output container out

Input containers may be lava buckets, hot coolant buckets, lava cells, hot coolant cells, or compatible fluid cells. Output containers may be buckets, empty cells, or empty fluid cells, and are filled with coolant or pahoehoe lava.

## Fluids and Automation

Fluid pipes can insert hot coolant or lava into the input tank and extract coolant or pahoehoe lava from the output tank from any side. Right-clicking the block with a compatible fluid container also interacts with the tanks before opening the GUI.

Fluid pulling upgrades let the exchanger pull hot coolant or lava from nearby tanks. Fluid ejector upgrades push the cooled output fluid into nearby tanks. Upgrade filters and side settings can be used to keep the hot and cold loops separated.

## Heat Transfer and Use Cases

HU leaves through the exchanger's heat transfer face. The neighboring machine must be a heat consumer with its own heat face pointed back at the exchanger; otherwise the exchanger will not process fluid.

Common links include:

- Reactor Fluid Port -> Liquid Heat Exchanger -> Steam Generator, converting reactor hot coolant into coolant while feeding a boiler
- Liquid Heat Exchanger -> Stirling Generator, turning hot coolant or lava heat into EU
- Liquid Heat Exchanger -> Blast Furnace or Fermenter, supplying process heat while returning the cooled fluid for reuse

## Recipe

<Recipe id="ic2_120:liquid_heat_exchanger" />
