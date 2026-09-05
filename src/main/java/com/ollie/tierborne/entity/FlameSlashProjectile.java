package com.ollie.tierborne.entity;

import com.ollie.tierborne.combat.Element;
import com.ollie.tierborne.combat.ElementalCombat;
import com.ollie.tierborne.config.RpgBalanceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import com.ollie.tierborne.registry.ModAttributes;

public final class FlameSlashProjectile extends Projectile implements ItemSupplier {
    private float damage;
    private float enchantmentScale;
    private Vec3 start = Vec3.ZERO;

    public FlameSlashProjectile(EntityType<? extends FlameSlashProjectile> type, Level level) {
        super(type, level);
    }

    public FlameSlashProjectile(ServerPlayer owner, float damage, float enchantmentScale) {
        this(ModEntities.FLAME_SLASH.get(), owner.level);
        setOwner(owner);
        this.damage = damage;
        this.enchantmentScale = enchantmentScale;
        Vec3 look = owner.getLookAngle().normalize();
        this.start = owner.getEyePosition().add(look.scale(0.8));
        setPos(start.x, start.y, start.z);
        setDeltaMovement(look.scale(RpgBalanceConfig.FLAME_SLASH_SPEED.get()));
    }

    @Override
    public void tick() {
        super.tick();
        if (position().distanceTo(start) >= RpgBalanceConfig.FLAME_SLASH_MAX_RANGE.get()) {
            impact();
            return;
        }
        Vec3 movement = getDeltaMovement();
        if (destroyWeakBlocks(movement)) {
            impact();
            return;
        }
        HitResult hit = ProjectileUtil.getHitResult(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) onHit(hit);
        if (isRemoved()) return;
        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
        spawnCrescentParticles();
    }

    private boolean destroyWeakBlocks(Vec3 movement) {
        if (!(level instanceof ServerLevel server) || !(getOwner() instanceof ServerPlayer owner)) return false;
        boolean blocked = false;
        net.minecraft.world.phys.AABB path = new net.minecraft.world.phys.AABB(position(),position().add(movement)).inflate(RpgBalanceConfig.FLAME_SLASH_WIDTH.get()/2.0,RpgBalanceConfig.FLAME_SLASH_HEIGHT.get()/2.0,RpgBalanceConfig.FLAME_SLASH_WIDTH.get()/2.0);
        for (BlockPos pos : BlockPos.betweenClosed(net.minecraft.util.Mth.floor(path.minX),net.minecraft.util.Mth.floor(path.minY),net.minecraft.util.Mth.floor(path.minZ),net.minecraft.util.Mth.floor(path.maxX),net.minecraft.util.Mth.floor(path.maxY),net.minecraft.util.Mth.floor(path.maxZ))) {
            BlockState state = server.getBlockState(pos);
            if (state.isAir()) continue;
            float resistance = state.getBlock().getExplosionResistance();
            boolean destroyable = state.getDestroySpeed(server, pos) >= 0.0F && server.mayInteract(owner, pos);
            if (destroyable && resistance >= 0.0F && resistance <= RpgBalanceConfig.FLAME_SLASH_MAX_BLOCK_RESISTANCE.get()) {
                server.destroyBlock(pos, false, owner);
            } else {
                blocked = true;
            }
        }
        return blocked;
    }

    private void spawnCrescentParticles() {
        if (!(level instanceof ServerLevel server)) return;
        Vec3 forward = getDeltaMovement().normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x).normalize();
        double halfWidth = RpgBalanceConfig.FLAME_SLASH_WIDTH.get() / 2.0;
        for (int i = -4; i <= 4; i++) {
            double t = i / 4.0;
            Vec3 point = position().add(right.scale(t * halfWidth)).add(0.0, (1.0 - Math.abs(t)) * 0.65 - 0.25, 0.0);
            server.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.01);
            if (i % 2 == 0) server.sendParticles(ParticleTypes.SMOKE, point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        if (tickCount % 3 == 0) server.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 1, 0.2, 0.2, 0.2, 0.0);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity target && getOwner() instanceof ServerPlayer owner) {
            float enchantmentDamage = net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getDamageBonus(owner.getMainHandItem(), target.getMobType()) * enchantmentScale;
            float amount = ElementalCombat.modifyDamage(target, Element.FIRE, damage + enchantmentDamage);
            amount *= (float) owner.getAttributeValue(ModAttributes.MAGIC_DAMAGE.get());
            if (target.hurt(DamageSource.indirectMagic(this, owner), amount)) {
                target.setSecondsOnFire((int)Math.round(RpgBalanceConfig.FIREBALL_IGNITION_SECONDS.get()));
            }
        }
        impact();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity != getOwner() && super.canHitEntity(entity);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() != HitResult.Type.ENTITY) impact();
    }

    private void impact() {
        if (isRemoved()) return;
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 30, 0.45, 0.45, 0.45, 0.04);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            server.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.75F);
        }
        discard();
    }

    @Override public ItemStack getItem() { return new ItemStack(Items.FIRE_CHARGE); }
    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) { damage=tag.getFloat("Damage");enchantmentScale=tag.getFloat("EnchantmentScale");start=new Vec3(tag.getDouble("StartX"),tag.getDouble("StartY"),tag.getDouble("StartZ")); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.putFloat("Damage",damage);tag.putFloat("EnchantmentScale",enchantmentScale);tag.putDouble("StartX",start.x);tag.putDouble("StartY",start.y);tag.putDouble("StartZ",start.z); }
    @Override public Packet<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
