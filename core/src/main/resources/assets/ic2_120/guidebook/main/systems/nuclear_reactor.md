---
navigation:
  title: Nuclear Reactor System
  parent: index.md
  position: 219
  icon: minecraft:book
---

# Nuclear Reactor System

The nuclear reactor is IC2's advanced power source. It supports two operating modes:
- **Electric mode**: fuel rods produce heat → reactor temperature management → heat vents cool it down → EU is generated
- **Heat mode**: fuel rods produce double heat → reactor temperature management → heat vents heat coolant → HU is output

Both modes use identical component layouts; heat mode is detected automatically by the surrounding 5×5×5 shell.

---

## Multiblock Structure

The nuclear reactor is a multiblock:
- **Center**: the Nuclear Reactor block (`nuclear_reactor`)
- **Expansion**: each of the six faces (up, down, north, south, east, west) can touch 0 or 1 Reactor Chamber blocks (`reactor_chamber`)
- **Shell (heat mode)**: a 5×5×5 cube of casing around the reactor

### Component Capacity

| Adjacent Chambers | Effective Capacity |
|---:|---:|
| 0 | 27 slots |
| 1 | 36 slots |
| 2 | 45 slots |
| 3 | 54 slots |
| 4 | 63 slots |
| 5 | 72 slots |
| 6 | 81 slots |

---

## Power Generation

### Core Formula

```
triangular(n) = (n² + n) × 2
Every 1 output = 5 EU/t
Maximum output: 8192 EU/t (Tier 5)
```

### Uranium Fuel Rod Output

```
base pulse = 1 + count / 2
total pulse = base pulse × count × adjacency bonus
EU/t = total pulse × 100
```

| Fuel Rod Type | Base EU/t (single cell, no adjacency) |
|----------|---:|
| Single Uranium Fuel Rod | 100 EU/t |
| Dual Uranium Fuel Rod | 400 EU/t |
| Quad Uranium Fuel Rod | 1,200 EU/t |

Adjacent neutron reflectors and other fuel rods increase the pulse count significantly.

### MOX Fuel Rods

MOX output scales with reactor temperature:

| Reactor Temp | Efficiency | EU/t per Pulse (single) |
|---:|:---:|---:|
| 0 | 0% | 100 |
| 2,500 | 25% | 200 |
| 5,000 | 50% | 300 |
| 7,500 | 75% | 400 |
| 10,000 | 100% | 500 |

---

## Reactor Temperature and Safety

The reactor can buffer heat up to **10,000 HU**.

### Temperature Thresholds

| Reactor Temp | Environmental Effect |
|---:|---|
| &gt; 4,000 | Random blocks within 5×5×5 may catch fire |
| &gt; 5,000 | Random water within 5×5×5 may evaporate |
| &gt; 7,000 | Entities within 7×7×7 take radiation damage |
| &gt; 8,500 | Random blocks within 5×5×5 may turn to lava |
| ≥ 10,000 | **Nuclear reactor explosion** |

A Hazmat Suit makes you immune to the radiation damage above 7,000.

---

## Component Values

### Fuel Rods

| Item | Output Baseline | Heat | Durability |
|------|---:|---:|---:|
| Single Uranium Fuel Rod | 100 EU/t | 4 HU/pulse | 20,000 pulses |
| Dual Uranium Fuel Rod | 400 EU/t | 4 HU/pulse | 20,000 pulses |
| Quad Uranium Fuel Rod | 1,200 EU/t | 4 HU/pulse | 20,000 pulses |
| Single MOX Fuel Rod | 100~500 EU/t | 4 HU/pulse (doubles above 5,000) | 10,000 pulses |
| Dual MOX Fuel Rod | 400~2000 EU/t | same as above | 10,000 pulses |
| Quad MOX Fuel Rod | 1,200~6000 EU/t | same as above | 10,000 pulses |
| Depleted Fuel Rod | 0 | 0 | — |

### Heat Vents

Format: heat capacity / self-vent / reactor absorption

| Item | Heat Capacity | Self-Vent | Absorbs from Reactor |
|------|---:|---:|---:|
| Heat Vent | 1,000 | 6 | 0 |
| Reactor Heat Vent | 1,000 | 5 | 5 |
| Component Heat Vent | 0 | 0 | 0 |
| Advanced Heat Vent | 1,000 | 12 | 0 |
| Overclocked Heat Vent | 1,000 | 20 | 36 |

Component Heat Vent adjacency cooling is not counted as dissipated heat and does not produce coolant.

### Heat Exchangers

Format: heat capacity / adjacent exchange / reactor exchange

| Item | Heat Capacity | Adjacent Exchange | Reactor Exchange |
|------|---:|---:|---:|
| Heat Exchanger | 2,500 | 12 | 4 |
| Reactor Heat Exchanger | 5,000 | 0 | 72 |
| Component Heat Exchanger | 5,000 | 36 | 0 |
| Advanced Heat Exchanger | 10,000 | 24 | 8 |

Exchange rate scales with the temperature difference: ≥100% full speed, ≥75% half, ≥50% quarter, ≥25% eighth, &lt;25% minimum 1 HU.

### Neutron Reflectors

| Item | Pulse Capacity | Durability |
|------|---:|---|
| Neutron Reflector | 30,000 | Limited |
| Thick Neutron Reflector | 120,000 | Limited |
| Iridium Neutron Reflector | Infinite | Infinite |

### Coolant Cells

| Item | Heat Capacity |
|------|---:|
| Reactor Coolant Cell | 10,000 HU |
| Triple Reactor Coolant Cell | 30,000 HU |
| Sixfold Reactor Coolant Cell | 60,000 HU |

### Plating and Condensators

| Item | Heat Capacity Bonus | Explosion Modifier |
|------|---:|---:|
| Reactor Plating | +500 | ×0.9 |
| Containment Reactor Plating | +1,700 | ×0.99 |

| Item | Heat Capacity | Redstone Repair | Lapis Repair |
|------|---:|---:|---:|
| Redstone Condensator | 20,000 | 10k/use | — |
| Lapis Condensator | 100,000 | 10k/use | 40k/use |

---

## Heat Mode (Fluid-Cooled Reactor)

Heat mode is detected automatically by the surrounding 5×5×5 shell and requires at least **2 fluid ports**.

### Electric vs Heat Mode

| Property | Electric Mode | Heat Mode |
|------|------|------|
| Heat production | Normal | **×2** |
| Power output | Normal | **No power** |
| Heat vent self-vent | Lowers reactor temp | **Heats coolant** |
| Fluid needs | None | Coolant + hot coolant |
| Heat vent repair | Passive | **Requires coolant** |

### Coolant

- Tank capacity: 16,000 mB × 2 (input / output)
- Conversion: 20,000 HU → 1,000 mB hot coolant
- Dissipated-heat definition: only a heat vent's **self-vent** portion counts toward coolant conversion

### Heat Conservation

```
Fuel rod heat × 2
→ heat distribution
→ heat vent self-vent (dissipated heat)
→ heats coolant
→ unconverted heat returns to vents/reactor
```

---

## Constants

| Constant | Value |
|------|------|
| Max output | 8,192 EU/t (Tier 5) |
| Energy buffer | 1,000,000 EU |
| Reactor temperature cap | 10,000 HU |
| Coolant tank capacity | 16,000 mB |
| HU per bucket of coolant | 20,000 HU |

---

## Related Pages

- [Nuclear Blocks and Components](../reference/nuclear_components.md)
- [Nuclear Reactor](../machines/nuclear_reactor.md)
- [Fluids, Cells, and Buckets](../reference/fluids_cells.md)
