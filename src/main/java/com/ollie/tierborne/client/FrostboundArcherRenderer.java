package com.ollie.tierborne.client;

import com.ollie.tierborne.Tierborne;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;

public final class FrostboundArcherRenderer extends SkeletonRenderer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Tierborne.MOD_ID, "textures/entity/frostbound_archer.png");

    public FrostboundArcherRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.STRAY, ModelLayers.STRAY_INNER_ARMOR, ModelLayers.STRAY_OUTER_ARMOR);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSkeleton entity) {
        return TEXTURE;
    }
}
