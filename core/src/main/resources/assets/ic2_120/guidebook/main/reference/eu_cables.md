---
navigation:
  title: EU Cables and Transformers
  parent: index.md
  position: 213
  icon: ic2_120:copper_cable
item_ids:
  - ic2_120:tin_cable
  - ic2_120:insulated_tin_cable
  - ic2_120:copper_cable
  - ic2_120:insulated_copper_cable
  - ic2_120:gold_cable
  - ic2_120:insulated_gold_cable
  - ic2_120:double_insulated_gold_cable
  - ic2_120:iron_cable
  - ic2_120:insulated_iron_cable
  - ic2_120:double_insulated_iron_cable
  - ic2_120:triple_insulated_iron_cable
  - ic2_120:glass_fibre_cable
  - ic2_120:splitter_cable
  - ic2_120:limiter_cable
---

# EU Cables and Transformers

Cables move EU from generators and storage to machines. Machines have an input tier, cables have a transmission tier, and when wiring you have to consider maximum throughput, loss, and electrocution risk all at once.

## Cable Specs

| Cable | Tier | Nominal Throughput | Loss |
|------|:---:|---:|---:|
| Tin Cable / Insulated Tin Cable | 1 | 32 EU/t | 0.2 EU/block |
| Copper Cable / Insulated Copper Cable | 2 | 128 EU/t | 0.2 EU/block |
| Gold Cable / Insulated Gold Cable / 2× Insulated Gold Cable | 3 | 512 EU/t | 0.4 EU/block |
| HV Cable / Insulated HV Cable / 2×/3× Insulated HV Cable | 4 | 2048 EU/t | 0.8 EU/block |
| Glass Fibre Cable | 5 | 8192 EU/t | 0.025 EU/block |
| EU Splitter Cable | 5 | 8192 EU/t | 0.5 EU/block |
| EU Limiter Cable | 5 | 8192 EU/t | 0.5 EU/block |

## Insulation

- Bare cables can electrocute players; the Hazmat Suit set and Rubber Boots provide protection.
- Rubber can be used to insulate Tin, Copper, Gold, and HV cables.
- The Plate Cutting Shears strip insulation and are also used to cut metal plates into cables.
- Glass Fibre Cables come with the highest insulation, perfect for long-distance and high-voltage backbones.

## Splitter and Limiter

- **EU Splitter Cable**: right-click to set a redstone trigger threshold from 1 to 15 and optionally invert the condition. By default, it disconnects at signal strength 1 or above.
- **EU Limiter Cable**: right-click to open a UI for setting a maximum throughput, useful for protecting low-tier branches.

## Transformers

Transformers shift the network between voltage tiers. The LV, MV, HV, and Ultra-HV Transformers correspond to LV, MV, HV, and EV networks respectively.

- The side facing low-tier machines should connect to a low-tier output.
- The side facing the high-tier backbone should connect to a higher-tier cable.
- If a machine frequently pops cables or loses power, first check its input tier, the cable tier, and the limiter settings.

Related system: [EU Energy System](../systems/eu_energy.md)
