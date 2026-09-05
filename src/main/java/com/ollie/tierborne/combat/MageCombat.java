package com.ollie.tierborne.combat;

import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.data.PlayerProgress;
import com.ollie.tierborne.data.PlayerProgressSavedData;
import com.ollie.tierborne.entity.FireballProjectile;
import com.ollie.tierborne.entity.MageVfxEntity;
import com.ollie.tierborne.item.ModItems;
import com.ollie.tierborne.network.ModNetwork;
import com.ollie.tierborne.playerclass.MagePlayerClass;
import com.ollie.tierborne.registry.ModAttributes;
import com.ollie.tierborne.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MageCombat {
    private static final Map<UUID, State> STATES = new HashMap<>();

    private MageCombat() {
    }

    public static boolean input(ServerPlayer player, String attackId) {
        if (!isMageAttack(attackId)) return false;
        if (!holdingStaff(player)) {
            player.displayClientMessage(Component.literal("Mage spells require a Mage Staff in your main hand."), true);
            return true;
        }
        State state = state(player);
        long now = player.level.getGameTime();
        long readyAt = state.cooldowns.getOrDefault(attackId, 0L);
        if (readyAt > now) {
            player.displayClientMessage(Component.literal(displayName(attackId) + " is ready in "
                    + String.format(java.util.Locale.ROOT, "%.1f", (readyAt - now) / 20.0D) + "s."), true);
            return true;
        }
        switch (attackId) {
            case "fireball_volley" -> fireballVolley(player);
            case "flame_ring" -> flameRing(player);
            case "ice_lance" -> iceLance(player);
            case "frost_nova" -> frostNova(player);
            case "venom_bolt" -> venomBolt(player);
            case "toxic_cloud" -> toxicCloud(player, state, now);
            case "chain_lightning" -> chainLightning(player);
            case "thunderstep" -> thunderstep(player);
            case "healing_pulse" -> healingPulse(player);
            case "purge" -> purge(player);
            case "adrenaline" -> adrenaline(player);
            default -> {
                return false;
            }
        }
        int duration = cooldownTicks(player, attackId);
        state.cooldowns.put(attackId, now + duration);
        state.cooldownDurations.put(attackId, duration);
        player.displayClientMessage(Component.literal("Cast " + displayName(attackId) + "."), true);
        return true;
    }

    public static boolean castStaffBolt(ServerPlayer player, ItemStack staff) {
        PlayerProgress progress = progress(player);
        if (!MagePlayerClass.ID.equals(progress.playerClassId())) {
            player.displayClientMessage(Component.literal("Only a Mage can channel the Mage Staff."), true);
            return false;
        }
        if (player.getCooldowns().isOnCooldown(staff.getItem())) return false;
        int cooldown = RpgBalanceConfig.ticks(progress.hasSkill(MagePlayerClass.QUICK_FOCUS)
                ? RpgBalanceConfig.MAGE_STAFF_QUICK_COOLDOWN_SECONDS
                : RpgBalanceConfig.MAGE_STAFF_COOLDOWN_SECONDS);
        player.getCooldowns().addCooldown(staff.getItem(), Math.max(1, cooldown));
        ModNetwork.syncMageCast(player, 9, 0);

        double range = RpgBalanceConfig.MAGE_STAFF_RANGE.get();
        LivingEntity target = firstTarget(player, range);
        Vec3 end = target == null ? clippedEnd(player, range) : target.getEyePosition();
        ParticleOptions particle = staffParticle(progress);
        beam(serverLevel(player), player.getEyePosition().add(0.0D, -0.15D, 0.0D), end, particle, 0.35D);
        player.level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.75F, 1.45F);

        if (progress.hasSkill(MagePlayerClass.FIRE_MAGE)) {
            vfx(player, end, MageVfxEntity.FIRE_EXPLOSION, 16, player.getYRot(), 0.0F);
        } else if (progress.hasSkill(MagePlayerClass.ICE_MAGE)) {
            vfx(player, end, MageVfxEntity.GLACIAL_SPIKE, 24, player.getYRot(), player.getXRot());
        } else if (progress.hasSkill(MagePlayerClass.LIGHTNING_MAGE)) {
            vfx(player, end, MageVfxEntity.THUNDER_STRIKE, 12, player.getYRot(), 0.0F);
        }

        if (progress.hasSkill(MagePlayerClass.DOCTOR)) {
            LivingEntity patient = target instanceof Player ? target : player;
            patient.heal((float) healing(player, 3.0D));
            burst(serverLevel(player), patient.position().add(0.0D, 1.0D, 0.0D),
                    ParticleTypes.HAPPY_VILLAGER, 14, 0.45D);
            return true;
        }
        if (target == null || target instanceof Player) return true;
        if (progress.hasSkill(MagePlayerClass.FIRE_MAGE)) {
            damage(player, target, RpgBalanceConfig.MAGE_STAFF_DAMAGE.get(), Element.FIRE);
            ElementalCombat.applyFire(player, target, RpgBalanceConfig.FIRE_MAGE_BURN_SECONDS.get());
        } else if (progress.hasSkill(MagePlayerClass.ICE_MAGE)) {
            damage(player, target, RpgBalanceConfig.MAGE_STAFF_DAMAGE.get(), Element.ICE);
            ElementalCombat.applyIce(player, target,
                    RpgBalanceConfig.ticks(RpgBalanceConfig.ICE_MAGE_SLOW_SECONDS), 1, 0);
        } else if (progress.hasSkill(MagePlayerClass.POISON_MAGE)) {
            damage(player, target, RpgBalanceConfig.MAGE_STAFF_DAMAGE.get(), Element.POISON);
            ElementalCombat.applyPoison(target,
                    RpgBalanceConfig.ticks(RpgBalanceConfig.POISON_MAGE_POISON_SECONDS), 0);
        } else if (progress.hasSkill(MagePlayerClass.LIGHTNING_MAGE)) {
            damage(player, target, ElementalCombat.lightningDamage(
                    target, RpgBalanceConfig.MAGE_STAFF_DAMAGE.get()), Element.LIGHTNING);
            LivingEntity jump = nearestEnemy(player, target.position(), 5.0D, Set.of(target.getUUID()));
            if (jump != null) {
                beam(serverLevel(player), target.getEyePosition(), jump.getEyePosition(),
                        ParticleTypes.ELECTRIC_SPARK, 0.3D);
                damage(player, jump, ElementalCombat.lightningDamage(
                        jump, RpgBalanceConfig.MAGE_STAFF_DAMAGE.get() * 0.6D), Element.LIGHTNING);
            }
        } else {
            damage(player, target, RpgBalanceConfig.MAGE_STAFF_DAMAGE.get());
        }
        return true;
    }

    public static void tick(ServerPlayer player) {
        PlayerProgress progress = progress(player);
        if (progress.hasSkill(MagePlayerClass.ICE_MAGE) && player.getTicksFrozen() > 0) {
            player.setTicksFrozen(Math.max(0, player.getTicksFrozen() - 3));
        }
        State state = STATES.get(player.getUUID());
        if (state == null || state.cloudUntil <= 0L) return;
        long now = player.level.getGameTime();
        if (now >= state.cloudUntil || !player.isAlive()) {
            state.cloudUntil = 0L;
            return;
        }
        if (now % 5L == 0L) {
            double radius = toxicCloudRadius(progress);
            burst(serverLevel(player), state.cloudCenter.add(0.0D, 0.8D, 0.0D),
                    ParticleTypes.SPORE_BLOSSOM_AIR, 22, radius * 0.55D);
        }
        if (now % 20L != 0L) return;
        double radius = toxicCloudRadius(progress);
        for (LivingEntity target : enemies(player, state.cloudCenter, radius)) {
            damage(player, target, RpgBalanceConfig.TOXIC_CLOUD_DAMAGE.get()
                    * (progress.hasSkill(MagePlayerClass.PLAGUE_CLOUD) ? 1.4D : 1.0D), Element.POISON);
            ElementalCombat.applyPoison(target, 50,
                    progress.hasSkill(MagePlayerClass.VIRULENT_VENOM) ? 1 : 0);
        }
    }

    public static List<AbilityStatus> statuses(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) return List.of();
        long now = player.level.getGameTime();
        List<AbilityStatus> result = new ArrayList<>();
        state.cooldowns.forEach((id, ready) -> {
            int remaining = (int) Math.max(0L, ready - now);
            if (remaining > 0) result.add(new AbilityStatus(displayName(id), remaining,
                    Math.max(1, state.cooldownDurations.getOrDefault(id, remaining)), false, "COOLDOWN"));
        });
        if (state.cloudUntil > now) result.add(new AbilityStatus("Toxic Cloud",
                (int) (state.cloudUntil - now), Math.max(1, state.cloudDuration), true, "ACTIVE"));
        return result;
    }

    public static void reset(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }

    private static void fireballVolley(ServerPlayer player) {
        PlayerProgress progress = progress(player);
        int count = progress.hasSkill(MagePlayerClass.VOLLEY_MASTERY) ? 5 : 3;
        double damage = RpgBalanceConfig.FIREBALL_VOLLEY_DAMAGE.get()
                * (progress.hasSkill(MagePlayerClass.VOLLEY_MASTERY) ? 1.25D : 1.0D);
        Vec3 look = player.getLookAngle();
        vfx(player, player.getEyePosition().add(look.scale(0.7D)), MageVfxEntity.BIG_FIREBALL_CHARGE,
                56, player.getYRot(), player.getXRot());
        for (int variant = 0; variant < 7; variant++) {
            double angle = Math.PI * 2.0D * variant / 7.0D;
            Vec3 circle = player.position().add(Math.cos(angle) * 1.3D, 1.2D + (variant % 2) * 0.7D,
                    Math.sin(angle) * 1.3D);
            vfx(player, circle, MageVfxEntity.FIRE_CIRCLE, 32,
                    (float) Math.toDegrees(angle), 0.0F, variant);
        }
        for (int index = 0; index < count; index++) {
            double offset = (index - (count - 1) / 2.0D) * 0.09D;
            FireballProjectile projectile = new FireballProjectile(player, (float) damage,
                    0.0F, 1.0F, 1.0F, index);
            double cosine = Math.cos(offset);
            double sine = Math.sin(offset);
            projectile.setDeltaMovement((look.x * cosine - look.z * sine), look.y,
                    (look.x * sine + look.z * cosine));
            serverLevel(player).addFreshEntity(projectile);
        }
        spellSound(player, ModSounds.MAGE_FIRE_CIRCLE.get(), 0.9F, 1.0F);
        spellSound(player, ModSounds.MAGE_FIRE_BALL.get(), 1.0F, 1.0F);
        cast(player, 16, 0, ParticleTypes.FLAME, SoundEvents.BLAZE_SHOOT);
    }

    private static void flameRing(ServerPlayer player) {
        PlayerProgress progress = progress(player);
        double radius = RpgBalanceConfig.FLAME_RING_RADIUS.get()
                + (progress.hasSkill(MagePlayerClass.INFERNO_CORE) ? 2.0D : 0.0D);
        for (LivingEntity target : enemies(player, player.position(), radius)) {
            damage(player, target, RpgBalanceConfig.FLAME_RING_DAMAGE.get(), Element.FIRE);
            ElementalCombat.applyFire(player, target, RpgBalanceConfig.FIRE_MAGE_BURN_SECONDS.get()
                    + (progress.hasSkill(MagePlayerClass.INFERNO_CORE) ? 3.0D : 0.0D));
            knockAway(player.position(), target, 0.8D);
        }
        ring(serverLevel(player), player.position().add(0.0D, 0.2D, 0.0D), radius, ParticleTypes.FLAME);
        vfx(player, player.position().add(0.0D, 1.0D, 0.0D), MageVfxEntity.METEOR,
                70, player.getYRot(), -65.0F);
        vfx(player, player.position(), MageVfxEntity.METEOR_CROSS, 108, player.getYRot(), 0.0F);
        vfx(player, player.position(), MageVfxEntity.RUPTURE, 54, player.getYRot(), 0.0F);
        for (int index = 0; index < 5; index++) {
            double angle = Math.PI * 2.0D * index / 5.0D;
            Vec3 impact = player.position().add(Math.cos(angle) * radius * 0.55D, 0.1D,
                    Math.sin(angle) * radius * 0.55D);
            vfx(player, impact, MageVfxEntity.RUBBLE, 46,
                    (float) Math.toDegrees(angle), 0.0F);
            vfx(player, impact, MageVfxEntity.FIRE_EXPLOSION, 16,
                    (float) Math.toDegrees(angle), 0.0F);
        }
        spellSound(player, ModSounds.MAGE_METEOR_CREATE.get(), 1.0F, 1.0F);
        spellSound(player, ModSounds.MAGE_METEOR_SHOOT.get(), 0.9F, 1.0F);
        spellSound(player, ModSounds.MAGE_METEOR_EXPLODE.get(), 1.0F, 1.0F);
        spellSound(player, ModSounds.MAGE_FIRE_BLAST.get(), 0.9F, 0.8F);
        cast(player, 18, 1, ParticleTypes.LAVA, SoundEvents.FIRECHARGE_USE);
    }

    private static void iceLance(ServerPlayer player) {
        PlayerProgress progress = progress(player);
        double range = RpgBalanceConfig.ICE_LANCE_RANGE.get();
        List<LivingEntity> targets = progress.hasSkill(MagePlayerClass.PIERCING_LANCE)
                ? targetsAlongRay(player, range, 1.1D) : firstTargetList(player, range);
        Vec3 end = targets.isEmpty() ? clippedEnd(player, range) :
                (progress.hasSkill(MagePlayerClass.PIERCING_LANCE)
                        ? clippedEnd(player, range) : targets.get(0).getEyePosition());
        beam(serverLevel(player), player.getEyePosition(), end, ParticleTypes.SNOWFLAKE, 0.25D);
        Vec3 look = player.getLookAngle();
        vfx(player, player.getEyePosition().add(look.scale(1.5D)), MageVfxEntity.HAIL_INHALE,
                30, player.getYRot(), player.getXRot());
        vfx(player, player.position().add(look.scale(3.0D)), MageVfxEntity.HAIL_SPIKE_CENTER,
                44, player.getYRot(), player.getXRot());
        vfx(player, player.position().add(look.scale(8.0D)), MageVfxEntity.HAIL_SPIKE_LEFT,
                44, player.getYRot(), player.getXRot());
        vfx(player, player.position().add(look.scale(13.0D)), MageVfxEntity.HAIL_SPIKE_RIGHT,
                44, player.getYRot(), player.getXRot());
        vfx(player, end, MageVfxEntity.GLACIAL_SPIKE, 24,
                player.getYRot(), player.getXRot());
        for (LivingEntity target : targets) {
            damage(player, target, shatterDamage(player, target,
                    RpgBalanceConfig.ICE_LANCE_DAMAGE.get()), Element.ICE);
            ElementalCombat.applyIce(player, target, 70, 3, 180);
        }
        spellSound(player, ModSounds.MAGE_ICE_CHARGE.get(), 1.0F, 1.0F);
        spellSound(player, ModSounds.MAGE_ICE_SPIKE.get(), 1.0F, 1.0F);
        cast(player, 14, 0, ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK);
    }

    private static void frostNova(ServerPlayer player) {
        double radius = RpgBalanceConfig.FROST_NOVA_RADIUS.get();
        for (LivingEntity target : enemies(player, player.position(), radius)) {
            damage(player, target, shatterDamage(player, target,
                    RpgBalanceConfig.FROST_NOVA_DAMAGE.get()), Element.ICE);
            ElementalCombat.applyIce(player, target, 80, 4, 220);
            vfx(player, target.position(), MageVfxEntity.CRYO_CAGE, 80,
                    target.getYRot(), 0.0F);
        }
        vfx(player, player.position(), MageVfxEntity.CRYO_PRISON, 114,
                player.getYRot(), 0.0F);
        spellSound(player, ModSounds.MAGE_ICE_CHARGE.get(), 1.0F, 0.8F);
        spellSound(player, ModSounds.MAGE_ICE_BREAK.get(), 1.0F, 1.0F);
        ring(serverLevel(player), player.position().add(0.0D, 0.3D, 0.0D), radius, ParticleTypes.SNOWFLAKE);
        cast(player, 20, 1, ParticleTypes.ITEM_SNOWBALL, SoundEvents.GLASS_BREAK);
    }

    private static void venomBolt(ServerPlayer player) {
        LivingEntity target = firstTarget(player, RpgBalanceConfig.VENOM_BOLT_RANGE.get());
        Vec3 end = target == null ? clippedEnd(player, RpgBalanceConfig.VENOM_BOLT_RANGE.get()) : target.getEyePosition();
        beam(serverLevel(player), player.getEyePosition(), end, ParticleTypes.ENTITY_EFFECT, 0.3D);
        if (target != null && !(target instanceof Player)) {
            damage(player, target, RpgBalanceConfig.VENOM_BOLT_DAMAGE.get(), Element.POISON);
            boolean virulent = progress(player).hasSkill(MagePlayerClass.VIRULENT_VENOM);
            ElementalCombat.applyPoison(target,
                    RpgBalanceConfig.ticksValue(virulent ? 9.0D : 6.0D), virulent ? 1 : 0);
        }
        cast(player, 14, 0, ParticleTypes.SPORE_BLOSSOM_AIR, SoundEvents.SLIME_SQUISH);
    }

    private static void toxicCloud(ServerPlayer player, State state, long now) {
        Vec3 target = clippedEnd(player, 18.0D);
        state.cloudCenter = target;
        state.cloudDuration = RpgBalanceConfig.ticksValue(RpgBalanceConfig.TOXIC_CLOUD_DURATION_SECONDS.get()
                + (progress(player).hasSkill(MagePlayerClass.PLAGUE_CLOUD) ? 4.0D : 0.0D));
        state.cloudUntil = now + state.cloudDuration;
        burst(serverLevel(player), target.add(0.0D, 0.8D, 0.0D), ParticleTypes.SPORE_BLOSSOM_AIR, 60,
                toxicCloudRadius(progress(player)) * 0.5D);
        cast(player, 18, 1, ParticleTypes.ENTITY_EFFECT, SoundEvents.BREWING_STAND_BREW);
    }

    private static void chainLightning(ServerPlayer player) {
        PlayerProgress progress = progress(player);
        LivingEntity current = firstTarget(player, 24.0D);
        if (current == null || current instanceof Player) {
            player.displayClientMessage(Component.literal("Chain Lightning found no enemy in sight."), true);
            return;
        }
        int jumps = progress.hasSkill(MagePlayerClass.CONDUCTIVITY) ? 5 : 3;
        double range = RpgBalanceConfig.CHAIN_LIGHTNING_RANGE.get()
                + (progress.hasSkill(MagePlayerClass.CONDUCTIVITY) ? 3.0D : 0.0D);
        Set<UUID> struck = new HashSet<>();
        Vec3 from = player.getEyePosition();
        for (int index = 0; index < jumps && current != null; index++) {
            beam(serverLevel(player), from, current.getEyePosition(), ParticleTypes.ELECTRIC_SPARK, 0.22D);
            vfx(player, current.position(), MageVfxEntity.THUNDER_STRIKE, 12,
                    current.getYRot(), 0.0F, index % 4);
            double strikeDamage = RpgBalanceConfig.CHAIN_LIGHTNING_DAMAGE.get() * Math.pow(0.85D, index);
            damage(player, current, ElementalCombat.lightningDamage(current, strikeDamage), Element.LIGHTNING);
            current.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
            struck.add(current.getUUID());
            from = current.getEyePosition();
            current = nearestEnemy(player, current.position(), range, struck);
        }
        spellSound(player, ModSounds.MAGE_THUNDER_STRIKE.get(), 1.0F, 1.0F);
        cast(player, 16, 0, ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_IMPACT);
    }

    private static void thunderstep(ServerPlayer player) {
        PlayerProgress progress = progress(player);
        double distance = RpgBalanceConfig.THUNDERSTEP_RANGE.get()
                + (progress.hasSkill(MagePlayerClass.OVERCHARGE) ? 4.0D : 0.0D);
        Vec3 start = player.position();
        Vec3 destination = clippedEnd(player, distance).subtract(player.getLookAngle().normalize().scale(0.8D));
        double damage = RpgBalanceConfig.THUNDERSTEP_DAMAGE.get()
                * (progress.hasSkill(MagePlayerClass.OVERCHARGE) ? 1.4D : 1.0D);
        vfx(player, start, MageVfxEntity.THUNDER_TELEPORT, 18, player.getYRot(), 0.0F);
        vfx(player, start, MageVfxEntity.THUNDER_EXPLOSION, 18, player.getYRot(), 0.0F);
        shockArea(player, start, damage);
        player.teleportTo(destination.x, destination.y, destination.z);
        if (!player.level.noCollision(player)) {
            player.teleportTo(start.x, start.y, start.z);
            player.displayClientMessage(Component.literal(
                    "Thunderstep destination was blocked; you returned to your starting position."), true);
        }
        shockArea(player, player.position(), damage);
        vfx(player, player.position(), MageVfxEntity.THUNDER_TELEPORT, 18,
                player.getYRot(), 0.0F);
        vfx(player, player.position(), MageVfxEntity.THUNDER_EXPLOSION, 18,
                player.getYRot(), 0.0F);
        spellSound(player, ModSounds.MAGE_THUNDER_TELEPORT.get(), 1.0F, 1.0F);
        beam(serverLevel(player), start.add(0.0D, 1.0D, 0.0D), player.position().add(0.0D, 1.0D, 0.0D),
                ParticleTypes.ELECTRIC_SPARK, 0.3D);
        cast(player, 12, 2, ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_THUNDER);
    }

    private static void healingPulse(ServerPlayer player) {
        PlayerProgress progress = progress(player);
        double radius = doctorRadius(progress, RpgBalanceConfig.HEALING_PULSE_RADIUS.get());
        double amount = healing(player, RpgBalanceConfig.HEALING_PULSE_HEALTH.get()
                * (progress.hasSkill(MagePlayerClass.TRIAGE) ? 1.35D : 1.0D));
        for (Player patient : player.level.getEntitiesOfClass(Player.class,
                new AABB(player.position(), player.position()).inflate(radius), Player::isAlive)) {
            double adjusted = progress.hasSkill(MagePlayerClass.TRIAGE)
                    && patient.getHealth() < patient.getMaxHealth() * 0.4F ? amount * 1.5D : amount;
            patient.heal((float) adjusted);
            burst(serverLevel(player), patient.position().add(0.0D, 1.0D, 0.0D),
                    ParticleTypes.HEART, 8, 0.35D);
        }
        cast(player, 18, 1, ParticleTypes.HAPPY_VILLAGER, SoundEvents.PLAYER_LEVELUP);
    }

    private static void purge(ServerPlayer player) {
        PlayerProgress progress = progress(player);
        double radius = doctorRadius(progress, RpgBalanceConfig.PURGE_RADIUS.get());
        for (Player patient : player.level.getEntitiesOfClass(Player.class,
                new AABB(player.position(), player.position()).inflate(radius), Player::isAlive)) {
            ElementalCombat.cleanse(patient);
            burst(serverLevel(player), patient.position().add(0.0D, 1.0D, 0.0D),
                    ParticleTypes.END_ROD, 12, 0.4D);
        }
        for (LivingEntity target : enemies(player, player.position(), radius)) {
            if (target.getMobType() == MobType.UNDEAD) damage(player, target, RpgBalanceConfig.PURGE_DAMAGE.get());
        }
        spellSound(player, ModSounds.MAGE_BARRIER_BREAK.get(), 0.9F, 1.2F);
        cast(player, 20, 1, ParticleTypes.END_ROD, SoundEvents.TOTEM_USE);
    }

    private static void adrenaline(ServerPlayer player) {
        PlayerProgress progress = progress(player);
        double radius = doctorRadius(progress, RpgBalanceConfig.HEALING_PULSE_RADIUS.get());
        int duration = RpgBalanceConfig.ticks(RpgBalanceConfig.ADRENALINE_DURATION_SECONDS);
        for (Player patient : player.level.getEntitiesOfClass(Player.class,
                new AABB(player.position(), player.position()).inflate(radius), Player::isAlive)) {
            patient.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0));
            patient.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0));
            patient.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0));
            burst(serverLevel(player), patient.position().add(0.0D, 1.0D, 0.0D),
                    ParticleTypes.WAX_ON, 14, 0.45D);
        }
        cast(player, 20, 1, ParticleTypes.WAX_ON, SoundEvents.BEACON_POWER_SELECT);
    }

    private static void shockArea(ServerPlayer player, Vec3 center, double damage) {
        double radius = RpgBalanceConfig.THUNDERSTEP_RADIUS.get();
        for (LivingEntity target : enemies(player, center, radius)) {
            damage(player, target, ElementalCombat.lightningDamage(target, damage), Element.LIGHTNING);
            knockAway(center, target, 0.65D);
        }
        ring(serverLevel(player), center.add(0.0D, 0.25D, 0.0D), radius, ParticleTypes.ELECTRIC_SPARK);
    }

    private static double shatterDamage(ServerPlayer player, LivingEntity target, double base) {
        if (progress(player).hasSkill(MagePlayerClass.SHATTER)
                && target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            return base * (1.0D + RpgBalanceConfig.ICE_SHATTER_BONUS_PERCENT.get() / 100.0D);
        }
        return base;
    }

    private static double healing(ServerPlayer player, double base) {
        return base * (1.0D + RpgBalanceConfig.DOCTOR_HEALING_BONUS_PERCENT.get() / 100.0D);
    }

    private static void damage(ServerPlayer player, LivingEntity target, double base) {
        damage(player, target, base, null);
    }

    private static void damage(ServerPlayer player, LivingEntity target, double base, Element element) {
        if (target == player || target instanceof Player) return;
        float elementalBase = element == null ? (float) base
                : ElementalCombat.modifyDamage(target, element, (float) base);
        float amount = (float) (elementalBase * player.getAttributeValue(ModAttributes.MAGIC_DAMAGE.get()));
        target.hurt(net.minecraft.world.damagesource.DamageSource.indirectMagic(player, player), amount);
    }

    private static List<LivingEntity> enemies(ServerPlayer player, Vec3 center, double radius) {
        return player.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius), target -> target.isAlive()
                        && target != player && !(target instanceof Player));
    }

    private static LivingEntity firstTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = clippedEnd(player, range);
        Vec3 ray = end.subtract(start);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end,
                player.getBoundingBox().expandTowards(ray).inflate(1.0D),
                entity -> entity instanceof LivingEntity living && living.isAlive()
                        && entity != player && entity.isPickable(), ray.lengthSqr());
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static List<LivingEntity> firstTargetList(ServerPlayer player, double range) {
        LivingEntity target = firstTarget(player, range);
        return target == null || target instanceof Player ? List.of() : List.of(target);
    }

    private static List<LivingEntity> targetsAlongRay(ServerPlayer player, double range, double width) {
        Vec3 start = player.getEyePosition();
        Vec3 end = clippedEnd(player, range);
        Vec3 ray = end.subtract(start);
        double lengthSquared = ray.lengthSqr();
        return player.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(start, end).inflate(width), target -> target.isAlive()
                                && target != player && !(target instanceof Player))
                .stream().filter(target -> {
                    double projection = target.getEyePosition().subtract(start).dot(ray) / lengthSquared;
                    if (projection < 0.0D || projection > 1.0D) return false;
                    return target.getEyePosition().distanceTo(start.add(ray.scale(projection))) <= width;
                }).sorted(Comparator.comparingDouble(player::distanceToSqr)).toList();
    }

    private static LivingEntity nearestEnemy(ServerPlayer player, Vec3 center, double radius, Set<UUID> excluded) {
        return enemies(player, center, radius).stream().filter(target -> !excluded.contains(target.getUUID()))
                .filter(player::hasLineOfSight).min(Comparator.comparingDouble(target -> target.distanceToSqr(center)))
                .orElse(null);
    }

    private static Vec3 clippedEnd(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        HitResult hit = player.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    private static void cast(ServerPlayer player, int duration, int style,
                             ParticleOptions particle, net.minecraft.sounds.SoundEvent sound) {
        ModNetwork.syncMageCast(player, duration, style);
        burst(serverLevel(player), player.position().add(0.0D, 1.0D, 0.0D), particle, 20, 0.55D);
        player.level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.9F, 1.1F);
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    private static void vfx(ServerPlayer player, Vec3 position, int effect, int duration,
                            float yaw, float pitch) {
        vfx(player, position, effect, duration, yaw, pitch, 0);
    }

    private static void vfx(ServerPlayer player, Vec3 position, int effect, int duration,
                            float yaw, float pitch, int variant) {
        MageVfxEntity.spawn(serverLevel(player), position, effect, duration, yaw, pitch, variant);
    }

    private static void spellSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound,
                                   float volume, float pitch) {
        player.level.playSound(null, player.blockPosition(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }

    private static void beam(ServerLevel level, Vec3 start, Vec3 end,
                             ParticleOptions particle, double spacing) {
        double distance = start.distanceTo(end);
        int steps = Math.max(1, (int) Math.ceil(distance / spacing));
        for (int index = 0; index <= steps; index++) {
            Vec3 point = start.lerp(end, index / (double) steps);
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.025D, 0.025D, 0.025D, 0.0D);
        }
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle) {
        int points = Math.max(28, (int) Math.round(radius * 12.0D));
        for (int index = 0; index < points; index++) {
            double angle = index * Math.PI * 2.0D / points;
            level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y,
                    center.z + Math.sin(angle) * radius, 2, 0.06D, 0.12D, 0.06D, 0.01D);
        }
    }

    private static void burst(ServerLevel level, Vec3 center, ParticleOptions particle,
                              int count, double spread) {
        level.sendParticles(particle, center.x, center.y, center.z, count,
                spread, spread, spread, 0.04D);
    }

    private static void knockAway(Vec3 center, LivingEntity target, double strength) {
        Vec3 direction = target.position().subtract(center).normalize().scale(strength);
        target.push(direction.x, 0.25D, direction.z);
    }

    private static ParticleOptions staffParticle(PlayerProgress progress) {
        if (progress.hasSkill(MagePlayerClass.FIRE_MAGE)) return ParticleTypes.FLAME;
        if (progress.hasSkill(MagePlayerClass.ICE_MAGE)) return ParticleTypes.SNOWFLAKE;
        if (progress.hasSkill(MagePlayerClass.POISON_MAGE)) return ParticleTypes.SPORE_BLOSSOM_AIR;
        if (progress.hasSkill(MagePlayerClass.LIGHTNING_MAGE)) return ParticleTypes.ELECTRIC_SPARK;
        if (progress.hasSkill(MagePlayerClass.DOCTOR)) return ParticleTypes.HAPPY_VILLAGER;
        return ParticleTypes.ENCHANT;
    }

    private static double toxicCloudRadius(PlayerProgress progress) {
        return RpgBalanceConfig.TOXIC_CLOUD_RADIUS.get()
                + (progress.hasSkill(MagePlayerClass.PLAGUE_CLOUD) ? 2.0D : 0.0D);
    }

    private static double doctorRadius(PlayerProgress progress, double base) {
        return base + (progress.hasSkill(MagePlayerClass.FIELD_MEDIC)
                ? RpgBalanceConfig.FIELD_MEDIC_RADIUS_BONUS.get() : 0.0D);
    }

    private static int cooldownTicks(ServerPlayer player, String attackId) {
        double seconds = switch (attackId) {
            case "fireball_volley" -> RpgBalanceConfig.FIREBALL_VOLLEY_COOLDOWN_SECONDS.get();
            case "flame_ring" -> RpgBalanceConfig.FLAME_RING_COOLDOWN_SECONDS.get();
            case "ice_lance" -> RpgBalanceConfig.ICE_LANCE_COOLDOWN_SECONDS.get();
            case "frost_nova" -> RpgBalanceConfig.FROST_NOVA_COOLDOWN_SECONDS.get();
            case "venom_bolt" -> RpgBalanceConfig.VENOM_BOLT_COOLDOWN_SECONDS.get();
            case "toxic_cloud" -> RpgBalanceConfig.TOXIC_CLOUD_COOLDOWN_SECONDS.get();
            case "chain_lightning" -> RpgBalanceConfig.CHAIN_LIGHTNING_COOLDOWN_SECONDS.get();
            case "thunderstep" -> RpgBalanceConfig.THUNDERSTEP_COOLDOWN_SECONDS.get();
            case "healing_pulse" -> RpgBalanceConfig.HEALING_PULSE_COOLDOWN_SECONDS.get();
            case "purge" -> RpgBalanceConfig.PURGE_COOLDOWN_SECONDS.get();
            case "adrenaline" -> RpgBalanceConfig.ADRENALINE_COOLDOWN_SECONDS.get();
            default -> 1.0D;
        };
        if (progress(player).hasSkill(MagePlayerClass.FIELD_MEDIC)
                && (attackId.equals("healing_pulse") || attackId.equals("purge") || attackId.equals("adrenaline"))) {
            seconds *= 1.0D - RpgBalanceConfig.FIELD_MEDIC_COOLDOWN_REDUCTION_PERCENT.get() / 100.0D;
        }
        return Math.max(1, RpgBalanceConfig.ticksValue(seconds));
    }

    private static String displayName(String id) {
        return switch (id) {
            case "fireball_volley" -> "Fireball Volley";
            case "flame_ring" -> "Meteor Ring";
            case "ice_lance" -> "Hailpiercer";
            case "frost_nova" -> "Cryo Prison";
            case "venom_bolt" -> "Venom Bolt";
            case "toxic_cloud" -> "Toxic Cloud";
            case "chain_lightning" -> "Chain Lightning";
            case "thunderstep" -> "Thunderstep";
            case "healing_pulse" -> "Healing Pulse";
            case "purge" -> "Purge";
            case "adrenaline" -> "Adrenaline";
            default -> id;
        };
    }

    private static boolean isMageAttack(String id) {
        return id.equals("fireball_volley") || id.equals("flame_ring") || id.equals("ice_lance")
                || id.equals("frost_nova") || id.equals("venom_bolt") || id.equals("toxic_cloud")
                || id.equals("chain_lightning") || id.equals("thunderstep")
                || id.equals("healing_pulse") || id.equals("purge") || id.equals("adrenaline");
    }

    private static boolean holdingStaff(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.MAGE_STAFF.get());
    }

    private static PlayerProgress progress(ServerPlayer player) {
        return PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
    }

    private static ServerLevel serverLevel(ServerPlayer player) {
        return (ServerLevel) player.level;
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
    }

    private static final class State {
        private final Map<String, Long> cooldowns = new HashMap<>();
        private final Map<String, Integer> cooldownDurations = new HashMap<>();
        private Vec3 cloudCenter = Vec3.ZERO;
        private long cloudUntil;
        private int cloudDuration;
    }
}
