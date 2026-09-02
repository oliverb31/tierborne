package com.ollie.tierborne.client.screen;

import com.ollie.tierborne.config.RpgBalanceConfig;
import com.ollie.tierborne.playerclass.*;
import java.util.*;

final class SkillDetailLines {
    private SkillDetailLines() {}
    static List<String> forSkill(Skill skill, SkillTreeDefinition tree, Set<String> unlocked) {
        List<String> lines=new ArrayList<>();
        if(skill.upgrade()!=null&&!(tree instanceof ArcherPlayerClass)){int current=tree.totalBonus(skill.upgrade().type(),unlocked);lines.add(skill.upgrade().type().displayName()+": "+signed(skill.upgrade().percentagePoints())+"%");lines.add("Current: "+signed(current)+"%  After: "+signed(current+skill.upgrade().percentagePoints())+"%");}
        switch(skill.id()){
            case FighterPlayerClass.ROOT->lines.add("No inherent purchase effects.");
            case FighterPlayerClass.PULL->{lines.add("Range: "+n(RpgBalanceConfig.PULL_RANGE.get())+" blocks  Maximum Active: "+n(RpgBalanceConfig.PULL_MAX_SECONDS.get())+"s");lines.add("Cooldown: "+n(RpgBalanceConfig.PULL_COOLDOWN_SECONDS.get())+"s  Hit: Full-power fist attack");}
            case FighterPlayerClass.MONK->{lines.add("Movement Speed: +"+n(RpgBalanceConfig.MONK_MOVE.get())+"%  Fist Damage: +"+n(RpgBalanceConfig.MONK_FIST.get())+"%");lines.add("Left Click: Offhand Fist  Right Click: Primary Fist");}
            case FighterPlayerClass.PULL_RECOVERY->lines.add("Pull Cooldown: "+n(RpgBalanceConfig.PULL_COOLDOWN_SECONDS.get())+"s -> "+n(RpgBalanceConfig.PULL_UPGRADED_COOLDOWN_SECONDS.get())+"s");
            case FighterPlayerClass.EXTENDED_PULL->lines.add("Pull Range: "+n(RpgBalanceConfig.PULL_RANGE.get())+" -> "+n(RpgBalanceConfig.PULL_UPGRADED_RANGE.get())+" blocks");
            case FighterPlayerClass.MONK_FIST_UPGRADE->lines.add("Fist Damage: +"+n(RpgBalanceConfig.MONK_FIST_UPGRADE.get())+"%");
            case FighterPlayerClass.CHAMPION->{lines.add("Combo: +"+n(RpgBalanceConfig.COMBO_COMPOUND_PERCENT.get())+"% compounding (rounded up)");lines.add("Initial Hit: +0%  Window: "+n(RpgBalanceConfig.COMBO_WINDOW_SECONDS.get())+"s  Same target required");}
            case FighterPlayerClass.CHAIN->{lines.add("READY: "+n(RpgBalanceConfig.CHAIN_READY_SECONDS.get())+"s  Opening Bonus: +"+n(RpgBalanceConfig.CHAIN_OPENING_PERCENT.get())+"%");lines.add("Combo Window: "+n(RpgBalanceConfig.CHAIN_WINDOW_SECONDS.get())+"s  Compound: "+n(RpgBalanceConfig.COMBO_COMPOUND_PERCENT.get())+"%");lines.add("Cooldown after active: "+n(RpgBalanceConfig.CHAIN_COOLDOWN_SECONDS.get())+"s");}
            case FighterPlayerClass.COMBO_WINDOW_I->lines.add("Combo Window: "+n(RpgBalanceConfig.COMBO_WINDOW_SECONDS.get())+"s -> "+n(RpgBalanceConfig.COMBO_WINDOW_I_SECONDS.get())+"s");
            case FighterPlayerClass.COMBO_WINDOW_II->lines.add("Combo Window: "+n(RpgBalanceConfig.COMBO_WINDOW_I_SECONDS.get())+"s -> "+n(RpgBalanceConfig.COMBO_WINDOW_II_SECONDS.get())+"s");
            case FighterPlayerClass.CHAIN_BONUS->lines.add("Chain Opening Bonus: +"+n(RpgBalanceConfig.CHAIN_OPENING_PERCENT.get())+"% -> +"+n(RpgBalanceConfig.CHAIN_UPGRADED_OPENING_PERCENT.get())+"%");
            case FighterPlayerClass.CHAIN_WINDOW->lines.add("Chain Window: "+n(RpgBalanceConfig.CHAIN_WINDOW_SECONDS.get())+"s -> "+n(RpgBalanceConfig.CHAIN_UPGRADED_WINDOW_SECONDS.get())+"s");
            case FighterPlayerClass.DUELIST->{lines.add("General Weapon/Fist Damage: +"+n(RpgBalanceConfig.DUELIST_DAMAGE.get())+"%");lines.add("Counter Chance: "+n(RpgBalanceConfig.DUELIST_COUNTER_CHANCE.get())+"%  Damage: Full-power current attack");}
            case FighterPlayerClass.DISARM->{lines.add("Range: "+n(RpgBalanceConfig.DISARM_RANGE.get())+" blocks  Duration: "+n(RpgBalanceConfig.DISARM_DURATION_SECONDS.get())+"s");lines.add("Target current HP < "+n(RpgBalanceConfig.DISARM_HEALTH_RATIO.get()*100)+"% of your current HP");lines.add("Cooldown: "+n(RpgBalanceConfig.DISARM_COOLDOWN_SECONDS.get())+"s");}
            case FighterPlayerClass.COUNTER_CHANCE->lines.add("Counter Chance: "+n(RpgBalanceConfig.DUELIST_COUNTER_CHANCE.get())+"% -> "+n(RpgBalanceConfig.DUELIST_UPGRADED_COUNTER_CHANCE.get())+"%");
            case FighterPlayerClass.PERFECT_COUNTER->lines.add("Successful Counter: Negates triggering incoming hit");
            case FighterPlayerClass.REFLECTIVE_COUNTER->lines.add("Counter Damage: Maximum of raw incoming damage or full current attack");
            case FighterPlayerClass.DUELIST_DAMAGE_UPGRADE->lines.add("General Weapon/Fist Damage: +"+n(RpgBalanceConfig.DUELIST_DAMAGE_UPGRADE.get())+"%");
            case FighterPlayerClass.DISARM_DURATION->lines.add("Disarm Duration: "+n(RpgBalanceConfig.DISARM_DURATION_SECONDS.get())+"s -> "+n(RpgBalanceConfig.DISARM_UPGRADED_DURATION_SECONDS.get())+"s");
            case FighterPlayerClass.DISARM_COOLDOWN->lines.add("Disarm Cooldown: "+n(RpgBalanceConfig.DISARM_COOLDOWN_SECONDS.get())+"s -> "+n(RpgBalanceConfig.DISARM_UPGRADED_COOLDOWN_SECONDS.get())+"s");
            case ArcherPlayerClass.ROOT->{lines.add("Bow Damage: +"+n(RpgBalanceConfig.ARCHER_BOW_DAMAGE.get())+"%  Crossbow Damage: +"+n(RpgBalanceConfig.ARCHER_CROSSBOW_DAMAGE.get())+"%");lines.add("Ability: Dash");lines.add("Dash Distance: "+n(RpgBalanceConfig.ARCHER_DASH_DISTANCE.get())+" blocks  Cooldown: "+n(RpgBalanceConfig.ARCHER_DASH_COOLDOWN_SECONDS.get())+"s");}
            case ArcherPlayerClass.CROSSBOW_DAMAGE_1->lines.add("Crossbow Damage: +"+n(RpgBalanceConfig.CROSSBOW_DAMAGE_I.get())+"%");
            case ArcherPlayerClass.BOW_DAMAGE_1->lines.add("Bow Damage: +"+n(RpgBalanceConfig.BOW_DAMAGE_I.get())+"%");
            case ArcherPlayerClass.ELEMENTAL_DAMAGE_1->lines.add("Bow and Crossbow Damage: +"+n(RpgBalanceConfig.ELEMENTAL_RANGED_DAMAGE_I.get())+"%");
            case ArcherPlayerClass.ELEMENTAL_DAMAGE_2->lines.add("Bow and Crossbow Damage: +"+n(RpgBalanceConfig.ELEMENTAL_RANGED_DAMAGE_II.get())+"%");
            case ArcherPlayerClass.RANGER_DAMAGE->lines.add("Bow and Crossbow Damage: +"+n(RpgBalanceConfig.RANGER_RANGED_DAMAGE.get())+"%");
            case ArcherPlayerClass.RANGER_MOVEMENT->lines.add("Movement Speed: +"+n(RpgBalanceConfig.RANGER_PRE_MOVEMENT.get())+"%");
            case ArcherPlayerClass.CROSSBOW_SPEED_1->lines.add("Crossbow Charge Speed: +"+n(RpgBalanceConfig.CROSSBOW_CHARGE_SPEED_I.get())+"%");
            case ArcherPlayerClass.MULTISHOT->{lines.add("Second Shot Delay: "+n(RpgBalanceConfig.MULTISHOT_DELAY_SECONDS.get())+"s");lines.add("Cooldown after active: "+n(RpgBalanceConfig.MULTISHOT_COOLDOWN_SECONDS.get())+"s");}
            case ArcherPlayerClass.CROSSBOWMAN->lines.add("Crossbow Damage: +"+n(RpgBalanceConfig.CROSSBOWMAN_DAMAGE.get())+"%");
            case ArcherPlayerClass.CROSSBOW_DAMAGE_2->lines.add("Crossbow Damage: +"+n(RpgBalanceConfig.CROSSBOW_DAMAGE_II.get())+"%");
            case ArcherPlayerClass.CROSSBOW_SPEED_2->lines.add("Crossbow Charge Speed: +"+n(RpgBalanceConfig.CROSSBOW_CHARGE_SPEED_II.get())+"%");
            case ArcherPlayerClass.BOW_HANDLING->lines.add("Bow Charge Speed: +"+n(RpgBalanceConfig.BOW_HANDLING_SPEED.get())+"%");
            case ArcherPlayerClass.FULLY_CHARGED->{lines.add("Activation Window: "+n(RpgBalanceConfig.FULLY_CHARGED_READY_WINDOW_SECONDS.get())+"s");lines.add("Empowered Draw Time: "+n(RpgBalanceConfig.FULLY_CHARGED_DRAW_TIME_MULTIPLIER.get())+"x current normal draw");lines.add("Maximum Damage: "+n(RpgBalanceConfig.FULLY_CHARGED_MAX_MULTIPLIER.get())+"x equivalent normal arrow");lines.add("Movement: "+n(RpgBalanceConfig.FULLY_CHARGED_MOVEMENT_PENALTY.get())+"%  Cooldown: "+n(RpgBalanceConfig.FULLY_CHARGED_COOLDOWN_SECONDS.get())+"s");}
            case ArcherPlayerClass.LONGBOWMAN->{lines.add("Bow Damage: +"+n(RpgBalanceConfig.LONGBOWMAN_DAMAGE.get())+"%");lines.add("Normal Bow Draw Time: +"+n(-RpgBalanceConfig.LONGBOWMAN_CHARGE_SPEED.get())+"%");lines.add("Movement While Drawing: "+n(RpgBalanceConfig.LONGBOWMAN_DRAW_MOVEMENT.get())+"%");}
            case ArcherPlayerClass.LONGBOW_DAMAGE->lines.add("Bow Damage: +"+n(RpgBalanceConfig.LONGBOW_DAMAGE_UPGRADE.get())+"%");
            case ArcherPlayerClass.FULLY_CHARGED_MOBILITY->lines.add("Fully Charged Movement: "+n(RpgBalanceConfig.FULLY_CHARGED_MOVEMENT_PENALTY.get())+"% -> "+n(RpgBalanceConfig.FULLY_CHARGED_IMPROVED_MOVEMENT_PENALTY.get())+"%");
            case ArcherPlayerClass.ELEMENTAL_SHOT->{lines.add("Damage: 100% max-charge equivalent  Fire: "+n(RpgBalanceConfig.ELEMENTAL_SHOT_FIRE_SECONDS.get())+"s");lines.add("Ice: Slowness "+RpgBalanceConfig.ELEMENTAL_SHOT_SLOW_LEVEL.get()+" for "+n(RpgBalanceConfig.ELEMENTAL_SHOT_SLOW_SECONDS.get())+"s  Cooldown: "+n(RpgBalanceConfig.ELEMENTAL_SHOT_COOLDOWN_SECONDS.get())+"s");}
            case ArcherPlayerClass.ELEMENTAL_ARCHER->lines.add("Unlocks one free, permanent Fire or Ice specialisation.");
            case ArcherPlayerClass.FIRE->lines.add("Normal arrows ignite for "+n(RpgBalanceConfig.FIRE_PASSIVE_SECONDS.get())+"s");
            case ArcherPlayerClass.FIRE_DURATION->lines.add("Burn Duration: "+n(RpgBalanceConfig.FIRE_PASSIVE_SECONDS.get())+"s -> "+n(RpgBalanceConfig.FIRE_UPGRADED_SECONDS.get())+"s");
            case ArcherPlayerClass.FIRE_DAMAGE->lines.add("Fire Arrow Damage: +"+n(RpgBalanceConfig.FIRE_BONUS_DAMAGE.get())+"%");
            case ArcherPlayerClass.ICE->lines.add("Normal arrows: Slowness "+RpgBalanceConfig.ICE_PASSIVE_LEVEL.get()+" for "+n(RpgBalanceConfig.ICE_PASSIVE_SECONDS.get())+"s");
            case ArcherPlayerClass.ICE_POTENCY->lines.add("Slowness: "+RpgBalanceConfig.ICE_PASSIVE_LEVEL.get()+" -> "+RpgBalanceConfig.ICE_UPGRADED_LEVEL.get());
            case ArcherPlayerClass.ICE_DURATION->lines.add("Ice Duration: "+n(RpgBalanceConfig.ICE_PASSIVE_SECONDS.get())+"s -> "+n(RpgBalanceConfig.ICE_UPGRADED_SECONDS.get())+"s");
            case ArcherPlayerClass.RANGER->lines.add("Movement Speed: +"+n(RpgBalanceConfig.RANGER_MOVEMENT.get())+"%");
            case ArcherPlayerClass.BACKSTEP->{lines.add("Distance: "+n(RpgBalanceConfig.BACKSTEP_DISTANCE.get())+" blocks");lines.add("Cooldown: "+n(RpgBalanceConfig.BACKSTEP_COOLDOWN_SECONDS.get())+"s");}
            case ArcherPlayerClass.BACKSTEP_DOUBLE->lines.add("Recast Window: "+n(RpgBalanceConfig.BACKSTEP_RECAST_SECONDS.get())+"s");
            case ArcherPlayerClass.BACKSTEP_DIRECTIONAL->lines.add("Dash Direction: Current movement input");
            case ArcherPlayerClass.BACKSTEP_RANGE->lines.add("Backstep Distance: "+n(RpgBalanceConfig.BACKSTEP_DISTANCE.get())+" -> "+n(RpgBalanceConfig.BACKSTEP_UPGRADED_DISTANCE.get())+" blocks");
            case ArcherPlayerClass.NATURES_ROOTS->{lines.add("Max Channel: "+n(RpgBalanceConfig.ROOTS_CHANNEL_SECONDS.get())+"s  Target Extra Root: "+n(RpgBalanceConfig.ROOTS_TAIL_SECONDS.get())+"s");lines.add("Range: "+n(RpgBalanceConfig.ROOTS_RANGE.get())+" blocks  Cooldown: "+n(RpgBalanceConfig.ROOTS_COOLDOWN_SECONDS.get())+"s");}
            case SwordsmanPlayerClass.DASH_STRIKE->{lines.add("Damage: +"+n(RpgBalanceConfig.DASH_STRIKE_DAMAGE.get())+"%");lines.add("Cooldown: "+n(RpgBalanceConfig.DASH_STRIKE_COOLDOWN_SECONDS.get())+"s  Distance: "+n(RpgBalanceConfig.DASH_STRIKE_DISTANCE.get())+" blocks");}
            case SwordsmanPlayerClass.DASH_DISTANCE->lines.add("Dash Distance: "+n(RpgBalanceConfig.DASH_STRIKE_DISTANCE.get())+" -> "+n(RpgBalanceConfig.DASH_STRIKE_UPGRADED_DISTANCE.get())+" blocks");
            case SwordsmanPlayerClass.DASH_COOLDOWN->lines.add("Dash Strike Cooldown: "+n(RpgBalanceConfig.DASH_STRIKE_COOLDOWN_SECONDS.get())+"s -> "+n(RpgBalanceConfig.DASH_STRIKE_UPGRADED_COOLDOWN_SECONDS.get())+"s");
            case SwordsmanPlayerClass.MULTISLASH->{lines.add("AOE Width: "+n(RpgBalanceConfig.MULTISLASH_SWEEP_WIDTH.get())+"  Range Bonus: +"+n(RpgBalanceConfig.MULTISLASH_BONUS_RANGE.get())+" blocks");lines.add("Duration: "+n(RpgBalanceConfig.MULTISLASH_DURATION_SECONDS.get())+"s  Cooldown: "+n(RpgBalanceConfig.MULTISLASH_COOLDOWN_SECONDS.get())+"s");}
            case SwordsmanPlayerClass.HEAVY_ATTACK->{lines.add("Damage: +"+n(RpgBalanceConfig.HEAVY_ATTACK_DAMAGE.get())+"%");lines.add("Cooldown / attack lockout: "+n(RpgBalanceConfig.HEAVY_ATTACK_COOLDOWN_SECONDS.get())+"s");}
            case SwordsmanPlayerClass.CLOAK->{lines.add("Duration: "+n(RpgBalanceConfig.CLOAK_DURATION_SECONDS.get())+"s  Cooldown: "+n(RpgBalanceConfig.CLOAK_COOLDOWN_SECONDS.get())+"s");lines.add("Breaks on attack; drops mob aggro");}
            case SwordsmanPlayerClass.LEAP_STRIKE->{lines.add("Cooldown: "+n(RpgBalanceConfig.LEAP_COOLDOWN_SECONDS.get())+"s  Radius: "+n(RpgBalanceConfig.LEAP_RADIUS.get()));lines.add("Launch: "+n(RpgBalanceConfig.LEAP_LAUNCH.get())+"  Knockback: "+n(RpgBalanceConfig.LEAP_KNOCKBACK.get()));}
            case SwordsmanPlayerClass.SWORDMASTER->{lines.add("Sword Damage: +"+n(RpgBalanceConfig.SWORDMASTER_DAMAGE.get())+"%  Movement: +"+n(RpgBalanceConfig.SWORDMASTER_SPEED.get())+"%");lines.add("Dash cooldown: "+n(RpgBalanceConfig.DASH_COOLDOWN_SECONDS.get())+"s  Velocity: "+n(RpgBalanceConfig.DASH_VELOCITY.get()));}
            case SwordsmanPlayerClass.DUAL->{lines.add("Left Click: Offhand Sword  Right Click: Primary Sword");lines.add("Damage/Sword: "+n(RpgBalanceConfig.DUAL_DAMAGE.get())+"%  Block Cooldown: "+n(RpgBalanceConfig.BLOCK_COOLDOWN_SECONDS.get())+"s");}
            case SwordsmanPlayerClass.HEAVY->{lines.add("Sword Damage: +"+n(RpgBalanceConfig.HEAVY_DAMAGE.get())+"%  Sword Charge Speed: "+n(RpgBalanceConfig.HEAVY_ATTACK_SPEED.get())+"%");lines.add("Sword Movement: "+n(RpgBalanceConfig.HEAVY_MOVE_PENALTY.get())+"%  Draw: "+n(RpgBalanceConfig.HEAVY_DRAW_DELAY_SECONDS.get())+"s");}
            case SwordsmanPlayerClass.ROGUE->{lines.add("Maximum Health: -"+n(RpgBalanceConfig.ROGUE_HEALTH_PENALTY.get())+"  Movement: +"+n(RpgBalanceConfig.ROGUE_SPEED.get())+"%");lines.add("Alternative target radius: "+n(RpgBalanceConfig.ROGUE_RETARGET_RADIUS.get())+" blocks");}
            case SwordsmanPlayerClass.SM_DAMAGE->lines.add("Sword Damage Contribution: +"+n(RpgBalanceConfig.SWORDMASTER_UPGRADE_DAMAGE.get())+"%");
            case SwordsmanPlayerClass.SM_SPEED->lines.add("Movement Speed Contribution: +"+n(RpgBalanceConfig.SWORDMASTER_UPGRADE_SPEED.get())+"%");
            case SwordsmanPlayerClass.SM_DASH_STRIKE->lines.add("Dash Strike Damage: +"+n(RpgBalanceConfig.DASH_STRIKE_UPGRADE_DAMAGE.get())+"%");
            case SwordsmanPlayerClass.SM_DASH->lines.add("Dash: "+n(RpgBalanceConfig.DASH_UPGRADE_VELOCITY.get())+" velocity, "+n(RpgBalanceConfig.DASH_UPGRADE_COOLDOWN_SECONDS.get())+"s cooldown");
            case SwordsmanPlayerClass.DUAL_SPEED->lines.add("Sword Charge Speed: +"+n(RpgBalanceConfig.DUAL_SPEED_UPGRADE.get())+"%");
            case SwordsmanPlayerClass.DUAL_DAMAGE->lines.add("Damage Per Sword: "+n(RpgBalanceConfig.DUAL_DAMAGE_UPGRADE.get())+"%");
            case SwordsmanPlayerClass.PARRY->lines.add("Counterattacks: 2  Cooldown: "+n(RpgBalanceConfig.PARRY_COOLDOWN_SECONDS.get())+"s");
            case SwordsmanPlayerClass.IMPROVED_BLOCK->lines.add("Knockback while blocking: Prevented");
            case SwordsmanPlayerClass.FIREBALL->{lines.add("Cast: "+n(RpgBalanceConfig.FIREBALL_CAST_SECONDS.get())+"s  Cooldown: "+n(RpgBalanceConfig.FIREBALL_COOLDOWN_SECONDS.get())+"s");lines.add("Sword Damage: "+n(RpgBalanceConfig.FIREBALL_DAMAGE_PERCENT.get())+"%  Ignition: "+n(RpgBalanceConfig.FIREBALL_IGNITION_SECONDS.get())+"s");}
            case SwordsmanPlayerClass.MAGIC->lines.add("Intrinsic Sword Fire: "+n(RpgBalanceConfig.MAGIC_SWORD_FIRE_SECONDS.get())+"s");
            case SwordsmanPlayerClass.ELEMENTAL_VULNERABILITY->lines.add("Elemental Damage: +"+n(RpgBalanceConfig.ELEMENTAL_VULNERABILITY_BONUS_PERCENT.get())+"% for "+n(RpgBalanceConfig.ELEMENTAL_VULNERABILITY_SECONDS.get())+"s");
            case SwordsmanPlayerClass.FIREBALL_COOLDOWN->lines.add("Fireball Cooldown: "+n(RpgBalanceConfig.FIREBALL_COOLDOWN_SECONDS.get())+"s -> "+n(RpgBalanceConfig.FIREBALL_UPGRADED_COOLDOWN_SECONDS.get())+"s");
            case SwordsmanPlayerClass.FIREBALL_CHARGE->lines.add("Charge: "+n(RpgBalanceConfig.FIREBALL_MAX_CHARGE_SECONDS.get())+"s  Damage: "+n(RpgBalanceConfig.FIREBALL_MIN_DAMAGE_PERCENT.get())+"-"+n(RpgBalanceConfig.FIREBALL_MAX_DAMAGE_PERCENT.get())+"%");
            case SwordsmanPlayerClass.TRIPLE_FIREBALL->lines.add("Fireballs: 1 -> 3  Recast Window: "+n(RpgBalanceConfig.TRIPLE_FIREBALL_RECAST_SECONDS.get())+"s");
            case SwordsmanPlayerClass.FIREBALL_SPEED_I->lines.add("Fireball Speed: "+n(RpgBalanceConfig.FIREBALL_BASE_SPEED.get())+" -> "+n(RpgBalanceConfig.FIREBALL_SPEED_I.get())+" blocks/s");
            case SwordsmanPlayerClass.FIREBALL_SPEED_II->lines.add("Fireball Speed: "+n(RpgBalanceConfig.FIREBALL_SPEED_I.get())+" -> "+n(RpgBalanceConfig.FIREBALL_SPEED_II.get())+" blocks/s");
            case SwordsmanPlayerClass.FIREBALL_DAMAGE_I->lines.add("Fireball Damage: "+n(RpgBalanceConfig.FIREBALL_DAMAGE_PERCENT.get())+"% -> "+n(RpgBalanceConfig.FIREBALL_DAMAGE_I.get())+"% base");
            case SwordsmanPlayerClass.FIREBALL_DAMAGE_II->lines.add("Fireball Damage: "+n(RpgBalanceConfig.FIREBALL_DAMAGE_I.get())+"% -> "+n(RpgBalanceConfig.FIREBALL_DAMAGE_II.get())+"% base");
            case SwordsmanPlayerClass.FLAME_SLASH->{lines.add("Damage: 2x full-charge Fireball  Cooldown: "+n(RpgBalanceConfig.FLAME_SLASH_COOLDOWN_SECONDS.get())+"s");lines.add("Width: "+n(RpgBalanceConfig.FLAME_SLASH_WIDTH.get())+"  Range: "+n(RpgBalanceConfig.FLAME_SLASH_MAX_RANGE.get())+" blocks");}
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
