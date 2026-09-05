# Mage elemental interactions

This is the implementation contract for interactions between Mage elements. It
is maintained alongside the code rather than reconstructed after development.

## Design rules

- Reactions are calculated on the logical server.
- Offensive reactions never damage players, preserving Tierborne's existing
  no-friendly-fire behaviour for class abilities.
- A target is considered **Chilled** only when a Mage ice spell marked it, not
  merely because an unrelated source applied vanilla Slowness.
- A target is considered **Toxic** only when a Mage poison spell marked it.
- Reaction bonus damage uses the caster's Magic Damage attribute.
- Consumed statuses are removed before the new element is applied.
- Every reaction emits particles and a sound so the outcome is visible.
- Reaction balance values live in `RpgBalanceConfig` and therefore appear in a
  world's `serverconfig/tierborne-balance.toml` after that world loads.

## Reaction table

| Incoming element | Existing state | Result |
| --- | --- | --- |
| Fire | Chilled or frozen | Thermal Shock bonus damage; Mage chill, freezing, and its Slowness are removed; fire is then applied |
| Ice | Burning | Thermal Shock bonus damage; existing fire is extinguished; chill, Slowness, and freezing are then applied |
| Fire | Toxic | Combustion bonus damage; Mage toxin and its Poison effect are consumed; fire is then applied |
| Lightning | Chilled or Toxic | Conductive bonus damage; the enabling status remains available for subsequent lightning hits |
| Doctor cleanse | Burning, Chilled, Toxic, frozen, or harmful potion effect | Fire, Mage elemental markers, freezing, and harmful effects are removed without hostile damage |

## Enemy affinities

Elemental damage also respects the target's natural type:

| Target | Incoming element | Default result |
| --- | --- | --- |
| Any Tierborne `IceMob`, including the Ice Knight | Fire | `+25%` damage |
| Any Tierborne `IceMob`, including the Ice Knight | Ice | `50%` damage resistance |
| Any entity with vanilla `MobType.UNDEAD` | Poison | `100%` damage resistance |

Affinity multipliers apply to the base spell and reaction bonus damage before
the caster's Magic Damage attribute. Elemental Vulnerability then multiplies the
affinity-adjusted value. This means vulnerability improves a favourable or
resisted hit proportionally but does not bypass complete poison immunity.
Undead targets are not given the Mage Toxic marker or Poison effect, preventing
an immune target from being primed for a later Combustion reaction.

Fire can trigger both Thermal Shock and Combustion on a target carrying both
states. Each reaction is resolved once, and the consumed markers prevent the
same status from being repeatedly detonated.

## Implemented reaction service

`ElementalCombat` owns marker expiry, status consumption, bonus damage, reaction
particles, and reaction sounds. Defaults are:

- Thermal Shock: `4` base magic damage
- Poison Combustion: `3` base magic damage
- Conductive lightning: `+25%` lightning damage

All three pass through the caster's Magic Damage modifier. Thermal Shock and
Combustion also respect the existing Elemental Vulnerability mechanic.

## Spell coverage

- Fire: Fire Mage Arcane Bolt, Blazing Barrage impacts, and Meteor Ring
- Ice: Ice Mage Arcane Bolt, Hailpiercer, and Cryo Prison
- Poison: Poison Mage Arcane Bolt, Venom Bolt, and every Toxic Cloud damage tick
- Lightning: Lightning Mage Arcane Bolt jumps, Chain Lightning, and both
  Thunderstep impact areas
- Doctor: Purge on every affected player

Fireball and Flame Slash use the shared projectile classes already used by the
Magic Swordsman. Their fire impacts therefore participate in the same reactions,
allowing deliberate cooperation between a Magic Swordsman and a Mage. Normal
weapon Fire Aspect and environmental fire do not directly invoke the reaction
service, although an Ice spell will still recognize and extinguish an entity
that is currently burning from those sources.

Lightning's Conductive modifier is evaluated independently for each Arcane Bolt
jump, each Chain Lightning jump, and each Thunderstep impact. Chill and Toxic are
not consumed, so a sequence of lightning hits can benefit for the marker's
remaining duration.

Purge calls the shared cleanse operation before drawing its cleansing particles.
It removes fire, frozen ticks, Mage Chill, Mage Toxic, and all vanilla harmful
effects. Its undead damage remains separate and does not affect players.

## Awakened Mage visual integration

The purchased SamusDev Awakened Mage models are rendered through a short-lived,
non-interactive `MageVfxEntity`. The entity only synchronises an effect identifier,
variant, orientation, and lifetime. It cannot deal damage, select targets, move a
player, or alter cooldowns. Those decisions remain in `MageCombat` on the logical
server. `MageVfxRenderer` and the projectile renderers are client-only registrations.

The visual mapping is:

| Tierborne ability | Imported animation families |
| --- | --- |
| Mage Staff | Fireball explosion, glacial spike, or thunder strike by subclass |
| Blazing Barrage | Seven fire-circle animations, big fireball charge, four fireball flight animations, eight-frame explosion |
| Meteor Ring | Meteor charge/impact, meteor cross, five rupture stages, rubble, explosion sequence |
| Hailpiercer | Inhale, three Hailpiercer spike animations, glacial spike |
| Cryo Prison | Prison eruption and looping target cage |
| Chain Lightning | Eight-stage thunder strike, with four animation variants |
| Thunderstep | Ten-stage teleport and ten-stage thunder explosion at departure and arrival |
| Tartarus ranged ice attacks | Animated glacial-spike projectile and impact |

All 52 supplied Blockbench animation models are referenced by these render paths.
The pack's YAML files remain reference material only; Tierborne does not load or
execute MythicMobs or ModelEngine configuration.

## Configuration keys

The implemented `elementalInteractions` config section defines:

- `thermalShockDamage`
- `poisonCombustionDamage`
- `conductiveLightningBonusPercent`
- `iceMobFireVulnerabilityPercent`
- `iceMobIceResistancePercent`
- `undeadPoisonResistancePercent`

## Status ownership

Mage status expiry times are stored in the target entity's persistent Forge data:

- `tierborne:mage_chilled_until`
- `tierborne:mage_toxic_until`

An expired marker is ignored. Consuming or cleansing a marker removes it. The
vanilla fire, freezing, Slowness, and Poison states continue to provide normal
Minecraft behaviour and visible HUD feedback.

## Validation log

- The first Forge build succeeded after adding the shared reaction service,
  wiring every Mage spell family, integrating Magic Swordsman fire projectiles,
  and exposing reaction descriptions in the skill-detail UI.
- A follow-up interaction review added target affinities and ensured undead are
  never assigned a Toxic marker.
- The second Forge build succeeded after the affinity pass.
- Both builds used the required Minecraft 1.19.2 / Forge 43.5.0 project without
  dependency or version changes.
- The Awakened Mage integration build succeeded after importing all 52 VFX models,
  adapting Mage skill names and descriptions, registering the Runestaff and sounds,
  and replacing vanilla-item projectile rendering with animated models.
- The Forge client reached the main menu and eagerly parsed all 52 imported VFX
  models without a Tierborne model, texture, sound, or resource-loading error.
- In-world visual inspection of Mage Staff, Blazing Barrage, Hailpiercer, and a
  Tartarus ranged ice mob remains a manual check because the test environment did
  not expose the Minecraft window for interaction. Startup alone is not treated as
  proof of visual correctness.
