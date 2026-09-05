package com.ollie.tierborne.client;

import com.ollie.tierborne.Tierborne;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.IronGolem;

public final class RuneboundColossusRenderer extends IronGolemRenderer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Tierborne.MOD_ID, "textures/entity/runebound_colossus.png");

    public RuneboundColossusRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void scale(IronGolem entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.2F, 1.2F, 1.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(IronGolem entity) {
        return TEXTURE;
    }
}
