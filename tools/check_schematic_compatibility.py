#!/usr/bin/env python3
"""Compare schematic palettes with the blocks registered by Minecraft 1.19.2."""

from __future__ import annotations

import argparse
from collections import Counter
import json
import re
import zipfile
from pathlib import Path

from inspect_schematic import get_block_container, read_nbt, unwrap_schematic


def registered_types(sources_jar: Path) -> tuple[set[str], set[str]]:
    with zipfile.ZipFile(sources_jar) as archive:
        block_source = archive.read("net/minecraft/world/level/block/Blocks.java").decode("utf-8")
        block_entity_source = archive.read(
            "net/minecraft/world/level/block/entity/BlockEntityType.java"
        ).decode("utf-8")
    pattern = r'register\("([^"]+)"'
    return set(re.findall(pattern, block_source)), set(re.findall(pattern, block_entity_source))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("sources_jar", type=Path)
    parser.add_argument("schematics", nargs="+", type=Path)
    args = parser.parse_args()

    valid_blocks, valid_block_entities = registered_types(args.sources_jar)
    reports = []
    for path in args.schematics:
        schematic = unwrap_schematic(read_nbt(path))
        blocks = get_block_container(schematic)
        palette = blocks.get("Palette", schematic.get("Palette", {}))
        names = {
            state.partition("[")[0].removeprefix("minecraft:")
            for state in palette
        }
        block_entities = blocks.get("BlockEntities", schematic.get("BlockEntities", []))
        block_entity_ids = Counter(
            str(value.get("Id", value.get("id", "<missing>"))).removeprefix("minecraft:")
            for value in block_entities
            if isinstance(value, dict)
        )
        reports.append(
            {
                "file": str(path),
                "unsupported_in_1_19_2": [
                    "minecraft:" + name for name in sorted(names - valid_blocks)
                ],
                "unsupported_block_entities_in_1_19_2": {
                    "minecraft:" + name: count
                    for name, count in sorted(block_entity_ids.items())
                    if name not in valid_block_entities
                },
            }
        )
    print(json.dumps(reports, indent=2))


if __name__ == "__main__":
    main()
