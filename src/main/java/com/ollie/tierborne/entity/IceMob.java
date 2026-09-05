package com.ollie.tierborne.entity;

import com.ollie.tierborne.dungeon.DungeonManager;
import com.ollie.tierborne.dungeon.DungeonSavedData;
import com.ollie.tierborne.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class IceMob extends Monster implements AnimatedBlockbenchMob {
    private static final EntityDataAccessor<String> ANIMATION =
            SynchedEntityData.defineId(IceMob.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> ANIMATION_START =
            SynchedEntityData.defineId(IceMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MOUNTED =
            SynchedEntityData.defineId(IceMob.class, EntityDataSerializers.BOOLEAN);

    private int attackCooldown;
    private LivingEntity attackTarget;
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.translatable("entity.tierborne.ice_knight"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private final Set<UUID> bossBarPlayers = new HashSet<>();

    public IceMob(EntityType<? extends IceMob> type, Level level) {
        super(type, level);
        xpReward = kind() == Kind.ICE_KNIGHT ? 40 : kind().isKnightMinion() ? 12 : 8;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ANIMATION, "");
        entityData.define(ANIMATION_START, 0);
        entityData.define(MOUNTED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.85D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide || !isAlive()) return;
        updateBossBar();

        String animation = entityData.get(ANIMATION);
        if (!animation.isEmpty()) {
            int elapsed = tickCount - getAnimationStartTick();
            tickAttack(animation, elapsed);
            if (elapsed >= animationTicks(animation)) finishAttack(animation);
            return;
        }

        if (attackCooldown > 0) attackCooldown--;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        double distance = distanceTo(target);
        if (attackCooldown <= 0 && hasLineOfSight(target) && canBeginAttack(distance)) {
            beginAttack(chooseAttack(distance), target);
        } else if (distance > preferredDistance()) {
            getNavigation().moveTo(target, movementSpeed());
        }
    }

    private boolean canBeginAttack(double distance) {
        return kind().isRanged() ? distance <= 20.0D : distance <= meleeRange();
    }

    private double preferredDistance() {
        return kind().isRanged() ? 10.0D : Math.max(2.0D, meleeRange() - 1.0D);
    }

    private double meleeRange() {
        return switch (kind()) {
            case ICE_KNIGHT -> 7.5D;
            case YETI, FROSTMITE, UNDEAD_ICE_WARRIOR -> 4.5D;
            case SNOWBALL_SPIRIT -> 2.5D;
            default -> 3.5D;
        };
    }

    private double movementSpeed() {
        return entityData.get(MOUNTED) ? 1.35D : kind() == Kind.SNOWBALL_SPIRIT ? 1.45D : 1.05D;
    }

    private String chooseAttack(double distance) {
        return switch (kind()) {
            case FROZEN_BLAZE -> "attack";
            case ICE_WITCH -> distance > 5.0D ? "range_attack" : "attack" + (random.nextInt(3) + 1);
            case ICEOLOGER -> random.nextInt(6) == 0 ? "attack2" : "attack";
            case YETI -> random.nextBoolean() ? "attack" : "attack2";
            case SNOWBALL_SPIRIT -> "charge";
            case ICE_KNIGHT -> chooseKnightAttack();
            case KNIGHT_SHIELD, KNIGHT_SPEAR, KNIGHT_SWORD -> random.nextBoolean() ? "attack1" : "attack2";
            case FROSTMITE, GNUT, UNDEAD_ICE_WARRIOR -> "attack";
        };
    }

    private String chooseKnightAttack() {
        if (!entityData.get(MOUNTED) && getHealth() <= getMaxHealth() * 0.5F) return "spawn_mount";
        if (entityData.get(MOUNTED)) {
            int choice = random.nextInt(5);
            return choice < 2 ? "swipe_front_mount" : choice < 4 ? "stab_mount" : "shockwave_mount";
        }
        int choice = random.nextInt(10);
        if (choice < 3) return "stab_forward";
        if (choice < 6) return "slash";
        if (choice < 8) return "shockwave";
        return "raise_spear";
    }

    private void beginAttack(String animation, LivingEntity target) {
        attackTarget = target;
        entityData.set(ANIMATION, animation);
        entityData.set(ANIMATION_START, tickCount);
        getNavigation().stop();
        playKnightAttackSound(animation);
        if (kind() == Kind.GNUT || kind() == Kind.SNOWBALL_SPIRIT) {
            Vec3 direction = target.position().subtract(position()).normalize();
            setDeltaMovement(direction.x * 0.6D, 0.3D, direction.z * 0.6D);
        }
    }

    private void playKnightAttackSound(String animation) {
        if (kind() != Kind.ICE_KNIGHT) return;
        SoundEvent sound = switch (animation) {
            case "spawn_mount" -> ModSounds.ICE_KNIGHT_SPAWN_MOUNT.get();
            case "raise_spear" -> ModSounds.ICE_KNIGHT_RAISE_SPEAR.get();
            case "shockwave", "shockwave_mount" -> ModSounds.ICE_KNIGHT_SHOCKWAVE.get();
            case "stab_forward", "stab_mount" -> ModSounds.ICE_KNIGHT_STAB_FORWARD.get();
            default -> ModSounds.ICE_KNIGHT_SLASH.get();
        };
        playSound(sound, 1.4F, 1.0F);
    }

    private void tickAttack(String animation, int elapsed) {
        if (attackTarget != null && attackTarget.isAlive() && elapsed < 12) {
            getLookControl().setLookAt(attackTarget, 30.0F, 30.0F);
        }
        if (elapsed == hitTick(animation)) performAttack(animation);
    }

    private int hitTick(String animation) {
        return switch (animation) {
            case "shockwave", "shockwave_mount" -> 20;
            case "raise_spear" -> 30;
            case "spawn_mount" -> 39;
            case "charge" -> 20;
            default -> 10;
        };
    }

    private void performAttack(String animation) {
        if (animation.equals("spawn_mount")) {
            entityData.set(MOUNTED, true);
            AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.30D);
            iceBurst(50, 1.2D);
            return;
        }
        if (animation.equals("raise_spear")) {
            summonKnightMinions();
            iceBurst(80, 2.0D);
            return;
        }
        if (animation.equals("shockwave") || animation.equals("shockwave_mount")) {
            areaAttack(10.0D, getAttackDamage() * 0.55F, 1.4D);
            iceBurst(100, 4.0D);
            playSound(SoundEvents.GENERIC_EXPLODE, 1.2F, 0.8F);
            return;
        }
        if (kind().isRanged() && (animation.equals("attack") || animation.equals("range_attack"))) {
            launchIceProjectile();
            return;
        }
        if (kind() == Kind.ICEOLOGER && animation.equals("attack2")) {
            summonSpirits(2);
            return;
        }
        if (kind() == Kind.SNOWBALL_SPIRIT) {
            areaAttack(4.0D, getAttackDamage(), 0.5D);
            iceBurst(30, 1.0D);
            discard();
            return;
        }
        double range = animation.contains("stab") ? 7.5D : meleeRange();
        areaAttack(range, getAttackDamage() * (entityData.get(MOUNTED) ? 1.2F : 1.0F), 0.35D);
    }

    private void launchIceProjectile() {
        if (attackTarget == null || !attackTarget.isAlive() || !(level instanceof ServerLevel server)) return;
        server.addFreshEntity(new IceProjectile(this, attackTarget, getAttackDamage()));
        playSound(SoundEvents.SNOWBALL_THROW, 1.0F, 0.8F);
    }

    private void areaAttack(double radius, float damage, double knockback) {
        AABB area = getBoundingBox().inflate(radius, 2.5D, radius);
        for (Player player : level.getEntitiesOfClass(Player.class, area,
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator())) {
            AABB playerHitbox = CombatHitboxes.standardPlayer(player);
            if (!area.intersects(playerHitbox)
                    || !CombatHitboxes.intersectsHorizontalRadius(position(), player, radius)
                    || !CombatHitboxes.hasLineOfSightToPlayer(this, player)) continue;
            player.hurt(DamageSource.mobAttack(this), damage);
            Vec3 away = player.position().subtract(position()).normalize().scale(knockback);
            player.push(away.x, Math.min(0.6D, knockback * 0.35D), away.z);
        }
    }

    private void summonSpirits(int count) {
        for (int index = 0; index < count; index++) {
            spawnMinion(ModEntities.SNOWBALL_SPIRIT.get(), index * Math.PI * 2.0D / count);
        }
    }

    private void summonKnightMinions() {
        List<EntityType<IceMob>> choices = List.of(ModEntities.ICE_KNIGHT_MINION_SWORD.get(),
                ModEntities.ICE_KNIGHT_MINION_SPEAR.get(), ModEntities.ICE_KNIGHT_MINION_SHIELD.get());
        for (int index = 0; index < 3; index++) {
            spawnMinion(choices.get(random.nextInt(choices.size())), index * Math.PI * 2.0D / 3.0D);
        }
    }

    private void spawnMinion(EntityType<IceMob> type, double angle) {
        if (!(level instanceof ServerLevel server)) return;
        IceMob minion = type.create(server);
        if (minion == null) return;
        minion.finalizeSpawn(server, server.getCurrentDifficultyAt(blockPosition()), MobSpawnType.EVENT, null, null);
        minion.moveTo(getX() + Math.cos(angle) * 3.0D, getY() + 0.25D,
                getZ() + Math.sin(angle) * 3.0D, getYRot(), 0.0F);
        minion.setTarget(getTarget());
        minion.setPersistenceRequired();
        minion.getPersistentData().putBoolean("tierborne:dungeon_marker_spawn", true);
        server.addFreshEntity(minion);
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()),
                minion.getX(), minion.getY() + 1.0D, minion.getZ(), 35, 0.5D, 0.8D, 0.5D, 0.05D);
    }

    private void iceBurst(int count, double spread) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    count, spread, spread * 0.5D, spread, 0.05D);
        }
    }

    private float getAttackDamage() {
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private void finishAttack(String animation) {
        entityData.set(ANIMATION, "");
        attackTarget = null;
        attackCooldown = kind() == Kind.ICE_KNIGHT ? 30 : kind().isRanged() ? 35 : 22;
    }

    private int animationTicks(String animation) {
        return switch (animation) {
            case "raise_spear" -> 120;
            case "slash" -> 30;
            case "attack2" -> kind() == Kind.ICEOLOGER ? 30
                    : kind() == Kind.ICE_WITCH ? 10 : 22;
            case "attack1", "attack3" -> kind() == Kind.ICE_WITCH ? 10 : 22;
            case "range_attack" -> 20;
            case "spawn_mount", "shockwave", "stab_forward", "swipe_front_mount", "charge" -> 40;
            case "shockwave_mount", "stab_mount" -> 30;
            default -> 22;
        };
    }

    private void updateBossBar() {
        if (kind() != Kind.ICE_KNIGHT || !(level instanceof ServerLevel server)) return;
        bossBar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
        Set<UUID> desired = new HashSet<>();
        if (getTarget() != null) {
            Optional<DungeonSavedData.Instance> instance = DungeonManager.instanceAt(server.getServer(), getX(), getZ());
            if (instance.isPresent()) {
                for (UUID playerId : instance.get().party) {
                    ServerPlayer player = server.getServer().getPlayerList().getPlayer(playerId);
                    if (player != null && player.level == server) {
                        desired.add(playerId);
                        if (bossBarPlayers.add(playerId)) bossBar.addPlayer(player);
                    }
                }
            }
        }
        for (UUID playerId : new ArrayList<>(bossBarPlayers)) {
            if (desired.contains(playerId)) continue;
            ServerPlayer player = server.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) bossBar.removePlayer(player);
            bossBarPlayers.remove(playerId);
        }
    }

    private void clearBossBar() {
        bossBar.removeAllPlayers();
        bossBarPlayers.clear();
    }

    @Override
    public void die(DamageSource source) {
        entityData.set(ANIMATION, "death");
        entityData.set(ANIMATION_START, tickCount);
        clearBossBar();
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        clearBossBar();
        super.remove(reason);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        int duration = kind() == Kind.ICE_KNIGHT ? 60
                : kind().isKnightMinion() ? 50
                : kind() == Kind.ICE_WITCH ? 100
                : kind() == Kind.YETI ? 40 : 20;
        if (deathTime >= duration && !level.isClientSide && !isRemoved()) {
            level.broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return kind() == Kind.FROZEN_BLAZE ? SoundEvents.VEX_AMBIENT : SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        if (kind() == Kind.ICE_KNIGHT) {
            if (entityData.get(MOUNTED)) {
                return random.nextBoolean() ? ModSounds.ICE_KNIGHT_MOUNT_HURT_1.get()
                        : ModSounds.ICE_KNIGHT_MOUNT_HURT_2.get();
            }
            return random.nextBoolean() ? ModSounds.ICE_KNIGHT_HURT_1.get()
                    : ModSounds.ICE_KNIGHT_HURT_2.get();
        }
        if (kind().isKnightMinion()) {
            return random.nextBoolean() ? ModSounds.ICE_KNIGHT_MINION_HURT_1.get()
                    : ModSounds.ICE_KNIGHT_MINION_HURT_2.get();
        }
        return SoundEvents.GLASS_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        if (kind() == Kind.ICE_KNIGHT) return ModSounds.ICE_KNIGHT_DEATH.get();
        if (kind().isKnightMinion()) return ModSounds.ICE_KNIGHT_MINION_DEATH.get();
        return SoundEvents.GLASS_BREAK;
    }

    @Override
    public String getAnimationName() {
        if (deathTime > 0) return entityData.get(MOUNTED) ? "death_mount" : "death";
        String active = entityData.get(ANIMATION);
        if (!active.isEmpty()) {
            if (active.equals("attack1") && kind() == Kind.ICE_WITCH) return "attack";
            if (active.equals("attack2") && kind() == Kind.ICE_WITCH) return "attack2";
            if (active.equals("attack3") && kind() == Kind.ICE_WITCH) return "attack3";
            return active;
        }
        if (kind() == Kind.FROZEN_BLAZE) return "spin";
        boolean walking = getDeltaMovement().horizontalDistanceSqr() > 0.0025D;
        if (entityData.get(MOUNTED)) return walking ? "walk_mount" : "idle_mount";
        return walking ? "walk" : "idle";
    }

    @Override
    public int getAnimationStartTick() {
        return entityData.get(ANIMATION_START);
    }

    @Override
    public boolean hasActiveAttackAnimation() {
        return !entityData.get(ANIMATION).isEmpty() || deathTime > 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Mounted", entityData.get(MOUNTED));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(MOUNTED, tag.getBoolean("Mounted"));
    }

    public Kind kind() {
        EntityType<?> type = getType();
        if (type == ModEntities.FROZEN_BLAZE.get()) return Kind.FROZEN_BLAZE;
        if (type == ModEntities.GNUT.get()) return Kind.GNUT;
        if (type == ModEntities.ICE_WITCH.get()) return Kind.ICE_WITCH;
        if (type == ModEntities.ICEOLOGER.get()) return Kind.ICEOLOGER;
        if (type == ModEntities.SNOWBALL_SPIRIT.get()) return Kind.SNOWBALL_SPIRIT;
        if (type == ModEntities.UNDEAD_ICE_WARRIOR.get()) return Kind.UNDEAD_ICE_WARRIOR;
        if (type == ModEntities.TARTARUS_YETI.get()) return Kind.YETI;
        if (type == ModEntities.ICE_KNIGHT_MINION_SHIELD.get()) return Kind.KNIGHT_SHIELD;
        if (type == ModEntities.ICE_KNIGHT_MINION_SPEAR.get()) return Kind.KNIGHT_SPEAR;
        if (type == ModEntities.ICE_KNIGHT_MINION_SWORD.get()) return Kind.KNIGHT_SWORD;
        if (type == ModEntities.ICE_KNIGHT.get()) return Kind.ICE_KNIGHT;
        return Kind.FROSTMITE;
    }

    public enum Kind {
        FROSTMITE,
        FROZEN_BLAZE,
        GNUT,
        ICE_WITCH,
        ICEOLOGER,
        SNOWBALL_SPIRIT,
        UNDEAD_ICE_WARRIOR,
        YETI,
        KNIGHT_SHIELD,
        KNIGHT_SPEAR,
        KNIGHT_SWORD,
        ICE_KNIGHT;

        private boolean isRanged() {
            return this == FROZEN_BLAZE || this == ICE_WITCH || this == ICEOLOGER;
        }

        private boolean isKnightMinion() {
            return this == KNIGHT_SHIELD || this == KNIGHT_SPEAR || this == KNIGHT_SWORD;
        }
    }
}
