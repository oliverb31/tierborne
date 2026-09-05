package com.ollie.tierborne.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import com.ollie.tierborne.registry.ModSounds;
import net.minecraft.sounds.SoundSource;

public final class IceProjectile extends Projectile implements ItemSupplier {
    private float damage;

    public IceProjectile(EntityType<? extends IceProjectile> type, Level level) {
        super(type, level);
    }

    public IceProjectile(IceMob owner, LivingEntity target, float damage) {
        this(ModEntities.ICE_PROJECTILE.get(), owner.level);
        setOwner(owner);
        this.damage = damage;
        Vec3 start = owner.position().add(0.0D, owner.getBbHeight() * 0.7D, 0.0D);
        Vec3 aim = target.getBoundingBox().getCenter().subtract(start).normalize();
        setPos(start.x, start.y, start.z);
        setDeltaMovement(aim.scale(0.9D));
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 100) {
            discard();
            return;
        }
        Vec3 start = position();
        Vec3 end = start.add(getDeltaMovement());
        HitResult hit = ProjectileUtil.getHitResult(this, this::canHitEntity);
        EntityHitResult standardPlayerHit = CombatHitboxes.firstStandardPlayerHit(this, start, end);
        if (standardPlayerHit != null && (hit.getType() == HitResult.Type.MISS
                || start.distanceToSqr(standardPlayerHit.getLocation()) < start.distanceToSqr(hit.getLocation()))) {
            hit = standardPlayerHit;
        }
        if (hit.getType() != HitResult.Type.MISS) onHit(hit);
        if (isRemoved()) return;
        Vec3 movement = getDeltaMovement();
        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(),
                    2, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        return entity != getOwner() && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity target && getOwner() instanceof IceMob owner) {
            target.hurt(DamageSource.indirectMagic(this, owner), damage);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
        }
        impact();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() != HitResult.Type.ENTITY) impact();
    }

    private void impact() {
        if (level instanceof ServerLevel server) {
            MageVfxEntity.spawn(server, position(), MageVfxEntity.GLACIAL_SPIKE,
                    24, getYRot(), getXRot());
            server.playSound(null, blockPosition(), ModSounds.MAGE_GLACIAL_SPIKES.get(),
                    SoundSource.HOSTILE, 0.9F, 0.9F);
            server.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(),
                    18, 0.3D, 0.3D, 0.3D, 0.04D);
        }
        discard();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.SNOWBALL);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        damage = tag.getFloat("Damage");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Damage", damage);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
