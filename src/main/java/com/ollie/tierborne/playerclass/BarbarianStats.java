package com.ollie.tierborne.playerclass;

import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.data.PlayerProgress;

public final class BarbarianStats {
    private BarbarianStats() {}

    public static double axeDamage(PlayerProgress progress, boolean berserkActive) {
        if (!BarbarianPlayerClass.ID.equals(progress.playerClassId())) return 0.0D;
        double bonus = RpgBalanceConfig.BARBARIAN_AXE_DAMAGE.get();
        if (progress.hasSkill(BarbarianPlayerClass.EXECUTIONER)) {
            bonus += RpgBalanceConfig.EXECUTIONER_AXE_DAMAGE.get();
        }
        if (progress.hasSkill(BarbarianPlayerClass.BERSERKER) && berserkActive) {
            bonus += progress.hasSkill(BarbarianPlayerClass.GREATER_FRENZY)
                    ? RpgBalanceConfig.BERSERK_UPGRADED_AXE_DAMAGE.get()
                    : RpgBalanceConfig.BERSERK_AXE_DAMAGE.get();
        }
        return bonus;
    }

    public static double berserkMovement(PlayerProgress progress) {
        return progress.hasSkill(BarbarianPlayerClass.GREATER_FRENZY)
                ? RpgBalanceConfig.BERSERK_UPGRADED_MOVEMENT_SPEED.get()
                : RpgBalanceConfig.BERSERK_MOVEMENT_SPEED.get();
    }

    public static double executeMovement(PlayerProgress progress) {
        return progress.hasSkill(BarbarianPlayerClass.EXECUTE_MOBILITY)
                ? RpgBalanceConfig.EXECUTE_UPGRADED_MOVEMENT_PENALTY.get()
                : RpgBalanceConfig.EXECUTE_MOVEMENT_PENALTY.get();
    }
}
