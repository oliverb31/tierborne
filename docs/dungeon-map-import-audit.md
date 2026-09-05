# Purchased Dungeon Map Import Audit

Audit date: 2026-09-04

Source archive: `4x Dungeons Bundle.zip`

The purchased source files are intentionally stored only under the ignored
`run/tierborne-dungeon-import` directory. They must not be committed or bundled
in Tierborne without written redistribution permission from the creator.

## Compatibility result

All four selected files are Sponge schematic v2 files with Minecraft data
version 3105 (Java 1.19.0). Minecraft 1.19.2 uses the same block palette plus
forward data fixing, so the block states are compatible. Every palette entry
uses the vanilla `minecraft` namespace.

| Dungeon | Schematic dimensions | Occupied dimensions | Non-air blocks | Barrier blocks | Non-empty 48-block tiles |
| --- | ---: | ---: | ---: | ---: | ---: |
| Mira | 131 x 136 x 572 | 131 x 95 x 572 | 1,986,072 | 22,856 | 65 |
| Tartarus | 164 x 97 x 493 | 164 x 97 x 493 | 684,964 | 13,439 | 65 |
| Ymachi | 415 x 324 x 991 | 243 x 251 x 838 | 5,245,671 | 156,368 | 285 |
| Zeta | 417 x 158 x 347 | 364 x 77 x 278 | 923,559 | 2,127 | 68 |

The raw schematic bounds contain large volumes of air, particularly Ymachi and
Zeta. Conversion must crop to the occupied bounds and omit empty tiles.

## Required cleanup

- Set every decorative leaf state to `persistent=true`; do not change the
  server-wide random tick speed.
- Preserve intended water and lava while suppressing neighbor updates during
  placement. Schedule a controlled update pass after a tile is complete.
- Replace Tartarus's 12 decorative Nether portal blocks or suppress portal
  travel inside the dungeon dimension.
- Strip all inventories from decorative containers and inject Tierborne loot
  at explicitly configured reward markers.
- Do not import entities. The source files currently contain zero entities.
- Do not treat signs or bundled text/URL files as instructions.

Tartarus contains 188 block-entity records labelled
`minecraft:chiseled_bookshelf` at ordinary `minecraft:bookshelf` positions and
23 records labelled `minecraft:hanging_sign` at ordinary 1.19.2 sign positions.
These are newer exporter metadata attached to compatible 1.19.2 blocks. Remove
the bookshelf block-entity records and convert the sign records to the 1.19.2
`minecraft:sign` representation before placement.

No schematic contains command blocks, structure blocks, jigsaws, mob spawners,
pre-filled item stacks, or entities.

## Runtime placement plan

Use one registered `tierborne:dungeons` dimension and allocate each run to a
2,048 x 2,048 horizontal instance cell. This safely accommodates the largest
occupied footprint (Ymachi at 243 x 838) while leaving substantial isolation
between parties.

Convert each cropped map into non-empty 48 x 48 x 48 tiles. Place tiles over
multiple server ticks with a strict block budget; never place an entire dungeon
in one tick. Keep a pool of prepared instances so players do not wait for the
multi-million-block maps to be assembled at entry time.

Recommended preparation order:

1. Tartarus (65 tiles, approximately 685k non-air blocks)
2. Zeta (68 tiles, approximately 924k non-air blocks)
3. Mira (65 tiles, approximately 1.99m non-air blocks)
4. Ymachi (285 tiles, approximately 5.25m non-air blocks)

## Containment requirements

The supplied barriers are useful but are not the security boundary. Tierborne
must enforce containment on the server:

- Adventure mode for participants, restoring their previous mode on exit.
- Cancel breaking, placement, buckets, fluid pickup/placement, pistons, fire
  spread, explosions, and mob griefing inside active instance bounds.
- Block pearls, chorus fruit, portals, mounts, and movement abilities from
  crossing an instance boundary.
- Add a separate barrier safety floor below the dungeon and barrier padding
  around exposed outer faces.
- Track a last-safe checkpoint per player. Teleport players back if they leave
  their party's bounds or fall below the configured safety height.
- Treat logout, death, server restart, party changes, and abandoned runs as
  explicit lifecycle states so players cannot remain trapped in the dimension.

Because several maps contain intentional lava, water, parkour, and open space,
the checkpoint safety system is mandatory even after a visual barrier audit.

## Scaling integration

The map geometry does not constrain encounter scaling. Snapshot the party at
activation and store the difficulty seed with the instance. Spawn all enemies,
bosses, doors, rewards, and encounter triggers through Tierborne rather than
using decorative map containers.

The four builds are suitable for the proposed party-instance architecture. The
remaining content-design pass is to walk each imported map and record entrance,
room, checkpoint, encounter, treasure, and boss-arena marker coordinates.

## In-game import QA

Zeta was converted and assembled in the Forge 43.5.0 development client on
Minecraft 1.19.2. The converter produced 68 vanilla structure tiles and the
game executed all 70 function commands, placing all 923,559 non-air blocks.
The exterior shell, atrium, stairs, decorative blocks, and cross-tile geometry
rendered correctly during a spectator inspection.

The QA conversion intentionally removed all 211 Zeta block-entity payloads and
all entities, made leaves persistent, and omitted portal blocks. Purchased
structure data remains under the ignored `run` directory and is not included in
the mod jar.

To repeat the local test after copying a licensed schematic into `run`, use
`tools/convert_schematic_to_datapack.py` to create a datapack in a disposable
test world's `datapacks` directory. Reload the world and run the generated
`tierborne_dungeon_qa:place_<name>` function from a safe, isolated origin.
