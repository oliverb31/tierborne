package com.ollie.tierborne.client;

import com.ollie.tierborne.Tierborne;
import net.minecraft.client.renderer.entity.ElderGuardianRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Guardian;

public final class AbyssalWatcherRenderer extends ElderGuardianRenderer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Tierborne.MOD_ID, "textures/entity/abyssal_watcher.png");

    public AbyssalWatcherRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Guardian entity) {
        return TEXTURE;
    }
}
