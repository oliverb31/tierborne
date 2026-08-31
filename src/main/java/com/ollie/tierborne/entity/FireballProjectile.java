package com.ollie.tierborne.entity;

import com.ollie.tierborne.combat.AbilityRuntime;
import com.ollie.tierborne.combat.Element;
import com.ollie.tierborne.combat.ElementalCombat;
import com.ollie.tierborne.config.RpgBalanceConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public final class FireballProjectile extends Projectile implements ItemSupplier {
    private float damage;
    private float enchantmentScale;
    private float charge = 1.0F;
    private float travelSpeed = 1.15F;
    private int homingLifetimeTicks;
    private boolean homingMember;
    private boolean homingSessionMember;

    public FireballProjectile(EntityType<? extends FireballProjectile> type, Level level) { super(type, level); }

    public FireballProjectile(ServerPlayer owner, float damage, float enchantmentScale,
                              boolean homingMember, float charge, float travelSpeed) {
        this(ModEntities.FIREBALL.get(), owner.level);
        setOwner(owner);
        this.damage = damage;
        this.enchantmentScale = enchantmentScale;
        this.homingMember = homingMember;
        this.homingSessionMember = homingMember;
        this.homingLifetimeTicks = homingMember
                ? RpgBalanceConfig.ticks(RpgBalanceConfig.HOMING_FIREBALL_MAX_CONTROL_SECONDS) : 0;
        this.charge = charge;
        this.travelSpeed = travelSpeed;
        Vec3 look = owner.getLookAngle();
        Vec3 spawnPosition = owner.getEyePosition().add(look.scale(0.45D)).add(0.0D, -0.15D, 0.0D);
        setPos(spawnPosition.x, spawnPosition.y, spawnPosition.z);
        setDeltaMovement(look.scale(travelSpeed));
    }

    @Override
    public void tick() {
        super.tick();
        if (homingSessionMember && tickCount >= homingLifetimeTicks) {
            endControlState();
            discard();
            return;
        }
        if (tickCount > 300) { endControlState(); discard(); return; }
        if (homingMember && getOwner() instanceof ServerPlayer owner
                && !AbilityRuntime.steerHoming(owner, this)) homingMember = false;
        HitResult hit = ProjectileUtil.getHitResult(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) onHit(hit);
        if (isRemoved()) return;
        Vec3 movement = getDeltaMovement();
        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
        ProjectileUtil.rotateTowardsMovement(this, 0.2F);
        spawnFlightParticles();
    }

    private void spawnFlightParticles() {
        if (!(level instanceof ServerLevel server)) return;
        double visualCharge = Math.max(0.0D, Math.min(1.0D, charge));
        int coreCount = lerpCount(RpgBalanceConfig.FIREBALL_MIN_CORE_PARTICLES.get(),
                RpgBalanceConfig.FIREBALL_MAX_CORE_PARTICLES.get(), visualCharge);
        int trailCount = lerpCount(RpgBalanceConfig.FIREBALL_MIN_TRAIL_PARTICLES.get(),
                RpgBalanceConfig.FIREBALL_MAX_TRAIL_PARTICLES.get(), visualCharge);
        double radius = lerp(RpgBalanceConfig.FIREBALL_MIN_PARTICLE_RADIUS.get(),
                RpgBalanceConfig.FIREBALL_MAX_PARTICLE_RADIUS.get(), visualCharge);
        server.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), coreCount,
                radius, radius, radius, 0.01D);
        if (tickCount % 2 == 0 && trailCount > 0) {
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), trailCount,
                    radius * 0.65D, radius * 0.65D, radius * 0.65D, 0.005D);
        }
        if (homingMember && tickCount % 3 == 0) {
            server.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 1,
                    radius * 0.25D, radius * 0.25D, radius * 0.25D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity target && getOwner() instanceof ServerPlayer owner) {
            float enchantmentDamage = net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getDamageBonus(owner.getMainHandItem(), target.getMobType()) * enchantmentScale;
            float amount = ElementalCombat.modifyDamage(target, Element.FIRE, damage + enchantmentDamage);
            if (target.hurt(DamageSource.indirectMagic(this, owner), amount)) {
                target.setSecondsOnFire((int) Math.round(RpgBalanceConfig.FIREBALL_IGNITION_SECONDS.get()));
                Vec3 direction = getDeltaMovement().normalize();
                target.push(direction.x * RpgBalanceConfig.FIREBALL_KNOCKBACK.get(), 0.2D,
                        direction.z * RpgBalanceConfig.FIREBALL_KNOCKBACK.get());
            }
        }
        impact();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity instanceof FireballProjectile other && other.getOwner() == getOwner()) return false;
        return super.canHitEntity(entity);
    }

    @Override protected void onHit(HitResult result) { super.onHit(result); if (result.getType() != HitResult.Type.ENTITY) impact(); }

    private void impact() {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 28, 0.35D, 0.35D, 0.35D, 0.05D);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 8, 0.25D, 0.25D, 0.25D, 0.02D);
            server.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        endControlState();
        discard();
    }

    public void releaseControl() { homingMember = false; }
    public void expireControlled() { endControlState(); discard(); }
    public float damageValue() { return damage; }
    public float enchantmentScale() { return enchantmentScale; }
    public float charge() { return charge; }
    public float travelSpeed() { return travelSpeed; }
    @Override public ItemStack getItem() { return new ItemStack(Items.FIRE_CHARGE); }

    private void endControlState() {
        if (homingSessionMember && getOwner() instanceof ServerPlayer owner) AbilityRuntime.homingProjectileRemoved(owner, getUUID());
        homingMember = false;
        homingSessionMember = false;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        endControlState();
        super.remove(reason);
    }

    private static int lerpCount(int minimum, int maximum, double amount) { return (int) Math.round(lerp(minimum, maximum, amount)); }
    private static double lerp(double minimum, double maximum, double amount) { return minimum + (maximum - minimum) * amount; }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) { damage=tag.getFloat("Damage");enchantmentScale=tag.getFloat("EnchantmentScale");charge=tag.getFloat("Charge");travelSpeed=tag.getFloat("TravelSpeed");homingMember=tag.getBoolean("HomingMember");homingSessionMember=tag.getBoolean("HomingSessionMember");homingLifetimeTicks=tag.getInt("HomingLifetimeTicks"); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.putFloat("Damage",damage);tag.putFloat("EnchantmentScale",enchantmentScale);tag.putFloat("Charge",charge);tag.putFloat("TravelSpeed",travelSpeed);tag.putBoolean("HomingMember",homingMember);tag.putBoolean("HomingSessionMember",homingSessionMember);tag.putInt("HomingLifetimeTicks",homingLifetimeTicks); }
    @Override public Packet<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
