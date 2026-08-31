package com.ollie.tierborne.playerclass;

import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.data.PlayerProgress;

public final class FighterStats {
    private FighterStats(){}
    public static boolean isFist(net.minecraft.world.entity.player.Player player){return player.getMainHandItem().isEmpty()&&player.getOffhandItem().isEmpty();}
    public static double generalDamage(PlayerProgress p){double value=0;if(p.hasSkill(FighterPlayerClass.CHAMPION_DAMAGE))value+=RpgBalanceConfig.FIGHTER_CHAMPION_PATH_DAMAGE.get();if(p.hasSkill(FighterPlayerClass.DUELIST_DAMAGE))value+=RpgBalanceConfig.FIGHTER_DUELIST_PATH_DAMAGE.get();if(p.hasSkill(FighterPlayerClass.DUELIST))value+=RpgBalanceConfig.DUELIST_DAMAGE.get();if(p.hasSkill(FighterPlayerClass.DUELIST_DAMAGE_UPGRADE))value+=RpgBalanceConfig.DUELIST_DAMAGE_UPGRADE.get();return value;}
    public static double fistDamage(PlayerProgress p){double value=0;if(p.hasSkill(FighterPlayerClass.MONK_FIST))value+=RpgBalanceConfig.FIGHTER_MONK_PATH_FIST.get();if(p.hasSkill(FighterPlayerClass.MONK))value+=RpgBalanceConfig.MONK_FIST.get();if(p.hasSkill(FighterPlayerClass.MONK_FIST_UPGRADE))value+=RpgBalanceConfig.MONK_FIST_UPGRADE.get();return value;}
    public static double movement(PlayerProgress p){double value=0;if(p.hasSkill(FighterPlayerClass.MONK_MOVE))value+=RpgBalanceConfig.FIGHTER_MONK_PATH_MOVE.get();if(p.hasSkill(FighterPlayerClass.MONK))value+=RpgBalanceConfig.MONK_MOVE.get();return value;}
    public static double meleeChargeSpeed(PlayerProgress p){double value=0;if(p.hasSkill(FighterPlayerClass.CHAMPION_SPEED))value+=RpgBalanceConfig.FIGHTER_CHAMPION_PATH_CHARGE.get();if(p.hasSkill(FighterPlayerClass.DUELIST_SPEED))value+=RpgBalanceConfig.FIGHTER_DUELIST_PATH_CHARGE.get();return value;}
}
