#!/usr/bin/env python3
"""Generate Minecraft 1.21.4+ item definitions for existing item models."""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CORE_TINTS = {
    "peat_ore": 0x4A3728,
    "rubber_leaves": 0xC4B848,
    "wooden_storage_box": 0xC6A669,
    "iron_storage_box": 0xB4B4B4,
    "bronze_storage_box": 0xCD7F32,
    "steel_storage_box": 0xD0D0D0,
    "iridium_storage_box": 0xFFFFFF,
    **{f"bronze_pipe_{size}": 0x854200 for size in ("tiny", "small", "medium", "large")},
    **{f"carbon_pipe_{size}": 0x191919 for size in ("tiny", "small", "medium", "large")},
    "bronze_pump_attachment": 0x854200,
    "carbon_pump_attachment": 0x191919,
}
SIDES = ("down", "up", "north", "south", "west", "east")
SPECIAL_ITEM_MODELS = {
    "fluid_cell", "empty_cell", "air_cell", "bio_cell", "water_cell", "lava_cell",
    "distilled_water_cell", "coolant_cell", "hot_coolant_cell", "uu_matter_cell",
    "weed_ex_cell", "pahoehoe_lava_cell", "biofuel_cell", "biomass_cell",
    "construction_foam_cell", "steam_cell", "superheated_steam_cell",
    "wood_transmission_shaft", "iron_transmission_shaft", "steel_transmission_shaft",
    "carbon_transmission_shaft", "bevel_gear",
}
FLUID_CELL_MODELS = {
    "fluid_cell", "empty_cell", "air_cell", "bio_cell", "water_cell", "lava_cell",
    "distilled_water_cell", "coolant_cell", "hot_coolant_cell", "uu_matter_cell",
    "weed_ex_cell", "pahoehoe_lava_cell", "biofuel_cell", "biomass_cell",
    "construction_foam_cell", "steam_cell", "superheated_steam_cell",
}


def fluid_cell_definition(base_model):
    def special(renderer):
        return {
            "type": "minecraft:special",
            "base": base_model,
            "model": {"type": renderer},
        }

    return {"model": {
        "type": "minecraft:select",
        "property": "minecraft:display_context",
        "cases": [{
            "when": [
                "firstperson_lefthand", "firstperson_righthand",
                "thirdperson_lefthand", "thirdperson_righthand",
            ],
            "model": special("ic2_120:fluid_cell_slosh"),
        }],
        "fallback": special("ic2_120:fluid_cell"),
    }}

for model_dir in ROOT.glob("*/src/main/resources/assets/*/models/item"):
    namespace = model_dir.parent.parent.name
    if namespace == "ic2":
        # Archived 1.20 resource namespace; current items use ic2_120.
        continue
    definition_dir = model_dir.parent.parent / "items"
    for model_file in model_dir.glob("*.json"):
        if namespace == "ic2_120" and model_file.stem in FLUID_CELL_MODELS:
            output = definition_dir / model_file.name
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_text(json.dumps(fluid_cell_definition(f"{namespace}:item/{model_file.stem}"), indent=2) + "\n")
            continue
        if namespace == "ic2_120" and model_file.stem in SPECIAL_ITEM_MODELS:
            continue
        if namespace == "ic2_120_advanced_solar_addon" and model_file.stem == "molecular_transformer":
            continue
        source = json.loads(model_file.read_text())
        output = definition_dir / model_file.name
        output.parent.mkdir(parents=True, exist_ok=True)
        model = f"{namespace}:item/{model_file.stem}"
        definition = {"model": {"type": "minecraft:model", "model": model}}
        overrides = source.get("overrides", [])
        predicates = {key for entry in overrides for key in entry.get("predicate", {})}
        if len(predicates) == 1:
            property_id = predicates.pop()
            definition["model"] = {
                "type": "minecraft:range_dispatch",
                "property": property_id,
                "entries": [
                    {
                        "threshold": entry["predicate"][property_id],
                        "model": {"type": "minecraft:model", "model": entry["model"]},
                    }
                    for entry in overrides
                ],
                "fallback": {"type": "minecraft:model", "model": model},
            }
        elif namespace == "ic2_120" and predicates == {f"ic2:{side}" for side in SIDES}:
            models_by_side = {
                next(iter(entry["predicate"])).split(":", 1)[1]: entry["model"]
                for entry in overrides
            }
            definition["model"] = {
                "type": "minecraft:range_dispatch",
                "property": "ic2_120:upgrade_direction",
                "entries": [
                    {
                        "threshold": index,
                        "model": {"type": "minecraft:model", "model": models_by_side[side]},
                    }
                    for index, side in enumerate(SIDES, 1)
                ],
                "fallback": {"type": "minecraft:model", "model": model},
            }
        if namespace == "ic2_120" and model_file.stem in CORE_TINTS:
            definition["model"]["tints"] = [
                {"type": "minecraft:constant", "value": CORE_TINTS[model_file.stem]}
            ]
        output.write_text(json.dumps(definition, indent=2) + "\n")
