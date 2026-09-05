package com.ollie.tierborne.combat;

import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.entity.IceMob;
import com.ollie.tierborne.registry.ModAttributes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ElementalCombat {
    private static final String CHILLED_UNTIL = "tierborne:mage_chilled_until";
    private static final String TOXIC_UNTIL = "tierborne:mage_toxic_until";
    private static final Map<UUID, Long> VULNERABLE_UNTIL = new HashMap<>();

    private ElementalCombat() {}

    public static void applyVulnerability(LivingEntity target) {
        VULNERABLE_UNTIL.put(target.getUUID(), target.level.getGameTime()
                + RpgBalanceConfig.ticks(RpgBalanceConfig.ELEMENTAL_VULNERABILITY_SECONDS));
    }

    public static float modifyDamage(LivingEntity target, Element element, float damage) {
        double affinityMultiplier = affinityMultiplier(target, element);
        Long until = VULNERABLE_UNTIL.get(target.getUUID());
        if (until == null) return (float) (damage * affinityMultiplier);
        if (until <= target.level.getGameTime() || !target.isAlive()) {
            VULNERABLE_UNTIL.remove(target.getUUID());
            return (float) (damage * affinityMultiplier);
        }
        return (float) (damage * affinityMultiplier
                * (1.0 + RpgBalanceConfig.ELEMENTAL_VULNERABILITY_BONUS_PERCENT.get() / 100.0));
    }

    private static double affinityMultiplier(LivingEntity target, Element element) {
        double multiplier = 1.0D;
        if (target instanceof IceMob) {
            if (element == Element.FIRE) {
                multiplier *= 1.0D + RpgBalanceConfig.ICE_MOB_FIRE_VULNERABILITY_PERCENT.get() / 100.0D;
            } else if (element == Element.ICE) {
                multiplier *= Math.max(0.0D,
                        1.0D - RpgBalanceConfig.ICE_MOB_ICE_RESISTANCE_PERCENT.get() / 100.0D);
            }
        }
        if (element == Element.POISON && target.getMobType() == MobType.UNDEAD) {
            multiplier *= Math.max(0.0D,
                    1.0D - RpgBalanceConfig.UNDEAD_POISON_RESISTANCE_PERCENT.get() / 100.0D);
        }
        return multiplier;
    }

    public static void applyFire(ServerPlayer caster, LivingEntity target, double burnSeconds) {
        boolean chilled = isMarked(target, CHILLED_UNTIL) || target.getTicksFrozen() > 0;
        boolean toxic = isMarked(target, TOXIC_UNTIL);
        if (chilled) {
            clearChill(target);
            reactionDamage(caster, target, Element.FIRE, RpgBalanceConfig.THERMAL_SHOCK_DAMAGE.get());
            reactionEffect(target, ParticleTypes.CLOUD, SoundEvents.FIRE_EXTINGUISH);
        }
        if (toxic) {
            clearToxin(target);
            reactionDamage(caster, target, Element.FIRE, RpgBalanceConfig.POISON_COMBUSTION_DAMAGE.get());
            reactionEffect(target, ParticleTypes.LAVA, SoundEvents.FIRECHARGE_USE);
        }
        target.setSecondsOnFire((int) Math.ceil(burnSeconds));
    }

    public static void applyIce(ServerPlayer caster, LivingEntity target, int slowTicks,
                                int slowAmplifier, int frozenTicks) {
        if (target.isOnFire()) {
            target.clearFire();
            reactionDamage(caster, target, Element.ICE, RpgBalanceConfig.THERMAL_SHOCK_DAMAGE.get());
            reactionEffect(target, ParticleTypes.CLOUD, SoundEvents.FIRE_EXTINGUISH);
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, slowAmplifier));
        if (frozenTicks > 0) target.setTicksFrozen(Math.max(target.getTicksFrozen(), frozenTicks));
        target.getPersistentData().putLong(CHILLED_UNTIL,
                target.level.getGameTime() + Math.max(slowTicks, frozenTicks));
    }

    public static void applyPoison(LivingEntity target, int poisonTicks, int amplifier) {
        if (target.getMobType() == MobType.UNDEAD) {
            target.getPersistentData().remove(TOXIC_UNTIL);
            target.removeEffect(MobEffects.POISON);
            return;
        }
        target.addEffect(new MobEffectInstance(MobEffects.POISON, poisonTicks, amplifier));
        target.getPersistentData().putLong(TOXIC_UNTIL, target.level.getGameTime() + poisonTicks);
    }

    public static double lightningDamage(LivingEntity target, double damage) {
        if (!isMarked(target, CHILLED_UNTIL) && !isMarked(target, TOXIC_UNTIL)) return damage;
        reactionEffect(target, ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_IMPACT);
        return damage * (1.0D + RpgBalanceConfig.CONDUCTIVE_LIGHTNING_BONUS_PERCENT.get() / 100.0D);
    }

    public static void cleanse(LivingEntity target) {
        clearChill(target);
        clearToxin(target);
        target.clearFire();
        new java.util.ArrayList<>(target.getActiveEffects()).stream()
                .filter(effect -> effect.getEffect().getCategory() == MobEffectCategory.HARMFUL)
                .forEach(effect -> target.removeEffect(effect.getEffect()));
    }

    private static boolean isMarked(LivingEntity target, String key) {
        long until = target.getPersistentData().getLong(key);
        if (until > target.level.getGameTime()) return true;
        target.getPersistentData().remove(key);
        return false;
    }

    private static void clearChill(LivingEntity target) {
        boolean mageChill = target.getPersistentData().contains(CHILLED_UNTIL);
        target.getPersistentData().remove(CHILLED_UNTIL);
        target.setTicksFrozen(0);
        if (mageChill) target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
    }

    private static void clearToxin(LivingEntity target) {
        boolean mageToxin = target.getPersistentData().contains(TOXIC_UNTIL);
        target.getPersistentData().remove(TOXIC_UNTIL);
        if (mageToxin) target.removeEffect(MobEffects.POISON);
    }

    private static void reactionDamage(ServerPlayer caster, LivingEntity target,
                                       Element element, double baseDamage) {
        if (target instanceof net.minecraft.world.entity.player.Player || target == caster) return;
        float damage = modifyDamage(target, element, (float) baseDamage)
                * (float) caster.getAttributeValue(ModAttributes.MAGIC_DAMAGE.get());
        target.hurt(DamageSource.indirectMagic(caster, caster), damage);
    }

    private static void reactionEffect(LivingEntity target,
                                       net.minecraft.core.particles.ParticleOptions particle,
                                       net.minecraft.sounds.SoundEvent sound) {
        if (!(target.level instanceof ServerLevel server)) return;
        server.sendParticles(particle, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(), 18, 0.35D, 0.45D, 0.35D, 0.04D);
        server.playSound(null, target.blockPosition(), sound, SoundSource.PLAYERS, 0.75F, 1.25F);
    }

    public static void clear(LivingEntity entity) {
        VULNERABLE_UNTIL.remove(entity.getUUID());
        entity.getPersistentData().remove(CHILLED_UNTIL);
        entity.getPersistentData().remove(TOXIC_UNTIL);
    }
}
