package com.ollie.tierborne.registry;

import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.block.OrcishAltarCoreBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Tierborne.MOD_ID);

    public static final RegistryObject<Block> ORCISH_ALTAR_CORE = BLOCKS.register("orcish_altar_core",
            () -> new OrcishAltarCoreBlock(BlockBehaviour.Properties.copy(Blocks.MANGROVE_PLANKS)
                    .strength(4.0F, 8.0F)
                    .lightLevel(state -> state.getValue(OrcishAltarCoreBlock.ACTIVE) ? 14 : 4)));

    private ModBlocks() {
    }
}
