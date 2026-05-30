---
navigation:
  title: Radioisotope Thermoelectric Generator
  parent: index.md
  position: 16
  icon: ic2_120:rt_generator
item_ids:
  - ic2_120:rt_generator
---

# Radioisotope Thermoelectric Generator

<BlockImage id="ic2_120:rt_generator" p:facing="north" p:active="true" scale="4" />

The Radioisotope Thermoelectric Generator (RTG) produces EU from radioactive isotope fuel pellets. It has 6 fuel slots, each holding one RTG pellet that lasts forever without being consumed. Adding more pellets increases the power output exponentially.

This generator requires no fuel input once loaded, making it ideal for remote or long-term installations. The output scales with pellet count: 1 pellet produces 1 EU/t, while 6 pellets produce 32 EU/t.

## Output

- **EU Output**: 1-32 EU/t (depending on pellet count)
- **Energy Storage**: 20,000 EU
- **Tier**: 1

### Power by Pellet Count

| Pellets | EU/t |
|---------|------|
| 1 | 1 |
| 2 | 2 |
| 3 | 4 |
| 4 | 8 |
| 5 | 16 |
| 6 | 32 |

## Slots

- 6 fuel slots: RTG fuel pellets (infinite duration)

The RTG does not accept EU input. It outputs EU from every side except its front face.

## Recipe

<Recipe id="ic2_120:rt_generator" />
