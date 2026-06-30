---
navigation:
  title: Power Generation and Storage
  parent: index.md
  position: 202
  icon: minecraft:book
item_ids:
  - minecraft:coal
  - minecraft:charcoal
  - minecraft:lava_bucket
  - ic2_120:coal_fuel_dust
  - ic2_120:plant_ball
  - ic2_120:bio_chaff
  - ic2_120:bio_cell
---

# Power Generation and Storage

<BlockImage id="ic2_120:generator" scale="4" />

## Generators

### Generator

- **Buffer**: 4,000 EU
- **Generation rate**: 10 EU/t
- **Fuel value**: 1 coal = 4,000 EU
- **Wiring**: front face cannot output; all other faces accept cables

### Stirling Generator

- **Generation rate**: 10 EU/t
- **Fuels**: coal, charcoal, lava bucket (1 bucket = 20,000 EU), etc.

### Geothermal Generator

- **Buffer**: 4,000 EU
- **Generation rate**: 10 EU/t
- **Fluid use**: 1 bucket (1000 mB) of lava = 20,000 EU
- **Output time**: about 33 minutes (2000 ticks at 10 EU/t)

### Solar Generator

- **Generation rate**: 1 EU/t on a clear day (daytime)
- **No output**: at night, in overcast or rainy weather
- **Requires open sky**: nothing may block the sky above it

### Water Kinetic Generator

- Converts the kinetic energy of water into KU, which a Kinetic Generator then turns into EU
- Requires flowing water
- See [Water Mill](../machines/water_generator.md) for details

### Wind Kinetic Generator

See [Kinetic System](../systems/kinetic_transmission.md)

- **Kinetic output**: varies with altitude and weather
- **Carbon rotor baseline**: y=150 + clear weather + gustFactor=1.0 → 512 KU/t = 128 EU/t
- **Max KU/t**: 2048 KU/t = 512 EU/t
- See [Wind Kinetic Generator](../machines/wind_kinetic_generator.md) for details

### Semifluid Generator

- **Fuels**: two families — refined fuel-oil and crude-oil / creosote
- **EU per mB**: refined fuel-oil is higher; crude-oil / creosote is lower
- See [Semifluid Generator](../machines/semifluid_generator.md) for details

### Steam Kinetic Generator

- Converts steam into kinetic energy (KU), which a Kinetic Generator then turns into EU
- See [Steam Kinetic Generator](../machines/steam_kinetic_generator.md) for details

### RT Generator (Radioisotope Thermoelectric Generator)

- **Heat-driven**: receives HU and converts it to EU
- **1 HU = 1 EU**
- See [RT Generator](../machines/rt_generator.md) for details

### Creative Generator

- **Infinite EU/t**: creative mode only

---

## Storage Devices

| Block | Voltage Tier | Capacity | Max Throughput per Side |
|---|---:|---:|---:|
| BatBox | Tier 1 (LV) | 40,000 EU | 32 EU/t |
| CESU | Tier 2 (MV) | 300,000 EU | 128 EU/t |
| MFE | Tier 3 (HV) | 4,000,000 EU | 512 EU/t |
| MFSU | Tier 4 (EV) | 40,000,000 EU | 2048 EU/t |

### Energy Storage Wiring Rules

- **BatBox**: front face input only; all other faces are output
- **CESU**: front face input only; all other faces are output
- **MFE**: front face input only; all other faces are output
- **MFSU**: front face input only; all other faces are output

### Charging Pad

- **Same capacity and tolerance**: matches the corresponding energy storage
- **Feature**: recharges chargeable items in a player's inventory while they stand on top

### Transformer

- **Function**: receives grid energy and raises the output voltage tier
- **After upgrading**: can output more energy to an upstream high-voltage grid

---

## Power and Storage Math

### Fuel Energy Table

| Fuel | EU Output |
|------|---:|
| 1 coal | 4,000 EU |
| 1 charcoal | 4,000 EU |
| 1 lava bucket | 20,000 EU |
| 1 mB biogas | about 60 EU |

### Wind Generator Altitude Model

- **Altitude formula**: Gaussian, mu=150, sigma=35
- **Clear weather factor**: 1.0
- **Rain factor**: 1.2
- **Thunderstorm factor**: 1.5
- **Gust factor**: 0.5 ~ 1.5 (updates every 10 seconds)

### Rotor Durability (Clear-Weather Baseline)

| Rotor Material | Durability |
|------|------|
| Wood | 3 hours |
| Iron | 24 hours |
| Steel | 48 hours |
| Carbon | 168 hours |
