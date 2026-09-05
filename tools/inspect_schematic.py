#!/usr/bin/env python3
"""Inspect gzip-compressed Sponge schematic files without external packages."""

from __future__ import annotations

import argparse
import gzip
import json
import struct
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from collections.abc import Iterator
from typing import Any, BinaryIO


TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12


class NbtError(ValueError):
    pass


@dataclass(frozen=True)
class Dimensions:
    width: int
    height: int
    length: int

    @property
    def volume(self) -> int:
        return self.width * self.height * self.length


def read_exact(stream: BinaryIO, size: int) -> bytes:
    value = stream.read(size)
    if len(value) != size:
        raise NbtError(f"Unexpected end of NBT data: wanted {size} bytes")
    return value


def read_number(stream: BinaryIO, fmt: str) -> Any:
    return struct.unpack(">" + fmt, read_exact(stream, struct.calcsize(fmt)))[0]


def read_string(stream: BinaryIO) -> str:
    length = read_number(stream, "H")
    # Some WorldEdit exports contain legacy section-sign text encoded as a raw
    # single byte. Preserve the record boundary and replace only malformed text;
    # block-state palette keys remain ordinary UTF-8.
    return read_exact(stream, length).decode("utf-8", errors="replace")


def read_payload(stream: BinaryIO, tag_type: int) -> Any:
    if tag_type == TAG_BYTE:
        return read_number(stream, "b")
    if tag_type == TAG_SHORT:
        return read_number(stream, "h")
    if tag_type == TAG_INT:
        return read_number(stream, "i")
    if tag_type == TAG_LONG:
        return read_number(stream, "q")
    if tag_type == TAG_FLOAT:
        return read_number(stream, "f")
    if tag_type == TAG_DOUBLE:
        return read_number(stream, "d")
    if tag_type == TAG_BYTE_ARRAY:
        length = read_number(stream, "i")
        if length < 0:
            raise NbtError("Negative byte array length")
        return read_exact(stream, length)
    if tag_type == TAG_STRING:
        return read_string(stream)
    if tag_type == TAG_LIST:
        child_type = read_number(stream, "b")
        length = read_number(stream, "i")
        if length < 0:
            raise NbtError("Negative list length")
        return [read_payload(stream, child_type) for _ in range(length)]
    if tag_type == TAG_COMPOUND:
        compound: dict[str, Any] = {}
        while True:
            child_type = read_number(stream, "b")
            if child_type == TAG_END:
                return compound
            child_name = read_string(stream)
            compound[child_name] = read_payload(stream, child_type)
    if tag_type == TAG_INT_ARRAY:
        length = read_number(stream, "i")
        if length < 0:
            raise NbtError("Negative int array length")
        return [read_number(stream, "i") for _ in range(length)]
    if tag_type == TAG_LONG_ARRAY:
        length = read_number(stream, "i")
        if length < 0:
            raise NbtError("Negative long array length")
        return [read_number(stream, "q") for _ in range(length)]
    raise NbtError(f"Unsupported NBT tag type {tag_type} at byte {stream.tell() - 1}")


def read_nbt(path: Path) -> dict[str, Any]:
    with gzip.open(path, "rb") as stream:
        root_type = read_number(stream, "b")
        if root_type != TAG_COMPOUND:
            raise NbtError(f"Root tag is {root_type}, expected compound")
        read_string(stream)
        return read_payload(stream, root_type)


def unwrap_schematic(root: dict[str, Any]) -> dict[str, Any]:
    schematic = root.get("Schematic", root)
    if not isinstance(schematic, dict):
        raise NbtError("Schematic root is not a compound")
    return schematic


def decode_varints(data: bytes) -> Iterator[int]:
    value = 0
    shift = 0
    for byte in data:
        value |= (byte & 0x7F) << shift
        if byte & 0x80:
            shift += 7
            if shift > 35:
                raise NbtError("Invalid VarInt in block data")
        else:
            yield value
            value = 0
            shift = 0
    if shift:
        raise NbtError("Truncated VarInt in block data")
def get_block_container(schematic: dict[str, Any]) -> dict[str, Any]:
    blocks = schematic.get("Blocks")
    return blocks if isinstance(blocks, dict) else schematic


def count_tiles(dimensions: Dimensions, tile_size: int) -> dict[str, int]:
    x = (dimensions.width + tile_size - 1) // tile_size
    y = (dimensions.height + tile_size - 1) // tile_size
    z = (dimensions.length + tile_size - 1) // tile_size
    return {"x": x, "y": y, "z": z, "total": x * y * z}


def inspect(path: Path, tile_size: int) -> dict[str, Any]:
    root = read_nbt(path)
    schematic = unwrap_schematic(root)
    blocks = get_block_container(schematic)

    dimensions = Dimensions(
        int(schematic["Width"]),
        int(schematic["Height"]),
        int(schematic["Length"]),
    )
    palette = blocks.get("Palette", schematic.get("Palette"))
    block_data = blocks.get("Data", blocks.get("BlockData", schematic.get("BlockData")))
    if not isinstance(palette, dict) or not isinstance(block_data, bytes):
        raise NbtError("Unsupported schematic block palette/data layout")

    id_to_state = {int(value): str(state) for state, value in palette.items()}
    counts: Counter[str] = Counter()
    tile_counts: Counter[tuple[int, int, int]] = Counter()
    block_count = 0
    non_air_count = 0
    barrier_count = 0
    min_x = min_y = min_z = None
    max_x = max_y = max_z = None
    layer_size = dimensions.width * dimensions.length
    for index, palette_id in enumerate(decode_varints(block_data)):
        block_count += 1
        state = id_to_state.get(palette_id, f"<missing:{palette_id}>")
        counts[state] += 1
        if state.startswith(("minecraft:air", "minecraft:cave_air", "minecraft:void_air")):
            continue
        non_air_count += 1
        y, within_layer = divmod(index, layer_size)
        z, x = divmod(within_layer, dimensions.width)
        tile_counts[(x // tile_size, y // tile_size, z // tile_size)] += 1
        if state.partition("[")[0] == "minecraft:barrier":
            barrier_count += 1
        min_x = x if min_x is None else min(min_x, x)
        min_y = y if min_y is None else min(min_y, y)
        min_z = z if min_z is None else min(min_z, z)
        max_x = x if max_x is None else max(max_x, x)
        max_y = y if max_y is None else max(max_y, y)
        max_z = z if max_z is None else max(max_z, z)

    if block_count != dimensions.volume:
        raise NbtError(
            f"Decoded {block_count} blocks but dimensions require {dimensions.volume}"
        )

    occupied_bounds = None
    if min_x is not None:
        occupied_bounds = {
            "min": [min_x, min_y, min_z],
            "max": [max_x, max_y, max_z],
            "size": [max_x - min_x + 1, max_y - min_y + 1, max_z - min_z + 1],
        }

    risky_block_names = (
        "minecraft:water",
        "minecraft:lava",
        "minecraft:fire",
        "minecraft:soul_fire",
        "minecraft:sand",
        "minecraft:red_sand",
        "minecraft:gravel",
        "minecraft:powder_snow",
        "minecraft:tnt",
        "minecraft:nether_portal",
        "minecraft:end_portal",
        "minecraft:end_gateway",
    )
    risky_counts: Counter[str] = Counter()
    for state, count in counts.items():
        block_name = state.partition("[")[0]
        if block_name.endswith("_leaves") or block_name in risky_block_names:
            risky_counts[block_name] += count
    unknown_namespaces = sorted(
        state for state in counts if not state.startswith("minecraft:")
    )
    block_entities = blocks.get("BlockEntities", schematic.get("BlockEntities", []))
    entities = schematic.get("Entities", [])

    return {
        "file": str(path),
        "compressed_bytes": path.stat().st_size,
        "schematic_version": schematic.get("Version"),
        "data_version": schematic.get("DataVersion"),
        "dimensions": [dimensions.width, dimensions.height, dimensions.length],
        "volume": dimensions.volume,
        "occupied_bounds": occupied_bounds,
        "non_air_blocks": non_air_count,
        "air_blocks": dimensions.volume - non_air_count,
        "palette_size": len(palette),
        "unknown_namespaces": unknown_namespaces,
        "barrier_blocks": barrier_count,
        "risky_blocks": dict(sorted(risky_counts.items())),
        "block_entities": len(block_entities) if isinstance(block_entities, list) else None,
        "entities": len(entities) if isinstance(entities, list) else None,
        "tiles": count_tiles(dimensions, tile_size),
        "non_empty_tiles": len(tile_counts),
        "max_non_air_blocks_in_tile": max(tile_counts.values(), default=0),
        "average_non_air_blocks_per_non_empty_tile": (
            round(non_air_count / len(tile_counts), 2) if tile_counts else 0
        ),
        "most_common_blocks": counts.most_common(20),
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("schematics", nargs="+", type=Path)
    parser.add_argument("--tile-size", type=int, default=48)
    args = parser.parse_args()

    reports = [inspect(path.resolve(), args.tile_size) for path in args.schematics]
    print(json.dumps(reports, indent=2))


if __name__ == "__main__":
    main()
