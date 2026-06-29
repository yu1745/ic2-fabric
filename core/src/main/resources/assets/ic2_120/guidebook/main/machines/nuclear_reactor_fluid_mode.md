---
navigation:
  title: Fluid-Cooled Reactor
  parent: index.md
  position: 60
  icon: ic2_120:reactor_vessel
item_ids:
  - ic2_120:reactor_vessel
  - ic2_120:reactor_fluid_port
  - ic2_120:reactor_access_hatch
  - ic2_120:reactor_redstone_port
---

# Fluid-Cooled Reactor (Fluid Mode)

<BlockImage id="ic2_120:reactor_vessel" scale="4" />

The Fluid-Cooled Reactor is the **Fluid Mode** of the Nuclear Reactor. Instead of generating EU directly, it converts heat into **hot coolant (HU)**, which a [Liquid Heat Exchanger](liquid_heat_exchanger.md) can turn back into EU or other energy. Heat production itself is identical to Electric Mode — only MOX fuel rods double their heat output when reactor temperature exceeds 50% of max heat.

This page covers the four blocks that define and operate the Fluid Mode shell. For the dry **Electric Mode**, see [Nuclear Reactor](nuclear_reactor.md).

## Fluid Mode Detection

Fluid Mode is detected **automatically** when the Nuclear Reactor is fully enclosed by a **5×5×5 shell of Reactor Pressure Vessels** centered on the reactor, and at least **2 Reactor Fluid Ports** are present. No redstone or GUI toggle is needed — the shell itself switches the mode.

A complete Fluid Mode setup:

<GameScene zoom="3" interactive={true} fullWidth={true}>
  <IsometricCamera yaw="45" pitch="30" />
  <ImportStructure src="nuclear_reactor_fluid_mode.snbt" />
</GameScene>

A minimal working unit — one reactor shell, Fluid Ports feeding a single [Liquid Heat Exchanger](liquid_heat_exchanger.md) loop, and a [Stirling Generator](stirling_generator.md) to turn the HU back into EU:

<GameScene zoom="3" interactive={true} fullWidth={true}>
  <IsometricCamera yaw="45" pitch="30" />
  <ImportStructure src="nuclear_reactor_fluid_minimal.snbt" />
</GameScene>

**Rendering note:** The preview above does not store pipe/machine connection states, so pipes may appear to **float** and not touch the machines. In-game, pipes connect to adjacent machines and reactor ports normally — the floating look is just a preview limitation, not a real disconnection.

### ⚠ Match Exchanger Count to Your Heat Output

A single Liquid Heat Exchanger outputs at most **100 HU/t** (10 Heat Conductors × 10 HU/t each), converting **1,000 mB hot coolant → 20,000 HU**. This is the per-machine ceiling, but you can run **multiple exchangers in parallel** — each one adds another 100 HU/t of capacity. Two exchangers handle up to **200 HU/t**, three up to **300 HU/t**, and so on.

The danger is when your reactor's heat production **exceeds the total processing capacity of your exchanger bank**. The exchangers then convert hot coolant back into cold coolant **slower than the reactor heats it up**, so:

1. **Hot coolant backs up** in the output tank and pipes — the reactor's Fluid Port cannot push more out.
2. **Cold coolant is produced too slowly** — the return line can't supply enough cold coolant to keep up with the reactor's heat output.
3. The reactor **cannot dissipate heat** (not enough cold coolant to heat up), so its temperature climbs.
4. Past **10,000 HU** the reactor **explodes**.

So size your exchanger count to your fuel-rod load: count the exchangers you need so that their combined capacity (×100 HU/t each) covers the reactor's heat production.

## Electric vs Fluid Mode

| Property | Electric Mode | Fluid Mode |
|------|------|------|
| Heat production | Normal | Normal (only MOX doubles above 50% heat) |
| Power output | Normal | **No power** (outputs HU instead) |
| Heat vent self-vent | Lowers reactor temp | **Heats coolant** |
| Fluid needs | None | Coolant + hot coolant |
| Heat vent repair | Passive | **Requires coolant** |

## Shell Blocks

### Reactor Pressure Vessel

The casing material that builds the 5×5×5 shell. It is purely structural — it does not conduct power or fluid on its own. The shell must be **complete and unbroken** for the reactor to enter Fluid Mode; any gap disables Fluid Mode and reverts the reactor to Electric Mode behavior.

### Reactor Fluid Port

Circulates coolant. Each port has a **16,000 mB** internal tank. At least **2 ports** are required: one to accept **coolant** in, one to eject **hot coolant** out. Connect them with pipes to a [Liquid Heat Exchanger](liquid_heat_exchanger.md) to recover the heat as HU.

- Conversion: **20,000 HU → 1,000 mB hot coolant**
- Only a heat vent's **self-vent** portion counts toward coolant conversion.

### Reactor Access Hatch

In Fluid Mode the Nuclear Reactor core is fully wrapped by the Pressure Vessel shell, so you **cannot right-click the reactor directly** to open its GUI. The Access Hatch is the only way to open the reactor interface — right-click the hatch instead. It also provides automated item I/O for inserting and extracting fuel rods, depleted cells, and other internal components. Can be placed on any face of the shell.

### Reactor Redstone Port

In Electric Mode redstone is accepted by **any block of the reactor structure**, but in Fluid Mode the Pressure Vessel shell blocks this — **only the Redstone Port can receive redstone signals**. This is a fundamental difference between the two modes. Wire a lever, button, or redstone circuit to a port on the shell for automated control such as **emergency shutdown** or pulse-based duty cycling.

## Heat Conservation

```
Fuel rod heat (MOX rod heat ×2 when reactor temp > 50%)
→ heat distribution
→ heat vent self-vent (dissipated heat)
→ heats coolant
→ unconverted heat returns to vents/reactor
```

## Constants

| Constant | Value |
|------|------|
| Coolant tank capacity | 16,000 mB |
| HU per bucket of coolant | 20,000 HU |
| Shell requirement | complete 5×5×5 of Pressure Vessels |
| Min fluid ports for Fluid Mode | 2 |

---

## Related Pages

- [Nuclear Reactor (Electric Mode)](nuclear_reactor.md)
- [Nuclear Blocks and Components](../reference/nuclear_components.md)
- [Liquid Heat Exchanger](liquid_heat_exchanger.md)
