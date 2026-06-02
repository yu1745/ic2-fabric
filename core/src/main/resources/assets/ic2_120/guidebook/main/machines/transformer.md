---
navigation:
  title: Transformer
  parent: index.md
  position: 48
  icon: ic2_120:lv_transformer
item_ids:
  - ic2_120:lv_transformer
  - ic2_120:mv_transformer
  - ic2_120:hv_transformer
  - ic2_120:ev_transformer
---

# Transformer

<BlockImage id="ic2_120:lv_transformer" scale="4" />

Transformers convert between adjacent EU voltage tiers without changing the total EU. The front face is the high-tier side; the other five faces are the low-tier side. The GUI switches between step-up and step-down modes.

## Tier Comparison

| Tier | Name | Low Side | High Side |
|------|------|----------|-----------|
| 1 | LV Transformer | 32 EU/t | 128 EU/t |
| 2 | MV Transformer | 128 EU/t | 512 EU/t |
| 3 | HV Transformer | 512 EU/t | 2,048 EU/t |
| 4 | EV Transformer | 2,048 EU/t | 8,192 EU/t |

## Modes

### Step-Up
The five non-front faces accept low-tier EU and the front face outputs the next tier. The transformer waits until it has enough stored EU to emit a full high-tier tick, reducing lossy trickle output.

### Step-Down
The front face accepts high-tier EU and the other five faces output the lower tier immediately. Use this to feed lower-tier machines or cables from a higher-tier storage or generator line.

## Facing and Buffer

- **Front face:** high-tier side.
- **Other faces:** low-tier side.
- **Mode switch:** use the transformer GUI.
- **Internal buffer:** LV 512 EU, MV 2,048 EU, HV 8,192 EU, EV 32,768 EU.

Place transformers between mismatched cable and machine tiers. For example, put an LV Transformer in step-down mode between a CESU/MV line and LV machines, with the front face toward the MV side.

## Recipes

<Recipe id="ic2_120:lv_transformer" />
<Recipe id="ic2_120:mv_transformer" />
<Recipe id="ic2_120:hv_transformer" />
<Recipe id="ic2_120:ev_transformer" />
