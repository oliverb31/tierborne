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
`tierborne-balance.toml`. Marker mobs remain unspawned until a participant is
within 40 blocks or has an unobstructed line to the marker position. Spawned
dungeon mobs use 360-degree, block-respecting line-of-sight acquisition at the
configured dungeon vision range and begin pursuing as soon as they first spot a
player. The default vision range is 48 blocks. Normal dungeon orcs begin choosing
idle walking routes immediately instead of waiting for the vanilla delayed
stroll check. Marker mobs are created one block above the actual collision
surface at their saved X/Z coordinate, including partial blocks such as slabs,
then gravity settles them onto the floor. A mob is skipped rather than being
created inside a floor, wall, or low ceiling. Imported crop blocks and stems are replaced with air during instance
preparation so carrots, potatoes, seeds, and similar farm drops are not scattered
through a dungeon.
One tick after any encounter mob spawns, the server applies a zero-damage wake-up
hit and a very small knockback pulse so its movement and collision state begin
immediately.

## Orcish Altar Core

The Orc Lush dungeon can be entered from an active Orcish Altar Core. Place the
core directly above the centre mangrove roots in this one-layer foundation:

```text
Mangrove Log   Mangrove Slab   Mangrove Log
Mangrove Slab  Mangrove Roots  Mangrove Slab
Mangrove Log   Mangrove Slab   Mangrove Log
```

The core emits brighter light and green particles while the foundation is
complete. Right-clicking an incomplete core opens a foundation schematic;
right-clicking a complete core opens the Solo/Party menu. Solo immediately prepares
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

## New-world defaults

Every world created with the current Tierborne build receives and enables the
bundled dungeon-map datapack while its spawn is created. Bundled mob-marker
positions are copied into that world's saved data at the same time rather than
waiting for the first dungeon run. The world starts with marker activation at
40 blocks and dungeon mob vision at 48 blocks. Collision-safe marker placement,
immediate normal-orc wandering, the Orc Boss at 1.44 times its original size,
crop removal, and the
incomplete-altar schematic are mod behavior and therefore apply to every new
world without additional setup.

When the Orc Boss first targets a player, its name and live health bar appear at
the top centre of the screen for every online party member currently inside that
dungeon instance. The bar remains for the encounter and is removed when the boss
dies, the player leaves, or the instance begins cleanup.
