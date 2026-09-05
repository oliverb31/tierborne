package com.ollie.tierborne.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.entity.IceMob;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class IceMobRenderer extends MobRenderer<IceMob, OrcModel<IceMob>> {
    private final ResourceLocation texture;
    private final float modelScale;

    public IceMobRenderer(EntityRendererProvider.Context context, String assetName,
                          float shadowRadius, float modelScale) {
        super(context, new OrcModel<>(new ResourceLocation(Tierborne.MOD_ID,
                "models/entity/" + assetName + ".bbmodel")), shadowRadius);
        this.texture = new ResourceLocation(Tierborne.MOD_ID, "textures/entity/" + assetName + ".png");
        this.modelScale = modelScale;
    }

    @Override
    public ResourceLocation getTextureLocation(IceMob entity) {
        return texture;
    }

    @Override
    protected void scale(IceMob entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(modelScale, modelScale, modelScale);
    }
}
