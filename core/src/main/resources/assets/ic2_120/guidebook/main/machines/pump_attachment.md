---
navigation:
  title: Pump Attachment
  parent: index.md
  position: 52
  icon: ic2_120:bronze_pump_attachment
item_ids:
  - ic2_120:bronze_pump_attachment
  - ic2_120:carbon_pump_attachment
---

# Pump Attachment

<BlockImage id="ic2_120:bronze_pump_attachment" scale="4" />

Pump Attachments are directional pipe blocks that turn one face into a passive extractor. The front plate must touch the tank or machine to drain; the other sides connect back into the pipe network.

## Block View

| Bronze Pump Attachment | Carbon Pump Attachment |
|:----------------------:|:----------------------:|
| <BlockImage id="ic2_120:bronze_pump_attachment" scale="2" /> | <BlockImage id="ic2_120:carbon_pump_attachment" scale="2" /> |

## Comparison

| Type | Extraction Rate |
|------|-----------------|
| Bronze Pump Attachment | 2.4 B/s |
| Carbon Pump Attachment | 4.8 B/s |

## Usage

- Place the attachment so its front face points into the source storage. Its front face does not connect to normal pipes.
- The network treats the attachment as a provider only if the touched storage supports extraction.
- No EU is required.
- Right-click the attachment to open its filter screen. Click the ghost slot with a filled fluid container to lock the attachment to that fluid; right-click or click with an empty cursor to clear it.
- A pipe network stalls if multiple pump attachments provide different fluids at the same time. Use filters or separate pipe networks when moving several fluids.

## Network Tips

Use Pump Attachments for tank-to-tank logistics, for example pulling Biofuel from a Fermenter into generators or moving distilled water out of a Condenser. Use the powered Pump instead when the job is removing source blocks from the world.

## Recipe

| Bronze Pump Attachment | Carbon Pump Attachment |
|:----------------------:|:----------------------:|
| <Recipe id="ic2_120:bronze_pump_attachment" /> | <Recipe id="ic2_120:carbon_pump_attachment" /> |
