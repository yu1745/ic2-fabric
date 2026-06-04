---
navigation:
  title: Solid Heat Generator
  parent: index.md
  position: 53
  icon: ic2_120:solid_heat_generator
item_ids:
  - ic2_120:solid_heat_generator
---

# Solid Heat Generator

<BlockImage id="ic2_120:solid_heat_generator" scale="4" />

The Solid Heat Generator burns solid fuel into Heat Units (HU). It is a small, direct heat source for machines that consume HU instead of EU.

## Heat Output

When it has a valid heat consumer connected, the Solid Heat Generator burns fuel at **20 HU/t**. Heat is produced and offered immediately each tick; the machine has **no heat buffer**. Any HU that is not accepted by the connected machine during that tick is lost.

Supported fuels are **Coal**, **Charcoal**, and **Coke**. The machine divides each fuel's furnace burn time by 4 before counting ticks:

| Fuel | Furnace ticks | Burns for | Total HU |
|------|---------------|-----------|----------|
| Coal | 1,600 | 400 ticks (20 s) | 8,000 HU |
| Charcoal | 1,600 | 400 ticks (20 s) | 8,000 HU |
| Coke | 3,200 | 800 ticks (40 s) | 16,000 HU |

Coke lasts twice as long as coal or charcoal and is the most efficient per item.

The generator does not start burning unless its heat face is connected to a valid heat consumer.

## Heat Setup

Heat transfer is direct machine-to-machine contact only. Place the Solid Heat Generator so its heat face touches the heat face of the receiving machine; the two heat faces must point into each other. It does not output HU from every side, and heat is not routed through a heat network.

Useful receiving machines include:

- <ItemLink id="ic2_120:blast_furnace" />
- <ItemLink id="ic2_120:steam_generator" />
- <ItemLink id="ic2_120:stirling_generator" />

Because unused HU is discarded, try to keep the consumer ready to accept heat before inserting fuel.

## Slots and Automation

- Fuel slot: accepts solid heat fuels for burning.
- Output slot: present in the interface, but the machine currently produces no item output.

Item automation inserts valid fuel into the fuel slot. Both machine slots are extractable for automation, although the output slot normally remains empty.

## Recipe

<Recipe id="ic2_120:solid_heat_generator" />
