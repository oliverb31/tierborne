# Dungeon runtime

Tierborne registers a void dimension at `tierborne:dungeons`. Licensed dungeon
structures are not included in the mod. A server owner must locally convert the
purchased 1.19 schematics into an enabled world datapack with
`tools/convert_schematic_to_datapack.py`.

The converter writes a manifest beside the structure tiles. Before party entry,
the server verifies the manifest covers every source position, every listed tile
exists, every tile has the declared dimensions and block count, and the final
placed-block total matches. Placement and cleanup are limited to 20,000 block
operations per server tick and resume from saved progress after a restart.

## Player commands

- `/tierborne party invite <player>` invites a player.
- `/tierborne party accept <leader>` accepts an invitation.
- `/tierborne party leave` leaves or disbands the staging party.
- `/tierborne dungeon start <mira|tartarus|ymachi|zeta>` starts preparation.
  Every accepted member must be online, in the same dimension, and within 16
  blocks of the leader.
- `/tierborne dungeon checkpoint` records the player's current safe position.
- `/tierborne dungeon leave` leaves individually and restores the prior position
  and game mode.
- `/tierborne dungeon finish` ends the run for the party and starts cleanup.
- `/tierborne dungeon reload` reloads manifests and requires operator permission.

Each instance owns a 2,048 by 2,048 cell. The map is centered using its complete
source dimensions, with a separate padded barrier floor and perimeter. Players
are placed in Adventure mode and server events prevent block changes, buckets,
fire spread, explosions, pistons, mob griefing, portals, pearls, chorus fruit,
mount escapes, boundary crossing, and falls below the safety floor.

Encounter mobs spawned inside an active cell receive a persistent health and
damage multiplier based on the party size captured at activation. The per-player
percentages are configurable under the `dungeons` section of
`tierborne-balance.toml`.
