package com.ollie.tierborne.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** A short-lived, non-interactive carrier for client-only animated spell visuals. */
public final class MageVfxEntity extends Entity {
    public static final int FIRE_CIRCLE = 0;
    public static final int BIG_FIREBALL_CHARGE = 1;
    public static final int FIRE_EXPLOSION = 2;
    public static final int GLACIAL_SPIKE = 3;
    public static final int THUNDER_STRIKE = 4;
    public static final int THUNDER_TELEPORT = 5;
    public static final int THUNDER_EXPLOSION = 6;
    public static final int CRYO_PRISON = 7;
    public static final int CRYO_CAGE = 8;
    public static final int HAIL_INHALE = 9;
    public static final int HAIL_SPIKE_CENTER = 10;
    public static final int HAIL_SPIKE_LEFT = 11;
    public static final int HAIL_SPIKE_RIGHT = 12;
    public static final int METEOR = 13;
    public static final int METEOR_CROSS = 14;
    public static final int RUPTURE = 15;
    public static final int RUBBLE = 16;

    private static final EntityDataAccessor<Integer> EFFECT =
            SynchedEntityData.defineId(MageVfxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DURATION =
            SynchedEntityData.defineId(MageVfxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(MageVfxEntity.class, EntityDataSerializers.INT);

    public MageVfxEntity(EntityType<? extends MageVfxEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static MageVfxEntity spawn(ServerLevel level, Vec3 position, int effect, int duration,
                                      float yaw, float pitch) {
        return spawn(level, position, effect, duration, yaw, pitch, 0);
    }

    public static MageVfxEntity spawn(ServerLevel level, Vec3 position, int effect, int duration,
                                      float yaw, float pitch, int variant) {
        MageVfxEntity visual = new MageVfxEntity(ModEntities.MAGE_VFX.get(), level);
        visual.setPos(position.x, position.y, position.z);
        visual.setYRot(yaw);
        visual.setXRot(pitch);
        visual.entityData.set(EFFECT, effect);
        visual.entityData.set(DURATION, Math.max(1, duration));
        visual.entityData.set(VARIANT, variant);
        level.addFreshEntity(visual);
        return visual;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(EFFECT, FIRE_CIRCLE);
        entityData.define(DURATION, 20);
        entityData.define(VARIANT, 0);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (!level.isClientSide && tickCount >= getDuration()) discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }

    public int getEffect() {
        return entityData.get(EFFECT);
    }

    public int getDuration() {
        return entityData.get(DURATION);
    }

    public int getVariant() {
        return entityData.get(VARIANT);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(EFFECT, tag.getInt("Effect"));
        entityData.set(DURATION, Math.max(1, tag.getInt("Duration")));
        entityData.set(VARIANT, tag.getInt("Variant"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Effect", getEffect());
        tag.putInt("Duration", getDuration());
        tag.putInt("Variant", getVariant());
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
