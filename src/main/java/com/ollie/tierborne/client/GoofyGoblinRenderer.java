package com.ollie.tierborne.client;

import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.entity.GoofyGoblin;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class GoofyGoblinRenderer extends MobRenderer<GoofyGoblin, GoofyGoblinModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Tierborne.MOD_ID, "textures/entity/goofy_goblin.png");

    public GoofyGoblinRenderer(EntityRendererProvider.Context context) {
        super(context, new GoofyGoblinModel(), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(GoofyGoblin entity) {
        return TEXTURE;
    }
}
