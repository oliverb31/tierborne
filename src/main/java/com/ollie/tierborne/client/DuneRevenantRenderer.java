package com.ollie.tierborne.client;

import com.ollie.tierborne.Tierborne;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

public final class DuneRevenantRenderer extends ZombieRenderer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Tierborne.MOD_ID, "textures/entity/dune_revenant.png");

    public DuneRevenantRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        return TEXTURE;
    }
}
