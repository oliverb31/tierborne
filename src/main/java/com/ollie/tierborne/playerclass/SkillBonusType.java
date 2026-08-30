package com.ollie.tierborne.playerclass;

public enum SkillBonusType {
    SWORD_DAMAGE("Sword Damage Buff"),
    BOW_DAMAGE("Bow Damage Buff"),
    MOVEMENT_SPEED("Movement Speed Buff"),
    WOODCUTTING_SPEED("Woodcutting Speed"),
    WOOD_DROP_CHANCE("Wood Drop Bonus"),
    MINING_SPEED("Mining Speed"),
    ORE_DROP_CHANCE("Ore Drop Bonus");

    private final String displayName;

    SkillBonusType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
