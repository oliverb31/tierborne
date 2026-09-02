# Third-party armour assets

Tierborne includes armour texture work derived from the following open-source
projects. The corresponding full licence texts are retained in
`THIRD_PARTY_LICENSES/`.

## SimpleOres2

- Source: https://github.com/Sinhika/SimpleOres2
- Revision: `f129d09deabf20e037292fe81cca77307d79c27b` (`1.19` branch)
- Licence: GNU Lesser General Public License v3.0 or later
- Assets used: Copper and Mythril armour model layers and item icons.
- Tierborne changes: `mythril` filenames were renamed to `mithril`; Copper and
  Mithril artwork is otherwise unmodified.

## Simply Steel Continued

- Source: https://github.com/SkpC9/Simply-Steel
- Revision: `10329a6222a398bb18a8142fb16390a7270ee317`
- Licence: MIT
- Assets used: Steel armour model layers and item icons.
- Tierborne changes: files were repathed into the Tierborne namespace; artwork
  is otherwise unmodified.

## Many More Ores and Crafts

- Source: https://github.com/Graclyxz/Many-More-Ores-and-Crafts
- Revision: `941837076ee80e0e616e74330508d496ca10b10a`
- Licence: MIT
- Assets used directly: Silver, Tungsten, Orichalcum and Adamantite humanoid
  armour textures and item icons.
- Assets used as a modification base: Amethyst humanoid armour textures and
  item icons for Tierborne's Runic and Uru sets.
- Tierborne changes:
  - New-format `textures/entity/equipment/humanoid/<material>.png` sheets were
    repathed as Forge 1.19.2 `<material>_layer_1.png` sheets.
  - New-format `humanoid_leggings` sheets were repathed as
    `<material>_layer_2.png` sheets.
  - Runic recolours the Amethyst base to silver/gunmetal with restrained violet
    rune strokes.
  - Uru substantially recolours the Amethyst base to near-black gunmetal with
    denser blue-violet rune strokes and restrained gold nodes.

Minecraft and Minecraft Forge are not covered by these third-party notices.
