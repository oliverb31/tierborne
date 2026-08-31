package com.ollie.tierborne.combat;

import com.ollie.tierborne.config.RpgBalanceConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ElementalCombat {
    private static final Map<UUID, Long> VULNERABLE_UNTIL = new HashMap<>();

    private ElementalCombat() {}

    public static void applyVulnerability(LivingEntity target) {
        VULNERABLE_UNTIL.put(target.getUUID(), target.level.getGameTime()
                + RpgBalanceConfig.ticks(RpgBalanceConfig.ELEMENTAL_VULNERABILITY_SECONDS));
    }

    public static float modifyDamage(LivingEntity target, Element element, float damage) {
        Long until = VULNERABLE_UNTIL.get(target.getUUID());
        if (until == null) return damage;
        if (until <= target.level.getGameTime() || !target.isAlive()) {
            VULNERABLE_UNTIL.remove(target.getUUID());
            return damage;
        }
        return (float)(damage * (1.0 + RpgBalanceConfig.ELEMENTAL_VULNERABILITY_BONUS_PERCENT.get() / 100.0));
    }

    public static void clear(LivingEntity entity) {
        VULNERABLE_UNTIL.remove(entity.getUUID());
    }
}
