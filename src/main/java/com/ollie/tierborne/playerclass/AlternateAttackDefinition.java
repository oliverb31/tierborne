package com.ollie.tierborne.playerclass;

import java.util.List;

public record AlternateAttackDefinition(String id, String skillId, String displayName, String description) {
    public static final List<AlternateAttackDefinition> ALL = List.of(
            new AlternateAttackDefinition("dash_strike", SwordsmanPlayerClass.DASH_STRIKE, "Dash Strike", "Dash forward and strike for increased damage."),
            new AlternateAttackDefinition("multislash", SwordsmanPlayerClass.MULTISLASH, "Multislash", "Perform two fully charged strikes with one sword or four with two swords."),
            new AlternateAttackDefinition("heavy_attack", SwordsmanPlayerClass.HEAVY_ATTACK, "Heavy Attack", "Deliver a powerful sword strike."),
            new AlternateAttackDefinition("cloak", SwordsmanPlayerClass.CLOAK, "Cloak", "Become invisible and shed hostile attention."),
            new AlternateAttackDefinition("leap_strike", SwordsmanPlayerClass.LEAP_STRIKE, "Leap Strike", "Leap upward and strike nearby enemies on landing."),
            new AlternateAttackDefinition("fireball", SwordsmanPlayerClass.FIREBALL, "Fireball", "Cast a server-authoritative fire projectile."),
            new AlternateAttackDefinition("multishot_bow", ArcherPlayerClass.MULTISHOT, "Multishot", "Fire two full-power crossbow arrows."),
            new AlternateAttackDefinition("fully_charged", ArcherPlayerClass.FULLY_CHARGED, "Fully Charged", "Empower one deliberately slow bow shot."),
            new AlternateAttackDefinition("elemental_shot", ArcherPlayerClass.ELEMENTAL_SHOT, "Elemental Shot", "Instantly fire a burning, freezing arrow."),
            new AlternateAttackDefinition("backstep", ArcherPlayerClass.BACKSTEP, "Backstep", "Fire while dashing away."),
            new AlternateAttackDefinition("natures_roots", ArcherPlayerClass.NATURES_ROOTS, "Nature's Roots", "Channel roots on the target under the crosshair."),
            new AlternateAttackDefinition("pull", FighterPlayerClass.PULL, "Pull", "Pull a target into a full-power fist strike."),
            new AlternateAttackDefinition("chain", FighterPlayerClass.CHAIN, "Chain", "Temporarily arm a stronger Combo."),
            new AlternateAttackDefinition("disarm", FighterPlayerClass.DISARM, "Disarm", "Temporarily disable a target's offensive actions."));

    public static AlternateAttackDefinition find(String id) { return ALL.stream().filter(a -> a.id.equals(id)).findFirst().orElse(null); }
    public static AlternateAttackDefinition forSkill(String skillId) { return ALL.stream().filter(a -> a.skillId.equals(skillId)).findFirst().orElse(null); }
}
