package com.ollie.tierborne.playerclass;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

public final class GeneralSkillRules {
    private GeneralSkillRules() {}

    public static boolean isWoodBlock(BlockState state) {
        Material material = state.getMaterial();
        return state.is(BlockTags.MINEABLE_WITH_AXE)
                && (material == Material.WOOD || material == Material.NETHER_WOOD
                || state.is(BlockTags.LOGS));
    }
}
