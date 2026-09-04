package com.ollie.tierborne.playerclass;

public enum SkillBonusType {
    SWORD_DAMAGE("Sword Damage Buff"),
    AXE_DAMAGE("Axe Damage Buff"),
    AXE_CHARGE_SPEED("Axe Charge Speed Buff"),
    BOW_DAMAGE("Bow Damage Buff"),
    CROSSBOW_DAMAGE("Crossbow Damage Buff"),
    RANGED_DAMAGE("All Ranged Damage Buff"),
    GENERAL_DAMAGE("General Damage Buff"),
    FIST_DAMAGE("Fist Damage Buff"),
    MELEE_CHARGE_SPEED("Melee Charge Speed Buff"),
    MOVEMENT_SPEED("Movement Speed Buff"),
    WOODCUTTING_SPEED("Woodcutting Speed"),
    WOOD_DROP_CHANCE("Wood Drop Bonus"),
    MINING_SPEED("Mining Speed"),
    ORE_DROP_CHANCE("Ore Drop Bonus"),
    MATERIAL_RECOVERY_CHANCE("Material Recovery Chance"),
    ENCHANTED_CRAFTING_CHANCE("Enchanted Crafting Chance");

    private final String displayName;

    SkillBonusType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
