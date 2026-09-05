package com.ollie.tierborne.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
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

public final class OrcProjectile extends Projectile implements ItemSupplier {
    public static final int SPEAR = 0;
    public static final int AXE = 1;
    public static final int ESSENCE = 2;

    private static final EntityDataAccessor<Integer> STYLE =
            SynchedEntityData.defineId(OrcProjectile.class, EntityDataSerializers.INT);
    private float damage;

    public OrcProjectile(EntityType<? extends OrcProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public OrcProjectile(OrcMob owner, LivingEntity target, int style, float damage) {
        this(ModEntities.ORC_PROJECTILE.get(), owner.level);
        setOwner(owner);
        this.entityData.set(STYLE, style);
        this.damage = damage;
        Vec3 start = owner.position().add(0.0D, owner.getBbHeight() * 0.72D, 0.0D);
        Vec3 aim = target.getBoundingBox().getCenter().subtract(start).normalize();
        setPos(start.x, start.y, start.z);
        setDeltaMovement(aim.scale(style == ESSENCE ? 0.82D : 1.05D));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(STYLE, SPEAR);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 80) {
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
        if (this.entityData.get(STYLE) != ESSENCE) {
            setDeltaMovement(movement.add(0.0D, -0.018D, 0.0D));
        } else if (this.level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(), getZ(),
                    3, 0.12D, 0.12D, 0.12D, 0.0D);
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
        if (result.getEntity() instanceof LivingEntity target && getOwner() instanceof OrcMob owner) {
            target.hurt(this.entityData.get(STYLE) == ESSENCE
                    ? DamageSource.indirectMagic(this, owner) : DamageSource.mobAttack(owner), this.damage);
            if (this.entityData.get(STYLE) == ESSENCE) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
        }
        impact();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() != HitResult.Type.ENTITY) impact();
    }

    private void impact() {
        if (this.level instanceof ServerLevel server) {
            server.sendParticles(this.entityData.get(STYLE) == ESSENCE ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.CRIT,
                    getX(), getY(), getZ(), 12, 0.2D, 0.2D, 0.2D, 0.05D);
        }
        discard();
    }

    public int getStyle() {
        return this.entityData.get(STYLE);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(switch (getStyle()) {
            case AXE -> Items.IRON_AXE;
            case ESSENCE -> Items.SLIME_BALL;
            default -> Items.TRIDENT;
        });
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.damage = tag.getFloat("Damage");
        this.entityData.set(STYLE, tag.getInt("Style"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Damage", this.damage);
        tag.putInt("Style", this.entityData.get(STYLE));
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
