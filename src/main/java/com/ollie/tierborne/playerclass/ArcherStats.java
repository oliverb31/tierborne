package com.ollie.tierborne.playerclass;

import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.data.PlayerProgress;

/** Single gameplay/UI source of truth for all Archer ranged and charge modifiers. */
public final class ArcherStats {
    private ArcherStats() {}
    public static double bowDamage(PlayerProgress p){return rangedDamage(p)+(p.playerClassId().equals(ArcherPlayerClass.ID)?RpgBalanceConfig.ARCHER_BOW_DAMAGE.get():0)+(p.hasSkill(ArcherPlayerClass.BOW_DAMAGE_1)?RpgBalanceConfig.BOW_DAMAGE_I.get():0)+(p.hasSkill(ArcherPlayerClass.LONGBOWMAN)?RpgBalanceConfig.LONGBOWMAN_DAMAGE.get():0)+(p.hasSkill(ArcherPlayerClass.LONGBOW_DAMAGE)?RpgBalanceConfig.LONGBOW_DAMAGE_UPGRADE.get():0);}
    public static double crossbowDamage(PlayerProgress p){return rangedDamage(p)+(p.playerClassId().equals(ArcherPlayerClass.ID)?RpgBalanceConfig.ARCHER_CROSSBOW_DAMAGE.get():0)+(p.hasSkill(ArcherPlayerClass.CROSSBOW_DAMAGE_1)?RpgBalanceConfig.CROSSBOW_DAMAGE_I.get():0)+(p.hasSkill(ArcherPlayerClass.CROSSBOWMAN)?RpgBalanceConfig.CROSSBOWMAN_DAMAGE.get():0)+(p.hasSkill(ArcherPlayerClass.CROSSBOW_DAMAGE_2)?RpgBalanceConfig.CROSSBOW_DAMAGE_II.get():0);}
    public static double rangedDamage(PlayerProgress p){double v=0;if(p.hasSkill(ArcherPlayerClass.ELEMENTAL_DAMAGE_1))v+=RpgBalanceConfig.ELEMENTAL_RANGED_DAMAGE_I.get();if(p.hasSkill(ArcherPlayerClass.ELEMENTAL_DAMAGE_2))v+=RpgBalanceConfig.ELEMENTAL_RANGED_DAMAGE_II.get();if(p.hasSkill(ArcherPlayerClass.RANGER_DAMAGE))v+=RpgBalanceConfig.RANGER_RANGED_DAMAGE.get();return v;}
    public static double bowChargeSpeed(PlayerProgress p){double v=0;if(p.hasSkill(ArcherPlayerClass.BOW_HANDLING))v+=RpgBalanceConfig.BOW_HANDLING_SPEED.get();if(p.hasSkill(ArcherPlayerClass.LONGBOWMAN))v+=RpgBalanceConfig.LONGBOWMAN_CHARGE_SPEED.get();return v;}
    public static double bowDrawTimeMultiplier(PlayerProgress p){double multiplier=p.hasSkill(ArcherPlayerClass.BOW_HANDLING)?100.0/(100.0+RpgBalanceConfig.BOW_HANDLING_SPEED.get()):1.0;if(p.hasSkill(ArcherPlayerClass.LONGBOWMAN))multiplier*=1.0-RpgBalanceConfig.LONGBOWMAN_CHARGE_SPEED.get()/100.0;return Math.max(0.1,multiplier);}
    public static double crossbowChargeSpeed(PlayerProgress p){double v=0;if(p.hasSkill(ArcherPlayerClass.CROSSBOW_SPEED_1))v+=RpgBalanceConfig.CROSSBOW_CHARGE_SPEED_I.get();if(p.hasSkill(ArcherPlayerClass.CROSSBOW_SPEED_2))v+=RpgBalanceConfig.CROSSBOW_CHARGE_SPEED_II.get();return v;}
}
