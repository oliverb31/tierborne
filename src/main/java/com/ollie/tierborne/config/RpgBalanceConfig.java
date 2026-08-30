package com.ollie.tierborne.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RpgBalanceConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue SWORDMASTER_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SWORDMASTER_SPEED;
    public static final ForgeConfigSpec.DoubleValue DASH_VELOCITY;
    public static final ForgeConfigSpec.DoubleValue DASH_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue DASH_UPGRADE_VELOCITY;
    public static final ForgeConfigSpec.DoubleValue DASH_UPGRADE_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue DASH_STRIKE_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue DASH_STRIKE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue DASH_STRIKE_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue DASH_STRIKE_UPGRADE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SWORDMASTER_UPGRADE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SWORDMASTER_UPGRADE_SPEED;

    public static final ForgeConfigSpec.DoubleValue DUAL_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue DUAL_ATTACK_SPEED;
    public static final ForgeConfigSpec.DoubleValue DUAL_DAMAGE_UPGRADE;
    public static final ForgeConfigSpec.DoubleValue DUAL_SPEED_UPGRADE;
    public static final ForgeConfigSpec.DoubleValue BLOCK_PERCENT;
    public static final ForgeConfigSpec.DoubleValue IMPROVED_BLOCK_PERCENT;
    public static final ForgeConfigSpec.DoubleValue SHIELD_BLOCK_PERCENT;
    public static final ForgeConfigSpec.DoubleValue PARRY_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue MULTISLASH_DURATION_SECONDS;
    public static final ForgeConfigSpec.DoubleValue MULTISLASH_COOLDOWN_SECONDS;

    public static final ForgeConfigSpec.DoubleValue HEAVY_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue HEAVY_ATTACK_SPEED;
    public static final ForgeConfigSpec.DoubleValue HEAVY_MOVE_PENALTY;
    public static final ForgeConfigSpec.DoubleValue HEAVY_MOVE_LINGER_SECONDS;
    public static final ForgeConfigSpec.DoubleValue HEAVY_DRAW_DELAY_SECONDS;
    public static final ForgeConfigSpec.DoubleValue HEAVY_ATTACK_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue HEAVY_ATTACK_UPGRADE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue HEAVY_ATTACK_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue HEAVY_ATTACK_UPGRADE_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue HEAVY_RANGE;
    public static final ForgeConfigSpec.DoubleValue HEAVY_UPGRADE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue LEAP_LAUNCH;
    public static final ForgeConfigSpec.DoubleValue LEAP_RADIUS;
    public static final ForgeConfigSpec.DoubleValue LEAP_KNOCKBACK;
    public static final ForgeConfigSpec.DoubleValue LEAP_COOLDOWN_SECONDS;

    public static final ForgeConfigSpec.DoubleValue ROGUE_HEALTH_PENALTY;
    public static final ForgeConfigSpec.DoubleValue ROGUE_SPEED;
    public static final ForgeConfigSpec.DoubleValue CLOAK_DURATION_SECONDS;
    public static final ForgeConfigSpec.DoubleValue CLOAK_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue BACKSTAB_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue BACKSTAB_DOT_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue FIRST_HIT_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue FIRST_HIT_RESET_SECONDS;
    public static final ForgeConfigSpec.DoubleValue NON_AGGRO_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue ROGUE_RETARGET_RADIUS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("swordmaster");
        SWORDMASTER_DAMAGE = percent(b, "subclassSwordDamagePercent", 20.0);
        SWORDMASTER_SPEED = percent(b, "subclassMovementSpeedPercent", 10.0);
        DASH_VELOCITY = value(b, "dashVelocity", 1.35, 0.0, 5.0);
        DASH_COOLDOWN_SECONDS = seconds(b, "dashCooldownSeconds", 4.0);
        DASH_UPGRADE_VELOCITY = value(b, "upgradedDashVelocity", 1.75, 0.0, 5.0);
        DASH_UPGRADE_COOLDOWN_SECONDS = seconds(b, "upgradedDashCooldownSeconds", 2.5);
        DASH_STRIKE_DISTANCE = value(b, "dashStrikeDistanceBlocks", 5.0, 1.0, 16.0);
        DASH_STRIKE_DAMAGE = percent(b, "dashStrikeExtraDamagePercent", 30.0);
        DASH_STRIKE_COOLDOWN_SECONDS = seconds(b, "dashStrikeCooldownSeconds", 5.0);
        DASH_STRIKE_UPGRADE_DAMAGE = percent(b, "upgradedDashStrikeExtraDamagePercent", 55.0);
        SWORDMASTER_UPGRADE_DAMAGE = percent(b, "swordDamageUpgradePercent", 20.0);
        SWORDMASTER_UPGRADE_SPEED = percent(b, "movementSpeedUpgradePercent", 10.0);
        b.pop().push("dualSwordsman");
        DUAL_DAMAGE = percent(b, "damagePerSwordPercent", -30.0);
        DUAL_ATTACK_SPEED = percent(b, "attackSpeedPercent", 0.0);
        DUAL_DAMAGE_UPGRADE = percent(b, "upgradedDamagePerSwordPercent", -15.0);
        DUAL_SPEED_UPGRADE = percent(b, "upgradedAttackSpeedPercent", 25.0);
        BLOCK_PERCENT = percent(b, "dualSwordsmanBaseBlockPercent", 20.0);
        IMPROVED_BLOCK_PERCENT = percent(b, "improvedBlockPercent", 70.0);
        SHIELD_BLOCK_PERCENT = percent(b, "shieldBlockPercent", 40.0);
        PARRY_COOLDOWN_SECONDS = seconds(b, "parryCooldownSeconds", 3.0);
        MULTISLASH_DURATION_SECONDS = seconds(b, "multislashDurationSeconds", 1.0);
        MULTISLASH_COOLDOWN_SECONDS = seconds(b, "multislashCooldownSeconds", 6.0);
        b.pop().push("heavySwordsman");
        HEAVY_DAMAGE = percent(b, "subclassSwordDamagePercent", 75.0);
        HEAVY_ATTACK_SPEED = percent(b, "attackSpeedPercent", -40.0);
        HEAVY_MOVE_PENALTY = percent(b, "swordMovementSpeedPercent", -25.0);
        HEAVY_MOVE_LINGER_SECONDS = seconds(b, "movementPenaltyLingerSeconds", 1.0);
        HEAVY_DRAW_DELAY_SECONDS = seconds(b, "swordDrawDelaySeconds", 1.0);
        HEAVY_ATTACK_DAMAGE = percent(b, "heavyAttackExtraDamagePercent", 30.0);
        HEAVY_ATTACK_UPGRADE_DAMAGE = percent(b, "upgradedHeavyAttackExtraDamagePercent", 65.0);
        HEAVY_ATTACK_COOLDOWN_SECONDS = seconds(b, "heavyAttackCooldownSeconds", 2.0);
        HEAVY_ATTACK_UPGRADE_COOLDOWN_SECONDS = seconds(b, "upgradedHeavyAttackCooldownSeconds", 1.25);
        HEAVY_RANGE = value(b, "meleeRangeBonusBlocks", 1.5, 0.0, 8.0);
        HEAVY_UPGRADE_DAMAGE = percent(b, "swordDamageUpgradePercent", 35.0);
        LEAP_LAUNCH = value(b, "leapLaunchVelocity", 0.85, 0.1, 3.0);
        LEAP_RADIUS = value(b, "leapImpactRadiusBlocks", 2.0, 0.5, 12.0);
        LEAP_KNOCKBACK = value(b, "leapKnockbackStrength", 1.6, 0.0, 8.0);
        LEAP_COOLDOWN_SECONDS = seconds(b, "leapCooldownSeconds", 8.0);
        b.pop().push("rogue");
        ROGUE_HEALTH_PENALTY = value(b, "maxHealthPenalty", 4.0, 0.0, 19.0);
        ROGUE_SPEED = percent(b, "movementSpeedPercent", 20.0);
        CLOAK_DURATION_SECONDS = seconds(b, "cloakDurationSeconds", 5.0);
        CLOAK_COOLDOWN_SECONDS = seconds(b, "cloakCooldownSeconds", 12.0);
        BACKSTAB_DAMAGE = percent(b, "backstabExtraDamagePercent", 50.0);
        BACKSTAB_DOT_THRESHOLD = value(b, "backstabFacingDotThreshold", 0.5, -1.0, 1.0);
        FIRST_HIT_DAMAGE = percent(b, "firstHitExtraDamagePercent", 35.0);
        FIRST_HIT_RESET_SECONDS = seconds(b, "firstHitResetSeconds", 10.0);
        NON_AGGRO_DAMAGE = percent(b, "nonAggroExtraDamagePercent", 25.0);
        ROGUE_RETARGET_RADIUS = value(b, "alternativePlayerTargetRadiusBlocks", 16.0, 1.0, 64.0);
        b.pop();
        SPEC = b.build();
    }

    private RpgBalanceConfig() {}
    private static ForgeConfigSpec.DoubleValue percent(ForgeConfigSpec.Builder b, String name, double value) { return b.comment("Percentage value.").defineInRange(name, value, -100.0, 1000.0); }
    private static ForgeConfigSpec.DoubleValue seconds(ForgeConfigSpec.Builder b, String name, double value) { return b.comment("Duration in seconds.").defineInRange(name, value, 0.0, 3600.0); }
    private static ForgeConfigSpec.DoubleValue value(ForgeConfigSpec.Builder b, String name, double value, double min, double max) { return b.defineInRange(name, value, min, max); }
    public static int ticks(ForgeConfigSpec.DoubleValue seconds) { return Math.max(0, (int)Math.round(seconds.get() * 20.0)); }
}
