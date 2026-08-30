package com.ollie.tierborne.playerclass;

import java.util.List;

public record AlternateAttackDefinition(String id, String skillId, String displayName, String description) {
    public static final List<AlternateAttackDefinition> ALL = List.of(
            new AlternateAttackDefinition("dash_strike", SwordsmanPlayerClass.DASH_STRIKE, "Dash Strike", "Dash forward and strike for increased damage."),
            new AlternateAttackDefinition("multislash", SwordsmanPlayerClass.MULTISLASH, "Multislash", "Alternate four fully charged attacks using two swords."),
            new AlternateAttackDefinition("heavy_attack", SwordsmanPlayerClass.HEAVY_ATTACK, "Heavy Attack", "Deliver a powerful sword strike."),
            new AlternateAttackDefinition("cloak", SwordsmanPlayerClass.CLOAK, "Cloak", "Become invisible and shed hostile attention."),
            new AlternateAttackDefinition("leap_strike", SwordsmanPlayerClass.LEAP_STRIKE, "Leap Strike", "Leap upward and strike nearby enemies on landing."));

    public static AlternateAttackDefinition find(String id) { return ALL.stream().filter(a -> a.id.equals(id)).findFirst().orElse(null); }
    public static AlternateAttackDefinition forSkill(String skillId) { return ALL.stream().filter(a -> a.skillId.equals(skillId)).findFirst().orElse(null); }
}
