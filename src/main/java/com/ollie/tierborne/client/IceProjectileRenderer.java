package com.ollie.tierborne.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.entity.IceMob;
import com.ollie.tierborne.entity.IceProjectile;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class IceProjectileRenderer extends EntityRenderer<IceProjectile> {
    private static final ResourceLocation MODEL = resource(
            "models/vfx/awakened_mage/sorcery_combo/glacial_spike/glacial_spike.bbmodel");
    private static final ResourceLocation TEXTURE = resource(
            "textures/vfx/awakened_mage/sorcery_combo/glacial_spike/glacial_spike.png");
    private final OrcModel<IceMob> model = new OrcModel<>(MODEL);

    public IceProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(IceProjectile projectile, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Vec3 velocity = projectile.getDeltaMovement();
        float yaw = (float) Math.toDegrees(Mth.atan2(velocity.x, velocity.z));
        float pitch = (float) Math.toDegrees(Mth.atan2(velocity.y, velocity.horizontalDistance()));
        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(yaw));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(-pitch));
        poseStack.scale(-0.65F, -0.65F, 0.65F);
        poseStack.translate(0.0D, -1.501D, 0.0D);
        model.setupManualAnimation("animation", (projectile.tickCount + partialTick) / 20.0F);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(projectile, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(IceProjectile entity) {
        return TEXTURE;
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(Tierborne.MOD_ID, path);
    }
}
