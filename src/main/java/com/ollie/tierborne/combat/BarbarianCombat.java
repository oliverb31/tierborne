package com.ollie.tierborne.combat;

import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.data.PlayerProgress;
import com.ollie.tierborne.data.PlayerProgressSavedData;
import com.ollie.tierborne.playerclass.BarbarianPlayerClass;
import com.ollie.tierborne.playerclass.BarbarianStats;
import com.ollie.tierborne.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class BarbarianCombat {
    private static final Map<UUID, State> STATES = new HashMap<>();

    private BarbarianCombat() {}

    public static boolean input(ServerPlayer player, AbilityAction action) {
        PlayerProgress progress = progress(player);
        if (!BarbarianPlayerClass.ID.equals(progress.playerClassId())) return false;
        String selected = progress.selectedAlternateAttack();
        if ("berserk".equals(selected) && progress.hasSkill(BarbarianPlayerClass.BERSERKER)) {
            if (action == AbilityAction.ALTERNATE_ATTACK) toggleBerserk(player, progress);
            return action == AbilityAction.ALTERNATE_ATTACK || action == AbilityAction.ALTERNATE_RELEASE;
        }
        if ("execute".equals(selected) && progress.hasSkill(BarbarianPlayerClass.EXECUTIONER)) {
            if (action == AbilityAction.ALTERNATE_ATTACK) startExecute(player, progress);
            else if (action == AbilityAction.ALTERNATE_RELEASE) releaseExecute(player, progress);
            return action == AbilityAction.ALTERNATE_ATTACK || action == AbilityAction.ALTERNATE_RELEASE;
        }
        return false;
    }

    public static void tick(ServerPlayer player) {
        PlayerProgress progress = progress(player);
        State state = state(player);
        long now = player.level.getGameTime();
        if (!BarbarianPlayerClass.ID.equals(progress.playerClassId())) {
            clearState(player, state);
            return;
        }
        if (state.berserkActive && now >= state.berserkEndsAt) {
            state.berserkActive = false;
            state.nextToggleAt = now + RpgBalanceConfig.ticks(RpgBalanceConfig.BERSERK_TOGGLE_COOLDOWN_SECONDS);
            player.displayClientMessage(Component.literal("Berserk ended."), true);
        }
        if (state.executeCharging && (!axe(player) || now >= state.executeStartedAt + executeChargeTicks(progress))) {
            state.executeReady = axe(player);
            if (!state.executeReady) cancelExecute(player, state, "Execute cancelled: an axe is required.");
        }
        if (state.bleedDamage > 0 && now >= state.nextBleedTick) {
            int interval = Math.max(1, RpgBalanceConfig.ticks(RpgBalanceConfig.BERSERK_BLEED_INTERVAL_SECONDS));
            state.nextBleedTick = now + interval;
            float drain = state.bleedDamage >= RpgBalanceConfig.BERSERK_BLEED_THRESHOLD.get()
                    ? (float)(state.bleedDamage * RpgBalanceConfig.BERSERK_BLEED_HIGH_DRAIN_PERCENT.get() / 100.0)
                    : RpgBalanceConfig.BERSERK_BLEED_LOW_DRAIN.get().floatValue();
            drain = Math.min(drain, state.bleedDamage);
            state.bleedDamage -= drain;
            state.dealingBleed = true;
            player.hurt(DamageSource.MAGIC, drain);
            state.dealingBleed = false;
            if (state.bleedDamage < 0.01F) state.bleedDamage = 0;
        }
        if (state.berserkActive || state.bleedDamage > 0) {
            player.addEffect(new MobEffectInstance(ModEffects.BLEED.get(), 12, 0, false, false, true));
        } else {
            player.removeEffect(ModEffects.BLEED.get());
        }
    }

    public static List<AbilityStatus> statuses(ServerPlayer player) {
        PlayerProgress progress = progress(player);
        if (!BarbarianPlayerClass.ID.equals(progress.playerClassId())) return List.of();
        State state = state(player);
        long now = player.level.getGameTime();
        List<AbilityStatus> statuses = new ArrayList<>();
        if (state.berserkActive) statuses.add(new AbilityStatus("Berserk", (int)Math.max(0, state.berserkEndsAt - now),
                berserkDurationTicks(progress), true, "ACTIVE"));
        if (state.inCombatUntil > now) statuses.add(new AbilityStatus("In Combat", (int)(state.inCombatUntil - now),
                RpgBalanceConfig.ticks(RpgBalanceConfig.BERSERK_COMBAT_LOCK_SECONDS), true, "LOCKED"));
        if (!state.berserkActive && state.nextToggleAt > now) statuses.add(new AbilityStatus("Berserk",
                (int)(state.nextToggleAt - now), RpgBalanceConfig.ticks(RpgBalanceConfig.BERSERK_TOGGLE_COOLDOWN_SECONDS), false, "COOLDOWN"));
        if (state.executeCharging) {
            int total = executeChargeTicks(progress);
            int elapsed = (int)Math.min(total, Math.max(0, now - state.executeStartedAt));
            statuses.add(new AbilityStatus("Execute", elapsed, total, true, state.executeReady ? "READY" : "CHARGING"));
        } else if (state.executeCooldownUntil > now) {
            statuses.add(new AbilityStatus("Execute", (int)(state.executeCooldownUntil - now),
                    RpgBalanceConfig.ticks(RpgBalanceConfig.EXECUTE_COOLDOWN_SECONDS), false, "COOLDOWN"));
        }
        return statuses;
    }

    public static float modifyAxeDamage(ServerPlayer player, LivingEntity target, float amount) {
        PlayerProgress progress = progress(player);
        if (!BarbarianPlayerClass.ID.equals(progress.playerClassId()) || !axe(player)) return amount;
        double bonus = BarbarianStats.axeDamage(progress, isBerserkActive(player));
        if (progress.hasSkill(BarbarianPlayerClass.EXECUTIONER) && target.getHealth() < player.getHealth()) {
            bonus += progress.hasSkill(BarbarianPlayerClass.LOW_HEALTH_DAMAGE)
                    ? RpgBalanceConfig.EXECUTIONER_UPGRADED_LOW_HEALTH_DAMAGE.get()
                    : RpgBalanceConfig.EXECUTIONER_LOW_HEALTH_DAMAGE.get();
        }
        return (float)(amount * (1.0 + bonus / 100.0));
    }

    public static void onDamageTaken(ServerPlayer player, float amount) {
        State state = state(player);
        if (!state.berserkActive || state.dealingBleed || amount <= 0) return;
        PlayerProgress progress = progress(player);
        double ratio = progress.hasSkill(BarbarianPlayerClass.LESS_BLEED)
                ? RpgBalanceConfig.BERSERK_REDUCED_BLEED_PERCENT.get()
                : RpgBalanceConfig.BERSERK_BLEED_PERCENT.get();
        state.bleedDamage += (float)(amount * ratio / 100.0);
        state.inCombatUntil = player.level.getGameTime() + RpgBalanceConfig.ticks(RpgBalanceConfig.BERSERK_COMBAT_LOCK_SECONDS);
    }

    public static void onDamageDealt(ServerPlayer player, LivingEntity target, float amount) {
        if (amount <= 0 || !(target instanceof Mob || target instanceof Player other && !player.isAlliedTo(other))) return;
        State state = state(player);
        if (state.berserkActive) {
            PlayerProgress progress = progress(player);
            double ratio = progress.hasSkill(BarbarianPlayerClass.BLOOD_FEAST)
                    ? RpgBalanceConfig.BERSERK_UPGRADED_LIFESTEAL_PERCENT.get()
                    : RpgBalanceConfig.BERSERK_LIFESTEAL_PERCENT.get();
            player.heal((float)(amount * ratio / 100.0));
        }
        if (target instanceof Enemy || target instanceof Player other && !player.isAlliedTo(other)) {
            state.bleedDamage = 0;
            player.removeEffect(ModEffects.BLEED.get());
        }
    }

    public static boolean isBerserkActive(ServerPlayer player) {
        return state(player).berserkActive;
    }

    public static boolean executeMovementPenaltyActive(ServerPlayer player) {
        return state(player).executeCharging;
    }

    public static void reset(ServerPlayer player) {
        State state = STATES.remove(player.getUUID());
        if (state != null) player.removeEffect(ModEffects.BLEED.get());
    }

    private static void toggleBerserk(ServerPlayer player, PlayerProgress progress) {
        State state = state(player);
        long now = player.level.getGameTime();
        if (now < state.nextToggleAt) {
            player.displayClientMessage(Component.literal("Berserk cannot be toggled yet."), true);
            return;
        }
        if (state.berserkActive && (state.bleedDamage > 0 || now < state.inCombatUntil)) {
            player.displayClientMessage(Component.literal(state.bleedDamage > 0
                    ? "Berserk cannot end while Bleed remains." : "Berserk cannot end while in combat."), true);
            return;
        }
        state.berserkActive = !state.berserkActive;
        state.nextToggleAt = now + RpgBalanceConfig.ticks(RpgBalanceConfig.BERSERK_TOGGLE_COOLDOWN_SECONDS);
        if (state.berserkActive) {
            state.berserkEndsAt = now + berserkDurationTicks(progress);
            player.displayClientMessage(Component.literal("Berserk activated."), true);
        } else {
            player.displayClientMessage(Component.literal("Berserk deactivated."), true);
        }
    }

    private static void startExecute(ServerPlayer player, PlayerProgress progress) {
        State state = state(player);
        long now = player.level.getGameTime();
        if (now < state.executeCooldownUntil) {
            player.displayClientMessage(Component.literal("Execute is on cooldown."), true);
            return;
        }
        if (!axe(player)) {
            player.displayClientMessage(Component.literal("Execute requires an axe in your main hand."), true);
            return;
        }
        state.executeCharging = true;
        state.executeReady = false;
        state.executeStartedAt = now;
        player.displayClientMessage(Component.literal("Charging Execute..."), true);
    }

    private static void releaseExecute(ServerPlayer player, PlayerProgress progress) {
        State state = state(player);
        if (!state.executeCharging) return;
        long now = player.level.getGameTime();
        if (now - state.executeStartedAt < executeChargeTicks(progress)) {
            cancelExecute(player, state, "Execute cancelled before it was fully charged.");
            return;
        }
        Optional<LivingEntity> target = target(player, RpgBalanceConfig.EXECUTE_RANGE.get());
        if (target.isEmpty()) {
            cancelExecute(player, state, "Execute failed: no target in reach.");
            return;
        }
        LivingEntity victim = target.get();
        float healthRatio = victim.getHealth() / victim.getMaxHealth();
        float damage;
        if (healthRatio < RpgBalanceConfig.EXECUTE_KILL_THRESHOLD_PERCENT.get() / 100.0F) {
            damage = victim.getHealth() + victim.getAbsorptionAmount() + 1.0F;
        } else {
            double max = progress.hasSkill(BarbarianPlayerClass.EXECUTE_DAMAGE)
                    ? RpgBalanceConfig.EXECUTE_UPGRADED_MAX_MULTIPLIER.get()
                    : RpgBalanceConfig.EXECUTE_MAX_MULTIPLIER.get();
            double multiplier = 1.0 + (max - 1.0) * Math.pow(1.0 - healthRatio, 2.0);
            damage = (float)(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * multiplier);
        }
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        victim.invulnerableTime = 0;
        boolean hit = victim.hurt(DamageSource.playerAttack(player), damage);
        state.executeCharging = false;
        state.executeReady = false;
        state.executeCooldownUntil = now + RpgBalanceConfig.ticks(RpgBalanceConfig.EXECUTE_COOLDOWN_SECONDS);
        player.displayClientMessage(Component.literal(hit ? "Execute struck the target." : "Execute was blocked."), true);
    }

    private static Optional<LivingEntity> target(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        HitResult block = player.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        Vec3 clippedEnd = end;
        AABB area = new AABB(start, end).inflate(1.0);
        return player.level.getEntitiesOfClass(LivingEntity.class, area,
                        entity -> entity != player && entity.isAlive() && validEnemy(player, entity))
                .stream().filter(entity -> entity.getBoundingBox().inflate(0.3).clip(start, clippedEnd).isPresent())
                .min(Comparator.comparingDouble(player::distanceToSqr));
    }

    private static boolean validEnemy(ServerPlayer player, LivingEntity target) {
        return target instanceof Enemy || target instanceof Player other && !player.isAlliedTo(other);
    }

    private static int berserkDurationTicks(PlayerProgress progress) {
        return RpgBalanceConfig.ticks(progress.hasSkill(BarbarianPlayerClass.LONGER_BERSERK)
                ? RpgBalanceConfig.BERSERK_UPGRADED_DURATION_SECONDS : RpgBalanceConfig.BERSERK_MAX_DURATION_SECONDS);
    }

    private static int executeChargeTicks(PlayerProgress progress) {
        return RpgBalanceConfig.ticks(progress.hasSkill(BarbarianPlayerClass.EXECUTE_CHARGE)
                ? RpgBalanceConfig.EXECUTE_UPGRADED_CHARGE_SECONDS : RpgBalanceConfig.EXECUTE_CHARGE_SECONDS);
    }

    private static void cancelExecute(ServerPlayer player, State state, String message) {
        state.executeCharging = false;
        state.executeReady = false;
        player.displayClientMessage(Component.literal(message), true);
    }

    private static boolean axe(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof AxeItem;
    }

    private static PlayerProgress progress(ServerPlayer player) {
        return PlayerProgressSavedData.get(player.getServer()).get(player.getUUID());
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
    }

    private static void clearState(ServerPlayer player, State state) {
        state.berserkActive = false;
        state.bleedDamage = 0;
        state.executeCharging = false;
        state.executeReady = false;
        player.removeEffect(ModEffects.BLEED.get());
    }

    private static final class State {
        private long berserkEndsAt;
        private long nextToggleAt;
        private long inCombatUntil;
        private long nextBleedTick;
        private long executeStartedAt;
        private long executeCooldownUntil;
        private float bleedDamage;
        private boolean berserkActive;
        private boolean dealingBleed;
        private boolean executeCharging;
        private boolean executeReady;
    }
}
