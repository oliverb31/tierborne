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
- `/tierborne dungeon start <mira|tartarus|ymachi|zeta|orc_lush>` starts preparation.
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

## Orcish Altar Core

The Orc Lush dungeon can be entered from an active Orcish Altar Core. Place the
core directly above the centre mangrove roots in this one-layer foundation:

```text
Mangrove Log   Mangrove Slab   Mangrove Log
Mangrove Slab  Mangrove Roots  Mangrove Slab
Mangrove Log   Mangrove Slab   Mangrove Log
```

The core emits brighter light and green particles while the foundation is
complete. Right-clicking it opens the Solo/Party menu. Solo immediately prepares
a one-player instance. Party sends an on-screen invitation to each online member;
the leader can start immediately with the members who have accepted.

Each player's return position and game mode are captured when their invitation is
shown. Finishing the dungeon or dying inside it returns that player to the saved
position. A dungeon death keeps the player's inventory and removes them from the
active run.

## Placing exact mob spawns

Operators can create an authoring instance with:

```text
/tierborne dungeon edit orc_lush
```

The authoring instance suppresses configured marker mobs, places the player in
Creative mode, and gives them a Dungeon Marker Wand. Hold the wand and right-click
the top face of the exact floor block where a mob should stand. Choose an Orc
Warrior, Spearthrower, Shaman, Elite, or Boss in the menu. Choosing the same block
again adds another mob there. `Remove Last` removes one marker from that block and
`Clear Block` removes all of them.

Enchantment particles show nearby saved positions while the wand is held. Marker
coordinates are stored relative to the original map in the world's
`tierborne_dungeon_markers` saved data, so they apply to every later instance no
matter which temporary instance cell is used. Bundled marker resources under
`data/tierborne/dungeon_markers` seed new worlds; once a world has initialized a
dungeon's markers, its editable saved copy takes precedence and is not duplicated.
Run `/tierborne dungeon finish` to leave editing mode and return to the position
and game mode used before editing.
