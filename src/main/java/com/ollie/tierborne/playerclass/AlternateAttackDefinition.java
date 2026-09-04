package com.ollie.tierborne.playerclass;

import java.util.List;

public record AlternateAttackDefinition(String id, String skillId, String displayName, String description) {
    public static final List<AlternateAttackDefinition> ALL = List.of(
            new AlternateAttackDefinition("dash_strike", SwordsmanPlayerClass.DASH_STRIKE, "Dash Strike", "Dash forward and strike for increased damage."),
            new AlternateAttackDefinition("multislash", SwordsmanPlayerClass.MULTISLASH, "Multislash", "Deal one combined hit equal to two full-power strikes from each equipped sword."),
            new AlternateAttackDefinition("heavy_attack", SwordsmanPlayerClass.HEAVY_ATTACK, "Heavy Attack", "Deliver a powerful sword strike."),
            new AlternateAttackDefinition("cloak", SwordsmanPlayerClass.CLOAK, "Cloak", "Become invisible and shed hostile attention."),
            new AlternateAttackDefinition("leap_strike", SwordsmanPlayerClass.LEAP_STRIKE, "Leap Strike", "Leap upward and strike nearby enemies on landing."),
            new AlternateAttackDefinition("fireball", SwordsmanPlayerClass.FIREBALL, "Fireball", "Cast a server-authoritative fire projectile."),
            new AlternateAttackDefinition("flame_slash", SwordsmanPlayerClass.FLAME_SLASH, "Flame Slash", "Launch a travelling crescent of elemental flame."),
            new AlternateAttackDefinition("multishot_bow", ArcherPlayerClass.MULTISHOT, "Multishot", "Fire two full-power crossbow arrows."),
            new AlternateAttackDefinition("fully_charged", ArcherPlayerClass.FULLY_CHARGED, "Fully Charged", "Empower one deliberately slow bow shot."),
            new AlternateAttackDefinition("elemental_shot", ArcherPlayerClass.ELEMENTAL_SHOT, "Elemental Shot", "Instantly fire a burning, freezing arrow."),
            new AlternateAttackDefinition("backstep", ArcherPlayerClass.BACKSTEP, "Backstep", "Fire while dashing away."),
            new AlternateAttackDefinition("natures_roots", ArcherPlayerClass.NATURES_ROOTS, "Nature's Roots", "Channel roots on the target under the crosshair."),
            new AlternateAttackDefinition("pull", FighterPlayerClass.PULL, "Pull", "Pull a target into a full-power fist strike."),
            new AlternateAttackDefinition("uppercut", FighterPlayerClass.UPPERCUT, "Uppercut", "Strike upward with both hands empty and launch a nearby target."),
            new AlternateAttackDefinition("chain", FighterPlayerClass.CHAIN, "Chain", "Temporarily arm a stronger Combo."),
            new AlternateAttackDefinition("disarm", FighterPlayerClass.DISARM, "Disarm", "Temporarily disable a target's offensive actions."),
            new AlternateAttackDefinition("berserk", BarbarianPlayerClass.BERSERKER, "Berserk", "Toggle blood-fuelled axe damage, speed, lifesteal and Bleed."),
            new AlternateAttackDefinition("execute", BarbarianPlayerClass.EXECUTIONER, "Execute", "Charge an axe strike that grows stronger as the target weakens."));

    public static AlternateAttackDefinition find(String id) { return ALL.stream().filter(a -> a.id.equals(id)).findFirst().orElse(null); }
    public static AlternateAttackDefinition forSkill(String skillId) { return ALL.stream().filter(a -> a.skillId.equals(skillId)).findFirst().orElse(null); }
}
