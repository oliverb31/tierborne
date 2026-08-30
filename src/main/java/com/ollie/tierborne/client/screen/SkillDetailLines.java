package com.ollie.tierborne.client.screen;

import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.playerclass.*;
import java.util.*;

final class SkillDetailLines {
    private SkillDetailLines() {}
    static List<String> forSkill(Skill skill, SkillTreeDefinition tree, Set<String> unlocked) {
        List<String> lines=new ArrayList<>();
        if(skill.upgrade()!=null){int current=tree.totalBonus(skill.upgrade().type(),unlocked);lines.add(skill.upgrade().type().displayName()+": "+signed(skill.upgrade().percentagePoints())+"%");lines.add("Current: "+signed(current)+"%  After: "+signed(current+skill.upgrade().percentagePoints())+"%");}
        switch(skill.id()){
            case SwordsmanPlayerClass.DASH_STRIKE->{lines.add("Damage: +"+n(RpgBalanceConfig.DASH_STRIKE_DAMAGE.get())+"%");lines.add("Cooldown: "+n(RpgBalanceConfig.DASH_STRIKE_COOLDOWN_SECONDS.get())+"s  Distance: "+n(RpgBalanceConfig.DASH_STRIKE_DISTANCE.get())+" blocks");}
            case SwordsmanPlayerClass.MULTISLASH->{lines.add("Attacks: 2 with one sword; 4 with two swords");lines.add("Duration: "+n(RpgBalanceConfig.MULTISLASH_DURATION_SECONDS.get())+"s  Cooldown: "+n(RpgBalanceConfig.MULTISLASH_COOLDOWN_SECONDS.get())+"s");}
            case SwordsmanPlayerClass.HEAVY_ATTACK->{lines.add("Damage: +"+n(RpgBalanceConfig.HEAVY_ATTACK_DAMAGE.get())+"%");lines.add("Cooldown / attack lockout: "+n(RpgBalanceConfig.HEAVY_ATTACK_COOLDOWN_SECONDS.get())+"s");}
            case SwordsmanPlayerClass.CLOAK->{lines.add("Duration: "+n(RpgBalanceConfig.CLOAK_DURATION_SECONDS.get())+"s  Cooldown: "+n(RpgBalanceConfig.CLOAK_COOLDOWN_SECONDS.get())+"s");lines.add("Breaks on attack; drops mob aggro");}
            case SwordsmanPlayerClass.LEAP_STRIKE->{lines.add("Cooldown: "+n(RpgBalanceConfig.LEAP_COOLDOWN_SECONDS.get())+"s  Radius: "+n(RpgBalanceConfig.LEAP_RADIUS.get()));lines.add("Launch: "+n(RpgBalanceConfig.LEAP_LAUNCH.get())+"  Knockback: "+n(RpgBalanceConfig.LEAP_KNOCKBACK.get()));}
            case SwordsmanPlayerClass.SWORDMASTER->{lines.add("Sword Damage: +"+n(RpgBalanceConfig.SWORDMASTER_DAMAGE.get())+"%  Movement: +"+n(RpgBalanceConfig.SWORDMASTER_SPEED.get())+"%");lines.add("Dash cooldown: "+n(RpgBalanceConfig.DASH_COOLDOWN_SECONDS.get())+"s  Velocity: "+n(RpgBalanceConfig.DASH_VELOCITY.get()));}
            case SwordsmanPlayerClass.DUAL->{lines.add("Dual Wield: Unlocked  Damage/Sword: "+n(RpgBalanceConfig.DUAL_DAMAGE.get())+"%");lines.add("Block: "+n(RpgBalanceConfig.BLOCK_PERCENT.get())+"%");}
            case SwordsmanPlayerClass.HEAVY->{lines.add("Sword Damage: +"+n(RpgBalanceConfig.HEAVY_DAMAGE.get())+"%  Sword Charge Speed: "+n(RpgBalanceConfig.HEAVY_ATTACK_SPEED.get())+"%");lines.add("Sword Movement: "+n(RpgBalanceConfig.HEAVY_MOVE_PENALTY.get())+"%  Draw: "+n(RpgBalanceConfig.HEAVY_DRAW_DELAY_SECONDS.get())+"s");}
            case SwordsmanPlayerClass.ROGUE->{lines.add("Maximum Health: -"+n(RpgBalanceConfig.ROGUE_HEALTH_PENALTY.get())+"  Movement: +"+n(RpgBalanceConfig.ROGUE_SPEED.get())+"%");lines.add("Alternative target radius: "+n(RpgBalanceConfig.ROGUE_RETARGET_RADIUS.get())+" blocks");}
            case SwordsmanPlayerClass.SM_DAMAGE->lines.add("Sword Damage Contribution: +"+n(RpgBalanceConfig.SWORDMASTER_UPGRADE_DAMAGE.get())+"%");
            case SwordsmanPlayerClass.SM_SPEED->lines.add("Movement Speed Contribution: +"+n(RpgBalanceConfig.SWORDMASTER_UPGRADE_SPEED.get())+"%");
            case SwordsmanPlayerClass.SM_DASH_STRIKE->lines.add("Dash Strike Damage: +"+n(RpgBalanceConfig.DASH_STRIKE_UPGRADE_DAMAGE.get())+"%");
            case SwordsmanPlayerClass.SM_DASH->lines.add("Dash: "+n(RpgBalanceConfig.DASH_UPGRADE_VELOCITY.get())+" velocity, "+n(RpgBalanceConfig.DASH_UPGRADE_COOLDOWN_SECONDS.get())+"s cooldown");
            case SwordsmanPlayerClass.DUAL_SPEED->lines.add("Sword Charge Speed: +"+n(RpgBalanceConfig.DUAL_SPEED_UPGRADE.get())+"%");
            case SwordsmanPlayerClass.DUAL_DAMAGE->lines.add("Damage Per Sword: "+n(RpgBalanceConfig.DUAL_DAMAGE_UPGRADE.get())+"%");
            case SwordsmanPlayerClass.PARRY->lines.add("Counterattacks: 2  Cooldown: "+n(RpgBalanceConfig.PARRY_COOLDOWN_SECONDS.get())+"s");
            case SwordsmanPlayerClass.IMPROVED_BLOCK->lines.add("Block: "+n(RpgBalanceConfig.BLOCK_PERCENT.get())+"% -> "+n(RpgBalanceConfig.IMPROVED_BLOCK_PERCENT.get())+"%");
            case SwordsmanPlayerClass.HEAVY_DAMAGE->lines.add("Sword Damage Contribution: +"+n(RpgBalanceConfig.HEAVY_UPGRADE_DAMAGE.get())+"%");
            case SwordsmanPlayerClass.HEAVY_RANGE->lines.add("Melee Range: +"+n(RpgBalanceConfig.HEAVY_RANGE.get())+" blocks");
            case SwordsmanPlayerClass.HEAVY_RECOVERY->lines.add("Normal Attack Lockout: Removed");
            case SwordsmanPlayerClass.HEAVY_ATTACK_DAMAGE->lines.add("Heavy Attack Damage: +"+n(RpgBalanceConfig.HEAVY_ATTACK_UPGRADE_DAMAGE.get())+"%");
            case SwordsmanPlayerClass.HEAVY_ATTACK_COOLDOWN->lines.add("Heavy Attack Cooldown: "+n(RpgBalanceConfig.HEAVY_ATTACK_UPGRADE_COOLDOWN_SECONDS.get())+"s");
            case SwordsmanPlayerClass.BACKSTAB->lines.add("Backstab Damage: +"+n(RpgBalanceConfig.BACKSTAB_DAMAGE.get())+"%  Threshold: "+n(RpgBalanceConfig.BACKSTAB_DOT_THRESHOLD.get()));
            case SwordsmanPlayerClass.FIRST_HIT->lines.add("First Hit Damage: +"+n(RpgBalanceConfig.FIRST_HIT_DAMAGE.get())+"%  Reset: "+n(RpgBalanceConfig.FIRST_HIT_RESET_SECONDS.get())+"s");
            case SwordsmanPlayerClass.NON_AGGRO->lines.add("Non-Aggro Damage: +"+n(RpgBalanceConfig.NON_AGGRO_DAMAGE.get())+"%");
            case GeneralSkillTree.WOOD_DROPS->lines.add("Bonus Wood Chance: "+GeneralSkillBalance.EXTRA_DROP_CHANCE_PERCENT+"%  Additional Wood: 1");
            case GeneralSkillTree.ORE_DROPS->lines.add("Bonus Ore Chance: "+GeneralSkillBalance.EXTRA_DROP_CHANCE_PERCENT+"%  Additional Ore: 1");
            case GeneralSkillTree.MATERIAL_RECOVERY->lines.add("Recovery Chance: "+GeneralSkillBalance.MATERIAL_REFUND_CHANCE_PERCENT+"%  Materials Returned: 1");
            case GeneralSkillTree.ENCHANTED_CRAFTING->{lines.add("Enchantment Chance: "+GeneralSkillBalance.ENCHANTED_CRAFTING_CHANCE_PERCENT+"%");lines.add("Enchanting Power: "+GeneralSkillBalance.ENCHANTMENT_MIN_LEVEL+"-"+GeneralSkillBalance.ENCHANTMENT_MAX_LEVEL);}
        }
        return lines;
    }
    private static String signed(int value){return value>=0?"+"+value:Integer.toString(value);}
    private static String n(double value){return String.format(Locale.ROOT,"%.1f",value);}
}
