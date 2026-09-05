# Combat hitboxes and animation runtime

This document describes Tierborne's hostile-mob attack collision rules and the
runtime used to play the imported Blockbench animations. It records the changes
made for the Tartarus ice enemies, Ice Knight boss, and existing orcs so later
combat or animation work does not silently reintroduce centre-point collision or
one-shot idle animations.

## Supported version

- Minecraft 1.19.2
- Forge 43.5.0
- Java 17 game runtime

The implementation intentionally uses the Forge/Minecraft 1.19.2 APIs already
resolved by this project.

## Standard player combat hitbox

Hostile attacks are tested against an explicit standing-player box:

- Width: `0.6` blocks (`0.3` blocks on each side of the player's X/Z position)
- Height: `1.8` blocks, beginning at the player's feet

`CombatHitboxes.standardPlayer` constructs this box independently of the
player's current pose. Consequently, crouching, swimming, gliding, or another
temporarily shortened pose does not make a hostile attack unexpectedly pass
through the part of the normal standing model that the player sees as occupied.

This helper is server-safe and contains no client rendering classes. Damage
remains server-authoritative.

### Target exclusions

Hostile orc and ice attacks do not damage:

- Creative-mode players
- Spectators
- Dead players
- The projectile's owner

These exclusions are applied both to area attacks and projectile collision.

## Melee and area collision

The old checks measured distance to `player.position()`, which represents the
point at the player's feet. A weapon or shockwave could therefore visually touch
the player's body while its mathematical radius missed that point.

The replacement checks the nearest horizontal point of the entire standard
player box. An attack connects when any part of that box is within the configured
horizontal radius and the broad attack volume overlaps it vertically.

### Ice enemies

`IceMob.areaAttack` supplies collision for:

- Frostmite attacks
- Gnut lunges
- Undead Ice Warrior attacks
- Yeti attacks
- Ice Knight and Ice Knight minion melee attacks
- Mounted Ice Knight attacks
- Ice Knight shockwaves
- Snowball Spirit charge explosions

All of these now use the same standard player-box test. The configured damage,
radius, knockback, animation hit frame, and cooldown are otherwise unchanged.

### Orc enemies

Orc melee attacks are directional cones. A cone checks the centre and eight
horizontal sample points covering the standard player's four corners and edge
midpoints. Contact with any sampled portion inside both the attack radius and
angle counts as a hit. This avoids requiring the player's feet-centre point to
be inside the cone.

Orc boss shockwaves use the same nearest-point radius test as ice area attacks.
Warrior, Elite, Spearthrower, Shaman, and boss damage values and animation hit
frames are unchanged.

## Block-respecting line of sight

Making the full standing box hittable must not allow attacks through walls.
`CombatHitboxes.hasLineOfSightToPlayer` traces from the attacker's eyes to three
points down the vertical centre of the standard player box:

- Lower body: `0.25` blocks above the feet
- Torso: `0.9` blocks above the feet
- Upper body: `1.6` blocks above the feet

The attack may connect when at least one of these points has an unobstructed
block ray. If all three rays hit solid collision before reaching the player, the
attack is rejected. This gives sensible results around low cover without
allowing damage through a complete wall.

## Projectile collision

Both `IceProjectile` and `OrcProjectile` aim at the centre of the target's
bounding box rather than only the eye position. Each server tick performs:

1. The normal Minecraft projectile trace for blocks and entities.
2. A swept-line trace against explicit standard player boxes.
3. A distance comparison between the two results.
4. Resolution of whichever valid collision occurs first along the flight path.

The distance comparison is important: an expanded player hitbox cannot steal a
collision that occurs behind a nearer wall or another nearer entity. This keeps
terrain collision intact while ensuring a projectile crossing any portion of a
standing player model registers the player hit.

This applies to:

- Frozen Blaze ice shots
- Ice Witch ranged attacks
- Iceologer ice shots
- Orc spears
- Orc axes
- Shaman essence projectiles

The standard box is padded by half the projectile width plus `0.05` blocks so the
projectile's own physical size participates in the swept collision.

## Player abilities and PvP scope

The standard-player hostile hitbox rules above govern attacks made by the new
ice mobs and existing orcs against players. Mage damage spells continue to
exclude players intentionally, matching Tierborne's existing no-friendly-fire
ability behaviour. Doctor rays and area support spells can target players
because those effects heal, cleanse, or buff rather than deal hostile damage.

## Blockbench animation loading

`OrcModel` is the shared runtime parser and renderer for both orc and ice
`.bbmodel` assets. It loads visible model bones, cube UVs, animation channels,
and position/rotation/scale keyframes from the bundled Blockbench files.

### Hidden helper geometry

Blockbench groups whose `visibility` property is false are excluded recursively.
This specifically prevents the Ice Knight's authoring-only `hitbox` group from
appearing as a visible cube in game. Hidden child groups are also skipped.

### Continuous animations

Some purchased Blockbench files label idle or movement animations as `once`,
despite those clips being continuous gameplay states. Tierborne explicitly loops
these state names:

- `idle`
- `walk`
- `idle_mount`
- `walk_mount`
- `spin`
- `charge`
- `throw_dash_idle`

Animations correctly labelled `loop` by the source file also loop normally.
Attack, summon, transition, hurt, and death animations remain one-shot clips.

### Corrected ice animation durations

Server attack state is held long enough for the corresponding visual clip:

- Ice Knight `slash`: 30 ticks
- Iceologer `attack2`: 30 ticks
- Ice Witch close attacks: 10 ticks
- Ice Witch ranged attack: 20 ticks
- Ice Knight `raise_spear`: 120 ticks
- Ice Knight two-second attacks and mount summon: 40 ticks
- Mounted 1.5-second attacks: 30 ticks
- Ice Knight death: 60 ticks
- Ice Knight minion death: 50 ticks
- Ice Witch death: 100 ticks
- Yeti death: 40 ticks
- Other one-second ice deaths: 20 ticks

The gameplay effect remains tied to `IceMob.hitTick`, so damage and summon
events occur on their intended animation frame rather than when the clip ends.

## Mage casting presentation

Mage casting is synchronized from the server with `SyncMageCastPacket` and
stored client-side by `ClientMageCastState`. `ClientEvents` applies three native
humanoid casting styles:

- Projectile/ray cast
- Radial or area cast
- Thunderstep teleport cast

Third-person rendering poses both arms for nearby clients. First-person
rendering moves and rotates the held staff during the same synchronized window.
Spell-specific particles and sounds provide the fire, ice, poison, lightning,
healing, cleansing, and buff feedback. No external animation dependency is
required.

## Relevant source files

- `entity/CombatHitboxes.java`: standard player boxes, cone/radius tests, LOS,
  and swept projectile-player collision
- `entity/IceMob.java`: ice attack selection, hit frames, area attacks, and
  animation state durations
- `entity/IceProjectile.java`: ice projectile aiming and collision resolution
- `entity/OrcMob.java`: orc cone attacks and boss shockwaves
- `entity/OrcProjectile.java`: orc projectile aiming and collision resolution
- `client/OrcModel.java`: Blockbench geometry and animation runtime
- `client/IceMobRenderer.java`: model/texture selection and display scale
- `client/ClientEvents.java`: Mage first- and third-person casting poses
- `client/ClientMageCastState.java`: synchronized Mage animation timing

## Validation record

On 2026-09-05:

- Every bundled ice `.bbmodel` was inspected for available animation names and
  clip lengths.
- Runtime-selected attack names were compared with their model animations.
- The Ice Knight hidden helper group and non-looping state metadata were found
  and corrected.
- `gradlew.bat build` completed successfully after the animation corrections.
- `gradlew.bat build` completed successfully after the combat-hitbox changes.
- The development client reached the title screen and loaded Tierborne resources
  without an error entry in `run/logs/latest.log` before the hitbox-only changes.

The available desktop-control bridge did not expose the Java game window, so a
human in-world visual pass remains necessary for subjective matters such as
whether a pose looks attractive. Compilation, asset parsing, animation-name
mapping, state timing, and client resource initialization have been verified.

