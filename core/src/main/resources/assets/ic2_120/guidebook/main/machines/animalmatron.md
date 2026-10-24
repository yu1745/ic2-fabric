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
- **Energy drain**: 2 EU/t while animals are being managed (each Overclocker Upgrade multiplies the drain by 1.6)
- **Scan interval**: once per second before overclockers

## Slots and Supplies

- **Water input/output**: accepts water buckets, distilled water buckets, water cells, distilled water cells, and matching universal fluid cells.
- **Weed-Ex input/output**: accepts Weed-Ex buckets, Weed-Ex cells, and matching universal fluid cells. Note: Weed-Ex currently has no effect in this machine — it can be filled but is never consumed. It is kept only for tank compatibility.
- **Feed slots**: five slots for the managed animals' foods.
- **Shears slot**: used for sheep wool collection.
- **Harvest output**: receives eggs and wool.
- **Upgrade slots**: overclocker, transformer, energy storage, fluid pipe, Ejector, and Pulling upgrades.

Managed animals are pigs, cows, mooshrooms, sheep, chickens, rabbits, horses, donkeys, mules, and llamas. Their accepted foods are carrot, wheat, wheat seeds, dandelion, golden apple, golden carrot, and hay block, depending on species.

## Care Rules

Each animal can receive up to 5 food items per Minecraft day, spaced across the day. Feeding consumes 100 mB of water when available. Babies become adults after 10 total feedings. Adults become breeding-ready after 10 total feedings (Weed-Ex is not required). The machine will breed same-species ready adults until the local managed population reaches 32.

If the water tank is empty, animals in range slowly take care damage, but the machine will not reduce them below half health.

## Extra Products

Chickens can contribute eggs about every 10 minutes. Sheep can be sheared about every 2 minutes if shears are installed; the sheep is marked sheared and the shears lose durability.

## Usage

Place the Animal-Matron beside or under a compact pen so the whole herd stays within 4 blocks. Pulling upgrades can pull water containers, feed, and shears into their work slots. Ejector upgrades can push water-container returns, eggs, and wool from the work output slots. Keep water supplied, and use overclockers only if your feed and fluid supply can keep up with the faster scans.
