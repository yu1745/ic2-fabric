---
navigation:
  title: Fluid Transport System
  parent: index.md
  position: 215
  icon: minecraft:book
---

# Fluid Transport System

IC2 has two separate fluid transport systems for different use cases:

- **Pipes and Pump Attachments**: long-distance transport, sending fluid from point A to point B through a pipe network
- **Machine Fluid Upgrades**: adjacent interaction; machines automatically exchange fluid with nearby containers without using pipes

---

## Pipes and Pump Attachments (Long-Distance Transport)

### Pipes

Pipes form the fluid transport path. There are 4 sizes × 2 materials = 8 pipe variants.

#### Sizes and Materials

| Size | Base Flow | Radius |
|------|-----------|--------|
| Tiny | 0.4 bucket/s | 2/16 block |
| Small | 0.8 bucket/s | 3/16 block |
| Medium | 2.4 bucket/s | 4/16 block |
| Large | 4.8 bucket/s | 5/16 block |

| Material | Multiplier | Color |
|----------|------------|-------|
| Bronze | ×1 | Copper-brown |
| Carbon Fibre | ×2 | Deep black |

**Effective flow = base flow × material multiplier**

#### All Pipes

| Item | Flow | Recipe (Core Material) |
|------|------|------------------------|
| Tiny Bronze Pipe | 0.4 bucket/s (400 mB/s) | Bronze Casing ×3 → 6 |
| Small Bronze Pipe | 0.8 bucket/s (800 mB/s) | Bronze Casing → 3 |
| Medium Bronze Pipe | 2.4 bucket/s (2400 mB/s) | Bronze Plate ×3 → 2 |
| Large Bronze Pipe | 4.8 bucket/s (4800 mB/s) | Bronze Plate → 1 |
| Tiny Carbon Fibre Pipe | 0.8 bucket/s (800 mB/s) | Carbon Fibre ×3 → 6 |
| Small Carbon Fibre Pipe | 1.6 bucket/s (1600 mB/s) | Carbon Fibre → 3 |
| Medium Carbon Fibre Pipe | 4.8 bucket/s (4800 mB/s) | Carbon Mesh ×3 → 2 |
| Large Carbon Fibre Pipe | 9.6 bucket/s (9600 mB/s) | Carbon Mesh → 1 |

#### Connections

- Pipes automatically connect to adjacent pipes, tanks, machines, etc. (any block that exposes a fluid API)
- **Wrench right-click on a face**: toggles the connection on that face. When disabled, that face connects to nothing
- **Wrench Shift + right-click**: toggles transparency mode, letting you see the fluid flowing inside

---

### Pump Attachment

A Pump Attachment is the **extraction end** of a pipe network. A pipe network **must** use a Pump Attachment to draw fluid from a tank or machine. Without a Pump Attachment, pipes are just empty conduits that pull nothing from anywhere.

| Item | Flow | Notes |
|------|------|-------|
| Bronze Pump Attachment | 0.4 bucket/s (400 mB/s) | Basic; matches a tiny bronze pipe in flow |
| Carbon Fibre Pump Attachment | 0.8 bucket/s (800 mB/s) | High-speed; matches a tiny carbon fibre pipe in flow |

#### Placement

Pump Attachments are directional and must be placed with the **front facing the target block**:

```
 Tank/Machine  ←  Pump Attachment  ←  Pipes
 (contains     front        back
  fluid)
```

- **Front** (face toward the target): only connects to non-pipe fluid containers (tanks, machines); does **not** connect to other pipes
- **Back and sides**: connect to other pipes, forming the transport path

#### Filtering

Right-click a Pump Attachment to open its GUI and configure a **filter sample**:

- Hold a fluid container (bucket, fluid cell, etc.) and click the filter slot → sets the filter; the attachment only draws that fluid
- Click the filter slot with an empty hand → clears the filter; the attachment draws any fluid

#### Common Uses

- **Drawing creosote from beneath a Coke Oven grate**: place the attachment directly below the grate, front facing up toward the grate, back facing the pipe that leads to a tank or a Semifluid Generator
- **Drawing water from a tank**: attach the pump to the side of a tank, pipe it to wherever water is needed
- **Drawing product from a machine**: attach the pump to the machine's fluid output face, pipe it into a tank

---

### Pipe Transport Mechanics

#### Workflow

1. **Find providers**: the network scans every Pump Attachment; the container on each attachment's front side is a fluid source
2. **Find receivers**: the network scans every non-pump connection face, looking for containers that can accept fluid
3. **Pathfinding**: find the nearest provider path for each receiver
4. **Transfer**: draw from the provider, follow the pipe path, inject into the receiver

All of this is built on Fabric's fluid API (`FluidStorage.SIDED`), so any third-party container that implements the API is supported.

#### Mixed-Fluid Protection

Within a single network and tick, only **one type of fluid** is transferred. If multiple Pump Attachments try to pull different fluids:

- the entire network halts
- the extra fluid sources must be removed to resume operation

---

## Machine Fluid Upgrades (Adjacent Interaction)

Independent from pipe networks. Once a machine has a **Fluid Ejector Upgrade** or **Fluid Inserter Upgrade** installed, it automatically exchanges fluid with adjacent containers every tick.

| Upgrade | Function |
|---------|----------|
| Fluid Ejector Upgrade | Machine automatically pushes its internal fluid into adjacent containers |
| Fluid Inserter Upgrade | Machine automatically pulls fluid from adjacent containers |

### Rate

Each upgrade boosts the rate (up to 4 upgrades):

| Upgrade Count | Per Tick | Per Second |
|---------------|----------|------------|
| 1 | 50 mB/t | 1000 mB/s |
| 2 | 200 mB/t | 4000 mB/s |
| 3 | 800 mB/t | 16000 mB/s |
| 4 | 3200 mB/t | 64000 mB/s |

### Configuration

- **Set filter**: hold the upgrade + a fluid container in the off-hand + right-click
- **Clear filter**: empty off-hand + right-click
- **Set direction**: sneak + right-click to cycle through directions

Direction cycle: Down → Up → North → South → West → East → All sides

---

## Jade HUD

With Jade installed, hovering over a pipe shows:

- **Flow rate**: current flow / max flow (mB/t) progress bar
- **Mixed-fluid halt**: halted state when fluid types clash
- **Pump Attachment**: label shows "Pump Attachment" and the filtered fluid type

---

## Related Pages

- [Fluids, Cells, and Buckets](../reference/fluids_cells.md)
- [Fluid Tank](../machines/tank.md)
- [Bronze Pipe](../machines/bronze_pipe.md)
- [Carbon Fibre Pipe](../machines/carbon_pipe.md)
- [Pump Attachment](../machines/pump_attachment.md)
