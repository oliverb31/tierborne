#!/usr/bin/env python3
"""Convert a Sponge v2 schematic into vanilla 48-cube structure tiles.

The output is intended for local import QA. It deliberately excludes entities
and imported block-entity NBT so purchased map data cannot execute commands or
carry inventories into the test world.
"""

from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import re
import struct
from collections import defaultdict
from pathlib import Path
from typing import Any, BinaryIO

from inspect_schematic import decode_varints, get_block_container, read_nbt, unwrap_schematic


TAG_END = 0
TAG_BYTE = 1
TAG_INT = 3
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10

AIR_NAMES = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}
NAMESPACE_PATTERN = re.compile(r"[^a-z0-9_.-]+")


def write_string(stream: BinaryIO, value: str) -> None:
    encoded = value.encode("utf-8")
    stream.write(struct.pack(">H", len(encoded)))
    stream.write(encoded)


def write_named_header(stream: BinaryIO, tag_type: int, name: str) -> None:
    stream.write(struct.pack(">b", tag_type))
    write_string(stream, name)


def write_int(stream: BinaryIO, value: int) -> None:
    stream.write(struct.pack(">i", value))


def write_named_int(stream: BinaryIO, name: str, value: int) -> None:
    write_named_header(stream, TAG_INT, name)
    write_int(stream, value)


def write_named_string(stream: BinaryIO, name: str, value: str) -> None:
    write_named_header(stream, TAG_STRING, name)
    write_string(stream, value)


def write_int_list_payload(stream: BinaryIO, values: tuple[int, int, int]) -> None:
    stream.write(struct.pack(">b", TAG_INT))
    write_int(stream, len(values))
    for value in values:
        write_int(stream, value)


def split_state(state: str) -> tuple[str, dict[str, str]]:
    name, separator, raw_properties = state.partition("[")
    properties: dict[str, str] = {}
    if separator:
        for entry in raw_properties.removesuffix("]").split(","):
            key, value = entry.split("=", 1)
            properties[key] = value
    if name.endswith("_leaves") and "persistent" in properties:
        properties["persistent"] = "true"
    return name, properties


def write_state_compound(stream: BinaryIO, state: str) -> None:
    name, properties = split_state(state)
    write_named_string(stream, "Name", name)
    if properties:
        write_named_header(stream, TAG_COMPOUND, "Properties")
        for key, value in sorted(properties.items()):
            write_named_string(stream, key, value)
        stream.write(struct.pack(">b", TAG_END))
    stream.write(struct.pack(">b", TAG_END))


def write_block_compound(
    stream: BinaryIO,
    position: tuple[int, int, int],
    state_index: int,
) -> None:
    write_named_header(stream, TAG_LIST, "pos")
    write_int_list_payload(stream, position)
    write_named_int(stream, "state", state_index)
    stream.write(struct.pack(">b", TAG_END))


def write_structure(
    path: Path,
    data_version: int,
    size: tuple[int, int, int],
    palette: list[str],
    blocks: list[tuple[tuple[int, int, int], int]],
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(path, "wb", compresslevel=6) as stream:
        write_named_header(stream, TAG_COMPOUND, "")
        write_named_int(stream, "DataVersion", data_version)

        write_named_header(stream, TAG_LIST, "size")
        write_int_list_payload(stream, size)

        write_named_header(stream, TAG_LIST, "palette")
        stream.write(struct.pack(">b", TAG_COMPOUND))
        write_int(stream, len(palette))
        for state in palette:
            write_state_compound(stream, state)

        write_named_header(stream, TAG_LIST, "blocks")
        stream.write(struct.pack(">b", TAG_COMPOUND))
        write_int(stream, len(blocks))
        for position, state_index in blocks:
            write_block_compound(stream, position, state_index)

        write_named_header(stream, TAG_LIST, "entities")
        stream.write(struct.pack(">b", TAG_COMPOUND))
        write_int(stream, 0)
        stream.write(struct.pack(">b", TAG_END))


def safe_name(value: str) -> str:
    return NAMESPACE_PATTERN.sub("_", value.lower()).strip("_")


def convert(
    source: Path,
    datapack: Path,
    dungeon_name: str,
    tile_size: int,
    manifest_only: bool = False,
    entrance: tuple[float, float, float] | None = None,
    entrance_yaw: float = 0.0,
    entrance_pitch: float = 0.0,
) -> dict[str, Any]:
    schematic = unwrap_schematic(read_nbt(source))
    blocks_container = get_block_container(schematic)
    width = int(schematic["Width"])
    height = int(schematic["Height"])
    length = int(schematic["Length"])
    expected_blocks = width * height * length
    data_version = int(schematic.get("DataVersion", 3105))
    palette_source = blocks_container.get("Palette", schematic.get("Palette"))
    block_data = blocks_container.get(
        "Data", blocks_container.get("BlockData", schematic.get("BlockData"))
    )
    if not isinstance(palette_source, dict) or not isinstance(block_data, bytes):
        raise ValueError("Unsupported schematic palette/data layout")

    id_to_state = {int(value): str(state) for state, value in palette_source.items()}
    tiles: dict[tuple[int, int, int], list[tuple[tuple[int, int, int], str]]] = defaultdict(list)
    layer_size = width * length
    decoded_count = 0
    for index, palette_id in enumerate(decode_varints(block_data)):
        decoded_count += 1
        state = id_to_state[palette_id]
        block_name = state.partition("[")[0]
        if block_name in AIR_NAMES or block_name in {
            "minecraft:nether_portal",
            "minecraft:end_portal",
            "minecraft:end_gateway",
        }:
            continue
        y, within_layer = divmod(index, layer_size)
        z, x = divmod(within_layer, width)
        tile = (x // tile_size, y // tile_size, z // tile_size)
        local = (x % tile_size, y % tile_size, z % tile_size)
        tiles[tile].append((local, state))
    if decoded_count != expected_blocks:
        raise ValueError(f"Decoded {decoded_count} blocks; expected {expected_blocks}")

    namespace = "tierborne_dungeon_qa"
    structure_root = datapack / "data" / namespace / "structures" / dungeon_name
    function_root = datapack / "data" / namespace / "functions"
    function_root.mkdir(parents=True, exist_ok=True)
    commands = [
        "# Generated local QA import. Purchased assets are not shipped with Tierborne.",
        f"say Placing {dungeon_name} in {len(tiles)} safe vanilla structure tiles...",
    ]
    total_blocks = 0
    tile_manifest = []
    for tile, tile_blocks in sorted(tiles.items()):
        tile_x, tile_y, tile_z = tile
        used_states = sorted({state for _, state in tile_blocks})
        state_indexes = {state: index for index, state in enumerate(used_states)}
        encoded_blocks = [
            (position, state_indexes[state]) for position, state in tile_blocks
        ]
        size = (
            min(tile_size, width - tile_x * tile_size),
            min(tile_size, height - tile_y * tile_size),
            min(tile_size, length - tile_z * tile_size),
        )
        tile_name = f"tile_{tile_x}_{tile_y}_{tile_z}"
        structure_path = structure_root / f"{tile_name}.nbt"
        if manifest_only:
            if not structure_path.is_file():
                raise ValueError(f"Existing tile is missing: {structure_path}")
        else:
            write_structure(
                structure_path,
                data_version,
                size,
                used_states,
                encoded_blocks,
            )
        commands.append(
            f"place template {namespace}:{dungeon_name}/{tile_name} "
            f"~{tile_x * tile_size} ~{tile_y * tile_size} ~{tile_z * tile_size}"
        )
        total_blocks += len(tile_blocks)
        tile_manifest.append(
            {
                "x": tile_x,
                "y": tile_y,
                "z": tile_z,
                "size": list(size),
                "blocks": len(tile_blocks),
                "structure": f"{namespace}:{dungeon_name}/{tile_name}",
            }
        )
    commands.append(f"say Finished placing {dungeon_name}: {total_blocks} blocks.")
    if not manifest_only:
        (function_root / f"place_{dungeon_name}.mcfunction").write_text(
            "\n".join(commands) + "\n", encoding="utf-8"
        )
    datapack.mkdir(parents=True, exist_ok=True)
    (datapack / "pack.mcmeta").write_text(
        json.dumps(
            {
                "pack": {
                    "pack_format": 10,
                    "description": "Tierborne local dungeon import QA",
                }
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    manifest = {
        "format": 1,
        "name": dungeon_name,
        "namespace": namespace,
        "source_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
        "source_dimensions": [width, height, length],
        "decoded_blocks": decoded_count,
        "manifest_only": manifest_only,
        "non_air_blocks": total_blocks,
        "tile_size": tile_size,
        "tiles": tile_manifest,
        "sanitization": {
            "block_entities_removed": len(blocks_container.get("BlockEntities", [])),
            "entities_removed": len(schematic.get("Entities", [])),
            "portal_blocks_removed": True,
            "leaf_decay_disabled": True,
        },
    }
    if entrance is not None:
        if not (0.0 <= entrance[0] < width and 0.0 <= entrance[1] < height
                and 0.0 <= entrance[2] < length):
            raise ValueError("Entrance must be inside the source dimensions")
        manifest["entrance"] = {
            "position": list(entrance),
            "yaw": entrance_yaw,
            "pitch": entrance_pitch,
        }
    manifest_path = datapack / "data" / namespace / "dungeons" / f"{dungeon_name}.json"
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    return {
        "source": str(source),
        "datapack": str(datapack),
        "function": f"{namespace}:place_{dungeon_name}",
        "tiles": len(tiles),
        "blocks": total_blocks,
        "block_entities_removed": len(blocks_container.get("BlockEntities", [])),
        "entities_removed": len(schematic.get("Entities", [])),
        "portal_blocks_removed": True,
        "leaf_decay_disabled": True,
        "manifest": str(manifest_path),
        "source_sha256": manifest["source_sha256"],
        "decoded_blocks": decoded_count,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("datapack", type=Path)
    parser.add_argument("--name")
    parser.add_argument("--tile-size", type=int, default=48)
    parser.add_argument(
        "--manifest-only",
        action="store_true",
        help="Write runtime metadata while requiring all previously converted tiles to exist.",
    )
    parser.add_argument(
        "--entrance",
        type=float,
        nargs=3,
        metavar=("X", "Y", "Z"),
        help="Optional dungeon-local player entrance position.",
    )
    parser.add_argument("--entrance-yaw", type=float, default=0.0)
    parser.add_argument("--entrance-pitch", type=float, default=0.0)
    args = parser.parse_args()
    if not 1 <= args.tile_size <= 48:
        parser.error("--tile-size must be between 1 and 48")
    name = safe_name(args.name or args.source.stem)
    print(json.dumps(convert(
        args.source,
        args.datapack,
        name,
        args.tile_size,
        args.manifest_only,
        tuple(args.entrance) if args.entrance is not None else None,
        args.entrance_yaw,
        args.entrance_pitch,
    ), indent=2))


if __name__ == "__main__":
    main()
