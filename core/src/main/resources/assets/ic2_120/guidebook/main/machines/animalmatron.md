---
navigation:
  title: Animal-Matron
  parent: index.md
  position: 62
  icon: ic2_120:animalmatron
item_ids:
  - ic2_120:animalmatron
---

# Animal-Matron

<BlockImage id="ic2_120:animalmatron" p:facing="north" scale="4" />

The Animal-Matron is an automated livestock caretaker. It tracks managed animals in a 4-block radius, feeds them on a schedule, grows babies into adults, breeds ready pairs, and can collect a few passive products.

## Energy and Storage

- **EU Storage**: 10,000 EU
- **Input**: 32 EU/t before transformer upgrades
- **Water Tank**: 8 buckets
- **Weed-Ex Tank**: 8 buckets
- **Energy drain**: 1 EU/t per currently managed animal
- **Scan interval**: once per second before overclockers

## Slots and Supplies

- **Water input/output**: accepts water buckets, distilled water buckets, water cells, distilled water cells, and matching universal fluid cells.
- **Weed-Ex input/output**: accepts Weed-Ex buckets, Weed-Ex cells, and matching universal fluid cells.
- **Feed slots**: five slots for the managed animals' foods.
- **Shears slot**: used for sheep wool collection.
- **Harvest output**: receives eggs and wool.
- **Upgrade slots**: overclocker, transformer, energy storage, and fluid pipe upgrades.

Managed animals are pigs, cows, mooshrooms, sheep, chickens, rabbits, horses, donkeys, mules, and llamas. Their accepted foods are carrot, wheat, wheat seeds, dandelion, golden apple, golden carrot, and hay block, depending on species.

## Care Rules

Each animal can receive up to 5 food items per Minecraft day, spaced across the day. Feeding consumes 100 mB of water when available. Babies become adults after 10 total feedings. Adults can become breeding-ready after 10 total feedings, but only if their daily 100 mB Weed-Ex cost has been paid. The machine will breed same-species ready adults until the local managed population reaches 32.

If the water tank is empty, animals in range slowly take care damage, but the machine will not reduce them below half health. Weed-Ex is not needed for growth, but without it animals will not be marked ready for automatic breeding.

## Extra Products

Chickens can contribute eggs about every 10 minutes. Sheep can be sheared about every 2 minutes if shears are installed; the sheep is marked sheared and the shears lose durability.

## Usage

Place the Animal-Matron beside or under a compact pen so the whole herd stays within 4 blocks. Keep water and Weed-Ex supplied, route eggs and wool out of the harvest slot, and use overclockers only if your feed and fluid supply can keep up with the faster scans.
