package com.ollie.tierborne.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.ollie.tierborne.Tierborne;
import com.ollie.tierborne.entity.IceMob;
import com.ollie.tierborne.entity.MageVfxEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Forge-native renderer for the animated models from the Awakened Mage pack. */
public final class MageVfxRenderer extends EntityRenderer<MageVfxEntity> {
    private final Map<String, OrcModel<IceMob>> models = new HashMap<>();

    public MageVfxRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
        preloadModels();
    }

    @Override
    public void render(MageVfxEntity visual, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        RenderFrame frame = frame(visual);
        OrcModel<IceMob> model = models.computeIfAbsent(frame.model,
                name -> new OrcModel<>(resource("models/vfx/awakened_mage/" + name + ".bbmodel")));
        float age = (visual.tickCount + partialTick) / 20.0F;

        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - entityYaw));
        if (frame.followPitch) poseStack.mulPose(Vector3f.XP.rotationDegrees(visual.getXRot()));
        poseStack.scale(-frame.scale, -frame.scale, frame.scale);
        poseStack.translate(0.0D, -1.501D, 0.0D);
        model.setupManualAnimation(frame.animation, age);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(
                resource("textures/vfx/awakened_mage/" + frame.texture + ".png")));
        model.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
        super.render(visual, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MageVfxEntity entity) {
        return resource("textures/vfx/awakened_mage/sorcery_combo/fireball/fireball_long.png");
    }

    private static RenderFrame frame(MageVfxEntity visual) {
        int age = visual.tickCount;
        return switch (visual.getEffect()) {
            case MageVfxEntity.BIG_FIREBALL_CHARGE -> single(
                    "blazing_barrage/big_fireball_charge/big_fireball_charge", "animation", 1.0F, false);
            case MageVfxEntity.FIRE_EXPLOSION -> sequence(
                    "sorcery_combo/fireball_explosion/fireball_explosion_", 8,
                    "animation", age, 1.15F);
            case MageVfxEntity.GLACIAL_SPIKE -> single(
                    "sorcery_combo/glacial_spike/glacial_spike", "animation", 1.0F, true);
            case MageVfxEntity.THUNDER_STRIKE -> sequence(
                    "sorcery_combo/thunder_strike/thunder_strike_", 8,
                    age == 0 && visual.getVariant() > 0
                            ? "animation" + (visual.getVariant() + 1) : "animation",
                    age, 1.0F);
            case MageVfxEntity.THUNDER_TELEPORT -> sequence(
                    "teleport_strike/thunder_teleport/thunder_teleport_", 10,
                    "animation", age, 1.0F);
            case MageVfxEntity.THUNDER_EXPLOSION -> sequence(
                    "teleport_strike/thunder_explosion/thunder_explosion_", 10,
                    "animation", Math.max(0, age - 2), 1.0F);
            case MageVfxEntity.CRYO_PRISON -> single(
                    "cryo_prison/cryo_prison", "animation", 1.0F, false);
            case MageVfxEntity.CRYO_CAGE -> single(
                    "cryo_prison/cryo_prison_cage", "crystal_the_enemy", 1.0F, false);
            case MageVfxEntity.HAIL_INHALE -> single(
                    "hailpiercer/hailpiercer_inhale_breath", "animation", 1.0F, true);
            case MageVfxEntity.HAIL_SPIKE_CENTER -> single(
                    "hailpiercer/hailpiercer", "ice_spike_o", 1.0F, true);
            case MageVfxEntity.HAIL_SPIKE_LEFT -> single(
                    "hailpiercer/hailpiercer", "ice_spike", 1.0F, true);
            case MageVfxEntity.HAIL_SPIKE_RIGHT -> single(
                    "hailpiercer/hailpiercer", "ice_spike2", 1.0F, true);
            case MageVfxEntity.METEOR -> single(
                    "meteor_of_doom/meteor_of_doom", age < 40 ? "charge_meteor" : "meteor_impact",
                    1.0F, true);
            case MageVfxEntity.METEOR_CROSS -> single(
                    "meteor_of_doom/meteor_of_doom_cross", "meteor_impact", 1.0F, false);
            case MageVfxEntity.RUPTURE -> rupture(age);
            case MageVfxEntity.RUBBLE -> single("extras/vfx_rubbles", "skill", 1.0F, false);
            default -> single("blazing_barrage/magic_fire_circle/magic_fire_circle",
                    visual.getVariant() == 0 ? "animation"
                            : "animation" + Math.min(7, visual.getVariant() + 1), 1.0F, false);
        };
    }

    private void preloadModels() {
        preload("blazing_barrage/big_fireball_charge/big_fireball_charge");
        preload("blazing_barrage/magic_fire_circle/magic_fire_circle");
        preload("cryo_prison/cryo_prison");
        preload("cryo_prison/cryo_prison_cage");
        preload("hailpiercer/hailpiercer");
        preload("hailpiercer/hailpiercer_inhale_breath");
        preload("meteor_of_doom/meteor_of_doom");
        preload("meteor_of_doom/meteor_of_doom_cross");
        preload("sorcery_combo/fireball/fireball");
        preload("sorcery_combo/glacial_spike/glacial_spike");
        for (int index = 1; index <= 8; index++) {
            preload("sorcery_combo/fireball_explosion/fireball_explosion_" + index);
            preload("sorcery_combo/thunder_strike/thunder_strike_" + index);
        }
        for (int index = 1; index <= 10; index++) {
            preload("teleport_strike/thunder_explosion/thunder_explosion_" + index);
            preload("teleport_strike/thunder_teleport/thunder_teleport_" + index);
        }
        for (int index = 1; index <= 5; index++) {
            preload("extras/vfx_earthquake_rupture_" + index);
        }
        preload("extras/vfx_rubbles");
    }

    private void preload(String path) {
        models.put(path, new OrcModel<>(resource("models/vfx/awakened_mage/" + path + ".bbmodel")));
    }

    private static RenderFrame rupture(int age) {
        int frame = age < 44 ? 1 : Math.min(5, 2 + (age - 44) / 2);
        String animation = frame == 1 ? "skill2" : "skill";
        return new RenderFrame("extras/vfx_earthquake_rupture_" + frame,
                "extras/vfx_earthquake_rupture_" + frame, animation, 1.0F, false);
    }

    private static RenderFrame sequence(String prefix, int count, String animation, int age, float scale) {
        int frame = Math.min(count, Math.max(1, age + 1));
        return new RenderFrame(prefix + frame, prefix + frame, animation, scale, false);
    }

    private static RenderFrame single(String path, String animation, float scale, boolean followPitch) {
        return new RenderFrame(path, path, animation, scale, followPitch);
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(Tierborne.MOD_ID, path);
    }

    private record RenderFrame(String model, String texture, String animation,
                               float scale, boolean followPitch) {
    }
}
