package com.ollie.tierborne.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.entity.OrcProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class OrcProjectileRenderer extends ThrownItemRenderer<OrcProjectile> {
    private static final ResourceLocation SPEAR_TEXTURE = texture("orc_vfx_spearthrower_am");
    private static final ResourceLocation AXE_TEXTURE = texture("orc_vfx_elite_am");

    private final OrcModel spearModel;
    private final OrcModel axeModel;

    public OrcProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, 0.8F, true);
        this.spearModel = model("orc_vfx_spearthrower_am");
        this.axeModel = model("orc_vfx_elite_am");
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(OrcProjectile projectile, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (projectile.getStyle() == OrcProjectile.ESSENCE) {
            super.render(projectile, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }

        boolean axe = projectile.getStyle() == OrcProjectile.AXE;
        OrcModel model = axe ? this.axeModel : this.spearModel;
        ResourceLocation texture = axe ? AXE_TEXTURE : SPEAR_TEXTURE;
        Vec3 velocity = projectile.getDeltaMovement();
        double horizontalSpeed = velocity.horizontalDistance();
        float yaw = (float) Math.toDegrees(Mth.atan2(velocity.x, velocity.z));
        float pitch = (float) Math.toDegrees(Mth.atan2(velocity.y, horizontalSpeed));
        float animationTime = (projectile.tickCount + partialTick) / 20.0F;

        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(yaw));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(-pitch));
        poseStack.translate(0.0D, axe ? -16.45D / 16.0D : -19.0D / 16.0D, 0.0D);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0D, -1.501D, 0.0D);

        model.setupManualAnimation(axe ? "idle" : "spawn", animationTime);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    private static OrcModel model(String assetName) {
        return new OrcModel(new ResourceLocation(Tierborne.MOD_ID, "models/entity/" + assetName + ".bbmodel"));
    }

    private static ResourceLocation texture(String assetName) {
        return new ResourceLocation(Tierborne.MOD_ID, "textures/entity/" + assetName + ".png");
    }
}
