---
navigation:
  title: Pattern Storage
  parent: index.md
  position: 71
  icon: ic2_120:pattern_storage
item_ids:
  - ic2_120:pattern_storage
---

# Pattern Storage

<BlockImage id="ic2_120:pattern_storage" p:facing="north" scale="4" />

The Pattern Storage stores scanned UU-Matter patterns and acts as a bridge between the Pattern Scanner and the Replicator. It must be placed adjacent to both the Scanner and Replicator to transfer patterns between them.

## Operation

Pattern Storage holds a list of scanned templates and one selected template. A Pattern Scanner can save a completed scan into it, and a Replicator reads the selected template from a unique adjacent Pattern Storage.

Adding a template that already exists updates that entry and selects it. Removing the selected template shifts selection to a valid remaining entry, or clears selection if the list becomes empty.

## Crystal Memory

The single item slot accepts Crystal Memory. The storage can import the template stored in a crystal or export its selected template into a crystal. This is useful for moving templates between bases without moving the machine.

## Placement Rules

- Put the Pattern Storage adjacent to the Pattern Scanner when saving scans.
- Put it adjacent to the Replicator when replicating.
- Avoid placing multiple Pattern Storage blocks adjacent to the same Scanner or Replicator; those machines look for a unique adjacent storage and stop when the choice is ambiguous.

The Pattern Storage does not use EU. Its only automation surface is the Crystal Memory slot, which accepts and extracts Crystal Memory items.
