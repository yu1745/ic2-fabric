# IC2-120

# IC2-120

<big>[English](README_EN.md) | [简体中文](README.md)</big>

> **⚠️ Required: Installation** To use this mod, you must copy **all three files** from the release to your Minecraft mods folder:
> - `ic2_120-*.jar` - Main mod file
> - `energy-*.jar` - Energy API runtime dependency
> - `fabric-language-kotlin-*.jar` - Fabric Kotlin language support library
> All three files are required, otherwise the mod will not work.

> **📢 Sinytra Connector Support**: This mod has been adapted for [Sinytra Connector](https://github.com/Sinytra/connector) and can also run on Forge via Connector. For instructions on using Connector to load Fabric Mods in a Forge environment, please refer to the [official Sinytra Connector documentation](https://github.com/Sinytra/connector)。

IndustrialCraft 2 Minecraft 1.20.1 Fabric port, written in Kotlin.

**Player Guide**: For gameplay and features, see [mod-features.md](mod特性.md) (Chinese). This document is for developers.

## Project Features

- 📦 Based on Fabric Loader and Fabric API
- 🔧 Developed with Kotlin 2.3.10
- ⚡ Class-level annotation registration system for streamlined mod development
- 🎨 Custom ComposeUI declarative GUI system
- 🔌 EU energy network system
- 🏭 Complete industrial machine suite (generators, processing machines, storage devices, etc.)

## Development Environment Requirements

- JDK 17 or higher
- Minecraft 1.20.1
- Fabric Loader
- Fabric API

## Build Commands

**Important**: Do not add the `--no-daemon` parameter to any Gradle commands to leverage the Gradle Daemon for faster builds.

```bash
# Build the mod
./gradlew build

# Clean build
./gradlew clean build

# Run client
./gradlew runClient

# Windows Chinese character fix: use runClient.bat (sets console to UTF-8 before launching)
./runClient.bat

# Run server
./gradlew runServer

# Generate sources
./gradlew genSources

# Generate data
./gradlew runDatagen
```

## Tech Stack

- **Kotlin** 2.3.10 - Primary development language
- **Fabric Loom** - Gradle build plugin
- **Fabric API** - Minecraft mod API
- **ComposeUI** - Custom declarative GUI system

## Project Structure

```
ic2-fabric/
├── src/
│   ├── main/kotlin/     # Common/server-side code
│   ├── client/kotlin/   # Client-side code
│   ├── main/java/       # Common/server-side Mixin classes
│   └── client/java/     # Client Mixin classes
├── docs/                # Technical documentation
└── assets/              # Mod resources (models, textures, lang files, etc.)
```

## Documentation

Full index and categories: [docs/README.md](docs/README.md). Common entries:

- [Class-based Annotation Registration System](docs/registry/CLASS_BASED_REGISTRY.md) - Automatic registration using annotations and enums
- [Synchronization System](docs/systems/sync-system.md) - Client/server property synchronization
- [Energy Flow Synchronization](docs/systems/energy-flow-sync.md) - Energy flow and sync logic between machines
- [Energy Network System](docs/systems/energy-network.md) - EU energy transmission and storage
- [Upgrade System](docs/systems/upgrade-system.md) - Machine upgrades and effect mechanics
- [Slot Specification System](docs/ui/slot-spec-system.md) - Machine GUI slot definitions and constraints
- [Machine Composition Reuse](docs/guides/machine-composition-reuse.md) - Machine logic composition and reusable design
- [Machine Implementation Guide](docs/guides/machine-implementation-guide.md) - Complete Block → BlockEntity → ScreenHandler → Screen workflow
- [Nuclear Power System](docs/systems/nuclear-power.md) - Nuclear-related mechanics and implementation
- [Heat System](docs/systems/heat-system.md) - HU heating and heat transfer mechanics (Chinese)
- [Fluid System](docs/systems/fluid-system.md) - Fluid pipes, pump attachments, and transmission rules (Chinese)
- [Transmission Shaft System](docs/archive/transmission_shaft.md) - Mechanical shafts and bevel gears (Chinese)
- [Implemented Items List](docs/guides/item-implemented.md) - List of currently implemented items
- [ComposeUI Declarative GUI](docs/ui/compose-ui.md) - GUI layout and rendering system
- [DrawContext Rendering Methods Reference](docs/ui/drawcontext-methods.md) - Rendering API documentation
- [Assets Inventory](docs/inventory/assets-inventory.md) - Mod block/item resource inventory
- [Biome Colored Blocks](docs/registry/biome-colored-blocks.md) - Implementing biome-color-changing blocks
- [Block Variants System](docs/registry/block-variants.md) - Block states and model variants
- [Unique Gift Item Anti-Duplication TODO](docs/archive/unique-gift-item-anti-dup-todo.md) - Anti-duplication draft and todos

## Unimplemented Features (Compared to Original IC2)

Compared to the original IC2, the following features are not yet implemented or are under development:

### 🔧 Kinetic Power Generation System (In Development)
- **Status**: Basic framework completed, mechanical transmission logic pending
- **Completed**:
  - Transmission shaft blocks (wood, iron, steel, carbon fiber)
  - Bevel gear block (90-degree direction change)
  - Visual rendering (BER)
- **To Be Implemented**:
  - Mechanical kinetic energy transmission system (speed/torque calculation)
  - Generator kinetic-to-electric energy conversion logic
  - Related crafting recipes

### 🏔️ Terraforming Series Machines
- Terraformer
- Various terraforming templates (cultivation, forestation, desertification, mushroom, etc.)
- Construction templates

### 💨 Steam Series Machines
- Steam Generator
- Steam Kinetic Generator
- Steam Repressurizer
- Steam-related fluids and recipes

### 📦 Logistics Series Machines
- Item Buffer
- Weighted Item Distributor
- Sorting Machine
- Logistics pipes and filters

### 🔥 Blast Furnace Multiblock Structure
- Blast Furnace multiblock structure
- Refractory bricks
- High-temperature smelting logic
- Steel production system

> **Note**: The above features will be implemented gradually. For specific progress, please check project Issues and Pull Requests.

## Contributing

Issues and Pull Requests are welcome!

## Copyright & License

**⚠️ Important Notice**

This project is a reverse engineering project based on IndustrialCraft 2 (IC2). The original IC2 mod is **not open-source software**, and its source code and assets are not officially authorized for public use.

The assets in this repository under `src/main/resources/assets/ic2` and `src/main/resources/assets/minecraft` directories (including but not limited to models, textures, language files, recipes, and related data) were organized through reverse engineering and are intended for compatibility research and technical verification only. This does not imply any authorization from the original IC2 project, Minecraft project, or related rights holders.

This project is for **learning and research purposes only** and must not be used for commercial purposes. If you are a copyright holder of IC2 and believe this project infringes on your rights, please contact us.

Except as otherwise required by law, the authors and contributors of this repository assume no liability for any direct or indirect legal consequences resulting from the use, distribution, modification, or redistribution of this project; users should verify the legality of their actions in their jurisdiction and assume all risks.

## Related Links

- [Fabric Official Documentation](https://fabricmc.net/wiki/)
- [Fabric API GitHub](https://github.com/FabricMC/fabric)
- [Minecraft 1.20.1 Version](https://www.minecraft.net/)
