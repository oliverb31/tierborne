# Third-party assets

Tierborne's armour textures are sourced from premade, openly licensed
Minecraft projects. The corresponding full licence texts are retained in
`THIRD_PARTY_LICENSES/`.

The source artwork is used as the credited base. Most changes are limited to
renaming, repathing, and Forge 1.19.2 texture-layout conversion; specific visual
edits are listed below.

## Pylon Resource Pack

- Source: https://github.com/pylonmc/pylon-resource-pack
- Revision: `a7b49d81cd2c640395df376be3af8490958bd54c`
- Licence: GNU Lesser General Public License v3.0
- Copper uses Pylon's Bronze worn layers and item icons.
- Steel uses Pylon's Steel worn layers and item icons.
- Tungsten uses Pylon's Palladium worn layers and item icons.
- Tierborne changes: files were renamed and repathed only.

## BetterEnd

- Source: https://github.com/quiqueck/BetterEnd
- Revision: `629a6dacc7756f4bb12e6eb8db35fb349a369a2a`
- Licence for the armour files used here: MIT
- Silver uses BetterEnd's Thallasium worn layers and item icons.
- Mithril uses BetterEnd's Terminite worn layers and item icons.
- Runic uses BetterEnd's Crystalite worn layers and item icons.
- Uru uses BetterEnd's Aeternium worn layers and item icons.
- Tierborne changes: new-format `textures/entity/equipment/humanoid` and
  `humanoid_leggings` files were renamed and repathed to Forge 1.19.2
  `<material>_layer_1.png` and `<material>_layer_2.png` locations.
- Crystalite's unused lower texture-sheet extension was cropped to the standard
  64x32 Forge 1.19.2 armour layout.
- Runic's Crystalite base was recoloured so its body and leg plates use the
  existing aquamarine palette while violet is retained only on the shoulders.
- BetterEnd's separately listed CC BY-NC-SA assets are not used.

## BetterNether

- Source: https://github.com/quiqueck/BetterNether
- Revision: `f984a669a4790b4215de8563708bcff2977c44d7`
- Licence for the armour files used here: MIT
- Orichalcum uses BetterNether's Flaming Ruby worn layers and item icons.
- Tierborne changes: new-format humanoid files were renamed and repathed to
  Forge 1.19.2 armour-layer locations; item icons were renamed and repathed.
- BetterNether's separately listed CC BY-NC-SA assets are not used.

## SimpleOres2

- Source: https://github.com/Sinhika/SimpleOres2
- Revision: `3c7d2b6efd5a79c2a19f714b70243eed4e84bbd2`
- Licence: GNU Lesser General Public License v3.0 or later
- Adamantite uses SimpleOres2's Adamantium worn layers and item icons.
- Tierborne changes: `adamantium` files were renamed to `adamantite` and
  repathed into the Tierborne namespace.

Minecraft and Minecraft Forge are not covered by these third-party notices.

## SamusDev RPG Class Awakened Mage v1.1

- Creator and copyright owner: SamusDev / samus2002
- Source package: `samus2002_AWAKENED_MAGE_v1.1 (1).zip`, purchased from MCModels
- Product site: https://samusdev.com/
- Imported content: 52 animated Blockbench VFX models, their textures, the Runestaff
  model and texture, and 14 Mage sound effects.
- Tierborne changes: the original ModelEngine blueprints are read by Tierborne's
  Forge-native Blockbench renderer; embedded textures were materialised as normal
  resource-pack PNG files; namespaces and paths were adapted for the `tierborne`
  resource domain. MythicMobs, MMOCore, MythicLib, ItemsAdder, Oraxen, Nexo, and
  ModelEngine configurations are not shipped or executed as gameplay code.
- The included terms permit editing the models, textures, and MythicMobs files for
  server/content use and prohibit resale. Videos and streams using these assets must
  credit “Custom minecraft models by SamusDev” with the website and YouTube links
  supplied in the package terms.

## mobs_mc

- Source: https://github.com/maikerumine/mobs_mc
- Revision: `c01b1c550068bfc1976adfd3d88af187d45f11b9`
- Licence for the four textures used here: MIT, as specified by the repository's per-file `LICENSE-media.md` rule for textures not otherwise listed.
- Dune Revenant uses `mobs_mc_husk.png`.
- Frostbound Archer uses `mobs_mc_stray.png`.
- Runebound Colossus uses `mobs_mc_iron_golem.png`.
- Abyssal Watcher uses `mobs_mc_guardian_elder.png`.
- Tierborne changes: files were renamed and repathed only. Models, spawn icons, code, and sounds from the source repository are not used.
