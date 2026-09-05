package com.ollie.tierborne.client;

import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.entity.OrcMob;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class OrcRenderer extends MobRenderer<OrcMob, OrcModel> {
    private final ResourceLocation texture;

    public OrcRenderer(EntityRendererProvider.Context context, String assetName, float shadowRadius) {
        super(context, new OrcModel(new ResourceLocation(Tierborne.MOD_ID,
                "models/entity/" + assetName + ".bbmodel")), shadowRadius);
        this.texture = new ResourceLocation(Tierborne.MOD_ID, "textures/entity/" + assetName + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(OrcMob entity) {
        return this.texture;
    }

    @Override
    protected void scale(OrcMob entity, PoseStack poseStack, float partialTick) {
        if (entity.kind() == OrcMob.Kind.BOSS) {
            poseStack.scale(1.44F, 1.44F, 1.44F);
        }
    }
}
