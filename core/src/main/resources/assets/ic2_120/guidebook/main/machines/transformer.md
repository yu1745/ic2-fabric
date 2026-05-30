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

Transformers convert between voltage tiers. They are essential for safely connecting machines and storage of different voltage levels. Each transformer has one low-voltage face (marked with dots) and five high-voltage faces.

## Tier Comparison

| Tier | Name | Low Side | High Side |
|------|------|----------|-----------|
| 1 | LV Transformer | 32 EU/t | 128 EU/t |
| 2 | MV Transformer | 128 EU/t | 512 EU/t |
| 3 | HV Transformer | 512 EU/t | 2,048 EU/t |
| 4 | EV Transformer | 2,048 EU/t | 8,192 EU/t |

## Modes

### Step-Down (Default)
Power flows from the five high-voltage faces into the single marked face. The transformer converts high-voltage packets into four low-voltage packets. For example, an MV Transformer receiving 128 EU/t on its marked face will output 32 EU/t × 4 on the other five faces.

### Step-Up (Sneak + Wrench)
Sneak-right-click with a wrench to toggle to step-up mode. Power flows from the marked face out to the five surrounding faces. The transformer converts low-voltage packets into a single high-voltage packet. In this mode, 32 EU/t × 4 input on the five faces becomes 128 EU/t output on the marked face.

## Visual Indicators

- The face with a single dot is the **low-voltage** (marked) face.
- Redstone dust on the texture indicates the high-voltage faces.
- Face the marked face toward the side that should receive or provide the lower voltage.

## Recipes

<Recipe id="ic2_120:lv_transformer" />
<Recipe id="ic2_120:mv_transformer" />
<Recipe id="ic2_120:hv_transformer" />
<Recipe id="ic2_120:ev_transformer" />
