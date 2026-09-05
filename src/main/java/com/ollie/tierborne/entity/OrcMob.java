package com.ollie.tierborne.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Forge-native runtime shared by the purchased orc models. Orcs are only spawned explicitly. */
public final class OrcMob extends Monster {
    private static final EntityDataAccessor<String> ATTACK_ANIMATION =
            SynchedEntityData.defineId(OrcMob.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> ANIMATION_START =
            SynchedEntityData.defineId(OrcMob.class, EntityDataSerializers.INT);

    private int attackCooldown;
    private int activeAttack;
    private LivingEntity attackTarget;

    public OrcMob(EntityType<? extends OrcMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = kind() == Kind.BOSS ? 25 : kind() == Kind.ELITE ? 12 : 7;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_ANIMATION, "");
        this.entityData.define(ANIMATION_START, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide || !isAlive()) {
            return;
        }

        if (this.activeAttack > 0) {
            tickAttack(this.tickCount - getAnimationStartTick());
            return;
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        double distance = distanceTo(target);
        double attackRange = isRanged() ? 15.0D : kind() == Kind.BOSS ? 7.0D : 4.0D;
        if (distance <= attackRange && this.attackCooldown <= 0 && hasLineOfSight(target)) {
            beginAttack(chooseAttack(distance), target);
        } else if (distance > (isRanged() ? 10.0D : 2.5D)) {
            getNavigation().moveTo(target, kind() == Kind.BOSS ? 1.05D : 1.1D);
        }
    }

    private void beginAttack(int attack, LivingEntity target) {
        this.activeAttack = attack;
        this.attackTarget = target;
        this.entityData.set(ATTACK_ANIMATION, "atk" + attack);
        this.entityData.set(ANIMATION_START, this.tickCount);
        getNavigation().stop();
    }

    private void tickAttack(int tick) {
        int duration = attackDuration(this.activeAttack);
        if (this.attackTarget != null && this.attackTarget.isAlive()) {
            getLookControl().setLookAt(this.attackTarget, 30.0F, 30.0F);
        }
        performAttack(this.activeAttack, tick);
        if (tick >= duration) {
            this.activeAttack = 0;
            this.attackTarget = null;
            this.attackCooldown = kind() == Kind.BOSS ? 35 : 25;
            this.entityData.set(ATTACK_ANIMATION, "");
        }
    }

    private int chooseAttack(double distance) {
        return switch (kind()) {
            case WARRIOR -> this.random.nextBoolean() ? 1 : 3;
            case SPEARTHROWER -> distance <= 3.0D ? 2 : 1;
            case SHAMAN -> distance <= 3.0D ? 2 : (this.random.nextBoolean() ? 1 : 3);
            case ELITE -> {
                if (distance > 5.0D) yield 4;
                int[] attacks = {1, 2, 3};
                yield attacks[this.random.nextInt(attacks.length)];
            }
            case BOSS -> this.random.nextInt(8) + 1;
        };
    }

    private int attackDuration(int attack) {
        if (kind() == Kind.BOSS) {
            return switch (attack) {
                case 1 -> 80;
                case 2 -> 37;
                case 3 -> 85;
                case 4 -> 120;
                case 5 -> 50;
                case 6 -> 35;
                case 7 -> 60;
                default -> 120;
            };
        }
        if (kind() == Kind.ELITE && attack == 4) return 60;
        if (kind() == Kind.SHAMAN && attack == 3) return 40;
        if (attack == 2) return 37;
        return 35;
    }

    private void performAttack(int attack, int tick) {
        switch (kind()) {
            case WARRIOR -> {
                if (tick == 18) coneHit(4.0D, 90.0D, 1.0F, attack == 3 ? 0.35D : 0.1D, 0.2D);
            }
            case SPEARTHROWER -> {
                if (attack == 2 && tick == 12) coneHit(3.0D, 90.0D, 0.6F, 0.7D, 0.35D);
                if (attack == 1 && tick == 18) launchProjectile(OrcProjectile.SPEAR, 1.25F);
            }
            case SHAMAN -> {
                if (attack == 2 && tick == 12) coneHit(3.0D, 90.0D, 0.6F, 0.6D, 0.3D);
                if (attack == 1 && tick == 21) launchProjectile(OrcProjectile.ESSENCE, 0.75F);
                if (attack == 3 && (tick == 10 || tick == 20 || tick == 30)) {
                    launchProjectile(OrcProjectile.ESSENCE, 0.75F);
                }
            }
            case ELITE -> {
                if (attack == 1 && tick == 17) coneHit(4.0D, 90.0D, 1.5F, 0.15D, 0.2D);
                if (attack == 2 && tick == 12) coneHit(3.0D, 90.0D, 0.6F, 0.7D, 0.35D);
                if (attack == 3 && (tick == 15 || tick == 27)) coneHit(4.5D, 100.0D, 0.8F, 0.2D, 0.2D);
                if (attack == 4 && (tick == 18 || tick == 40)) launchProjectile(OrcProjectile.AXE, 0.75F);
            }
            case BOSS -> performBossAttack(attack, tick);
        }
    }

    private void performBossAttack(int attack, int tick) {
        switch (attack) {
            case 1 -> {
                if (tick == 45) shockwave(position().add(getLookAngle().scale(5.0D)), 8.0D, 1.0F, 0.8D);
            }
            case 2 -> {
                if (tick == 12) coneHit(4.5D, 90.0D, 0.6F, 0.9D, 0.45D);
            }
            case 3 -> {
                if ((tick == 16 || tick == 20 || tick == 24) && this.attackTarget != null) lungeAt(this.attackTarget, 0.65D);
                if (tick == 41) shockwave(position().add(getLookAngle().scale(2.0D)), 8.0D, 1.0F, 1.0D);
            }
            case 4 -> {
                if (tick >= 16 && tick <= 90 && tick % 2 == 0 && this.attackTarget != null) lungeAt(this.attackTarget, 0.22D);
                if (tick >= 18 && tick <= 102 && (tick - 18) % 7 == 0) shockwave(position(), 4.5D, 1.0F, 0.25D);
            }
            case 5 -> {
                if (tick == 25) coneHit(5.5D, 60.0D, 1.0F, 1.1D, 0.5D);
            }
            case 6 -> {
                if (tick == 10) coneHit(5.0D, 60.0D, 1.0F, 1.1D, 0.5D);
            }
            case 7 -> {
                if (tick == 17) shockwave(position(), 6.0D, 1.0F, 1.2D);
            }
            case 8 -> {
                if (tick == 40) shockwave(position(), 8.0D, 1.0F, 1.3D);
            }
            default -> {
            }
        }
    }

    private void coneHit(double radius, double angle, float multiplier, double horizontalKnockback, double verticalKnockback) {
        Vec3 forward = getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        double minimumDot = Math.cos(Math.toRadians(angle * 0.5D));
        for (Player player : this.level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius), Player::isAlive)) {
            Vec3 offset = player.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            if (offset.lengthSqr() > radius * radius || offset.lengthSqr() < 0.001D
                    || forward.dot(offset.normalize()) < minimumDot) continue;
            damage(player, multiplier);
            Vec3 push = offset.normalize().scale(horizontalKnockback);
            player.push(push.x, verticalKnockback, push.z);
        }
        attackSound();
    }

    private void shockwave(Vec3 center, double radius, float multiplier, double knockback) {
        if (!(this.level instanceof ServerLevel server)) return;
        for (Player player : this.level.getEntitiesOfClass(Player.class,
                getBoundingBox().move(center.subtract(position())).inflate(radius), Player::isAlive)) {
            Vec3 offset = player.position().subtract(center);
            if (offset.horizontalDistanceSqr() > radius * radius) continue;
            damage(player, multiplier);
            Vec3 push = offset.multiply(1.0D, 0.0D, 1.0D).normalize().scale(knockback);
            player.push(push.x, 0.5D, push.z);
        }
        server.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.25D, center.z,
                90, radius * 0.35D, 0.25D, radius * 0.35D, 0.08D);
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                        this.level.getBlockState(new net.minecraft.core.BlockPos(center).below())),
                center.x, center.y + 0.1D, center.z,
                120, radius * 0.35D, 0.15D, radius * 0.35D, 0.25D);
        server.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.4F, 0.65F);
    }

    private void launchProjectile(int style, float multiplier) {
        if (this.attackTarget == null || !this.attackTarget.isAlive() || !(this.level instanceof ServerLevel server)) return;
        OrcProjectile projectile = new OrcProjectile(this, this.attackTarget, style,
                (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * multiplier);
        server.addFreshEntity(projectile);
        server.playSound(null, blockPosition(), style == OrcProjectile.ESSENCE
                        ? SoundEvents.ILLUSIONER_CAST_SPELL : SoundEvents.WITCH_THROW,
                SoundSource.HOSTILE, 0.9F, style == OrcProjectile.ESSENCE ? 1.5F : 0.6F);
    }

    private void lungeAt(LivingEntity target, double speed) {
        Vec3 direction = target.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D).normalize();
        setDeltaMovement(direction.x * speed, Math.max(getDeltaMovement().y, 0.08D), direction.z * speed);
        hasImpulse = true;
    }

    private void damage(Player player, float multiplier) {
        player.invulnerableTime = 0;
        player.hurt(DamageSource.mobAttack(this),
                (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * multiplier);
    }

    private void attackSound() {
        this.level.playSound(null, blockPosition(), SoundEvents.WITCH_THROW, SoundSource.HOSTILE, 0.8F,
                kind() == Kind.BOSS ? 0.45F : 0.75F);
    }

    public String getAnimationName() {
        if (this.deathTime > 0) return "death";
        String attack = this.entityData.get(ATTACK_ANIMATION);
        if (!attack.isEmpty()) return attack;
        return getDeltaMovement().horizontalDistanceSqr() > 0.0025D ? "walk" : "idle";
    }

    public int getAnimationStartTick() {
        return this.entityData.get(ANIMATION_START);
    }

    public boolean hasActiveAttackAnimation() {
        return !this.entityData.get(ATTACK_ANIMATION).isEmpty() || this.deathTime > 0;
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level.isClientSide) {
            this.entityData.set(ATTACK_ANIMATION, "death");
            this.entityData.set(ANIMATION_START, this.tickCount);
        }
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.deathTime >= 60 && !this.level.isClientSide && !isRemoved()) {
            this.level.broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return kind() == Kind.BOSS ? SoundEvents.RAVAGER_AMBIENT : SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return kind() == Kind.BOSS ? SoundEvents.RAVAGER_HURT : SoundEvents.ZOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return kind() == Kind.BOSS ? SoundEvents.RAVAGER_DEATH : SoundEvents.ZOGLIN_DEATH;
    }

    private boolean isRanged() {
        Kind kind = kind();
        return kind == Kind.SPEARTHROWER || kind == Kind.SHAMAN || kind == Kind.ELITE;
    }

    public Kind kind() {
        EntityType<?> type = getType();
        if (type == ModEntities.ORC_SPEARTHROWER.get()) return Kind.SPEARTHROWER;
        if (type == ModEntities.ORC_SHAMAN.get()) return Kind.SHAMAN;
        if (type == ModEntities.ORC_ELITE.get()) return Kind.ELITE;
        if (type == ModEntities.ORC_BOSS.get()) return Kind.BOSS;
        return Kind.WARRIOR;
    }

    public enum Kind {
        WARRIOR,
        SPEARTHROWER,
        SHAMAN,
        ELITE,
        BOSS
    }
}
