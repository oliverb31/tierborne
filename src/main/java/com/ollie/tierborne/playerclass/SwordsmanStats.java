package com.ollie.tierborne.playerclass;
import com.ollie.tierborne.config.RpgBalanceConfig; import java.util.Set;
public final class SwordsmanStats {
 private SwordsmanStats(){}
 public static double subclassSwordDamage(Set<String> s){double v=0;if(s.contains(SwordsmanPlayerClass.SWORDMASTER))v+=RpgBalanceConfig.SWORDMASTER_DAMAGE.get();if(s.contains(SwordsmanPlayerClass.SM_DAMAGE))v+=RpgBalanceConfig.SWORDMASTER_UPGRADE_DAMAGE.get();if(s.contains(SwordsmanPlayerClass.DUAL))v+=s.contains(SwordsmanPlayerClass.DUAL_DAMAGE)?RpgBalanceConfig.DUAL_DAMAGE_UPGRADE.get():RpgBalanceConfig.DUAL_DAMAGE.get();if(s.contains(SwordsmanPlayerClass.HEAVY))v+=RpgBalanceConfig.HEAVY_DAMAGE.get();if(s.contains(SwordsmanPlayerClass.HEAVY_DAMAGE))v+=RpgBalanceConfig.HEAVY_UPGRADE_DAMAGE.get();return v;}
 public static double subclassMovementSpeed(Set<String> s){double v=0;if(s.contains(SwordsmanPlayerClass.SWORDMASTER))v+=RpgBalanceConfig.SWORDMASTER_SPEED.get();if(s.contains(SwordsmanPlayerClass.SM_SPEED))v+=RpgBalanceConfig.SWORDMASTER_UPGRADE_SPEED.get();if(s.contains(SwordsmanPlayerClass.ROGUE))v+=RpgBalanceConfig.ROGUE_SPEED.get();return v;}
}
